package org.ignas.frauddetection.probabilitystatistics.service.repositories;

import com.mongodb.async.client.MongoClient;
import com.mongodb.async.client.MongoClients;
import com.mongodb.async.client.MongoCollection;
import com.mongodb.client.model.UpdateOptions;
import io.vertx.core.Future;
import org.bson.Document;
import org.ignas.frauddetection.DetectionLauncher;
import org.ignas.frauddetection.probabilitystatistics.domain.GeneralOccurrences;

public class GeneralProbabilitiesStorage {

    private MongoClient client;

    private MongoCollection<Document> generalProbabilities;

    public GeneralProbabilitiesStorage(String database, String collection) {
        client = MongoClients.create(DetectionLauncher.BAYES_MONGODB_SETTINGS);

        generalProbabilities = client.getDatabase(database)
            .getCollection(collection);

    }

    public void persist(int numberOfNewTransactions, int numberOfNewFraudulentTransaction) {
        generalProbabilities.updateOne(
            new Document(),
            new Document("$inc", new Document("totalTransactions", (long) numberOfNewTransactions)
                .append("fraudulentTransactions", (long) numberOfNewFraudulentTransaction)),
            new UpdateOptions().upsert(true),
            (result, t) -> {
                if (t != null) {
                    System.out.println(t.getMessage());
                }
            }
        );
    }

    public Future<GeneralOccurrences> fetch() {
        long start = System.currentTimeMillis();

        Future<GeneralOccurrences> loader = Future.future();

        generalProbabilities.find()
            .first((result, t) -> {
            if (t != null) {
                System.out.println(t.getMessage());
                loader.fail(t);
                return;
            }

//            Initial system launch, without data yet available
            if (result == null) {
                long end = System.currentTimeMillis();
                loader.complete(new GeneralOccurrences(0, 0));
                return;
            }

            long end = System.currentTimeMillis();
            loader.complete(
                new GeneralOccurrences(
                    result.getLong("totalTransactions"),
                    result.getLong("fraudulentTransactions"))
            );
        });

        return loader;
    }

    public void close() {
        client.close();
    }
}
