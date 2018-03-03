package org.ignas.frauddetection.transactionevaluation.domain.calculation.evaluators;

public class DeviationStatistics {

    private float value;
    private float averageValue;
    private float deviationFromAverage;

    public DeviationStatistics(float value, float averageValue, float deviationFromAverage) {
        this.value = value;
        this.averageValue = averageValue;
        this.deviationFromAverage = deviationFromAverage;
    }

    public float getValue() {
        return value;
    }

    public float getAverageValue() {
        return averageValue;
    }

    public float getDeviationFromAverage() {
        return deviationFromAverage;
    }
}
