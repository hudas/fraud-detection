package org.ignas.frauddetection.transactionstatistics.domain;

public class PeriodStats {

    private float sum;
    private float sumSquared;

    private float count;
    private float countSquared;

    private float ratioSum;
    private float ratioSumSquared;

    private long instances;

    public PeriodStats(
        float sum,
        float sumSquared,
        float count,
        float countSquared,
        float ratioSum,
        float ratioSumSquared,
        long instances) {

        this.sum = sum;
        this.sumSquared = sumSquared;
        this.count = count;
        this.countSquared = countSquared;
        this.ratioSum = ratioSum;
        this.ratioSumSquared = ratioSumSquared;
        this.instances = instances;
    }

    public float getSum() {
        return sum;
    }

    public float getSumSquared() {
        return sumSquared;
    }

    public float getCount() {
        return count;
    }

    public float getCountSquared() {
        return countSquared;
    }

    public float getRatioSum() {
        return ratioSum;
    }

    public float getRatioSumSquared() {
        return ratioSumSquared;
    }

    public long getInstances() {
        return instances;
    }
}
