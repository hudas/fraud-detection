package org.ignas.frauddetection.transactionevaluation.integration.converters;

import org.ignas.frauddetection.probabilitystatistics.api.request.CriteriaGroupProbabilityRequest;
import org.ignas.frauddetection.probabilitystatistics.api.request.CriteriaProbabilityRequest;
import org.ignas.frauddetection.probabilitystatistics.api.response.BayesTable;
import org.ignas.frauddetection.transactionevaluation.domain.CriteriaGroup;

import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toMap;

public class GroupProbabilityAPIMapper {

    /**
     * Currently ridiculously simple mapper which does not require separate class,
     *  however in future in case API changes, this will isolate changes in mapper
     *
     * @param groups
     * @return
     */
    public static CriteriaGroupProbabilityRequest mapRequest(List<String> groups) {
        return new CriteriaGroupProbabilityRequest(groups);
    }

    public static Map<String, CriteriaGroup> mapResponse(BayesTable result) {
        return result.getTable()
            .entrySet()
            .stream()
            .collect(toMap(Map.Entry::getKey, entry -> new CriteriaGroup(entry.getValue())));
    }
}
