package org.ignas.frauddetection.transactionstatistic.api.response.generalindicators;

import java.math.BigDecimal;

public class CountStatistics {

    private Integer average;
    private Integer deviationAverage;

    private Integer expected;
    private Integer deviationExpected;

    public CountStatistics(Integer average, Integer deviationAverage, Integer expected, Integer deviationExpected) {
        this.average = average;
        this.deviationAverage = deviationAverage;
        this.expected = expected;
        this.deviationExpected = deviationExpected;
    }

    public Integer getAverage() {
        return average;
    }

    public Integer getDeviationAverage() {
        return deviationAverage;
    }

    public Integer getExpected() {
        return expected;
    }

    public Integer getDeviationExpected() {
        return deviationExpected;
    }
}
