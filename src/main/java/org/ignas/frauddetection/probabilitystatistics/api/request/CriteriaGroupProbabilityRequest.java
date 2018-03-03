package org.ignas.frauddetection.probabilitystatistics.api.request;

import java.util.List;

public class CriteriaGroupProbabilityRequest {

    private List<String> groups;

    public CriteriaGroupProbabilityRequest(List<String> groups) {
        this.groups = groups;
    }

    public List<String> getGroups() {
        return groups;
    }
}
