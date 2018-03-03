package org.ignas.frauddetection.probabilitystatistics.api.response;

public class CriteriaGroupRisk {

    private float fraudProbabilityAverage;

    private float deviationFromAverage;

    public CriteriaGroupRisk(float fraudProbabilityAverage, float deviationFromAverage) {
        this.fraudProbabilityAverage = fraudProbabilityAverage;
        this.deviationFromAverage = deviationFromAverage;
    }

    public float getFraudProbabilityAverage() {
        return fraudProbabilityAverage;
    }

    public float getDeviationFromAverage() {
        return deviationFromAverage;
    }
}
