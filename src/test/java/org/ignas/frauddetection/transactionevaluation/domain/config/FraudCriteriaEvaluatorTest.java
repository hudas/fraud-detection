package org.ignas.frauddetection.transactionevaluation.domain.config;

import org.ignas.frauddetection.shared.Location;
import org.ignas.frauddetection.transactionevaluation.domain.Transaction;
import org.ignas.frauddetection.transactionevaluation.domain.calculation.EvaluationResult;
import org.ignas.frauddetection.transactionevaluation.domain.stats.DebtorStatistics;
import org.ignas.frauddetection.transactionevaluation.domain.stats.EnvironmentStatistics;
import org.ignas.frauddetection.transactionevaluation.domain.stats.GlobalStatistics;
import org.ignas.frauddetection.transactionevaluation.domain.stats.HistoricalData;
import org.ignas.frauddetection.transactionevaluation.domain.stats.details.FraudRate;
import org.ignas.frauddetection.transactionevaluation.domain.stats.details.MeanPeriodStatistics;
import org.ignas.frauddetection.transactionevaluation.domain.stats.details.MeanStatistics;
import org.ignas.frauddetection.transactionevaluation.domain.stats.details.PersonalPeriodStatistics;
import org.joda.time.Days;
import org.joda.time.LocalDateTime;
import org.joda.time.Seconds;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static com.google.common.collect.ImmutableList.of;

class FraudCriteriaEvaluatorTest {

    @Test
    void evaluateNonExistingCriterion() {
        FraudCriteriaEvaluator evaluator = new FraudCriteriaEvaluator();

        try {
            EvaluationResult result = evaluator.evaluate("Any_criteria", null, null);
            Assertions.fail("Exception was not thrown");
        } catch (IllegalArgumentException ex) {

        }
    }

    @Test
    void evaluateSum() {
        FraudCriteriaEvaluator evaluator = new FraudCriteriaEvaluator();

        Transaction transaction = new Transaction("any", 1f, null, null, null, null);
        HistoricalData data = new HistoricalData(
            new DebtorStatistics(
                null,
                null,
                0,
                null,
                null,
                of(
                    new PersonalPeriodStatistics(Days.days(1), 10f, 2),
                    new PersonalPeriodStatistics(Days.days(7), 300f, 5),
                    new PersonalPeriodStatistics(Days.days(30), 650f, 20)
                )
            ),
            new GlobalStatistics(
                of(
                    MeanPeriodStatistics.<Float>builder(Days.days(1)).pastValues(20f, 5f).build(),
                    MeanPeriodStatistics.<Float>builder(Days.days(7)).pastValues(100f, 25f).build(),
                    MeanPeriodStatistics.<Float>builder(Days.days(30)).pastValues(500f, 100f).build()
                ),
                of(),
                of(),
                null,
                null,
                null
            ),
            null
        );

        EvaluationResult day = evaluator.evaluate("AVERAGE_SUM/P1D", transaction, data);
        EvaluationResult week = evaluator.evaluate("AVERAGE_SUM/P7D", transaction, data);
        EvaluationResult month = evaluator.evaluate("AVERAGE_SUM/P30D", transaction, data);

        Assertions.assertEquals("LESS_THAN_EXPECTED", day.getResult().representation());
        Assertions.assertNull(day.getRawResult());

        Assertions.assertEquals("MUCH_MORE_THAN_EXPECTED", week.getResult().representation());
        Assertions.assertNull(week.getRawResult());

        Assertions.assertEquals("MORE_THAN_EXPECTED", month.getResult().representation());
        Assertions.assertNull(month.getRawResult());
    }


    @Test
    void evaluateCount() {
        FraudCriteriaEvaluator evaluator = new FraudCriteriaEvaluator();

        Transaction transaction = new Transaction("any", 1f, null, null, null, null);
        HistoricalData data = new HistoricalData(
            new DebtorStatistics(
                null,
                null,
                0,
                null,
                null,
                of(
                    new PersonalPeriodStatistics(Days.days(1), 10f, 0),
                    new PersonalPeriodStatistics(Days.days(7), 300f, 4),
                    new PersonalPeriodStatistics(Days.days(30), 650f, 25)
                )
            ),
            new GlobalStatistics(
                of(),
                of(),
                of(
                    MeanPeriodStatistics.<Float>builder(Days.days(1)).pastValues(0.5f, 0.3f).build(),
                    MeanPeriodStatistics.<Float>builder(Days.days(7)).pastValues(10f, 2f).build(),
                    MeanPeriodStatistics.<Float>builder(Days.days(30)).pastValues(30f, 5f).build()
                ),
                null,
                null,
                null
            ),
            null
        );

        EvaluationResult day = evaluator.evaluate("AVERAGE_COUNT/P1D", transaction, data);
        EvaluationResult week = evaluator.evaluate("AVERAGE_COUNT/P7D", transaction, data);
        EvaluationResult month = evaluator.evaluate("AVERAGE_COUNT/P30D", transaction, data);

        Assertions.assertEquals("MORE_THAN_EXPECTED", day.getResult().representation());
        Assertions.assertNull(day.getRawResult());

        Assertions.assertEquals("MUCH_LESS_THAN_EXPECTED", week.getResult().representation());
        Assertions.assertNull(week.getRawResult());

        Assertions.assertEquals("EXPECTED", month.getResult().representation());
        Assertions.assertNull(month.getRawResult());
    }

