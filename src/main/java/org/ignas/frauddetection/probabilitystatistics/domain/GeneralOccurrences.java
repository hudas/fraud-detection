package org.ignas.frauddetection.probabilitystatistics.domain;

public class GeneralOccurrences {

    private long totalTransactions;
    private long totalFraudTransactions;

    public GeneralOccurrences(long totalTransactions, long totalFraudTransactions) {
        this.totalTransactions = totalTransactions;
        this.totalFraudTransactions = totalFraudTransactions;
    }

    public long getTotalTransactions() {
        return totalTransactions;
    }

    public long getTotalFraudTransactions() {
        return totalFraudTransactions;
    }

    public float getFraudProbability() {
        if (totalTransactions == 0) {
            return 0f;
        }

        return ((float) totalFraudTransactions) / totalTransactions;
    }
}
