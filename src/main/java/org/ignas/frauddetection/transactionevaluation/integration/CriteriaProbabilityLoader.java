package org.ignas.frauddetection.transactionevaluation.integration;

import io.vertx.core.Future;
import io.vertx.core.eventbus.EventBus;
import org.ignas.frauddetection.probabilitystatistics.api.request.CriteriaProbabilityRequest;
import org.ignas.frauddetection.probabilitystatistics.api.response.ProbabilityStatistics;
import org.ignas.frauddetection.shared.ServiceIntegration;

import java.util.Map;

/**
 * At this point it becomes clear that All loaders can be replaced with single good generics based abstraction which receives address and required mappers.
 * TODO: Refactor further
 */
public class CriteriaProbabilityLoader implements ServiceIntegration<Map<String, String>, ProbabilityStatistics> {

    public static final String ARCHIVE_ADDRESS = "probability-statistic.archive.criteria";
    private EventBus bus;

    public CriteriaProbabilityLoader(EventBus bus) {
        this.bus = bus;
    }

    @Override
    public Future<ProbabilityStatistics> load(Map<String, String> request) {
        Future<ProbabilityStatistics> loader = Future.future();

        bus.send(ARCHIVE_ADDRESS, new CriteriaProbabilityRequest(request), criteriaResponse -> {
            if (!(criteriaResponse.result().body() instanceof ProbabilityStatistics)) {
                loader.fail("Unsupported message type: " + criteriaResponse.result().getClass());
                return;
            }

            loader.complete((ProbabilityStatistics) criteriaResponse.result().body());
        });

        return loader;
    }
}
