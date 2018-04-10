package org.ignas.frauddetection.transactionstatistics;

import com.google.common.collect.Lists;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.eventbus.EventBus;
import org.ignas.frauddetection.probabilitystatistics.domain.BatchToProcess;
import org.ignas.frauddetection.probabilitystatistics.service.repositories.GeneralProbabilitiesStorage;
import org.ignas.frauddetection.shared.Location;
import org.ignas.frauddetection.transactionstatistics.domain.ConditionOccurrences;
import org.ignas.frauddetection.transactionstatistics.repositories.ConditionStorage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static com.google.common.collect.Lists.newArrayList;
import static org.ignas.frauddetection.transactionstatistics.domain.LocationService.toNearestArea;

public class ExternalConditionUpdater extends AbstractVerticle {

    private ConditionStorage conditionStorage;

    public ExternalConditionUpdater() {
        conditionStorage = new ConditionStorage(
            "mongodb://localhost",
            "transactions"
        );
    }

    @Override
    public void start(Future<Void> startFuture) {
        EventBus bus = vertx.eventBus();

        bus.consumer("transaction-processing.public-periodic-data-updated", (batchEvent) -> {
            if (!(batchEvent.body() instanceof BatchToProcess)) {
                throw new IllegalArgumentException("Unsupported type: " + batchEvent.body().getClass());
            }

            BatchToProcess batch = (BatchToProcess) batchEvent.body();

            Map<String, ConditionOccurrences<String>> creditorOccurrences = new HashMap<>();

            batch.getItems().forEach(item -> {
                ConditionOccurrences<String> creditor = creditorOccurrences.computeIfAbsent(
                    item.getTransaction().getCreditorId(), ConditionOccurrences::empty);

                int nonFraudIncrement = !item.isAlreadyProcessedTransaction() ? 1 : 0;
                int fraudIncrement = item.isFraudulent() ? 1 : 0;

                creditor.increaseOccurrences(nonFraudIncrement, fraudIncrement);
            });

            Map<Integer, ConditionOccurrences<Integer>> timeOccurrences = new HashMap<>();

            batch.getItems().forEach(item -> {
                ConditionOccurrences<Integer> time = timeOccurrences.computeIfAbsent(
                    item.getTransaction().getTime().getHourOfDay(), ConditionOccurrences::empty);

                int nonFraudIncrement = !item.isAlreadyProcessedTransaction() ? 1 : 0;
                int fraudIncrement = item.isFraudulent() ? 1 : 0;

                time.increaseOccurrences(nonFraudIncrement, fraudIncrement);
            });

            Map<Location, ConditionOccurrences<Location>> locationOccurrences = new HashMap<>();

            batch.getItems().forEach(item -> {
                ConditionOccurrences<Location> creditor = locationOccurrences.<Location>computeIfAbsent(
                    toNearestArea(item.getTransaction().getLocation()), ConditionOccurrences::<Location>empty);

                int nonFraudIncrement = !item.isAlreadyProcessedTransaction() ? 1 : 0;
                int fraudIncrement = item.isFraudulent() ? 1 : 0;

                creditor.increaseOccurrences(nonFraudIncrement, fraudIncrement);
            });

            conditionStorage.updateOccurrences(
                new ArrayList<ConditionOccurrences<String>>(creditorOccurrences.values()),
                new ArrayList<ConditionOccurrences<Integer>>(timeOccurrences.values()),
                new ArrayList<ConditionOccurrences<Location>>(locationOccurrences.values())
            );

            // Resend same event without any modifications
            bus.publish("transaction-processing.conditions-updated", batch);
        });
    }
}
