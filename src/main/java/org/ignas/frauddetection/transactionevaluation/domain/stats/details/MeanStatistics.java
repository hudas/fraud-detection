package org.ignas.frauddetection.transactionevaluation.domain.stats.details;

import org.joda.time.Days;

public class MeanStatistics<T> {

    private T average;
    private T deviationAverage;

    private T expected;
    private T deviationExpected;

    public MeanStatistics(T average, T deviationAverage, T expected, T deviationExpected) {
        this.average = average;
        this.deviationAverage = deviationAverage;
        this.expected = expected;
        this.deviationExpected = deviationExpected;
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

    public T getExpected() {
        return expected;
    }

    public T getDeviationExpected() {
        return deviationExpected;
    }

    public static class Builder<T> {

        private T average;
        private T deviationAverage;
        private T expected;
        private T deviationExpected;

        public Builder() {

        }

        public Builder<T> pastValues(T average, T deviation) {
            this.average = average;
            this.deviationAverage = deviation;

            return this;
        }

        public Builder<T> expectedValues(T expected, T deviation) {
            this.expected = expected;
            this.deviationExpected = deviation;

            return this;
        }

        public MeanStatistics<T> build() {
            return new MeanStatistics<T>(average, deviationAverage, expected, deviationExpected);
        }
    }
}
