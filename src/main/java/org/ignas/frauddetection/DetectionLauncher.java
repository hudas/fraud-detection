package org.ignas.frauddetection;

import com.google.common.collect.ImmutableList;
import com.mongodb.ServerAddress;
import com.mongodb.async.client.MongoClientSettings;
import com.mongodb.connection.ClusterSettings;
import com.mongodb.connection.ConnectionPoolSettings;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Verticle;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.impl.launcher.VertxLifecycleHooks;
import io.vertx.core.json.JsonObject;
import jdk.internal.jline.internal.Log;
import org.ignas.frauddetection.httpapi.EvaluationRequestController;
import org.ignas.frauddetection.probabilitystatistics.*;
import org.ignas.frauddetection.processinglog.EvaluationArchive;
import org.ignas.frauddetection.resultsanalyser.ResultsAnalyser;
import org.ignas.frauddetection.transactionevaluation.TransactionMappingResolver;
import org.ignas.frauddetection.transactionstatistics.*;
import org.slf4j.event.Level;

import java.util.Arrays;
import java.util.logging.Logger;

/**
 * Custom fraud detection cluster launcher
 *
 * TODO: Should refactor vertice addresses to some external routing mechanism this way decoupling services.
 *
 */
public class DetectionLauncher implements VertxLifecycleHooks {

    public static final MongoClientSettings TRANSACTIONS_MONGODB_SETTINGS = MongoClientSettings.builder()
        .clusterSettings(ClusterSettings.builder()
                .hosts(Arrays.asList(new ServerAddress(DetectionLauncher.TRANSACTIONS_MONGODB_HOST)))
        .build())
        .connectionPoolSettings(ConnectionPoolSettings.builder()
                .minSize(500)
                .maxSize(2000)
                .maxWaitQueueSize(10000)
                .build())
        .build();


    public static final String TRANSACTIONS_MONGODB_HOST = "10.135.80.119:27017";


    public static final MongoClientSettings PERIODIC_TRANSACTIONS_DB_SETTINGS = MongoClientSettings.builder()
        .clusterSettings(ClusterSettings.builder()
            .hosts(Arrays.asList(new ServerAddress(DetectionLauncher.PERIODIC_TRANSACTIONS_DB_HOST)))
            .build())
        .connectionPoolSettings(ConnectionPoolSettings.builder()
            .minSize(500)
            .maxSize(2000)
            .maxWaitQueueSize(10000)
            .build())
        .build();


    public static final String PERIODIC_TRANSACTIONS_DB_HOST = "10.135.12.54:27017";
//    public static final String MONGODB_HOST = "localhost";

    public static final MongoClientSettings BAYES_MONGODB_SETTINGS = MongoClientSettings.builder()
        .clusterSettings(ClusterSettings.builder()
            .hosts(Arrays.asList(new ServerAddress(DetectionLauncher.BAYES_MONGODB_HOST)))
            .build())
        .connectionPoolSettings(ConnectionPoolSettings.builder()
            .minSize(500)
            .maxSize(2000)
            .maxWaitQueueSize(10000)
            .build())
        .build();


    public static final String BAYES_MONGODB_HOST = "10.135.79.66:27017";


    public static final long CACHE_TTL = 10000;

    public static void main(String... args) {
        Vertx vertx = Vertx.vertx();

        ImmutableList<Verticle> standardVertices = ImmutableList.<Verticle>builder()
            .add(new EvaluationRequestController())
            .build();

        standardVertices.forEach(vertx::deployVerticle);

        DeploymentOptions workerDeployment = new DeploymentOptions()
            .setWorker(true);

        ImmutableList<Verticle> workers = ImmutableList.<Verticle>builder()
            .add(new TransactionMappingResolver())
            .add(new TransactionStatisticArchive())
            .add(new ProbabilityStatisticsArchive())
            .add(new ResultsAnalyser())
            .add(new ProbabilityLearningBatcher())
            .add(new CriteriaUpdater())
            .add(new CriteriaGroupUpdater())
            .add(new GeneralDataUpdater())
            .add(new CriteriaStatisticsUpdater())
            .add(new EvaluationArchive())
            .add(new ExternalConditionUpdater())
            .add(new TransactionLearningBatcher())
            .add(new PublicStatisticsUpdater())
            .add(new PublicPeriodicStatisticsUpdater())
            .add(new PersonalStatisticsUpdater())

            .build();

        workers.forEach(it -> vertx.deployVerticle(it, workerDeployment));
    }

    @Override
    public void afterConfigParsed(JsonObject config) {

    }

    @Override
    public void beforeStartingVertx(VertxOptions options) {

    }

    @Override
    public void afterStartingVertx(Vertx vertx) {

    }

    @Override
    public void beforeDeployingVerticle(DeploymentOptions deploymentOptions) {

    }

    @Override
    public void beforeStoppingVertx(Vertx vertx) {

    }

    @Override
    public void afterStoppingVertx() {

    }

    @Override
    public void handleDeployFailed(Vertx vertx, String mainVerticle, DeploymentOptions deploymentOptions, Throwable cause) {

    }
}
