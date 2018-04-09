package org.ignas.frauddetection.transactionstatistics;

import com.google.common.collect.ImmutableList;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.eventbus.EventBus;
import org.ignas.frauddetection.shared.ImmutableObjectCodec;
import org.ignas.frauddetection.shared.Location;
import org.ignas.frauddetection.transactionstatistics.api.request.StatisticsRequest;
import org.ignas.frauddetection.transactionstatistics.api.response.*;
import org.ignas.frauddetection.transactionstatistics.api.response.generalindicators.*;
import org.ignas.frauddetection.transactionstatistics.domain.*;
import org.ignas.frauddetection.transactionstatistics.repositories.ConditionStorage;
import org.ignas.frauddetection.transactionstatistics.repositories.GeneralPeriodicTransactionsStorage;
import org.ignas.frauddetection.transactionstatistics.repositories.GeneralTransactionsStorage;
import org.joda.time.LocalDateTime;

import static com.google.common.collect.Lists.newArrayList;
import static org.ignas.frauddetection.transactionstatistics.repositories.GeneralPeriodicTransactionsStorage.*;

public class TransactionStatisticArchive extends AbstractVerticle {

    private GeneralTransactionsStorage generalTransactionsStorage;
    private GeneralPeriodicTransactionsStorage generalPeriodicTransactionsStorage;

    private ConditionStorage conditionStorage;

    public TransactionStatisticArchive() {
        this.conditionStorage = new ConditionStorage("mongodb://localhost", "transactions");
        generalTransactionsStorage = new GeneralTransactionsStorage(
            "mongodb://localhost",
            "transactions"
        );
        generalPeriodicTransactionsStorage = new GeneralPeriodicTransactionsStorage(
            "mongodb://localhost",
            "transactions"
        );
    }

    @Override
    public void start() {
        EventBus bus = vertx.eventBus();

        bus.registerDefaultCodec(StatisticsRequest.class, new ImmutableObjectCodec<>(StatisticsRequest.class));
        bus.registerDefaultCodec(Statistics.class, new ImmutableObjectCodec<>(Statistics.class));

        bus.consumer("transaction-statistic.archive")
            .handler(message -> {
                if (!(message.body() instanceof StatisticsRequest)) {
                    System.out.println("Unsupported type: " + message.body().getClass());
                    return;
                }

                StatisticsRequest request = (StatisticsRequest) message.body();

                Future<ExternalConditions> conditionsLoader = conditionStorage.fetchOccurrences(
                    request.getRequestByCreditorId(),
                    request.getRequestByTime(),
                    LocationService.toNearestArea(request.getRequestByLocation())
                );

                Future<NonPeriodicGeneralStats> statsLoader = generalTransactionsStorage.fetchNonPeriodicStats();

                Future<PeriodicGeneralStats> periodicLoader = generalPeriodicTransactionsStorage.fetchPeriodicStats();

                CompositeFuture.all(conditionsLoader, statsLoader, periodicLoader).setHandler(
                    event -> {
                        if (event.failed()) {
                            event.cause().printStackTrace();
                            throw new IllegalStateException(event.cause());
                        }

                        ExternalConditions conditions = event.result().resultAt(0);
                        NonPeriodicGeneralStats nonPeriodicStats = event.result().resultAt(1);
                        PeriodicGeneralStats periodicStats = event.result().resultAt(2);

                        CredibilityScore creditorScore = buildScore(conditions.getCreditor());
                        CredibilityScore timeScore = buildScore(conditions.getTime());
                        CredibilityScore locationScore = buildScore(conditions.getLocation());

                        TimeDifferenceStatistics timeDiff = buildTimeDiff(nonPeriodicStats);
                        DistanceDifferenceStatistics lastDiff = buildLastDistanceDiff(nonPeriodicStats);
                        DistanceDifferenceStatistics commonDiff = buildCommonDistanceDiff(nonPeriodicStats);

                        SumStatistics dailySum = buildSumStatsForPeriod(periodicStats.get(SUM_TYPE, DAY_PERIOD), 1);
                        SumStatistics weeklySum = buildSumStatsForPeriod(periodicStats.get(SUM_TYPE, WEEK_PERIOD), 7);
                        SumStatistics monthlySum = buildSumStatsForPeriod(periodicStats.get(SUM_TYPE, MONTH_PERIOD), 30);

                        CountStatistics dailyCount = buildCountStatsForPeriod(periodicStats.get(COUNT_TYPE, DAY_PERIOD), 1);
                        CountStatistics weeklyCount = buildCountStatsForPeriod(periodicStats.get(COUNT_TYPE, WEEK_PERIOD), 7);
                        CountStatistics monthlyCount = buildCountStatsForPeriod(periodicStats.get(COUNT_TYPE, MONTH_PERIOD), 30);

                        RatioStatistics dailyRatio = buildRatioStatsForPeriod(periodicStats.get(RATIO_TYPE, DAY_PERIOD), 1);
                        RatioStatistics weeklyRatio = buildRatioStatsForPeriod(periodicStats.get(RATIO_TYPE, WEEK_PERIOD), 7);
                        RatioStatistics monthlyRatio = buildRatioStatsForPeriod(periodicStats.get(RATIO_TYPE, MONTH_PERIOD), 30);

                        Statistics result = new Statistics(
                            new DebtorTransactionStatistics(
                                new Location(54.25123f, 25.45211f),
                                new Location(54.25123f, 25.45211f),
                                10f,
                                LocalDateTime.now().minusHours(1),
                                3600,
                                ImmutableList.of(
                                    new PersonalPeriod(1,1f,2),
                                    new PersonalPeriod(7,110f,4),
                                    new PersonalPeriod(30,550f,22)
                                )
                            ),
                            creditorScore,
                            timeScore,
                            locationScore,
                            new PublicStatistics(
                                newArrayList(dailySum, weeklySum, monthlySum),
                                newArrayList(dailyRatio, weeklyRatio, monthlyRatio),
                                newArrayList(dailyCount, weeklyCount, monthlyCount),
                                newArrayList(timeDiff),
                                newArrayList(commonDiff),
                                newArrayList(lastDiff)
                            )
                        );

                        message.reply(result);
                    }
                );
            });
    }

