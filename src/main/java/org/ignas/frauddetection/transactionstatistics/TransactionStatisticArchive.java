package org.ignas.frauddetection.transactionstatistics;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.EventBus;
import org.ignas.frauddetection.shared.ImmutableObjectCodec;
import org.ignas.frauddetection.shared.Location;
import org.ignas.frauddetection.transactionstatistics.api.request.StatisticsRequest;
import org.ignas.frauddetection.transactionstatistics.api.response.*;
import org.ignas.frauddetection.transactionstatistics.api.response.generalindicators.*;
import org.joda.time.LocalDateTime;

public class TransactionStatisticArchive extends AbstractVerticle {

    // TODO: delete after calculations implemented
    private static final Statistics TEMPORARY_HARDCODED_RESULT = new Statistics(
        new DebtorTransactionStatistics(
            new Location(54.25123f, 25.45211f),
            new Location(54.25123f, 25.45211f),
            10f,
            LocalDateTime.now().minusHours(1),
            3600,
            ImmutableList.of(
                new PersonalPeriod(1,1f,2),
                new PersonalPeriod(7,110f,4),
                new PersonalPeriod(30,550f,22)
            )
        ),
        new CredibilityScore(123f, 125f, 12f),
        new CredibilityScore(123f, 125f, 12f),
        new CredibilityScore(123f, 125f, 12f),
        new PublicStatistics(
            Lists.newArrayList(
                new SumStatistics(1, 10f,10f, 11f, 6f),
                new SumStatistics(7, 50f,20f, 55f, 11f),
                new SumStatistics(30, 500f,100f, 505f, 120f)
            ),
            Lists.newArrayList(
                new RatioStatistics(1, 10f,1f),
                new RatioStatistics(7, 20f,5f),
                new RatioStatistics(30, 50f,10f)
            ),
            Lists.newArrayList(
                new CountStatistics(1, 1, 0, 2, 1),
                new CountStatistics(7, 3, 0, 5, 1),
                new CountStatistics(30, 20, 3, 22, 3)
            ),
            Lists.newArrayList(
                new TimeDifferenceStatistics(7, 1, 2, 3, 4)
            ),
            Lists.newArrayList(
                new DistanceDifferenceStatistics(7, 10f, 15f)
            ),
            Lists.newArrayList(
                new DistanceDifferenceStatistics(7, 10f, 15f)
            )
        )
    );

    @Override
    public void start() {
        EventBus bus = vertx.eventBus();

        bus.registerDefaultCodec(StatisticsRequest.class, new ImmutableObjectCodec<>(StatisticsRequest.class));
        bus.registerDefaultCodec(Statistics.class, new ImmutableObjectCodec<>(Statistics.class));

        bus.consumer("transaction-statistic.archive")
            .handler(message -> message.reply(TEMPORARY_HARDCODED_RESULT));
    }
}
