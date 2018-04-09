package org.ignas.frauddetection.transactionstatistics.domain;

public class PeriodStats {

    private float valueSum;
    private float valueSumSquared;

    private long instances;

    public PeriodStats(float valueSum, float valueSumSquared, long instances) {
        this.valueSum = valueSum;
        this.valueSumSquared = valueSumSquared;
        this.instances = instances;
    }

    public float getValueSum() {
        return valueSum;
    }

    public float getValueSumSquared() {
        return valueSumSquared;
    }

    public long getInstances() {
        return instances;
    }
}
