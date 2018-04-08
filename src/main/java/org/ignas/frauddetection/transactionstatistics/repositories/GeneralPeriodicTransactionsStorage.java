package org.ignas.frauddetection.transactionstatistics.repositories;

import com.mongodb.async.client.MongoClient;
import com.mongodb.async.client.MongoClients;
import com.mongodb.async.client.MongoCollection;
import com.mongodb.client.model.BulkWriteOptions;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOneModel;
import com.mongodb.client.model.UpdateOptions;
import io.swagger.models.auth.In;
import io.vertx.core.Future;
import org.bson.Document;
import org.ignas.frauddetection.transactionstatistics.domain.PeriodIncrement;

import javax.print.Doc;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.mongodb.client.model.Filters.*;

/**
 * Init missing periods:
 * > db.transactions.update({ start: '2017-01-08', end: '2017-01-14' }, { $setOnInsert: { start: '2017-01-01', end: '2017-01-07', debtors: [] }}, {upsert: true})
 *
 * Add missing debtors:
 * > db.transactions.update({ start: '2017-01-01', end: '2017-01-07', debtors : { $not : { $elemMatch : { id: '123'} }} }, { $addToSet: { debtors: { "id" : '123', amounts: [] } }})
 *  (Should increase instances)
 *
 * Add missing amounts:
 * > db.transactions.update({ start: '2017-01-01', end: '2017-01-07', 'debtors.id' : '123'}, { $push: { 'debtors.$.amounts': 123.46 }})
 *   (should not increase instances)
 */
public class GeneralPeriodicTransactionsStorage {

    private static final String INSTANCES_FIELD = "instances";
    private static final String SQUARED_DISTANCE_COMMON = "sumOfSquaredDistanceFromComm";
    private static final String DISTANCE_COMMON = "sumOfDistanceFromComm";
    private static final String SQUARED_DISTANCE_LAST = "sumOfSquaredDistanceFromLast";
    private static final String DISTANCE_LAST = "sumOfDistanceFromLast";
    private static final String SQUARED_TIME_DIFFERENCE = "sumOfSquaredTimeDiffFromLast";
    private static final String TIME_DIFFERENCE = "sumOfTimeDiffFromLast";

    private MongoClient client;

    private final MongoCollection<Document> dailyArchive;
    private final MongoCollection<Document> weeklyArchive;
    private final MongoCollection<Document> monthlyArchive;

    private final MongoCollection<Document> periodTotals;

    public GeneralPeriodicTransactionsStorage(String url, String database) {
        client = MongoClients.create(url);

        dailyArchive = client.getDatabase(database).getCollection("dailyArchive");
        weeklyArchive = client.getDatabase(database).getCollection("weeklyArchive");
        monthlyArchive = client.getDatabase(database).getCollection("monthlyArchive");

        periodTotals = client.getDatabase(database).getCollection("periodTotals");
    }

    public void close() {
        client.close();
    }

    /**
     * db.periodTotals.update({ length: 1 }, { $inc : { sum: 5, count: 1, instances: 1 } }, { upsert: true } )
     */
    public void updateTotals(
        int newDailyInstances, int newWeeklyInstances, int newMonthlyInstances,
        float sumIncrement, float countIncrement,
        float dailyRatioIncrement, float weeklyRatioIncrement, float monthlyRatioIncrement) {

        List<UpdateOneModel<Document>> increments = new ArrayList<>();
        increments.add(
            buildTotalUpdateForPeriod("day", newDailyInstances, sumIncrement, countIncrement, dailyRatioIncrement)
        );
        increments.add(
            buildTotalUpdateForPeriod("week", newWeeklyInstances, sumIncrement, countIncrement, weeklyRatioIncrement)
        );
        increments.add(
            buildTotalUpdateForPeriod("month", newMonthlyInstances, sumIncrement, countIncrement, monthlyRatioIncrement)
        );

        periodTotals.bulkWrite(increments, new BulkWriteOptions().ordered(false), (result, t) -> {
            if (t != null) {
                t.printStackTrace();
            }
        });
    }

    private UpdateOneModel<Document> buildTotalUpdateForPeriod(
        String period,
        int newDailyInstances,
        float sumIncrement,
        float countIncrement,
        float ratioIncrement) {

        Document increment = new Document("instances", newDailyInstances)
            .append("sum", sumIncrement)
            .append("count", countIncrement)
            .append("ration", ratioIncrement);

        return new UpdateOneModel<Document>(
            Filters.eq("length", period),
            new Document("$inc", increment),
            new UpdateOptions().upsert(true)
        );
    }

    public Future<Integer> updateDaily(List<PeriodIncrement> increments) {
        return update(dailyArchive, increments);
    }

    public Future<Integer> updateWeekly(List<PeriodIncrement> increments) {
        return update(weeklyArchive, increments);
    }

    public Future<Integer> updateMonthly(List<PeriodIncrement> increments) {
        return update(weeklyArchive, increments);
    }

    private Future<Integer> update(MongoCollection<Document> collection, List<PeriodIncrement> increments) {
        List<UpdateOneModel<Document>> periodCreations = increments.stream()
            .distinct()
            .map(this::buildInitPeriodQuery)
            .collect(Collectors.toList());

        List<UpdateOneModel<Document>> debtorCreations = increments.stream()
            .distinct()
            .map(this::buildInitDebtorQuery)
            .collect(Collectors.toList());

        List<UpdateOneModel<Document>> debtorAdditions = increments.stream()
            .distinct()
            .map(this::buildAddAmountQuery)
            .collect(Collectors.toList());

        io.vertx.core.Future<Integer> future = io.vertx.core.Future.future();
        collection.bulkWrite(periodCreations, new BulkWriteOptions().ordered(false), (periodResult, periodEx) -> {
            if (periodEx != null) {
                periodEx.printStackTrace();
                return;
            }

            collection.bulkWrite(debtorCreations, new BulkWriteOptions().ordered(false), (debtorResult, debtorEx) -> {
                if (debtorEx != null) {
                    debtorEx.printStackTrace();
                    return;
                }

                // Returns count of new periods inserted
                future.complete(debtorResult.getInsertedCount());

                collection.bulkWrite(debtorAdditions, new BulkWriteOptions().ordered(false), (additionsResult, additionsEx) -> {
                    if (debtorEx != null) {
                        debtorEx.printStackTrace();
                        return;
                    }

                });
            });
        });

        return future;
    }

    private UpdateOneModel<Document> buildInitPeriodQuery(PeriodIncrement it) {
        return new UpdateOneModel<Document>(
            and(eq("start", it.getStart()), eq("end", it.getEnd())),
            new Document("$setOnInsert", new Document("start", it.getStart()).append("end", it.getEnd()).append("debtors", new ArrayList<>())),
            new UpdateOptions().upsert(true)
        );
    }

    private UpdateOneModel<Document> buildInitDebtorQuery(PeriodIncrement it) {
        return new UpdateOneModel<Document>(
            and(
                eq("start", it.getStart()),
                eq("end", it.getEnd()),
                not(elemMatch("debtors", eq("id", it.getDebtor())))
            ),
            new Document("$addToSet", new Document("debtors", new Document("id", it.getDebtor()).append("amounts", new ArrayList<>())))
        );
    }

    private UpdateOneModel<Document> buildAddAmountQuery(PeriodIncrement it) {
        return new UpdateOneModel<Document>(
            and(
                eq("start", it.getStart()),
                eq("end", it.getEnd()),
                eq("debtors.id", it.getDebtor())
            ),
            new Document("$push", new Document("debtors.$.amounts", it.getAmount()))
        );
    }


}
