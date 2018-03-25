package org.ignas.frauddetection.probabilitystatistics.service.repositories;

import com.google.common.collect.Iterables;
import com.mongodb.async.client.MongoClient;
import com.mongodb.async.client.MongoClients;
import com.mongodb.async.client.MongoCollection;
import com.mongodb.client.model.*;
import io.vertx.core.Future;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.ignas.frauddetection.probabilitystatistics.api.response.BayesTable;
import org.ignas.frauddetection.probabilitystatistics.domain.CriteriaStatistics;
import org.ignas.frauddetection.probabilitystatistics.domain.CriteriaUpdate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.mongodb.client.model.Filters.all;
import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;

public class CriteriaStorage {

    private static final String OCCURRENCES_FIELD = "totalOccurrences";
    private static final String OCCURRENCES_IN_FRAUD_FIELD = "occurrencesInFraud";

    private MongoClient client;

    private MongoCollection<Document> criteriaProbabilities;

    public CriteriaStorage(String url, String database, String collection) {
        client = MongoClients.create(url);

        criteriaProbabilities = client.getDatabase(database)
            .getCollection(collection);

    }

    public Future<List<CriteriaStatistics>> fetchValues(List<String> names) {
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

    public Future<List<CriteriaStatistics>> fetchStatistics(Map<String, String> criteriaValues) {
        Future<List<CriteriaStatistics>> totalLoader = Future.future();

        List<CriteriaStatistics> loadedStatistics = new ArrayList<>();

        int criteriaToLoad = criteriaValues.size();

        criteriaValues.entrySet()
            .stream()
            .map(criterion -> fetchStatistics(criterion.getKey(), criterion.getValue()))
            .forEach(loader -> loader.setHandler(criteriaResult -> {
                if (loader.failed()) {
                    loader.cause().printStackTrace();
                    totalLoader.fail(loader.cause());
                    return;
                }

                loadedStatistics.add(criteriaResult.result());

                if (loadedStatistics.size() == criteriaToLoad) {
                    totalLoader.complete(loadedStatistics);
                }
            }));

        return totalLoader;
    }

    /**
     * Overloaded method for single criteria fetching.
     * @param name
     * @param value
     * @return
     */
    public Future<CriteriaStatistics> fetchStatistics(String name, String value) {
        Future<CriteriaStatistics> loader = Future.future();
        criteriaProbabilities.find(
            and(eq("name", name), eq("values.name", name)))
            .projection(Projections.include("name", "values.$"))
            .first((result, t) -> {
                if (t != null) {
                    t.printStackTrace();
                    loader.fail(t);
                    return;
                }

                if (result == null) {
                    loader.complete(
                        new CriteriaStatistics(
                            name,
                            value,
                            0l,
                            0l
                        )
                    );
                    return;
                }

                Document values = Iterables.getFirst((List< Document >) result.get("values"), null);
                if (values == null) {
                    throw new IllegalStateException(
                        "Values missing for criteria: " + name + " Value: " + name
                    );
                }

                loader.complete(
                    new CriteriaStatistics(
                        name,
                        value,
                        values.getLong(OCCURRENCES_FIELD),
                        values.getLong(OCCURRENCES_IN_FRAUD_FIELD)
                    )
                );
            });

        return loader;
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
        List<UpdateOneModel<Document>> emptyDocumentPopulation = changes.stream()
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
        Document filter = new Document("name", update.targetCriteria());
        Document updateOperation = new Document("name", update.targetCriteria())
            .append("values", new ArrayList<>());

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
            new Document("name", update.targetCriteria())
                .append("values", new Document("$not", new Document("$elemMatch", new Document("name", update.criteriaValue())))),

            new Document("$addToSet", new Document("values", new Document("name", update.criteriaValue())))
        );
    }


    /**
     * update({ "name" : "NOT_EXPECTED", "values.name" : "MORE"}, { $inc: { "values.$.totalOccurences" : 1 }})
     *
     * @param update
     * @return
     */
    private static UpdateOneModel<Document> mapToMongoOperation(CriteriaUpdate update) {
        Document filter = new Document("name", update.targetCriteria())
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
}
