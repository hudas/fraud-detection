package org.ignas.frauddetection.transactionevaluation;

import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import org.apache.http.HttpStatus;
import org.ignas.frauddetection.probabilitystatistics.api.response.ProbabilityStatistics;
import org.ignas.frauddetection.shared.OneWayServiceIntegration;
import org.ignas.frauddetection.shared.ServiceIntegration;
import org.ignas.frauddetection.transactionevaluation.api.request.BehaviourData;
import org.ignas.frauddetection.transactionevaluation.api.request.LearningRequest;
import org.ignas.frauddetection.transactionevaluation.api.request.TransactionData;
import org.ignas.frauddetection.transactionevaluation.cache.GroupProbabilityCache;
import org.ignas.frauddetection.transactionevaluation.domain.Risk;
import org.ignas.frauddetection.transactionevaluation.domain.Transaction;
import org.ignas.frauddetection.transactionevaluation.domain.calculation.EvaluationResult;
import org.ignas.frauddetection.transactionevaluation.domain.config.FraudCriteriaEvaluator;
import org.ignas.frauddetection.transactionevaluation.domain.stats.HistoricalData;
import org.ignas.frauddetection.transactionevaluation.service.GroupRiskEvaluator;
import org.ignas.frauddetection.transactionevaluation.service.LearningEventPublisher;

import java.util.HashMap;
import java.util.Map;

public class FraudEvaluationHandler implements Handler<Message<Object>> {

    private GroupProbabilityCache cache;
    private ServiceIntegration<Transaction, HistoricalData> transactionStatisticsIntegration;
    private ServiceIntegration<Map<String, String>, ProbabilityStatistics> criteriaProbabilityIntegration;

    private static FraudCriteriaEvaluator criteriaEvaluator = new FraudCriteriaEvaluator();
    private static GroupRiskEvaluator evaluator = new GroupRiskEvaluator(criteriaEvaluator);
    private LearningEventPublisher learningInitiator;

    public FraudEvaluationHandler(
        GroupProbabilityCache cache,
        ServiceIntegration<Transaction, HistoricalData> transactionStatisticsIntegration,
        ServiceIntegration<Map<String, String>, ProbabilityStatistics> criteriaProbabilityIntegration,
        OneWayServiceIntegration<LearningRequest> learningInitiationIntegration) {

        this.cache = cache;
        this.transactionStatisticsIntegration = transactionStatisticsIntegration;
        this.criteriaProbabilityIntegration = criteriaProbabilityIntegration;
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
                    historyLoaded.cause().printStackTrace();
                    throw new IllegalStateException(historyLoaded.cause());
                }

                Map<String, EvaluationResult> evaluationResult =
                    criteriaEvaluator.evaluateAll(transactionData, historyLoaded.result());

                Map<String, String> criteriaValues = new HashMap<>();
                evaluationResult.forEach((key, value) -> criteriaValues.put(key, value.getResult().representation()));

                EvaluationResult dailyRatioResult = evaluationResult.get("AVERAGE_PERIOD_AMOUNT_RATIO/P1D");
                EvaluationResult weeklyRatioResult = evaluationResult.get("AVERAGE_PERIOD_AMOUNT_RATIO/P7D");
                EvaluationResult monthlyRatioResult = evaluationResult.get("AVERAGE_PERIOD_AMOUNT_RATIO/P30D");

                EvaluationResult distanceFromCommon = evaluationResult.get("AVERAGE_DISTANCE_FROM_COMMON_LOCATION");
                EvaluationResult distanceFromLast = evaluationResult.get("AVERAGE_DISTANCE_FROM_LAST_LOCATION");
                EvaluationResult timeToLast = evaluationResult.get("MIN_TIME_BETWEEN_TRANSACTIONS");

                Future<ProbabilityStatistics> probabilityStatistics = criteriaProbabilityIntegration.load(criteriaValues);

                probabilityStatistics.setHandler(probabilitiesLoaded -> {
                    if (probabilitiesLoaded.failed()) {
                        System.out.println("Failed to load probabilities: " + probabilitiesLoaded.cause().getMessage());
                        throw new IllegalStateException(probabilitiesLoaded.cause());
                    }

                    ProbabilityStatistics statistics = probabilitiesLoaded.result();

                    Map<String, Risk.Value> groupValues = evaluator.evaluate(statistics);

                    Float fraudProbability = calculateFraudProbability(statistics.getFraudProbability(), groupValues);

                    event.reply(fraudProbability);

                    BehaviourData behaviour = new BehaviourData(
                        dailyRatioResult.getRawResult(),
                        weeklyRatioResult.getRawResult(),
                        monthlyRatioResult.getRawResult(),
                        Math.round(timeToLast.getRawResult()),
                        distanceFromLast.getRawResult(),
                        distanceFromCommon.getRawResult()
                    );
                    learningInitiator.publishData(requestData, behaviour, criteriaValues, groupValues);
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
