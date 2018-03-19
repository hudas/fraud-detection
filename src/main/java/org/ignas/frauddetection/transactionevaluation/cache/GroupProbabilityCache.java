package org.ignas.frauddetection.transactionevaluation.cache;

import io.vertx.core.Future;
import org.ignas.frauddetection.shared.FraudCriteriaGroup;
import org.ignas.frauddetection.shared.ServiceIntegration;
import org.ignas.frauddetection.transactionevaluation.domain.CriteriaGroup;

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

    private Map<String, CriteriaGroup> CACHED_VALUES;

    private ServiceIntegration<List<String>, Map<String, CriteriaGroup>> loader;

    public GroupProbabilityCache(ServiceIntegration<List<String>, Map<String, CriteriaGroup>> loader) {
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

    public Float getProbability(String group, String eventValue) {
        if (CACHED_VALUES == null) {
            throw new IllegalStateException("Not cached yet");
        }

        return CACHED_VALUES.get(group).eventProbability(eventValue);
    }

    private List<String> getCriteriaGroupNames() {
        return Arrays.stream(FraudCriteriaGroup.values())
            .map(Enum::name)
            .collect(Collectors.toList());
    }
}
