package org.ignas.frauddetection.transactionstatistic.api.response.generalindicators;

import org.joda.time.Days;

import java.math.BigDecimal;

public class SumStatistics {

    /**
     * Length in Days
     */
    private int periodLength;

    private float average;
    private float deviationAverage;

    private float expected;
    private float deviationExpected;

    public SumStatistics(int periodLength, float average, float deviationAverage, float expected, float deviationExpected) {
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
