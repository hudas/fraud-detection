package org.ignas.frauddetection.transactionevaluation.service;

import org.ignas.frauddetection.probabilitystatistics.api.response.CriteriaGroupRisk;
import org.ignas.frauddetection.probabilitystatistics.api.response.ProbabilityStatistics;
import org.ignas.frauddetection.transactionevaluation.cache.GroupProbabilityCache;
import org.ignas.frauddetection.transactionevaluation.domain.Risk;
import org.ignas.frauddetection.transactionevaluation.domain.config.FraudCriteriaConfig;

import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.*;

public class FraudEvaluator {

    public static Float evaluate(GroupProbabilityCache cache, ProbabilityStatistics criteriaProbabilities) {
        Float fraudProbability = criteriaProbabilities.getFraudProbability();

        Map<String, List<Float>> criteriaGroupProbabilities = criteriaProbabilities.getCriteriaProbabilites()
            .entrySet()
            .stream()
            .collect(
                groupingBy(
                    it -> FraudCriteriaConfig.resolveGroup(it.getKey()),
                    mapping(Map.Entry::getValue, toList()))
            );

        Map<String, Risk.Value> groupRisks = criteriaGroupProbabilities.entrySet()
            .stream()
            .map(entry -> mapToRisk(
                entry.getKey(),
                entry.getValue(),
                criteriaProbabilities.getGroupStatistics()))
            .collect(toMap(Risk::getGroupName, (group) -> group.evaluate(fraudProbability)));

        return groupRisks.entrySet()
            .stream()
            .map(entry -> cache.getProbability(entry.getKey(), entry.getValue().name()))
            .reduce(fraudProbability, (result, increment) -> result * increment);
    }

    private static Risk mapToRisk(String key, List<Float> probabilities, Map<String, CriteriaGroupRisk> groupStatistics) {
        CriteriaGroupRisk groupRiskStatistics = groupStatistics.get(key);

        return new Risk(
            key,
            probabilities,
            groupRiskStatistics.getFraudProbabilityAverage(),
            groupRiskStatistics.getDeviationFromAverage()
        );
    }
}

