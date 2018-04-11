package org.ignas.frauddetection.transactionevaluation.domain.calculation.criteria;

import org.ignas.frauddetection.transactionevaluation.domain.Transaction;
import org.ignas.frauddetection.transactionevaluation.domain.calculation.EvaluationResult;
import org.ignas.frauddetection.transactionevaluation.domain.calculation.PrintableResult;
import org.ignas.frauddetection.transactionevaluation.domain.stats.HistoricalData;

public interface Criteria {
    EvaluationResult evaluate(Transaction transaction, HistoricalData stats);
}
