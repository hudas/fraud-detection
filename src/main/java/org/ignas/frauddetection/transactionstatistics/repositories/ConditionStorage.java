package org.ignas.frauddetection.transactionstatistics.repositories;

import com.google.common.collect.ImmutableList;
import com.mongodb.async.client.MongoClient;
import com.mongodb.async.client.MongoClients;
import com.mongodb.async.client.MongoCollection;
import com.mongodb.client.model.*;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import org.bson.Document;
import org.ignas.frauddetection.shared.Location;
import org.ignas.frauddetection.transactionstatistics.domain.*;

import javax.print.Doc;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.google.common.collect.Iterables.concat;
import static com.google.common.collect.Lists.newArrayList;
import static com.mongodb.client.model.Filters.*;
import static java.lang.String.join;

public class ConditionStorage {

    private static final String ID_FIELD = "id";

    private static final String TOTAL_OCCURRENCES_FIELD = "totalOccurrences";
    private static final String FRAUD_OCCURRENCES_FIELD = "fraudOccurrences";
    public static final String TYPE_FIELD = "type";
    public static final String CREDITOR_TYPE = "CREDITOR";
    public static final String INSTANCES_COUNT_FIELD = "instances";
    public static final String PROBABILITIES_TOTAL_FIELD = "sumOfProbabilities";
    public static final String SQUARED_PROBABILITIES_TOTAL_FIELD = "sumOfSquaredProbabilities";
    public static final String TIME_TYPE = "TIME";
    public static final String LOCATION_TYPE = "LOCATION";

    private MongoClient client;

    private MongoCollection<Document> creditorsConditions;
    private MongoCollection<Document> timeConditions;
    private MongoCollection<Document> locationConditions;

    private MongoCollection<Document> conditionTotals;

    public void stop() {
        client.close();
    }

    public ConditionStorage(String url, String database) {
        client = MongoClients.create(url);

        creditorsConditions = client.getDatabase(database)
            .getCollection("creditorsConditions");
        timeConditions = client.getDatabase(database)
            .getCollection("timeConditions");
        locationConditions = client.getDatabase(database)
            .getCollection("locationConditions");

        conditionTotals = client.getDatabase(database)
            .getCollection("conditionTotals");
    }

    public Future<ExternalConditions> fetchOccurrences(String creditor, org.joda.time.LocalDateTime hour, Location location) {
        long start = System.currentTimeMillis();

        Future<ConditionOccurrences> creditorLoader = Future.future();
        creditorsConditions.find(eq(ID_FIELD, creditor))
            .first((result, t) -> {
                if (t != null) {
                    t.printStackTrace();
                    return;
                }

                if (result == null) {
                    creditorLoader.complete(ConditionOccurrences.empty(creditor));
                    return;
                }

                creditorLoader.complete(buildOccurrencesFromDocument(result));
            });

        Future<ConditionOccurrences> timeLoader = Future.future();
        timeConditions.find(eq(ID_FIELD, hour.getHourOfDay()))
            .first((result, t) -> {
                if (t != null) {
                    t.printStackTrace();
                    return;
                }

                if (result == null) {
                    timeLoader.complete(ConditionOccurrences.empty(hour.getHourOfDay()));
                    return;
                }

                timeLoader.complete(buildOccurrencesFromDocument(result));
            });

        Future<ConditionOccurrences> locationLoader = Future.future();
        locationConditions.find(eq(ID_FIELD, location.toString()))
            .first((result, t) -> {
                if (t != null) {
                    t.printStackTrace();
                    return;
                }

                if (result == null) {
                    locationLoader.complete(ConditionOccurrences.empty(hour.getHourOfDay()));
                    return;
                }

                locationLoader.complete(buildOccurrencesFromDocument(result));
            });

        Future<ConditionTotals> totalsLoader = Future.future();
        ConditionTotals totals = new ConditionTotals();

        conditionTotals.find()
            .forEach(document -> {
                ConditionTotalValue value = new ConditionTotalValue(
                    document.getLong(INSTANCES_COUNT_FIELD),
                    document.getDouble(PROBABILITIES_TOTAL_FIELD).floatValue(),
                    document.getDouble(SQUARED_PROBABILITIES_TOTAL_FIELD).floatValue()
                );

                String type = document.getString(TYPE_FIELD);
                switch (type) {
                    case CREDITOR_TYPE:
                        totals.setCreditorTotal(value);
                        return;
                    case TIME_TYPE:
                        totals.setTimeTotal(value);
                        return;
                    case LOCATION_TYPE:
                        totals.setLocationTotal(value);
                        return;
                    default:
                        throw new IllegalArgumentException("Unknown type" + type);
                }
            }, (result, t) -> {
                if (t != null) {
                    t.printStackTrace();
                    totalsLoader.fail(t);
                    return;
                }

                if (result == null) {
                    totalsLoader.complete(ConditionTotals.unknown());
                    return;
                }

                totalsLoader.complete(totals);
            });


        Future<ExternalConditions> future = Future.future();

        CompositeFuture.all(creditorLoader, timeLoader, locationLoader, totalsLoader)
            .setHandler(allLoaded -> {
                if (allLoaded.failed()) {
                    allLoaded.cause().printStackTrace();
                    future.fail(allLoaded.cause());
                    return;
                }

                ConditionOccurrences<String> creditorOccurrences = allLoaded.result().resultAt(0);
                ConditionOccurrences<Integer> timeOccurrences = allLoaded.result().resultAt(1);
                ConditionOccurrences<String> locationOccurrences = allLoaded.result().resultAt(2);

                ConditionTotals totalStats = allLoaded.result().resultAt(3);

                ConditionStats creditorStats = buildStats(creditorOccurrences, totalStats.getCreditorTotal());
                ConditionStats timeStats = buildStats(timeOccurrences, totalStats.getTimeTotal());
                ConditionStats locationStats = buildStats(locationOccurrences, totalStats.getLocationTotal());

                long end = System.currentTimeMillis();
//                System.out.println("ConditionStorage.fetchOccurrences took: " + (end - start));
                future.complete(new ExternalConditions(creditorStats, timeStats, locationStats));
            });

        return future;
    }

