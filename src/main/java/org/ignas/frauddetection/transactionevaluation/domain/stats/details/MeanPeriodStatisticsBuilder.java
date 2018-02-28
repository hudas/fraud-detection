package org.ignas.frauddetection.transactionevaluation.domain.stats.details;

import org.joda.time.Days;

public class MeanPeriodStatisticsBuilder<T> {

    private Days periodLength;

    private T average;
    private T deviationAverage;
    private T expected;
    private T deviationExpected;

    public MeanPeriodStatisticsBuilder(Days periodLength) {
        this.periodLength = periodLength;
    }

    public MeanPeriodStatisticsBuilder<T> pastValues(T average, T deviation) {
        this.average = average;
        this.deviationAverage = deviation;

        return this;
    }

    public MeanPeriodStatisticsBuilder<T> expectedValues(T expected, T deviation) {
        this.expected = expected;
        this.deviationExpected = deviation;

        return this;
    }

    public MeanPeriodStatistics<T> build() {
        return new MeanPeriodStatistics<>(periodLength, average, deviationAverage, expected, deviationExpected);
    }
}
