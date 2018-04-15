package org.ignas.frauddetection.probabilitystatistics.api.request;

import org.ignas.frauddetection.transactionevaluation.api.request.CriteriaContainer;

import java.util.Map;

public class CriteriaProbabilityRequest {

    private String transactionId;

    private Map<String, String> criteriaValues;

    public CriteriaProbabilityRequest(CriteriaContainer request) {
        this.transactionId = request.getTransactionId();
        this.criteriaValues = request.getCriteriaValues();
    }

    public Map<String, String> getCriteriaValues() {
        return criteriaValues;
    }

    public String getTransactionId() {
        return transactionId;
    }
}
