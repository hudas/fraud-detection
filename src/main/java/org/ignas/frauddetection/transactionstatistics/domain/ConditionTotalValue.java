package org.ignas.frauddetection.transactionstatistics.domain;

public class ConditionTotalValue {

    private long instances;

    private float sumOfValues;
    private float sumOfSquaredValues;

    public ConditionTotalValue(long instances, float sumOfValues, float sumOfSquaredValues) {
        this.instances = instances;
        this.sumOfValues = sumOfValues;
        this.sumOfSquaredValues = sumOfSquaredValues;
    }

    public static ConditionTotalValue unknown() {
        return new ConditionTotalValue(0, 0f, 0f);
    }

    public long getInstances() {
        return instances;
    }

    public float getSumOfValues() {
        return sumOfValues;
    }

    public float getSumOfSquaredValues() {
        return sumOfSquaredValues;
    }
}
