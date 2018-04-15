package org.ignas.frauddetection.transactionstatistics;

import com.google.common.collect.ImmutableList;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.eventbus.EventBus;
import org.ignas.frauddetection.probabilitystatistics.domain.PersonalStats;
import org.ignas.frauddetection.probabilitystatistics.service.repositories.PersonalStatisticsStorage;
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

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.google.common.collect.Lists.newArrayList;
import static org.ignas.frauddetection.transactionstatistics.repositories.GeneralPeriodicTransactionsStorage.*;

public class TransactionStatisticArchive extends AbstractVerticle {

    private GeneralTransactionsStorage generalTransactionsStorage;
    private GeneralPeriodicTransactionsStorage generalPeriodicTransactionsStorage;
    private PersonalStatisticsStorage personalStatistics;

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
        personalStatistics = new PersonalStatisticsStorage(
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

                Future<PersonalStats> personalLoader = personalStatistics.fetchPersonalStats(request.getRequestByDebtorId());

                CompositeFuture.all(conditionsLoader, statsLoader, periodicLoader, personalLoader).setHandler(
                    event -> {
                        if (event.failed()) {
                            event.cause().printStackTrace();
                            throw new IllegalStateException(event.cause());
                        }

                        ExternalConditions conditions = event.result().resultAt(0);
                        NonPeriodicGeneralStats nonPeriodicStats = event.result().resultAt(1);
                        PeriodicGeneralStats periodicStats = event.result().resultAt(2);
                        PersonalStats personalStats = event.result().resultAt(3);

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

                        DebtorTransactionStatistics debtorStats = buildDebtorStats(personalStats);

                        Statistics result = new Statistics(
                            debtorStats,
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

    @Override
    public void stop() throws Exception {
        this.conditionStorage.stop();
        this.generalTransactionsStorage.close();
        this.generalPeriodicTransactionsStorage.close();
        this.personalStatistics.close();
    }

    private DebtorTransactionStatistics buildDebtorStats(PersonalStats personalStats) {
        if (personalStats == null) {
            return DebtorTransactionStatistics.unknown();
        }

        Location latestLocation = personalStats.getLatestTransaction().getLocation();

        Optional<Map.Entry<String, Long>> mostUsedLocation = personalStats.getLocationOccurences()
            .entrySet()
            .stream()
            .max(Comparator.comparingLong(Map.Entry::getValue));

        Location commonLocation;

        if (mostUsedLocation.isPresent()) {
            commonLocation = Location.fromShortCode(mostUsedLocation.get().getKey());
        } else {
            commonLocation = latestLocation;
        }

        Float mostValuableTransaction = personalStats.getMaxAmount();
        Long minTimeBetween = personalStats.getMinTimeDiff();

        LocalDateTime lastTransactionTime = personalStats.getLatestTransaction().getTime();

        List<PersonalPeriod> periods = personalStats.getPeriods()
            .values()
            .stream()
            .map(it -> new PersonalPeriod(it.getLength(), it.getSum(), it.getCount()))
            .collect(Collectors.toList());

        return new DebtorTransactionStatistics(
            commonLocation,
            latestLocation,
            mostValuableTransaction,
            lastTransactionTime,
            minTimeBetween.intValue(),
            periods
        );
    }

    private CredibilityScore buildScore(ConditionStats<?> creditor) {
        if (creditor.getInstances() == 0) {
            return new CredibilityScore(
                creditor.getMatchingValue(),
                0,
                0
            );
        }

        float average = calcAverage(creditor.getValuesSum(), creditor.getInstances());
        float deviation = calcDeviation(creditor.getInstances(), average, creditor.getValuesSum(), creditor.getValuesSquaredSum());

        return new CredibilityScore(
            creditor.getMatchingValue(),
            average,
            deviation
        );
    }

    private TimeDifferenceStatistics buildTimeDiff(NonPeriodicGeneralStats nonPeriodicStats) {
        if (nonPeriodicStats.getInstances() == 0) {
            return new TimeDifferenceStatistics(0f, 0f);
        }

        float averageTimeDiff = calcAverage((float) nonPeriodicStats.getSumOfTimeDiffFromLast(), nonPeriodicStats.getInstances());
        float deviationTimeDiff = calcDeviation(nonPeriodicStats.getInstances(), averageTimeDiff, nonPeriodicStats.getSumOfTimeDiffFromLast(), nonPeriodicStats.getSumOfSquaredTimeDiffFromLast());

        return new TimeDifferenceStatistics(averageTimeDiff, deviationTimeDiff);
    }

    private DistanceDifferenceStatistics buildLastDistanceDiff(NonPeriodicGeneralStats nonPeriodicStats) {
        if (nonPeriodicStats.getInstances() == 0) {
            return new DistanceDifferenceStatistics(0f, 0f);
        }

        float averageDistanceLast = calcAverage(nonPeriodicStats.getSumOfDistanceFromLast(), nonPeriodicStats.getInstances());
        float deviationDistanceLast = calcDeviation(nonPeriodicStats.getInstances(), averageDistanceLast, nonPeriodicStats.getSumOfDistanceFromLast(), nonPeriodicStats.getSumOfSquaredTimeDiffFromLast());

        return new DistanceDifferenceStatistics(averageDistanceLast, deviationDistanceLast);
    }

    private DistanceDifferenceStatistics buildCommonDistanceDiff(NonPeriodicGeneralStats nonPeriodicStats) {
        if (nonPeriodicStats.getInstances() == 0) {
            return new DistanceDifferenceStatistics(0f, 0f);
        }


        float averageDistanceCommon = calcAverage(nonPeriodicStats.getSumOfDistanceFromLast(), nonPeriodicStats.getInstances());
        float deviationDistanceCommon = calcDeviation(nonPeriodicStats.getInstances(), averageDistanceCommon, nonPeriodicStats.getSumOfDistanceFromComm(), nonPeriodicStats.getSumOfSquaredDistanceFromComm());

        return new DistanceDifferenceStatistics(averageDistanceCommon, deviationDistanceCommon);
    }

    private RatioStatistics buildRatioStatsForPeriod(PeriodStats stats, int length) {
        if (stats.getInstances() == 0) {
            return new RatioStatistics(length, 0f, 0f);
        }

        float averageDailySum = calcAverage(stats.getValueSum(), stats.getInstances());
        float deviationDailySum = calcDeviation(stats.getInstances(), averageDailySum, stats.getValueSum(), stats.getValueSumSquared());

        return new RatioStatistics(length, averageDailySum, deviationDailySum);
    }


    private CountStatistics buildCountStatsForPeriod(PeriodStats stats, int periodLength) {
        if (stats.getInstances() == 0) {
            return new CountStatistics(periodLength, 0f, 0f);
        }

        float averageDailySum = calcAverage(stats.getValueSum(), stats.getInstances());
        float deviationDailySum = calcDeviation(stats.getInstances(), averageDailySum, stats.getValueSum(), stats.getValueSumSquared());

        return new CountStatistics(periodLength, averageDailySum, deviationDailySum);
    }

    private SumStatistics buildSumStatsForPeriod(PeriodStats stats, int periodLength) {
        if (stats.getInstances() == 0) {
            return new SumStatistics(periodLength, 0f, 0f);
        }

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
