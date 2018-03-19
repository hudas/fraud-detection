package org.ignas.frauddetection.transactionevaluation.domain.stats;

import org.ignas.frauddetection.shared.Location;
import org.ignas.frauddetection.transactionevaluation.domain.stats.details.PersonalPeriodStatistics;
import org.joda.time.Days;
import org.joda.time.LocalDateTime;
import org.joda.time.Seconds;

import java.math.BigDecimal;
import java.util.List;

public class DebtorStatistics {

    private Location mostUsedLocation;

    private Location lastTransactionLocation;

    private float mostValuableTransaction;

    private Seconds shortestTimeBetweenTransactions;

    private LocalDateTime lastTransactionExecutionTime;

    private List<PersonalPeriodStatistics> periodicStatistics;

    public DebtorStatistics(
        Location mostUsedLocation,
        Location lastTransactionLocation,
        float mostValuableTransaction,
        LocalDateTime lastTransactionExecutionTime,
        Seconds shortestTimeBetweenTransactions,
        List<PersonalPeriodStatistics> periodicStatistics) {

        this.mostUsedLocation = mostUsedLocation;
        this.lastTransactionLocation = lastTransactionLocation;
        this.mostValuableTransaction = mostValuableTransaction;
        this.lastTransactionExecutionTime = lastTransactionExecutionTime;
        this.periodicStatistics = periodicStatistics;
        this.shortestTimeBetweenTransactions = shortestTimeBetweenTransactions;
    }

    public Location getLastTransactionLocation() {
        return lastTransactionLocation;
    }

    public Location getMostUsedLocation() {
        return mostUsedLocation;
    }

    public float getMostValuableTransaction() {
        return mostValuableTransaction;
    }

    public LocalDateTime getLastTransactionExecutionTime() {
        return lastTransactionExecutionTime;
    }

    public Seconds getShortestTimeBetweenTransactions() {
        return shortestTimeBetweenTransactions;
    }

    public float getExpensesForPeriod(Days period) {
        return periodicStatistics.stream()
                .filter(stats -> stats.isForPeriod(period))
                .map(PersonalPeriodStatistics::getExpensesSum)
                .findAny()
                .orElseThrow(IllegalStateException::new);
    }

    public int getNumberOfTransactionsForPeriod(Days period) {
        return periodicStatistics.stream()
                .filter(stats -> stats.isForPeriod(period))
                .map(PersonalPeriodStatistics::getTransactionCount)
                .findAny()
                .orElseThrow(IllegalStateException::new);
    }
}
