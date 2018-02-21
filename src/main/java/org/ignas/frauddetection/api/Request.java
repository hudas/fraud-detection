package org.ignas.frauddetection.api;

import io.vertx.core.Handler;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.RoutingContext;

public interface Request {

    String path();
    HttpMethod method();

    String acceptedContentType();
    String jsonSchema();

    Handler<RoutingContext> handler();
}
