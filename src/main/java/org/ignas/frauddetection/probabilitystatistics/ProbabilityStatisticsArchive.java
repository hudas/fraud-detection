package org.ignas.frauddetection.probabilitystatistics;


import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import org.ignas.frauddetection.probabilitystatistics.api.request.CriteriaGroupProbabilityRequest;
import org.ignas.frauddetection.probabilitystatistics.api.request.CriteriaProbabilityRequest;
import org.ignas.frauddetection.probabilitystatistics.api.response.BayesTable;
import org.ignas.frauddetection.probabilitystatistics.api.response.CriteriaGroupRisk;
import org.ignas.frauddetection.probabilitystatistics.api.response.ProbabilityStatistics;
import org.ignas.frauddetection.probabilitystatistics.domain.CriteriaStatistics;
import org.ignas.frauddetection.probabilitystatistics.domain.GeneralOccurrences;
import org.ignas.frauddetection.probabilitystatistics.domain.GroupTotalStats;
import org.ignas.frauddetection.probabilitystatistics.service.repositories.CriteriaStorage;
import org.ignas.frauddetection.probabilitystatistics.service.repositories.GeneralProbabilitiesStorage;
import org.ignas.frauddetection.probabilitystatistics.service.repositories.GroupStatisticsStorage;
import org.ignas.frauddetection.shared.ImmutableObjectCodec;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProbabilityStatisticsArchive extends AbstractVerticle {

    private static final List<String> VALUE_CODES =
        ImmutableList.of("VERY_LOW_RISK", "LOW_RISK", "EXPECTED_RISK", "HIGH_RISK", "VERY_HIGH_RISK");

    private GroupStatisticsStorage groupStatisticsStorage;
    private GeneralProbabilitiesStorage generalProbabilitiesStorage;
    private CriteriaStorage criteriaStorage;
    private CriteriaStorage groupStorage;

    public ProbabilityStatisticsArchive() {
        groupStatisticsStorage = new GroupStatisticsStorage(
            "mongodb://localhost", "bayes", "groupStatistics");

        generalProbabilitiesStorage = new GeneralProbabilitiesStorage(
            "mongodb://localhost", "bayes", "generalProbabilities");

        criteriaStorage = new CriteriaStorage(
            "mongodb://localhost", "bayes", "criteriaProbabilities");

        groupStorage = new CriteriaStorage(
            "mongodb://localhost", "bayes", "criteriaGroupProbabilities");
    }

    @Override
    public void start(Future<Void> startup) {
        registerAPICodecs();

        vertx.eventBus()
            .consumer("probability-statistic.archive.criteria")
            .handler(message -> {
                if (!(message.body() instanceof CriteriaProbabilityRequest)) {
                    message.fail(400,"Unsupported message type: " + message.body().getClass());
                    return;
                }

                CriteriaProbabilityRequest request = (CriteriaProbabilityRequest) message.body();


                Future<GeneralOccurrences> generalLoader = generalProbabilitiesStorage.fetch();
                Future<List<CriteriaStatistics>> criteriaLoader = criteriaStorage.fetchStatistics(request.getCriteriaValues());
                Future<Map<String, GroupTotalStats>> groupLoader = groupStatisticsStorage.fetchTotalStats();

                CompositeFuture.all(generalLoader, criteriaLoader, groupLoader)
                    .setHandler(loaded -> {
                        if (loaded.failed()) {
                            message.fail(500,"Unsupported message type: " + message.body().getClass());
                            return;
                        }

                        GeneralOccurrences general = loaded.result().resultAt(0);
                        List<CriteriaStatistics> criteria = loaded.result().resultAt(1);
                        Map<String, GroupTotalStats> group = loaded.result().resultAt(2);

                        float fraudProbability = general.getFraudProbability();

                        Map<String, Float> criteriaProbabilities =
                            collectCriteriaProbabilities(criteria, general.getTotalFraudTransactions());

                        fillWithDefaults(criteriaProbabilities, request.getCriteriaValues());

                        Map<String, CriteriaGroupRisk> groupRisks = group.entrySet()
                            .stream()
                            .collect(
                                HashMap::new,
                                (map, entry) -> {
                                    GroupTotalStats totalStats = entry.getValue();

                                    CriteriaGroupRisk risk = new CriteriaGroupRisk(
                                        totalStats.getAverageProbability(),
                                        totalStats.getDeviationProbability()
                                    );

                                    map.put(entry.getKey(), risk);
                                },
                                HashMap::putAll
                            );

                        message.reply(
                            new ProbabilityStatistics(
                                fraudProbability,
                                criteriaProbabilities,
                                groupRisks
                        ));
                    });
            });

        vertx.eventBus()
            .consumer("probability-statistic.archive.criteria-group")
            .handler(message -> {
                if (!(message.body() instanceof CriteriaGroupProbabilityRequest)) {
                    message.fail(101, "Wrong type");
                    return;
                }

                CriteriaGroupProbabilityRequest probabilityRequest = (CriteriaGroupProbabilityRequest) message.body();

                Future<GeneralOccurrences> generalLoader = generalProbabilitiesStorage.fetch();
                Future<List<CriteriaStatistics>> groupStatsLoader =
                    groupStorage.fetchValues(probabilityRequest.getGroups());

                CompositeFuture.all(generalLoader, groupStatsLoader)
                    .setHandler(loaded -> {
                        if (loaded.failed()) {
                            message.fail(500, loaded.cause().getMessage());
                            return;
                        }

                        GeneralOccurrences general = loaded.result().resultAt(0);
                        List<CriteriaStatistics> stats = loaded.result().resultAt(1);

                        Map<String, Map<String, Float>> tableValues = new HashMap<>();
                        stats.forEach(criterion -> {
                            Map<String, Float> criteriaValues = tableValues.computeIfAbsent(
                                criterion.getName(), k -> new HashMap<>());

                            float occurrenceInFraudProbability =
                                ((float) criterion.getFraudOccurrences()) / general.getTotalTransactions();

                            criteriaValues.put(criterion.getValue(), occurrenceInFraudProbability);
                        });

                        fillEmptyWithDefaults(tableValues, probabilityRequest.getGroups());

                        message.reply(new BayesTable(tableValues));
                    });
            });

        startup.complete();
    }

    private void fillWithDefaults(Map<String, Float> criteriaProbabilities, Map<String, String> requestedValues) {
        requestedValues.keySet().forEach((key) -> {
            Float result = criteriaProbabilities.get(key);

            if (result == null) {
                criteriaProbabilities.put(key, 0f);
            }
        });
    }

    private void fillEmptyWithDefaults(Map<String, Map<String, Float>> tableValues, List<String> requestedGroups) {
        requestedGroups.forEach(group ->
            VALUE_CODES.forEach(value -> {
                Map<String, Float> loadedCriteriaValues = tableValues.get(group);

                Float loadedValue = loadedCriteriaValues.get(value);

                if (loadedValue == null) {
                    loadedCriteriaValues.put(value, 0f);
                }
            })
        );
    }

    private Map<String, Float> collectCriteriaProbabilities(
        List<CriteriaStatistics> criteria,
        long totalFraudTransactions) {

        ImmutableMap.Builder builder = ImmutableMap.<String, Float>builder();

        criteria.forEach(stats -> {
            float probabilityOfCriteriaValueInFraud =
                ((float) stats.getFraudOccurrences()) / totalFraudTransactions;

            builder.put(stats.getName(), probabilityOfCriteriaValueInFraud);
        });

        return (Map<String, Float>) builder.build();
    }

    private void registerAPICodecs() {
        vertx.eventBus().registerDefaultCodec(
            BayesTable.class,
            new ImmutableObjectCodec<BayesTable>(BayesTable.class)
        );

        vertx.eventBus().registerDefaultCodec(
            ProbabilityStatistics.class,
            new ImmutableObjectCodec<ProbabilityStatistics>(ProbabilityStatistics.class)
        );
    }
}
