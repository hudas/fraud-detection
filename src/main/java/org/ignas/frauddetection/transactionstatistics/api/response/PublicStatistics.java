package org.ignas.frauddetection.transactionstatistics.api.response;

import org.ignas.frauddetection.transactionstatistics.api.response.generalindicators.*;

import java.util.List;

public class PublicStatistics {

    private List<SumStatistics> sum;

    private List<RatioStatistics> sumRatio;

    private List<CountStatistics> count;

    private List<TimeDifferenceStatistics> time;

    private List<DistanceDifferenceStatistics> distance;

    public PublicStatistics(
        List<SumStatistics> sum,
        List<RatioStatistics> sumRatio,
        List<CountStatistics> count,
        List<TimeDifferenceStatistics> time,
        List<DistanceDifferenceStatistics> distance) {

        this.sum = sum;
        this.sumRatio = sumRatio;
        this.count = count;
        this.time = time;
        this.distance = distance;
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

    public List<DistanceDifferenceStatistics> getDistance() {
        return distance;
    }
}
