package org.ignas.frauddetection.httpapi;

import com.fasterxml.jackson.datatype.jsr310.JSR310Module;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.Json;
import io.vertx.ext.web.Router;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.api.validation.HTTPRequestValidationHandler;
import io.vertx.ext.web.api.validation.ValidationException;
import io.vertx.ext.web.handler.BodyHandler;
import org.apache.http.HttpStatus;
import org.ignas.frauddetection.httpapi.request.EvaluationRequest;
import org.ignas.frauddetection.transactionevaluation.api.request.FraudEvaluationRequest;

public class EvaluationRequestController extends AbstractVerticle {


    @Override
    public void start(Future<Void> startFuture) {
        HttpServer server = vertx.createHttpServer();

        Router router = Router.router(vertx);

        // Globally Register Jackson mapper module for Java 8 time mapping support
        Json.mapper.registerModule(new JSR310Module());
        EventBus eb = vertx.eventBus();

        vertx.fileSystem()
            .readFile("api-schemas/request-schema.json", event -> {

                if (event.failed()) {
                    System.out.println(event.cause().getMessage());
                    return;
                }

                String requestSchema = event.map(Buffer::toString).result();

                router.route(HttpMethod.POST, "/evaluate-fraud/:transactionId")
                    .handler(BodyHandler.create())
                    .handler(HTTPRequestValidationHandler.create()
                        .addJsonBodySchema(requestSchema))
                    .handler(req -> {
                        System.out.println("START:" + System.currentTimeMillis());
                        EvaluationRequest requestBody = req.getBodyAsJson()
                            .mapTo(EvaluationRequest.class);

                        FraudEvaluationRequest evaluate = new FraudEvaluationRequest(
                          requestBody.getTransactionId(),
                          requestBody.getCreditorAccountId(),
                          requestBody.getLocation(),
                          requestBody.getTime().toLocalDateTime()
                        );

                        eb.publish("transaction-mapping.resolver", evaluate);

                        req.response()
                            .putHeader("content-type", "application/json")
                            .end("Hello from Ignas!" + req.getBodyAsJson().toString());
                    })
                    .failureHandler(EvaluationRequestController::handleFailure);

                server.requestHandler(router::accept).listen(8080);
            });


        System.out.println("HTTP server started on port 8080");
    }

    private static void handleFailure(RoutingContext context) {
        HttpServerResponse response = context.response();
        Throwable failure = context.failure();

        if (failure instanceof ValidationException) {
            response.setStatusCode(HttpStatus.SC_BAD_REQUEST)
                .end("Bad request:\n" + context.failure().getMessage());
        } else {
            response.setStatusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR)
                .end("Internal error:\n" + context.failure().getMessage());
        }
    }
}
