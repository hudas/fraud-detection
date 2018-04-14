package org.ignas.frauddetection.transactionstatistics.domain;

public class ConditionTotals {

    private ConditionTotalValue creditorTotal;
    private ConditionTotalValue timeTotal;
    private ConditionTotalValue locationTotal;

    public static ConditionTotals unknown() {
        ConditionTotals totals = new ConditionTotals();

        totals.setCreditorTotal(ConditionTotalValue.unknown());
        totals.setTimeTotal(ConditionTotalValue.unknown());
        totals.setLocationTotal(ConditionTotalValue.unknown());

        return totals;
    }

    public void setCreditorTotal(ConditionTotalValue creditorTotal) {
        this.creditorTotal = creditorTotal;
    }

    public void setTimeTotal(ConditionTotalValue timeTotal) {
        this.timeTotal = timeTotal;
    }

    public void setLocationTotal(ConditionTotalValue locationTotal) {
        this.locationTotal = locationTotal;
    }

    public ConditionTotalValue getCreditorTotal() {
        return creditorTotal;
    }

    public ConditionTotalValue getTimeTotal() {
        return timeTotal;
    }

    public ConditionTotalValue getLocationTotal() {
        return locationTotal;
    }
}
