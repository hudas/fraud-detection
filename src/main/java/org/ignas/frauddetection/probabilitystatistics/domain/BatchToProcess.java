package org.ignas.frauddetection.probabilitystatistics.domain;

import org.ignas.frauddetection.transactionevaluation.api.request.LearningRequest;

import java.util.ArrayList;
import java.util.List;

public class BatchToProcess {

    private List<LearningRequest> items = new ArrayList<>();

    public BatchToProcess(List<LearningRequest> items) {
        this.items = items;
    }

    public List<LearningRequest> getItems() {
        return items;
    }
}
