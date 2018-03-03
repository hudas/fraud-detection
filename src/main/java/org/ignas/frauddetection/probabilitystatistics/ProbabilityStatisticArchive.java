package org.ignas.frauddetection.probabilitystatistics;


import com.google.common.collect.ImmutableMap;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.MessageConsumer;
import org.ignas.frauddetection.probabilitystatistics.api.request.CriteriaGroupProbabilityRequest;
import org.ignas.frauddetection.probabilitystatistics.api.request.CriteriaProbabilityRequest;
import org.ignas.frauddetection.probabilitystatistics.api.response.BayesTable;
import org.ignas.frauddetection.probabilitystatistics.api.response.CriteriaGroupRisk;
import org.ignas.frauddetection.probabilitystatistics.api.response.ProbabilityStatistics;
import org.ignas.frauddetection.shared.ImmutableObjectCodec;
import org.ignas.frauddetection.transactionstatistics.api.request.StatisticsRequest;

import java.util.Map;

public class ProbabilityStatisticArchive extends AbstractVerticle {

    private Map<String, Map<String, Float>> groupValues = ImmutableMap.<String, Map<String, Float>>builder()
        .put("AMOUNT", ImmutableMap.of("VERY_LOW_RISK", 1f, "LOW_RISK", 1f, "EXPECTED_RISK", 1f, "HIGH_RISK", 1f, "VERY_HIGH_RISK", 1f))
        .put("COUNT", ImmutableMap.of("VERY_LOW_RISK", 1f, "LOW_RISK", 1f, "EXPECTED_RISK", 1f, "HIGH_RISK", 1f, "VERY_HIGH_RISK", 1f))
        .put("TIME", ImmutableMap.of("VERY_LOW_RISK", 1f, "LOW_RISK", 1f, "EXPECTED_RISK", 1f, "HIGH_RISK", 1f, "VERY_HIGH_RISK", 1f))
        .put("LOCATION", ImmutableMap.of("VERY_LOW_RISK", 1f, "LOW_RISK", 1f, "EXPECTED_RISK", 1f, "HIGH_RISK", 1f, "VERY_HIGH_RISK", 1f))
        .build();


    @Override
    public void start() {
        vertx.eventBus().registerDefaultCodec(
            BayesTable.class,
            new ImmutableObjectCodec<BayesTable>(BayesTable.class)
        );

        vertx.eventBus().registerDefaultCodec(
            ProbabilityStatistics.class,
            new ImmutableObjectCodec<ProbabilityStatistics>(ProbabilityStatistics.class)
        );

        vertx.eventBus()
            .consumer("probability-statistic.archive.criteria")
            .handler(message -> {
                if (!(message.body() instanceof CriteriaProbabilityRequest)) {
                    message.fail(101, "Wrong type");
                    return;
                }

                CriteriaProbabilityRequest request = (CriteriaProbabilityRequest) message.body();

                ImmutableMap.Builder builder = ImmutableMap.<String, Float>builder();

                request.getCriteriaValues().keySet().forEach(key -> builder.put(key, 0.5f));

                message.reply(new ProbabilityStatistics(
                    0.5f,
                    builder.build(),
                    ImmutableMap.<String, CriteriaGroupRisk>builder()
                        .put("AMOUNT", new CriteriaGroupRisk(0.7f, 0.1f))
                        .put("COUNT", new CriteriaGroupRisk(0.7f, 0.1f))
                        .put("TIME", new CriteriaGroupRisk(0.7f, 0.1f))
                        .put("LOCATION", new CriteriaGroupRisk(0.7f, 0.1f))
                        .build()
                ));
            });

        vertx.eventBus()
            .consumer("probability-statistic.archive.criteria-group")
            .handler(message -> {
                if (!(message.body() instanceof CriteriaGroupProbabilityRequest)) {
                    message.fail(101, "Wrong type");
                    return;
                }

                message.reply(new BayesTable(groupValues));
            });
    }
}
