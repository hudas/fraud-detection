package org.ignas.frauddetection.probabilitystatistics;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.eventbus.EventBus;
import org.ignas.frauddetection.probabilitystatistics.domain.BatchToProcess;
import org.ignas.frauddetection.probabilitystatistics.service.repositories.GeneralProbabilitiesStorage;
import org.ignas.frauddetection.transactionevaluation.api.request.LearningRequest;

public class GeneralDataUpdater extends AbstractVerticle {

    private GeneralProbabilitiesStorage probabilitiesStorage;

    public GeneralDataUpdater() {
        probabilitiesStorage = new GeneralProbabilitiesStorage(
            "mongodb://localhost",
            "bayes",
            "generalProbabilities"
        );
    }

    @Override
    public void start(Future<Void> startFuture) throws Exception {
        EventBus bus = vertx.eventBus();

        bus.consumer("probability-processing.batch-prepared", (batchEvent) -> {
            if (!(batchEvent.body() instanceof BatchToProcess)) {
                throw new IllegalArgumentException("Unsupported type: " + batchEvent.body().getClass());
            }

            BatchToProcess batch = (BatchToProcess) batchEvent.body();

            int newTransactions = (int) batch.getItems()
                .stream()
                .filter(request -> !request.isAlreadyProcessedTransaction())
                .count();

            int newFraudulentTransactions = (int) batch.getItems()
                .stream()
                .filter(LearningRequest::isFraudulent)
                .count();

            probabilitiesStorage.persist(newTransactions, newFraudulentTransactions);

            // Resend same event without any modifications
            bus.publish("probability-processing.general-data-updated", batch);
        });
    }

    @Override
    public void stop() {
        probabilitiesStorage.close();
    }
}
