package org.ignas.frauddetection.transactionevaluation.domain.stats;

import org.ignas.frauddetection.transactionevaluation.domain.stats.details.FraudRate;

public class EnvironmentStatistics {

    private FraudRate time;
    private FraudRate location;
    private FraudRate creditor;

    public EnvironmentStatistics(FraudRate time, FraudRate location, FraudRate creditor) {
        this.time = time;
        this.location = location;
        this.creditor = creditor;
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