    /**
     *  update({ id: "1234" }, { $inc : { "totalOccurrences" : 1, "fraudOccurrences" : 1 }}, {upsert:true})
     *
     * @param creditorOccurences
     * @param timeOccurences
     * @param locationOccurences
     */
    public void updateOccurrences(
        List<ConditionOccurrences<String>> creditorOccurences,
        List<ConditionOccurrences<Integer>> timeOccurences,
        List<ConditionOccurrences<Location>> locationOccurences) {

        List<UpdateOneModel<Document>> creditorIncrements = creditorOccurences.stream()
            .map(this::buildIncrement)
            .collect(Collectors.toList());

        List<UpdateOneModel<Document>> timeIncrements = timeOccurences.stream()
            .map(this::buildIncrement)
            .collect(Collectors.toList());

        List<UpdateOneModel<Document>> locationIncrements = locationOccurences.stream()
            .map(this::buildIncrement)
            .collect(Collectors.toList());

        List<String> ids = creditorOccurences.stream()
            .map(ConditionOccurrences::getName)
            .collect(Collectors.toList());

        List<String> hours = timeOccurences.stream()
            .map(ConditionOccurrences::getName)
            .collect(Collectors.toList());

        List<String> locations = locationOccurences.stream()
            .map(ConditionOccurrences::getName)
            .collect(Collectors.toList());

        Future<List<ConditionOccurrences<String>>> creditorLoader = loadPreviousOccurrences(creditorsConditions, ids);
        Future<List<ConditionOccurrences<String>>> timeLoader = loadPreviousOccurrences(timeConditions, hours);
        Future<List<ConditionOccurrences<String>>> locationLoader = loadPreviousOccurrences(locationConditions, locations);

        CompositeFuture.all(creditorLoader, timeLoader, locationLoader)
            .setHandler(previousValuesLoader -> {
                if (previousValuesLoader.failed()) {
                    previousValuesLoader.cause().printStackTrace();
                    return;
                }

                List<ConditionOccurrences<String>> creditorValues = previousValuesLoader.result().resultAt(0);
                List<ConditionOccurrences<String>> timeValues = previousValuesLoader.result().resultAt(1);
                List<ConditionOccurrences<String>> locationValues = previousValuesLoader.result().resultAt(2);

                ConditionTotalIncrement creditor = buildIncrement(creditorOccurences, creditorValues);
                ConditionTotalIncrement time = buildIncrement(timeOccurences, timeValues);
                ConditionTotalIncrement location = buildIncrement(locationOccurences, locationValues);

                List<UpdateOneModel<Document>> totalUpdates = ImmutableList.<UpdateOneModel<Document>>builder()
                    .add(buildTotalIncrease(creditor, CREDITOR_TYPE))
                    .add(buildTotalIncrease(time, TIME_TYPE))
                    .add(buildTotalIncrease(location, LOCATION_TYPE))
                    .build();


                creditorsConditions.bulkWrite(
                    creditorIncrements,
                    new BulkWriteOptions().ordered(false),
                    (result, ex) -> {
                        if (ex != null) {
                            System.out.println(ex.getMessage());
                            return;
                        }
                    });

                timeConditions.bulkWrite(
                    timeIncrements,
                    new BulkWriteOptions().ordered(false),
                    (result, ex) -> {
                        if (ex != null) {
                            System.out.println(ex.getMessage());
                            return;
                        }
                    });

                locationConditions.bulkWrite(
                    locationIncrements,
                    new BulkWriteOptions().ordered(false),
                    (result, ex) -> {
                        if (ex != null) {
                            System.out.println(ex.getMessage());
                            return;
                        }

                    });

                conditionTotals.bulkWrite(
                    totalUpdates,
                    new BulkWriteOptions().ordered(false),
                    (result, t) -> {
                        if (t != null) {
                            System.out.println(t.getMessage());
                            return;
                        }
                });
            });
    }

