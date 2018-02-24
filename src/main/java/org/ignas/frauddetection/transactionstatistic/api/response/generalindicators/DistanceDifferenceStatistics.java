package org.ignas.frauddetection.transactionstatistic.api.response.generalindicators;

import org.joda.time.Seconds;

import java.math.BigDecimal;


public class DistanceDifferenceStatistics {

    private BigDecimal average;
    private BigDecimal deviationAverage;

    public DistanceDifferenceStatistics(BigDecimal average, BigDecimal deviationAverage) {
        this.average = average;
        this.deviationAverage = deviationAverage;
    }

    public BigDecimal getAverage() {
        return average;
    }

    public BigDecimal getDeviationAverage() {
        return deviationAverage;
    }
}
