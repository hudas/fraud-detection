package org.ignas.frauddetection.httpapi;

import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import org.ignas.frauddetection.httpapi.request.EvaluationRequest;
import org.ignas.frauddetection.httpapi.request.MarkRequest;
import org.ignas.frauddetection.resultsanalyser.api.ResultLoggingRequest;
import org.ignas.frauddetection.shared.OneWayServiceIntegration;
import org.ignas.frauddetection.shared.ServiceIntegration;

public class FraudulentTransactionHandler implements Handler<RoutingContext> {

    private OneWayServiceIntegration<String> preparationIntegration;

    public FraudulentTransactionHandler(
        OneWayServiceIntegration<String> preparationIntegration) {
        this.preparationIntegration = preparationIntegration;
    }

    @Override
    public void handle(RoutingContext req) {
        MarkRequest request = req.getBodyAsJson().mapTo(MarkRequest.class);

        preparationIntegration.publish(request.getTransactionId());
    }
}
