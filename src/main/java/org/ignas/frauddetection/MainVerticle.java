package org.ignas.frauddetection;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.Router;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.web.api.validation.HTTPRequestValidationHandler;
import io.vertx.ext.web.api.validation.ValidationException;
import io.vertx.ext.web.handler.BodyHandler;
import org.apache.http.HttpStatus;

public class MainVerticle extends AbstractVerticle {

    @Override
    public void start() {
        HttpServer server = vertx.createHttpServer();

        Router router = Router.router(vertx);

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
                        req.response()
                            .putHeader("content-type", "application/json")
                            .end("Hello from Ignas!" + req.getBodyAsJson().toString());
                    })
                    .failureHandler(context -> {
                        HttpServerResponse response = context.response();
                        Throwable failure = context.failure();

                        if (failure instanceof ValidationException) {
                            response.setStatusCode(HttpStatus.SC_BAD_REQUEST)
                                .end("Bad request:\n" + context.failure().getMessage());
                        } else {
                            response.setStatusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR).end();
                        }
                    });

                server.requestHandler(router::accept).listen(8080);
            });


        System.out.println("HTTP server started on port 8080");
    }
}
