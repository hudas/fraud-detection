package org.ignas.frauddetection.transactionevaluation.api.request;

import org.ignas.frauddetection.shared.Location;
import org.joda.time.LocalDateTime;

import java.math.BigDecimal;

public class TransactionData {

    private String transactionId;

    private Float amount;

    private String debtorId;

    private String creditorId;

    private Location location;

    private LocalDateTime time;

    public TransactionData() {
    }

    public TransactionData(
            String transactionId,
            Float amount,
            String debtorId,
            String creditorId,
            Location location,
            LocalDateTime time) {

        this.amount = amount;
        this.transactionId = transactionId;
        this.debtorId = debtorId;
        this.creditorId = creditorId;
        this.location = location;
        this.time = time;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public Float getAmount() {
        return amount;
    }

    public String getDebtorId() {
        return debtorId;
    }

    public String getCreditorId() {
        return creditorId;
    }

    public Location getLocation() {
        return location;
    }

    public LocalDateTime getTime() {
        return time;
    }
}
