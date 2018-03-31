package org.ignas.frauddetection.transactionevaluation.api.request;

public class BehaviourData {

    private long timeDifferenceFromLast;

    private long distanceFromLast;

    private long distanceFromCommon;

    public BehaviourData(
        long timeDifferenceFromLast,
        long distanceFromLast,
        long distanceFromCommon) {

        this.timeDifferenceFromLast = timeDifferenceFromLast;
        this.distanceFromLast = distanceFromLast;
        this.distanceFromCommon = distanceFromCommon;
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
