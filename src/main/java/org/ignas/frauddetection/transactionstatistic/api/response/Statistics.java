package org.ignas.frauddetection.transactionstatistic.api.response;

public class Statistics {

    private DebtorTransactionStatistics debtorStatistics;

    private CredibilityScore creditorScore;

    private CredibilityScore timeScore;

    private CredibilityScore locationScore;

    public Statistics() {
    }

    public Statistics(
        DebtorTransactionStatistics debtorStatistics,
        CredibilityScore creditorScore,
        CredibilityScore timeScore,
        CredibilityScore locationScore) {

        this.debtorStatistics = debtorStatistics;
        this.creditorScore = creditorScore;
        this.timeScore = timeScore;
        this.locationScore = locationScore;
    }


    public DebtorTransactionStatistics getDebtorStatistics() {
        return debtorStatistics;
    }

    public CredibilityScore getCreditorScore() {
        return creditorScore;
    }

    public CredibilityScore getTimeScore() {
        return timeScore;
    }

    public CredibilityScore getLocationScore() {
        return locationScore;
    }

}
