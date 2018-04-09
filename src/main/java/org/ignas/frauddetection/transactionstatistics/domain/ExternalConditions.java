package org.ignas.frauddetection.transactionstatistics.domain;

public class ExternalConditions {

    private ConditionStats creditor;
    private ConditionStats time;
    private ConditionStats location;

    public ExternalConditions(
        ConditionStats creditor,
        ConditionStats time,
        ConditionStats location) {

        this.creditor = creditor;
        this.time = time;
        this.location = location;
    }

    public ConditionStats getCreditor() {
        return creditor;
    }

    public ConditionStats getTime() {
        return time;
    }

    public ConditionStats getLocation() {
        return location;
    }
}
