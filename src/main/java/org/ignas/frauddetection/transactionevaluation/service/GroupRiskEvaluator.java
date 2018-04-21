package org.ignas.frauddetection.transactionevaluation.service;

import org.ignas.frauddetection.probabilitystatistics.api.response.CriteriaGroupRisk;
import org.ignas.frauddetection.probabilitystatistics.api.response.ProbabilityStatistics;
import org.ignas.frauddetection.transactionevaluation.cache.GroupProbabilityCache;
import org.ignas.frauddetection.transactionevaluation.domain.Risk;
import org.ignas.frauddetection.transactionevaluation.domain.config.FraudCriteriaConfig;
import org.ignas.frauddetection.transactionevaluation.domain.config.FraudCriteriaEvaluator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.*;

public class GroupRiskEvaluator {

    private FraudCriteriaEvaluator criteriaEvaluator;

    public GroupRiskEvaluator(FraudCriteriaEvaluator criteriaEvaluator) {
        this.criteriaEvaluator = criteriaEvaluator;
    }

    public Map<String, Risk.Value> evaluate(ProbabilityStatistics stats) {
        Float fraudProbability = stats.getFraudProbability();

        Map<String, List<Float>> criteriaGroupProbabilities = stats.getCriteriaProbabilites()
            .entrySet()
            .stream()
            .collect(
                groupingBy(
                    it -> criteriaEvaluator.resolveGroup(it.getKey()),
                    mapping(Map.Entry::getValue, toList()))
            );

        Map<String, List<Float>> criteriaGroupNonFraudProbabilities = stats.getCriteriaNonFraudProbabilites()
            .entrySet()
            .stream()
            .collect(
                groupingBy(
                    it -> criteriaEvaluator.resolveGroup(it.getKey()),
                    mapping(Map.Entry::getValue, toList()))
            );


        List<Risk> groupRisks = new ArrayList<>();
        for (Map.Entry<String, List<Float>> criteriaFraud: criteriaGroupProbabilities.entrySet()) {

            List<Float> fraudProbabilities = criteriaFraud.getValue();
            List<Float> nonFraudProbabilities = criteriaGroupNonFraudProbabilities.get(criteriaFraud.getKey());

            groupRisks.add(
                mapToRisk(
                    criteriaFraud.getKey(),
                    fraudProbabilities,
                    nonFraudProbabilities,
                    stats.getGroupStatistics()
                )
            );
        }


        return groupRisks.stream()
            .collect(toMap(Risk::getGroupName, (group) -> group.evaluate(fraudProbability)));
    }

    private Risk mapToRisk(String key, List<Float> probabilities, List<Float> nonFraudProbabilities, Map<String, CriteriaGroupRisk> groupStatistics) {
        CriteriaGroupRisk groupRiskStatistics = groupStatistics.get(key);

        return new Risk(
            key,
            probabilities,
            nonFraudProbabilities,
            groupRiskStatistics.getFraudProbabilityAverage(),
            groupRiskStatistics.getDeviationFromAverage()
        );
    }
}

