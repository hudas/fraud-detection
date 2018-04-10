package org.ignas.frauddetection.probabilitystatistics.domain;

import org.joda.time.LocalDateTime;

public class PersonalPeriodTransaction {

    private LocalDateTime time;
    private float amount;

    public PersonalPeriodTransaction(
        LocalDateTime time,
        float amount) {

        this.time = time;
        this.amount = amount;
    }

    public LocalDateTime getTime() {
        return time;
    }

    public float getAmount() {
        return amount;
    }
}
