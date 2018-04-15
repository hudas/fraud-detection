package org.ignas.frauddetection.httpapi;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;

public class ConfigurationProvider {

    public Vertx vertx;

    public ConfigurationProvider(Vertx vertx) {
        this.vertx = vertx;
    }

    public Future<APISchemas> loadApiSchemas() {
        Future<APISchemas> future = Future.future();

        Future<String> evaluationLoader = loadSchema("api-schemas/evaluation-request-schema.json");
        Future<String> markLoader = loadSchema("api-schemas/mark-request-schema.json");

        CompositeFuture.all(evaluationLoader, markLoader).setHandler(config -> {
            if (config.failed()) {
                System.out.println(config.cause().getMessage());
                future.fail(config.cause().getMessage());
                return;
            }

            future.complete(new APISchemas(config.result().resultAt(0), config.result().resultAt(1)));
        });

        return future;
    }

    private Future<String> loadSchema(String s) {
        Future<String> markLoader = Future.future();

        vertx.fileSystem()
            .readFile(s, configLoading -> {
                if (configLoading.failed()) {
                    System.out.println(configLoading.cause().getMessage());
                    markLoader.fail(configLoading.cause().getMessage());
                    return;
                }

                markLoader.complete(configLoading.map(Buffer::toString).result());
            });
        return markLoader;
    }
}
