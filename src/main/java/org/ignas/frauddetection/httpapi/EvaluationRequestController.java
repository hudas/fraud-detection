package org.ignas.frauddetection.httpapi;

import com.fasterxml.jackson.datatype.joda.JodaModule;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.Json;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.api.validation.HTTPRequestValidationHandler;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.TimeoutHandler;
import org.ignas.frauddetection.httpapi.integration.ProbabilityCalculatorIntegration;
import org.ignas.frauddetection.httpapi.integration.RequestLoggerIntegration;
import org.ignas.frauddetection.resultsanalyser.api.ResultLoggingRequest;
import org.ignas.frauddetection.shared.DetectionStatus;
import org.ignas.frauddetection.shared.OneWayServiceIntegration;

public class EvaluationRequestController extends AbstractVerticle {

    public static final int DEFAULT_TIMEOUT = 3000;
    public static final int SERVER_PORT = 8081;

    @Override
    public void start(Future<Void> verticeStartup) {
        registerDateMapper();

        new ConfigurationProvider(vertx)
            .loadApiSchemas()
            .setHandler(validationSchemas -> {
                Router router = buildRouter(validationSchemas.result());

                vertx.createHttpServer()
                    .requestHandler(router::accept)
                    .listen(SERVER_PORT);

                System.out.println("HTTP server started on port " + SERVER_PORT);

                verticeStartup.complete();
            });
    }

    private Router buildRouter(APISchemas schemas) {
        EventBus bus = vertx.eventBus();

        Router router = Router.router(vertx);

        router.route(HttpMethod.POST, "/evaluate-fraud")
            .handler(BodyHandler.create())
            .handler(HTTPRequestValidationHandler.create().addJsonBodySchema(schemas.getEvaluationSchema()))
            .handler(TimeoutHandler.create(DEFAULT_TIMEOUT, DetectionStatus.TIMEOUT))
            .handler(
                new EvaluationRequestHandler(
                    new ProbabilityCalculatorIntegration(bus),
                    new RequestLoggerIntegration(bus))
            )
            .failureHandler(new EvaluationRequestFailureHandler());

        router.route(HttpMethod.POST, "/mark-fraudulent")
            .handler(BodyHandler.create())
            .handler(TimeoutHandler.create(DEFAULT_TIMEOUT, DetectionStatus.TIMEOUT))
            .handler(HTTPRequestValidationHandler.create().addJsonBodySchema(schemas.getMarkingSchema()))
            .handler(
                new FraudulentTransactionHandler(request -> bus.publish("evaluation-archive.marked-fraudulent", request))
            )
            .failureHandler(new EvaluationRequestFailureHandler());

        return router;
    }

    private void registerDateMapper() {
        Json.mapper.registerModule(new JodaModule());
    }
}
