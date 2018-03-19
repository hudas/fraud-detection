package org.ignas.frauddetection.transactionevaluation;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.eventbus.EventBus;
import org.apache.http.HttpStatus;
import org.ignas.frauddetection.probabilitystatistics.api.request.CriteriaGroupProbabilityRequest;
import org.ignas.frauddetection.probabilitystatistics.api.request.CriteriaProbabilityRequest;
import org.ignas.frauddetection.probabilitystatistics.api.response.ProbabilityStatistics;
import org.ignas.frauddetection.shared.FraudCriteriaGroup;
import org.ignas.frauddetection.shared.ImmutableObjectCodec;
import org.ignas.frauddetection.transactionevaluation.api.request.FraudEvaluationRequest;
import org.ignas.frauddetection.transactionevaluation.cache.GroupProbabilityCache;
import org.ignas.frauddetection.transactionevaluation.domain.Transaction;
import org.ignas.frauddetection.transactionevaluation.domain.calculation.criteria.NamedCriteria;
import org.ignas.frauddetection.transactionevaluation.domain.config.FraudCriteriaConfig;
import org.ignas.frauddetection.transactionevaluation.domain.stats.DebtorStatistics;
import org.ignas.frauddetection.transactionevaluation.domain.stats.EnvironmentStatistics;
import org.ignas.frauddetection.transactionevaluation.domain.stats.GlobalStatistics;
import org.ignas.frauddetection.transactionevaluation.domain.stats.HistoricalData;
import org.ignas.frauddetection.transactionevaluation.domain.stats.details.*;
import org.ignas.frauddetection.transactionevaluation.integration.BayesProbabilityIntegration;
import org.ignas.frauddetection.transactionevaluation.integration.TransactionStatisticsIntegration;
import org.ignas.frauddetection.transactionevaluation.service.FraudEvaluator;
import org.ignas.frauddetection.transactionstatistics.api.request.StatisticsRequest;
import org.ignas.frauddetection.transactionstatistics.api.response.CredibilityScore;
import org.ignas.frauddetection.transactionstatistics.api.response.PersonalPeriod;
import org.ignas.frauddetection.transactionstatistics.api.response.Statistics;
import org.ignas.frauddetection.transactionstatistics.api.response.generalindicators.DistanceDifferenceStatistics;
import org.ignas.frauddetection.transactionstatistics.api.response.generalindicators.TimeDifferenceStatistics;
import org.joda.time.Days;
import org.joda.time.Seconds;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.*;

public class EvaluationController extends AbstractVerticle {

    public static final int CACHE_TTL = 10000;

    @Override
    public void start(Future<Void> setupFuture) throws Exception {
        EventBus bus = vertx.eventBus();

        registerCodecs(bus);

        GroupProbabilityCache cache = new GroupProbabilityCache(new FraudProbabilityLoader(bus));

        cache.reload()
            .setHandler(cachePrepared -> {
                System.out.println("Cache loaded, will init vertice");

                vertx.setPeriodic(CACHE_TTL, (timer) -> cache.reload());

                bus.consumer("transaction-mapping.resolver")
                    .handler(
                        new FraudEvaluationHandler(
                            cache,
                            new TransactionStatisticsIntegration(bus),
                            new BayesProbabilityIntegration(bus))
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
            FraudEvaluationRequest.class,
            new ImmutableObjectCodec<>(FraudEvaluationRequest.class)
        );
    }
}
