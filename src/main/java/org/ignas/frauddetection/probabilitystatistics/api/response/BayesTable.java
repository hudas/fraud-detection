package org.ignas.frauddetection.probabilitystatistics.api.response;

import java.util.Map;

public class BayesTable {

    private Map<String, Map<String, Float>> table;

    public BayesTable(Map<String, Map<String, Float>> table) {
        this.table = table;
    }

    public Map<String, Map<String, Float>> getTable() {
        return table;
    }
}
