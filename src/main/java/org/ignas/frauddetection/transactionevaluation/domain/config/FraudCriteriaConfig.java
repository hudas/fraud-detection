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
import java.util.Map;
import java.util.stream.Collectors;

// TODO: Refactor static state out to be build when initialising application.
// This will lead to testability and Separation of concerns
public class FraudCriteriaConfig {

    private static final List<NamedCriteria> CRITERIAS = ImmutableList.<NamedCriteria>builder()
        .addAll(defineAmountCriterias())
        .addAll(defineCountCriterias())
        .addAll(defineTimeCriterias())
        .addAll(defineLocationCriteria())
        .addAll(defineCreditorCriteria())
        .build();

    // Idea behind this, is to build inverted hash map which helps to resolve criteria group by criteria name.
    private static final Map<String, String> CRITERIA_GROUP_CACHE = CRITERIAS.stream()
        .collect(Collectors.toMap(criteria -> criteria.name(), criteria -> criteria.group()));


    public static List<NamedCriteria> definedCriterias() {
        return CRITERIAS;
    }

    public static String resolveGroup(String criteriaName) {
        return CRITERIA_GROUP_CACHE.get(criteriaName);
    }

    private static ImmutableList<NamedCriteria> defineCreditorCriteria() {
        ImmutableList.Builder<NamedCriteria> builder = ImmutableList.<NamedCriteria>builder();

        return builder.add(
                new NamedCriteria.Builder<DeviationStatistics>("CREDITOR_RISK", "LOCATION")
                    .calculator(new DeviationEvaluator())
                    .mapper((Transaction transaction, HistoricalData statistics) -> {
                        FraudRate creditor = statistics.getEnvironment().getCreditor();

                        return new DeviationStatistics(
                            creditor.getFraudRate(),
                            creditor.getFraudRateAverage(),
                            creditor.getFraudRateDeviation()
                        );
                    })
                    .build()
            ).build();
    }

    private static ImmutableList<NamedCriteria> defineLocationCriteria() {
        ImmutableList.Builder<NamedCriteria> builder = ImmutableList.<NamedCriteria>builder();

        return builder.add(
                new NamedCriteria.Builder<DeviationStatistics>("LOCATION_RISK", "LOCATION")
                    .calculator(new DeviationEvaluator())
                    .mapper((Transaction transaction, HistoricalData statistics) -> {
                        FraudRate location = statistics.getEnvironment().getLocation();

                        return new DeviationStatistics(
                            location.getFraudRate(),
                            location.getFraudRateAverage(),
                            location.getFraudRateDeviation()
                        );
                    })
                    .build()
            )
            .add(
                new NamedCriteria.Builder<DeviationStatistics>("AVERAGE_DISTANCE_FROM_COMMON_LOCATION", "LOCATION")
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
                    .build()
            )
            .add(
                new NamedCriteria.Builder<DeviationStatistics>("AVERAGE_DISTANCE_FROM_LAST_LOCATION", "LOCATION")
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
                    .build()
            ).build();
    }

    private static ImmutableList<NamedCriteria> defineTimeCriterias() {
        ImmutableList.Builder<NamedCriteria> builder = ImmutableList.<NamedCriteria>builder();

        return builder.add(
                new NamedCriteria.Builder<DeviationStatistics>("TIME_RISK", "TIME")
                    .calculator(new DeviationEvaluator())
                    .mapper((Transaction transaction, HistoricalData statistics) -> {
                        FraudRate time = statistics.getEnvironment().getTime();

                        return new DeviationStatistics(
                            time.getFraudRate(),
                            time.getFraudRateAverage(),
                            time.getFraudRateDeviation()
                        );
                    })
                    .build()
            )
            .add(
                new NamedCriteria.Builder<DeviationStatistics>("AVERAGE_TIME_BETWEEN_TRANSACTIONS", "TIME")
                    .calculator(new DeviationEvaluator())
                    .mapper((Transaction transaction, HistoricalData statistics) -> {
                        LocalDateTime previousTransactionTime = statistics.getDebtor()
                            .getLastTransactionExecutionTime();

                        LocalDateTime executionTime = transaction.getTime();

                        Seconds timeBetween =
                            Seconds.secondsBetween(executionTime, previousTransactionTime);

                        MeanStatistics<Seconds> time = statistics.getGlobal().getTime();

                        return new DeviationStatistics(
                            timeBetween.getSeconds(),
                            time.getAverage().getSeconds(),
                            time.getDeviationAverage().getSeconds()
                        );
                    })
                    .build()
            )
            .add(
                new NamedCriteria.Builder<DeviationStatistics>("EXPECTED_TIME_BETWEEN_TRANSACTIONS", "TIME")
                    .calculator(new DeviationEvaluator())
                    .mapper((Transaction transaction, HistoricalData statistics) -> {
                        LocalDateTime previousTransactionTime = statistics.getDebtor()
                            .getLastTransactionExecutionTime();

                        LocalDateTime executionTime = transaction.getTime();

                        Seconds timeBetween =
                            Seconds.secondsBetween(executionTime, previousTransactionTime);

                        MeanStatistics<Seconds> time = statistics.getGlobal().getTime();

                        return new DeviationStatistics(
                            timeBetween.getSeconds(),
                            time.getExpected().getSeconds(),
                            time.getDeviationExpected().getSeconds()
                        );
                    })
                    .build()
            )
            .add(
                new NamedCriteria.Builder<ComparableStatistics>("MIN_TIME_BETWEEN_TRANSACTIONS", "TIME")
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
                    .build()
            ).build();
    }

