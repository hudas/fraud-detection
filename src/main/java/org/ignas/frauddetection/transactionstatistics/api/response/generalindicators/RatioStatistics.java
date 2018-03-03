package org.ignas.frauddetection.transactionstatistics.api.response.generalindicators;

public class RatioStatistics {

    /**
     * Length in Days
     */
    private int periodLength;

    private float average;
    private float deviationAverage;

    public RatioStatistics(int periodLength, float average, float deviationAverage) {
        this.periodLength = periodLength;
        this.average = average;
        this.deviationAverage = deviationAverage;
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
}
