package org.ignas.frauddetection.probabilitystatistics.service;

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

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;

public class GroupStatisticsStorage {

    private MongoClient client;

    private MongoCollection<Document> groupStatistics;

    public GroupStatisticsStorage(String url, String database, String collection) {
        client = MongoClients.create(url);

        groupStatistics = client.getDatabase(database)
            .getCollection(collection);

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
            .map(combination ->
                new UpdateOneModel<Document>(
                    and(eq("name", combination.getGroup()), eq("combinations.code", combination.getGroup())),
                    new Document("$addToSet", new Document("combinations", new Document("code", combination.getCode())
                        .append("occurences", 0)
                        .append("fraudOccurences", 0)
                    ))
                )
            )
            .collect(Collectors.toList());

        Future<Void> loader = Future.future();

        groupStatistics.bulkWrite(updates, new BulkWriteOptions().ordered(false), ((result, t) -> {
            if (t != null) {
                loader.fail(t);
            }

            loader.complete();
        }));

        return loader;
    }


    public Future<CombinationStatistics> fetch(CombinationStatistics combination) {
        Future<CombinationStatistics> loader = Future.future();

        groupStatistics.find(and(eq("name", combination.getGroup()), eq("combinations.code", combination.getCode())))
            .projection(Projections.include("name", "combinations.$"))
            .first((result, t) -> {
                if (t != null) {
                    loader.fail(t);
                }

                loader.complete(
                    new CombinationStatistics(
                        combination.getGroup(),
                        combination.getCode(),
                        result.getLong("combinations.occurences"),
                        result.getLong("combinations.fraudOccurences")
                    )
                );
            });

        return loader;
    }

    public Future<Map<String, GroupTotalStats>> loadTotals() {
        Future loader = Future.future();

        Map<String, GroupTotalStats> resultMap = new HashMap<>();

        groupStatistics.find()
            .projection(Projections.include("name", "totals"))
            .forEach(
                document -> resultMap.put(
                    document.getString("name"),
                    new GroupTotalStats(
                        document.getLong("averageProbability"),
                        document.getLong("deviationProbability"),
                        document.getLong("sumOfOccurancesInFraud"),
                        document.getLong("sumOfSquaredOccurrencesInFraud")
                    )
                ),
                (result, t) -> loader.complete(resultMap)
            );

        return loader;
    }

    public void close() {
        client.close();
    }


    public Future<Void> initGroupsIfNotPresent() {
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
            }

            loader.complete();
        });

        return loader;
    }


    public void updateOccurences(List<CombinationStatistics> combinations) {
        List<UpdateOneModel<Document>> increments = combinations.stream()
            .map(it ->
                new UpdateOneModel<Document>(
                    and(eq("name", it.getGroup()), eq("combinations.code", it.getCode())),
                    new Document("$inc", new Document("combinations.$.occurences", it.getOccurences())
                        .append("combinations.$.fraudOccurences", it.getFraudOccurences()))
                )
            )
            .collect(Collectors.toList());

        groupStatistics.bulkWrite(increments, new BulkWriteOptions().ordered(false), (result, t) -> {
            if (t != null) {
                System.out.println(t.getMessage());
            }
        });
    }

    public void updateTotals(Map<String, GroupTotalStats> totals) {
        List<UpdateOneModel<Document>> updates = new ArrayList<>();

        for (Map.Entry<String, GroupTotalStats> total : totals.entrySet()) {
            updates.add(
                new UpdateOneModel<Document>(
                    Filters.eq("name", total.getKey()),
                    new Document("$inc", new Document("averageProbability", total.getValue().getAverageProbability())
                        .append("deviationProbability", total.getValue().getDeviationProbability())
                        .append("sumOfOccurrencesInFraud", total.getValue().getSumOfOccurencesInFraud())
                        .append("sumOfSquaredOccurrencesInFraud", total.getValue().getSumOfSquaredFraudOccurences())
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

    private UpdateOneModel<Document> buildInitQueryForGroup(String groupName) {
        return new UpdateOneModel<Document>(
            new Document("name", groupName),
            new Document("$setOnInsert", new Document("name", groupName)
                .append("combinations", new ArrayList<>())
                .append("totals",
                    new Document("averageProbability", 0)
                        .append("deviationProbability", 0)
                        .append("sumOfOccurrencesInFraud", 0)
                        .append("sumOfSquaredOccurrencesInFraud", 0)
                )
            ),
            new UpdateOptions().upsert(true)
        );
    }
}
