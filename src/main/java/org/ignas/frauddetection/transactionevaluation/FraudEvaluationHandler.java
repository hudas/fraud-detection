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
import org.ignas.frauddetection.transactionevaluation.service.LearningEventPublisher;

import java.util.Map;

public class FraudEvaluationHandler implements Handler<Message<Object>> {

    private GroupProbabilityCache cache;
    private ServiceIntegration<Transaction, HistoricalData> transactionStatisticsIntegration;
    private ServiceIntegration<Map<String, String>, ProbabilityStatistics> probabilityStatisticsIntegration;

    private static FraudCriteriaEvaluator criteriaEvaluator = new FraudCriteriaEvaluator();
    private static GroupRiskEvaluator evaluator = new GroupRiskEvaluator(criteriaEvaluator);
    private LearningEventPublisher learningInitiator;

    public FraudEvaluationHandler(
        GroupProbabilityCache cache,
        ServiceIntegration<Transaction, HistoricalData> transactionStatisticsIntegration,
        ServiceIntegration<Map<String, String>, ProbabilityStatistics> probabilityStatisticsIntegration,
        OneWayServiceIntegration<LearningRequest> learningInitiationIntegration) {

        this.cache = cache;
        this.transactionStatisticsIntegration = transactionStatisticsIntegration;
        this.probabilityStatisticsIntegration = probabilityStatisticsIntegration;
        this.learningInitiator = new LearningEventPublisher(criteriaEvaluator, learningInitiationIntegration);
    }

    @Override
    public void handle(Message<Object> event) {
        if (!(event.body() instanceof TransactionData)) {
            event.fail(HttpStatus.SC_BAD_REQUEST, "Unsupported message type: " + event.body().getClass());
            return;
        }

        TransactionData requestData = (TransactionData) event.body();

        Transaction transactionData = mapToDomain(requestData);

        transactionStatisticsIntegration.load(transactionData)
            .setHandler(historyLoaded -> {
                if (historyLoaded.failed()) {
                    System.out.println("Failed to load transaction history: " + historyLoaded.cause().getMessage());
                    throw new IllegalStateException(historyLoaded.cause());
                }

                Map<String, String> criteriaValues =
                    criteriaEvaluator.evaluateAll(transactionData, historyLoaded.result());

                Future<ProbabilityStatistics> probabilityStatistics = probabilityStatisticsIntegration.load(criteriaValues);

                probabilityStatistics.setHandler(probabilitiesLoaded -> {
                    if (probabilitiesLoaded.failed()) {
                        System.out.println("Failed to load probabilities: " + probabilitiesLoaded.cause().getMessage());
                        throw new IllegalStateException(probabilitiesLoaded.cause());
                    }

                    ProbabilityStatistics statistics = probabilitiesLoaded.result();

                    Map<String, Risk.Value> groupValues = evaluator.evaluate(statistics);

                    Float fraudProbability = calculateFraudProbability(statistics.getFraudProbability(), groupValues);

                    event.reply(fraudProbability);

                    learningInitiator.publishData(requestData, criteriaValues, groupValues);
                });
            });
    }



    private Float calculateFraudProbability(Float fraudProbability, Map<String, Risk.Value> groupValues) {
        return groupValues.entrySet()
            .stream()
            .map(entry -> cache.getProbability(entry.getKey(), entry.getValue().name()))
            .reduce(fraudProbability, (result, increment) -> result * increment);
    }


    private Transaction mapToDomain(TransactionData request) {
        return new Transaction(
            request.getTransactionId(),
            request.getAmount(),
            request.getDebtorId(),
            request.getCreditorId(),
            request.getLocation(),
            request.getTime()
        );
    }
}
