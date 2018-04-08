package org.ignas.frauddetection.transactionstatistics.domain;

import org.joda.time.LocalDateTime;

import java.util.Objects;

public class PeriodValue {

    private LocalDateTime start;
    private LocalDateTime end;

    private String debtor;
    private float sum;

    public PeriodValue(
        LocalDateTime start,
        LocalDateTime end,
        String debtor,
        float sum) {

        this.start = start;
        this.end = end;
        this.debtor = debtor;
        this.sum = sum;
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
}
