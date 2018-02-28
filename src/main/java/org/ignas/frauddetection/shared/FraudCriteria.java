package org.ignas.frauddetection.shared;

public enum FraudCriteria {
    PERIOD_EXPENCES_SUM,
    PERIOD_EXPECTED_EXPENCES_SUM,
    PERIOD_EXPENSES_SUM_PERCENTAGE,
    PERIOD_TRANSACTION_COUNT,
    PERIOD_EXPECTED_TRANSACTION_COUNT,

    MAX_EXPENSES_SUM_PERCENTAGE,
    MAX_TRANSACTION_VALUE,
    MAX_TRANSACTION_COUNT,
    MIN_TIME_DIFFERENCE,

    TRANSACTION_AMOUNT_CATEGORY,

    TIME_RISK,
    LOCATION_RISK,
    CREDITOR_RISK,

    TIME_DIFFERENCE,
    EXPECTED_TIME_DIFFERENCE,
    LOCATION_DIFFERENCE
}
