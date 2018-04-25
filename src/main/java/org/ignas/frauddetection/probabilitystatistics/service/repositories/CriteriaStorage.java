package org.ignas.frauddetection.probabilitystatistics.service.repositories;

import com.mongodb.async.client.MongoClient;
import com.mongodb.async.client.MongoClients;
import com.mongodb.async.client.MongoCollection;
import com.mongodb.client.model.*;
import io.vertx.core.Future;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.ignas.frauddetection.DetectionLauncher;
import org.ignas.frauddetection.probabilitystatistics.domain.CriteriaStatistics;
import org.ignas.frauddetection.probabilitystatistics.domain.CriteriaUpdate;
import org.joda.time.LocalDateTime;
import org.joda.time.Seconds;

import java.util.*;
import java.util.stream.Collectors;

import static com.mongodb.client.model.Filters.all;
import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;

public class CriteriaStorage {

    private static final String OCCURRENCES_FIELD = "totalOccurrences";
    private static final String OCCURRENCES_IN_FRAUD_FIELD = "occurrencesInFraud";
    public static final String NAME_FIELD = "name";
    public static final String VALUE_NAME_FIELD = "name";
    public static final String VALUES_OBJECT = "values";

    private MongoClient client;

    private MongoCollection<Document> criteriaProbabilities;

    private List<CriteriaStatistics> CACHE = null;
    private LocalDateTime CACHED_AT = null;

    public CriteriaStorage(String database, String collection) {
        client = MongoClients.create(DetectionLauncher.BAYES_MONGODB_SETTINGS);

        criteriaProbabilities = client.getDatabase(database)
            .getCollection(collection);
    }

    public Future<List<CriteriaStatistics>> fetchValues(List<String> names) {
        long start = System.currentTimeMillis();

//      Collect filters and join using OR condition, this way preventing multiple roundtrips to DB.
        List<Bson> nameFilters = names.stream()
            .map(name -> Filters.eq("name", name))
            .collect(Collectors.toList());

        Future<List<CriteriaStatistics>> loader = Future.future();

        List<CriteriaStatistics> statisticsResult = new ArrayList<>();

        criteriaProbabilities.find(Filters.or(nameFilters))
            .forEach(
                doc -> statisticsResult.addAll(extractValues(doc)),
                (result, t) -> {
                    if (t != null) {
                        t.printStackTrace();
                        loader.fail(t);
                        return;
                    }

                    loader.complete(statisticsResult);
                });

        return loader;
    }

    public Future<List<CriteriaStatistics>> fetchStatistics(Map<String, String> requestedValues) {
        if (CACHE != null && CACHED_AT != null
            && Seconds.secondsBetween(LocalDateTime.now(), CACHED_AT).getSeconds() * 1000 <= DetectionLauncher.CACHE_TTL) {

            List<CriteriaStatistics> cachedResultValues = new ArrayList<>();

            for(Map.Entry<String, String> requested: requestedValues.entrySet()) {
                Optional<CriteriaStatistics> cachedValue = CACHE.stream()
                    .filter(it -> it.getName().equals(requested.getKey()) && it.getValue().equals(requested.getValue()))
                    .findAny();

                cachedResultValues.add(cachedValue.get());
            }

            if (cachedResultValues.size() == requestedValues.size()) {
                System.out.println("CriteriaStorage.fetchStatistics Returns from cache.");

                return Future.succeededFuture(CACHE);
            }
            // If sizes does not match, this means that some value is missing in cache and whole cache will be reloaded.
        }

        Future<List<CriteriaStatistics>> totalLoader = Future.future();

        List<CriteriaStatistics> loadedStatistics = new ArrayList<>();


        criteriaProbabilities.find()
            .projection(Projections.include("name", "values"))
            .forEach(
                document -> {
                    String name = document.getString(NAME_FIELD);

                    List<Document> values = (List<Document>) document.get("values");

                    if (values == null) {
                        totalLoader.fail(new IllegalStateException("Values missing for criteria: " + name));
                        throw new IllegalStateException("Values missing for criteria: " + name);
                    }


                    for (Document valueDoc: values) {

                        String value = valueDoc.getString(NAME_FIELD);

                        loadedStatistics.add(new CriteriaStatistics(
                                name,
                                value,
                                valueDoc.getLong(OCCURRENCES_FIELD),
                                valueDoc.getLong(OCCURRENCES_IN_FRAUD_FIELD)
                            )
                        );
                    }
                },
                (result, t) -> {
                    if (t != null) {
                        t.printStackTrace();
                        totalLoader.fail(t);
                        return;
                    }

                    addUnknownValues(requestedValues, loadedStatistics);

                    // Caching all response.
                    CACHE = loadedStatistics;
                    System.out.println("CriteriaStorage.fetchStatistics updates cache.");


                    List<CriteriaStatistics> requestedResult = new ArrayList<>();
                    for (CriteriaStatistics stats: loadedStatistics) {

                        boolean requested = requestedValues.entrySet().stream()
                            .anyMatch(it -> stats.getName().equals(it.getKey()) && stats.getValue().equals(it.getValue()));

                        if (!requested) {
                            continue;
                        }

                        requestedResult.add(stats);
                    }

                    totalLoader.complete(requestedResult);
                });

        return totalLoader;
    }



