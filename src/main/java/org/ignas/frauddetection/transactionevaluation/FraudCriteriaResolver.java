package org.ignas.frauddetection.transactionevaluation;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.EventBus;
import org.apache.http.HttpStatus;
import org.ignas.frauddetection.shared.ImmutableObjectCodec;
import org.ignas.frauddetection.transactionevaluation.api.request.FraudEvaluationRequest;
import org.ignas.frauddetection.transactionevaluation.domain.Transaction;
import org.ignas.frauddetection.transactionevaluation.domain.stats.DebtorStatistics;
import org.ignas.frauddetection.transactionevaluation.domain.stats.EnvironmentStatistics;
import org.ignas.frauddetection.transactionevaluation.domain.stats.GlobalStatistics;
import org.ignas.frauddetection.transactionevaluation.domain.stats.details.FraudRate;
import org.ignas.frauddetection.transactionevaluation.domain.stats.details.MeanPeriodStatistics;
import org.ignas.frauddetection.transactionevaluation.domain.stats.details.PersonalPeriodStatistics;
import org.ignas.frauddetection.transactionstatistic.api.request.StatisticsRequest;
import org.ignas.frauddetection.transactionstatistic.api.response.CredibilityScore;
import org.ignas.frauddetection.transactionstatistic.api.response.PersonalPeriod;
import org.ignas.frauddetection.transactionstatistic.api.response.Statistics;
import org.joda.time.Days;
import org.joda.time.Seconds;

import java.util.List;
import java.util.stream.Collectors;

public class FraudCriteriaResolver extends AbstractVerticle {

    @Override
    public void start() throws Exception {
        EventBus bus = vertx.eventBus();
        bus.registerDefaultCodec(FraudEvaluationRequest.class, new ImmutableObjectCodec<>(FraudEvaluationRequest.class));

        bus.consumer("transaction-mapping.resolver")
            .handler(event -> {

                if (!(event.body() instanceof FraudEvaluationRequest)) {
                    event.fail(HttpStatus.SC_BAD_REQUEST, "Unsupported message type: " + event.body().getClass());
                    return;
                }

                FraudEvaluationRequest request = (FraudEvaluationRequest) event.body();

                Transaction transaction = new Transaction(
                    request.getTransactionId(),
                    request.getAmount(),
                    request.getDebtorId(),
                    request.getCreditorId(),
                    request.getLocation(),
                    request.getTime()
                );

                StatisticsRequest requestForStatistics = new StatisticsRequest(
                    transaction.getDebtor(),
                    transaction.getCreditor(),
                    transaction.getLocation(),
                    transaction.getTime()
                );

                bus.send("transaction-statistic.archive", requestForStatistics, (reply) -> {

                    if (!(reply.result().body() instanceof Statistics)) {
                        reply.result().fail(HttpStatus.SC_BAD_REQUEST, "Unsupported message type: " + event.body().getClass());
                        return;
                    }

                    Statistics statistics = (Statistics) reply.result().body();

                    GlobalStatistics globalStats = new GlobalStatistics(
                        sumStatsFromDTO(statistics),
                        countStatsFromDTO(statistics),
                        timeStatsFromDTO(statistics),
                        distanceStatsFromDTO(statistics)
                    );

                    EnvironmentStatistics environmentStats = new EnvironmentStatistics(
                        fraudRateFromDTO(statistics.getCreditorScore()),
                        fraudRateFromDTO(statistics.getLocationScore()),
                        fraudRateFromDTO(statistics.getTimeScore())
                    );

                    DebtorStatistics debtorStats = debtorFromDTO(statistics);

                    System.out.println("Reply Message:" + System.currentTimeMillis());
                });
            });
    }

    private DebtorStatistics debtorFromDTO(Statistics statistics) {
        return new DebtorStatistics(
            statistics.getDebtorStatistics().getMostUsedLocation(),
            statistics.getDebtorStatistics().getMostValuableTransaction(),
            statistics.getDebtorStatistics().getLastTransactionExecutionTime(),
            statistics.getDebtorStatistics().getPeriodicStatistics().stream()
                .map(this::personalPeriodFromDTO)
                .collect(Collectors.toList())
        );
    }

    private PersonalPeriodStatistics personalPeriodFromDTO(PersonalPeriod stats) {
        return new PersonalPeriodStatistics(Days.days(stats.getPeriodLength()), stats.getExpensesSum(), stats.getTransactionCount());
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
    private List<MeanPeriodStatistics<Float>> distanceStatsFromDTO(Statistics statistics) {
        return statistics.getPublicStatistics()
                            .getDistance()
                            .stream()
                            .map(sumStatistics -> MeanPeriodStatistics.<Float>builder(Days.days(sumStatistics.getPeriodLength()))
                                .pastValues(sumStatistics.getAverage(), sumStatistics.getDeviationAverage())
                                .build()
                            )
                            .collect(Collectors.toList());
    }

    private List<MeanPeriodStatistics<Seconds>> timeStatsFromDTO(Statistics statistics) {
        return statistics.getPublicStatistics()
                            .getTime()
                            .stream()
                            .map(sumStatistics -> MeanPeriodStatistics.<Seconds>builder(Days.days(sumStatistics.getPeriodLength()))
                                .expectedValues(Seconds.seconds(sumStatistics.getExpected()), Seconds.seconds(sumStatistics.getDeviationExpected()))
                                .pastValues(Seconds.seconds(sumStatistics.getAverage()), Seconds.seconds(sumStatistics.getDeviationAverage()))
                                .build()
                            )
                            .collect(Collectors.toList());
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
                            .collect(Collectors.toList());
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
                            .collect(Collectors.toList());
    }
}
