package org.ignas.frauddetection.httpapi;

import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.ext.web.RoutingContext;
import org.ignas.frauddetection.httpapi.request.EvaluationRequest;
import org.ignas.frauddetection.transactionevaluation.api.request.FraudEvaluationRequest;

public class EvaluationRequestHandler implements Handler<RoutingContext> {
    private final EventBus eb;

    public EvaluationRequestHandler(EventBus eb) {
        this.eb = eb;
    }

    @Override
    public void handle(RoutingContext req) {
        EvaluationRequest requestBody = req.getBodyAsJson()
            .mapTo(EvaluationRequest.class);

        eb.send("transaction-mapping.resolver", toResolverAPI(requestBody), reply -> {

            if (!(reply.result().body() instanceof Float)) {
                req.fail(new IllegalStateException("Unsupported message type: " + reply.result().getClass()));
            }

            Float result = (Float) reply.result().body();

            req.response()
                .putHeader("content-type", "application/json")
                .end(String.format("{ \"fraud-probability\" : \"%f\" }", result));
        });
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
