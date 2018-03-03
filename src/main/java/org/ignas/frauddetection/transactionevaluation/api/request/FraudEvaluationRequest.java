package org.ignas.frauddetection.transactionevaluation.api.request;

import org.ignas.frauddetection.shared.Location;
import org.joda.time.LocalDateTime;

import java.math.BigDecimal;

public class FraudEvaluationRequest {

    private String transactionId;

    private BigDecimal amount;

    private String debtorId;

    private String creditorId;

    private Location location;

    private LocalDateTime time;

    public FraudEvaluationRequest() {
    }

    public FraudEvaluationRequest(
            String transactionId,
            BigDecimal amount,
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

    public BigDecimal getAmount() {
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
