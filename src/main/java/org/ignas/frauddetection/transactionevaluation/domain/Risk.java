package org.ignas.frauddetection.transactionevaluation.domain;

import com.google.common.collect.Range;

import java.util.Arrays;
import java.util.List;

public class Risk {

    // TODO: Duplicates criteria values could be refactored further.
    public enum Value {
        VERY_LOW_RISK(Range.<Float>lessThan(-2f)),
        LOW_RISK(Range.<Float>closedOpen(-2f, -1f)),
        EXPECTED_RISK(Range.<Float>closed(-1f, 1f)),
        HIGH_RISK(Range.<Float>openClosed(1f, 2f)),
        VERY_HIGH_RISK(Range.<Float>greaterThan(2f));

        private Range<Float> deviationFromAverageRatio;

        Value(Range<Float> deviationFromAverageRatio) {
            this.deviationFromAverageRatio = deviationFromAverageRatio;
        }

        private boolean abstracts(Float deviationFromAverageRatio) {
            return this.deviationFromAverageRatio.contains(deviationFromAverageRatio);
        }

    }

    private String groupName;

    private List<Float> fraudProbabilities;
    private List<Float> nonFraudProbabilities;

    private float averageGroupProbability;
    private float deviationFromAverage;

    public Risk(
        String groupName,
        List<Float> fraudProbabilities,
        List<Float> nonFraudProbabilities,
        float averageGroupProbability,
        float deviationFromAverage) {

        this.groupName = groupName;
        this.fraudProbabilities = fraudProbabilities;
        this.averageGroupProbability = averageGroupProbability;
        this.nonFraudProbabilities = nonFraudProbabilities;
        this.deviationFromAverage = deviationFromAverage;
    }

    public String getGroupName() {
        return groupName;
    }

    public Value evaluate(Float generalFraudRate) {
        Float probabilityOfCriteriaCombinationInFraudOccurences = generalFraudRate * fraudProbabilities.stream()
            .filter(probability -> probability != 0)
            .reduce((result, criteria) -> result * criteria)
            .orElse(0f);

        if (averageGroupProbability == 0 && probabilityOfCriteriaCombinationInFraudOccurences > 0) {
            return Value.EXPECTED_RISK;
        }

        if (deviationFromAverage == 0) {
            if (averageGroupProbability != probabilityOfCriteriaCombinationInFraudOccurences) {
                return Value.EXPECTED_RISK;
            } else {
                return Value.EXPECTED_RISK;
            }
        }

        Float probabilityOfCriteriaCombinationInNonFraudOccurences = (1 - generalFraudRate) * nonFraudProbabilities.stream()
            .filter(probability -> probability != 0)
            .reduce((result, criteria) -> result * criteria)
            .orElse(0f);

        Float probability = probabilityOfCriteriaCombinationInFraudOccurences /
            (probabilityOfCriteriaCombinationInFraudOccurences + probabilityOfCriteriaCombinationInNonFraudOccurences);


        Float ratioOfActualAndGeneralProbability = (probability - averageGroupProbability) / deviationFromAverage;

//        System.out.println(groupName + " Riskrate: " + ratioOfActualAndGeneralProbability + " Average: " + averageGroupProbability + " Actual " + probabilityOfCriteriaCombinationInFraudOccurences);
        return Arrays.stream(Value.values())
            .filter(value -> value.abstracts(ratioOfActualAndGeneralProbability))
            .findAny()
            .orElseThrow(IllegalStateException::new);
    }
}
