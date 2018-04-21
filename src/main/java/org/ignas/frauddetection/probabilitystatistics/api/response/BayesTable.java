package org.ignas.frauddetection.probabilitystatistics.api.response;

import java.util.Map;

public class BayesTable {

    private Map<String, Map<String, Float>> fraudProbabilities;
    private Map<String, Map<String, Float>> nonFraudProbabilities;

    public BayesTable(
        Map<String, Map<String, Float>> fraudProbabilities,
        Map<String, Map<String, Float>> nonFraudProbabilities) {
        this.fraudProbabilities = fraudProbabilities;
        this.nonFraudProbabilities = nonFraudProbabilities;
    }


    public Map<String, Map<String, Float>> getFraudProbabilities() {
        return fraudProbabilities;
    }

    public Map<String, Map<String, Float>> getNonFraudProbabilities() {
        return nonFraudProbabilities;
    }
}
