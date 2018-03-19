package org.ignas.frauddetection.transactionstatistics.api.response.generalindicators;

public class CountStatistics {

    private int periodLength;

    private float average;
    private float deviationAverage;

    private float expected;
    private float deviationExpected;

    public CountStatistics(int periodLength, float average, float deviationAverage, float expected, float deviationExpected) {
        this.periodLength = periodLength;
        this.average = average;
        this.deviationAverage = deviationAverage;
        this.expected = expected;
        this.deviationExpected = deviationExpected;
    }

    public int getPeriodLength() {
        return periodLength;
    }

    public float getAverage() {
        return average;
    }

    public float getDeviationAverage() {
        return deviationAverage;
    }

    public float getExpected() {
        return expected;
    }

    public float getDeviationExpected() {
        return deviationExpected;
    }
}
