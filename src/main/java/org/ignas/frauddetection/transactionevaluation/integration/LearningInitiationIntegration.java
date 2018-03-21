package org.ignas.frauddetection.transactionevaluation.integration;

import io.vertx.core.eventbus.EventBus;
import org.ignas.frauddetection.shared.OneWayServiceIntegration;
import org.ignas.frauddetection.transactionevaluation.api.request.LearningRequest;

public class LearningInitiationIntegration implements OneWayServiceIntegration<LearningRequest> {

    private final EventBus bus;

    public LearningInitiationIntegration(EventBus bus) {
        this.bus = bus;
    }

    @Override
    public void publish(LearningRequest request) {
        bus.publish("learning.processing-request", request);
    }
}
