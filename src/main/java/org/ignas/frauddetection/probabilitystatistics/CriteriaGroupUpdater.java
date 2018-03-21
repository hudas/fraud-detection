package org.ignas.frauddetection.probabilitystatistics;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.eventbus.EventBus;
import org.ignas.frauddetection.probabilitystatistics.domain.BatchToProcess;
import org.ignas.frauddetection.probabilitystatistics.domain.CriteriaUpdate;
import org.ignas.frauddetection.probabilitystatistics.service.BatchProcessor;
import org.ignas.frauddetection.probabilitystatistics.service.CriteriaGroupStorage;
import org.ignas.frauddetection.transactionevaluation.api.request.LearningRequest;

import static org.ignas.frauddetection.probabilitystatistics.service.BatchProcessor.parseCriteriaGroupUpdates;

public class CriteriaGroupUpdater extends AbstractVerticle {

    private CriteriaGroupStorage groupStorage;

    public CriteriaGroupUpdater() {
        this.groupStorage = new CriteriaGroupStorage("mongodb://localhost", "bayes", "criteriaGroupProbabilities");
    }

    @Override
    public void start(Future<Void> startFuture) throws Exception {
        EventBus bus = vertx.eventBus();

        bus.consumer("probability-processing.batch-prepared", batchEvent -> {
            if (!(batchEvent.body() instanceof BatchToProcess)) {
                throw new IllegalArgumentException("Unsupported message type: " + batchEvent.body().getClass());
            }

            groupStorage.persist(parseCriteriaGroupUpdates((BatchToProcess) batchEvent.body()));
        });
    }
}
