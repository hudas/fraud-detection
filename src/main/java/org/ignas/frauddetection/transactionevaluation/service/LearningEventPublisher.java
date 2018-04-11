package org.ignas.frauddetection.transactionevaluation.service;

import org.ignas.frauddetection.shared.OneWayServiceIntegration;
import org.ignas.frauddetection.transactionevaluation.api.request.BehaviourData;
import org.ignas.frauddetection.transactionevaluation.api.request.LearningRequest;
import org.ignas.frauddetection.transactionevaluation.api.request.TransactionData;
import org.ignas.frauddetection.transactionevaluation.domain.Risk;
import org.ignas.frauddetection.transactionevaluation.domain.config.FraudCriteriaEvaluator;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class LearningEventPublisher {

    private FraudCriteriaEvaluator criteriaEvaluator;

    private OneWayServiceIntegration<LearningRequest> learningInitiationIntegration;

    public LearningEventPublisher(
        FraudCriteriaEvaluator criteriaEvaluator,
        OneWayServiceIntegration<LearningRequest> learningInitiationIntegration) {

        this.criteriaEvaluator = criteriaEvaluator;
        this.learningInitiationIntegration = learningInitiationIntegration;
    }

    public void publishData(
        TransactionData requestData,
        BehaviourData behaviour,
        Map<String, String> criteriaValues,
        Map<String, Risk.Value> groupValues) {

        Map<String, String> groupValueRepresentations = mapValuesToRepresentations(groupValues);

        Map<String, Map<String, String>> groupedCriteriaValues = groupCriteriaValues(criteriaValues);

        learningInitiationIntegration.publish(
            new LearningRequest(
                false,
                requestData,
                behaviour,
                groupedCriteriaValues,
                groupValueRepresentations
            )
        );
    }

    private Map<String, Map<String, String>> groupCriteriaValues(Map<String, String> criteriaValues) {
        Map<String, Map<String, String>> groupedCriteriaValues = new HashMap<>();

        for (Map.Entry<String, String> entries : criteriaValues.entrySet()) {
            String group = criteriaEvaluator.resolveGroup(entries.getKey());

            Map<String, String> values = groupedCriteriaValues.computeIfAbsent(group, k -> new HashMap<>());

            values.put(entries.getKey(), entries.getValue());
        }

        return groupedCriteriaValues;
    }

    private Map<String, String> mapValuesToRepresentations(Map<String, Risk.Value> groupValues) {
        return groupValues.entrySet()
            .stream()
            .collect(Collectors.toMap(Map.Entry::getKey, value -> value.getValue().name()));
    }
}
