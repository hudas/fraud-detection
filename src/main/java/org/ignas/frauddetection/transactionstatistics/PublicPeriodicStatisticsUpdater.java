package org.ignas.frauddetection.transactionstatistics;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.eventbus.EventBus;
import org.ignas.frauddetection.probabilitystatistics.domain.BatchToProcess;
import org.ignas.frauddetection.transactionevaluation.api.request.BehaviourData;
import org.ignas.frauddetection.transactionevaluation.api.request.LearningRequest;
import org.ignas.frauddetection.transactionevaluation.api.request.TransactionData;
import org.ignas.frauddetection.transactionstatistics.domain.PeriodIncrement;
import org.ignas.frauddetection.transactionstatistics.repositories.GeneralPeriodicTransactionsStorage;
import org.joda.time.DateTime;
import org.joda.time.LocalDateTime;

import java.util.ArrayList;
import java.util.List;

public class PublicPeriodicStatisticsUpdater extends AbstractVerticle {

    private GeneralPeriodicTransactionsStorage periodicStorage;

    public PublicPeriodicStatisticsUpdater() {
        periodicStorage = new GeneralPeriodicTransactionsStorage(
            "mongodb://localhost",
            "transactions"
        );
    }

    @Override
    public void start(Future<Void> startFuture) {
        EventBus bus = vertx.eventBus();

        bus.consumer("transaction-processing.public-non-periodic-data-updated", (batchEvent) -> {
            if (!(batchEvent.body() instanceof BatchToProcess)) {
                throw new IllegalArgumentException("Unsupported type: " + batchEvent.body().getClass());
            }

            BatchToProcess batch = (BatchToProcess) batchEvent.body();

            List<PeriodIncrement> dailyIncrements = new ArrayList<>();
            List<PeriodIncrement> weeklyIncrements = new ArrayList<>();
            List<PeriodIncrement> monthlyIncrements = new ArrayList<>();

            float totalSumIncrement = 0;
            int totalQuantityIncrement = 0;

            float totalDailyRatioIncrement = 0;
            float totalWeeklyRatioIncrement = 0;
            float totalMonthlyRatioIncrement = 0;

            for (LearningRequest request : batch.getItems()) {
                if (request.isAlreadyProcessedTransaction()) {
                    continue;
                }

                TransactionData data = request.getTransaction();

                totalQuantityIncrement += 1;
                totalSumIncrement += data.getAmount();

                totalDailyRatioIncrement += request.getBehaviourData().getSumRatioDaily();
                totalWeeklyRatioIncrement += request.getBehaviourData().getSumRatioWeekly();
                totalMonthlyRatioIncrement += request.getBehaviourData().getSumRatioMonthly();

                LocalDateTime time = data.getTime();

                dailyIncrements.add(new PeriodIncrement(
                    time,
                    time,
                    data.getDebtorId(),
                    data.getAmount()
                ));

                weeklyIncrements.add(new PeriodIncrement(
                    time.dayOfWeek().withMinimumValue(),
                    time.dayOfWeek().withMaximumValue(),
                    data.getDebtorId(),
                    data.getAmount()
                ));

                monthlyIncrements.add(new PeriodIncrement(
                    time.dayOfMonth().withMinimumValue(),
                    time.dayOfMonth().withMaximumValue(),
                    data.getDebtorId(),
                    data.getAmount()
                ));
            }

            Future<Integer> dailyLoader = periodicStorage.updateDaily(dailyIncrements);
            Future<Integer> weeklyLoader = periodicStorage.updateWeekly(weeklyIncrements);
            Future<Integer> monthlyLoader = periodicStorage.updateMonthly(monthlyIncrements);

            final float finalTotalSumIncrement = totalSumIncrement;
            final int finalTotalQuantityIncrement = totalQuantityIncrement;

            final float finalTotalDailyRatioIncrement = totalDailyRatioIncrement;
            final float finalTotalWeeklyRatioIncrement = totalWeeklyRatioIncrement;
            final float finalTotalMonthlyRatioIncrement = totalMonthlyRatioIncrement;

            CompositeFuture.all(dailyLoader, weeklyLoader, monthlyLoader)
                .setHandler(event -> {
                    if (event.failed()) {
                        event.cause().printStackTrace();
                        return;
                    }

                    Integer newDailyInstances = event.result().resultAt(0);
                    Integer newWeeklyInstances = event.result().resultAt(0);
                    Integer newMonthlyInstances = event.result().resultAt(0);


                    periodicStorage.updateTotals(
                        newDailyInstances,
                        newWeeklyInstances,
                        newMonthlyInstances,
                        finalTotalSumIncrement,
                        finalTotalQuantityIncrement,
                        finalTotalDailyRatioIncrement,
                        finalTotalWeeklyRatioIncrement,
                        finalTotalMonthlyRatioIncrement
                    );
                });

            // Resend same event without any modifications
            bus.publish("transaction-processing.public-periodic-data-updated", batch);
        });
    }
}
