package org.ignas.frauddetection.transactionevaluation.domain.stats.details;

import org.joda.time.Days;

public class MeanPeriodStatistics<T> {

    private Days periodLength;

    private T average;
    private T deviationAverage;


    MeanPeriodStatistics(Days periodLength, T average, T deviationAverage) {
        this.periodLength = periodLength;
        this.average = average;
        this.deviationAverage = deviationAverage;
    }

    public static <R> Builder<R> builder(Days period) {
        return new Builder<>(period);
    }

    public boolean isForPeriod(Days period) {
        return periodLength.compareTo(period) == 0;
    }

    public T getAverage() {
        return average;
    }

    public T getDeviationAverage() {
        return deviationAverage;
    }

    public static class Builder<T> {

        private Days periodLength;

        private T average;
        private T deviationAverage;

        public Builder(Days periodLength) {
            this.periodLength = periodLength;
        }

        public Builder<T> pastValues(T average, T deviation) {
            this.average = average;
            this.deviationAverage = deviation;

            return this;
        }

        public MeanPeriodStatistics<T> build() {
            return new MeanPeriodStatistics<>(periodLength, average, deviationAverage);
        }
    }
}
