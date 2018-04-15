package org.ignas.frauddetection.httpapi.integration;

import io.vertx.core.Future;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.EventBus;
import org.ignas.frauddetection.httpapi.request.EvaluationRequest;
import org.ignas.frauddetection.shared.ServiceIntegration;
import org.ignas.frauddetection.transactionevaluation.api.request.TransactionData;

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

            if (reply.failed()) {
                System.out.println("ProbabilityCalculatorIntegration" + reply.cause().getMessage());
                reply.cause().printStackTrace();
                loader.fail(reply.cause());
                return;
            }

            if (!(reply.result().body() instanceof Float)) {
                loader.fail(new IllegalStateException("ProbabilityCalculatorIntegration: Unsupported message type: " + reply.result().getClass()));
                return;
            }

            loader.complete((Float) reply.result().body());
        });

        return loader;
    }

    private static TransactionData toResolverAPI(EvaluationRequest domain) {
        return new TransactionData(
            domain.getTransactionId(),
            domain.getAmount().floatValue(),
            domain.getDebtorAccountId(),
            domain.getCreditorAccountId(),
            domain.getLocation(),
            domain.getTime().toLocalDateTime()
        );
    }
}
