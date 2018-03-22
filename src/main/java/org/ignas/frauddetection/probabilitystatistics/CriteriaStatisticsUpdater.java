package org.ignas.frauddetection.probabilitystatistics;


import com.google.common.collect.ImmutableMap;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.eventbus.EventBus;
import org.ignas.frauddetection.probabilitystatistics.domain.BatchToProcess;
import org.ignas.frauddetection.probabilitystatistics.domain.CombinationStatistics;
import org.ignas.frauddetection.probabilitystatistics.domain.GeneralOccurences;
import org.ignas.frauddetection.probabilitystatistics.domain.GroupTotalStats;
import org.ignas.frauddetection.probabilitystatistics.service.*;

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
        .put("AMOUNT", 3125000)
        .put("COUNT", 15625)
        .put("TIME", 250)
        .put("LOCATION", 625)
        .build();

    private GroupStatisticsStorage storage;
    private GeneralProbabilitiesStorage generalProbabilities;

    @Override
    public void start(Future<Void> startFuture) {
        storage = new GroupStatisticsStorage(
            "mongodb://localhost", "bayes", "groupStatistics");

        generalProbabilities = new GeneralProbabilitiesStorage(
            "mongodb://localhost", "bayes", "generalProbabilities");

        Future<Void> initResult = storage.initGroupsIfNotPresent();

        initResult.setHandler(result -> {
            if (result.failed()) {
                System.out.println("Failed to start vertice: CriteriaStatisticsUpdater\n" + result.cause().getMessage());
                return;
            }

            EventBus bus = vertx.eventBus();

            bus.consumer("probability-processing.batch-prepared", (batchEvent) -> {
                if (!(batchEvent.body() instanceof BatchToProcess)) {
                    throw new IllegalArgumentException("Invalid message type: " + batchEvent.body().getClass());
                }

                Future<GeneralOccurences> statsLoader = generalProbabilities.fetch();

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

                Future<Void> initLoader = storage.initCombinationsIfNotPresent(groupCombinations);

                initLoader.setHandler(initFinished -> {
                    List<CombinationStatistics> beforeUpdateStatistics = new ArrayList<>();
                    Future statisticsLoaded = Future.future();

                    groupCombinations.stream()
                        .distinct()
                        .map(storage::fetch)
                        .forEach(future ->
                            future.setHandler(statsLoaded -> {
                                if (result.failed()) {
                                    throw new IllegalStateException();
                                }

                                beforeUpdateStatistics.add(statsLoaded.result());

                                if (beforeUpdateStatistics.size() == groupCombinations.size()) {
                                    statisticsLoaded.complete();
                                }
                            }));

                    statisticsLoaded.setHandler(loaded -> {
                        storage.updateOccurences(groupCombinations);

                        statsLoader.setHandler(totalStatsLoaded -> {
                            if (totalStatsLoaded.failed()) {
                                throw new IllegalStateException("Failed to load stats");
                            }

                            GeneralOccurences occurences = totalStatsLoaded.result();

                            Future<Map<String, GroupTotalStats>> totalStatsLoader = storage.loadTotals();

                            totalStatsLoader.setHandler(groupStatsLoaded -> {
                                Map<String, GroupTotalStats> groupStats = groupStatsLoaded.result();

                                Map<String, List<CombinationStatistics>> increments = groupCombinations.stream()
                                    .collect(Collectors.groupingBy(CombinationStatistics::getGroup));

                                for (Map.Entry<String, GroupTotalStats> groupEntry : groupStats.entrySet()) {
                                   long additionalOccurrences = increments.get(groupEntry.getKey())
                                       .stream()
                                       .mapToLong(CombinationStatistics::getFraudOccurences)
                                       .sum();

                                   List<CombinationStatistics> oldGroupStats = beforeUpdateStatistics.stream()
                                       .filter(item -> item.getGroup().equals(groupEntry.getKey()))
                                       .collect(Collectors.toList());

                                   groupEntry.getValue()
                                       .updateStatistics(
                                           additionalOccurrences,
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
            startFuture.complete();
        });
    }

    @Override
    public void stop() {
        storage.close();
    }
}