    @Test
    void evaluateAmountRatio() {
        FraudCriteriaEvaluator evaluator = new FraudCriteriaEvaluator();

        Transaction transaction = new Transaction("any", 15f, null, null, null, null);
        HistoricalData data = new HistoricalData(
            new DebtorStatistics(
                null,
                null,
                0,
                null,
                null,
                of(
                    new PersonalPeriodStatistics(Days.days(1), 30f, 1),
                    new PersonalPeriodStatistics(Days.days(7), 300f, 4),
                    new PersonalPeriodStatistics(Days.days(30), 650f, 25)
                )
            ),
            new GlobalStatistics(
                of(),
                of(
                    MeanPeriodStatistics.<Float>builder(Days.days(1)).pastValues(0.8f, 0.2f).build(),
                    MeanPeriodStatistics.<Float>builder(Days.days(7)).pastValues(0.1f, 0.05f).build(),
                    MeanPeriodStatistics.<Float>builder(Days.days(30)).pastValues(0.03f, 0.01f).build()
                ),
                of(),
                null,
                null,
                null
            ),
            null
        );

        EvaluationResult day = evaluator.evaluate("AVERAGE_PERIOD_AMOUNT_RATIO/P1D", transaction, data);
        EvaluationResult week = evaluator.evaluate("AVERAGE_PERIOD_AMOUNT_RATIO/P7D", transaction, data);
        EvaluationResult month = evaluator.evaluate("AVERAGE_PERIOD_AMOUNT_RATIO/P30D", transaction, data);

        Assertions.assertEquals("MUCH_LESS_THAN_EXPECTED", day.getResult().representation());
        Assertions.assertEquals(0.33f, day.getRawResult(), 0.01f);

        Assertions.assertEquals("LESS_THAN_EXPECTED", week.getResult().representation());
        Assertions.assertEquals(0.047f, week.getRawResult(), 0.001f);

        Assertions.assertEquals("EXPECTED", month.getResult().representation());
        Assertions.assertEquals(0.022f, month.getRawResult(), 0.001f);
    }


    @Test
    void evaluateMaxSpentAmount() {
        FraudCriteriaEvaluator evaluator = new FraudCriteriaEvaluator();

        Transaction smallTransaction = new Transaction("any", 15f, null, null, null, null);
        Transaction bigTransaction = new Transaction("any", 1155f, null, null, null, null);
        Transaction sameTransaction = new Transaction("any", 1150f, null, null, null, null);

        HistoricalData data = new HistoricalData(
            new DebtorStatistics(
                null,
                null,
                1150,
                null,
                null,
                of()
            ),
            new GlobalStatistics(
                of(),
                of(),
                of(),
                null,
                null,
                null
            ),
            null
        );

        EvaluationResult smaller = evaluator.evaluate("MAX_SPENT_AMOUNT", smallTransaction, data);
        EvaluationResult bigger = evaluator.evaluate("MAX_SPENT_AMOUNT", bigTransaction, data);
        EvaluationResult same = evaluator.evaluate("MAX_SPENT_AMOUNT", sameTransaction, data);

        Assertions.assertEquals("FALSE", smaller.getResult().representation());
        Assertions.assertNull(smaller.getRawResult());

        Assertions.assertEquals("TRUE", bigger.getResult().representation());
        Assertions.assertNull(smaller.getRawResult());

        Assertions.assertEquals("FALSE", same.getResult().representation());
        Assertions.assertNull(smaller.getRawResult());
    }

