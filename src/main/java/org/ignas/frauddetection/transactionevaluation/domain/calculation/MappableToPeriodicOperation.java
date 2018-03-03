package org.ignas.frauddetection.transactionevaluation.domain.calculation;

import org.ignas.frauddetection.transactionevaluation.domain.Transaction;
import org.ignas.frauddetection.transactionevaluation.domain.stats.DebtorStatistics;
import org.ignas.frauddetection.transactionevaluation.domain.stats.EnvironmentStatistics;
import org.ignas.frauddetection.transactionevaluation.domain.stats.GlobalStatistics;
import org.ignas.frauddetection.transactionevaluation.domain.stats.HistoricalData;
import org.joda.time.Days;

public interface MappableToPeriodicOperation<T> {
    T transform(Days period, Transaction transaction, HistoricalData statistics);
}
