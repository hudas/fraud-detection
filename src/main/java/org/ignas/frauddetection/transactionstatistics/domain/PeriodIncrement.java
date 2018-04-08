package org.ignas.frauddetection.transactionstatistics.domain;

import org.joda.time.LocalDateTime;

import java.util.Objects;

public class PeriodIncrement {

    private LocalDateTime start;
    private LocalDateTime end;

    private String debtor;
    private float amount;

    public PeriodIncrement(
        LocalDateTime start,
        LocalDateTime end,
        String debtor,
        float amount) {

        this.start = start;
        this.end = end;
        this.debtor = debtor;
        this.amount = amount;
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

    public float getAmount() {
        return amount;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PeriodIncrement that = (PeriodIncrement) o;
        return Objects.equals(start, that.start) &&
            Objects.equals(end, that.end);
    }

    @Override
    public int hashCode() {

        return Objects.hash(start, end);
    }
}
