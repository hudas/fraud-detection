package org.ignas.frauddetection.httpapi.integration;

import io.vertx.core.Future;
import io.vertx.core.eventbus.EventBus;
import org.ignas.frauddetection.httpapi.request.EvaluationRequest;
import org.ignas.frauddetection.shared.ServiceIntegration;
import org.ignas.frauddetection.transactionevaluation.api.request.FraudEvaluationRequest;

public class ProbabilityCalculatorIntegration implements ServiceIntegration<EvaluationRequest, Float> {

    public static final String RESOLVER_ADDRESS = "transaction-mapping.resolver";
    private EventBus bus;

    public ProbabilityCalculatorIntegration(EventBus bus) {
        this.bus = bus;
    }

    @Override
    public Future<Float> load(EvaluationRequest request) {
        Future<Float> loader = Future.future();

        bus.send(RESOLVER_ADDRESS, toResolverAPI(request), reply -> {

            if (!(reply.result().body() instanceof Float)) {
                loader.fail(new IllegalStateException("Unsupported message type: " + reply.result().getClass()));
            }

            loader.complete((Float) reply.result().body());
        });

        return loader;
    }

    private static FraudEvaluationRequest toResolverAPI(EvaluationRequest requestBody) {
        return new FraudEvaluationRequest(
            requestBody.getTransactionId(),
            requestBody.getAmount(),
            requestBody.getDebtorAccountId(),
            requestBody.getCreditorAccountId(),
            requestBody.getLocation(),
            requestBody.getTime().toLocalDateTime()
        );
    }
}