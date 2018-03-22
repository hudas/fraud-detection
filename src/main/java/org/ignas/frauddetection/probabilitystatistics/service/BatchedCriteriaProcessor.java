package org.ignas.frauddetection.probabilitystatistics.service;

import org.ignas.frauddetection.probabilitystatistics.domain.BatchToProcess;
import org.ignas.frauddetection.probabilitystatistics.domain.CriteriaUpdate;
import org.ignas.frauddetection.probabilitystatistics.domain.CriteriaUpdateIncrement;
import org.ignas.frauddetection.transactionevaluation.api.request.LearningRequest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class BatchedCriteriaProcessor {

    public static List<CriteriaUpdate> parseCriteriaUpdates(BatchToProcess batch) {
        return parseBatch(batch, request -> request.getGroupedCriteriaValues()
            .values()
            .stream()
            .collect(HashMap::new, HashMap::putAll, HashMap::putAll)
        );
    }

    public static List<CriteriaUpdate> parseCriteriaGroupUpdates(BatchToProcess batch) {
        return parseBatch(batch, LearningRequest::getCriteriaGroupValues);

    }

    private static List<CriteriaUpdate> parseBatch(
        BatchToProcess batch,
        Function<LearningRequest, Map<String, String>> requestValueExtractor) {

        Map<String, CriteriaUpdate> updates = new HashMap<>();

        batch.getItems()
            .stream()
            .flatMap(request -> extractIncrements(
                requestValueExtractor.apply(request),
                request.isFraudulent(),
                request.isAlreadyProcessedTransaction())
            )
            .forEach(increment -> {
                CriteriaUpdate update = updates.get(increment.pseudoUniqueCode());

                if (update == null) {
                    update = CriteriaUpdate.empty(increment.getCriteria(), increment.getValue());
                    updates.put(increment.pseudoUniqueCode(), update);
                }

                // Mutable on purpose to reduce overhead
                update.apply(increment);
            });

        return updates.values()
            .stream()
            .filter(CriteriaUpdate::causesSideEffects)
            .collect(Collectors.toList());
    }

    private static Stream<CriteriaUpdateIncrement> extractIncrements(
        Map<String, String> values,
        boolean fraudulent,
        boolean alreadyProcessedTransaction) {

        return values.entrySet()
            .stream()
            .map(criteria ->
                new CriteriaUpdateIncrement(
                    criteria.getKey(),
                    criteria.getValue(),
                    fraudulent,
                    alreadyProcessedTransaction
                )
            );
    }
}
