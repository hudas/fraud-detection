package org.ignas.frauddetection.probabilitystatistics.api.response;

import java.util.Map;

public class ProbabilityStatistics {

    private float fraudProbability;

    private Map<String, Float> criteriaProbabilites;

    private Map<String, CriteriaGroupRisk> groupStatistics;

    public ProbabilityStatistics(
        float fraudProbability,
        Map<String, Float> criteriaProbabilites,
        Map<String, CriteriaGroupRisk> groupStatistics) {

        this.fraudProbability = fraudProbability;
        this.criteriaProbabilites = criteriaProbabilites;
        this.groupStatistics = groupStatistics;
    }

    public float getFraudProbability() {
        return fraudProbability;
    }

    public Map<String, Float> getCriteriaProbabilites() {
        return criteriaProbabilites;
    }

    public Map<String, CriteriaGroupRisk> getGroupStatistics() {
        return groupStatistics;
    }
}

