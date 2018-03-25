package org.ignas.frauddetection.transactionevaluation.api.request;

import java.util.Map;

public class LearningRequest {

    private boolean fraudulent;

    private boolean alreadyProcessedTransaction;

    private TransactionData transaction;

    private Map<String, Map<String, String>> grouptedCriteriaValues;

    private Map<String, String> criteriaGroupValues;

    public LearningRequest(
        boolean fraudulent,
        TransactionData transaction,
        Map<String, Map<String, String>> grouptedCriteriaValues,
        Map<String, String> criteriaGroupValues) {

        this.fraudulent = fraudulent;
        this.transaction = transaction;
        this.grouptedCriteriaValues = grouptedCriteriaValues;
        this.criteriaGroupValues = criteriaGroupValues;
    }

    public boolean isFraudulent() {
        return fraudulent;
    }

    public TransactionData getTransaction() {
        return transaction;
    }

    public Map<String, Map<String, String>> getGroupedCriteriaValues() {
        return grouptedCriteriaValues;
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

    public void markFraud() {
        fraudulent = true;
    }
}
