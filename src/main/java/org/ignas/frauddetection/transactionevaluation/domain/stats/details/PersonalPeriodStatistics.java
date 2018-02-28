package org.ignas.frauddetection.transactionevaluation.domain.stats.details;

import org.joda.time.Days;

import java.math.BigDecimal;

public class PersonalPeriodStatistics {

    private Days periodLength;

    private BigDecimal expensesSum;

    private Integer transactionCount;


    public PersonalPeriodStatistics(Days periodLength, BigDecimal expensesSum, Integer transactionCount) {
        this.periodLength = periodLength;
        this.expensesSum = expensesSum;
        this.transactionCount = transactionCount;
    }

    public Days getPeriodLength() {
        return periodLength;
    }

    public BigDecimal getExpensesSum() {
        return expensesSum;
    }

    public Integer getTransactionCount() {
        return transactionCount;
    }
}
