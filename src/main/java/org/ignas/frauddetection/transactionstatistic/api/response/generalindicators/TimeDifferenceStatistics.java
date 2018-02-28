package org.ignas.frauddetection.transactionstatistic.api.response.generalindicators;

import org.joda.time.Days;
import org.joda.time.Seconds;

import java.math.BigDecimal;

public class TimeDifferenceStatistics {

    private int periodLength;

    private int average;
    private int deviationAverage;

    private int expected;
    private int deviationExpected;

    public TimeDifferenceStatistics(int periodLength, int average, int deviationAverage, int expected, int deviationExpected) {
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
