package org.ignas.frauddetection.transactionevaluation.integration;

import io.vertx.core.Future;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.EventBus;
import org.ignas.frauddetection.probabilitystatistics.api.request.CriteriaProbabilityRequest;
import org.ignas.frauddetection.probabilitystatistics.api.response.ProbabilityStatistics;
import org.ignas.frauddetection.shared.ServiceIntegration;
import org.ignas.frauddetection.transactionevaluation.api.request.CriteriaContainer;

import java.util.Map;

/**
 * At this point it becomes clear that All loaders can be replaced with single good generics based abstraction which receives address and required mappers.
 * TODO: Refactor further
 */
public class CriteriaProbabilityLoader implements ServiceIntegration<CriteriaContainer, ProbabilityStatistics> {

    public static final String ARCHIVE_ADDRESS = "probability-statistic.archive.criteria";
    private EventBus bus;

    public CriteriaProbabilityLoader(EventBus bus) {
        this.bus = bus;
    }

    @Override
    public Future<ProbabilityStatistics> load(CriteriaContainer request) {
        Future<ProbabilityStatistics> loader = Future.future();

        long start = System.currentTimeMillis();

        DeliveryOptions options = new DeliveryOptions().setSendTimeout(300000);
        bus.send(ARCHIVE_ADDRESS, new CriteriaProbabilityRequest(request), options, criteriaResponse -> {

            if (criteriaResponse.failed()) {
                criteriaResponse.cause().printStackTrace();

                long end = System.currentTimeMillis();
//                System.out.println("CriteriaProbabilityLoader.load TOOK:" + (end-start) + " And failed Transaction: " + request.getTransactionId());
                loader.fail(criteriaResponse.cause());
                return;
            }


            if (!(criteriaResponse.result().body() instanceof ProbabilityStatistics)) {
                //loader.fail("Unsupported message type: " + criteriaResponse.result().getClass());
                return;
            }

            long end = System.currentTimeMillis();
//            System.out.println("CriteriaProbabilityLoader.load TOOK:" + (end-start) + "                Transaction: " + request.getTransactionId());
            loader.complete((ProbabilityStatistics) criteriaResponse.result().body());
        });

        return loader;
    }
}
