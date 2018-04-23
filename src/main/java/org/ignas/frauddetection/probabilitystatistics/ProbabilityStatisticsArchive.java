package org.ignas.frauddetection.probabilitystatistics;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import org.ignas.frauddetection.DetectionLauncher;
import org.ignas.frauddetection.probabilitystatistics.api.response.BayesTable;
import org.ignas.frauddetection.probabilitystatistics.api.response.ProbabilityStatistics;
import org.ignas.frauddetection.probabilitystatistics.service.CriteriaProbabilitiesHandler;
import org.ignas.frauddetection.probabilitystatistics.service.GroupProbabilitiesHandler;
import org.ignas.frauddetection.probabilitystatistics.service.repositories.CriteriaStorage;
import org.ignas.frauddetection.probabilitystatistics.service.repositories.GeneralProbabilitiesStorage;
import org.ignas.frauddetection.probabilitystatistics.service.repositories.GroupStatisticsStorage;
import org.ignas.frauddetection.shared.ImmutableObjectCodec;


public class ProbabilityStatisticsArchive extends AbstractVerticle {

    private GroupStatisticsStorage groupStatisticsStorage;
    private GeneralProbabilitiesStorage generalProbabilitiesStorage;
    private CriteriaStorage criteriaStorage;
    private CriteriaStorage groupStorage;

    public ProbabilityStatisticsArchive() {
        groupStatisticsStorage = new GroupStatisticsStorage("bayes", "groupStatistics");

        generalProbabilitiesStorage = new GeneralProbabilitiesStorage("bayes", "generalProbabilities");

        criteriaStorage = new CriteriaStorage("bayes", "criteriaProbabilities");

        groupStorage = new CriteriaStorage("bayes", "criteriaGroupProbabilities");
    }

    @Override
    public void start(Future<Void> startup) {
        registerAPICodecs();

        vertx.eventBus()
            .consumer("probability-statistic.archive.criteria")
            .handler(
                new CriteriaProbabilitiesHandler(
                    criteriaStorage,
                    generalProbabilitiesStorage,
                    groupStatisticsStorage)
            );

        vertx.eventBus()
            .consumer("probability-statistic.archive.criteria-group")
            .handler(new GroupProbabilitiesHandler(groupStorage, generalProbabilitiesStorage));

        startup.complete();
    }


    @Override
    public void stop() {
        groupStorage.close();
        generalProbabilitiesStorage.close();
        groupStatisticsStorage.close();
        criteriaStorage.close();
    }


    private void registerAPICodecs() {
        vertx.eventBus().registerDefaultCodec(
            BayesTable.class,
            new ImmutableObjectCodec<>(BayesTable.class)
        );

        vertx.eventBus().registerDefaultCodec(
            ProbabilityStatistics.class,
            new ImmutableObjectCodec<>(ProbabilityStatistics.class)
        );
    }
}
