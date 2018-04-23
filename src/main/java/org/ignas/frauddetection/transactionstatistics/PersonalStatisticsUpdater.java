package org.ignas.frauddetection.transactionstatistics;

import com.google.common.collect.Lists;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.eventbus.EventBus;
import org.ignas.frauddetection.DetectionLauncher;
import org.ignas.frauddetection.probabilitystatistics.domain.BatchToProcess;
import org.ignas.frauddetection.probabilitystatistics.domain.PersonalStats;
import org.ignas.frauddetection.probabilitystatistics.service.repositories.GeneralProbabilitiesStorage;
import org.ignas.frauddetection.probabilitystatistics.service.repositories.GroupStatisticsStorage;
import org.ignas.frauddetection.probabilitystatistics.service.repositories.PersonalStatisticsStorage;
import org.ignas.frauddetection.transactionevaluation.api.request.LearningRequest;

import java.util.HashMap;
import java.util.Map;

public class PersonalStatisticsUpdater extends AbstractVerticle {

    private PersonalStatisticsStorage personalStatistics;

    public PersonalStatisticsUpdater() {
        personalStatistics = new PersonalStatisticsStorage("transactions");
    }

    @Override
    public void start(Future<Void> startFuture) {
        EventBus bus = vertx.eventBus();

        bus.consumer("transaction-processing.batch-prepared", (batchPrepared) -> {
            if (!(batchPrepared.body() instanceof BatchToProcess)) {
                throw new IllegalArgumentException("Unsupported type: " + batchPrepared.body().getClass());
            }

            BatchToProcess batch = (BatchToProcess) batchPrepared.body();

            Map<String, PersonalStats> updates = new HashMap<>();

            // Batch transactions for single debtor
            for (LearningRequest request : batch.getItems()) {
                if (request.isAlreadyProcessedTransaction()) {
                    continue;
                }

                updates.computeIfAbsent(
                    request.getTransaction().getDebtorId(),
                    PersonalStats::new
                ).updateFrom(request);
            }

            if (!updates.isEmpty()) {
                personalStatistics.update(Lists.newArrayList(updates.values()));
            }

            bus.publish("transaction-processing.personal-data-updated", batch);
        });
    }

    public void stop() {
        personalStatistics.close();
    }
}
