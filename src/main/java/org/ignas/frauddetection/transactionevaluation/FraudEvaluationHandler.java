package org.ignas.frauddetection.transactionevaluation;

import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import org.apache.http.HttpStatus;
import org.ignas.frauddetection.probabilitystatistics.api.response.ProbabilityStatistics;
import org.ignas.frauddetection.shared.ServiceIntegration;
import org.ignas.frauddetection.transactionevaluation.api.request.FraudEvaluationRequest;
import org.ignas.frauddetection.transactionevaluation.cache.GroupProbabilityCache;
import org.ignas.frauddetection.transactionevaluation.domain.Transaction;
import org.ignas.frauddetection.transactionevaluation.domain.calculation.criteria.NamedCriteria;
import org.ignas.frauddetection.transactionevaluation.domain.config.FraudCriteriaConfig;
import org.ignas.frauddetection.transactionevaluation.domain.config.FraudCriteriaEvaluator;
import org.ignas.frauddetection.transactionevaluation.domain.stats.HistoricalData;
import org.ignas.frauddetection.transactionevaluation.service.FraudEvaluator;

import java.util.Map;

import static java.util.stream.Collectors.toMap;

public class FraudEvaluationHandler implements Handler<Message<Object>> {

    private GroupProbabilityCache cache;
    private ServiceIntegration<Transaction, HistoricalData> transactionStatisticsIntegration;
    private ServiceIntegration<Map<String, String>, ProbabilityStatistics> probabilityStatisticsIntegration;

    private static FraudCriteriaEvaluator criteriaEvaluator = new FraudCriteriaEvaluator();
    private static FraudEvaluator evaluator = new FraudEvaluator(criteriaEvaluator);

    public FraudEvaluationHandler(
        GroupProbabilityCache cache,
        ServiceIntegration<Transaction, HistoricalData> transactionStatisticsIntegration,
        ServiceIntegration<Map<String, String>, ProbabilityStatistics> probabilityStatisticsIntegration) {

        this.cache = cache;
        this.transactionStatisticsIntegration = transactionStatisticsIntegration;
        this.probabilityStatisticsIntegration = probabilityStatisticsIntegration;
    }

    @Override
    public void handle(Message<Object> event) {
        if (!(event.body() instanceof FraudEvaluationRequest)) {
            event.fail(HttpStatus.SC_BAD_REQUEST, "Unsupported message type: " + event.body().getClass());
            return;
        }

        Transaction transactionData = mapToDomain((FraudEvaluationRequest) event.body());

        Future<HistoricalData> transactionStatistics = transactionStatisticsIntegration.load(transactionData);

        transactionStatistics.setHandler(data -> {
            HistoricalData transactionDataHistory = data.result();

            Future<ProbabilityStatistics> probabilityStatistics =
                probabilityStatisticsIntegration.load(criteriaEvaluator.evaluateAll(transactionData, transactionDataHistory));

            probabilityStatistics.setHandler(
                probabilityData -> event.reply(evaluator.evaluate(cache ,probabilityData.result())));
        });
    }


    private Transaction mapToDomain(FraudEvaluationRequest request) {
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
