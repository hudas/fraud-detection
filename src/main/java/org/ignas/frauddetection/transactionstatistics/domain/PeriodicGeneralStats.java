package org.ignas.frauddetection.transactionstatistics.domain;

import java.util.HashMap;
import java.util.Map;

public class PeriodicGeneralStats {

    private Map<String, Map<String, PeriodStats>> statsMap = new HashMap<>();

    public void add(String type, String period, PeriodStats stats) {
        statsMap.computeIfAbsent(type, it -> new HashMap<String, PeriodStats>())
            .put(period, stats);
    }

    public PeriodStats get(String type, String period) {
        return statsMap.get(type).get(period);
    }
}
