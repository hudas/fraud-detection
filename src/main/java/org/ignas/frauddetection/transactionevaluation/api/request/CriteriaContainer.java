package org.ignas.frauddetection.transactionevaluation.api.request;

import java.util.Map;

public class CriteriaContainer {

    private String transactionId;

    private Map<String, String> criteriaValues;

    public CriteriaContainer(String transactionId, Map<String, String> criteriaValues) {
        this.transactionId = transactionId;
        this.criteriaValues = criteriaValues;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public Map<String, String> getCriteriaValues() {
        return criteriaValues;
    }
}
