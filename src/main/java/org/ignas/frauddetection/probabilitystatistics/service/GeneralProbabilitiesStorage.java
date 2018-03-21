package org.ignas.frauddetection.probabilitystatistics.service;

import com.mongodb.async.client.MongoClient;
import com.mongodb.async.client.MongoClients;
import com.mongodb.async.client.MongoCollection;
import com.mongodb.client.model.UpdateOptions;
import org.bson.Document;

public class GeneralProbabilitiesStorage {

    private MongoClient client;

    private MongoCollection<Document> generalProbabilities;

    public GeneralProbabilitiesStorage(String url, String database, String collection) {
        client = MongoClients.create(url);

        generalProbabilities = client.getDatabase(database)
            .getCollection(collection);

    }

    public void persist(int numberOfNewTransactions, int numberOfNewFraudulentTransaction) {
        generalProbabilities.updateOne(
            new Document(),
            new Document("$inc", new Document("totalTransactions", numberOfNewTransactions)
                .append("fraudulentTransactions", numberOfNewFraudulentTransaction)),
            new UpdateOptions().upsert(true),
            (result, t) -> {
                if (t != null) {
                    System.out.println(t.getMessage());
                }
            }
        );
    }
}
