package org.ignas.frauddetection.httpapi;

import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import org.ignas.frauddetection.httpapi.request.EvaluationRequest;
import org.ignas.frauddetection.shared.ServiceIntegration;

public class EvaluationRequestHandler implements Handler<RoutingContext> {

    private ServiceIntegration<EvaluationRequest, Float> probabilityCalculator;

    public EvaluationRequestHandler(ServiceIntegration<EvaluationRequest, Float> probabilityCalculator) {
        this.probabilityCalculator = probabilityCalculator;
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
                        return;
                    }

                    String responseRepresentation =
                        String.format("{ \"fraud-probability\" : \"%f\" }", result.result());

                    req.response()
                        .putHeader("content-type", "application/json")
                        .end(responseRepresentation);
                }
        );
    }
}
