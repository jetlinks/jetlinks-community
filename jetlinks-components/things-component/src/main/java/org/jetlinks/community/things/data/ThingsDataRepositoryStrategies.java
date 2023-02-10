package org.jetlinks.community.things.data;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ThingsDataRepositoryStrategies {

    private final static Map<String, ThingsDataRepositoryStrategy> strategyMap = new ConcurrentHashMap<>();

    static void register(ThingsDataRepositoryStrategy strategy) {
        strategyMap.put(strategy.getId(), strategy);
    }


    public static List<ThingsDataRepositoryStrategy> getAll() {
        List<ThingsDataRepositoryStrategy> strategies = new ArrayList<>(strategyMap.values());
        strategies.sort(Comparator.comparingLong(ThingsDataRepositoryStrategy::getOrder));
        return strategies;
    }

    public static Optional<ThingsDataRepositoryStrategy> getStrategy(String id) {
        return Optional.ofNullable(strategyMap.get(id));
    }
}
