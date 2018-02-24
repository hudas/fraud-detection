package org.ignas.frauddetection.httpapi.request;

import org.ignas.frauddetection.shared.Location;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public class EvaluationRequest {

    private String debtorCreditCardId;

    private String debtorAccountId;

    private String creditorAccountId;

    private BigDecimal amount;

    private OffsetDateTime time;

    private Location location;

    public EvaluationRequest() {
    }

    public String getDebtorCreditCardId() {
        return debtorCreditCardId;
    }

    public void setDebtorCreditCardId(String debtorCreditCardId) {
        this.debtorCreditCardId = debtorCreditCardId;
    }

    public String getDebtorAccountId() {
        return debtorAccountId;
    }

    public void setDebtorAccountId(String debtorAccountId) {
        this.debtorAccountId = debtorAccountId;
    }

    public String getCreditorAccountId() {
        return creditorAccountId;
    }

    public void setCreditorAccountId(String creditorAccountId) {
        this.creditorAccountId = creditorAccountId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public OffsetDateTime getTime() {
        return time;
    }

    public void setTime(OffsetDateTime time) {
        this.time = time;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }
}
