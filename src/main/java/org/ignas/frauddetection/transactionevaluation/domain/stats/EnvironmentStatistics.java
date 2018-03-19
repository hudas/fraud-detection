package org.ignas.frauddetection.transactionevaluation.domain.stats;

import org.ignas.frauddetection.transactionevaluation.domain.stats.details.FraudRate;

public class EnvironmentStatistics {

    private FraudRate time;
    private FraudRate location;
    private FraudRate creditor;

    public EnvironmentStatistics(FraudRate creditor, FraudRate location, FraudRate time) {
        this.creditor = creditor;
        this.location = location;
        this.time = time;
    }

    public FraudRate getTime() {
        return time;
    }

    public FraudRate getLocation() {
        return location;
    }

    public FraudRate getCreditor() {
        return creditor;
    }
}
