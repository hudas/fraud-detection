package org.ignas.frauddetection.transactionevaluation.api.request;

import java.util.Map;

public class LearningRequest {

    private boolean fraudulent;

    private boolean alreadyProcessedTransaction;

    private TransactionData transaction;

    private BehaviourData behaviourData;

    private Map<String, Map<String, String>> groupedCriteriaValues;

    private Map<String, String> criteriaGroupValues;

    public LearningRequest(
        boolean fraudulent,
        TransactionData transaction,
        BehaviourData behaviourData,
        Map<String, Map<String, String>> grouptedCriteriaValues,
        Map<String, String> criteriaGroupValues) {

        this.fraudulent = fraudulent;
        this.behaviourData = behaviourData;
        this.transaction = transaction;
        this.groupedCriteriaValues = grouptedCriteriaValues;
        this.criteriaGroupValues = criteriaGroupValues;
    }

    public boolean isFraudulent() {
        return fraudulent;
    }

    public TransactionData getTransaction() {
        return transaction;
    }

    public BehaviourData getBehaviourData() {
        return behaviourData;
    }

    public Map<String, Map<String, String>> getGroupedCriteriaValues() {
        return groupedCriteriaValues;
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
        alreadyProcessedTransaction = true;
    }
}
