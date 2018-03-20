package org.ignas.frauddetection.resultsanalyser;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.eventbus.EventBus;
import org.ignas.frauddetection.resultsanalyser.api.ResultLoggingRequest;
import org.ignas.frauddetection.shared.ImmutableObjectCodec;

public class ResultsAnalyser extends AbstractVerticle {

    /**
     * Threadsafe storage
     */
    private RequestLogsStorage storage;

    @Override
    public void start(Future<Void> startFuture) {
        setupStorage();

        EventBus bus = vertx.eventBus();
        bus.registerDefaultCodec(
            ResultLoggingRequest.class,
            new ImmutableObjectCodec<ResultLoggingRequest>(ResultLoggingRequest.class)
        );

        bus.consumer("results.analyser", request -> {
            if (!(request.body() instanceof ResultLoggingRequest)) {
                return;
            }

            storage.log((ResultLoggingRequest) request.body());
        });

        startFuture.complete();
    }

    private void setupStorage() {
        storage = new RequestLogsStorage(
            "mongodb://localhost",
            "logging",
            "evaluations"
        );
    }

    @Override
    public void stop() {
        storage.close();
    }
}
