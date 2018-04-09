package org.ignas.frauddetection.transactionstatistics.domain;

public class ConditionStats<T> {

    private float matchingValue;
    private float valuesSum;
    private float valuesSquaredSum;

    private Long instances;

    public ConditionStats(float matchingValue, float valuesSum, float valuesSquaredSum, Long instances) {
        this.matchingValue = matchingValue;
        this.valuesSum = valuesSum;
        this.valuesSquaredSum = valuesSquaredSum;
        this.instances = instances;
    }

    public float getMatchingValue() {
        return matchingValue;
    }

    public float getValuesSum() {
        return valuesSum;
    }

    public float getValuesSquaredSum() {
        return valuesSquaredSum;
    }

    public Long getInstances() {
        return instances;
    }
}
