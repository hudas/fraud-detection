package org.ignas.frauddetection.transactionevaluation.domain.calculation;

import org.ignas.frauddetection.transactionevaluation.domain.Transaction;
import org.ignas.frauddetection.transactionevaluation.domain.stats.HistoricalData;
import org.joda.time.Days;

public interface MappableToOperation<T> {
    T transform(Transaction transaction, HistoricalData statistics);
}
