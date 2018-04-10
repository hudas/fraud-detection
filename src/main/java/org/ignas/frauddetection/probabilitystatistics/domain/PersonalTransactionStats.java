package org.ignas.frauddetection.probabilitystatistics.domain;

import org.ignas.frauddetection.shared.Location;
import org.joda.time.LocalDateTime;

public class PersonalTransactionStats {

    private Location location;

    private float amount;

    private LocalDateTime time;

    public PersonalTransactionStats(
        Location location,
        float amount,
        LocalDateTime time) {

        this.location = location;
        this.amount = amount;
        this.time = time;
    }

    public Location getLocation() {
        return location;
    }

    public float getAmount() {
        return amount;
    }

    public LocalDateTime getTime() {
        return time;
    }
}
