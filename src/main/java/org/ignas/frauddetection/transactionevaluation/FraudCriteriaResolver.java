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

public class FraudCriteriaResolver extends AbstractVerticle {

    public static final int CACHE_TTL = 10000;

    @Override
    public void start(Future<Void> setupFuture) throws Exception {
        EventBus bus = vertx.eventBus();

        registerCodecs(bus);

        List<String> groupNames = Arrays.stream(FraudCriteriaGroup.values())
            .map(Enum::name)
            .collect(Collectors.toList());

        GroupProbabilityCache cache = new GroupProbabilityCache(new FraudProbabilityLoader(bus, groupNames));

        cache.reload()
            .setHandler(cachePrepared -> {
                System.out.println("Cache loaded, will init vertice");

                vertx.setPeriodic(CACHE_TTL, (timer) -> cache.reload());

                bus.consumer("transaction-mapping.resolver")
                    .handler(event -> {

                        if (!(event.body() instanceof FraudEvaluationRequest)) {
                            event.fail(HttpStatus.SC_BAD_REQUEST, "Unsupported message type: " + event.body().getClass());
                            return;
                        }

                        FraudEvaluationRequest request = (FraudEvaluationRequest) event.body();

                        Transaction transactionData = mapToDomain(request);

                        bus.send(
                            "transaction-statistic.archive",
                            mapToAPI(transactionData),
                            statisticsResponse -> {

                                if (!(statisticsResponse.result().body() instanceof Statistics)) {
                                    statisticsResponse.result()
                                        .fail(HttpStatus.SC_BAD_REQUEST,
                                            "Unsupported message type: " + event.body().getClass());
                                    throw new IllegalStateException("Unsupported message type: " + event.body().getClass());
                                }

                                HistoricalData data = mapToDomain((Statistics) statisticsResponse.result().body());

                                bus.send(
                                    "probability-statistic.archive.criteria",
                                    new CriteriaProbabilityRequest(mapToCriteriaValues(transactionData, data)),
                                    criteriaResponse -> {

                                        if (!(criteriaResponse.result().body() instanceof ProbabilityStatistics)) {
                                            criteriaResponse.result()
                                                .fail(HttpStatus.SC_BAD_REQUEST, "Unsupported message type: " + event.body().getClass());
                                            throw new IllegalStateException("Unsupported message type: " + event.body().getClass());
                                        }

                                        ProbabilityStatistics criteriaProbabilities = (ProbabilityStatistics) criteriaResponse.result().body();
                                        event.reply(FraudEvaluator.evaluate(cache, criteriaProbabilities));
                                });
                        });
                    });

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

    private Map<String, String> mapToCriteriaValues(Transaction transactionData, HistoricalData data) {
        return FraudCriteriaConfig.definedCriterias()
                                .stream()
                                .collect(toMap(
                                    NamedCriteria::name,
                                    it -> it.evaluate(transactionData, data).representation())
                                );
    }

    private HistoricalData mapToDomain(Statistics historicalData) {
        GlobalStatistics globalStats = new GlobalStatistics(
            sumStatsFromDTO(historicalData),
            ratioStatsFromDTO(historicalData),
            countStatsFromDTO(historicalData),
            timeStatsFromDTO(historicalData),
            distanceStatsFromDTO(historicalData),
            distanceStatsFromDTO(historicalData)
        );

        EnvironmentStatistics environmentStats = new EnvironmentStatistics(
            fraudRateFromDTO(historicalData.getCreditorScore()),
            fraudRateFromDTO(historicalData.getLocationScore()),
            fraudRateFromDTO(historicalData.getTimeScore())
        );

        DebtorStatistics debtorStats = debtorFromDTO(historicalData);

        return new HistoricalData(debtorStats, globalStats, environmentStats);
    }

    private Transaction mapToDomain(FraudEvaluationRequest request) {
        return new Transaction(
            request.getTransactionId(),
            request.getAmount().floatValue(),
            request.getDebtorId(),
            request.getCreditorId(),
            request.getLocation(),
            request.getTime()
        );
    }

    private StatisticsRequest mapToAPI(Transaction transactionData) {
        return new StatisticsRequest(
                            transactionData.getDebtor(),
                            transactionData.getCreditor(),
                            transactionData.getLocation(),
                            transactionData.getTime()
                        );
    }

    private DebtorStatistics debtorFromDTO(Statistics statistics) {
        return new DebtorStatistics(
            statistics.getDebtorStatistics().getMostUsedLocation(),
            statistics.getDebtorStatistics().getLastTransactionLocation(),
            statistics.getDebtorStatistics().getMostValuableTransaction(),
            statistics.getDebtorStatistics().getLastTransactionExecutionTime(),
            Seconds.seconds(statistics.getDebtorStatistics().getMinTimeBetweenTransactions()),
            statistics.getDebtorStatistics().getPeriodicStatistics().stream()
                .map(this::personalPeriodFromDTO)
                .collect(toList())
        );
    }

    private PersonalPeriodStatistics personalPeriodFromDTO(PersonalPeriod stats) {
        return new PersonalPeriodStatistics(
                Days.days(stats.getPeriodLength()),
                stats.getExpensesSum(),
                stats.getTransactionCount()
        );
    }


    private FraudRate fraudRateFromDTO(CredibilityScore creditorScore) {
        return new FraudRate(
            creditorScore.getFraudRate(),
            creditorScore.getFraudRateAverage(),
            creditorScore.getFraudRateDeviation()
        );
    }

    /**
     * That's not DRY for sure.
     * In order to keep payload in event bus low we do not introduce any abstractions to DTO's which could help keep it DRY
     *
     * @param statistics
     * @return
     */
    private MeanStatistics<Float> distanceStatsFromDTO(Statistics statistics) {
        DistanceDifferenceStatistics stats = statistics.getPublicStatistics().getDistance()
                .stream()
                .findFirst()
                .orElseThrow(IllegalStateException::new);

        return MeanStatistics.<Float>builder()
                .pastValues(stats.getAverage(), stats.getDeviationAverage())
                .build();
    }

    private MeanStatistics<Seconds> timeStatsFromDTO(Statistics statistics) {
        TimeDifferenceStatistics stats = statistics.getPublicStatistics().getTime()
                .stream()
                .findFirst()
                .orElseThrow(IllegalStateException::new);

        return MeanStatistics.<Seconds>builder()
                .expectedValues(Seconds.seconds(stats.getExpected()), Seconds.seconds(stats.getDeviationExpected()))
                .pastValues(Seconds.seconds(stats.getAverage()), Seconds.seconds(stats.getDeviationAverage()))
                .build();
    }

    private List<MeanPeriodStatistics<Float>> ratioStatsFromDTO(Statistics historicalData) {
        return historicalData.getPublicStatistics()
                .getSumRatio()
                .stream()
                .map(ratio -> MeanPeriodStatistics.<Float>builder(Days.days(ratio.getPeriodLength()))
                        .pastValues(ratio.getAverage(), ratio.getDeviationAverage())
                        .build()
                )
                .collect(toList());
    }

    private List<MeanPeriodStatistics<Integer>> countStatsFromDTO(Statistics statistics) {
        return statistics.getPublicStatistics()
                            .getCount()
                            .stream()
                            .map(sumStatistics -> MeanPeriodStatistics.<Integer>builder(Days.days(sumStatistics.getPeriodLength()))
                                .expectedValues(sumStatistics.getExpected(), sumStatistics.getDeviationExpected())
                                .pastValues(sumStatistics.getAverage(), sumStatistics.getDeviationAverage())
                                .build()
                            )
                            .collect(toList());
    }

    private List<MeanPeriodStatistics<Float>> sumStatsFromDTO(Statistics statistics) {
        return statistics.getPublicStatistics()
                            .getSum()
                            .stream()
                            .map(sumStatistics -> MeanPeriodStatistics.<Float>builder(Days.days(sumStatistics.getPeriodLength()))
                                .expectedValues(sumStatistics.getExpected(), sumStatistics.getDeviationExpected())
                                .pastValues(sumStatistics.getAverage(), sumStatistics.getDeviationAverage())
                                .build()
                            )
                            .collect(toList());
    }
}
