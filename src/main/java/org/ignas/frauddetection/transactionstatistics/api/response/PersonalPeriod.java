package org.ignas.frauddetection.transactionstatistics.api.response;

public class PersonalPeriod {

    private int periodLength;

    private float expensesSum;

    private int transactionCount;


    public PersonalPeriod(int periodLength, float expensesSum, int transactionCount) {
        this.periodLength = periodLength;
        this.expensesSum = expensesSum;
        this.transactionCount = transactionCount;
    }

    public int getPeriodLength() {
        return periodLength;
    }

    public float getExpensesSum() {
        return expensesSum;
    }

    public int getTransactionCount() {
        return transactionCount;
    }
}
