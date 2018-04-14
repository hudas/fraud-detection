package org.ignas.frauddetection.transactionevaluation.integration;

import io.vertx.core.Future;
import io.vertx.core.eventbus.EventBus;
import org.ignas.frauddetection.probabilitystatistics.api.request.CriteriaGroupProbabilityRequest;
import org.ignas.frauddetection.probabilitystatistics.api.response.BayesTable;
import org.ignas.frauddetection.shared.ServiceIntegration;
import org.ignas.frauddetection.transactionevaluation.domain.CriteriaGroup;
import org.ignas.frauddetection.transactionevaluation.integration.converters.GroupProbabilityAPIMapper;

import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toMap;
import static org.ignas.frauddetection.transactionevaluation.integration.converters.GroupProbabilityAPIMapper.mapRequest;
import static org.ignas.frauddetection.transactionevaluation.integration.converters.GroupProbabilityAPIMapper.mapResponse;

/**
 * At this point it becomes clear that All loaders can be replaced with single good generics based abstraction which receives address and required mappers.
 * TODO: Refactor further
 */
public class FraudProbabilityLoader implements ServiceIntegration<List<String>, Map<String, CriteriaGroup>> {

    public static final String PROBABILITY_STATISTIC_ADDRESS = "probability-statistic.archive.criteria-group";
    private EventBus bus;

    public FraudProbabilityLoader(EventBus bus) {
        this.bus = bus;
    }

    @Override
    public Future<Map<String, CriteriaGroup>> load(List<String> groups) {
        Future<Map<String, CriteriaGroup>> loader = Future.future();

        bus.send(PROBABILITY_STATISTIC_ADDRESS, mapRequest(groups), (reply) -> {
            if (reply.failed()) {
                System.out.println("FraudProbabilityLoader" + reply.cause().getMessage());
                reply.cause().printStackTrace();
                loader.fail(reply.cause());
                return;
            }

            if (!(reply.result().body() instanceof BayesTable)) {
                loader.fail("Unsupported message type: " + reply.result().getClass());
                return;
            }

            loader.complete(mapResponse((BayesTable) reply.result().body()));
        });

        return loader;
    }
}
