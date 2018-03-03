package org.ignas.frauddetection.probabilitystatistics.api.request;

import java.util.Map;

public class CriteriaProbabilityRequest {

    private Map<String, String> criteriaValues;

    public CriteriaProbabilityRequest(Map<String, String> criteriaValues) {
        this.criteriaValues = criteriaValues;
    }

    public Map<String, String> getCriteriaValues() {
        return criteriaValues;
    }
}
