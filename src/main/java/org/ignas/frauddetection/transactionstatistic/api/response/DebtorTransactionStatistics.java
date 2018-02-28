package org.ignas.frauddetection.transactionstatistic.api.response;

import org.ignas.frauddetection.shared.Location;
import org.joda.time.LocalDateTime;

import java.math.BigDecimal;
import java.util.List;

public class DebtorTransactionStatistics {

    private Location mostUsedLocation;

    private BigDecimal mostValuableTransaction;

    private LocalDateTime lastTransactionExecutionTime;

    private List<PersonalPeriod> periodicStatistics;

    public DebtorTransactionStatistics(
        Location mostUsedLocation,
        BigDecimal mostValuableTransaction,
        LocalDateTime lastTransactionExecutionTime,
        List<PersonalPeriod> periodicStatistics) {

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

    public List<PersonalPeriod> getPeriodicStatistics() {
        return periodicStatistics;
    }
}