    private CredibilityScore buildScore(ConditionStats<?> creditor) {
        float average = calcAverage(creditor.getValuesSum(), creditor.getInstances());
        float deviation = calcDeviation(creditor.getInstances(), average, creditor.getValuesSum(), creditor.getValuesSquaredSum());

        return new CredibilityScore(
            creditor.getMatchingValue(),
            average,
            deviation
        );
    }

    private TimeDifferenceStatistics buildTimeDiff(NonPeriodicGeneralStats nonPeriodicStats) {
        float averageTimeDiff = calcAverage((float) nonPeriodicStats.getSumOfTimeDiffFromLast(), nonPeriodicStats.getInstances());
        float deviationTimeDiff = calcDeviation(nonPeriodicStats.getInstances(), averageTimeDiff, nonPeriodicStats.getSumOfTimeDiffFromLast(), nonPeriodicStats.getSumOfSquaredTimeDiffFromLast());

        return new TimeDifferenceStatistics(averageTimeDiff, deviationTimeDiff);
    }

    private DistanceDifferenceStatistics buildLastDistanceDiff(NonPeriodicGeneralStats nonPeriodicStats) {
        float averageDistanceLast = calcAverage(nonPeriodicStats.getSumOfDistanceFromLast(), nonPeriodicStats.getInstances());
        float deviationDistanceLast = calcDeviation(nonPeriodicStats.getInstances(), averageDistanceLast, nonPeriodicStats.getSumOfDistanceFromLast(), nonPeriodicStats.getSumOfSquaredTimeDiffFromLast());

        return new DistanceDifferenceStatistics(averageDistanceLast, deviationDistanceLast);
    }

    private DistanceDifferenceStatistics buildCommonDistanceDiff(NonPeriodicGeneralStats nonPeriodicStats) {
        float averageDistanceCommon = calcAverage(nonPeriodicStats.getSumOfDistanceFromLast(), nonPeriodicStats.getInstances());
        float deviationDistanceCommon = calcDeviation(nonPeriodicStats.getInstances(), averageDistanceCommon, nonPeriodicStats.getSumOfDistanceFromComm(), nonPeriodicStats.getSumOfSquaredDistanceFromComm());

        return new DistanceDifferenceStatistics(averageDistanceCommon, deviationDistanceCommon);
    }

    private RatioStatistics buildRatioStatsForPeriod(PeriodStats stats, int length) {
        float averageDailySum = calcAverage(stats.getValueSum(), stats.getInstances());
        float deviationDailySum = calcDeviation(stats.getInstances(), averageDailySum, stats.getValueSum(), stats.getValueSumSquared());

        return new RatioStatistics(length, averageDailySum, deviationDailySum);
    }


    private CountStatistics buildCountStatsForPeriod(PeriodStats stats, int periodLength) {
        float averageDailySum = calcAverage(stats.getValueSum(), stats.getInstances());
        float deviationDailySum = calcDeviation(stats.getInstances(), averageDailySum, stats.getValueSum(), stats.getValueSumSquared());

        return new CountStatistics(periodLength, averageDailySum, deviationDailySum);
    }

    private SumStatistics buildSumStatsForPeriod(PeriodStats stats, int periodLength) {
        float average = calcAverage(stats.getValueSum(), stats.getInstances());
        float deviation = calcDeviation(stats.getInstances(), average, stats.getValueSum(), stats.getValueSumSquared());

        return new SumStatistics(periodLength, average, deviation);
    }

    private float calcAverage(float sum, long instances) {
        return sum / instances;
    }

    private float calcDeviation(long instances, float average, float sum, float squaredSum) {
        float denominator = instances * average * average - 2 * average * sum + squaredSum;

        return (float) Math.sqrt(denominator / (instances - 1));
    }
}
