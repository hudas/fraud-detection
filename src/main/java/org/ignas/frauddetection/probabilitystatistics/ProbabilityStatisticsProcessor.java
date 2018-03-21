package org.ignas.frauddetection.probabilitystatistics;


import com.google.common.collect.ImmutableMap;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.EventBus;
import org.ignas.frauddetection.probabilitystatistics.domain.BatchToProcess;
import org.ignas.frauddetection.probabilitystatistics.service.RequestsBatcher;
import org.ignas.frauddetection.shared.ImmutableObjectCodec;
import org.ignas.frauddetection.transactionevaluation.api.request.LearningRequest;

import java.util.Map;

public class ProbabilityStatisticsProcessor extends AbstractVerticle {

    public static final int BATCH_SIZE = 5;

    @Override
    public void start() {
        RequestsBatcher batcher = new RequestsBatcher(BATCH_SIZE);

        EventBus bus = vertx.eventBus();

        bus.registerDefaultCodec(BatchToProcess.class, new ImmutableObjectCodec<BatchToProcess>(BatchToProcess.class));

        bus.consumer("learning.processing-request")
            .handler(message -> {
                if (!(message.body() instanceof LearningRequest)) {
                    message.fail(101, "Wrong type");
                    return;
                }

                batcher.add((LearningRequest) message.body())
                    .ifPresent(batch -> bus.publish("probability-processing.batch-prepared", batch));
            });
    }
}
