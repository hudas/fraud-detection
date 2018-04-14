package org.ignas.frauddetection.transactionstatistics.api.response;

import com.google.common.collect.ImmutableList;
import org.ignas.frauddetection.shared.Location;
import org.joda.time.LocalDateTime;

import java.util.List;

public class DebtorTransactionStatistics {

    private Location mostUsedLocation;

    private Location lastTransactionLocation;

    private float mostValuableTransaction;

    private LocalDateTime lastTransactionExecutionTime;

    private List<PersonalPeriod> periodicStatistics;

    private int minTimeBetweenTransactions;

    public DebtorTransactionStatistics(
        Location mostUsedLocation,
        Location lastTransactionLocation,
        float mostValuableTransaction,
        LocalDateTime lastTransactionExecutionTime,
        int minTimeBetweenTransactions,
        List<PersonalPeriod> periodicStatistics) {

        this.lastTransactionLocation = lastTransactionLocation;
        this.mostUsedLocation = mostUsedLocation;
        this.mostValuableTransaction = mostValuableTransaction;
        this.lastTransactionExecutionTime = lastTransactionExecutionTime;
        this.periodicStatistics = periodicStatistics;
        this.minTimeBetweenTransactions = minTimeBetweenTransactions;
    }

    public static DebtorTransactionStatistics unknown() {
        return new DebtorTransactionStatistics(
            null,
            null,
            0,
            null,
            Integer.MAX_VALUE,
            ImmutableList.of(
                new PersonalPeriod(1, 0, 0),
                new PersonalPeriod(7, 0, 0),
                new PersonalPeriod(30, 0, 0)
            )
        );
    }

    public Location getMostUsedLocation() {
        return mostUsedLocation;
    }

    public Location getLastTransactionLocation() {
        return lastTransactionLocation;
    }

    public float getMostValuableTransaction() {
        return mostValuableTransaction;
    }

    public LocalDateTime getLastTransactionExecutionTime() {
        return lastTransactionExecutionTime;
    }

    public List<PersonalPeriod> getPeriodicStatistics() {
        return periodicStatistics;
    }

    public int getMinTimeBetweenTransactions() {
        return minTimeBetweenTransactions;
    }

}
