package org.ignas.frauddetection.transactionstatistic.api.response.generalindicators;

import org.joda.time.Seconds;

import java.math.BigDecimal;

public class TimeDifferenceStatistics {

    private Seconds average;
    private Seconds deviationAverage;

    private Seconds expected;
    private Seconds deviationExpected;

    public TimeDifferenceStatistics(Seconds average, Seconds deviationAverage, Seconds expected, Seconds deviationExpected) {
        this.average = average;
        this.deviationAverage = deviationAverage;
        this.expected = expected;
        this.deviationExpected = deviationExpected;
    }

    public Seconds getAverage() {
        return average;
    }

    public Seconds getDeviationAverage() {
        return deviationAverage;
    }

    public Seconds getExpected() {
        return expected;
    }

    public Seconds getDeviationExpected() {
        return deviationExpected;
    }
}
