package org.ignas.frauddetection.transactionstatistic.api.response;

public class CredibilityScore {

    private Long numberOfFraudulentActivites;

    private Long numberOfTotalActivites;

    public CredibilityScore() {
    }

    public CredibilityScore(Long numberOfFraudulentActivites, Long numberOfTotalActivites) {
        this.numberOfFraudulentActivites = numberOfFraudulentActivites;
        this.numberOfTotalActivites = numberOfTotalActivites;
    }

    public Long getNumberOfFraudulentActivites() {
        return numberOfFraudulentActivites;
    }

    public Long getNumberOfTotalActivites() {
        return numberOfTotalActivites;
    }
}
