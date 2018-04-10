package org.ignas.frauddetection.transactionevaluation.integration.converters;

import com.google.common.collect.ImmutableList;
import org.ignas.frauddetection.shared.Location;
import org.ignas.frauddetection.transactionevaluation.domain.Transaction;
import org.ignas.frauddetection.transactionevaluation.domain.stats.HistoricalData;
import org.ignas.frauddetection.transactionevaluation.domain.stats.details.MeanPeriodStatistics;
import org.ignas.frauddetection.transactionevaluation.domain.stats.details.MeanStatistics;
import org.ignas.frauddetection.transactionstatistics.api.request.StatisticsRequest;
import org.ignas.frauddetection.transactionstatistics.api.response.*;
import org.ignas.frauddetection.transactionstatistics.api.response.generalindicators.*;
import org.joda.time.Days;
import org.joda.time.LocalDateTime;
import org.joda.time.Seconds;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StatisticsAPIConverterTest {

    public static final Statistics PREDEFINED_STATS = new Statistics(
        new DebtorTransactionStatistics(
            new Location(54.123456f, 25.123456f),
            new Location(54.123f, 25.123f),
            555.66f,
            LocalDateTime.parse("2018-03-06T14:00:35"),
            60,
            ImmutableList.of(
                new PersonalPeriod(1, 5.21f, 2),
                new PersonalPeriod(7, 52.11f, 11)
            )
        ),
        new CredibilityScore(0.05f, 0.02f, 0.01f),
        new CredibilityScore(0.06f, 0.03f, 0.02f),
        new CredibilityScore(0.065f, 0.035f, 0.025f),
        new PublicStatistics(
            ImmutableList.of(
                new SumStatistics(1, 8.23f, 0.23f),
                new SumStatistics(7, 64.4f, 3.4f)
            ),
            ImmutableList.of(
                new RatioStatistics(1, 0.8f, 0.05f),
                new RatioStatistics(7, 0.1f, 0.15f)
            ),
            ImmutableList.of(
                new CountStatistics(1, 1.2f, 0.2f),
                new CountStatistics(7, 10.0f, 2f)
            ),
            ImmutableList.of(
                new TimeDifferenceStatistics(36000, 3600)
            ),
            ImmutableList.of(
                new DistanceDifferenceStatistics(0.24f, 0.03f)
            ),
            ImmutableList.of(
                new DistanceDifferenceStatistics(0.12f, 0.85f)
            )
        )
    );

    @Test
    void mapNullResponse() {
        HistoricalData data = StatisticsAPIConverter.mapResponse(null);

        Assertions.assertNull(data);
    }

    @Test
    void mapDebtorStatsResponse() {
        HistoricalData data = StatisticsAPIConverter.mapResponse(PREDEFINED_STATS);

        assertEquals(54.123456f, data.getDebtor().getMostUsedLocation().getLatitude());
        assertEquals(25.123456f, data.getDebtor().getMostUsedLocation().getLongtitude());

        assertEquals(54.123f, data.getDebtor().getLastTransactionLocation().getLatitude());
        assertEquals(25.123f, data.getDebtor().getLastTransactionLocation().getLongtitude());

        assertEquals(555.66f, data.getDebtor().getMostValuableTransaction());

        assertEquals(LocalDateTime.parse("2018-03-06T14:00:35"), data.getDebtor().getLastTransactionExecutionTime());

        assertEquals(Seconds.seconds(60), data.getDebtor().getShortestTimeBetweenTransactions());
    }

    @Test
    void mapDebtorPeriodicStatsResponse() {
        HistoricalData data = StatisticsAPIConverter.mapResponse(PREDEFINED_STATS);

        assertEquals(2, data.getDebtor().getNumberOfTransactionsForPeriod(Days.ONE));
        assertEquals(11, data.getDebtor().getNumberOfTransactionsForPeriod(Days.days(7)));

        assertEquals(5.21f, data.getDebtor().getExpensesForPeriod(Days.ONE));
        assertEquals(52.11f, data.getDebtor().getExpensesForPeriod(Days.days(7)));
    }

    @Test
    void mapCreditorScoreResponse() {
        HistoricalData data = StatisticsAPIConverter.mapResponse(PREDEFINED_STATS);

        assertEquals(0.05f, data.getEnvironment().getCreditor().getFraudRate());
        assertEquals(0.02f, data.getEnvironment().getCreditor().getFraudRateAverage());
        assertEquals(0.01f, data.getEnvironment().getCreditor().getFraudRateDeviation());
    }

    @Test
    void mapTimeScoreResponse() {
        HistoricalData data = StatisticsAPIConverter.mapResponse(PREDEFINED_STATS);

        assertEquals(0.06f, data.getEnvironment().getTime().getFraudRate());
        assertEquals(0.03f, data.getEnvironment().getTime().getFraudRateAverage());
        assertEquals(0.02f, data.getEnvironment().getTime().getFraudRateDeviation());
    }

    @Test
    void mapLocationScoreResponse() {
        HistoricalData data = StatisticsAPIConverter.mapResponse(PREDEFINED_STATS);

        assertEquals(0.065f, data.getEnvironment().getLocation().getFraudRate());
        assertEquals(0.035f, data.getEnvironment().getLocation().getFraudRateAverage());
        assertEquals(0.025f, data.getEnvironment().getLocation().getFraudRateDeviation());
    }

    @Test
    void mapPublicPeriodicStatsResponse() {
        HistoricalData data = StatisticsAPIConverter.mapResponse(PREDEFINED_STATS);

        MeanPeriodStatistics<Float> sumForDay = data.getGlobal().sumStatisticsForPeriod(Days.ONE);
        assertEquals(8.23f, sumForDay.getAverage().floatValue());
        assertEquals(0.23f, sumForDay.getDeviationAverage().floatValue());

        MeanPeriodStatistics<Float> sumForWeek = data.getGlobal().sumStatisticsForPeriod(Days.days(7));
        assertEquals(64.4f, sumForWeek.getAverage().floatValue());
        assertEquals(3.4f, sumForWeek.getDeviationAverage().floatValue());

        MeanPeriodStatistics<Float> ratioForDay = data.getGlobal().ratioStatisticsForPeriod(Days.ONE);
        assertEquals(0.8f, ratioForDay.getAverage().floatValue());
        assertEquals(0.05f, ratioForDay.getDeviationAverage().floatValue());

        MeanPeriodStatistics<Float> ratioForWeek = data.getGlobal().ratioStatisticsForPeriod(Days.days(7));
        assertEquals(0.1f, ratioForWeek.getAverage().floatValue());
        assertEquals(0.15f, ratioForWeek.getDeviationAverage().floatValue());

        MeanPeriodStatistics<Float> countForDay = data.getGlobal().countStatisticsForPeriod(Days.ONE);
        assertEquals(1.2f, countForDay.getAverage().floatValue());
        assertEquals(0.2f, countForDay.getDeviationAverage().floatValue());


        MeanPeriodStatistics<Float> countForWeek = data.getGlobal().countStatisticsForPeriod(Days.days(7));
        assertEquals(10.0f, countForWeek.getAverage().floatValue());
        assertEquals(2.0f, countForWeek.getDeviationAverage().floatValue());
    }

    @Test
    void mapPublicStatsResponse() {
        HistoricalData data = StatisticsAPIConverter.mapResponse(PREDEFINED_STATS);

        MeanStatistics<Float> commonDistance = data.getGlobal().getDistanceToCommonLocation();
        assertEquals(0.24f, commonDistance.getAverage().floatValue());
        assertEquals(0.03f, commonDistance.getDeviationAverage().floatValue());

        MeanStatistics<Float> lastLocation = data.getGlobal().getDistanceToLastLocation();
        assertEquals(0.12f, lastLocation.getAverage().floatValue());
        assertEquals(0.85f, lastLocation.getDeviationAverage().floatValue());


        MeanStatistics<Seconds> timeDiff = data.getGlobal().getTimeDifference();
        assertEquals(Seconds.seconds(36000), timeDiff.getAverage());
        assertEquals(Seconds.seconds(3600), timeDiff.getDeviationAverage());
    }


    @Test
    void mapNullRequest() {
        StatisticsRequest request = StatisticsAPIConverter.mapRequest(null);

        assertNull(request);
    }

    @Test
    void mapRequest() {
        Transaction transaction = new Transaction(
            "1234",
            123.45f,
            "AnyDebtorAccountID",
            "AnyCreditorAccountID",
            new Location(54.123456f, 25.123456f),
            LocalDateTime.parse("2018-03-03T15:12:34")
        );

        StatisticsRequest result = StatisticsAPIConverter.mapRequest(transaction);

        assertEquals(LocalDateTime.parse("2018-03-03T15:12:34"), result.getRequestByTime());
        assertEquals("AnyDebtorAccountID", result.getRequestByDebtorId());
        assertEquals("AnyCreditorAccountID", result.getRequestByCreditorId());
        assertEquals(54.123456f, result.getRequestByLocation().getLatitude());
        assertEquals(25.123456f, result.getRequestByLocation().getLongtitude());
    }
}
