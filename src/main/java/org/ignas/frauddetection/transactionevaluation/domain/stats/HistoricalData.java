package org.ignas.frauddetection.transactionevaluation.domain.stats;

public class HistoricalData {

    private DebtorStatistics debtor;

    private GlobalStatistics global;

    private EnvironmentStatistics environment;

    public HistoricalData(
        DebtorStatistics debtor,
        GlobalStatistics global,
        EnvironmentStatistics environment) {

        this.debtor = debtor;
        this.global = global;
        this.environment = environment;
    }

    public DebtorStatistics getDebtor() {
        return debtor;
    }

    public GlobalStatistics getGlobal() {
        return global;
    }

    public EnvironmentStatistics getEnvironment() {
        return environment;
    }
}
