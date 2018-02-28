package org.ignas.frauddetection.transactionstatistic.api.response;

import org.joda.time.Days;

import java.math.BigDecimal;

public class PersonalPeriod {

    private int periodLength;

    private BigDecimal expensesSum;

    private int transactionCount;


    public PersonalPeriod(int periodLength, BigDecimal expensesSum, int transactionCount) {
        this.periodLength = periodLength;
        this.expensesSum = expensesSum;
        this.transactionCount = transactionCount;
    }

    public int getPeriodLength() {
        return periodLength;
    }

    public BigDecimal getExpensesSum() {
        return expensesSum;
    }

    public int getTransactionCount() {
        return transactionCount;
    }
}
