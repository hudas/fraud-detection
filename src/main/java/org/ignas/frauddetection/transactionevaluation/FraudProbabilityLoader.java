package org.ignas.frauddetection.transactionevaluation;

import io.vertx.core.Future;
import io.vertx.core.eventbus.EventBus;
import org.ignas.frauddetection.probabilitystatistics.api.request.CriteriaGroupProbabilityRequest;
import org.ignas.frauddetection.probabilitystatistics.api.response.BayesTable;
import org.ignas.frauddetection.shared.ServiceIntegration;
import org.ignas.frauddetection.transactionevaluation.cache.GroupProbabilityCache;
import org.ignas.frauddetection.transactionevaluation.domain.CriteriaGroup;

import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toMap;

public class FraudProbabilityLoader implements ServiceIntegration<List<String>, Map<String, CriteriaGroup>> {

    private EventBus bus;

    public FraudProbabilityLoader(EventBus bus) {
        this.bus = bus;
    }

    @Override
    public Future<Map<String, CriteriaGroup>> load(List<String> groups) {
        Future<Map<String, CriteriaGroup>> future = Future.future();

        bus.send(
            "probability-statistic.archive.criteria-group",
            new CriteriaGroupProbabilityRequest(groups),
            (reply) -> {

                if (!(reply.result().body() instanceof BayesTable)) {
                    throw new IllegalStateException("Unsupported message type: " + reply.result().body().getClass());
                }

                BayesTable result = (BayesTable) reply.result().body();

                Map<String, CriteriaGroup> resultToCache = result.getTable().entrySet()
                    .stream()
                    .collect(toMap(Map.Entry::getKey, entry -> new CriteriaGroup(entry.getValue())));

                future.complete(resultToCache);
            }
        );

        return future;
    }
}
