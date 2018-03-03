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

    private List<Float> groupCriteriaProbabilities;

    private float averageGroupProbability;
    private float deviationFromAverage;

    public Risk(
        String groupName,
        List<Float> groupCriteriaProbabilities,
        float averageGroupProbability,
        float deviationFromAverage) {

        this.groupName = groupName;
        this.groupCriteriaProbabilities = groupCriteriaProbabilities;
        this.averageGroupProbability = averageGroupProbability;
        this.deviationFromAverage = deviationFromAverage;
    }

    public String getGroupName() {
        return groupName;
    }

    public Value evaluate(Float generalFraudRate) {
        Float probabilityOfCriteriaCombinationInFraudOccurences = generalFraudRate * groupCriteriaProbabilities.stream()
            .reduce((result, criteria) -> result * criteria)
            .orElse(0f);

        Float ratioOfActualAndGeneralProbability =
            (averageGroupProbability - probabilityOfCriteriaCombinationInFraudOccurences)
                / deviationFromAverage;

        return Arrays.stream(Value.values())
            .filter(value -> value.abstracts(ratioOfActualAndGeneralProbability))
            .findAny()
            .orElseThrow(IllegalStateException::new);
    }
}
