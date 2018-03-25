package org.ignas.frauddetection.processinglog;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.EventBus;
import org.ignas.frauddetection.processinglog.service.ResultStorage;
import org.ignas.frauddetection.shared.ImmutableObjectCodec;
import org.ignas.frauddetection.shared.Location;
import org.ignas.frauddetection.transactionevaluation.api.request.LearningRequest;
import org.ignas.frauddetection.transactionstatistics.api.request.StatisticsRequest;
import org.ignas.frauddetection.transactionstatistics.api.response.*;
import org.ignas.frauddetection.transactionstatistics.api.response.generalindicators.*;
import org.joda.time.LocalDateTime;

public class EvaluationArchive extends AbstractVerticle {

    private ResultStorage resultStorage;

    public EvaluationArchive() {
        this.resultStorage = new ResultStorage(
            "mongodb://localhost",
            "evaluation",
            "evaluatedTransactions"
        );
    }

    @Override
    public void start() {
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

                resultStorage.storeLog(request);
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
}
