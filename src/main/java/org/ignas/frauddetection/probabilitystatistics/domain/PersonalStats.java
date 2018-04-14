package org.ignas.frauddetection.probabilitystatistics.domain;

import org.ignas.frauddetection.transactionevaluation.api.request.LearningRequest;
import org.ignas.frauddetection.transactionevaluation.api.request.TransactionData;
import org.ignas.frauddetection.transactionstatistics.domain.LocationService;
import org.joda.time.LocalDateTime;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class PersonalStats {

    private String debtor;

    private PersonalTransactionStats latestTransaction;

    private float maxAmount = 0;

    private Long minTimeDiff;

    private Map<String, Long> locationOccurences = new HashMap<>();

    private Map<Integer, Long> timeOccurences = new HashMap<>();

    private Map<Integer, PersonalPeriodStats> periods = new HashMap<>();

    public PersonalStats(String debtor) {
        this.debtor = debtor;
    }

    public void updateFrom(LearningRequest request) {
        TransactionData requestTransaction = request.getTransaction();

        if (latestTransaction == null
            || latestTransaction != null && requestTransaction.getTime().isAfter(latestTransaction.getTime())) {


            latestTransaction = new PersonalTransactionStats(
                requestTransaction.getLocation(),
                requestTransaction.getAmount(),
                requestTransaction.getTime()
            );
        }

        upateAmount(requestTransaction.getAmount());
        updateMinTimeDiff(Float.valueOf(request.getBehaviourData().getTimeDifferenceFromLast()).longValue());

        String shortCode = LocationService.toNearestArea(requestTransaction.getLocation()).toString();
        Integer hourOfDay = requestTransaction.getTime().getHourOfDay();

        incLocationCount(shortCode);
        incTimeCount(hourOfDay);

        periods.computeIfAbsent(1, PersonalPeriodStats::new).add(requestTransaction);
        periods.computeIfAbsent(7, PersonalPeriodStats::new).add(requestTransaction);
        periods.computeIfAbsent(30, PersonalPeriodStats::new).add(requestTransaction);
    }

    private void updateMinTimeDiff(long newTimeDiff) {
        if (newTimeDiff == 0f) {
            return;
        }

        if (minTimeDiff == null || minTimeDiff < 0
            || minTimeDiff > newTimeDiff) {
            minTimeDiff = newTimeDiff;
        }
    }

    private void upateAmount(float newAmount) {
        if (maxAmount < newAmount) {
            maxAmount = newAmount;
        }
    }

    public void incLocationCount(String areaCode) {
        incLocationCount(areaCode, 1l);
    }

    public void incLocationCount(String areaCode, Long increment) {
        Long current = locationOccurences.computeIfAbsent(areaCode, code -> 0l);
        locationOccurences.put(areaCode, current + increment);
    }

    public void incTimeCount(Integer hourOfDay) {
        incTimeCount(hourOfDay, 1l);
    }

    public void incTimeCount(Integer hourOfDay, Long increment) {
        Long current = timeOccurences.computeIfAbsent(hourOfDay, code -> 0l);
        timeOccurences.put(hourOfDay, current + increment);
    }

    public String getDebtor() {
        return debtor;
    }

    public void setPeriods(List<PersonalPeriodStats> periods) {
        for(PersonalPeriodStats period: periods) {
            this.periods.put(period.getLength(), period);
        }
    }

    public void setLatestTransaction(PersonalTransactionStats latestTransaction) {
        this.latestTransaction = latestTransaction;
    }

    public void setMaxAmount(float maxAmount) {
        this.maxAmount = maxAmount;
    }

    public void setMinTimeDiff(Long minTimeDiff) {
        this.minTimeDiff = minTimeDiff;
    }

    public PersonalStats mergeWith(PersonalStats increment) {
        PersonalTransactionStats incrementLatest = increment.getLatestTransaction();

        if (latestTransaction == null
            || incrementLatest.getTime().isAfter(latestTransaction.getTime())) {

            latestTransaction = new PersonalTransactionStats(
                incrementLatest.getLocation(),
                incrementLatest.getAmount(),
                incrementLatest.getTime()
            );
        }

        upateAmount(increment.getMaxAmount());
        if (increment.getMinTimeDiff() == null || increment.getMinTimeDiff() <= 0) {
            updateMinTimeDiff(Long.MAX_VALUE);
        } else {
            updateMinTimeDiff(increment.getMinTimeDiff());
        }


        for (Map.Entry<String, Long> occurrence: increment.getLocationOccurences().entrySet()) {
            incLocationCount(occurrence.getKey(), occurrence.getValue());
        }

        for (Map.Entry<Integer, Long> occurrence: increment.getTimeOccurences().entrySet()) {
            incTimeCount(occurrence.getKey(), occurrence.getValue());
        }

        for (Map.Entry<Integer, PersonalPeriodStats> periodIncrement : increment.getPeriods().entrySet()) {
            periods.computeIfAbsent(periodIncrement.getKey(), PersonalPeriodStats::new)
                .updateFrom(periodIncrement.getValue());
        }

        return this;
    }

    public PersonalTransactionStats getLatestTransaction() {
        return latestTransaction;
    }

    public float getMaxAmount() {
        return maxAmount;
    }

    public Long getMinTimeDiff() {
        return minTimeDiff;
    }

    public Map<String, Long> getLocationOccurences() {
        return locationOccurences;
    }

    public Map<Integer, Long> getTimeOccurences() {
        return timeOccurences;
    }

    public Map<Integer, PersonalPeriodStats> getPeriods() {
        return periods;
    }

    public void removeOutdatedTransactions(LocalDateTime from) {
        for (PersonalPeriodStats period: periods.values()) {
            period.removeOutdated(from);
        }
    }
}
