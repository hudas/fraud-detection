package org.ignas.frauddetection.transactionevaluation.domain.config;

import com.google.common.collect.ImmutableList;
import org.ignas.frauddetection.shared.Location;
import org.ignas.frauddetection.transactionevaluation.domain.Transaction;
import org.ignas.frauddetection.transactionevaluation.domain.calculation.criteria.NamedCriteria;
import org.ignas.frauddetection.transactionevaluation.domain.calculation.criteria.PeriodicCriteria;
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
            .calculator(new DeviationEvaluator())
            .mapper((Transaction transaction, HistoricalData statistics) -> {
                FraudRate creditor = statistics.getEnvironment().getCreditor();

                return new DeviationStatistics(
                    creditor.getFraudRate(),
                    creditor.getFraudRateAverage(),
                    creditor.getFraudRateDeviation()
                );
            })
            .build();
    }

    private NamedCriteria defineLocationCriterion() {
        return new NamedCriteria.Builder<DeviationStatistics>("LOCATION_RISK", LOCATION_GROUP)
            .calculator(new DeviationEvaluator())
            .mapper((Transaction transaction, HistoricalData statistics) -> {
                FraudRate location = statistics.getEnvironment().getLocation();

                return new DeviationStatistics(
                    location.getFraudRate(),
                    location.getFraudRateAverage(),
                    location.getFraudRateDeviation()
                );
            })
            .build();
    }

    private NamedCriteria defineCommonDistanceCriterion() {
        return new NamedCriteria.Builder<DeviationStatistics>("AVERAGE_DISTANCE_FROM_COMMON_LOCATION", LOCATION_GROUP)
            .calculator(new DeviationEvaluator())
            .mapper((Transaction transaction, HistoricalData statistics) -> {
                Location transactionLocation = transaction.getLocation();

                float distanceDegrees = (float) transactionLocation
                    .distanceTo(statistics.getDebtor().getMostUsedLocation());

                return new DeviationStatistics(
                    distanceDegrees,
                    statistics.getGlobal().getDistanceToCommonLocation().getAverage(),
                    statistics.getGlobal().getDistanceToCommonLocation().getDeviationAverage()
                );
            })
            .build();
    }

    private NamedCriteria defineLastTransactionDistanceCriterion() {
        return new NamedCriteria.Builder<DeviationStatistics>("AVERAGE_DISTANCE_FROM_LAST_LOCATION", LOCATION_GROUP)
            .calculator(new DeviationEvaluator())
            .mapper((Transaction transaction, HistoricalData statistics) -> {
                Location transactionLocation = transaction.getLocation();
                Location previousLocation = statistics.getDebtor().getLastTransactionLocation();

                float distanceDegrees = (float) transactionLocation
                    .distanceTo(previousLocation);

                return new DeviationStatistics(
                    distanceDegrees,
                    statistics.getGlobal().getDistanceToLastLocation().getAverage(),
                    statistics.getGlobal().getDistanceToLastLocation().getDeviationAverage()
                );
            })
            .build();
    }

    private static NamedCriteria defineTimeRiskCriterion() {
        return new NamedCriteria.Builder<DeviationStatistics>("TIME_RISK", TIME_GROUP)
            .calculator(new DeviationEvaluator())
            .mapper((Transaction transaction, HistoricalData statistics) -> {
                FraudRate time = statistics.getEnvironment().getTime();

                return new DeviationStatistics(
                    time.getFraudRate(),
                    time.getFraudRateAverage(),
                    time.getFraudRateDeviation()
                );
            })
            .build();
    }

    private static NamedCriteria defineTimeBetweenTransactionsCriterion() {
        return new NamedCriteria.Builder<DeviationStatistics>("AVERAGE_TIME_BETWEEN_TRANSACTIONS", TIME_GROUP)
            .calculator(new DeviationEvaluator())
            .mapper((Transaction transaction, HistoricalData statistics) -> {
                LocalDateTime previousTransactionTime = statistics.getDebtor()
                    .getLastTransactionExecutionTime();

                LocalDateTime executionTime = transaction.getTime();

                Seconds timeBetween =
                    Seconds.secondsBetween(previousTransactionTime, executionTime);

                MeanStatistics<Seconds> time = statistics.getGlobal().getTimeDifference();

                return new DeviationStatistics(
                    timeBetween.getSeconds(),
                    time.getAverage().getSeconds(),
                    time.getDeviationAverage().getSeconds()
                );
            })
            .build();
    }

    private static NamedCriteria defineMinTimeBetweenTransactionsCriterion() {
        return new NamedCriteria.Builder<ComparableStatistics>("MIN_TIME_BETWEEN_TRANSACTIONS", TIME_GROUP)
            .calculator(comparableStatistics -> {
                float timeBetween = comparableStatistics.getTransactionResult();
                float minTimeBetween = comparableStatistics.getHistoricalResult();

                return timeBetween < minTimeBetween ? BooleanCriteria.TRUE : BooleanCriteria.FALSE;
            })
            .mapper((Transaction transaction, HistoricalData statistics) -> {
                LocalDateTime lastTransaction = statistics.getDebtor().getLastTransactionExecutionTime();
                Seconds timeBetween = Seconds.secondsBetween(lastTransaction, transaction.getTime());

                Seconds shortestPreviousTime = statistics.getDebtor().getShortestTimeBetweenTransactions();

                return new ComparableStatistics(
                    timeBetween.getSeconds(),
                    shortestPreviousTime.getSeconds()
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

                return new DeviationStatistics(
                    totalTransactions,
                    globalCountDetails.getAverage(),
                    globalCountDetails.getDeviationAverage()
                );
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

                return new DeviationStatistics(
                    totalPersonalExpenses,
                    globalSumDetails.getAverage(),
                    globalSumDetails.getDeviationAverage()
                );
            })
            .build();
    }

    private static List<PeriodicCriteria<DeviationStatistics>> defineAmountRatio() {
        return new PeriodicCriteria.Builder<DeviationStatistics>("AVERAGE_PERIOD_AMOUNT_RATIO", AMOUNT_GROUP)
            .period(Days.SEVEN, Days.days(30))
            .calculator(new DeviationEvaluator())
            .mapper((Days period, Transaction transaction, HistoricalData statistics) -> {
                float totalAmountForPeriod = transaction.getAmount() + statistics.getDebtor().getExpensesForPeriod(period);

                float amountRatio = transaction.getAmount() / totalAmountForPeriod;

                MeanPeriodStatistics<Float> globalRatioDetails = statistics.getGlobal()
                    .ratioStatisticsForPeriod(period);

                return new DeviationStatistics(
                    amountRatio,
                    globalRatioDetails.getAverage(),
                    globalRatioDetails.getDeviationAverage()
                );
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

                return new ComparableStatistics(
                    transaction.getAmount(),
                    mostValuableTransaction
                );
            })
            .build();
    }


    private static NamedCriteria defineAmountCategory() {
        return new NamedCriteria.Builder<Transaction>("AMOUNT_CATEGORY", AMOUNT_GROUP)
            .calculator(transaction -> Arrays.stream(AmountCategoryCriteria.values())
                .filter(category -> category.abstracts(transaction.getAmount()))
                .findAny()
                .orElseThrow(IllegalStateException::new))
            .mapper((Transaction transaction, HistoricalData statistics) -> transaction)
            .build();
    }
}
