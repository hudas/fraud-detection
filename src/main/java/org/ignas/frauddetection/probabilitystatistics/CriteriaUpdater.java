package org.ignas.frauddetection.probabilitystatistics;


import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.eventbus.EventBus;
import org.ignas.frauddetection.DetectionLauncher;
import org.ignas.frauddetection.probabilitystatistics.domain.BatchToProcess;
import org.ignas.frauddetection.probabilitystatistics.service.BatchedCriteriaProcessor;
import org.ignas.frauddetection.probabilitystatistics.service.repositories.CriteriaStorage;


public class CriteriaUpdater extends AbstractVerticle {

    private CriteriaStorage storage;

    @Override
    public void start(Future<Void> startFuture) {
        storage = new CriteriaStorage("bayes", "criteriaProbabilities");

        EventBus bus = vertx.eventBus();

        bus.consumer("probability-processing.group-data-updated", (batchEvent) -> {
            if (!(batchEvent.body() instanceof BatchToProcess)) {
                throw new IllegalArgumentException("Invalid message type: " + batchEvent.body().getClass());
            }

            BatchToProcess batch = (BatchToProcess) batchEvent.body();
            storage.persist(BatchedCriteriaProcessor.parseCriteriaUpdates(batch));

            bus.publish("probability-processing.criteria-data-updated" ,batch);
        });

        startFuture.complete();
    }

    @Override
    public void stop() throws Exception {
        storage.close();
    }
}
