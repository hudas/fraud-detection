package org.ignas.frauddetection.transactionevaluation.domain.config;

import com.google.common.collect.ImmutableList;
import org.ignas.frauddetection.shared.Location;
import org.ignas.frauddetection.transactionevaluation.domain.Transaction;
import org.ignas.frauddetection.transactionevaluation.domain.calculation.PrintableResult;
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

import static com.google.common.collect.ImmutableList.of;
import static org.junit.jupiter.api.Assertions.*;

class FraudCriteriaEvaluatorTest {

    @Test
    void evaluateAll() {
    }

    @Test
    void evaluateNonExistingCriterion() {
        FraudCriteriaEvaluator evaluator = new FraudCriteriaEvaluator();

        try {
            PrintableResult result = evaluator.evaluate("Any_criteria", null, null);
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

        PrintableResult day = evaluator.evaluate("AVERAGE_SUM/P1D", transaction, data);
        PrintableResult week = evaluator.evaluate("AVERAGE_SUM/P7D", transaction, data);
        PrintableResult month = evaluator.evaluate("AVERAGE_SUM/P30D", transaction, data);

        Assertions.assertEquals("LESS_THAN_EXPECTED", day.representation());
        Assertions.assertEquals("MUCH_MORE_THAN_EXPECTED", week.representation());
        Assertions.assertEquals("MORE_THAN_EXPECTED", month.representation());
    }

    @Test
    void evaluateExpectedSum() {
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
                    MeanPeriodStatistics.<Float>builder(Days.days(1)).expectedValues(15f, 5f).build(),
                    MeanPeriodStatistics.<Float>builder(Days.days(7)).expectedValues(260f, 50f).build(),
                    MeanPeriodStatistics.<Float>builder(Days.days(30)).expectedValues(740f, 100f).build()
                ),
                of(),
                of(),
                null,
                null,
                null
            ),
            null
        );

        PrintableResult day = evaluator.evaluate("EXPECTED_SUM/P1D", transaction, data);
        PrintableResult week = evaluator.evaluate("EXPECTED_SUM/P7D", transaction, data);
        PrintableResult month = evaluator.evaluate("EXPECTED_SUM/P30D", transaction, data);

        Assertions.assertEquals("EXPECTED", day.representation());
        Assertions.assertEquals("EXPECTED", week.representation());
        Assertions.assertEquals("EXPECTED", month.representation());
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

        PrintableResult day = evaluator.evaluate("AVERAGE_COUNT/P1D", transaction, data);
        PrintableResult week = evaluator.evaluate("AVERAGE_COUNT/P7D", transaction, data);
        PrintableResult month = evaluator.evaluate("AVERAGE_COUNT/P30D", transaction, data);

        Assertions.assertEquals("MORE_THAN_EXPECTED", day.representation());
        Assertions.assertEquals("MUCH_LESS_THAN_EXPECTED", week.representation());
        Assertions.assertEquals("EXPECTED", month.representation());
    }


    @Test
    void evaluateExpectedCount() {
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
                    MeanPeriodStatistics.<Float>builder(Days.days(1)).expectedValues(0.7f, 0.3f).build(),
                    MeanPeriodStatistics.<Float>builder(Days.days(7)).expectedValues(6f, 2f).build(),
                    MeanPeriodStatistics.<Float>builder(Days.days(30)).expectedValues(30f, 5f).build()
                ),
                null,
                null,
                null
            ),
            null
        );

        PrintableResult day = evaluator.evaluate("EXPECTED_COUNT/P1D", transaction, data);
        PrintableResult week = evaluator.evaluate("EXPECTED_COUNT/P7D", transaction, data);
        PrintableResult month = evaluator.evaluate("EXPECTED_COUNT/P30D", transaction, data);

        Assertions.assertEquals("EXPECTED", day.representation());
        Assertions.assertEquals("EXPECTED", week.representation());
        Assertions.assertEquals("EXPECTED", month.representation());
    }


    @Test
    void evaluateAmountRation() {
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
                    new PersonalPeriodStatistics(Days.days(7), 300f, 4),
                    new PersonalPeriodStatistics(Days.days(30), 650f, 25)
                )
            ),
            new GlobalStatistics(
                of(),
                of(
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

        PrintableResult week = evaluator.evaluate("AVERAGE_PERIOD_AMOUNT_RATIO/P7D", transaction, data);
        PrintableResult month = evaluator.evaluate("AVERAGE_PERIOD_AMOUNT_RATIO/P30D", transaction, data);

        Assertions.assertEquals("LESS_THAN_EXPECTED", week.representation());
        Assertions.assertEquals("EXPECTED", month.representation());
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

        PrintableResult smaller = evaluator.evaluate("MAX_SPENT_AMOUNT", smallTransaction, data);
        PrintableResult bigger = evaluator.evaluate("MAX_SPENT_AMOUNT", bigTransaction, data);
        PrintableResult same = evaluator.evaluate("MAX_SPENT_AMOUNT", sameTransaction, data);

        Assertions.assertEquals("FALSE", smaller.representation());
        Assertions.assertEquals("TRUE", bigger.representation());
        Assertions.assertEquals("FALSE", same.representation());
    }

    @Test
    void evaluateAmountCategory() {
        FraudCriteriaEvaluator evaluator = new FraudCriteriaEvaluator();

        Transaction smallTransaction = new Transaction("any", 25f, null, null, null, null);
        Transaction bigTransaction = new Transaction("any", 100f, null, null, null, null);
        Transaction biggerTransaction = new Transaction("any", 500f, null, null, null, null);
        Transaction veryBigTransaction = new Transaction("any", 1000f, null, null, null, null);


        HistoricalData data = new HistoricalData(null, null, null);

        PrintableResult small = evaluator.evaluate("AMOUNT_CATEGORY", smallTransaction, data);
        PrintableResult big = evaluator.evaluate("AMOUNT_CATEGORY", bigTransaction, data);
        PrintableResult bigger = evaluator.evaluate("AMOUNT_CATEGORY", biggerTransaction, data);
        PrintableResult veryBig = evaluator.evaluate("AMOUNT_CATEGORY", veryBigTransaction, data);

        Assertions.assertEquals("VERY_SMALL_AMOUNT", small.representation());
        Assertions.assertEquals("SMALL_AMOUNT", big.representation());
        Assertions.assertEquals("BIG_AMOUNT", bigger.representation());
        Assertions.assertEquals("VERY_BIG_AMOUNT", veryBig.representation());
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

        PrintableResult small = evaluator.evaluate("TIME_RISK", transaction, data);

        Assertions.assertEquals("MUCH_MORE_THAN_EXPECTED", small.representation());
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

        PrintableResult morning = evaluator.evaluate("MIN_TIME_BETWEEN_TRANSACTIONS", morningTransaction, data);
        PrintableResult evening = evaluator.evaluate("MIN_TIME_BETWEEN_TRANSACTIONS", eveningTransaction, data);

        Assertions.assertEquals("TRUE", morning.representation());
        Assertions.assertEquals("FALSE", evening.representation());
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

        PrintableResult morning = evaluator.evaluate("AVERAGE_TIME_BETWEEN_TRANSACTIONS", morningTransaction, data);
        PrintableResult evening = evaluator.evaluate("AVERAGE_TIME_BETWEEN_TRANSACTIONS", eveningTransaction, data);

        Assertions.assertEquals("MUCH_LESS_THAN_EXPECTED", morning.representation());
        Assertions.assertEquals("MUCH_MORE_THAN_EXPECTED", evening.representation());
    }


    @Test
    void evaluateExpectedTimeBetweenTransactions() {
        FraudCriteriaEvaluator evaluator = new FraudCriteriaEvaluator();

        Transaction morningTransaction = new Transaction(
            "any",
            25f,
            null,
            null,
            null,
            LocalDateTime.parse("2018-03-20T12:11:52")
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
                MeanStatistics.<Seconds>builder().expectedValues(Seconds.seconds(24000), Seconds.seconds(7200)).build(),
                null,
                null
            ),
            null
        );

        PrintableResult morning = evaluator.evaluate("EXPECTED_TIME_BETWEEN_TRANSACTIONS", morningTransaction, data);
        PrintableResult evening = evaluator.evaluate("EXPECTED_TIME_BETWEEN_TRANSACTIONS", eveningTransaction, data);

        Assertions.assertEquals("LESS_THAN_EXPECTED", morning.representation());
        Assertions.assertEquals("MORE_THAN_EXPECTED", evening.representation());
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

        PrintableResult result = evaluator.evaluate("LOCATION_RISK", transaction, data);

        Assertions.assertEquals("LESS_THAN_EXPECTED", result.representation());
    }

    @Test
    void evaluateCommonLocationRisk() {
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
                new Location(54.123456f, 25.456123f),
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

        PrintableResult result = evaluator.evaluate("AVERAGE_DISTANCE_FROM_COMMON_LOCATION", morningTransaction, data);

        Assertions.assertEquals("EXPECTED", result.representation());
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

        PrintableResult result = evaluator.evaluate("AVERAGE_DISTANCE_FROM_LAST_LOCATION", morningTransaction, data);

        Assertions.assertEquals("MUCH_LESS_THAN_EXPECTED", result.representation());
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

        PrintableResult result = evaluator.evaluate("CREDITOR_RISK", transaction, data);

        Assertions.assertEquals("LESS_THAN_EXPECTED", result.representation());
    }

    @Test
    void resolveGroup() {
    }
}
