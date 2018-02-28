package org.ignas.frauddetection.transactionevaluation.domain.calculation;

import org.ignas.frauddetection.transactionevaluation.domain.criteria.ValueExpectancy;

import java.util.Arrays;

/**
 *
 * Minimal accuracy loss from floating point usage, however huge performance advantage over BigDecimals
 */
public class DeviationExpectancyCalculator {

    public static ValueExpectancy calculate(float probability, float averageProbability, float deviationFromAverage) {

        Float rateOfDeviationFromAverage = (probability - averageProbability)/ deviationFromAverage;

        return Arrays.stream(ValueExpectancy.values())
            .filter(it -> it.abstracts(rateOfDeviationFromAverage))
            .findAny()
            .orElseThrow(IllegalStateException::new);
    }
}
