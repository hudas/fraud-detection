package org.ignas.frauddetection.transactionstatistics.repositories;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.mongodb.async.client.MongoClient;
import com.mongodb.async.client.MongoClients;
import com.mongodb.async.client.MongoCollection;
import com.mongodb.client.model.*;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import org.bson.Document;
import org.ignas.frauddetection.probabilitystatistics.domain.CriteriaUpdate;
import org.ignas.frauddetection.probabilitystatistics.service.repositories.CriteriaStorage;
import org.ignas.frauddetection.shared.Location;
import org.ignas.frauddetection.transactionstatistics.domain.ConditionOccurrences;
import org.ignas.frauddetection.transactionstatistics.domain.ExternalConditions;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.common.collect.Iterables.concat;
import static com.google.common.collect.Lists.newArrayList;
import static com.mongodb.client.model.Filters.*;
import static java.lang.String.join;

public class ConditionStorage {

    private static final String CREDITORS_OBJECT = "creditors";
    private static final String LOCATIONS_OBJECT = "locations";
    private static final String TIMES_OBJECT = "times";

    private static final String ID_FIELD = "id";

    private static final String TOTAL_OCCURRENCES_FIELD = "totalOccurrences";
    private static final String FRAUD_OCCURRENCES_FIELD = "fraudOccurrences";

    private MongoClient client;

    private MongoCollection<Document> creditorsConditions;
    private MongoCollection<Document> timeConditions;
    private MongoCollection<Document> locationConditions;

    public ConditionStorage(String url, String database) {
        client = MongoClients.create(url);

        creditorsConditions = client.getDatabase(database)
            .getCollection("creditorsConditions");
        timeConditions = client.getDatabase(database)
            .getCollection("timeConditions");
        locationConditions = client.getDatabase(database)
            .getCollection("locationConditions");
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
//
//        creditorOccurences.stream().map(it -> )
//        creditorsConditions.find()

        creditorsConditions.bulkWrite(
            creditorIncrements,
            new BulkWriteOptions().ordered(false),
            (result, ex) -> {
                if (ex != null) {
                    System.out.println(ex.getMessage());
                    return;
                }

                int newCreditorsAdded = result.getInsertedCount();

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
    }


    private UpdateOneModel<Document> buildIncrement(ConditionOccurrences<?> creditor) {

        Document increment = new Document(TOTAL_OCCURRENCES_FIELD, creditor.getOccurrences())
            .append(TOTAL_OCCURRENCES_FIELD, creditor.getOccurrences());

        return new UpdateOneModel<>(
            eq(ID_FIELD, creditor.getName()),
            new Document("$inc", increment),
            new UpdateOptions().upsert(true)
        );
    }

    public Future<ExternalConditions> fetchOccurrences(String creditor, org.joda.time.LocalDateTime hour, Location location) {
        Future<ConditionOccurrences> creditorLoader = Future.future();
        creditorsConditions.find(Filters.eq(ID_FIELD, creditor))
            .first((result, t) -> {
                if (t != null) {
                    t.printStackTrace();
                    return;
                }

                ConditionOccurrences mappedResult = new ConditionOccurrences<String>(
                    result.getString(ID_FIELD),
                    result.getLong(TOTAL_OCCURRENCES_FIELD),
                    result.getLong(FRAUD_OCCURRENCES_FIELD)
                );

                creditorLoader.complete(mappedResult);
            });

        Future<ConditionOccurrences> timeLoader = Future.future();
        timeConditions.find(Filters.eq(ID_FIELD, hour.getHourOfDay()))
            .first((result, t) -> {
                if (t != null) {
                    t.printStackTrace();
                    return;
                }

                ConditionOccurrences mappedResult = new ConditionOccurrences<Integer>(
                    result.getInteger(ID_FIELD),
                    result.getLong(TOTAL_OCCURRENCES_FIELD),
                    result.getLong(FRAUD_OCCURRENCES_FIELD)
                );

                timeLoader.complete(mappedResult);
            });

        Future<ConditionOccurrences> locationLoader = Future.future();
        locationConditions.find(Filters.eq(ID_FIELD, location.toString()))
            .first((result, t) -> {
                if (t != null) {
                    t.printStackTrace();
                    return;
                }

                ConditionOccurrences mappedResult = new ConditionOccurrences<String>(
                    result.getString(ID_FIELD),
                    result.getLong(TOTAL_OCCURRENCES_FIELD),
                    result.getLong(FRAUD_OCCURRENCES_FIELD)
                );

                timeLoader.complete(mappedResult);
            });

        Future<ExternalConditions> future = Future.future();

        CompositeFuture.all(creditorLoader, timeLoader, locationLoader)
            .setHandler(allLoaded -> {
                if (allLoaded.failed()) {
                    allLoaded.cause().printStackTrace();
                    future.fail(allLoaded.cause());
                    return;
                }

                ConditionOccurrences<String> creditorStats = allLoaded.result().resultAt(0);
                ConditionOccurrences<Integer> timeStats = allLoaded.result().resultAt(1);
                ConditionOccurrences<String> locationStats = allLoaded.result().resultAt(2);

                future.complete(new ExternalConditions(creditorStats, timeStats, locationStats));
            });

        return future;
    }
}
