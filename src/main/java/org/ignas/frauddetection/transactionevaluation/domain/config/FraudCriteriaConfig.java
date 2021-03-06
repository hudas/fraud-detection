package org.ignas.frauddetection.transactionevaluation.domain.config;

import com.google.common.collect.ImmutableList;
import org.apache.commons.lang3.tuple.Pair;
import org.ignas.frauddetection.shared.Location;
import org.ignas.frauddetection.transactionevaluation.domain.Transaction;
import org.ignas.frauddetection.transactionevaluation.domain.calculation.MapperResult;
import org.ignas.frauddetection.transactionevaluation.domain.calculation.criteria.NamedCriteria;
import org.ignas.frauddetection.transactionevaluation.domain.calculation.criteria.PeriodicCriteria;
import org.ignas.frauddetection.transactionevaluation.domain.calculation.evaluators.AmplifiedDeviationEvaluator;
import org.ignas.frauddetection.transactionevaluation.domain.calculation.evaluators.ComparableStatistics;
import org.ignas.frauddetection.transactionevaluation.domain.calculation.evaluators.DeviationEvaluator;
import org.ignas.frauddetection.transactionevaluation.domain.calculation.evaluators.DeviationStatistics;
import org.ignas.frauddetection.transactionevaluation.domain.criteria.AmountCategoryCriteria;
import org.ignas.frauddetection.transactionevaluation.domain.criteria.BooleanCriteria;
import org.ignas.frauddetection.transactionevaluation.domain.stats.HistoricalData;
import org.ignas.frauddetection.transactionevaluation.domain.stats.details.FraudRate;
import org.ignas.frauddetection.transactionevaluation.domain.stats.details.MeanPeriodStatistics;
import org.ignas.frauddetection.transactionevaluation.domain.stats.details.MeanStatistics;
import org.joda.time.Days;
import org.joda.time.LocalDateTime;
import org.joda.time.Seconds;

import java.util.Arrays;
import java.util.List;

public class FraudCriteriaConfig {

    public static final String LOCATION_GROUP = "LOCATION";
    public static final String TIME_GROUP = "TIME";
    public static final String COUNT_GROUP = "COUNT";
    public static final String AMOUNT_GROUP = "AMOUNT";

    private final ImmutableList<NamedCriteria> CRITERIA;


    public FraudCriteriaConfig() {
        CRITERIA = ImmutableList.<NamedCriteria>builder()
            .addAll(defineAmountCriteria())
            .addAll(defineCountCriteria())
            .addAll(defineAmountRatio())
            .add(defineMaxSpentAmount())
            .add(defineAmountCategory())
            .add(defineTimeRiskCriterion())
            .add(defineMinTimeBetweenTransactionsCriterion())
            .add(defineTimeBetweenTransactionsCriterion())
            .add(defineLocationCriterion())
            .add(defineCommonDistanceCriterion())
            .add(defineLastTransactionDistanceCriterion())
            .add(defineCreditorCriterion())
            .build();
    }

    public ImmutableList<NamedCriteria> definedCriteria() {
        return CRITERIA;
    }

    private static NamedCriteria defineCreditorCriterion() {
        return new NamedCriteria.Builder<DeviationStatistics>("CREDITOR_RISK", LOCATION_GROUP)
            .calculator(new AmplifiedDeviationEvaluator())
            .mapper((Transaction transaction, HistoricalData statistics) -> {
                FraudRate creditor = statistics.getEnvironment().getCreditor();

                return MapperResult.withoutBehaviour(new DeviationStatistics(
                    creditor.getFraudRate(),
                    creditor.getFraudRateAverage(),
                    creditor.getFraudRateDeviation()
                ));
            })
            .build();
    }

    private NamedCriteria defineLocationCriterion() {
        return new NamedCriteria.Builder<DeviationStatistics>("LOCATION_RISK", LOCATION_GROUP)
            .calculator(new AmplifiedDeviationEvaluator())
            .mapper((Transaction transaction, HistoricalData statistics) -> {
                FraudRate location = statistics.getEnvironment().getLocation();

                return MapperResult.withoutBehaviour(new DeviationStatistics(
                    location.getFraudRate(),
                    location.getFraudRateAverage(),
                    location.getFraudRateDeviation()
                ));
            })
            .build();
    }

