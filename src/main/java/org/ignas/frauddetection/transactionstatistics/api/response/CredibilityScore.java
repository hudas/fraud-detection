package org.ignas.frauddetection.transactionstatistics.api.response;

public class CredibilityScore {

    private float fraudRate;

    private float fraudRateAverage;

    private float fraudRateDeviation;

    public CredibilityScore(
        float fraudRate,
        float fraudRateAverage,
        float fraudRateDeviation) {

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
