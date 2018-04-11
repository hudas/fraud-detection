package org.ignas.frauddetection.transactionevaluation.api.request;

public class BehaviourData {

    private float sumRatioDaily;

    private float sumRatioWeekly;

    private float sumRatioMonthly;

    private float timeDifferenceFromLast;

    private float distanceFromLast;

    private float distanceFromCommon;

    public BehaviourData(
        float sumRatioDaily,
        float sumRatioWeekly,
        float sumRatioMonthly,
        float timeDifferenceFromLast,
        float distanceFromLast,
        float distanceFromCommon) {

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

    public float getTimeDifferenceFromLast() {
        return timeDifferenceFromLast;
    }

    public float getDistanceFromLast() {
        return distanceFromLast;
    }

    public float getDistanceFromCommon() {
        return distanceFromCommon;
    }
}
