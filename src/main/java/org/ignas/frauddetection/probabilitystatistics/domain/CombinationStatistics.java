package org.ignas.frauddetection.probabilitystatistics.domain;

import java.util.Objects;

public class CombinationStatistics {

    private String group;

    private String code;

    private long occurences;

    private long fraudOccurences;

    public CombinationStatistics() {
    }

    public CombinationStatistics(String group, String code, long occurences, long fraudOccurences) {
        this.group = group;
        this.code = code;
        this.occurences = occurences;
        this.fraudOccurences = fraudOccurences;
    }

    public String getGroup() {
        return group;
    }

    public String getCode() {
        return code;
    }

    public long getOccurences() {
        return occurences;
    }

    public long getFraudOccurences() {
        return fraudOccurences;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CombinationStatistics that = (CombinationStatistics) o;
        return Objects.equals(group, that.group) &&
            Objects.equals(code, that.code);
    }

    @Override
    public int hashCode() {

        return Objects.hash(group, code);
    }
}