    private static ImmutableList<NamedCriteria> defineCountCriterias() {
        ImmutableList.Builder<NamedCriteria> builder = new ImmutableList.Builder<>();

        return builder.addAll(
                new PeriodicCriteria.Builder<DeviationStatistics>("AVERAGE_COUNT", "COUNT")
                    .period(Days.ONE, Days.SEVEN, Days.days(30))
                    .calculator(new DeviationEvaluator())
                    .mapper((Days period, Transaction transaction, HistoricalData statistics) -> {
                        int alreadyInitiatedTransactions = statistics.getDebtor()
                            .numberOfTransactionsForPeriod(period);

                        // This single transaction we are evaluating is equal to 1
                        int totalTransactions = 1 + alreadyInitiatedTransactions;

                        MeanPeriodStatistics<Integer> globalCountDetails = statistics.getGlobal()
                            .countStatisticsForPeriod(period);

                        return new DeviationStatistics(
                            totalTransactions,
                            globalCountDetails.getAverage(),
                            globalCountDetails.getDeviationAverage()
                        );
                    })
                    .build()
            )
            .addAll(
                new PeriodicCriteria.Builder<DeviationStatistics>("EXPECTED_COUNT", "COUNT")
                    .period(Days.ONE, Days.SEVEN, Days.days(30))
                    .calculator(new DeviationEvaluator())
                    .mapper((Days period, Transaction transaction, HistoricalData statistics) -> {
                        int alreadyInitiatedTransactions = statistics.getDebtor()
                            .numberOfTransactionsForPeriod(period);

                        // This single transaction we are evaluating is equal to 1
                        int totalTransactions = 1 + alreadyInitiatedTransactions;

                        MeanPeriodStatistics<Integer> globalCountDetails = statistics.getGlobal()
                            .countStatisticsForPeriod(period);

                        return new DeviationStatistics(
                            totalTransactions,
                            globalCountDetails.getExpected(),
                            globalCountDetails.getDeviationExpected()
                        );
                    })
                    .build()
            ).build();
    }

    private static ImmutableList<NamedCriteria> defineAmountCriterias() {
        ImmutableList.Builder<NamedCriteria> builder = new ImmutableList.Builder<>();

        return builder.addAll(
                new PeriodicCriteria.Builder<DeviationStatistics>("AVERAGE_SUM", "AMOUNT")
                    .period(Days.ONE, Days.SEVEN, Days.days(30))
                    .calculator(new DeviationEvaluator())
                    .mapper((Days period, Transaction transaction, HistoricalData statistics) -> {
                        float alreadySpent = statistics.getDebtor().expensesForPeriod(period);
                        float totalPersonalExpenses = alreadySpent + transaction.getAmount();

                        MeanPeriodStatistics<Float> globalSumDetails = statistics.getGlobal()
                            .sumStatisticsForPeriod(period);

                        return new DeviationStatistics(
                            totalPersonalExpenses,
                            globalSumDetails.getAverage(),
                            globalSumDetails.getDeviationAverage()
                        );
                    })
                    .build()
            )
            .addAll(
                new PeriodicCriteria.Builder<DeviationStatistics>("EXPECTED_SUM", "AMOUNT")
                    .period(Days.ONE, Days.SEVEN, Days.days(30))
                    .calculator(new DeviationEvaluator())
                    .mapper((Days period, Transaction transaction, HistoricalData statistics) -> {
                        float alreadySpent = statistics.getDebtor().expensesForPeriod(period);
                        float totalPersonalExpenses = alreadySpent + transaction.getAmount();

                        MeanPeriodStatistics<Float> globalSumDetails = statistics.getGlobal()
                            .sumStatisticsForPeriod(period);

                        return new DeviationStatistics(
                            totalPersonalExpenses,
                            globalSumDetails.getExpected(),
                            globalSumDetails.getDeviationExpected()
                        );
                    })
                    .build()
            )
            .addAll(
                new PeriodicCriteria.Builder<DeviationStatistics>("AVERAGE_PERIOD_AMOUNT_RATIO", "AMOUNT")
                    .period(Days.ONE, Days.SEVEN, Days.days(30))
                    .calculator(new DeviationEvaluator())
                    .mapper((Days period, Transaction transaction, HistoricalData statistics) -> {
                        float alreadyInitiatedTransactions = statistics.getDebtor()
                            .expensesForPeriod(period);

                        float amountRatio = transaction.getAmount() / alreadyInitiatedTransactions;

                        MeanPeriodStatistics<Float> globalRatioDetails = statistics.getGlobal()
                            .ratioStatisticsForPeriod(period);

                        return new DeviationStatistics(
                            amountRatio,
                            globalRatioDetails.getAverage(),
                            globalRatioDetails.getDeviationAverage()
                        );
                    })
                    .build()
            )
            .add(
                new NamedCriteria.Builder<ComparableStatistics>("MAX_SPENT_AMOUNT", "AMOUNT")
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
                    .build()
            )
            .add(
                new NamedCriteria.Builder<Transaction>("AMOUNT_CATEGORY", "AMOUNT")
                    .calculator(transaction -> Arrays.stream(AmountCategoryCriteria.values())
                        .filter(category -> category.abstracts(transaction.getAmount()))
                        .findAny()
                        .orElseThrow(IllegalStateException::new))
                    .mapper((Transaction transaction, HistoricalData statistics) -> transaction)
                    .build()
            ).build();
    }
}
