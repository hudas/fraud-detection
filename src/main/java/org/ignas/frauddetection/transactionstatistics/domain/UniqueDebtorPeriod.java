package org.ignas.frauddetection.transactionstatistics.domain;

import org.joda.time.LocalDateTime;

import java.util.Objects;

public class UniqueDebtorPeriod {

    private LocalDateTime start;
    private LocalDateTime end;

    private String debtor;

    public UniqueDebtorPeriod(
        LocalDateTime start,
        LocalDateTime end,
        String debtor) {

        this.start = start;
        this.end = end;
        this.debtor = debtor;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UniqueDebtorPeriod that = (UniqueDebtorPeriod) o;
        return Objects.equals(start, that.start) &&
            Objects.equals(end, that.end) &&
            Objects.equals(debtor, that.debtor);
    }

    @Override
    public int hashCode() {

        return Objects.hash(start, end, debtor);
    }
}
