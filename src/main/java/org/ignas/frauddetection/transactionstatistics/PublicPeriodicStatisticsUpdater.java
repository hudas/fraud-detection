package org.ignas.frauddetection.transactionstatistics;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.eventbus.EventBus;
import org.ignas.frauddetection.probabilitystatistics.domain.BatchToProcess;
import org.ignas.frauddetection.transactionevaluation.api.request.LearningRequest;
import org.ignas.frauddetection.transactionevaluation.api.request.TransactionData;
import org.ignas.frauddetection.transactionstatistics.domain.PeriodIncrement;
import org.ignas.frauddetection.transactionstatistics.domain.DebtorPeriodValue;
import org.ignas.frauddetection.transactionstatistics.domain.SquaredIncrement;
import org.ignas.frauddetection.transactionstatistics.domain.TotalIncrement;
import org.ignas.frauddetection.transactionstatistics.repositories.GeneralPeriodicTransactionsStorage;
import org.joda.time.LocalDateTime;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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

            float totalSquaredDailyRatioIncrement = 0;
            float totalSquaredWeeklyRatioIncrement = 0;
            float totalSquaredMonthlyRatioIncrement = 0;

            for (LearningRequest request : batch.getItems()) {
                if (request.isAlreadyProcessedTransaction()) {
                    continue;
                }

                TransactionData data = request.getTransaction();

                totalQuantityIncrement += 1;
                totalSumIncrement += data.getAmount();

                float dailyRatio = request.getBehaviourData().getSumRatioDaily();
                float weeklyRatio = request.getBehaviourData().getSumRatioWeekly();
                float monthlyRatio = request.getBehaviourData().getSumRatioDaily();

                totalDailyRatioIncrement += dailyRatio;
                totalWeeklyRatioIncrement += weeklyRatio;
                totalMonthlyRatioIncrement += monthlyRatio;

                totalSquaredDailyRatioIncrement += dailyRatio * dailyRatio;
                totalSquaredWeeklyRatioIncrement += weeklyRatio * weeklyRatio;
                totalSquaredMonthlyRatioIncrement += monthlyRatio * monthlyRatio;

                LocalDateTime time = data.getTime();

                dailyIncrements.add(new PeriodIncrement(
                    time,
                    time,
                    data.getDebtorId(),
                    data.getAmount(),
                    1
                ));

                weeklyIncrements.add(new PeriodIncrement(
                    time.dayOfWeek().withMinimumValue(),
                    time.dayOfWeek().withMaximumValue(),
                    data.getDebtorId(),
                    data.getAmount(),
                    1
                ));

                monthlyIncrements.add(new PeriodIncrement(
                    time.dayOfMonth().withMinimumValue(),
                    time.dayOfMonth().withMaximumValue(),
                    data.getDebtorId(),
                    data.getAmount(),
                    1
                ));
            }

            final float finalTotalSumIncrement = totalSumIncrement;

            final int finalTotalQuantityIncrement = totalQuantityIncrement;

            final float finalTotalDailyRatioIncrement = totalDailyRatioIncrement;
            final float finalTotalWeeklyRatioIncrement = totalWeeklyRatioIncrement;
            final float finalTotalMonthlyRatioIncrement = totalMonthlyRatioIncrement;

            final float finalTotalDailySquaredRatioIncrement = totalSquaredDailyRatioIncrement;
            final float finalTotalWeeklySquaredRatioIncrement = totalSquaredWeeklyRatioIncrement;
            final float finalTotalMonthlySquaredRatioIncrement = totalSquaredMonthlyRatioIncrement;


            Future<List<DebtorPeriodValue>> previousDailyLoader = periodicStorage.fetchOldDaily(dailyIncrements);
            Future<List<DebtorPeriodValue>> previousWeeklyLoader = periodicStorage.fetchOldWeekly(weeklyIncrements);
            Future<List<DebtorPeriodValue>> previousMonthlyLoader = periodicStorage.fetchOldMonthly(monthlyIncrements);

            CompositeFuture.all(previousDailyLoader, previousWeeklyLoader, previousMonthlyLoader)
                .setHandler(previousDataEvent -> {
                    if (previousDataEvent.failed()) {
                        previousDataEvent.cause().printStackTrace();
                        return;
                    }

                    final List<DebtorPeriodValue> dailyPreviousValues = previousDataEvent.result().resultAt(0);

                    final List<DebtorPeriodValue> weeklyPreviousValues = previousDataEvent.result().resultAt(1);
                    final List<DebtorPeriodValue> monthlyPreviousValues = previousDataEvent.result().resultAt(2);

                    final SquaredIncrement dailySquares = periodSquaredIncrement(dailyIncrements, dailyPreviousValues);
                    final SquaredIncrement weeklySquares = periodSquaredIncrement(weeklyIncrements, weeklyPreviousValues);
                    final SquaredIncrement monthlySquares = periodSquaredIncrement(monthlyIncrements, monthlyPreviousValues);

                    Future<Integer> dailyLoader = periodicStorage.updateDaily(dailyIncrements);
                    Future<Integer> weeklyLoader = periodicStorage.updateWeekly(weeklyIncrements);
                    Future<Integer> monthlyLoader = periodicStorage.updateMonthly(monthlyIncrements);

                    CompositeFuture.all(dailyLoader, weeklyLoader, monthlyLoader)
                        .setHandler(updateEvent -> {
                            if (updateEvent.failed()) {
                                updateEvent.cause().printStackTrace();
                                return;
                            }

                            Integer newDailyInstances = updateEvent.result().resultAt(0);
                            Integer newWeeklyInstances = updateEvent.result().resultAt(0);
                            Integer newMonthlyInstances = updateEvent.result().resultAt(0);

                            periodicStorage.updateTotals(
                                newDailyInstances,
                                newWeeklyInstances,
                                newMonthlyInstances,
                                finalTotalQuantityIncrement,
                                new TotalIncrement(finalTotalSumIncrement, dailySquares.getSumIncrement()),
                                new TotalIncrement(finalTotalSumIncrement, weeklySquares.getSumIncrement()),
                                new TotalIncrement(finalTotalSumIncrement, monthlySquares.getSumIncrement()),
                                new TotalIncrement(finalTotalQuantityIncrement, dailySquares.getQuantityIncrement()),
                                new TotalIncrement(finalTotalQuantityIncrement, weeklySquares.getQuantityIncrement()),
                                new TotalIncrement(finalTotalQuantityIncrement, monthlySquares.getQuantityIncrement()),
                                new TotalIncrement(finalTotalDailyRatioIncrement, finalTotalDailySquaredRatioIncrement),
                                new TotalIncrement(finalTotalWeeklyRatioIncrement, finalTotalWeeklySquaredRatioIncrement),
                                new TotalIncrement(finalTotalMonthlyRatioIncrement, finalTotalMonthlySquaredRatioIncrement)
                            );

                            // Resend same event without any modifications
                            bus.publish("transaction-processing.public-periodic-data-updated", batch);
                        });

                });
        });
    }

    @Override
    public void stop() {
        periodicStorage.close();
    }

    public SquaredIncrement periodSquaredIncrement(List<PeriodIncrement> increments, List<DebtorPeriodValue> oldValues) {
        float dailySquaredSumDiff = 0;
        float dailySquaredCountDiff = 0;

        for (PeriodIncrement increment : increments) {
            Optional<DebtorPeriodValue> optionalOldValue = oldValues.stream()
                .filter(value -> value.matches(increment))
                .findAny();

            if (optionalOldValue.isPresent()) {
                DebtorPeriodValue oldValue = optionalOldValue.get();

                float oldSquaredSum = oldValue.getSum() * oldValue.getSum();
                float newSquaredSum = (oldValue.getSum() + increment.getAmount()) * (oldValue.getSum() + increment.getAmount());

                dailySquaredSumDiff += newSquaredSum - oldSquaredSum;

                float oldSquaredCount = oldValue.getCount() * oldValue.getCount();
                float newSquaredCount = (oldValue.getCount() + increment.getCount()) * (oldValue.getCount() + increment.getCount());

                dailySquaredCountDiff += oldSquaredCount - newSquaredCount;
            } else {
                dailySquaredSumDiff += increment.getAmount() * increment.getAmount();
                dailySquaredCountDiff += increment.getCount() * increment.getCount();
            }
        }

        return new SquaredIncrement(dailySquaredSumDiff, dailySquaredCountDiff);
    }
}
