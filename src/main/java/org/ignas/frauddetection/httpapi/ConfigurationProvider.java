package org.ignas.frauddetection.httpapi;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;

public class ConfigurationProvider {

    public Vertx vertx;

    public ConfigurationProvider(Vertx vertx) {
        this.vertx = vertx;
    }

    public Future<String> loadApiSchema() {
        Future<String> loader = Future.future();

        vertx.fileSystem()
            .readFile("api-schemas/request-schema.json", configLoading -> {
                if (configLoading.failed()) {
                    System.out.println(configLoading.cause().getMessage());
                    loader.fail(configLoading.cause().getMessage());
                    return;
                }

                loader.complete(configLoading.map(Buffer::toString).result());
            });

        return loader;
    }
}
