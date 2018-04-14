package org.ignas.frauddetection.probabilitystatistics.service.repositories;

import com.google.common.collect.ImmutableList;
import com.mongodb.async.client.MongoClient;
import com.mongodb.async.client.MongoClients;
import com.mongodb.async.client.MongoCollection;
import com.mongodb.client.model.*;
import io.vertx.core.Future;
import org.bson.Document;
import org.ignas.frauddetection.probabilitystatistics.domain.CombinationStatistics;
import org.ignas.frauddetection.probabilitystatistics.domain.GroupTotalStats;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.google.common.collect.Iterables.getFirst;
import static com.mongodb.client.model.Filters.*;

public class GroupStatisticsStorage {

    private static final String DEVIATION_PROBABILITY_FIELD = "deviationProbability";
    private static final String OCCURRENCES_SUM_FIELD = "sumOfOccurrencesInFraud";
    private static final String SQUARED_OCCURRENCES_SUM_FIELD = "sumOfSquaredOccurrencesInFraud";
    private static final String AVERAGE_PROBABILITY_FIELD = "averageProbability";
    private static final String NAME_FIELD = "name";
    private static final String TOTALS_DOC = "totals";
    private static final String COMBINATIONS_DOC = "combinations";
    private static final String CODE_FIELD = "code";
    private static final String OCCURENCES_FIELD = "occurences";
    private static final String FRAUD_OCCURENCES_FIELD = "fraudOccurences";

    private MongoClient client;
    private MongoCollection<Document> groupStatistics;

    public GroupStatisticsStorage(String url, String database, String collection) {
        client = MongoClients.create(url);

        groupStatistics = client.getDatabase(database)
            .getCollection(collection);
    }

    public Future<CombinationStatistics> fetchCombination(CombinationStatistics combination) {
        Future<CombinationStatistics> loader = Future.future();

        groupStatistics.find(and(eq(NAME_FIELD, combination.getGroup()), eq(COMBINATIONS_DOC + "." + CODE_FIELD, combination.getCode())))
            .projection(Projections.include(NAME_FIELD, COMBINATIONS_DOC + ".$"))
            .first((result, t) -> {
                if (t != null) {
                    t.printStackTrace();
                    loader.fail(t);
                    return;
                }

                Document loadedCombination = getFirst((List <Document>) result.get(COMBINATIONS_DOC), null);

                loader.complete(
                    new CombinationStatistics(
                        combination.getGroup(),
                        combination.getCode(),
                        loadedCombination.getLong(OCCURENCES_FIELD),
                        loadedCombination.getLong(FRAUD_OCCURENCES_FIELD)
                    )
                );
            });

        return loader;
    }

    public Future<Map<String, GroupTotalStats>> fetchTotalStats(long id) {
        System.out.println("GroupStatisticsStorage-FETCH-START" + id);

        Future loader = Future.future();

        Map<String, GroupTotalStats> resultMap = new HashMap<>();

        groupStatistics.find()
            .projection(Projections.include(NAME_FIELD, TOTALS_DOC))
            .forEach(
                document -> {
                    System.out.println("GroupStatisticsStorage-FETCH-ONE-STOP" + id);
                    String name = document.getString(NAME_FIELD);
                    Document totals = (Document) document.get(TOTALS_DOC);

                    resultMap.put(
                        name,
                        new GroupTotalStats(
                            totals.getDouble(AVERAGE_PROBABILITY_FIELD).floatValue(),
                            totals.getDouble(DEVIATION_PROBABILITY_FIELD).floatValue(),
                            totals.getLong(OCCURRENCES_SUM_FIELD),
                            totals.getLong(SQUARED_OCCURRENCES_SUM_FIELD)
                        )
                    );
                },
                (result, t) -> {
                    if (t != null) {
                        System.out.println(t.getMessage());
                        System.out.println("GroupStatisticsStorage-FETCH-FAIL" + id);
                        loader.fail(t);
                        return;
                    }

                    System.out.println("GroupStatisticsStorage-FETCH-STOP" + id);

                    loader.complete(resultMap);
                }
            );

        return loader;
    }

