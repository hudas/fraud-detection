package org.ignas.frauddetection.probabilitystatistics.service;

import com.google.common.collect.ImmutableList;
import org.ignas.frauddetection.probabilitystatistics.domain.BatchToProcess;
import org.ignas.frauddetection.transactionevaluation.api.request.LearningRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * In memory stores requests, and returns batch when full.
 *
 */
public class RequestsBatcher {

    private final int batchSize;

    private ConcurrentLinkedQueue<LearningRequest> buffer = new ConcurrentLinkedQueue<>();

    private AtomicBoolean flushing = new AtomicBoolean(false);

    public RequestsBatcher(int batchSize) {
        this.batchSize = batchSize;
    }

    public Optional<BatchToProcess> add(LearningRequest request) {
        buffer.add(request);
        System.out.println("Request received, Transactions batch size: " + buffer.size());

        if (buffer.size() >= batchSize && flushing.compareAndSet(false, true)) {
            BatchToProcess requestsBatch = prepareBatch();
            flushing.set(false);

            return Optional.of(requestsBatch);
        }

        // Normal flow - empty is returned with each request
        return Optional.empty();
    }

    private BatchToProcess prepareBatch() {
        List<LearningRequest> requestBatch = new ArrayList<>();

        for (int i = 0; i < batchSize; i++) {
            requestBatch.add(buffer.poll());
        }

        return new BatchToProcess(requestBatch);
    }
}
