package org.ignas.frauddetection;

import com.google.common.collect.ImmutableList;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Verticle;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.impl.launcher.VertxLifecycleHooks;
import io.vertx.core.json.JsonObject;
import org.ignas.frauddetection.httpapi.EvaluationRequestController;
import org.ignas.frauddetection.transactionmapping.FraudCriteriaResolver;
import org.ignas.frauddetection.transactionstatistic.TransactionStatisticArchive;

/**
 * Custom fraud detection cluster launcher
 *
 * TODO: Should refactor vertice adrresses to some external routing mechanism this way decoupling services.
 *
 */
public class DetectionLauncher implements VertxLifecycleHooks {

    public static void main(String... args) {
        Vertx vertx = Vertx.vertx();

        ImmutableList<Verticle> deploymentConfig = ImmutableList.<Verticle>builder()
            .add(new EvaluationRequestController())
            .add(new FraudCriteriaResolver())
            .add(new TransactionStatisticArchive())
            .build();

        deploymentConfig.forEach(vertx::deployVerticle);
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
