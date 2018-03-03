package org.ignas.frauddetection.transactionevaluation.domain.stats.details;

public class FraudRate {

    private float fraudRate;

    private float fraudRateAverage;

    private float fraudRateDeviation;

    public FraudRate(float fraudRate, float fraudRateAverage, float fraudRateDeviation) {
        this.fraudRate = fraudRate;
        this.fraudRateAverage = fraudRateAverage;
        this.fraudRateDeviation = fraudRateDeviation;
    }

    public float getFraudRate() {
        return fraudRate;
    }

    public float getFraudRateAverage() {
        return fraudRateAverage;
    }

    public float getFraudRateDeviation() {
        return fraudRateDeviation;
    }
}
