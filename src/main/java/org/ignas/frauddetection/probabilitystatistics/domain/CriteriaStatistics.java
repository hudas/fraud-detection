package org.ignas.frauddetection.probabilitystatistics.domain;

import java.util.Objects;

public class CriteriaStatistics {

    private String name;

    private String value;

    private Long occurrences;

    private Long fraudOccurrences;


    public CriteriaStatistics(String name, String code, Long occurrences, Long fraudOccurrences) {
        this.name = name;
        this.value = code;
        this.occurrences = occurrences != null ? occurrences : 0;
        this.fraudOccurrences = fraudOccurrences != null ? fraudOccurrences : 0;
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    public Long getOccurrences() {
        return occurrences;
    }

    public Long getFraudOccurrences() {
        return fraudOccurrences;
    }

    public Long getNonFraudOccurences() {
        return occurrences - fraudOccurrences;
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
