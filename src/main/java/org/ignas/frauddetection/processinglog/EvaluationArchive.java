package org.ignas.frauddetection.processinglog;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.EventBus;
import org.ignas.frauddetection.DetectionLauncher;
import org.ignas.frauddetection.probabilitystatistics.service.RequestsBatcher;
import org.ignas.frauddetection.processinglog.service.ResultStorage;
import org.ignas.frauddetection.transactionevaluation.api.request.LearningRequest;

public class EvaluationArchive extends AbstractVerticle {

    private static int BATCH_SIZE = 5000;

    private ResultStorage resultStorage;

    public EvaluationArchive() {
        this.resultStorage = new ResultStorage(
            "evaluation",
            "evaluatedTransactions"
        );
    }

    @Override
    public void start() {
        RequestsBatcher batcher = new RequestsBatcher(BATCH_SIZE);

        EventBus bus = vertx.eventBus();

        bus.consumer("learning.processing-request")
            .handler(message -> {
                if (!(message.body() instanceof LearningRequest)) {
                    message.fail(400, "Unnsuported message type: " + message.body().getClass());
                    return;
                }

                LearningRequest request = (LearningRequest) message.body();

                if (request.isFraudulent()) {
                    return;
                }
                batcher.add((LearningRequest) message.body())
                    .ifPresent(batch -> resultStorage.storeLogs(batch.getItems()));

            });

        bus.consumer("evaluation-archive.marked-fraudulent")
            .handler(message -> {
                if (!(message.body() instanceof String)) {
                    message.fail(400, "Unnsuported message type: " + message.body().getClass());
                    return;
                }

                String fraudulentTransactionId = (String) message.body();

                resultStorage.fetchTransactionResult(fraudulentTransactionId)
                    .setHandler(recreadedRequestLoader -> {
                        if (recreadedRequestLoader.failed()) {
                            recreadedRequestLoader.cause().printStackTrace();
                            throw new IllegalStateException(recreadedRequestLoader.cause());
                        }

                        LearningRequest recreatedRequest = recreadedRequestLoader.result();
                        recreatedRequest.markFraud();

                        bus.publish("learning.processing-request", recreatedRequest);
                    });
            });
    }

    @Override
    public void stop() {
        resultStorage.close();
    }
}