    @Test
    void evaluateAmountCategory() {
        FraudCriteriaEvaluator evaluator = new FraudCriteriaEvaluator();

        Transaction smallTransaction = new Transaction("any", 25f, null, null, null, null);
        Transaction bigTransaction = new Transaction("any", 100f, null, null, null, null);
        Transaction biggerTransaction = new Transaction("any", 500f, null, null, null, null);
        Transaction veryBigTransaction = new Transaction("any", 1000f, null, null, null, null);


        HistoricalData data = new HistoricalData(null, null, null);

        EvaluationResult small = evaluator.evaluate("AMOUNT_CATEGORY", smallTransaction, data);
        EvaluationResult big = evaluator.evaluate("AMOUNT_CATEGORY", bigTransaction, data);
        EvaluationResult bigger = evaluator.evaluate("AMOUNT_CATEGORY", biggerTransaction, data);
        EvaluationResult veryBig = evaluator.evaluate("AMOUNT_CATEGORY", veryBigTransaction, data);

        Assertions.assertEquals("VERY_SMALL_AMOUNT", small.getResult().representation());
        Assertions.assertNull(small.getRawResult());

        Assertions.assertEquals("SMALL_AMOUNT", big.getResult().representation());
        Assertions.assertNull(big.getRawResult());

        Assertions.assertEquals("BIG_AMOUNT", bigger.getResult().representation());
        Assertions.assertNull(bigger.getRawResult());

        Assertions.assertEquals("VERY_BIG_AMOUNT", veryBig.getResult().representation());
        Assertions.assertNull(veryBig.getRawResult());
    }


    @Test
    void evaluateTimeRisk() {
        FraudCriteriaEvaluator evaluator = new FraudCriteriaEvaluator();

        Transaction transaction = new Transaction("any", 25f, null, null, null, null);

        HistoricalData data = new HistoricalData(
            null,
            null,
            new EnvironmentStatistics(null, null, new FraudRate(0.005f, 0.002f, 0.001f))
        );

        EvaluationResult timeRisk = evaluator.evaluate("TIME_RISK", transaction, data);

        Assertions.assertEquals("MUCH_MORE_THAN_EXPECTED", timeRisk.getResult().representation());
        Assertions.assertNull(timeRisk.getRawResult());
    }


    @Test
    void evaluateMinTimeBetweenTransactionsRisk() {
        FraudCriteriaEvaluator evaluator = new FraudCriteriaEvaluator();

        Transaction morningTransaction = new Transaction(
            "any",
            25f,
            null,
            null,
            null,
            LocalDateTime.parse("2018-03-20T08:11:52")
        );

        Transaction eveningTransaction = new Transaction(
            "any",
            25f,
            null,
            null,
            null,
            LocalDateTime.parse("2018-03-20T17:11:52")
        );

        HistoricalData data = new HistoricalData(
            new DebtorStatistics(
                null,
                null,
                0f,
                LocalDateTime.parse("2018-03-20T08:10:03"),
                Seconds.seconds(120),
                of()
            ),
            null,
            null
        );

        EvaluationResult morning = evaluator.evaluate("MIN_TIME_BETWEEN_TRANSACTIONS", morningTransaction, data);
        EvaluationResult evening = evaluator.evaluate("MIN_TIME_BETWEEN_TRANSACTIONS", eveningTransaction, data);

        Assertions.assertEquals("TRUE", morning.getResult().representation());
        Assertions.assertEquals(109f, morning.getRawResult(), 0.001f);

        Assertions.assertEquals("FALSE", evening.getResult().representation());
        Assertions.assertEquals(32509f, evening.getRawResult(), 0.001f);
    }


    @Test
    void evaluateTimeBetweenTransactions() {
        FraudCriteriaEvaluator evaluator = new FraudCriteriaEvaluator();

        Transaction morningTransaction = new Transaction(
            "any",
            25f,
            null,
            null,
            null,
            LocalDateTime.parse("2018-03-20T08:11:52")
        );

        Transaction eveningTransaction = new Transaction(
            "any",
            25f,
            null,
            null,
            null,
            LocalDateTime.parse("2018-03-20T17:11:52")
        );

        HistoricalData data = new HistoricalData(
            new DebtorStatistics(
                null,
                null,
                0f,
                LocalDateTime.parse("2018-03-20T08:10:03"),
                null,
                of()
            ),
            new GlobalStatistics(
                of(),
                of(),
                of(),
                MeanStatistics.<Seconds>builder().pastValues(Seconds.seconds(18000), Seconds.seconds(7200)).build(),
                null,
                null
            ),
            null
        );

        EvaluationResult morning = evaluator.evaluate("AVERAGE_TIME_BETWEEN_TRANSACTIONS", morningTransaction, data);
        EvaluationResult evening = evaluator.evaluate("AVERAGE_TIME_BETWEEN_TRANSACTIONS", eveningTransaction, data);

        Assertions.assertEquals("MUCH_LESS_THAN_EXPECTED", morning.getResult().representation());
        Assertions.assertNull(morning.getRawResult());

        Assertions.assertEquals("MUCH_MORE_THAN_EXPECTED", evening.getResult().representation());
        Assertions.assertNull(morning.getRawResult());
    }

