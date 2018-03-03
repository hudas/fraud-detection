package org.ignas.frauddetection.shared;

import static org.ignas.frauddetection.shared.FraudCriteriaGroup.*;

public enum FraudCriteria {

    PERIOD_EXPENCES_SUM(TRANSACTION_AMOUNT),
    PERIOD_EXPECTED_EXPENCES_SUM(TRANSACTION_AMOUNT),
    PERIOD_EXPENSES_SUM_PERCENTAGE(TRANSACTION_AMOUNT),
    MAX_EXPENSES_SUM_PERCENTAGE(TRANSACTION_AMOUNT),
    MAX_TRANSACTION_VALUE(TRANSACTION_AMOUNT),
    TRANSACTION_AMOUNT_CATEGORY(TRANSACTION_AMOUNT),

    PERIOD_TRANSACTION_COUNT(TRANSACTION_COUNT),
    PERIOD_EXPECTED_TRANSACTION_COUNT(TRANSACTION_COUNT),
    MAX_TRANSACTION_COUNT(TRANSACTION_COUNT),

    TIME_RISK(TRANSACTION_TIME),
    TIME_DIFFERENCE(TRANSACTION_TIME),
    EXPECTED_TIME_DIFFERENCE(TRANSACTION_TIME),
    MIN_TIME_DIFFERENCE(TRANSACTION_TIME),

    LOCATION_RISK(TRANSACTION_LOCATION),
    CREDITOR_RISK(TRANSACTION_LOCATION),
    LOCATION_DIFFERENCE(TRANSACTION_LOCATION);


    private FraudCriteriaGroup group;

    public FraudCriteriaGroup group() {
        return group;
    }

    FraudCriteria(FraudCriteriaGroup group) {
        this.group = group;
    }
}
