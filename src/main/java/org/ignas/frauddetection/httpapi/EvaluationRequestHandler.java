package org.ignas.frauddetection.httpapi;

import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.ext.web.RoutingContext;
import org.ignas.frauddetection.httpapi.request.EvaluationRequest;
import org.ignas.frauddetection.resultsanalyser.api.ResultLoggingRequest;
import org.ignas.frauddetection.shared.OneWayServiceIntegration;
import org.ignas.frauddetection.shared.ServiceIntegration;

public class EvaluationRequestHandler implements Handler<RoutingContext> {

    private ServiceIntegration<EvaluationRequest, Float> probabilityCalculator;
    private OneWayServiceIntegration<ResultLoggingRequest> resultLogger;

    public EvaluationRequestHandler(
        ServiceIntegration<EvaluationRequest, Float> probabilityCalculator,
        OneWayServiceIntegration<ResultLoggingRequest> resultLogger) {

        this.probabilityCalculator = probabilityCalculator;
        this.resultLogger = resultLogger;
    }

    @Override
    public void handle(RoutingContext req) {
        EvaluationRequest request = req.getBodyAsJson()
            .mapTo(EvaluationRequest.class);

        Future<Float> probabilityLoading = probabilityCalculator.load(request);

        probabilityLoading
            .setHandler(
                result -> {
                    if (result.failed()){
                        req.fail(500);
                        result.cause().printStackTrace();
                        System.out.println(result.cause().getMessage());
                        return;
                    }

                    Float probability = result.result();

                    resultLogger.publish(mapToRequest(request, probability));

                    String responseRepresentation =
                        String.format("{ \"fraud-probability\" : \"%f\" }", probability);


                    req.response()
                        .putHeader("content-type", "application/json")
                        .end(responseRepresentation);
                }
        );
    }

    private ResultLoggingRequest mapToRequest(EvaluationRequest request, Float probability) {
        return new ResultLoggingRequest(
            request.getTransactionId(),
            request.getDebtorCreditCardId(),
            request.getDebtorAccountId(),
            request.getCreditorAccountId(),
            request.getAmount(),
            request.getTime().toLocalDateTime(),
            request.getLocation(),
            probability
        );
    }
}
