package org.ignas.frauddetection.transactionstatistics.repositories;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.mongodb.async.client.MongoClient;
import com.mongodb.async.client.MongoClients;
import com.mongodb.async.client.MongoCollection;
import com.mongodb.client.model.*;
import org.bson.Document;
import org.ignas.frauddetection.probabilitystatistics.domain.CriteriaUpdate;
import org.ignas.frauddetection.probabilitystatistics.service.repositories.CriteriaStorage;
import org.ignas.frauddetection.shared.Location;
import org.ignas.frauddetection.transactionstatistics.domain.ConditionOccurrences;

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

    private MongoCollection<Document> externalConditions;

    public ConditionStorage(String url, String database, String collection) {
        client = MongoClients.create(url);

        externalConditions = client.getDatabase(database)
            .getCollection(collection);
    }


    /**
     *  .update({ creditors: { $exists : true }}, { $setOnInsert: { "creditors" : []}}, { upsert : true })
     *  .update({ "creditors" : { $not : { $elemMatch : { id : "123" }}}}, { $addToSet : { "creditors" : { id: "123", totalOccurences: 0, fraudOccurences: 0 }}})
     *  .update({ "creditors.id" : "123" }, { $inc : { "creditors.$.totalOccurences" : 1, "creditors.$.fraudOccurences" : 1 }})
     *
     * @param creditorOccurences
     * @param timeOccurences
     * @param locationOccurences
     */
    public void updateOccurrences(
        List<ConditionOccurrences<String>> creditorOccurences,
        List<ConditionOccurrences<Integer>> timeOccurences,
        List<ConditionOccurrences<Location>> locationOccurences) {

        List<UpdateOneModel<Document>> initOperations = Stream.of(CREDITORS_OBJECT, LOCATIONS_OBJECT, TIMES_OBJECT)
            .map(this::buildFieldInitOperation)
            .collect(Collectors.toList());

        List<UpdateOneModel<Document>> addOperations =
            buildOperationsForMissingConditions(creditorOccurences, timeOccurences, locationOccurences);

        List<UpdateOneModel<Document>> incOperations =
            buildOperationsForIncrements(creditorOccurences, timeOccurences, locationOccurences);

        externalConditions.bulkWrite(
            initOperations,
            new BulkWriteOptions().ordered(false),
            (initResult, initException) -> {
                if (initException != null) {
                    System.out.println(initException.getMessage());
                    return;
                }


                externalConditions.bulkWrite(
                    addOperations,
                    new BulkWriteOptions().ordered(false),
                    (addResult, addException) -> {
                        if (addException != null) {
                            System.out.println(addException.getMessage());
                            return;
                        }


                        externalConditions.bulkWrite(
                            incOperations,
                            new BulkWriteOptions().ordered(false),
                            (incrementResult, incrementException) -> {
                                if (addException != null) {
                                    System.out.println(addException.getMessage());
                                    return;
                                }


                                if (incrementException != null) {
                                    System.out.println(addException.getMessage());
                                    return;
                                }
                            });
                    });
            });
    }


    private List<UpdateOneModel<Document>> buildOperationsForIncrements(
        List<ConditionOccurrences<String>> creditorOccurrences,
        List<ConditionOccurrences<Integer>> timeOccurrences,
        List<ConditionOccurrences<Location>> locationOccurrences) {

        List<UpdateOneModel<Document>> addMissingCreditorsOperations = creditorOccurrences.stream()
            .map(creditor -> buildIncrement(CREDITORS_OBJECT, creditor))
            .collect(Collectors.toList());

        List<UpdateOneModel<Document>> addMissingTimeOperations = timeOccurrences.stream()
            .map(time -> buildIncrement(TIMES_OBJECT, time))
            .collect(Collectors.toList());

        List<UpdateOneModel<Document>> addMissingLocationOperations = locationOccurrences.stream()
            .map(location -> buildIncrement(LOCATIONS_OBJECT, location))
            .collect(Collectors.toList());

        return newArrayList(
            concat(addMissingCreditorsOperations, addMissingTimeOperations, addMissingLocationOperations));
    }

    private UpdateOneModel<Document> buildIncrement(
        String containerCollection,
        ConditionOccurrences<?> creditor) {

        String occurrencesMatcher = join(".", containerCollection, TOTAL_OCCURRENCES_FIELD);
        String fraudMatcher = join(".", containerCollection, TOTAL_OCCURRENCES_FIELD);

        Document increment = new Document(occurrencesMatcher, creditor.getOccurrences())
            .append(fraudMatcher, creditor.getOccurrences());

        return new UpdateOneModel<>(
            eq(join(".", containerCollection, ID_FIELD)),
            new Document("$inc", increment)
        );
    }

    private List<UpdateOneModel<Document>> buildOperationsForMissingConditions(
        List<ConditionOccurrences<String>> creditorOccurences,
        List<ConditionOccurrences<Integer>> timeOccurences,
        List<ConditionOccurrences<Location>> locationOccurences) {

        List<UpdateOneModel<Document>> addMissingCreditorsOperations = creditorOccurences.stream()
            .map(creditor -> buildMissingConditionAddition(CREDITORS_OBJECT, creditor))
            .collect(Collectors.toList());

        List<UpdateOneModel<Document>> addMissingTimeOperations = timeOccurences.stream()
            .map(time -> buildMissingConditionAddition(TIMES_OBJECT, time))
            .collect(Collectors.toList());

        List<UpdateOneModel<Document>> addMissingLocationOperations = locationOccurences.stream()
            .map(location -> buildMissingConditionAddition(LOCATIONS_OBJECT, location))
            .collect(Collectors.toList());

        return newArrayList(
            concat(addMissingCreditorsOperations, addMissingTimeOperations, addMissingLocationOperations));
    }

    private UpdateOneModel<Document> buildMissingConditionAddition(
        String containerCollection,
        ConditionOccurrences<?> creditor) {

        Document emptyCreditor = new Document(ID_FIELD, creditor.getName())
            .append(TOTAL_OCCURRENCES_FIELD, 0l)
            .append(FRAUD_OCCURRENCES_FIELD, 0l);

        return new UpdateOneModel<>(
            not(elemMatch(containerCollection, eq(ID_FIELD, creditor.getName()))),
            new Document("$addToSet", new Document(containerCollection, emptyCreditor))
        );
    }

    private UpdateOneModel<Document> buildFieldInitOperation(String field) {
        return new UpdateOneModel<>(
            Filters.exists(field, true),
            new Document("$setInInsert", new Document(field, new ArrayList<>()))
        );
    }
}
