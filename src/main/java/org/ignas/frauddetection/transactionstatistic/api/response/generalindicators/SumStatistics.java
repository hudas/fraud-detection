package org.ignas.frauddetection.transactionstatistic.api.response.generalindicators;

import java.math.BigDecimal;

public class SumStatistics {

    private BigDecimal average;
    private BigDecimal deviationAverage;

    private BigDecimal expected;
    private BigDecimal deviationExpected;

    public SumStatistics(BigDecimal average, BigDecimal deviationAverage, BigDecimal expected, BigDecimal deviationExpected) {
        this.average = average;
        this.deviationAverage = deviationAverage;
        this.expected = expected;
        this.deviationExpected = deviationExpected;
    }

    public BigDecimal getAverage() {
        return average;
    }

    public BigDecimal getDeviationAverage() {
        return deviationAverage;
    }

    public BigDecimal getExpected() {
        return expected;
    }

    public BigDecimal getDeviationExpected() {
        return deviationExpected;
    }
}
