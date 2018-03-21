package org.ignas.frauddetection.probabilitystatistics.domain;

public class CriteriaUpdateIncrement {

    private String criteria;
    private String value;

    private boolean fraudulent;
    private boolean alreadyProcessedTransaction;

    public CriteriaUpdateIncrement(String criteria, String value, boolean fraudulent, boolean existing) {
        this.criteria = criteria;
        this.value = value;
        this.fraudulent = fraudulent;
        this.alreadyProcessedTransaction = existing;
    }


    public String pseudoUniqueCode() {
        return criteria + "_" + value;
    }

    public String getCriteria() {
        return criteria;
    }

    public String getValue() {
        return value;
    }

    public boolean isFraudulent() {
        return fraudulent;
    }

    public boolean isAlreadyProcessedTransaction() {
        return alreadyProcessedTransaction;
    }
}