    @Test
    void evaluateLocationRisk() {
        FraudCriteriaEvaluator evaluator = new FraudCriteriaEvaluator();

        Transaction transaction = new Transaction(
            "any",
            25f,
            null,
            null,
            null,
            null
        );

        HistoricalData data = new HistoricalData(
            null,
            null,
            new EnvironmentStatistics(
                null,
                new FraudRate(0.0001f, 0.0002f, 0.00005f),
                null
            )
        );

        EvaluationResult result = evaluator.evaluate("LOCATION_RISK", transaction, data);

        Assertions.assertEquals("LESS_THAN_EXPECTED", result.getResult().representation());
        Assertions.assertNull(result.getRawResult());
    }

    @Test
    void evaluateCommonLocationRisk() {
        FraudCriteriaEvaluator evaluator = new FraudCriteriaEvaluator();

        Transaction morningTransaction = new Transaction(
            "any",
            25f,
            null,
            null,
            new Location(54.123456f, 25.456123f),
            null
        );

        HistoricalData data = new HistoricalData(
            new DebtorStatistics(
                new Location(54.0000f, 25.0000f),
                null,
                0f,
                null,
                null,
                of()
            ),
            new GlobalStatistics(
                of(),
                of(),
                of(),
                null,
                MeanStatistics.<Float>builder().pastValues(0.5f, 0.2f).build(),
                null
            ),
            null
        );

        EvaluationResult result = evaluator.evaluate("AVERAGE_DISTANCE_FROM_COMMON_LOCATION", morningTransaction, data);

        Assertions.assertEquals("EXPECTED", result.getResult().representation());
        Assertions.assertEquals(result.getRawResult(), 0.47f, 0.01f);
    }

    @Test
    void evaluateLastLocationRisk() {
        FraudCriteriaEvaluator evaluator = new FraudCriteriaEvaluator();

        Transaction morningTransaction = new Transaction(
            "any",
            25f,
            null,
            null,
            new Location(54.0000f, 25.0000f),
            null
        );

        HistoricalData data = new HistoricalData(
            new DebtorStatistics(
                null,
                new Location(54.0500f, 25.0500f),
                0f,
                null,
                null,
                of()
            ),
            new GlobalStatistics(
                of(),
                of(),
                of(),
                null,
                null,
                MeanStatistics.<Float>builder().pastValues(0.5f, 0.2f).build()
            ),
            null
        );

        EvaluationResult result = evaluator.evaluate("AVERAGE_DISTANCE_FROM_LAST_LOCATION", morningTransaction, data);

        Assertions.assertEquals("MUCH_LESS_THAN_EXPECTED", result.getResult().representation());
        Assertions.assertEquals(result.getRawResult(), 0.07f, 0.01f);
    }

    @Test
    void evaluateCreditorRisk() {
        FraudCriteriaEvaluator evaluator = new FraudCriteriaEvaluator();

        Transaction transaction = new Transaction(
            "any",
            25f,
            null,
            null,
            null,
            null
        );

        HistoricalData data = new HistoricalData(
            null,
            null,
            new EnvironmentStatistics(
                new FraudRate(0.0001f, 0.0002f, 0.00005f),
                null,
                null
            )
        );

        EvaluationResult result = evaluator.evaluate("CREDITOR_RISK", transaction, data);

        Assertions.assertEquals("LESS_THAN_EXPECTED", result.getResult().representation());
        Assertions.assertNull(result.getRawResult());
    }

