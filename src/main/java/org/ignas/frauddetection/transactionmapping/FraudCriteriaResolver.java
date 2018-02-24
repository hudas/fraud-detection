package org.ignas.frauddetection.transactionmapping;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import org.ignas.frauddetection.transactionstatistic.api.request.StatisticsRequest;
import org.ignas.frauddetection.transactionstatistic.api.response.Statistics;

public class FraudCriteriaResolver extends AbstractVerticle {

    @Override
    public void start() throws Exception {
        EventBus bus = vertx.eventBus();

        bus.consumer("transaction-mapping.resolver")
            .handler(event -> {
                System.out.println("Resolver:" + event.body());

                JsonObject body = (JsonObject) event.body();

                StatisticsRequest request = new StatisticsRequest(
                    body.getString("debtorAccountId"),
                    body.getString("creditorAccountId"),
                    null,
                    null
                );

                bus.send("transaction-statistic.archive", request, (reply) -> {
                    System.out.println("Reply Message:" + reply.result().body());
                });
            });
    }
}
