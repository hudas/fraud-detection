package org.ignas.frauddetection.transactionevaluation;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.eventbus.EventBus;
import org.ignas.frauddetection.DetectionLauncher;
import org.ignas.frauddetection.probabilitystatistics.api.request.CriteriaGroupProbabilityRequest;
import org.ignas.frauddetection.probabilitystatistics.api.request.CriteriaProbabilityRequest;
import org.ignas.frauddetection.shared.ImmutableObjectCodec;
import org.ignas.frauddetection.shared.OneWayServiceIntegration;
import org.ignas.frauddetection.transactionevaluation.api.request.LearningRequest;
import org.ignas.frauddetection.transactionevaluation.api.request.TransactionData;
import org.ignas.frauddetection.transactionevaluation.cache.GroupProbabilityCache;
import org.ignas.frauddetection.transactionevaluation.integration.CriteriaProbabilityLoader;
import org.ignas.frauddetection.transactionevaluation.integration.FraudProbabilityLoader;
import org.ignas.frauddetection.transactionevaluation.integration.LearningInitiationIntegration;
import org.ignas.frauddetection.transactionevaluation.integration.TransactionStatisticsLoader;

public class TransactionMappingResolver extends AbstractVerticle {


    @Override
    public void start(Future<Void> setupFuture) {
        EventBus bus = vertx.eventBus();

        registerCodecs(bus);

        GroupProbabilityCache cache = new GroupProbabilityCache(new FraudProbabilityLoader(bus));

        cache.reload()
            .setHandler(cachePrepared -> {
                System.out.println("Cache loaded, will init vertice");

                vertx.setPeriodic(DetectionLauncher.CACHE_TTL, (timer) -> cache.reload());

                bus.consumer("transaction-mapping.resolver")
                    .handler(
                        new FraudEvaluationHandler(
                            cache,
                            new TransactionStatisticsLoader(bus),
                            new CriteriaProbabilityLoader(bus),
                            new LearningInitiationIntegration(bus)
                        )
                    );

                setupFuture.complete();
        });
    }

    private void registerCodecs(EventBus bus) {
        bus.registerDefaultCodec(
            CriteriaGroupProbabilityRequest.class,
            new ImmutableObjectCodec<>(CriteriaGroupProbabilityRequest.class)
        );

        bus.registerDefaultCodec(
            CriteriaProbabilityRequest.class,
            new ImmutableObjectCodec<>(CriteriaProbabilityRequest.class)
        );

        bus.registerDefaultCodec(
            TransactionData.class,
            new ImmutableObjectCodec<>(TransactionData.class)
        );

        bus.registerDefaultCodec(
            LearningRequest.class,
            new ImmutableObjectCodec<>(LearningRequest.class)
        );
    }
}
