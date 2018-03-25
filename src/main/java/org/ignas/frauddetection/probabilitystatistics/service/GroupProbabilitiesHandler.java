package org.ignas.frauddetection.probabilitystatistics.service;

import com.google.common.collect.ImmutableList;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import org.ignas.frauddetection.probabilitystatistics.api.request.CriteriaGroupProbabilityRequest;
import org.ignas.frauddetection.probabilitystatistics.api.response.BayesTable;
import org.ignas.frauddetection.probabilitystatistics.domain.CriteriaStatistics;
import org.ignas.frauddetection.probabilitystatistics.domain.GeneralOccurrences;
import org.ignas.frauddetection.probabilitystatistics.service.repositories.CriteriaStorage;
import org.ignas.frauddetection.probabilitystatistics.service.repositories.GeneralProbabilitiesStorage;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GroupProbabilitiesHandler implements Handler<Message<Object>> {

    private static final List<String> VALUE_CODES =
        ImmutableList.of("VERY_LOW_RISK", "LOW_RISK", "EXPECTED_RISK", "HIGH_RISK", "VERY_HIGH_RISK");

    private CriteriaStorage groupStorage;
    private GeneralProbabilitiesStorage generalProbabilitiesStorage;

    public GroupProbabilitiesHandler(CriteriaStorage groupStorage, GeneralProbabilitiesStorage generalProbabilitiesStorage) {
        this.groupStorage = groupStorage;
        this.generalProbabilitiesStorage = generalProbabilitiesStorage;
    }

    @Override
    public void handle(Message<Object> message) {
        if (!(message.body() instanceof CriteriaGroupProbabilityRequest)) {
            message.fail(400, "Unsupported request type");
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

                BayesTable resultTable = buildBayesTable(general, stats, probabilityRequest.getGroups());

                message.reply(resultTable);
            });
    }

    private BayesTable buildBayesTable(
        GeneralOccurrences general,
        List<CriteriaStatistics> stats,
        List<String> requestedGroups) {

        Map<String, Map<String, Float>> tableValues = new HashMap<>();

        stats.forEach(criterion -> {
            Map<String, Float> criteriaValues = tableValues.computeIfAbsent(
                criterion.getName(), k -> new HashMap<>());

            float occurrenceInFraudProbability =
                ((float) criterion.getFraudOccurrences()) / general.getTotalTransactions();

            criteriaValues.put(criterion.getValue(), occurrenceInFraudProbability);
        });

        fillEmptyWithDefaults(tableValues, requestedGroups);

        return new BayesTable(tableValues);
    }

    private void fillEmptyWithDefaults(Map<String, Map<String, Float>> tableValues, List<String> requestedGroups) {
        requestedGroups.forEach(group ->
            VALUE_CODES.forEach(value -> {
                tableValues.computeIfAbsent(group, key -> new HashMap<String, Float>())
                    .putIfAbsent(value, 0f);
            })
        );
    }
}
