package org.jetlinks.community.elastic.search.manager;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * @author bsetfeng
 * @since 1.0
 **/
@Component
public class StandardsIndexManagerCenter implements StandardsIndexManager, BeanPostProcessor, CommandLineRunner {


    Map<String, IndexManager> managerMap = new ConcurrentHashMap<>();

    Map<String, IndexPatternManager> patternManagerMap = new ConcurrentHashMap<>();

    Map<String, IndexStrategyManager> strategyManagerMap = new ConcurrentHashMap<>();

    Set<String> standardsIndices = new ConcurrentSkipListSet<>();

    private final List<IndexStrategyProvider> indexStrategyProviders;

    public StandardsIndexManagerCenter(List<IndexStrategyProvider> indexStrategyProviders) {
        this.indexStrategyProviders = indexStrategyProviders;
    }

    public void registerIndexPatternManager(IndexPatternManager patternManager) {
        patternManagerMap.put(patternManager.getName(), patternManager);
    }

    public void registerIndexStrategyManager(IndexStrategyManager strategyManager) {
        strategyManagerMap.put(strategyManager.getName(), strategyManager);
    }

    @Override
    public void registerIndexManager(String index, IndexManager indexManager) {
        managerMap.put(index, indexManager);
    }


    @Override
    public String getStandardsIndex(String index) {
        return Optional.ofNullable(managerMap.get(index))
            .map(m -> {
                String standardsIndex = m.getStandardIndex(index);
                standardsIndices.add(standardsIndex);
                return standardsIndex;
            })
            .orElse(index);
    }

    @Override
    public boolean indexIsChange(String index) {
        return managerMap.containsKey(index);
    }

    @Override
    public boolean indexIsUpdate(String index) {
        return Optional.ofNullable(managerMap.get(index))
            .map(m -> !standardsIndices.contains(m.getStandardIndex(index)))
            .orElse(false);
    }


    @Override
    public boolean standardsIndexIsUpdate(String standardsIndex) {
        return !standardsIndices.contains(standardsIndex);
    }

    @Override
    public void addStandardsIndex(String standardsIndex) {
        standardsIndices.add(standardsIndex);
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof IndexPatternManager) {
            registerIndexPatternManager((IndexPatternManager) bean);
        }
        if (bean instanceof IndexStrategyManager) {
            registerIndexStrategyManager((IndexStrategyManager) bean);
        }
        return bean;
    }


    @Override
    public void run(String... args) throws Exception {
        indexStrategyProviders.forEach(provider -> {
            provider.getIndexStrategies()
                .forEach((k, v) -> managerMap.put(k, new IndexManager() {
                    @Override
                    public IndexPatternManager getIndexPatternManager() {
                        return patternManagerMap.get(v.getPatternName());
                    }

                    @Override
                    public IndexStrategyManager getIndexStrategyManager() {
                        return strategyManagerMap.get(v.getStrategyName());
                    }

                    @Override
                    public String getFormat() {
                        return provider.getFormat();
                    }

                    @Override
                    public String getConnector() {
                        return provider.connector();
                    }
                }));
        });
    }
}
