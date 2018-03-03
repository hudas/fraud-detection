package org.ignas.frauddetection.transactionstatistics.api.response.generalindicators;

public class CountStatistics {

    private int periodLength;

    private int average;
    private int deviationAverage;

    private int expected;
    private int deviationExpected;

    public CountStatistics(int periodLength, int average, int deviationAverage, int expected, int deviationExpected) {
        this.periodLength = periodLength;
        this.average = average;
        this.deviationAverage = deviationAverage;
        this.expected = expected;
        this.deviationExpected = deviationExpected;
    }

    public int getPeriodLength() {
        return periodLength;
    }

    public int getAverage() {
        return average;
    }

    public int getDeviationAverage() {
        return deviationAverage;
    }

    public int getExpected() {
        return expected;
    }

    public int getDeviationExpected() {
        return deviationExpected;
    }
}
