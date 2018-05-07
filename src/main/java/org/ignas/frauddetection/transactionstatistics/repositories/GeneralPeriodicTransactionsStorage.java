package org.ignas.frauddetection.transactionstatistics.repositories;

import com.google.common.collect.ImmutableList;
import com.mongodb.async.client.MongoClient;
import com.mongodb.async.client.MongoClients;
import com.mongodb.async.client.MongoCollection;
import com.mongodb.client.model.*;
import io.vertx.core.Future;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.ignas.frauddetection.DetectionLauncher;
import org.ignas.frauddetection.transactionstatistics.domain.*;
import org.joda.time.LocalDateTime;
import org.joda.time.Seconds;

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

    public static final String INSTANCES_FIELD = "instances";

    public static final String DEBTORS_OBJECT = "debtors";
    public static final String ID_FIELD = "id";
    public static final String AMOUNTS_FIELD = "amounts";
    public static final String TYPE_FIELD = "type";

    public static final String SUM_TYPE = "SUM";
    public static final String COUNT_TYPE = "COUNT";
    public static final String RATIO_TYPE = "RATIO";

    public static final String VALUE_SUM_FIELD = "valueSum";
    public static final String VALUE_SQUARED_SUM_FIELD = "valueSquaredSum";

    private MongoClient client;
    private MongoClient learningClient;

    private final MongoCollection<Document> dailyArchive;
    private final MongoCollection<Document> weeklyArchive;
    private final MongoCollection<Document> monthlyArchive;

    private final MongoCollection<Document> periodTotals;

    private PeriodicGeneralStats CACHE = null;
    private LocalDateTime CACHED_AT = null;


    public GeneralPeriodicTransactionsStorage(String database) {
        learningClient = MongoClients.create(DetectionLauncher.PERIODIC_TRANSACTIONS_DB_SETTINGS);

        dailyArchive = learningClient.getDatabase(database).getCollection("dailyArchive");
        weeklyArchive = learningClient.getDatabase(database).getCollection("weeklyArchive");
        monthlyArchive = learningClient.getDatabase(database).getCollection("monthlyArchive");

        client = MongoClients.create(DetectionLauncher.TRANSACTIONS_MONGODB_SETTINGS);

        periodTotals = client.getDatabase(database).getCollection("periodTotals");
    }

    public void close() {
        learningClient.close();
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
            .add(buildTotalUpdateForPeriod(DAY_PERIOD, SUM_TYPE, newDailyInstances, dailySum))
            .add(buildTotalUpdateForPeriod(WEEK_PERIOD, SUM_TYPE, newWeeklyInstances, weeklySum))
            .add(buildTotalUpdateForPeriod(MONTH_PERIOD, SUM_TYPE, newMonthlyInstances, monthlySum))
            .add(buildTotalUpdateForPeriod(DAY_PERIOD, COUNT_TYPE, newDailyInstances, dailyCount))
            .add(buildTotalUpdateForPeriod(WEEK_PERIOD, COUNT_TYPE, newWeeklyInstances, weeklyCount))
            .add(buildTotalUpdateForPeriod(MONTH_PERIOD, COUNT_TYPE, newMonthlyInstances, monthlyCount))
            .add(buildTotalUpdateForPeriod(DAY_PERIOD, RATIO_TYPE, totalInstances, dailyRatio))
            .add(buildTotalUpdateForPeriod(WEEK_PERIOD, RATIO_TYPE, totalInstances, weeklyRatio))
            .add(buildTotalUpdateForPeriod(MONTH_PERIOD, RATIO_TYPE, totalInstances, monthlyRatio))
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
        if (CACHE != null && CACHED_AT != null
            && Seconds.secondsBetween(LocalDateTime.now(), CACHED_AT).getSeconds() * 1000 <= DetectionLauncher.CACHE_TTL) {
            return Future.succeededFuture(CACHE);
        }

        Future<PeriodicGeneralStats> future = Future.future();

        PeriodicGeneralStats generalStats = new PeriodicGeneralStats();

        periodTotals.find()
            .forEach(document -> {

                PeriodStats stats = new PeriodStats(
                    document.getDouble(VALUE_SUM_FIELD).floatValue(),
                    document.getDouble(VALUE_SQUARED_SUM_FIELD).floatValue(),
                    document.getLong(INSTANCES_FIELD)
                );

                String length = document.getString(LENGTH_FIELD);
                String type = document.getString(TYPE_FIELD);

                generalStats.add(type, length, stats);

                },
                (result, t) -> {
                    if (t != null) {
                        t.printStackTrace();
                        future.fail(t);
                        return;
                    }

                    CACHE = generalStats;
                    CACHED_AT = LocalDateTime.now();
                    future.complete(generalStats);
            });

        return future;
    }

    private Future<Integer> update(MongoCollection<Document> collection, List<PeriodIncrement> increments) {
        List<UpdateOneModel<Document>> periodCreations = increments.stream()
            .distinct()
            .map(this::buildInitPeriodQuery)
            .collect(Collectors.toList());

        List<PeriodIncrement> newDebtorRequiringIncrements = new ArrayList<>();

        List<UpdateOneModel<Document>> debtorCreations = increments.stream()
            .filter(it -> newDebtorRequiringIncrements.stream()
                .noneMatch(existing ->
                    existing.getDebtor().equals(it.getDebtor())
                        && existing.getStart().equals(it.getStart())
                        && existing.getEnd().equals(it.getEnd())
                )
            )
            .peek(newDebtorRequiringIncrements::add)
            .map(this::buildInitDebtorQuery)
            .collect(Collectors.toList());

        List<UpdateOneModel<Document>> debtorAdditions = increments.stream()
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
                future.complete(debtorResult.getModifiedCount());

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
        Document emptyDoc = new Document(START_FIELD, it.getStart().toString())
            .append(END_FIELD, it.getEnd().toString())
            .append(DEBTORS_OBJECT, new ArrayList<>());

        return new UpdateOneModel<Document>(
            and(eq(START_FIELD, it.getStart().toString()), eq(END_FIELD, it.getEnd().toString())),
            new Document("$setOnInsert", emptyDoc),
            new UpdateOptions().upsert(true)
        );
    }

    private UpdateOneModel<Document> buildInitDebtorQuery(PeriodIncrement it) {
        Document emptyDebtor = new Document(DEBTORS_OBJECT, new Document(ID_FIELD, it.getDebtor())
            .append(AMOUNTS_FIELD, new ArrayList<>()));

        return new UpdateOneModel<Document>(
            and(
                eq(START_FIELD, it.getStart().toString()),
                eq(END_FIELD, it.getEnd().toString()),
                not(elemMatch(DEBTORS_OBJECT, eq(ID_FIELD, it.getDebtor())))
            ),
            new Document("$addToSet", emptyDebtor)
        );
    }

    private UpdateOneModel<Document> buildAddAmountQuery(PeriodIncrement it) {
        return new UpdateOneModel<Document>(
            and(
                eq(START_FIELD, it.getStart().toString()),
                eq(END_FIELD, it.getEnd().toString()),
                eq(DEBTORS_ID_FIELD, it.getDebtor())
            ),
            new Document("$push", new Document("debtors.$.amounts", it.getAmount()))
        );
    }


    private Future<List<DebtorPeriodValue>> fetchValuesBeforeIncrements(
        MongoCollection<Document> collection, List<PeriodIncrement> increments) {
        long start = System.currentTimeMillis();

        Future<List<DebtorPeriodValue>> future = Future.future();

        final List<PeriodIncrement> uniqueDebtorPeriods = new ArrayList<>();

        List<Bson> filters = increments.stream()
            .filter(it -> uniqueDebtorPeriods.stream()
                .noneMatch(existing ->
                    existing.getDebtor().equals(it.getDebtor())
                        && existing.getStart().equals(it.getStart())
                        && existing.getEnd().equals(it.getEnd())
                )
            )
            .peek(uniqueDebtorPeriods::add)
            .map(increment ->
                and(
                    eq(START_FIELD, increment.getStart().toString()),
                    eq(END_FIELD, increment.getEnd().toString()),
                    Filters.elemMatch(DEBTORS_OBJECT, Filters.eq("id", increment.getDebtor()))
                )
            )
            .collect(Collectors.toList());

        List<DebtorPeriodValue> oldValues = new ArrayList<>();

        collection.find(or(filters))
            .projection(Projections.include(START_FIELD, END_FIELD, DEBTORS_OBJECT))
            .forEach(document -> {
                List<DebtorPeriodValue> filteredValues = parseDebtorsFromPeriodDoc(document, uniqueDebtorPeriods);

                if (filteredValues.isEmpty()) {
                    return;
                }

                oldValues.addAll(filteredValues);

            }, (result, t) -> {
                if (t != null) {
                    t.printStackTrace();
                    return;
                }

                future.complete(oldValues);
            });

        return future;
    }

    private List<DebtorPeriodValue> parseDebtorsFromPeriodDoc(Document document, List<PeriodIncrement> uniqueDebtorPeriods) {
        List<DebtorPeriodValue> filteredValues = new ArrayList<>();

        LocalDateTime periodStart = LocalDateTime.parse(document.getString(START_FIELD));
        LocalDateTime periodEnd = LocalDateTime.parse(document.getString(END_FIELD));

        List<String> debtorsInPeriod = uniqueDebtorPeriods.stream()
            .filter(it -> it.forPeriod(periodStart, periodEnd))
            .map(it -> it.getDebtor())
            .collect(Collectors.toList());

        // https://jira.mongodb.org/browse/SERVER-20127 cannot use positional operator to fetch only required value
        List<Document> debtors = (List<Document>) document.get(DEBTORS_OBJECT);

        for(Document debtorDoc: debtors) {
            String debtorId = debtorDoc.getString("id");

            boolean requested = debtorsInPeriod.stream()
                .anyMatch(it -> it.equals(debtorId));

            if (!requested) {
                continue;
            }

            List<Double> amounts = (List<Double>) debtorDoc.get("amounts");
            Float sum = amounts.stream().reduce(0d, Double::sum).floatValue();

            filteredValues.add(new DebtorPeriodValue(
                periodStart,
                periodEnd,
                debtorId,
                sum,
                amounts.size()
            ));
        }
        return filteredValues;
    }

    private UpdateOneModel<Document> buildTotalUpdateForPeriod(
        String period,
        String type,
        int newInstances,
        TotalIncrement inc) {

        Document increment = new Document(INSTANCES_FIELD, (long) newInstances)
            .append(VALUE_SUM_FIELD, inc.getValueIncrease())
            .append(VALUE_SQUARED_SUM_FIELD, inc.getValueIncreaseSquared());

        return new UpdateOneModel<>(
            and(eq(LENGTH_FIELD, period), eq(TYPE_FIELD, type)),
            new Document("$inc", increment),
            new UpdateOptions().upsert(true)
        );
    }

}
