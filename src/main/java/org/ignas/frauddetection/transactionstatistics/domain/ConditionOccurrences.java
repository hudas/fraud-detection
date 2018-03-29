package org.ignas.frauddetection.transactionstatistics.domain;

public class ConditionOccurrences<T> {

    private T name;

    private Long occurrences;
    private Long fraudOccurrences;

    public ConditionOccurrences(T name, Long occurrences, Long fraudOccurrences) {
        this.name = name;
        this.occurrences = occurrences;
        this.fraudOccurrences = fraudOccurrences;
    }

    public static <T> ConditionOccurrences empty(T condition) {
        return new ConditionOccurrences<>(condition, 0l, 0l);
    }

    public void increaseOccurrences(int nonFraudIncrement, int fraudIncrement) {
        this.occurrences += nonFraudIncrement;
        this.fraudOccurrences += fraudIncrement;
    }

    public String getName() {
        return name.toString();
    }

    public Long getOccurrences() {
        return occurrences;
    }

    public Long getFraudOccurrences() {
        return fraudOccurrences;
    }
}
