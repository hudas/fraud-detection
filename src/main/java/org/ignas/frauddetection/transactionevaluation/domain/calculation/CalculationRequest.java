package org.ignas.frauddetection.transactionevaluation.domain.calculation;


public class CalculationRequest {

    private float probability;
    private float averageProbability;
    private float deviationFromAverage;

    public CalculationRequest(float probability, float averageProbability, float deviationFromAverage) {
        this.probability = probability;
        this.averageProbability = averageProbability;
        this.deviationFromAverage = deviationFromAverage;
    }
}
