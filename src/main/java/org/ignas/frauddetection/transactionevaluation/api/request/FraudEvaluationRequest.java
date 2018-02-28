package org.ignas.frauddetection.transactionevaluation.api.request;

import org.ignas.frauddetection.shared.Location;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class FraudEvaluationRequest {

    private String transactionId;

    private BigDecimal amount;

    private String debtorId;

    private String creditorId;

    private Location location;

    private LocalDateTime time;

    public FraudEvaluationRequest() {
    }

    public FraudEvaluationRequest(String debtorId, String creditorId, Location location, LocalDateTime time) {
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
