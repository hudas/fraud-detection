package org.ignas.frauddetection.transactionevaluation.integration;

import io.vertx.core.Future;
import io.vertx.core.eventbus.EventBus;
import org.ignas.frauddetection.probabilitystatistics.api.request.CriteriaGroupProbabilityRequest;
import org.ignas.frauddetection.probabilitystatistics.api.response.BayesTable;
import org.ignas.frauddetection.shared.ServiceIntegration;

import java.util.List;

import static org.ignas.frauddetection.transactionevaluation.integration.converters.GroupProbabilityAPIMapper.mapRequest;

/**
 * At this point it becomes clear that All loaders can be replaced with single good generics based abstraction which receives address and required mappers.
 * TODO: Refactor further
 */
public class FraudProbabilityLoader implements ServiceIntegration<List<String>, BayesTable> {

    public static final String PROBABILITY_STATISTIC_ADDRESS = "probability-statistic.archive.criteria-group";
    private EventBus bus;

    public FraudProbabilityLoader(EventBus bus) {
        this.bus = bus;
    }

    @Override
    public Future<BayesTable> load(List<String> groups) {
        Future<BayesTable> loader = Future.future();

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

            loader.complete((BayesTable) reply.result().body());
        });

        return loader;
    }
}
