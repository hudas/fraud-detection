package org.ignas.frauddetection.httpapi;

import com.fasterxml.jackson.datatype.joda.JodaModule;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.Json;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.api.validation.HTTPRequestValidationHandler;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.TimeoutHandler;

public class EvaluationRequestController extends AbstractVerticle {


    public static final int DEFAULT_TIMEOUT = 1000;

    @Override
    public void start(Future<Void> startFuture) {
        registerDateMapper();

        vertx.fileSystem()
            .readFile("api-schemas/request-schema.json", configLoading -> {
                if (configLoading.failed()) {
                    System.out.println(configLoading.cause().getMessage());
                    return;
                }

                String validationSchema = configLoading.map(Buffer::toString).result();

                Router router = buildRouter(validationSchema);

                vertx.createHttpServer().requestHandler(router::accept).listen(8080);
                System.out.println("HTTP server started on port 8080");
            });
    }

    private Router buildRouter(String requestSchema) {
        EventBus eb = vertx.eventBus();

        Router router = Router.router(vertx);

        router.route(HttpMethod.POST, "/evaluate-fraud/:transactionId")
            .handler(BodyHandler.create())
            .handler(HTTPRequestValidationHandler.create().addJsonBodySchema(requestSchema))
            .handler(TimeoutHandler.create(DEFAULT_TIMEOUT, 500))
            .handler(new EvaluationRequestHandler(eb))
            .failureHandler(new EvaluationRequestFailureHandler());

        return router;
    }

    private void registerDateMapper() {
        Json.mapper.registerModule(new JodaModule());
    }
}
