package org.ignas.frauddetection.transactionevaluation.domain;

import java.util.Map;

public class CriteriaGroup {

    private Map<String, Float> valueProbabilities;

    public CriteriaGroup(Map<String, Float> valueProbabilities) {
        this.valueProbabilities = valueProbabilities;
    }

    public Float eventProbability(String eventKey) {
        return valueProbabilities.get(eventKey);
    }
}
