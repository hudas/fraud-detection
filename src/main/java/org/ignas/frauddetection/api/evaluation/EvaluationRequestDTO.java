package org.ignas.frauddetection.api.evaluation;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;

public class EvaluationRequestDTO {

    private String debtorCreditCardId;

    private String debtorAccountId;

    private String creditorAccountId;

    private BigDecimal amount;

    private OffsetDateTime time;

    private TransactionLocationDTO location;

    public EvaluationRequestDTO() {
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

    public TransactionLocationDTO getLocation() {
        return location;
    }

    public void setLocation(TransactionLocationDTO location) {
        this.location = location;
    }
}
