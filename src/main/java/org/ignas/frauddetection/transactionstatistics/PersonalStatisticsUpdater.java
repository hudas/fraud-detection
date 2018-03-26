package org.ignas.frauddetection.transactionstatistics;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.eventbus.EventBus;
import org.ignas.frauddetection.probabilitystatistics.domain.BatchToProcess;
import org.ignas.frauddetection.probabilitystatistics.service.repositories.GeneralProbabilitiesStorage;
import org.ignas.frauddetection.transactionevaluation.api.request.LearningRequest;

public class PersonalStatisticsUpdater extends AbstractVerticle {

    private GeneralProbabilitiesStorage probabilitiesStorage;

    public PersonalStatisticsUpdater() {
        probabilitiesStorage = new GeneralProbabilitiesStorage(
            "mongodb://localhost",
            "transactions",
            "personalStatistics"
        );
    }

    @Override
    public void start(Future<Void> startFuture) {
        EventBus bus = vertx.eventBus();

        bus.consumer("transaction-processing.batch-prepared", (batchPrepared) -> {
            if (!(batchPrepared.body() instanceof BatchToProcess)) {
                throw new IllegalArgumentException("Unsupported type: " + batchPrepared.body().getClass());
            }

            BatchToProcess batch = (BatchToProcess) batchPrepared.body();

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

            bus.publish("transaction-processing.personal-data-updated", batch);
        });
    }
}
