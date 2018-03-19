package org.ignas.frauddetection.transactionstatistics.api.response;

import org.ignas.frauddetection.transactionstatistics.api.response.generalindicators.*;

import java.util.List;

public class PublicStatistics {

    private List<SumStatistics> sum;

    private List<RatioStatistics> sumRatio;

    private List<CountStatistics> count;

    private List<TimeDifferenceStatistics> time;

    private List<DistanceDifferenceStatistics> distanceToCommon;

    private List<DistanceDifferenceStatistics> distanceToLast;

    public PublicStatistics(
        List<SumStatistics> sum,
        List<RatioStatistics> sumRatio,
        List<CountStatistics> count,
        List<TimeDifferenceStatistics> time,
        List<DistanceDifferenceStatistics> distanceToCommon,
        List<DistanceDifferenceStatistics> distanceToLast) {

        this.sum = sum;
        this.sumRatio = sumRatio;
        this.count = count;
        this.time = time;
        this.distanceToCommon = distanceToCommon;
        this.distanceToLast = distanceToLast;
    }

    public List<SumStatistics> getSum() {
        return sum;
    }

    public List<RatioStatistics> getSumRatio() {
        return sumRatio;
    }

    public List<CountStatistics> getCount() {
        return count;
    }

    public List<TimeDifferenceStatistics> getTime() {
        return time;
    }

    public List<DistanceDifferenceStatistics> getDistanceToCommon() {
        return distanceToCommon;
    }

    public List<DistanceDifferenceStatistics> getDistanceToLast() {
        return distanceToLast;
    }
}
