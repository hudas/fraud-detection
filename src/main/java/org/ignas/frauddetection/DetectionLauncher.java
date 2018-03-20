package org.ignas.frauddetection;

import com.google.common.collect.ImmutableList;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Verticle;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.impl.launcher.VertxLifecycleHooks;
import io.vertx.core.json.JsonObject;
import org.ignas.frauddetection.httpapi.EvaluationRequestController;
import org.ignas.frauddetection.probabilitystatistics.ProbabilityStatisticArchive;
import org.ignas.frauddetection.resultsanalyser.ResultsAnalyser;
import org.ignas.frauddetection.transactionevaluation.EvaluationController;
import org.ignas.frauddetection.transactionstatistics.TransactionStatisticArchive;

/**
 * Custom fraud detection cluster launcher
 *
 * TODO: Should refactor vertice addresses to some external routing mechanism this way decoupling services.
 *
 */
public class DetectionLauncher implements VertxLifecycleHooks {

    public static void main(String... args) {
        Vertx vertx = Vertx.vertx();

        ImmutableList<Verticle> standardVertices = ImmutableList.<Verticle>builder()
            .add(new EvaluationRequestController())
            .build();

        standardVertices.forEach(vertx::deployVerticle);

        DeploymentOptions workerDeployment = new DeploymentOptions()
            .setWorker(true);

        ImmutableList<Verticle> workers = ImmutableList.<Verticle>builder()
            .add(new EvaluationController())
            .add(new TransactionStatisticArchive())
            .add(new ProbabilityStatisticArchive())
            .add(new ResultsAnalyser())
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
