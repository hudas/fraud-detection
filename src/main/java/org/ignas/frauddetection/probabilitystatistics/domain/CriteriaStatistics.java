package org.ignas.frauddetection.probabilitystatistics.domain;

import java.util.Objects;

public class CriteriaStatistics {

    private String name;

    private String value;

    private long occurrences;

    private long fraudOccurrences;


    public CriteriaStatistics(String name, String code, long occurrences, long fraudOccurrences) {
        this.name = name;
        this.value = code;
        this.occurrences = occurrences;
        this.fraudOccurrences = fraudOccurrences;
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    public long getOccurrences() {
        return occurrences;
    }

    public long getFraudOccurrences() {
        return fraudOccurrences;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CriteriaStatistics that = (CriteriaStatistics) o;
        return Objects.equals(name, that.name) &&
            Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {

        return Objects.hash(name, value);
    }
}
