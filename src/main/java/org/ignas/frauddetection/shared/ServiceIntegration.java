package org.ignas.frauddetection.shared;

import io.vertx.core.Future;

public interface ServiceIntegration<T, R> {
    Future<R> load(T request);
}
