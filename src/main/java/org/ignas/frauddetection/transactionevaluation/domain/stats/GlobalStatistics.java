package org.ignas.frauddetection.transactionevaluation.domain.stats;

import org.ignas.frauddetection.transactionevaluation.domain.stats.details.MeanPeriodStatistics;
import org.ignas.frauddetection.transactionevaluation.domain.stats.details.MeanStatistics;
import org.joda.time.Days;
import org.joda.time.Seconds;

import java.util.List;

public class GlobalStatistics {

    private List<MeanPeriodStatistics<Float>> sum;

    private List<MeanPeriodStatistics<Float>> singleAmountRatio;

    private List<MeanPeriodStatistics<Float>> count;

    private MeanStatistics<Seconds> time;

    private MeanStatistics<Float> distanceToCommonLocation;

    private MeanStatistics<Float> distanceToLastLocation;

    public GlobalStatistics(
        List<MeanPeriodStatistics<Float>> sum,
        List<MeanPeriodStatistics<Float>> singleAmountRatio,
        List<MeanPeriodStatistics<Float>> count,
        MeanStatistics<Seconds> time,
        MeanStatistics<Float> distanceToCommonLocation,
        MeanStatistics<Float> distanceToLastLocation) {

        this.sum = sum;
        this.singleAmountRatio = singleAmountRatio;
        this.count = count;
        this.time = time;
        this.distanceToCommonLocation = distanceToCommonLocation;
        this.distanceToLastLocation = distanceToLastLocation;
    }

    public MeanStatistics<Seconds> getTimeDifference() {
        return time;
    }

    public MeanStatistics<Float> getDistanceToCommonLocation() {
        return distanceToCommonLocation;
    }

    public MeanStatistics<Float> getDistanceToLastLocation() {
        return distanceToLastLocation;
    }

    public MeanPeriodStatistics<Float> sumStatisticsForPeriod(Days period) {
        return getPeriodStatistics(sum, period);
    }

    public MeanPeriodStatistics<Float> countStatisticsForPeriod(Days period) {
        return getPeriodStatistics(count, period);
    }

    public MeanPeriodStatistics<Float> ratioStatisticsForPeriod(Days period) {
        return getPeriodStatistics(singleAmountRatio, period);
    }

    private <T> MeanPeriodStatistics<T> getPeriodStatistics(List<MeanPeriodStatistics<T>> periodStatistics, Days period) {
        return periodStatistics.stream()
                .filter(stats -> stats.isForPeriod(period))
                .findAny()
                .orElseThrow(IllegalStateException::new);
    }
}
