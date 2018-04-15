package org.ignas.frauddetection.processinglog.service;

import com.google.common.collect.Iterables;
import com.mongodb.async.client.MongoClient;
import com.mongodb.async.client.MongoClients;
import com.mongodb.async.client.MongoCollection;
import io.vertx.core.Future;
import org.bson.Document;
import org.ignas.frauddetection.probabilitystatistics.domain.CombinationStatistics;
import org.ignas.frauddetection.shared.Location;
import org.ignas.frauddetection.transactionevaluation.api.request.BehaviourData;
import org.ignas.frauddetection.transactionevaluation.api.request.LearningRequest;
import org.ignas.frauddetection.transactionevaluation.api.request.TransactionData;
import org.joda.time.LocalDateTime;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.mongodb.client.model.Filters.*;

public class ResultStorage {


    public static final String TRANSACTION_DATA_OBJECT = "data";
    public static final String CRITERIA_OBJECT = "criteria";
    public static final String AMOUNT_FIELD = "amount";
    public static final String DEBTOR_FIELD = "debtor";
    public static final String CREDITOR_FIELD = "creditor";
    public static final String TIME_FIELD = "time";
    public static final String LONGTITUDE_FIELD = "longtitude";
    public static final String LATITUDE_FIELD = "latitude";
    public static final String NAME_FIELD = "name";
    public static final String VALUE_FIELD = "value";
    public static final String CRITERIA_VALUES_OBJECT = "criteria";
    private MongoClient client;
    private MongoCollection<Document> resultCollection;

    public ResultStorage(String url, String database, String collection) {
        client = MongoClients.create(url);

        resultCollection = client.getDatabase(database)
            .getCollection(collection);
    }

    public Future<LearningRequest> fetchTransactionResult(String transactionId) {
        Future<LearningRequest> loader = Future.future();

        resultCollection.find(eq("id", transactionId))
            .first((result, t) -> {
                if (t != null) {
                    t.printStackTrace();
                    loader.fail(t);
                    return;
                }

                LearningRequest recreatedRequest = recreateRequestFromData(transactionId, result);

                loader.complete(recreatedRequest);
            });

        return loader;
    }

    public void close() {
        client.close();
    }

    public Future<Void> storeLog(LearningRequest request) {
        Document document = new Document("id", request.getTransaction().getTransactionId())
                .append(TRANSACTION_DATA_OBJECT, buildTransactionDocument(request))
                .append(CRITERIA_OBJECT, buildCriteriaDocuments(request));

        Future<Void> loader = Future.future();

        resultCollection.insertOne(document, ((result, t) -> {
            if (t != null) {
                t.printStackTrace();
                loader.fail(t);
                return;
            }

            loader.complete();
        }));

        return loader;
    }

    private List<Document> buildCriteriaDocuments(LearningRequest request) {
        List<Document> result = new ArrayList<>();

        for (Map.Entry<String, String> groupEntry : request.getCriteriaGroupValues().entrySet()) {

            Map<String, String> groupCriteria = request.getGroupedCriteriaValues().get(groupEntry.getKey());

            List<Document> mappedValues = new ArrayList<>();

            for (Map.Entry<String, String> values : groupCriteria.entrySet()) {
                mappedValues.add(new Document(values.getKey(), values.getValue()));
            }

            Document groupDocument = new Document(NAME_FIELD, groupEntry.getKey())
                .append(VALUE_FIELD, groupEntry.getValue())
                .append(CRITERIA_VALUES_OBJECT, mappedValues);

            result.add(groupDocument);
        }

        return result;
    }

    private Document buildTransactionDocument(LearningRequest request) {
        return new Document(AMOUNT_FIELD, request.getTransaction().getAmount())
            .append(DEBTOR_FIELD, request.getTransaction().getDebtorId())
            .append(CREDITOR_FIELD, request.getTransaction().getCreditorId())
            .append(TIME_FIELD, request.getTransaction().getTime().toString())
            .append(LONGTITUDE_FIELD, request.getTransaction().getLocation().getLongtitude())
            .append(LATITUDE_FIELD, request.getTransaction().getLocation().getLatitude());
    }

    private LearningRequest recreateRequestFromData(String transactionId, Document result) {
        TransactionData data = parseTransaction(
            transactionId,
            (Document) result.get(TRANSACTION_DATA_OBJECT)
        );

        Map<String, Map<String, String>> groupedCriteriaValues = new HashMap<>();

        Map<String, String> groupValues = new HashMap<>();

        List<Document> groupDocuments = (List<Document>) result.get(CRITERIA_OBJECT);

        for (Document group: groupDocuments) {

            String groupName = group.getString(NAME_FIELD);
            String value = group.getString(VALUE_FIELD);

            groupValues.put(groupName, value);

            Map<String, String> groupCriterias = new HashMap<>();

            List<Document> criteriaDocs = (List<Document>) group.get(CRITERIA_VALUES_OBJECT);
            for (Document criteria: criteriaDocs) {
                String key = Iterables.getFirst(criteria.keySet(), null);

                groupCriterias.put(key, criteria.getString(key));
            }

            groupedCriteriaValues.put(groupName, groupCriterias);
        }

        return new LearningRequest(
            false,
            data,
            null,
            groupedCriteriaValues,
            groupValues
        );
    }

    private TransactionData parseTransaction(String id, Document transactionData) {
        return new TransactionData(
            id,
            transactionData.getDouble(AMOUNT_FIELD).floatValue(),
            transactionData.getString(DEBTOR_FIELD),
            transactionData.getString(CREDITOR_FIELD),
            new Location(
                transactionData.getDouble(LONGTITUDE_FIELD).floatValue(),
                transactionData.getDouble(LATITUDE_FIELD).floatValue()
            ),
            LocalDateTime.parse(transactionData.getString(TIME_FIELD))
        );
    }

}
