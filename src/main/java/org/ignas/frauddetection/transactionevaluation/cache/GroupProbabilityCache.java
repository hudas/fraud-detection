package org.ignas.frauddetection.transactionevaluation.cache;

import io.vertx.core.Future;
import org.ignas.frauddetection.probabilitystatistics.api.response.BayesTable;
import org.ignas.frauddetection.shared.FraudCriteriaGroup;
import org.ignas.frauddetection.shared.ServiceIntegration;
import org.ignas.frauddetection.transactionevaluation.domain.CriteriaGroup;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * The most popular cache implementations such as Guava, Ehcache etc does not support fully async API
 * Since we need only basic feature of the cache - async reloading this minimal implementation is provided.
 *
 */
public class GroupProbabilityCache {

    private BayesTable CACHED_VALUES;

    private ServiceIntegration<List<String>, BayesTable> loader;

    public GroupProbabilityCache(ServiceIntegration<List<String>, BayesTable> loader) {
        this.loader = loader;
    }

    /**
     * Will reset cache.
     *  If something goes wrong, will keep the same values cached silently
     *  Since we are not building general framework such handling is fine.
     */
    public Future reload() {
        Future future = Future.future();

        this.loader.load(getCriteriaGroupNames())
            .otherwise(this.CACHED_VALUES)
            .setHandler(result -> {
                this.CACHED_VALUES = result.result();
                future.complete();
            });

        return future;
    }

    public Float getOccurenceInNonFraud(String group, String eventValue) {
        if (CACHED_VALUES == null) {
            throw new IllegalStateException("Not cached yet");
        }

        return CACHED_VALUES.getNonFraudProbabilities().get(group).get(eventValue);
    }

    public Float getOccurenceInFraud(String group, String eventValue) {
        if (CACHED_VALUES == null) {
            throw new IllegalStateException("Not cached yet");
        }

        return CACHED_VALUES.getFraudProbabilities().get(group).get(eventValue);
    }

    private List<String> getCriteriaGroupNames() {
        return Arrays.stream(FraudCriteriaGroup.values())
            .map(Enum::name)
            .collect(Collectors.toList());
    }
}
