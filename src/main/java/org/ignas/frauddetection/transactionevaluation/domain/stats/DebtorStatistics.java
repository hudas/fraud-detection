package org.ignas.frauddetection.transactionevaluation.domain.stats;

import org.ignas.frauddetection.shared.Location;
import org.ignas.frauddetection.transactionevaluation.domain.stats.details.PersonalPeriodStatistics;
import org.joda.time.LocalDateTime;

import java.math.BigDecimal;
import java.util.List;

public class DebtorStatistics {

    private Location mostUsedLocation;

    private BigDecimal mostValuableTransaction;

    private LocalDateTime lastTransactionExecutionTime;

    private List<PersonalPeriodStatistics> periodicStatistics;

    public DebtorStatistics(
        Location mostUsedLocation,
        BigDecimal mostValuableTransaction,
        LocalDateTime lastTransactionExecutionTime,
        List<PersonalPeriodStatistics> periodicStatistics) {

        this.mostUsedLocation = mostUsedLocation;
        this.mostValuableTransaction = mostValuableTransaction;
        this.lastTransactionExecutionTime = lastTransactionExecutionTime;
        this.periodicStatistics = periodicStatistics;
    }

    public Location getMostUsedLocation() {
        return mostUsedLocation;
    }

    public BigDecimal getMostValuableTransaction() {
        return mostValuableTransaction;
    }

    public LocalDateTime getLastTransactionExecutionTime() {
        return lastTransactionExecutionTime;
    }

    public List<PersonalPeriodStatistics> getPeriodicStatistics() {
        return periodicStatistics;
    }
}
