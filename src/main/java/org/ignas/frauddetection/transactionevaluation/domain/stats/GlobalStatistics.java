package org.ignas.frauddetection.transactionevaluation.domain.stats;

import org.ignas.frauddetection.transactionevaluation.domain.stats.details.MeanPeriodStatistics;
import org.joda.time.Seconds;

import java.math.BigDecimal;
import java.util.List;

public class GlobalStatistics {

    private List<MeanPeriodStatistics<Float>> sum;

    private List<MeanPeriodStatistics<Integer>> count;

    private List<MeanPeriodStatistics<Seconds>> time;

    private List<MeanPeriodStatistics<Float>> distance;

    public GlobalStatistics(
        List<MeanPeriodStatistics<Float>> sum,
        List<MeanPeriodStatistics<Integer>> count,
        List<MeanPeriodStatistics<Seconds>> time,
        List<MeanPeriodStatistics<Float>> distance) {

        this.sum = sum;
        this.count = count;
        this.time = time;
        this.distance = distance;
    }
}
