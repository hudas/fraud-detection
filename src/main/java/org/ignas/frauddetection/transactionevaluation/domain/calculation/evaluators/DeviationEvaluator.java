package org.ignas.frauddetection.transactionevaluation.domain.calculation.evaluators;

import org.ignas.frauddetection.transactionevaluation.domain.calculation.Evaluator;
import org.ignas.frauddetection.transactionevaluation.domain.criteria.ValueExpectancy;

import java.util.Arrays;

/**
 *
 * Minimal accuracy loss from floating point usage, however huge performance advantage over BigDecimals
 */
public class DeviationEvaluator implements Evaluator<DeviationStatistics> {

    @Override
    public ValueExpectancy evaluate(DeviationStatistics transaction) {
        // First transaction, no previous known data.
        if (transaction.getAverageValue() == 0f || transaction.getDeviationFromAverage() == 0f) {
            return ValueExpectancy.EXPECTED;
        }

        float rateOfDeviationFromAverage = (transaction.getValue() - transaction.getAverageValue())
                / transaction.getDeviationFromAverage();

        return Arrays.stream(ValueExpectancy.values())
                .filter(it -> it.abstracts(rateOfDeviationFromAverage))
                .findAny()
                .orElseThrow(IllegalStateException::new);
    }
}
