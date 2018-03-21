package org.ignas.frauddetection.transactionevaluation.api.request;

import java.util.Map;

public class LearningRequest {

    private boolean fraudulent;

    private boolean alreadyProcessedTransaction;

    private TransactionData transaction;

    private Map<String, String> criteriaValues;

    private Map<String, String> criteriaGroupValues;

    public LearningRequest(
        boolean fraudulent,
        TransactionData transaction,
        Map<String, String> criteriaValues,
        Map<String, String> criteriaGroupValues) {

        this.fraudulent = fraudulent;
        this.transaction = transaction;
        this.criteriaValues = criteriaValues;
        this.criteriaGroupValues = criteriaGroupValues;
    }

    public boolean isFraudulent() {
        return fraudulent;
    }

    public TransactionData getTransaction() {
        return transaction;
    }

    public Map<String, String> getCriteriaValues() {
        return criteriaValues;
    }

    public Map<String, String> getCriteriaGroupValues() {
        return criteriaGroupValues;
    }

    public boolean isAlreadyProcessedTransaction() {
        return alreadyProcessedTransaction;
    }

    public void setAlreadyProcessedTransaction(boolean alreadyProcessedTransaction) {
        this.alreadyProcessedTransaction = alreadyProcessedTransaction;
    }
}
