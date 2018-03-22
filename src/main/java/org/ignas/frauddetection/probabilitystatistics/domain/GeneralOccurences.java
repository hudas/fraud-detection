package org.ignas.frauddetection.probabilitystatistics.domain;

public class GeneralOccurences {

    private long totalTransactions;
    private long totalFraudTransactions;

    public GeneralOccurences(long totalTransactions, long totalFraudTransactions) {
        this.totalTransactions = totalTransactions;
        this.totalFraudTransactions = totalFraudTransactions;
    }

    public long getTotalTransactions() {
        return totalTransactions;
    }

    public long getTotalFraudTransactions() {
        return totalFraudTransactions;
    }
}
