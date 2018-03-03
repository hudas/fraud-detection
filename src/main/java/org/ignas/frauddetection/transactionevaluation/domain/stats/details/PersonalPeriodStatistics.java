package org.ignas.frauddetection.transactionevaluation.domain.stats.details;

import org.joda.time.Days;

import java.math.BigDecimal;

public class PersonalPeriodStatistics {

    private Days periodLength;

    private Float expensesSum;

    private Integer transactionCount;


    public PersonalPeriodStatistics(Days periodLength, Float expensesSum, Integer transactionCount) {
        this.periodLength = periodLength;
        this.expensesSum = expensesSum;
        this.transactionCount = transactionCount;
    }

    public boolean isForPeriod(Days period) {
        return periodLength.compareTo(period) == 0;
    }

    public Float getExpensesSum() {
        return expensesSum;
    }

    public Integer getTransactionCount() {
        return transactionCount;
    }
}
