package org.ignas.frauddetection.transactionevaluation.integration;

import io.vertx.core.Future;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.EventBus;
import org.ignas.frauddetection.shared.ServiceIntegration;
import org.ignas.frauddetection.transactionevaluation.domain.Transaction;
import org.ignas.frauddetection.transactionevaluation.domain.stats.HistoricalData;
import org.ignas.frauddetection.transactionstatistics.api.response.Statistics;

import static org.ignas.frauddetection.transactionevaluation.integration.converters.StatisticsAPIConverter.mapRequest;
import static org.ignas.frauddetection.transactionevaluation.integration.converters.StatisticsAPIConverter.mapResponse;

/**
 * At this point it becomes clear that All loaders can be replaced with single good generics based abstraction which receives address and required mappers.
 * TODO: Refactor further
 */
public class TransactionStatisticsLoader implements ServiceIntegration<Transaction, HistoricalData>{

    private EventBus bus;

    public TransactionStatisticsLoader(EventBus bus) {
        this.bus = bus;
    }

    @Override
    public Future<HistoricalData> load(Transaction request) {
        Future<HistoricalData> loader = Future.future();

        bus.send("transaction-statistic.archive", mapRequest(request), statisticsResponse -> {

            if (statisticsResponse.failed()) {
                System.out.println("ProbabilityCalculatorIntegration" + statisticsResponse.cause().getMessage());
                statisticsResponse.cause().printStackTrace();
                loader.fail(statisticsResponse.cause());
            }


            if (!(statisticsResponse.result().body() instanceof Statistics)) {
                loader.fail("Unsupported message type: " + statisticsResponse.result().getClass());
                return;
            }

            loader.complete(mapResponse((Statistics) statisticsResponse.result().body()));
        });

        return loader;
    }
}