    @Test
    void resolveGroup() {
        FraudCriteriaEvaluator evaluator = new FraudCriteriaEvaluator();

        String averageSumGroupDay = evaluator.resolveGroup("AVERAGE_SUM/P1D");
        String averageSumGroupWeek = evaluator.resolveGroup("AVERAGE_SUM/P7D");
        String averageSumGroupMonth = evaluator.resolveGroup("AVERAGE_SUM/P30D");

        String averageCountGroup = evaluator.resolveGroup("AVERAGE_COUNT/P1D");
        String averageRatioGroup = evaluator.resolveGroup("AVERAGE_PERIOD_AMOUNT_RATIO/P7D");
        String maxAmountGroup = evaluator.resolveGroup("MAX_SPENT_AMOUNT");
        String categoryGroup = evaluator.resolveGroup("AMOUNT_CATEGORY");

        String timeGroup = evaluator.resolveGroup("TIME_RISK");
        String minTimeBetweenGroup = evaluator.resolveGroup("MIN_TIME_BETWEEN_TRANSACTIONS");
        String averageTimeBetweenGroup = evaluator.resolveGroup("AVERAGE_TIME_BETWEEN_TRANSACTIONS");

        String locationGroup = evaluator.resolveGroup("LOCATION_RISK");
        String averageDistanceCommonGroup = evaluator.resolveGroup("AVERAGE_DISTANCE_FROM_COMMON_LOCATION");
        String averageDistanceLastGroup = evaluator.resolveGroup("AVERAGE_DISTANCE_FROM_LAST_LOCATION");
        String creditorGroup = evaluator.resolveGroup("CREDITOR_RISK");

        Assertions.assertEquals("AMOUNT", averageSumGroupDay);
        Assertions.assertEquals("AMOUNT", averageSumGroupWeek);
        Assertions.assertEquals("AMOUNT", averageSumGroupMonth);
        Assertions.assertEquals("AMOUNT", averageRatioGroup);
        Assertions.assertEquals("AMOUNT", maxAmountGroup);
        Assertions.assertEquals("AMOUNT", categoryGroup);

        Assertions.assertEquals("COUNT", averageCountGroup);


        Assertions.assertEquals("TIME", timeGroup);
        Assertions.assertEquals("TIME", minTimeBetweenGroup);
        Assertions.assertEquals("TIME", averageTimeBetweenGroup);

        Assertions.assertEquals("LOCATION", locationGroup);
        Assertions.assertEquals("LOCATION", averageDistanceCommonGroup);
        Assertions.assertEquals("LOCATION", averageDistanceLastGroup);
        Assertions.assertEquals("LOCATION", creditorGroup);
    }

    @Test
    void evaluateAll() {
        Transaction transaction = new Transaction(
            "any",
            1f,
            "LTDebtor",
            "LTCreditor",
            new Location(54.0000f, 25.0000f),
            LocalDateTime.parse("2018-03-20T10:45:31")
        );

        HistoricalData data = new HistoricalData(
            new DebtorStatistics(
                new Location(54.1234f, 25.1234f),
                new Location(54.1234f, 25.1234f),
                1150f,
                LocalDateTime.parse("2018-03-20T08:00:11"),
                Seconds.seconds(1800),
                of(
                    new PersonalPeriodStatistics(Days.days(1), 10f, 2),
                    new PersonalPeriodStatistics(Days.days(7), 300f, 5),
                    new PersonalPeriodStatistics(Days.days(30), 650f, 20)
                )
            ),
            new GlobalStatistics(
                of(
                    MeanPeriodStatistics.<Float>builder(Days.days(1)).pastValues(20f, 5f).build(),
                    MeanPeriodStatistics.<Float>builder(Days.days(7)).pastValues(100f, 25f).build(),
                    MeanPeriodStatistics.<Float>builder(Days.days(30)).pastValues(500f, 100f).build()
                ),
                of(
                    MeanPeriodStatistics.<Float>builder(Days.days(1)).pastValues(0.8f, 0.3f).build(),
                    MeanPeriodStatistics.<Float>builder(Days.days(7)).pastValues(0.1f, 0.05f).build(),
                    MeanPeriodStatistics.<Float>builder(Days.days(30)).pastValues(0.03f, 0.01f).build()
                ),
                of(
                    MeanPeriodStatistics.<Float>builder(Days.days(1)).pastValues(0.5f, 0.3f).build(),
                    MeanPeriodStatistics.<Float>builder(Days.days(7)).pastValues(10f, 2f).build(),
                    MeanPeriodStatistics.<Float>builder(Days.days(30)).pastValues(30f, 5f).build()
                ),
                MeanStatistics.<Seconds>builder().pastValues(Seconds.seconds(18000), Seconds.seconds(7200)).build(),
                MeanStatistics.<Float>builder().pastValues(0.5f, 0.2f).build(),
                MeanStatistics.<Float>builder().pastValues(0.5f, 0.2f).build()
            ),
            new EnvironmentStatistics(
                new FraudRate(0.0001f, 0.0002f, 0.00005f),
                new FraudRate(0.0001f, 0.0002f, 0.00005f),
                new FraudRate(0.005f, 0.002f, 0.001f)
            )
        );

        FraudCriteriaEvaluator evaluator = new FraudCriteriaEvaluator();

        Map<String, EvaluationResult> result = evaluator.evaluateAll(transaction, data);

        Assertions.assertEquals(18, result.size());
    }
}
