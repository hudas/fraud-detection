package org.ignas.frauddetection.transactionstatistic.api.response;

import java.math.BigDecimal;
import java.util.List;

public class DebtorTransactionStatistics {

    private BigDecimal mostValuableTransaction;

    private List<PersonalPeriodStatistics> personalStatistic;

    private List<GeneralPeriodStatistics> generalStatistic;

    public DebtorTransactionStatistics(
        BigDecimal mostValuableTransaction,
        List<PersonalPeriodStatistics> personalStatistic,
        List<GeneralPeriodStatistics> generalStatistic) {
        this.mostValuableTransaction = mostValuableTransaction;
        this.personalStatistic = personalStatistic;
        this.generalStatistic = generalStatistic;
    }

    public BigDecimal getMostValuableTransaction() {
        return mostValuableTransaction;
    }

    public List<PersonalPeriodStatistics> getPersonalStatistic() {
        return personalStatistic;
    }

    public List<GeneralPeriodStatistics> getGeneralStatistic() {
        return generalStatistic;
    }
}
