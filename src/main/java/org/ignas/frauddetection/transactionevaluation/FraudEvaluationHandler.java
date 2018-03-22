package org.ignas.frauddetection.transactionevaluation;

import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import org.apache.http.HttpStatus;
import org.ignas.frauddetection.probabilitystatistics.api.response.ProbabilityStatistics;
import org.ignas.frauddetection.shared.OneWayServiceIntegration;
import org.ignas.frauddetection.shared.ServiceIntegration;
import org.ignas.frauddetection.transactionevaluation.api.request.LearningRequest;
import org.ignas.frauddetection.transactionevaluation.api.request.TransactionData;
import org.ignas.frauddetection.transactionevaluation.cache.GroupProbabilityCache;
import org.ignas.frauddetection.transactionevaluation.domain.Risk;
import org.ignas.frauddetection.transactionevaluation.domain.Transaction;
import org.ignas.frauddetection.transactionevaluation.domain.config.FraudCriteriaEvaluator;
import org.ignas.frauddetection.transactionevaluation.domain.stats.HistoricalData;
import org.ignas.frauddetection.transactionevaluation.service.GroupRiskEvaluator;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class FraudEvaluationHandler implements Handler<Message<Object>> {

    private GroupProbabilityCache cache;
    private ServiceIntegration<Transaction, HistoricalData> transactionStatisticsIntegration;
    private ServiceIntegration<Map<String, String>, ProbabilityStatistics> probabilityStatisticsIntegration;
    private OneWayServiceIntegration<LearningRequest> learningInitiator;

    private static FraudCriteriaEvaluator criteriaEvaluator = new FraudCriteriaEvaluator();
    private static GroupRiskEvaluator evaluator = new GroupRiskEvaluator(criteriaEvaluator);

    public FraudEvaluationHandler(
        GroupProbabilityCache cache,
        ServiceIntegration<Transaction, HistoricalData> transactionStatisticsIntegration,
        ServiceIntegration<Map<String, String>, ProbabilityStatistics> probabilityStatisticsIntegration,
        OneWayServiceIntegration<LearningRequest> learningInitiator) {

        this.cache = cache;
        this.transactionStatisticsIntegration = transactionStatisticsIntegration;
        this.probabilityStatisticsIntegration = probabilityStatisticsIntegration;
        this.learningInitiator = learningInitiator;
    }

    @Override
    public void handle(Message<Object> event) {
        if (!(event.body() instanceof TransactionData)) {
            event.fail(HttpStatus.SC_BAD_REQUEST, "Unsupported message type: " + event.body().getClass());
            return;
        }

        TransactionData dataDTO = (TransactionData) event.body();
        Transaction transactionData = mapToDomain(dataDTO);

        Future<HistoricalData> transactionStatistics = transactionStatisticsIntegration.load(transactionData);

        transactionStatistics.setHandler(data -> {
            HistoricalData transactionDataHistory = data.result();

            Map<String, String> criteriaValues = criteriaEvaluator.evaluateAll(transactionData, transactionDataHistory);

            Future<ProbabilityStatistics> probabilityStatistics = probabilityStatisticsIntegration.load(criteriaValues);

            probabilityStatistics.setHandler(
                probabilityLoader -> {

                    ProbabilityStatistics statistics = probabilityLoader.result();

                    Map<String, Risk.Value> groupValues = evaluator.evaluate(statistics);

                    Float fraudProbability = groupValues.entrySet()
                        .stream()
                        .map(entry -> cache.getProbability(entry.getKey(), entry.getValue().name()))
                        .reduce(statistics.getFraudProbability(), (result, increment) -> result * increment);

                    event.reply(fraudProbability);

                    Map<String, String> groupValueRepresentations = groupValues.entrySet()
                        .stream()
                        .collect(Collectors.toMap(Map.Entry::getKey, value -> value.getValue().name()));

                    Map<String, Map<String, String>> groupedCriteriaValues = new HashMap<>();

                    for (Map.Entry<String, String> entries : criteriaValues.entrySet()) {
                        String group = criteriaEvaluator.resolveGroup(entries.getKey());

                        Map<String, String> values = groupedCriteriaValues.get(group);
                        if (values == null) {
                            values = new HashMap<>();
                            groupedCriteriaValues.put(group, values);
                        }

                        values.put(entries.getKey(), entries.getValue());
                    }

                    learningInitiator.publish(
                        new LearningRequest(
                        false,
                            dataDTO,
                            groupedCriteriaValues,
                            groupValueRepresentations
                        )
                    );
                });
        });
    }


    private Transaction mapToDomain(TransactionData request) {
        return new Transaction(
            request.getTransactionId(),
            request.getAmount().floatValue(),
            request.getDebtorId(),
            request.getCreditorId(),
            request.getLocation(),
            request.getTime()
        );
    }
}
