package org.ignas.frauddetection.resultsanalyser.api;

import org.ignas.frauddetection.shared.Location;
import org.joda.time.DateTime;
import org.joda.time.LocalDateTime;

import java.math.BigDecimal;

public class ResultLoggingRequest {

    private String transactionId;

    private String debtorCreditCardId;

    private String debtorAccountId;

    private String creditorAccountId;

    private BigDecimal amount;

    private LocalDateTime time;

    private Location location;

    private Float probability;

    public ResultLoggingRequest(
        String transactionId,
        String debtorCreditCardId,
        String debtorAccountId,
        String creditorAccountId,
        BigDecimal amount,
        LocalDateTime time,
        Location location,
        Float probability) {

        this.transactionId = transactionId;
        this.debtorCreditCardId = debtorCreditCardId;
        this.debtorAccountId = debtorAccountId;
        this.creditorAccountId = creditorAccountId;
        this.amount = amount;
        this.time = time;
        this.location = location;
        this.probability = probability;
    }

    public ResultLoggingRequest() {
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public void setDebtorCreditCardId(String debtorCreditCardId) {
        this.debtorCreditCardId = debtorCreditCardId;
    }

    public void setDebtorAccountId(String debtorAccountId) {
        this.debtorAccountId = debtorAccountId;
    }

    public void setCreditorAccountId(String creditorAccountId) {
        this.creditorAccountId = creditorAccountId;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public void setTime(LocalDateTime time) {
        this.time = time;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public void setProbability(Float probability) {
        this.probability = probability;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public String getDebtorCreditCardId() {
        return debtorCreditCardId;
    }

    public String getDebtorAccountId() {
        return debtorAccountId;
    }

    public String getCreditorAccountId() {
        return creditorAccountId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public LocalDateTime getTime() {
        return time;
    }

    public Location getLocation() {
        return location;
    }

    public Float getProbability() {
        return probability;
    }
}
