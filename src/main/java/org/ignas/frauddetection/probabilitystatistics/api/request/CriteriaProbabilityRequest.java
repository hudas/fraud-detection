package org.ignas.frauddetection.probabilitystatistics.api.request;

import java.util.Map;

public class CriteriaProbabilityRequest {

    private long id;

    private Map<String, String> criteriaValues;

    public CriteriaProbabilityRequest(long id, Map<String, String> criteriaValues) {
        this.id = id;
        this.criteriaValues = criteriaValues;
    }

    public long getId() {
        return id;
    }

    public Map<String, String> getCriteriaValues() {
        return criteriaValues;
    }
}
