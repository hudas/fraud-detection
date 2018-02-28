package org.ignas.frauddetection.transactionstatistic;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
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
            new Location("54.25123", "25.45211"),
            BigDecimal.TEN,
            LocalDateTime.now().minusHours(1),
            ImmutableList.of(
                new PersonalPeriod(
                    7,
                    BigDecimal.valueOf(1),
                    2
                )
            )
        ),
        new CredibilityScore(123f, 125f, 12f),
        new CredibilityScore(123f, 125f, 12f),
        new CredibilityScore(123f, 125f, 12f),
        new PublicStatistics(
            Lists.newArrayList(new SumStatistics(7, 10f,5f, 11f, 6f)),
            Lists.newArrayList(new CountStatistics(7, 1, 2, 3, 4)),
            Lists.newArrayList(new TimeDifferenceStatistics(7, 1, 2, 3, 4)),
            Lists.newArrayList(new DistanceDifferenceStatistics(7, 10f, 15f))
        )
    );

    @Override
    public void start() {
        vertx.eventBus().registerDefaultCodec(StatisticsRequest.class, new ImmutableObjectCodec<>(StatisticsRequest.class));
        vertx.eventBus().registerDefaultCodec(Statistics.class, new ImmutableObjectCodec<>(Statistics.class));

        vertx.eventBus().consumer("transaction-statistic.archive")
            .handler(message -> {
                message.reply(TEMPORARY_HARDCODED_RESULT);
            });
    }
}
