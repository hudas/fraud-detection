package org.ignas.frauddetection.transactionevaluation.domain.stats.details;

import org.joda.time.Days;

public class MeanPeriodStatistics<T> {

    private Days periodLength;

    private T average;
    private T deviationAverage;

    private T expected;
    private T deviationExpected;

    MeanPeriodStatistics(Days periodLength, T average, T deviationAverage, T expected, T deviationExpected) {
        this.periodLength = periodLength;
        this.average = average;
        this.deviationAverage = deviationAverage;
        this.expected = expected;
        this.deviationExpected = deviationExpected;
    }

    public static <R> MeanPeriodStatisticsBuilder<R> builder(Days period) {
        return new MeanPeriodStatisticsBuilder<>(period);
    }
}
