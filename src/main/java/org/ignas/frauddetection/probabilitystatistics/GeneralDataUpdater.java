package org.ignas.frauddetection.probabilitystatistics;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.eventbus.EventBus;

public class GeneralDataUpdater extends AbstractVerticle {

    @Override
    public void start(Future<Void> startFuture) throws Exception {

        EventBus bus = vertx.eventBus();

        bus.consumer("probability-processing.batch-prepared", (batchEvent) -> {
            System.out.println("Batch received to process");
        });
    }
}
