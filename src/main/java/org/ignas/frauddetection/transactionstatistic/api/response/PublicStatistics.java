package org.ignas.frauddetection.transactionstatistic.api.response;

import org.ignas.frauddetection.transactionstatistic.api.response.generalindicators.CountStatistics;
import org.ignas.frauddetection.transactionstatistic.api.response.generalindicators.DistanceDifferenceStatistics;
import org.ignas.frauddetection.transactionstatistic.api.response.generalindicators.SumStatistics;
import org.ignas.frauddetection.transactionstatistic.api.response.generalindicators.TimeDifferenceStatistics;
import org.joda.time.Days;

import java.util.List;

public class PublicStatistics {

    private List<SumStatistics> sum;

    private List<CountStatistics> count;

    private List<TimeDifferenceStatistics> time;

    private List<DistanceDifferenceStatistics> distance;

    public PublicStatistics(
        List<SumStatistics> sum,
        List<CountStatistics> count,
        List<TimeDifferenceStatistics> time,
        List<DistanceDifferenceStatistics> distance) {

        this.sum = sum;
        this.count = count;
        this.time = time;
        this.distance = distance;
    }

    public List<SumStatistics> getSum() {
        return sum;
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