    private ConditionStats buildStats(ConditionOccurrences<?> occurrences, ConditionTotalValue totalValue) {
        final Float valueRisk;
        if (occurrences.getOccurrences() == 0) {
            valueRisk = 0f;
        } else {
            valueRisk = ((float) occurrences.getFraudOccurrences()) / occurrences.getOccurrences();
        }

        return new ConditionStats(
            valueRisk,
            totalValue.getSumOfValues(),
            totalValue.getSumOfSquaredValues(),
            totalValue.getInstances()
        );
    }

    private UpdateOneModel<Document> buildTotalIncrease(ConditionTotalIncrement creditor, String type) {
        Document increase = new Document(INSTANCES_COUNT_FIELD, creditor.getNewInstances())
            .append(PROBABILITIES_TOTAL_FIELD, creditor.getValueIncrease())
            .append(SQUARED_PROBABILITIES_TOTAL_FIELD, creditor.getValueIncreaseSquared());

        return new UpdateOneModel<Document>(
            eq(TYPE_FIELD, type),
            new Document("$inc", increase),
            new UpdateOptions().upsert(true)
        );
    }

    private <T> ConditionTotalIncrement buildIncrement(
        List<ConditionOccurrences<T>> increments,
        List<ConditionOccurrences<String>> oldValues) {

        float sumDiff = 0;
        float squaredSumDiff = 0;
        long newInstances = 0;

        for(ConditionOccurrences<T> increment: increments) {

            Optional<ConditionOccurrences<String>> optionalPreviousValue = oldValues.stream()
                .filter(value -> value.getName().equals(increment.getName()))
                .findAny();

            ConditionOccurrences<String> previousValue;

            if (optionalPreviousValue.isPresent()) {
                previousValue = optionalPreviousValue.get();
            } else {
                newInstances += 1;
                previousValue = ConditionOccurrences.empty(increment.getName());
            }

            final float previousRate;

            if (previousValue.getOccurrences() == 0) {
                previousRate = 0f;
            } else {
                previousRate = ((float) previousValue.getFraudOccurrences()) / previousValue.getOccurrences();
            }

            float incrementedOccurences = previousValue.getOccurrences() + increment.getOccurrences();
            float incrementedFraudOccurences = previousValue.getFraudOccurrences() + increment.getFraudOccurrences();

            float newRate = incrementedFraudOccurences / incrementedOccurences;

            sumDiff += newRate - previousRate;
            squaredSumDiff += newRate * newRate - previousRate * previousRate;
        }

        return new ConditionTotalIncrement(newInstances, sumDiff, squaredSumDiff);
    }

    private Future<List<ConditionOccurrences<String>>> loadPreviousOccurrences(
        MongoCollection<Document> collection,
        List<String> ids){

        List<ConditionOccurrences<String>> previousValues = new ArrayList<>();

        Future<List<ConditionOccurrences<String>>> loader = Future.future();
        collection.find(Filters.in(ID_FIELD, ids))
            .forEach((document) -> {
                previousValues.add(buildOccurrencesFromDocument(document));
            }, (result, t) -> {
                if (t != null) {
                    t.printStackTrace();
                    return;
                }

                loader.complete(previousValues);
            });

        return loader;
    }

    private ConditionOccurrences buildOccurrencesFromDocument(Document document) {
        return new ConditionOccurrences<String>(
                            document.getString(ID_FIELD),
                            document.getLong(TOTAL_OCCURRENCES_FIELD),
                            document.getLong(FRAUD_OCCURRENCES_FIELD)
                        );
    }


    private UpdateOneModel<Document> buildIncrement(ConditionOccurrences<?> creditor) {

        Document increment = new Document(TOTAL_OCCURRENCES_FIELD, creditor.getOccurrences())
            .append(FRAUD_OCCURRENCES_FIELD, creditor.getFraudOccurrences());

        return new UpdateOneModel<>(
            eq(ID_FIELD, creditor.getName()),
            new Document("$inc", increment),
            new UpdateOptions().upsert(true)
        );
    }


}