    private NamedCriteria defineCommonDistanceCriterion() {
        return new NamedCriteria.Builder<DeviationStatistics>("AVERAGE_DISTANCE_FROM_COMMON_LOCATION", LOCATION_GROUP)
            .calculator(new DeviationEvaluator())
            .mapper((Transaction transaction, HistoricalData statistics) -> {
                Location mostUsedLocation = statistics.getDebtor().getMostUsedLocation();

                // First transaction for each debtor should be always expected.
                if (mostUsedLocation == null) {
                    return new MapperResult<>(
                        new DeviationStatistics(
                            0f,
                            0f,
                            0f
                        ),
                        0f
                    );
                }

                Location transactionLocation = transaction.getLocation();

                float distanceDegrees = (float) transactionLocation
                    .distanceTo(mostUsedLocation);

                return new MapperResult<>(
                    new DeviationStatistics(
                        distanceDegrees,
                        statistics.getGlobal().getDistanceToCommonLocation().getAverage(),
                        statistics.getGlobal().getDistanceToCommonLocation().getDeviationAverage()
                    ),
                    distanceDegrees
                );
            })
            .build();
    }

    private NamedCriteria defineLastTransactionDistanceCriterion() {
        return new NamedCriteria.Builder<DeviationStatistics>("AVERAGE_DISTANCE_FROM_LAST_LOCATION", LOCATION_GROUP)
            .calculator(new DeviationEvaluator())
            .mapper((Transaction transaction, HistoricalData statistics) -> {
                Location previousLocation = statistics.getDebtor().getLastTransactionLocation();
                if (previousLocation == null) {
                    return new MapperResult<>(
                        new DeviationStatistics(
                            0f,
                            0f,
                            0f
                        ),
                        0f
                    );
                }

                Location transactionLocation = transaction.getLocation();


                float distanceDegrees = (float) transactionLocation
                    .distanceTo(previousLocation);

                return new MapperResult<>(
                    new DeviationStatistics(
                        distanceDegrees,
                        statistics.getGlobal().getDistanceToLastLocation().getAverage(),
                        statistics.getGlobal().getDistanceToLastLocation().getDeviationAverage()
                    ),
                    distanceDegrees
                );
            })
            .build();
    }

    private static NamedCriteria defineTimeRiskCriterion() {
        return new NamedCriteria.Builder<DeviationStatistics>("TIME_RISK", TIME_GROUP)
            .calculator(new AmplifiedDeviationEvaluator())
            .mapper((Transaction transaction, HistoricalData statistics) -> {
                FraudRate time = statistics.getEnvironment().getTime();

                return MapperResult.withoutBehaviour(new DeviationStatistics(
                    time.getFraudRate(),
                    time.getFraudRateAverage(),
                    time.getFraudRateDeviation()
                ));
            })
            .build();
    }

    private static NamedCriteria defineTimeBetweenTransactionsCriterion() {
        return new NamedCriteria.Builder<DeviationStatistics>("AVERAGE_TIME_BETWEEN_TRANSACTIONS", TIME_GROUP)
            .calculator(new DeviationEvaluator())
            .mapper((Transaction transaction, HistoricalData statistics) -> {
                LocalDateTime previousTransactionTime = statistics.getDebtor()
                    .getLastTransactionExecutionTime();

                if (previousTransactionTime == null) {
                    return MapperResult.withoutBehaviour(
                        new DeviationStatistics(
                            0f,
                            0f,
                            0f
                        )
                    );
                }

                LocalDateTime executionTime = transaction.getTime();

                Seconds timeBetween =
                    Seconds.secondsBetween(previousTransactionTime, executionTime);

                MeanStatistics<Seconds> time = statistics.getGlobal().getTimeDifference();

                return MapperResult.withoutBehaviour(
                    new DeviationStatistics(
                        timeBetween.getSeconds(),
                        time.getAverage().getSeconds(),
                        time.getDeviationAverage().getSeconds()
                    )
                );
            })
            .build();
    }

    private static NamedCriteria defineMinTimeBetweenTransactionsCriterion() {
        return new NamedCriteria.Builder<ComparableStatistics>("MIN_TIME_BETWEEN_TRANSACTIONS", TIME_GROUP)
            .calculator(comparableStatistics -> {
                float minTimeBetween = comparableStatistics.getHistoricalResult();

                if (minTimeBetween == 0f) {
                    return BooleanCriteria.TRUE;
                }

                float timeBetween = comparableStatistics.getTransactionResult();

                return timeBetween < minTimeBetween ? BooleanCriteria.TRUE : BooleanCriteria.FALSE;
            })
            .mapper((Transaction transaction, HistoricalData statistics) -> {
                LocalDateTime lastTransaction = statistics.getDebtor().getLastTransactionExecutionTime();
                if (lastTransaction == null) {
                    return new MapperResult<ComparableStatistics>(
                        new ComparableStatistics(
                            0f,
                            0f
                        ),
                        0f
                    );
                }

                Seconds timeBetween = Seconds.secondsBetween(lastTransaction, transaction.getTime());

                Seconds shortestPreviousTime = statistics.getDebtor().getShortestTimeBetweenTransactions();

                return new MapperResult(
                    new ComparableStatistics(
                        timeBetween.getSeconds(),
                        shortestPreviousTime.getSeconds()
                    ),
                    Float.valueOf(timeBetween.getSeconds())
                );
            })
            .build();
    }

