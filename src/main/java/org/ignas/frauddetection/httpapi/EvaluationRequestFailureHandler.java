package org.ignas.frauddetection.httpapi;

import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.api.validation.ValidationException;
import org.apache.http.HttpStatus;
import org.ignas.frauddetection.httpapi.request.EvaluationRequest;

public class EvaluationRequestFailureHandler implements Handler<RoutingContext> {

    @Override
    public void handle(RoutingContext context) {
        HttpServerResponse response = context.response();
        Throwable failure = context.failure();

        if (failure instanceof ValidationException) {
            response.setStatusCode(HttpStatus.SC_BAD_REQUEST)
                .end("Bad request:\n" + context.failure().getMessage());
        } else {
            if (failure != null) {
                response.setStatusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR)
                    .end("Internal error:\n" + context.failure().getMessage());
            } else {
                response.setStatusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR)
                    .end("Unknown Internal error");
            }
        }
    }
}
