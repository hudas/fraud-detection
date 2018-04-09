package org.ignas.frauddetection.transactionstatistics.domain;

import org.joda.time.LocalDateTime;

import java.util.Objects;

public class DebtorPeriodValue {

    private LocalDateTime start;
    private LocalDateTime end;

    private String debtor;

    private float sum;
    private float count;

    public DebtorPeriodValue(
        LocalDateTime start,
        LocalDateTime end,
        String debtor,
        float sum,
        float count) {

        this.start = start;
        this.end = end;
        this.debtor = debtor;
        this.sum = sum;
        this.count = count;
    }

    public LocalDateTime getStart() {
        return start;
    }

    public LocalDateTime getEnd() {
        return end;
    }

    public String getDebtor() {
        return debtor;
    }

    public float getSum() {
        return sum;
    }

    public float getCount() {
        return count;
    }

    public boolean matches(PeriodIncrement increment) {
        return getDebtor().equals(increment.getDebtor())
            && getStart().equals(increment.getStart())
            && getEnd().equals(increment.getEnd());
    }
}
