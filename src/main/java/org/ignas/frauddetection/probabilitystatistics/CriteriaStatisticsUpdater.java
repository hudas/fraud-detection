package org.ignas.frauddetection.probabilitystatistics;


import com.google.common.collect.ImmutableMap;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.eventbus.EventBus;
import org.ignas.frauddetection.DetectionLauncher;
import org.ignas.frauddetection.probabilitystatistics.domain.BatchToProcess;
import org.ignas.frauddetection.probabilitystatistics.domain.CombinationStatistics;
import org.ignas.frauddetection.probabilitystatistics.domain.GeneralOccurrences;
import org.ignas.frauddetection.probabilitystatistics.domain.GroupTotalStats;
import org.ignas.frauddetection.probabilitystatistics.service.*;
import org.ignas.frauddetection.probabilitystatistics.service.repositories.GeneralProbabilitiesStorage;
import org.ignas.frauddetection.probabilitystatistics.service.repositories.GroupStatisticsStorage;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


public class CriteriaStatisticsUpdater extends AbstractVerticle {

    /**
     * Currently hard-coded, should be refactored to be provided.
     *
     * Contains amount of possible different combinations from criteria values of group.
     *
     */
    private static final Map<String, Integer> POSSIBLE_COMBINATION_COUNTS = ImmutableMap.<String, Integer>builder()
        .put("AMOUNT", 125000)
        .put("COUNT", 125)
        .put("TIME", 50)
        .put("LOCATION", 625)
        .build();

    private GroupStatisticsStorage storage;
    private GeneralProbabilitiesStorage generalProbabilities;

    @Override
    public void start(Future<Void> startFuture) {
        storage = new GroupStatisticsStorage("bayes", "groupStatistics");

        generalProbabilities = new GeneralProbabilitiesStorage("bayes", "generalProbabilities");

        storage.initTotalsIfNotPresent()
            .setHandler(result -> {
                if (result.failed()) {
                    System.out.println("Failed to start vertice: CriteriaStatisticsUpdater\n" + result.cause().getMessage());
                    return;
                }

                EventBus bus = vertx.eventBus();

                bus.consumer("probability-processing.criteria-data-updated", (batchEvent) -> {
                    if (!(batchEvent.body() instanceof BatchToProcess)) {
                        throw new IllegalArgumentException("Invalid message type: " + batchEvent.body().getClass());
                    }

                    BatchToProcess batch = (BatchToProcess) batchEvent.body();

                    List<CombinationStatistics> groupCombinations = batch.getItems()
                        .stream()
                        .flatMap(request ->
                            request.getGroupedCriteriaValues()
                                .entrySet()
                                .stream()
                                .map(groupCriterias ->
                                    new CombinationStatistics(
                                        groupCriterias.getKey(),
                                        CriteriaValuesEncoder.encode(groupCriterias.getValue()),
                                        !request.isAlreadyProcessedTransaction() ? 1 : 0,
                                        request.isFraudulent() ? 1 : 0
                                    )
                                )
                        )
                        .collect(Collectors.toList());

                    List<CombinationStatistics> uniqueCombinations = groupCombinations
                        .stream()
                        .distinct()
                        .collect(Collectors.toList());

                    generalProbabilities.fetch()
                        .setHandler((totalStatsLoaded) -> {
                            if (totalStatsLoaded.failed()) {
                                totalStatsLoaded.cause().printStackTrace();
                                throw new IllegalStateException("Failed to load stats", totalStatsLoaded.cause());
                            }

                            GeneralOccurrences occurences = totalStatsLoaded.result();

                            storage.initCombinationsIfNotPresent(groupCombinations)
                                .setHandler(initFinished -> {
                                    if (initFinished.failed()) {
                                        initFinished.cause().printStackTrace();
                                        return;
                                    }

                                    storage.fetchCombination(uniqueCombinations)
                                        .setHandler(loaded -> {
                                            if (loaded.failed()) {
                                                loaded.cause().printStackTrace();
                                                return;
                                            }

                                            List<CombinationStatistics> beforeUpdateStatistics = loaded.result();

                                            storage.updateOccurences(groupCombinations);
                                            storage.fetchTotalStats("A").setHandler(groupStatsLoaded -> {
                                                if (groupStatsLoaded.failed()) {
                                                    groupStatsLoaded.cause().printStackTrace();
                                                    throw new IllegalStateException(groupStatsLoaded.cause().getMessage());
                                                }

                                                Map<String, GroupTotalStats> groupStats = groupStatsLoaded.result();

                                                Map<String, List<CombinationStatistics>> increments = groupCombinations.stream()
                                                    .collect(Collectors.groupingBy(CombinationStatistics::getGroup));

                                                for (Map.Entry<String, GroupTotalStats> groupEntry : groupStats.entrySet()) {
                                                    List<CombinationStatistics> oldGroupStats = beforeUpdateStatistics.stream()
                                                        .filter(item -> item.getGroup().equals(groupEntry.getKey()))
                                                        .collect(Collectors.toList());

                                                    groupEntry.getValue()
                                                        .updateStatistics(
                                                            occurences.getTotalFraudTransactions(),
                                                            POSSIBLE_COMBINATION_COUNTS.get(groupEntry.getKey()),
                                                            increments.get(groupEntry.getKey()),
                                                            oldGroupStats
                                                        );
                                                }

                                                storage.updateTotals(groupStats);
                                            });
                                        });
                                    });
                                });
                        });
                });

        startFuture.complete();
    }

    @Override
    public void stop() {
        storage.close();
        generalProbabilities.close();
    }
}
