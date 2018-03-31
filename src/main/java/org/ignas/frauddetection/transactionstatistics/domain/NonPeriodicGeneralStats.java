package org.ignas.frauddetection.transactionstatistics.domain;

public class NonPeriodicGeneralStats {

    private long sumOfTimeDiffFromLast;
    private long sumOfSquaredTimeDiffFromLast;

    private float sumOfDistanceFromLast;
    private float sumOfSquaredDistanceFromLast;

    private float sumOfDistanceFromComm;
    private float sumOfSquaredDistanceFromComm;

    private long instances;

    public NonPeriodicGeneralStats(
        long instances,
        long sumOfTimeDiffFromLast,
        long sumOfSquaredTimeDiffFromLast,
        float sumOfDistanceFromLast,
        float sumOfSquaredDistanceFromLast,
        float sumOfDistanceFromComm,
        float sumOfSquaredDistanceFromComm) {

        this.sumOfTimeDiffFromLast = sumOfTimeDiffFromLast;
        this.sumOfSquaredTimeDiffFromLast = sumOfSquaredTimeDiffFromLast;
        this.sumOfDistanceFromLast = sumOfDistanceFromLast;
        this.sumOfSquaredDistanceFromLast = sumOfSquaredDistanceFromLast;
        this.sumOfDistanceFromComm = sumOfDistanceFromComm;
        this.sumOfSquaredDistanceFromComm = sumOfSquaredDistanceFromComm;
        this.instances = instances;
    }

    public long getSumOfTimeDiffFromLast() {
        return sumOfTimeDiffFromLast;
    }

    public long getSumOfSquaredTimeDiffFromLast() {
        return sumOfSquaredTimeDiffFromLast;
    }

    public float getSumOfDistanceFromLast() {
        return sumOfDistanceFromLast;
    }

    public float getSumOfSquaredDistanceFromLast() {
        return sumOfSquaredDistanceFromLast;
    }

    public float getSumOfDistanceFromComm() {
        return sumOfDistanceFromComm;
    }

    public float getSumOfSquaredDistanceFromComm() {
        return sumOfSquaredDistanceFromComm;
    }

    public long getInstances() {
        return instances;
    }
}
