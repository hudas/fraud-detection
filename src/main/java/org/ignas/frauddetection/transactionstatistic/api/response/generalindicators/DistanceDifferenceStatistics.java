package org.ignas.frauddetection.transactionstatistic.api.response.generalindicators;

import org.joda.time.Days;
import org.joda.time.Seconds;

import java.math.BigDecimal;


public class DistanceDifferenceStatistics {

    private int periodLength;

    private float average;
    private float deviationAverage;

    public DistanceDifferenceStatistics(int periodLength, float average, float deviationAverage) {
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
