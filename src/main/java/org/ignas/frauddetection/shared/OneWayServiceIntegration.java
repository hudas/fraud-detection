package org.ignas.frauddetection.shared;

import io.vertx.core.Future;

public interface OneWayServiceIntegration<T> {
    void publish(T request);
}
