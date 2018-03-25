package org.ignas.frauddetection.shared;

public interface OneWayServiceIntegration<T> {
    void publish(T request);
}
