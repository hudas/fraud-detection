package org.ignas.frauddetection.transactionevaluation.domain.stats.details;

import org.joda.time.Days;

public class MeanStatistics<T> {

    private T average;
    private T deviationAverage;


    public MeanStatistics(T average, T deviationAverage) {
        this.average = average;
        this.deviationAverage = deviationAverage;
    }

    public static <R> Builder<R> builder() {
        return new Builder<>();
    }

    public T getAverage() {
        return average;
    }

    public T getDeviationAverage() {
        return deviationAverage;
    }

    public static class Builder<T> {

        private T average;
        private T deviationAverage;

        public Builder() {

        }

        public Builder<T> pastValues(T average, T deviation) {
            this.average = average;
            this.deviationAverage = deviation;

            return this;
        }

        public MeanStatistics<T> build() {
            return new MeanStatistics<T>(average, deviationAverage);
        }
    }
}
