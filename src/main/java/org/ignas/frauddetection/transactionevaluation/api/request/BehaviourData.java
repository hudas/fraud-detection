package org.ignas.frauddetection.transactionevaluation.api.request;

public class BehaviourData {

    private float sumRatioDaily;

    private float sumRatioWeekly;

    private float sumRatioMonthly;

    private long timeDifferenceFromLast;

    private long distanceFromLast;

    private long distanceFromCommon;

    public BehaviourData(
        float sumRatioDaily,
        float sumRatioWeekly,
        float sumRatioMonthly,
        long timeDifferenceFromLast,
        long distanceFromLast,
        long distanceFromCommon) {

        this.sumRatioDaily = sumRatioDaily;
        this.sumRatioWeekly = sumRatioWeekly;
        this.sumRatioMonthly = sumRatioMonthly;
        this.timeDifferenceFromLast = timeDifferenceFromLast;
        this.distanceFromLast = distanceFromLast;
        this.distanceFromCommon = distanceFromCommon;
    }

    public float getSumRatioDaily() {
        return sumRatioDaily;
    }

    public float getSumRatioWeekly() {
        return sumRatioWeekly;
    }

    public float getSumRatioMonthly() {
        return sumRatioMonthly;
    }

    public long getTimeDifferenceFromLast() {
        return timeDifferenceFromLast;
    }

    public long getDistanceFromLast() {
        return distanceFromLast;
    }

    public long getDistanceFromCommon() {
        return distanceFromCommon;
    }
}
