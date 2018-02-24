package org.ignas.frauddetection.transactionstatistic.api.response;

import org.ignas.frauddetection.transactionstatistic.api.response.generalindicators.CountStatistics;
import org.ignas.frauddetection.transactionstatistic.api.response.generalindicators.DistanceDifferenceStatistics;
import org.ignas.frauddetection.transactionstatistic.api.response.generalindicators.SumStatistics;
import org.ignas.frauddetection.transactionstatistic.api.response.generalindicators.TimeDifferenceStatistics;
import org.joda.time.Days;

public class GeneralPeriodStatistics {

    private Days periodLength;

    private SumStatistics sum;

    private CountStatistics count;

    private TimeDifferenceStatistics time;

    private DistanceDifferenceStatistics distance;


    public GeneralPeriodStatistics() {
    }

    public GeneralPeriodStatistics(
        Days periodLength,
        SumStatistics sum,
        CountStatistics count,
        TimeDifferenceStatistics time,
        DistanceDifferenceStatistics distance) {

        this.periodLength = periodLength;
        this.sum = sum;
        this.count = count;
        this.time = time;
        this.distance = distance;
    }

    public Days getPeriodLength() {
        return periodLength;
    }

    public SumStatistics getSum() {
        return sum;
    }

    public CountStatistics getCount() {
        return count;
    }

    public TimeDifferenceStatistics getTime() {
        return time;
    }

    public DistanceDifferenceStatistics getDistance() {
        return distance;
    }
}
