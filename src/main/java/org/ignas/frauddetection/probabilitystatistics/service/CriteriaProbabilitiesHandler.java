package org.ignas.frauddetection.probabilitystatistics.service;

import com.google.common.collect.ImmutableMap;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.Message;
import org.ignas.frauddetection.probabilitystatistics.api.request.CriteriaProbabilityRequest;
import org.ignas.frauddetection.probabilitystatistics.api.response.CriteriaGroupRisk;
import org.ignas.frauddetection.probabilitystatistics.api.response.ProbabilityStatistics;
import org.ignas.frauddetection.probabilitystatistics.domain.CriteriaStatistics;
import org.ignas.frauddetection.probabilitystatistics.domain.GeneralOccurrences;
import org.ignas.frauddetection.probabilitystatistics.domain.GroupTotalStats;
import org.ignas.frauddetection.probabilitystatistics.service.repositories.CriteriaStorage;
import org.ignas.frauddetection.probabilitystatistics.service.repositories.GeneralProbabilitiesStorage;
import org.ignas.frauddetection.probabilitystatistics.service.repositories.GroupStatisticsStorage;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CriteriaProbabilitiesHandler implements Handler<Message<Object>> {

    private GroupStatisticsStorage groupStatisticsStorage;
    private GeneralProbabilitiesStorage generalProbabilitiesStorage;
    private CriteriaStorage criteriaStorage;

    public CriteriaProbabilitiesHandler(
        CriteriaStorage criteriaStorage,
        GeneralProbabilitiesStorage generalProbabilitiesStorage,
        GroupStatisticsStorage groupStatisticsStorage) {

        this.criteriaStorage = criteriaStorage;
        this.generalProbabilitiesStorage = generalProbabilitiesStorage;
        this.groupStatisticsStorage = groupStatisticsStorage;
    }

    @Override
    public void handle(Message<Object> message) {
        if (!(message.body() instanceof CriteriaProbabilityRequest)) {
            message.fail(400, "Unsupported message type: " + message.body().getClass());
            return;
        }

        CriteriaProbabilityRequest request = (CriteriaProbabilityRequest) message.body();

        Future<GeneralOccurrences> generalLoader = generalProbabilitiesStorage.fetch();
        Future<List<CriteriaStatistics>> criteriaLoader = criteriaStorage.fetchStatistics(request.getCriteriaValues());
        Future<Map<String, GroupTotalStats>> groupLoader = groupStatisticsStorage.fetchTotalStats(request.getTransactionId());



        CompositeFuture.all(generalLoader, criteriaLoader, groupLoader)
            .setHandler(loaded -> {
                if (loaded.failed()) {
                    loaded.cause().printStackTrace();

//                    System.out.println("CriteriaProbabilitiesHandler:    Failed: " + request.getTransactionId());
                    message.fail(500, loaded.cause().getMessage());
                    return;
                }

                GeneralOccurrences general = loaded.result().resultAt(0);
                List<CriteriaStatistics> criteria = loaded.result().resultAt(1);
                Map<String, GroupTotalStats> group = loaded.result().resultAt(2);

                ProbabilityStatistics result = buildProbabilityStatistics(request.getCriteriaValues(), general, criteria, group);

                message.reply(result);
            });
    }

    private ProbabilityStatistics buildProbabilityStatistics(
        Map<String, String> criteriaValues,
        GeneralOccurrences general,
        List<CriteriaStatistics> criteria,
        Map<String, GroupTotalStats> group) {

        float fraudProbability = general.getFraudProbability();

        Map<String, Float> criteriaProbabilities =
            collectCriteriaProbabilities(criteria, general.getTotalFraudTransactions());

        Map<String, Float> criteriaNonFraudProbabilities =
            collectCriteriaNonFraudProbabilities(criteria, general.getTotalNonFraudTransactions());

        fillWithDefaults(criteriaProbabilities, criteriaValues);

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

        return new ProbabilityStatistics(
            fraudProbability,
            criteriaProbabilities,
            criteriaNonFraudProbabilities,
            groupRisks
        );
    }


    private Map<String, Float> collectCriteriaProbabilities(
        List<CriteriaStatistics> criteria,
        long totalFraudTransactions) {

        ImmutableMap.Builder<String, Float> builder = ImmutableMap.<String, Float>builder();

        criteria.forEach(stats -> {

            final float probabilityOfCriteriaValueInFraud;

            if (totalFraudTransactions == 0) {
                probabilityOfCriteriaValueInFraud = 0;
            } else {
                probabilityOfCriteriaValueInFraud = ((float) stats.getFraudOccurrences()) / totalFraudTransactions;
            }

            builder.put(stats.getName(), probabilityOfCriteriaValueInFraud);
        });

        return builder.build();
    }

    private Map<String, Float> collectCriteriaNonFraudProbabilities(
        List<CriteriaStatistics> criteria,
        long totalNonFraudTransactions) {

        ImmutableMap.Builder<String, Float> builder = ImmutableMap.<String, Float>builder();

        criteria.forEach(stats -> {

            final float probabilityOfCriteriaValueInNonFraud;

            if (totalNonFraudTransactions == 0) {
                probabilityOfCriteriaValueInNonFraud = 0;
            } else {
                probabilityOfCriteriaValueInNonFraud = ((float) stats.getOccurrences()) / totalNonFraudTransactions;
            }

            builder.put(stats.getName(), probabilityOfCriteriaValueInNonFraud);
        });

        return builder.build();
    }

    private void fillWithDefaults(Map<String, Float> criteriaProbabilities, Map<String, String> requestedValues) {
        requestedValues.keySet().forEach((key) -> {
            Float result = criteriaProbabilities.get(key);

            if (result == null) {
                criteriaProbabilities.put(key, 0f);
            }
        });
    }
}