    /**
     * Nested on purpose.
     * Implements mongo command sequence:
     *
     * CREATES EMPTY ROOT OBJECT IF NOT EXISTS:
     * .update({ "name" : "NOT_EXPECTED" }, { $setOnInsert: { "name" : "NOT_EXPECTED", "values" : []}}, { upsert : true })
     *
     * CREATES MISSING VALUES IN ARRAY:
     * db.criteriaProbabilities.update({ "name" : "NOT_EXPECTED" }, { $addToSet: { "values" : { "name" : "MORE"}}})
     *
     * INCREMENTS MISSING VALUES:
     * db.criteriaProbabilities.update({ "name" : "NOT_EXPECTED", "values.name" : "MORE"}, { $inc: { "values.$.totalOccurences" : 1 }})
     *
     * Such sequence is workaround to this: https://jira.mongodb.org/browse/SERVER-24340
     * The problem is that, positional operator ($) does not support upserting.
     *  Therefore with current mongo implementation it is impossible to create missing documents
     *    and increment existing documents with signle query.
     *
     *    Another possibility would be fetchCombination all existing documents from mongo,
     *    identify documents which are missing, and create only those who are missing.
     *    This would require additional application logic, would be less effective.
     *     Fetching all documents in order to find missing ones is required.
     *     With these queries mongo does creation in place.
     *
     *
     *
     */
    public void persist(List<CriteriaUpdate> changes) {
        List<CriteriaUpdate> documentInsertions = new ArrayList<>();

        List<UpdateOneModel<Document>> emptyDocumentPopulation = changes.stream()
            .filter(update -> documentInsertions.stream()
                .noneMatch(insertion -> insertion.targetCriteria().equals(update.targetCriteria()))
            )
            .peek(documentInsertions::add)
            .map(CriteriaStorage::mapToDocumentCreation)
            .collect(Collectors.toList());

        List<UpdateOneModel<Document>> missingValuesCreation = changes.stream()
            .map(CriteriaStorage::mapToValueCreation)
            .collect(Collectors.toList());

        List<UpdateOneModel<Document>> requiredIncrements = changes.stream()
            .map(CriteriaStorage::mapToMongoOperation)
            .collect(Collectors.toList());


        criteriaProbabilities.bulkWrite(
            emptyDocumentPopulation,
            (populationResult, populationException) -> {
                if (populationException != null) {
                    System.out.println(populationException.getMessage());
                    return;
                }

                criteriaProbabilities.bulkWrite(
                    missingValuesCreation,
                    (valueResult, valueException) -> {
                        if (valueException != null) {
                            System.out.println(valueException.getMessage());
                            return;
                        }

                        criteriaProbabilities.bulkWrite(
                            requiredIncrements,
                            new BulkWriteOptions().ordered(false),
                            (result, t) -> {
                                if (t != null) {
                                    System.out.println(t.getMessage());
                                }
                            });
                    });
            });
    }

