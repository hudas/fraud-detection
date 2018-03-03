package org.ignas.frauddetection.transactionevaluation.domain.calculation;

public interface Evaluator<T> {
    PrintableResult evaluate(T transaction);
}
