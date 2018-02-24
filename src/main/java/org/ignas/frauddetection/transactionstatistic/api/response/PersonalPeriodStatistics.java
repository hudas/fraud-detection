package org.ignas.frauddetection.transactionstatistic.api.response;

import org.ignas.frauddetection.shared.Location;
import org.joda.time.Days;
import org.joda.time.LocalDateTime;

import java.math.BigDecimal;

public class PersonalPeriodStatistics {

    private Days periodLength;

    private BigDecimal expensesSum;
    private BigDecimal expensesAverage;

    private BigDecimal transactionCount;

    private LocalDateTime lastTransactionExecutionTime;

    private Location mostUsedLocation;

    public PersonalPeriodStatistics() {
    }

    public PersonalPeriodStatistics(
        Days periodLength,
        BigDecimal expensesSum,
        BigDecimal expensesAverage,
        BigDecimal transactionCount,
        LocalDateTime lastTransactionExecutionTime,
        Location mostUsedLocation) {
        this.periodLength = periodLength;
        this.expensesSum = expensesSum;
        this.expensesAverage = expensesAverage;
        this.transactionCount = transactionCount;
        this.lastTransactionExecutionTime = lastTransactionExecutionTime;
        this.mostUsedLocation = mostUsedLocation;
    }

    public Days getPeriodLength() {
        return periodLength;
    }


}
