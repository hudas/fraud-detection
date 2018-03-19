package org.ignas.frauddetection.transactionevaluation.integration.converters;

import org.ignas.frauddetection.transactionevaluation.domain.Transaction;
import org.ignas.frauddetection.transactionevaluation.domain.stats.DebtorStatistics;
import org.ignas.frauddetection.transactionevaluation.domain.stats.EnvironmentStatistics;
import org.ignas.frauddetection.transactionevaluation.domain.stats.GlobalStatistics;
import org.ignas.frauddetection.transactionevaluation.domain.stats.HistoricalData;
import org.ignas.frauddetection.transactionevaluation.domain.stats.details.FraudRate;
import org.ignas.frauddetection.transactionevaluation.domain.stats.details.MeanPeriodStatistics;
import org.ignas.frauddetection.transactionevaluation.domain.stats.details.MeanStatistics;
import org.ignas.frauddetection.transactionevaluation.domain.stats.details.PersonalPeriodStatistics;
import org.ignas.frauddetection.transactionstatistics.api.request.StatisticsRequest;
import org.ignas.frauddetection.transactionstatistics.api.response.CredibilityScore;
import org.ignas.frauddetection.transactionstatistics.api.response.PersonalPeriod;
import org.ignas.frauddetection.transactionstatistics.api.response.Statistics;
import org.ignas.frauddetection.transactionstatistics.api.response.generalindicators.DistanceDifferenceStatistics;
import org.ignas.frauddetection.transactionstatistics.api.response.generalindicators.TimeDifferenceStatistics;
import org.joda.time.Days;
import org.joda.time.Seconds;

import java.util.List;

import static java.util.stream.Collectors.toList;

public class StatisticsAPIConverter {

    public static HistoricalData mapResponse(Statistics historicalData) {
        GlobalStatistics globalStats = new GlobalStatistics(
            sumStatsFromDTO(historicalData),
            ratioStatsFromDTO(historicalData),
            countStatsFromDTO(historicalData),
            timeStatsFromDTO(historicalData),
            distanceStatsFromDTO(historicalData),
            distanceStatsFromDTO(historicalData)
        );

        EnvironmentStatistics environmentStats = new EnvironmentStatistics(
            fraudRateFromDTO(historicalData.getCreditorScore()),
            fraudRateFromDTO(historicalData.getLocationScore()),
            fraudRateFromDTO(historicalData.getTimeScore())
        );

        DebtorStatistics debtorStats = debtorFromDTO(historicalData);

        return new HistoricalData(debtorStats, globalStats, environmentStats);
    }

    public static StatisticsRequest mapRequest(Transaction transactionData) {
        return new StatisticsRequest(
            transactionData.getDebtor(),
            transactionData.getCreditor(),
            transactionData.getLocation(),
            transactionData.getTime()
        );
    }


    private static DebtorStatistics debtorFromDTO(Statistics statistics) {
        return new DebtorStatistics(
            statistics.getDebtorStatistics().getMostUsedLocation(),
            statistics.getDebtorStatistics().getLastTransactionLocation(),
            statistics.getDebtorStatistics().getMostValuableTransaction(),
            statistics.getDebtorStatistics().getLastTransactionExecutionTime(),
            Seconds.seconds(statistics.getDebtorStatistics().getMinTimeBetweenTransactions()),
            statistics.getDebtorStatistics().getPeriodicStatistics().stream()
                .map(StatisticsAPIConverter::personalPeriodFromDTO)
                .collect(toList())
        );
    }



    private static PersonalPeriodStatistics personalPeriodFromDTO(PersonalPeriod stats) {
        return new PersonalPeriodStatistics(
            Days.days(stats.getPeriodLength()),
            stats.getExpensesSum(),
            stats.getTransactionCount()
        );
    }


    private static FraudRate fraudRateFromDTO(CredibilityScore creditorScore) {
        return new FraudRate(
            creditorScore.getFraudRate(),
            creditorScore.getFraudRateAverage(),
            creditorScore.getFraudRateDeviation()
        );
    }

    /**
     * That's not DRY for sure.
     * In order to keep payload in event bus low we do not introduce any abstractions to DTO's which could help keep it DRY
     *
     * @param statistics
     * @return
     */
    private static MeanStatistics<Float> distanceStatsFromDTO(Statistics statistics) {
        DistanceDifferenceStatistics stats = statistics.getPublicStatistics().getDistance()
            .stream()
            .findFirst()
            .orElseThrow(IllegalStateException::new);

        return MeanStatistics.<Float>builder()
            .pastValues(stats.getAverage(), stats.getDeviationAverage())
            .build();
    }

    private static MeanStatistics<Seconds> timeStatsFromDTO(Statistics statistics) {
        TimeDifferenceStatistics stats = statistics.getPublicStatistics().getTime()
            .stream()
            .findFirst()
            .orElseThrow(IllegalStateException::new);

        return MeanStatistics.<Seconds>builder()
            .expectedValues(Seconds.seconds(stats.getExpected()), Seconds.seconds(stats.getDeviationExpected()))
            .pastValues(Seconds.seconds(stats.getAverage()), Seconds.seconds(stats.getDeviationAverage()))
            .build();
    }

    private static List<MeanPeriodStatistics<Float>> ratioStatsFromDTO(Statistics historicalData) {
        return historicalData.getPublicStatistics()
            .getSumRatio()
            .stream()
            .map(ratio -> MeanPeriodStatistics.<Float>builder(Days.days(ratio.getPeriodLength()))
                .pastValues(ratio.getAverage(), ratio.getDeviationAverage())
                .build()
            )
            .collect(toList());
    }

    private static List<MeanPeriodStatistics<Integer>> countStatsFromDTO(Statistics statistics) {
        return statistics.getPublicStatistics()
            .getCount()
            .stream()
            .map(sumStatistics -> MeanPeriodStatistics.<Integer>builder(Days.days(sumStatistics.getPeriodLength()))
                .expectedValues(sumStatistics.getExpected(), sumStatistics.getDeviationExpected())
                .pastValues(sumStatistics.getAverage(), sumStatistics.getDeviationAverage())
                .build()
            )
            .collect(toList());
    }

    private static List<MeanPeriodStatistics<Float>> sumStatsFromDTO(Statistics statistics) {
        return statistics.getPublicStatistics()
            .getSum()
            .stream()
            .map(sumStatistics -> MeanPeriodStatistics.<Float>builder(Days.days(sumStatistics.getPeriodLength()))
                .expectedValues(sumStatistics.getExpected(), sumStatistics.getDeviationExpected())
                .pastValues(sumStatistics.getAverage(), sumStatistics.getDeviationAverage())
                .build()
            )
            .collect(toList());
    }
}
