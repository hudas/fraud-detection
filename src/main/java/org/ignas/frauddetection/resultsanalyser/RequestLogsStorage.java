package org.ignas.frauddetection.resultsanalyser;

import com.google.common.collect.ImmutableList;
import com.mongodb.async.client.MongoClient;
import com.mongodb.async.client.MongoClients;
import com.mongodb.async.client.MongoCollection;
import com.mongodb.client.model.InsertManyOptions;
import org.bson.Document;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.ignas.frauddetection.resultsanalyser.api.ResultLoggingRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class RequestLogsStorage {

    private static final int BATCH_SIZE = 5;

    private ConcurrentLinkedQueue<ResultLoggingRequest> buffer = new ConcurrentLinkedQueue<ResultLoggingRequest>();

    private final MongoClient client;
    private MongoCollection<Document> evaluations;

    private AtomicBoolean flushing = new AtomicBoolean(false);

    public RequestLogsStorage(String dbUrl, String database, String collectionName) {
        client = MongoClients.create(dbUrl);

        evaluations = client.getDatabase(database).getCollection(collectionName);
    }

    public void log(ResultLoggingRequest log) {
        buffer.add(log);

        if (buffer.size() >= BATCH_SIZE && flushing.compareAndSet(false, true)) {
            flush();
            flushing.set(false);
        }
    }

    public void close() {
        client.close();
    }

    private void flush() {
        System.out.println("Flushing");

        List<Document> logsToFlush = new ArrayList<>();

        // We do not care that not all buffer can be flushed in single flush.
        //  Eventually all logs will be flushed.
        for (int i = 0; i < BATCH_SIZE; i++) {
            logsToFlush.add(mapToDocument(buffer.poll()));
        }

        evaluations.insertMany(logsToFlush, new InsertManyOptions().ordered(false), (result, t) -> {
            if (t != null) {
                System.out.println(t.getMessage());
            }
        });
    }

    private Document mapToDocument(ResultLoggingRequest log) {
        return new Document()
            .append("transactionId", log.getTransactionId())
            .append("amount", log.getAmount())
            .append("creditor", log.getCreditorAccountId())
            .append("debtor", log.getDebtorAccountId())
            .append("latitude", log.getLocation().getLatitude())
            .append("longtitude", log.getLocation().getLongtitude())
            .append("time", log.getTime().toString())
            .append("probability", log.getProbability());
    }
}
