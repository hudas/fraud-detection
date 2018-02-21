package org.ignas.frauddetection;

import io.vertx.core.AbstractVerticle;
import io.vertx.ext.web.Router;
import io.vertx.core.http.HttpServer;

public class MainVerticle extends AbstractVerticle {

    @Override
    public void start() throws Exception {
      HttpServer server = vertx.createHttpServer();

      Router router = Router.router(vertx);

      router.route().handler(req -> {
              req.response()
                .putHeader("content-type", "text/plain")
                .end("Hello from Ignas!");
            });

      server.requestHandler(router::accept).listen(8080);
      System.out.println("HTTP server started on port 8080");
    }
}
