package org.ignas.frauddetection.transactionstatistic;

import com.google.common.collect.ImmutableList;
import io.vertx.core.AbstractVerticle;
import org.ignas.frauddetection.shared.ImmutableObjectCodec;
import org.ignas.frauddetection.shared.Location;
import org.ignas.frauddetection.transactionstatistic.api.request.StatisticsRequest;
import org.ignas.frauddetection.transactionstatistic.api.response.*;
import org.ignas.frauddetection.transactionstatistic.api.response.generalindicators.CountStatistics;
import org.ignas.frauddetection.transactionstatistic.api.response.generalindicators.DistanceDifferenceStatistics;
import org.ignas.frauddetection.transactionstatistic.api.response.generalindicators.SumStatistics;
import org.ignas.frauddetection.transactionstatistic.api.response.generalindicators.TimeDifferenceStatistics;
import org.joda.time.Days;
import org.joda.time.LocalDateTime;
import org.joda.time.Seconds;

import java.math.BigDecimal;

public class TransactionStatisticArchive extends AbstractVerticle {

    // TODO: delete after calculations implemented
    private static final Statistics TEMPORARY_HARDCODED_RESULT = new Statistics(
        new DebtorTransactionStatistics(
            BigDecimal.TEN,
            ImmutableList.of(
                new PersonalPeriodStatistics(
                    Days.SEVEN,
                    BigDecimal.valueOf(1),
                    BigDecimal.valueOf(2),
                    BigDecimal.valueOf(3),
                    LocalDateTime.now().minusHours(1),
                    new Location("54.25123", "25.45211"))
            ),
            ImmutableList.of(
                new GeneralPeriodStatistics(
                    Days.SEVEN,
                    new SumStatistics(
                        BigDecimal.valueOf(10),
                        BigDecimal.valueOf(5),
                        BigDecimal.valueOf(11),
                        BigDecimal.valueOf(6)
                    ),
                    new CountStatistics(1, 2, 3, 4),
                    new TimeDifferenceStatistics(Seconds.ONE, Seconds.TWO, Seconds.TWO, Seconds.ONE),
                    new DistanceDifferenceStatistics(BigDecimal.TEN, BigDecimal.ONE)
                )
            )
        ),
        new CredibilityScore(123L, 125L),
        new CredibilityScore(123L, 125L),
        new CredibilityScore(123L, 125L)
    );

    @Override
    public void start() {
        vertx.eventBus().registerDefaultCodec(StatisticsRequest.class, new ImmutableObjectCodec<>(StatisticsRequest.class));
        vertx.eventBus().registerDefaultCodec(Statistics.class, new ImmutableObjectCodec<>(Statistics.class));

        vertx.eventBus().consumer("transaction-statistic.archive")
            .handler(message -> {
                System.out.println("Archive: " + message);
                System.out.println("Returning fake response!");
                message.reply(TEMPORARY_HARDCODED_RESULT);
            });
    }
}
