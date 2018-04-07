package org.ignas.frauddetection.transactionstatistics;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.eventbus.EventBus;
import org.ignas.frauddetection.probabilitystatistics.domain.BatchToProcess;
import org.ignas.frauddetection.transactionevaluation.api.request.BehaviourData;
import org.ignas.frauddetection.transactionevaluation.api.request.LearningRequest;
import org.ignas.frauddetection.transactionstatistics.repositories.GeneralTransactionsStorage;

public class PublicPeriodicStatisticsUpdater extends AbstractVerticle {

    private GeneralTransactionsStorage nonPeriodicTransactionsStorage;

    public PublicPeriodicStatisticsUpdater() {
        nonPeriodicTransactionsStorage = new GeneralTransactionsStorage(
            "mongodb://localhost",
            "transactions"
        );
    }

    @Override
    public void start(Future<Void> startFuture) throws Exception {
        EventBus bus = vertx.eventBus();

        bus.consumer("transaction-processing.public-non-periodic-data-updated", (batchEvent) -> {
            if (!(batchEvent.body() instanceof BatchToProcess)) {
                throw new IllegalArgumentException("Unsupported type: " + batchEvent.body().getClass());
            }

            BatchToProcess batch = (BatchToProcess) batchEvent.body();

            long timeDiffIncrement = 0;
            long squaredTimeDiffIncrement = 0;

            float distanceFromLastIncrement = 0;
            float squaredDistanceFromLastIncrement = 0;

            float distanceFromCommonIncrement = 0;
            float squaredDistanceFromCommonIncrement = 0;

            for (LearningRequest request : batch.getItems()) {
                BehaviourData data = request.getBehaviourData();

                long timeDiff = data.getTimeDifferenceFromLast();
                timeDiffIncrement += timeDiff;
                squaredTimeDiffIncrement += timeDiff * timeDiff;

                float distanceFromLast = data.getDistanceFromLast();
                distanceFromLastIncrement += distanceFromLast;
                squaredDistanceFromLastIncrement += distanceFromLast * distanceFromLast;

                float distanceFromCommon = data.getDistanceFromLast();
                distanceFromLastIncrement += distanceFromCommon;
                squaredDistanceFromLastIncrement += distanceFromCommon * distanceFromCommon;
            }

            long additionalInstances = batch.getItems().size();

            nonPeriodicTransactionsStorage.increment(
                additionalInstances,
                timeDiffIncrement, squaredTimeDiffIncrement,
                distanceFromLastIncrement, squaredDistanceFromLastIncrement,
                distanceFromCommonIncrement, squaredDistanceFromCommonIncrement
            );

            // Resend same event without any modifications
            bus.publish("transaction-processing.public-periodic-data-updated", batch);
        });
    }
}
