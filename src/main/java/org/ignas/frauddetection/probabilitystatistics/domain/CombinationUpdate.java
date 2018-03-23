package org.ignas.frauddetection.probabilitystatistics.domain;

import java.util.Objects;

public class CombinationUpdate {

    private String group;

    private String code;

    private long occurencesBeforeUpdate;

    private long occurrencesDelta;

    private long fraudOccurrencesBeforeUpdate;

    private long fraudOccurrencesDelta;

    public CombinationUpdate(String group, String code) {
        this.group = group;
        this.code = code;
    }

    public void setBeforeState(long occurrencesBeforeUpdate, long fraudOccurrencesBeforeUpdate) {
        this.occurencesBeforeUpdate = occurrencesBeforeUpdate;
        this.fraudOccurrencesBeforeUpdate = fraudOccurrencesBeforeUpdate;
    }

    public void addIncrements(long occurrencesDelta, long fraudOccurrencesDelta) {
        this.occurrencesDelta += occurrencesDelta;
        this.fraudOccurrencesDelta += fraudOccurrencesDelta;
    }

    public long getFraudOccurrencesBeforeUpdate() {
        return fraudOccurrencesBeforeUpdate;
    }

    public long getFraudOccurrencesDelta() {
        return fraudOccurrencesDelta;
    }
}
