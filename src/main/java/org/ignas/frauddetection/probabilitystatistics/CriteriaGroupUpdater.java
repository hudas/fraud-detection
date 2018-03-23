package org.ignas.frauddetection.probabilitystatistics;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.eventbus.EventBus;
import org.ignas.frauddetection.probabilitystatistics.domain.BatchToProcess;
import org.ignas.frauddetection.probabilitystatistics.service.repositories.CriteriaStorage;

import static org.ignas.frauddetection.probabilitystatistics.service.BatchedCriteriaProcessor.parseCriteriaGroupUpdates;

public class CriteriaGroupUpdater extends AbstractVerticle {

    private CriteriaStorage groupStorage;

    public CriteriaGroupUpdater() {
        this.groupStorage = new CriteriaStorage("mongodb://localhost", "bayes", "criteriaGroupProbabilities");
    }

    @Override
    public void start(Future<Void> startFuture) {
        EventBus bus = vertx.eventBus();

        bus.consumer("probability-processing.general-data-updated", batchEvent -> {
            if (!(batchEvent.body() instanceof BatchToProcess)) {
                throw new IllegalArgumentException("Unsupported message type: " + batchEvent.body().getClass());
            }

            BatchToProcess batch = (BatchToProcess) batchEvent.body();
            groupStorage.persist(parseCriteriaGroupUpdates(batch));

            bus.publish("probability-processing.group-data-updated", batch);
        });
    }
}
