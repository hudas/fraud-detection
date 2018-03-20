package org.ignas.frauddetection.httpapi.integration;

import io.vertx.core.eventbus.EventBus;
import org.ignas.frauddetection.resultsanalyser.api.ResultLoggingRequest;
import org.ignas.frauddetection.shared.OneWayServiceIntegration;

public class RequestLoggerIntegration implements OneWayServiceIntegration<ResultLoggingRequest> {

    private EventBus bus;

    public RequestLoggerIntegration(EventBus bus) {
        this.bus = bus;
    }

    @Override
    public void publish(ResultLoggingRequest request) {
        bus.publish("results.analyser", request);
    }

}
