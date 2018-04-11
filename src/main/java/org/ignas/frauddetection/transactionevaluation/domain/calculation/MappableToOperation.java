package org.ignas.frauddetection.transactionevaluation.domain.calculation;

import org.apache.commons.lang3.tuple.Pair;
import org.ignas.frauddetection.transactionevaluation.domain.Transaction;
import org.ignas.frauddetection.transactionevaluation.domain.stats.HistoricalData;
import org.joda.time.Days;

public interface MappableToOperation<T> {
    MapperResult<T> transform(Transaction transaction, HistoricalData statistics);
}