    private static List<PeriodicCriteria<DeviationStatistics>> defineCountCriteria() {
        return new PeriodicCriteria.Builder<DeviationStatistics>("AVERAGE_COUNT", COUNT_GROUP)
            .period(Days.ONE, Days.SEVEN, Days.days(30))
            .calculator(new DeviationEvaluator())
            .mapper((Days period, Transaction transaction, HistoricalData statistics) -> {
                int alreadyInitiatedTransactions = statistics.getDebtor()
                    .getNumberOfTransactionsForPeriod(period);

                // This single transaction we are evaluating is equal to 1
                int totalTransactions = 1 + alreadyInitiatedTransactions;

                MeanPeriodStatistics<Float> globalCountDetails = statistics.getGlobal()
                    .countStatisticsForPeriod(period);

                return MapperResult.withoutBehaviour(new DeviationStatistics(
                    totalTransactions,
                    globalCountDetails.getAverage(),
                    globalCountDetails.getDeviationAverage()
                ));
            })
            .build();

    }

    private static List<PeriodicCriteria<DeviationStatistics>> defineAmountCriteria() {
        return new PeriodicCriteria.Builder<DeviationStatistics>("AVERAGE_SUM", AMOUNT_GROUP)
            .period(Days.ONE, Days.SEVEN, Days.days(30))
            .calculator(new DeviationEvaluator())
            .mapper((Days period, Transaction transaction, HistoricalData statistics) -> {
                float alreadySpent = statistics.getDebtor().getExpensesForPeriod(period);
                float totalPersonalExpenses = alreadySpent + transaction.getAmount();

                MeanPeriodStatistics<Float> globalSumDetails = statistics.getGlobal()
                    .sumStatisticsForPeriod(period);

                return MapperResult.<DeviationStatistics>withoutBehaviour(new DeviationStatistics(
                    totalPersonalExpenses,
                    globalSumDetails.getAverage(),
                    globalSumDetails.getDeviationAverage()
                ));
            })
            .build();
    }

    private static List<PeriodicCriteria<DeviationStatistics>> defineAmountRatio() {
        return new PeriodicCriteria.Builder<DeviationStatistics>("AVERAGE_PERIOD_AMOUNT_RATIO", AMOUNT_GROUP)
            .period(Days.days(1), Days.SEVEN, Days.days(30))
            .calculator(new DeviationEvaluator())
            .mapper((Days period, Transaction transaction, HistoricalData statistics) -> {
                float expensesForPeriod = statistics.getDebtor().getExpensesForPeriod(period);

                final float amountRatio;
                if (expensesForPeriod == 0) {
                    amountRatio = 1;
                } else {
                    amountRatio = transaction.getAmount() / expensesForPeriod;
                }

                MeanPeriodStatistics<Float> globalRatioDetails = statistics.getGlobal()
                    .ratioStatisticsForPeriod(period);

                return new MapperResult(new DeviationStatistics(
                    amountRatio,
                    globalRatioDetails.getAverage(),
                    globalRatioDetails.getDeviationAverage()
                ), amountRatio);

            })
            .build();
    }

    private static NamedCriteria defineMaxSpentAmount() {
        return new NamedCriteria.Builder<ComparableStatistics>("MAX_SPENT_AMOUNT", AMOUNT_GROUP)
            .calculator(comparableStatistics -> {
                float spentAmount = comparableStatistics.getTransactionResult();
                float maxPreviouslySpent = comparableStatistics.getHistoricalResult();

                return spentAmount > maxPreviouslySpent ? BooleanCriteria.TRUE : BooleanCriteria.FALSE;
            })
            .mapper((Transaction transaction, HistoricalData statistics) -> {
                float mostValuableTransaction = statistics.getDebtor().getMostValuableTransaction();

                return MapperResult.withoutBehaviour(new ComparableStatistics(
                    transaction.getAmount(),
                    mostValuableTransaction
                ));
            })
            .build();
    }


    private static NamedCriteria defineAmountCategory() {
        return new NamedCriteria.Builder<Transaction>("AMOUNT_CATEGORY", AMOUNT_GROUP)
            .calculator(transaction -> Arrays.stream(AmountCategoryCriteria.values())
                .filter(category -> category.abstracts(transaction.getAmount()))
                .findAny()
                .orElseThrow(IllegalStateException::new))
            .mapper((Transaction transaction, HistoricalData statistics) ->
                MapperResult.<Transaction>withoutBehaviour(transaction)
            )
            .build();
    }
}
