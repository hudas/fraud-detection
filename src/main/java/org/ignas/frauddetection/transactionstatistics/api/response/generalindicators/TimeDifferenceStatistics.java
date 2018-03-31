package org.ignas.frauddetection.transactionstatistics.api.response.generalindicators;

public class TimeDifferenceStatistics {

    private float average;
    private float deviationAverage;

    public TimeDifferenceStatistics(
        float average,
        float deviationAverage) {

        this.average = average;
        this.deviationAverage = deviationAverage;
    }

    public float getAverage() {
        return average;
    }

    public float getDeviationAverage() {
        return deviationAverage;
    }
}
