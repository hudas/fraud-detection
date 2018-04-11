package org.ignas.frauddetection.transactionevaluation.domain.config;

import org.ignas.frauddetection.transactionevaluation.domain.Transaction;
import org.ignas.frauddetection.transactionevaluation.domain.calculation.EvaluationResult;
import org.ignas.frauddetection.transactionevaluation.domain.calculation.PrintableResult;
import org.ignas.frauddetection.transactionevaluation.domain.calculation.criteria.NamedCriteria;
import org.ignas.frauddetection.transactionevaluation.domain.stats.HistoricalData;

import java.util.Map;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toMap;

public class FraudCriteriaEvaluator {

    // Idea behind this, is to build inverted hash map which helps to resolve criteria group by criteria name.
    private final Map<String, String> CRITERIA_GROUP_CACHE;

    private FraudCriteriaConfig criteriaConfig;

    public FraudCriteriaEvaluator() {
        this(new FraudCriteriaConfig());
    }

    public FraudCriteriaEvaluator(FraudCriteriaConfig criteriaConfig) {
        this.criteriaConfig = criteriaConfig;

        CRITERIA_GROUP_CACHE = criteriaConfig.definedCriteria()
            .stream()
            .collect(Collectors.toMap(NamedCriteria::name, NamedCriteria::group));

    }

    public Map<String, EvaluationResult> evaluateAll(Transaction transactionData, HistoricalData data) {
        return criteriaConfig.definedCriteria()
            .stream()
            .collect(toMap(
                NamedCriteria::name,
                it -> it.evaluate(transactionData, data))
            );
    }

    public EvaluationResult evaluate(String criteria, Transaction transactionData, HistoricalData data) {
        return criteriaConfig.definedCriteria()
            .stream()
            .filter(defined -> defined.name().equals(criteria))
            .findAny()
            .orElseThrow(IllegalArgumentException::new)
            .evaluate(transactionData, data);
    }

    public String resolveGroup(String criteriaName) {
        return CRITERIA_GROUP_CACHE.get(criteriaName);
    }
}
