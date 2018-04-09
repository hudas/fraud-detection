package org.ignas.frauddetection.transactionstatistics.repositories;

import com.google.common.collect.ImmutableList;
import com.mongodb.async.client.MongoClient;
import com.mongodb.async.client.MongoClients;
import com.mongodb.async.client.MongoCollection;
import com.mongodb.client.model.*;
import io.vertx.core.Future;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.ignas.frauddetection.transactionstatistics.domain.*;
import org.joda.time.LocalDateTime;

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

    public static final String START_FIELD = "start";
    public static final String END_FIELD = "end";
    public static final String DEBTORS_ID_FIELD = "debtors.id";
    public static final String DAY_PERIOD = "day";
    public static final String WEEK_PERIOD = "week";
    public static final String MONTH_PERIOD = "month";
    public static final String LENGTH_FIELD = "length";

    public static final String RATIO_FIELD = "ratio";
    public static final String RATIO_SQUARED_FIELD = "ratio";

    public static final String COUNT_FIELD = "count";
    public static final String COUNT_SQUARED_FIELD = "countSquared";

    public static final String SUM_FIELD = "sum";
    public static final String SUM_SQUARED_FIELD = "sumSquared";

    public static final String INSTANCES_FIELD = "instances";

    public static final String DEBTORS_OBJECT = "debtors";
    public static final String ID_FIELD = "id";
    public static final String AMOUNTS_FIELD = "amounts";
    public static final String TYPE_FIELD = "type";
    public static final String SUM_TYPE = "SUM";
    public static final String COUNT_TYPE = "COUNT";
    public static final String RATIO_TYPE = "RATIO";
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
     * db.periodTotals.update({ length: 1, type: 'SUM' }, { $inc : { valueSum: 5, squaredValueSum: 1, instances: 1 } }, { upsert: true } )
     */
    public void updateTotals(
        int newDailyInstances,
        int newWeeklyInstances,
        int newMonthlyInstances,
        int totalInstances,
        TotalIncrement dailySum,
        TotalIncrement weeklySum,
        TotalIncrement monthlySum,
        TotalIncrement dailyCount,
        TotalIncrement weeklyCount,
        TotalIncrement monthlyCount,
        TotalIncrement dailyRatio,
        TotalIncrement weeklyRatio,
        TotalIncrement monthlyRatio) {

        List<UpdateOneModel<Document>> increments = ImmutableList.<UpdateOneModel<Document>>builder()
            .add(buildTotalUpdateForPeriod(DAY_PERIOD, SUM_TYPE, SUM_FIELD, SUM_SQUARED_FIELD, newDailyInstances, dailySum))
            .add(buildTotalUpdateForPeriod(WEEK_PERIOD, SUM_TYPE, SUM_FIELD, SUM_SQUARED_FIELD, newWeeklyInstances, weeklySum))
            .add(buildTotalUpdateForPeriod(MONTH_PERIOD, SUM_TYPE, SUM_FIELD, SUM_SQUARED_FIELD, newMonthlyInstances, monthlySum))
            .add(buildTotalUpdateForPeriod(DAY_PERIOD, COUNT_TYPE, COUNT_FIELD, COUNT_SQUARED_FIELD, newDailyInstances, dailyCount))
            .add(buildTotalUpdateForPeriod(WEEK_PERIOD, COUNT_TYPE, COUNT_FIELD, COUNT_SQUARED_FIELD, newWeeklyInstances, weeklyCount))
            .add(buildTotalUpdateForPeriod(MONTH_PERIOD, COUNT_TYPE, COUNT_FIELD, COUNT_SQUARED_FIELD, newMonthlyInstances, monthlyCount))
            .add(buildTotalUpdateForPeriod(DAY_PERIOD, RATIO_TYPE, RATIO_FIELD, RATIO_SQUARED_FIELD, totalInstances, dailyRatio))
            .add(buildTotalUpdateForPeriod(WEEK_PERIOD, RATIO_TYPE, RATIO_FIELD, RATIO_SQUARED_FIELD, totalInstances, weeklyRatio))
            .add(buildTotalUpdateForPeriod(MONTH_PERIOD, RATIO_TYPE, RATIO_FIELD, RATIO_SQUARED_FIELD, totalInstances, monthlyRatio))
            .build();

        periodTotals.bulkWrite(increments, new BulkWriteOptions().ordered(false), (result, t) -> {
            if (t != null) {
                t.printStackTrace();
            }
        });
    }

    public Future<Integer> updateDaily(List<PeriodIncrement> increments) {
        return update(dailyArchive, increments);
    }

    public Future<Integer> updateWeekly(List<PeriodIncrement> increments) {
        return update(weeklyArchive, increments);
    }

    public Future<Integer> updateMonthly(List<PeriodIncrement> increments) {
        return update(monthlyArchive, increments);
    }

    public Future<List<DebtorPeriodValue>> fetchOldDaily(List<PeriodIncrement> increments) {
        return fetchValuesBeforeIncrements(dailyArchive, increments);
    }

    public Future<List<DebtorPeriodValue>> fetchOldWeekly(List<PeriodIncrement> increments) {
        return fetchValuesBeforeIncrements(weeklyArchive, increments);
    }

    public Future<List<DebtorPeriodValue>> fetchOldMonthly(List<PeriodIncrement> increments) {
        return fetchValuesBeforeIncrements(monthlyArchive, increments);
    }

    public Future<PeriodicGeneralStats> fetchPeriodicStats() {
        Future<PeriodicGeneralStats> future = Future.future();

        PeriodicGeneralStats generalStats = new PeriodicGeneralStats();

        periodTotals.find()
            .forEach(document -> {

                PeriodStats stats = new PeriodStats(
                    document.getDouble(SUM_SQUARED_FIELD).floatValue(),
                    document.getDouble(COUNT_FIELD).floatValue(),
                    document.getDouble(COUNT_SQUARED_FIELD).floatValue(),
                    document.getDouble(SUM_FIELD).floatValue(),
                    document.getDouble(RATIO_FIELD).floatValue(),
                    document.getDouble(RATIO_SQUARED_FIELD).floatValue(),
                    document.getLong(INSTANCES_FIELD)
                );

                String length = document.getString(LENGTH_FIELD);

                if (length.equals(DAY_PERIOD)) {
                    generalStats.setDaily(stats);
                } else if (length.equals(WEEK_PERIOD)) {
                    generalStats.setWeekly(stats);
                } else if (length.equals(MONTH_PERIOD)) {
                    generalStats.setMonthly(stats);
                }
                },
                (result, t) -> {
                if (t != null) {
                    t.printStackTrace();
                    future.fail(t);
                    return;
                }

                future.complete(generalStats);
            });

        return future;
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
        Document emptyDoc = new Document(START_FIELD, it.getStart())
            .append(END_FIELD, it.getEnd())
            .append(DEBTORS_OBJECT, new ArrayList<>());

        return new UpdateOneModel<Document>(
            and(eq(START_FIELD, it.getStart()), eq(END_FIELD, it.getEnd())),
            new Document("$setOnInsert", emptyDoc),
            new UpdateOptions().upsert(true)
        );
    }

    private UpdateOneModel<Document> buildInitDebtorQuery(PeriodIncrement it) {
        Document emptyDebtor = new Document(DEBTORS_OBJECT, new Document(ID_FIELD, it.getDebtor())
            .append(AMOUNTS_FIELD, new ArrayList<>()));

        return new UpdateOneModel<Document>(
            and(
                eq(START_FIELD, it.getStart()),
                eq(END_FIELD, it.getEnd()),
                not(elemMatch(DEBTORS_OBJECT, eq(ID_FIELD, it.getDebtor())))
            ),
            new Document("$addToSet", emptyDebtor)
        );
    }

    private UpdateOneModel<Document> buildAddAmountQuery(PeriodIncrement it) {
        return new UpdateOneModel<Document>(
            and(
                eq(START_FIELD, it.getStart()),
                eq(END_FIELD, it.getEnd()),
                eq(DEBTORS_ID_FIELD, it.getDebtor())
            ),
            new Document("$push", new Document("debtors.$.amounts", it.getAmount()))
        );
    }


    private Future<List<DebtorPeriodValue>> fetchValuesBeforeIncrements(
        MongoCollection<Document> collection, List<PeriodIncrement> increments) {
        Future<List<DebtorPeriodValue>> future = Future.future();

        List<Bson> filters = increments.stream()
            .map(increment ->
                and(
                    eq(START_FIELD, increment.getStart()),
                    eq(END_FIELD, increment.getEnd()),
                    eq(DEBTORS_ID_FIELD, increment.getDebtor())
                )
            )
            .collect(Collectors.toList());

        List<DebtorPeriodValue> oldValues = new ArrayList<>();


        collection.find(or(filters))
            .projection(Projections.include(START_FIELD, END_FIELD, "debtors.$"))
            .forEach(document -> {
                List<Float> amounts = (List<Float>) document.get("debtors.amounts");
                Float sum = amounts.stream().reduce(0f, Float::sum);

                oldValues.add(new DebtorPeriodValue(
                    LocalDateTime.fromDateFields(document.getDate(START_FIELD)),
                    LocalDateTime.fromDateFields(document.getDate(END_FIELD)),
                    document.getString("debtors.id"),
                    sum,
                    amounts.size()
                ));

            }, (result, t) -> {
                if (t != null) {
                    t.printStackTrace();
                    return;
                }

                future.complete(oldValues);
            });

        return future;
    }

    private UpdateOneModel<Document> buildTotalUpdateForPeriod(
        String period,
        String type,
        String field,
        String squaredField,
        int newInstances,
        TotalIncrement inc) {

        Document increment = new Document(INSTANCES_FIELD, newInstances)
            .append(field, inc.getValueIncrease())
            .append(squaredField, inc.getValueIncreaseSquared());

        return new UpdateOneModel<>(
            and(eq(LENGTH_FIELD, period), eq(TYPE_FIELD, type)),
            new Document("$inc", increment),
            new UpdateOptions().upsert(true)
        );
    }

}
