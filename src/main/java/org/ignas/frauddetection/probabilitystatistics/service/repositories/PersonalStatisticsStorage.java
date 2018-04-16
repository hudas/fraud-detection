package org.ignas.frauddetection.probabilitystatistics.service.repositories;

import com.mongodb.async.client.MongoClient;
import com.mongodb.async.client.MongoClients;
import com.mongodb.async.client.MongoCollection;
import com.mongodb.client.model.BulkWriteOptions;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOneModel;
import com.mongodb.client.model.UpdateOptions;
import io.vertx.core.Future;
import org.bson.Document;
import org.ignas.frauddetection.probabilitystatistics.domain.PersonalPeriodStats;
import org.ignas.frauddetection.probabilitystatistics.domain.PersonalPeriodTransaction;
import org.ignas.frauddetection.probabilitystatistics.domain.PersonalStats;
import org.ignas.frauddetection.probabilitystatistics.domain.PersonalTransactionStats;
import org.ignas.frauddetection.shared.Location;
import org.joda.time.LocalDateTime;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class PersonalStatisticsStorage {

    public static final String DEBTOR_FIELD = "debtor";
    public static final String LAST_TRANSACTION_OBJECT = "lastTransaction";
    public static final String LOCATION_OBJECT = "location";
    public static final String LONGTITUDE_FIELD = "longtitude";
    public static final String LATITUDE_FIELD = "latitude";
    public static final String TIME_FIELD = "time";
    public static final String MAX_AMOUNT_FIELD = "maxAmount";
    public static final String MIN_TIME_DIFF_FIELD = "minTimeDiff";
    public static final String LOCATIONS_OBJECT = "locationOccurences";
    public static final String AREA_CODE_FIELD = "areaCode";
    public static final String OCCURRENCES_FIELD = "occurrences";
    public static final String TIMES_OBJECT = "timeOccurrences";
    public static final String HOUR_FIELD = "hour";
    public static final String PERIODS_OBJECT = "periods";
    public static final String TRANSACTIONS_OBJECT = "transactions";
    public static final String TRANSACTION_TIME_FIELD = "time";
    public static final String TRANSACTION_AMOUNT_FIELD = "amount";
    public static final String LENGTH_FIELD = "length";
    public static final String COUNT_FIELD = "count";
    public static final String SUM_FIELD = "sum";
    private MongoClient client;

    private MongoCollection<Document> personalStatistics;

    public PersonalStatisticsStorage(String url, String database) {
        client = MongoClients.create(url);

        personalStatistics = client.getDatabase(database)
            .getCollection("personalStatistics");

    }


    public Future<PersonalStats> fetchPersonalStats(String debtor) {
        long start = System.currentTimeMillis();

        Future<PersonalStats> future = Future.future();

        personalStatistics.find(Filters.eq("debtor", debtor))
            .first((result, t) -> {
                if (t != null) {
                    t.printStackTrace();
                    future.fail(t);
                    return;
                }

                if (result == null) {
                    long end = System.currentTimeMillis();
                    System.out.println("PersonalStatisticsStorage.fetchPersonalStats took: " + (end - start));
                    future.complete(null);
                    return;
                }

                long end = System.currentTimeMillis();
                System.out.println("PersonalStatisticsStorage.initTotalsIfNotPresent took: " + (end - start));
                future.complete(buildFromDocument(result));
            });

        return future;
    }

    public void update(List<PersonalStats> updates) {
        List<String> debtorsToUpdate = updates.stream()
            .map(PersonalStats::getDebtor)
            .collect(Collectors.toList());

        List<PersonalStats> currentValues = new ArrayList<>();

        Future<List<PersonalStats>> valuesLoader = Future.future();

        personalStatistics.find(Filters.in("debtor", debtorsToUpdate))
            .forEach(document -> {
                currentValues.add(buildFromDocument(document));
            }, (result, t) -> {
                if (t != null) {
                    t.printStackTrace();
                    valuesLoader.fail(t);
                    return;
                }

                valuesLoader.complete(currentValues);
            });


        valuesLoader.setHandler(event -> {
           if (event.failed()) {
               throw new IllegalStateException(event.cause());
           }

            List<PersonalStats> valuesBeforeUpdate = event.result();
            List<PersonalStats> finalValues = new ArrayList<>();

            for(PersonalStats increment: updates) {
                PersonalStats currentValue = valuesBeforeUpdate.stream()
                    .filter(it -> it.getDebtor().equals(increment.getDebtor()))
                    .findAny()
                    .orElse(new PersonalStats(increment.getDebtor()));

                PersonalStats mergeResult = currentValue.mergeWith(increment);

                mergeResult.removeOutdatedTransactions(LocalDateTime.now());

                finalValues.add(mergeResult);
            }

            List<ReplaceOneModel<Document>> replacements = finalValues.stream()
                .map(this::buildReplacementObject)
                .collect(Collectors.toList());

            personalStatistics.bulkWrite(
                replacements,
                new BulkWriteOptions().ordered(false),
                (result, t) -> {
                    if (t != null) {
                        t.printStackTrace();
                        throw new IllegalStateException(t);
                    }
            });
        });
    }

    private ReplaceOneModel<Document> buildReplacementObject(PersonalStats newStats) {
        PersonalTransactionStats latest = newStats.getLatestTransaction();

        Location latestLocation = latest.getLocation();

        Document latestLocationDoc = new Document(LONGTITUDE_FIELD, latestLocation.getLongtitude())
            .append(LATITUDE_FIELD, latestLocation.getLatitude());

        Document lastTransactionDoc = new Document(LOCATION_OBJECT, latestLocationDoc)
            .append(TRANSACTION_AMOUNT_FIELD, latest.getAmount())
            .append(TIME_FIELD, latest.getTime().toString());

        List<Document> locationsOccurrencesDocs = newStats.getLocationOccurences()
            .entrySet()
            .stream()
            .map(it -> new Document(AREA_CODE_FIELD, it.getKey()).append(OCCURRENCES_FIELD, it.getValue()))
            .collect(Collectors.toList());

        List<Document> timesOccurrencesDocs = newStats.getTimeOccurences()
            .entrySet()
            .stream()
            .map(it -> new Document(HOUR_FIELD, it.getKey()).append(OCCURRENCES_FIELD, it.getValue()))
            .collect(Collectors.toList());

        List<Document> periodsDocs = newStats.getPeriods()
            .values()
            .stream()
            .map(it -> new Document(LENGTH_FIELD, it.getLength())
                .append(TRANSACTIONS_OBJECT, it.getTransactions()
                    .stream()
                    .map(trans -> new Document(TIME_FIELD, trans.getTime().toString())
                        .append(TRANSACTION_AMOUNT_FIELD, trans.getAmount())
                    )
                    .collect(Collectors.toList())
                )
                .append(COUNT_FIELD, it.getCount())
                .append(SUM_FIELD, it.getSum())
            )
            .collect(Collectors.toList());

        Document replacement = new Document(DEBTOR_FIELD, newStats.getDebtor())
            .append(LAST_TRANSACTION_OBJECT, lastTransactionDoc)
            .append(MAX_AMOUNT_FIELD, newStats.getMaxAmount())
            .append(MIN_TIME_DIFF_FIELD, newStats.getMinTimeDiff())
            .append(LOCATIONS_OBJECT, locationsOccurrencesDocs)
            .append(TIMES_OBJECT, timesOccurrencesDocs)
            .append(PERIODS_OBJECT, periodsDocs);

        return new ReplaceOneModel(
            Filters.eq(DEBTOR_FIELD, newStats.getDebtor()),
            replacement,
            new UpdateOptions().upsert(true)
        );
    }

    public void close() {
        client.close();
    }

    private PersonalStats buildFromDocument(Document document) {
        String debtor = document.getString(DEBTOR_FIELD);

        PersonalStats stats = new PersonalStats(debtor);

        PersonalTransactionStats lastTransaction = parseLastTransaction(document);

        Float maxAmount = document.getDouble(MAX_AMOUNT_FIELD).floatValue();
        Integer minTimeDifference = document.getLong(MIN_TIME_DIFF_FIELD).intValue();

        stats.setLatestTransaction(lastTransaction);
        stats.setMinTimeDiff(minTimeDifference.longValue());
        stats.setMaxAmount(maxAmount);

        addLocationCounts(document, stats);
        addTimeCounts(document, stats);
        stats.setPeriods(parsePeriodStats(document));

        return stats;
    }

    private PersonalTransactionStats parseLastTransaction(Document document) {
        Document lastDoc = (Document) document.get(LAST_TRANSACTION_OBJECT);

        Document locationDoc = (Document) lastDoc.get(LOCATION_OBJECT);

        Float longtitude = locationDoc.getDouble(LONGTITUDE_FIELD).floatValue();
        Float latitude = locationDoc.getDouble(LATITUDE_FIELD).floatValue();

        Location location = new Location(latitude, longtitude);

        Float amount = lastDoc.getDouble(TRANSACTION_AMOUNT_FIELD).floatValue();
        LocalDateTime time = LocalDateTime.parse(lastDoc.getString(TIME_FIELD));


        return new PersonalTransactionStats(
            location,
            amount,
            time
        );
    }

    private void addLocationCounts(Document document, PersonalStats stats) {
        List<Document> locationDocs = (List<Document>) document.get(LOCATIONS_OBJECT);

        for(Document locationCountDoc : locationDocs) {
            String areaCode = locationCountDoc.getString(AREA_CODE_FIELD);
            Long count = locationCountDoc.getLong(OCCURRENCES_FIELD);

            stats.incLocationCount(areaCode, count);
        }
    }

    private void addTimeCounts(Document document, PersonalStats stats) {
        List<Document> timeDocs = (List<Document>) document.get(TIMES_OBJECT);

        for(Document timeDoc : timeDocs) {
            Integer hour = timeDoc.getInteger(HOUR_FIELD);
            Long count = timeDoc.getLong(OCCURRENCES_FIELD);

            stats.incTimeCount(hour, count);
        }
    }

    private List<PersonalPeriodStats> parsePeriodStats(Document document) {
        List<PersonalPeriodStats> periods = new ArrayList<>();

        List<Document> periodDocs = (List<Document>) document.get(PERIODS_OBJECT);

        for (Document periodDoc : periodDocs) {
            Integer length = periodDoc.getInteger(LENGTH_FIELD);

            List<PersonalPeriodTransaction> periodTransactions = new ArrayList<>();

            List<Document> transactionDocs = (List<Document>) periodDoc.get(TRANSACTIONS_OBJECT);

            if (transactionDocs != null) {
                for (Document transactionDoc: transactionDocs) {
                    LocalDateTime transactionTime = LocalDateTime.parse(transactionDoc.getString(TRANSACTION_TIME_FIELD));
                    Float transactionValue = transactionDoc.getDouble(TRANSACTION_AMOUNT_FIELD).floatValue();

                    periodTransactions.add(new PersonalPeriodTransaction(transactionTime, transactionValue));
                }
            }

            Integer transactionCount = periodDoc.getInteger(COUNT_FIELD);
            Float transactionSum = periodDoc.getDouble(SUM_FIELD).floatValue();

            periods.add(new PersonalPeriodStats(length, transactionCount, transactionSum, periodTransactions));
        }
        return periods;
    }
}
