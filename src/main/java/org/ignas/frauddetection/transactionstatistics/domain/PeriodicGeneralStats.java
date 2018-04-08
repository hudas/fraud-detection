package org.ignas.frauddetection.transactionstatistics.domain;

public class PeriodicGeneralStats {

    private PeriodStats daily;
    private PeriodStats weekly;
    private PeriodStats monthly;

    public PeriodicGeneralStats() {
    }

    public void setDaily(PeriodStats daily) {
        this.daily = daily;
    }

    public void setWeekly(PeriodStats weekly) {
        this.weekly = weekly;
    }

    public void setMonthly(PeriodStats monthly) {
        this.monthly = monthly;
    }

    public PeriodStats getDaily() {
        return daily;
    }

    public PeriodStats getWeekly() {
        return weekly;
    }

    public PeriodStats getMonthly() {
        return monthly;
    }
}