    /**
     * update(
     *  {
     *      name: "AMOUNT",
     *      combinations: { $not : { $elemMatch : { code: "AAAA" }}}
     *  },
     *  {
     *      $addToSet : { combinations: { code: "AAAA", occurences: 0, fraudOccurences: 0 }}
     *  }
     * )
     * @param combinations
     */
    public Future<Void> initCombinationsIfNotPresent(List<CombinationStatistics> combinations) {
        List<UpdateOneModel<Document>> updates = combinations
            .stream()
            .distinct()
            .map(combination ->
                new UpdateOneModel<Document>(
                    and(
                        eq(NAME_FIELD, combination.getGroup()),
                        not(elemMatch(COMBINATIONS_DOC, new Document(CODE_FIELD, combination.getCode())))
                    ),
                    new Document("$addToSet", new Document(COMBINATIONS_DOC, new Document(CODE_FIELD, combination.getCode())
                        .append(OCCURENCES_FIELD, 0l)
                        .append(FRAUD_OCCURENCES_FIELD, 0l)
                    ))
                )
            )
            .collect(Collectors.toList());

        Future<Void> loader = Future.future();

        groupStatistics.bulkWrite(updates, new BulkWriteOptions().ordered(false), ((result, t) -> {
            if (t != null) {
                t.printStackTrace();
                loader.fail(t);
                return;
            }

            loader.complete();
        }));

        return loader;
    }

    public void updateOccurences(List<CombinationStatistics> combinations) {
        List<UpdateOneModel<Document>> increments = combinations.stream()
            .map(it ->
                new UpdateOneModel<Document>(
                    and(eq(NAME_FIELD, it.getGroup()), eq(COMBINATIONS_DOC + "." + CODE_FIELD, it.getCode())),
                    new Document(
                        "$inc",
                        new Document(COMBINATIONS_DOC + ".$." + OCCURENCES_FIELD, it.getOccurences())
                            .append(COMBINATIONS_DOC + ".$." + FRAUD_OCCURENCES_FIELD, it.getFraudOccurences())
                    )
                )
            )
            .collect(Collectors.toList());

        groupStatistics.bulkWrite(increments, new BulkWriteOptions().ordered(false), (result, t) -> {
            if (t != null) {
                System.out.println(t.getMessage());
            }
        });
    }

    public Future<Void> initTotalsIfNotPresent() {
        List<UpdateOneModel<Document>> initDocuments = ImmutableList.of(
            buildInitQueryForGroup("AMOUNT"),
            buildInitQueryForGroup("COUNT"),
            buildInitQueryForGroup("TIME"),
            buildInitQueryForGroup("LOCATION")
        );


        Future<Void> loader = Future.future();
        groupStatistics.bulkWrite(initDocuments, new BulkWriteOptions().ordered(false), (result, t) -> {
            if (t != null) {
                System.out.println(t.getMessage());
                loader.fail(t);
                return;
            }

            loader.complete();
        });

        return loader;
    }

    public void updateTotals(Map<String, GroupTotalStats> totals) {
        List<UpdateOneModel<Document>> updates = new ArrayList<>();

        for (Map.Entry<String, GroupTotalStats> total : totals.entrySet()) {
            updates.add(
                new UpdateOneModel<Document>(
                    Filters.eq(NAME_FIELD, total.getKey()),
                    new Document("$inc",
                        new Document(TOTALS_DOC + "." + AVERAGE_PROBABILITY_FIELD, total.getValue().getAverageProbability())
                            .append(TOTALS_DOC + "." + DEVIATION_PROBABILITY_FIELD, total.getValue().getDeviationProbability())
                            .append(TOTALS_DOC + "." + OCCURRENCES_SUM_FIELD, total.getValue().getSumOfOccurencesInFraud())
                            .append(TOTALS_DOC + "." + SQUARED_OCCURRENCES_SUM_FIELD, total.getValue().getSumOfSquaredFraudOccurences())
                    )
                )
            );
        }

        groupStatistics.bulkWrite(updates, new BulkWriteOptions().ordered(false), ((result, t) -> {
            if (t != null) {
                System.out.println(t.getMessage());
            }
        }));
    }

    public void close() {
        client.close();
    }

    private UpdateOneModel<Document> buildInitQueryForGroup(String groupName) {
        return new UpdateOneModel<Document>(
            new Document(NAME_FIELD, groupName),
            new Document("$setOnInsert", new Document(NAME_FIELD, groupName)
                .append(COMBINATIONS_DOC, new ArrayList<>())
                .append(TOTALS_DOC,
                    new Document(AVERAGE_PROBABILITY_FIELD, 0f)
                        .append(DEVIATION_PROBABILITY_FIELD, 0f)
                        .append(OCCURRENCES_SUM_FIELD, 0l)
                        .append(SQUARED_OCCURRENCES_SUM_FIELD, 0l)
                )
            ),
            new UpdateOptions().upsert(true)
        );
    }
}
