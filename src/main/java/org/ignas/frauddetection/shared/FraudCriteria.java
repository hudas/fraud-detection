package org.ignas.frauddetection.shared;

import static org.ignas.frauddetection.shared.FraudCriteriaGroup.*;

public enum FraudCriteria {

    PERIOD_EXPENCES_SUM(AMOUNT),
    PERIOD_EXPECTED_EXPENCES_SUM(AMOUNT),
    PERIOD_EXPENSES_SUM_PERCENTAGE(AMOUNT),
    MAX_EXPENSES_SUM_PERCENTAGE(AMOUNT),
    MAX_TRANSACTION_VALUE(AMOUNT),
    TRANSACTION_AMOUNT_CATEGORY(AMOUNT),

    PERIOD_TRANSACTION_COUNT(COUNT),
    PERIOD_EXPECTED_TRANSACTION_COUNT(COUNT),
    MAX_TRANSACTION_COUNT(COUNT),

    TIME_RISK(TIME),
    TIME_DIFFERENCE(TIME),
    EXPECTED_TIME_DIFFERENCE(TIME),
    MIN_TIME_DIFFERENCE(TIME),

    LOCATION_RISK(LOCATION),
    CREDITOR_RISK(LOCATION),
    LOCATION_DIFFERENCE(LOCATION);


    private FraudCriteriaGroup group;

    public FraudCriteriaGroup group() {
        return group;
    }

    FraudCriteria(FraudCriteriaGroup group) {
        this.group = group;
    }
}
