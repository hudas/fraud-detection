package org.ignas.frauddetection.transactionevaluation.integration;

import io.vertx.core.Future;
import io.vertx.core.eventbus.EventBus;
import org.ignas.frauddetection.probabilitystatistics.api.request.CriteriaProbabilityRequest;
import org.ignas.frauddetection.probabilitystatistics.api.response.ProbabilityStatistics;
import org.ignas.frauddetection.shared.ServiceIntegration;

import java.util.Map;

public class BayesProbabilityIntegration implements ServiceIntegration<Map<String, String>, ProbabilityStatistics> {

    private EventBus bus;

    public BayesProbabilityIntegration(EventBus bus) {
        this.bus = bus;
    }

    @Override
    public Future<ProbabilityStatistics> load(Map<String, String> request) {
        Future<ProbabilityStatistics> loader = Future.future();

        bus.send(
            "probability-statistic.archive.criteria",
            new CriteriaProbabilityRequest(request),
            criteriaResponse -> {

                if (!(criteriaResponse.result().body() instanceof ProbabilityStatistics)) {
                    loader.fail("Unsupported message type: " + criteriaResponse.result().getClass());
                    return;
                }

                loader.complete((ProbabilityStatistics) criteriaResponse.result().body());
            }
        );

        return loader;
    }
}
