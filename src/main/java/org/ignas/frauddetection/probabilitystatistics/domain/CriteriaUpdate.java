package org.ignas.frauddetection.probabilitystatistics.domain;

public class CriteriaUpdate {

    private String criteria;
    private String value;

    private int newFraudOccurences;

    private int newOccurences;

    public CriteriaUpdate(String criteria, String value, int newFraudOccurences, int newOccurences) {
        this.criteria = criteria;
        this.value = value;
        this.newFraudOccurences = newFraudOccurences;
        this.newOccurences = newOccurences;
    }

    public static CriteriaUpdate empty(String criteria, String value) {
        return new CriteriaUpdate(criteria, value, 0, 0);
    }

    /**
     * The idea behind this is that we get notification that transaction was fraudulent only after client notified us,
     * therefore transaction was already processed and it should not affect new occurrences count,
     * however previously it was processed as non fraudulent, therefore if it is fraudulent we increase fraudulent count.
     * @param increment
     */
    public void apply(CriteriaUpdateIncrement increment) {
        if (!increment.isAlreadyProcessedTransaction()) {
             newOccurences++;
        }

        if (increment.isFraudulent()) {
            newFraudOccurences++;
        }
    }

    public boolean causesSideEffects() {
        return newOccurences != 0 || newFraudOccurences != 0;
    }

    public String targetCriteria() {
        return criteria;
    }

    public String criteriaValue() {
        return value;
    }

    public int getNewFraudOccurences() {
        return newFraudOccurences;
    }

    public int getNewOccurences() {
        return newOccurences;
    }
}
