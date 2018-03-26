package org.ignas.frauddetection.transactionstatistics;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.eventbus.EventBus;
import org.ignas.frauddetection.probabilitystatistics.domain.BatchToProcess;
import org.ignas.frauddetection.probabilitystatistics.service.repositories.GeneralProbabilitiesStorage;
import org.ignas.frauddetection.transactionevaluation.api.request.LearningRequest;

public class PublicStatisticsUpdater extends AbstractVerticle {

    private GeneralProbabilitiesStorage probabilitiesStorage;

    public PublicStatisticsUpdater() {
        probabilitiesStorage = new GeneralProbabilitiesStorage(
            "mongodb://localhost",
            "bayes",
            "publicStatistics"
        );
    }

    @Override
    public void start(Future<Void> startFuture) throws Exception {
        EventBus bus = vertx.eventBus();

        bus.consumer("transaction-processing.personal-data-updated", (batchEvent) -> {
            if (!(batchEvent.body() instanceof BatchToProcess)) {
                throw new IllegalArgumentException("Unsupported type: " + batchEvent.body().getClass());
            }

            BatchToProcess batch = (BatchToProcess) batchEvent.body();
//
//            int newTransactions = (int) batch.getItems()
//                .stream()
//                .filter(request -> !request.isAlreadyProcessedTransaction())
//                .count();
//
//            int newFraudulentTransactions = (int) batch.getItems()
//                .stream()
//                .filter(LearningRequest::isFraudulent)
//                .count();
//
//            probabilitiesStorage.persist(newTransactions, newFraudulentTransactions);

            // Resend same event without any modifications
            bus.publish("transaction-processing.public-data-updated", batch);
        });
    }
}
