package org.ignas.frauddetection.transactionstatistics.domain;

public class ExternalConditions {

    private ConditionOccurrences<String> creditor;
    private ConditionOccurrences<Integer> time;
    private ConditionOccurrences<String> location;

    public ExternalConditions(
        ConditionOccurrences<String> creditor,
        ConditionOccurrences<Integer> time,
        ConditionOccurrences<String> location) {

        this.creditor = creditor;
        this.time = time;
        this.location = location;
    }

    public ConditionOccurrences<String> getCreditor() {
        return creditor;
    }

    public ConditionOccurrences<Integer> getTime() {
        return time;
    }

    public ConditionOccurrences<String> getLocation() {
        return location;
    }
}
