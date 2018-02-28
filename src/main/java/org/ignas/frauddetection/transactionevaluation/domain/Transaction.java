package org.ignas.frauddetection.transactionevaluation.domain;

import org.ignas.frauddetection.shared.Location;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Transaction {

    private String id;

    private BigDecimal amount;

    private String debtor;

    private String creditor;

    private Location location;

    private LocalDateTime time;

    public Transaction(String id, BigDecimal amount, String debtor, String creditor, Location location, LocalDateTime time) {
        this.id = id;
        this.amount = amount;
        this.debtor = debtor;
        this.creditor = creditor;
        this.location = location;
        this.time = time;
    }

    public String getDebtor() {
        return debtor;
    }

    public String getCreditor() {
        return creditor;
    }

    public Location getLocation() {
        return location;
    }

    public LocalDateTime getTime() {
        return time;
    }
}
