package org.ignas.frauddetection.probabilitystatistics.service;

import com.mongodb.async.client.MongoClient;
import com.mongodb.async.client.MongoClients;
import com.mongodb.async.client.MongoCollection;
import com.mongodb.client.model.BulkWriteOptions;
import com.mongodb.client.model.UpdateOneModel;
import com.mongodb.client.model.UpdateOptions;
import org.bson.Document;
import org.ignas.frauddetection.probabilitystatistics.domain.CriteriaUpdate;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class CriteriaGroupStorage {

    private MongoClient client;

    private MongoCollection<Document> criteriaProbabilities;

    public CriteriaGroupStorage(String url, String database, String collection) {
        client = MongoClients.create(url);

        criteriaProbabilities = client.getDatabase(database)
            .getCollection(collection);
    }

    /**
     * Nested on purpose.
     * Implements mongo command sequence:
     *
     * CREATES EMPTY ROOT OBJECT IF NOT EXISTS:
     * .update({ "name" : "NOT_EXPECTED" }, { $setOnInsert: { "name" : "NOT_EXPECTED", "values" : []}}, { upsert : true })
     *
     * CREATES MISSING VALUES IN ARRAY:
     * db.criteriaProbabilities.update({ "name" : "NOT_EXPECTED" }, { $addToSet: { "values" : { "name" : "MORE"}}})
     *
     * INCREMENTS MISSING VALUES:
     * db.criteriaProbabilities.update({ "name" : "NOT_EXPECTED", "values.name" : "MORE"}, { $inc: { "values.$.totalOccurences" : 1 }})
     *
     * Such sequence is workaround to this: https://jira.mongodb.org/browse/SERVER-24340
     * The problem is that, positional operator ($) does not support upserting.
     *  Therefore with current mongo implementation it is impossible to create missing documents
     *    and increment existing documents with signle query.
     *
     *    Another possibility would be fetch all existing documents from mongo,
     *    identify documents which are missing, and create only those who are missing.
     *    This would require additional application logic, would be less effective.
     *     Fetching all documents in order to find missing ones is required.
     *     With these queries mongo does creation in place.
     *
     *
     *
     */

    public void persist(List<CriteriaUpdate> changes) {
        List<UpdateOneModel<Document>> emptyDocumentPopulation = changes.stream()
            .map(CriteriaGroupStorage::mapToDocumentCreation)
            .collect(Collectors.toList());

        List<UpdateOneModel<Document>> missingValuesCreation = changes.stream()
            .map(CriteriaGroupStorage::mapToValueCreation)
            .collect(Collectors.toList());

        List<UpdateOneModel<Document>> requiredIncrements = changes.stream()
            .map(CriteriaGroupStorage::mapToMongoOperation)
            .collect(Collectors.toList());


        criteriaProbabilities.bulkWrite(
            emptyDocumentPopulation,
            (populationResult, populationException) -> {
                if (populationException != null) {
                    System.out.println(populationException.getMessage());
                    return;
                }

                criteriaProbabilities.bulkWrite(
                    missingValuesCreation,
                    (valueResult, valueException) -> {
                        if (valueException != null) {
                            System.out.println(valueException.getMessage());
                            return;
                        }

                        criteriaProbabilities.bulkWrite(
                            requiredIncrements,
                            new BulkWriteOptions().ordered(false),
                            (result, t) -> {
                                if (t != null) {
                                    System.out.println(t.getMessage());
                                }
                            });
                    });
            });
    }

    public void close() {
        client.close();
    }

    /**
     * Raw mongo query:
     *  update({ "name" : "NOT_EXPECTED" }, { $setOnInsert: { "name" : "NOT_EXPECTED", "values" : []}}, { upsert : true })
     *
     * This query is meant to insert empty documents that are missing. (Does not affect existing documents)
     * @param update
     * @return
     */
    private static UpdateOneModel<Document> mapToDocumentCreation(CriteriaUpdate update) {
        Document filter = new Document("name", update.targetCriteria());
        Document updateOperation = new Document("name", update.targetCriteria())
            .append("values", new ArrayList<>());

        return new UpdateOneModel<>(
            filter,
            new Document("$setOnInsert", updateOperation),
            new UpdateOptions().upsert(true)
        );
    }

    /**
     * Raw mongo query:
     *  update(
     *      {"name" : "EXPECTED", "values" : { $not : { $elemMatch : { "name" : "MORE" } } }},
     *      { $addToSet: { "values" : { "name" : "MORE"}}}
     *  )
     *
     * This query is meant to insert criteria values that are missing (Does not affect existing values).
     *
     * @param update
     * @return
     */
    private static UpdateOneModel<Document> mapToValueCreation(CriteriaUpdate update) {
        return new UpdateOneModel<>(
            new Document("name", update.targetCriteria())
                .append("values", new Document("$not", new Document("$elemMatch", new Document("name", update.criteriaValue())))),

            new Document("$addToSet", new Document("values", new Document("name", update.criteriaValue())))
        );
    }


    /**
     * update({ "name" : "NOT_EXPECTED", "values.name" : "MORE"}, { $inc: { "values.$.totalOccurences" : 1 }})
     *
     * @param update
     * @return
     */
    private static UpdateOneModel<Document> mapToMongoOperation(CriteriaUpdate update) {
        Document filter = new Document("name", update.targetCriteria())
            .append("values.name", update.criteriaValue());

        Document updateOperation = buildOperation(update);

        return new UpdateOneModel<>(
            filter,
            new Document("$inc", updateOperation)
        );
    }

    private static Document buildOperation(CriteriaUpdate update) {
        Document updateOperation = new Document();

        if (update.getNewOccurences() != 0) {
            updateOperation.append("values.$.totalOccurences", update.getNewOccurences());
        }

        if (update.getNewFraudOccurences() != 0) {
            updateOperation.append("values.$.occurencesInFraud", update.getNewFraudOccurences());
        }
        return updateOperation;
    }

}
