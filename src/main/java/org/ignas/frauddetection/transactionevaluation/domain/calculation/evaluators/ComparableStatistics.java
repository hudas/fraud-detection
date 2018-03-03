package org.ignas.frauddetection.transactionevaluation.domain.calculation.evaluators;

public class ComparableStatistics {

    private float transactionResult;
    private float historicalResult;

    public ComparableStatistics(float transactionResult, float historicalResult) {
        this.transactionResult = transactionResult;
        this.historicalResult = historicalResult;
    }

    public float getTransactionResult() {
        return transactionResult;
    }

    public float getHistoricalResult() {
        return historicalResult;
    }
}
