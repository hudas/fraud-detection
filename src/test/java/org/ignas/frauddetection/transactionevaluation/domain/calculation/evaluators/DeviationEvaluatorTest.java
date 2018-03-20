package org.ignas.frauddetection.transactionevaluation.domain.calculation.evaluators;

import io.swagger.v3.oas.annotations.Parameters;
import org.ignas.frauddetection.transactionevaluation.domain.criteria.ValueExpectancy;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class DeviationEvaluatorTest {

    @ParameterizedTest
    @MethodSource("provideValues")
    void evaluate(DeviationStatistics statistics, ValueExpectancy expected) {
        DeviationEvaluator evaluator = new DeviationEvaluator();

        ValueExpectancy result = evaluator.evaluate(statistics);

        Assertions.assertEquals(expected, result);
    }

    public static Stream<Arguments> provideValues() {
        return Stream.of(
            Arguments.of(new DeviationStatistics(1,1,1), ValueExpectancy.EXPECTED),
            Arguments.of(new DeviationStatistics(2,1,1), ValueExpectancy.EXPECTED),
            Arguments.of(new DeviationStatistics(0,1,1), ValueExpectancy.EXPECTED),
            Arguments.of(new DeviationStatistics(2.5f,1,1), ValueExpectancy.MORE_THAN_EXPECTED),
            Arguments.of(new DeviationStatistics(3f,1,1), ValueExpectancy.MORE_THAN_EXPECTED),
            Arguments.of(new DeviationStatistics(3.5f,1,1), ValueExpectancy.MUCH_MORE_THAN_EXPECTED)
        );
    }
}
