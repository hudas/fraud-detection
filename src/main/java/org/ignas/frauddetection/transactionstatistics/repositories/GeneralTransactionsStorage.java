package org.ignas.frauddetection.transactionstatistics.repositories;

import com.mongodb.async.client.MongoClient;
import com.mongodb.async.client.MongoClients;
import com.mongodb.async.client.MongoCollection;
import com.mongodb.client.model.UpdateOptions;
import io.vertx.core.Future;
import org.bson.Document;
import org.ignas.frauddetection.DetectionLauncher;
import org.ignas.frauddetection.transactionstatistics.domain.NonPeriodicGeneralStats;
import org.joda.time.LocalDateTime;
import org.joda.time.Seconds;

public class GeneralTransactionsStorage {

    private static final String INSTANCES_FIELD = "instances";
    private static final String SQUARED_DISTANCE_COMMON = "sumOfSquaredDistanceFromComm";
    private static final String DISTANCE_COMMON = "sumOfDistanceFromComm";
    private static final String SQUARED_DISTANCE_LAST = "sumOfSquaredDistanceFromLast";
    private static final String DISTANCE_LAST = "sumOfDistanceFromLast";
    private static final String SQUARED_TIME_DIFFERENCE = "sumOfSquaredTimeDiffFromLast";
    private static final String TIME_DIFFERENCE = "sumOfTimeDiffFromLast";

    private NonPeriodicGeneralStats CACHE = null;
    private LocalDateTime CACHED_AT = null;

    private MongoClient client;

    private MongoCollection<Document> nonPeriodicGeneralStatistics;

    public GeneralTransactionsStorage(String database) {
        client = MongoClients.create(DetectionLauncher.TRANSACTIONS_MONGODB_SETTINGS);

        nonPeriodicGeneralStatistics = client.getDatabase(database)
            .getCollection("nonPeriodicGeneralStatistics");

    }

    public void close() {
        client.close();
    }

    public void increment(
        long count,
        long timeDiff,
        long squaredTimeDiff,
        float distanceFromLast,
        float squaredDistanceFromLast,
        float distanceFromCommon,
        float squaredDistanceFromCommon) {

        Document incrementDocument = new Document(TIME_DIFFERENCE, timeDiff)
            .append(SQUARED_TIME_DIFFERENCE, squaredTimeDiff)
            .append(DISTANCE_LAST, distanceFromLast)
            .append(SQUARED_DISTANCE_LAST, squaredDistanceFromLast)
            .append(DISTANCE_COMMON, distanceFromCommon)
            .append(SQUARED_DISTANCE_COMMON, squaredDistanceFromCommon)
            .append(INSTANCES_FIELD, count);

        nonPeriodicGeneralStatistics.updateOne(
            new Document(),
            new Document("$inc", incrementDocument),
            new UpdateOptions().upsert(true),
            (result, t) -> {

            }
        );

    }

    public Future<NonPeriodicGeneralStats> fetchNonPeriodicStats() {
        if (CACHE != null && CACHED_AT != null
            && Seconds.secondsBetween(LocalDateTime.now(), CACHED_AT).getSeconds() * 1000 <= DetectionLauncher.CACHE_TTL) {
            return Future.succeededFuture(CACHE);
        }

        Future<NonPeriodicGeneralStats> loader = Future.future();

        nonPeriodicGeneralStatistics.find().first((result, t) -> {
            if (t != null) {
                t.printStackTrace();
                loader.fail(t);
                return;
            }

            if (result == null) {
                loader.complete(NonPeriodicGeneralStats.unknown());
                return;
            }

            NonPeriodicGeneralStats stats = new NonPeriodicGeneralStats(
                result.getLong(INSTANCES_FIELD),
                result.getLong(TIME_DIFFERENCE),
                result.getLong(SQUARED_TIME_DIFFERENCE),
                result.getDouble(DISTANCE_LAST).floatValue(),
                result.getDouble(SQUARED_DISTANCE_LAST).floatValue(),
                result.getDouble(DISTANCE_COMMON).floatValue(),
                result.getDouble(SQUARED_DISTANCE_COMMON).floatValue()
            );

            CACHE = stats;
            CACHED_AT = LocalDateTime.now();
            loader.complete(stats);
        });

        return loader;
    }
}