    public void close() {
        client.close();
    }

    /**
     * Raw mongo query:
     *  update({ "name" : "NOT_EXPECTED" }, { $setOnInsert: { "name" : "NOT_EXPECTED", "values" : []}}, { upsert : true })
     *
     * This query is meant to insert empty documents that are missing. (Does not affect existing documents)
     * @param update
     * @return
     */
    private static UpdateOneModel<Document> mapToDocumentCreation(CriteriaUpdate update) {
        Document filter = new Document(NAME_FIELD, update.targetCriteria());
        Document updateOperation = new Document(NAME_FIELD, update.targetCriteria())
            .append(VALUES_OBJECT, new ArrayList<>());

        return new UpdateOneModel<>(
            filter,
            new Document("$setOnInsert", updateOperation),
            new UpdateOptions().upsert(true)
        );
    }

    /**
     * Raw mongo query:
     *  update(
     *      {"name" : "EXPECTED", "values" : { $not : { $elemMatch : { "name" : "MORE" } } }},
     *      { $addToSet: { "values" : { "name" : "MORE"}}}
     *  )
     *
     * This query is meant to insert criteria values that are missing (Does not affect existing values).
     *
     * @param update
     * @return
     */
    private static UpdateOneModel<Document> mapToValueCreation(CriteriaUpdate update) {
        return new UpdateOneModel<>(
            new Document(NAME_FIELD, update.targetCriteria())
                .append(VALUES_OBJECT,
                    new Document("$not", new Document("$elemMatch", new Document(VALUE_NAME_FIELD, update.criteriaValue())))),
            new Document("$addToSet", new Document(VALUES_OBJECT, new Document(VALUE_NAME_FIELD, update.criteriaValue())))
        );
    }


    /**
     * update({ "name" : "NOT_EXPECTED", "values.name" : "MORE"}, { $inc: { "values.$.totalOccurences" : 1 }})
     *
     * @param update
     * @return
     */
    private static UpdateOneModel<Document> mapToMongoOperation(CriteriaUpdate update) {
        Document filter = new Document(NAME_FIELD, update.targetCriteria())
            .append("values.name", update.criteriaValue());

        Document updateOperation = buildOperation(update);

        return new UpdateOneModel<>(
            filter,
            new Document("$inc", updateOperation)
        );
    }

    private static Document buildOperation(CriteriaUpdate update) {
        Document updateOperation = new Document();

        if (update.getNewOccurences() != 0) {
            updateOperation.append("values.$." + OCCURRENCES_FIELD, (long) update.getNewOccurences());
        }

        if (update.getNewFraudOccurences() != 0) {
            updateOperation.append("values.$." + OCCURRENCES_IN_FRAUD_FIELD, (long) update.getNewFraudOccurences());
        }
        return updateOperation;
    }


    private List<CriteriaStatistics> extractValues(Document doc) {
        List<CriteriaStatistics> documentValues = new ArrayList<>();

        String criteriaName = doc.getString("name");

        List<Document> values = (List<Document>) doc.get("values");
        values.forEach(value -> {
            String valueName = value.getString("name");
            Long occurences = value.getLong(OCCURRENCES_FIELD);
            Long occurencesInFraud = value.getLong(OCCURRENCES_IN_FRAUD_FIELD);

            documentValues.add(new CriteriaStatistics(criteriaName, valueName, occurences, occurencesInFraud));
        });
        return documentValues;
    }

    private void addUnknownValues(Map<String, String> criteriaValues, List<CriteriaStatistics> loadedStatistics) {
        for (Map.Entry<String, String> requestedValue : criteriaValues.entrySet()) {
            boolean notPresent = loadedStatistics.stream()
                .noneMatch(it -> it.getName().equals(requestedValue.getKey()) && it.getValue().equals(requestedValue.getValue()));

            if (notPresent) {
                loadedStatistics.add(
                    new CriteriaStatistics(
                        requestedValue.getKey(),
                        requestedValue.getValue(),
                        0l,
                        0l
                    )
                );
            }
        }
    }
}
