package org.ignas.frauddetection.probabilitystatistics.service;

import org.ignas.frauddetection.probabilitystatistics.domain.BatchToProcess;
import org.ignas.frauddetection.probabilitystatistics.domain.CriteriaUpdate;
import org.ignas.frauddetection.probabilitystatistics.domain.CriteriaUpdateIncrement;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class BatchProcessor {

    public static List<CriteriaUpdate> parseRequiredUpdates(BatchToProcess batch) {
        Map<String, CriteriaUpdate> updates = new HashMap<>();

        batch.getItems()
            .stream()
            .flatMap(request -> request.getCriteriaValues()
                .entrySet()
                .stream()
                .map(criteria ->
                    new CriteriaUpdateIncrement(
                        criteria.getKey(),
                        criteria.getValue(),
                        request.isFraudulent(),
                        request.isAlreadyProcessedTransaction()
                    )
                )
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
}
