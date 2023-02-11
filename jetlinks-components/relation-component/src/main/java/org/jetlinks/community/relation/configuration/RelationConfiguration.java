package org.jetlinks.community.relation.configuration;

import org.hswebframework.ezorm.rdb.mapping.ReactiveRepository;
import org.jetlinks.community.relation.RelationManagerInitializer;
import org.jetlinks.community.relation.RelationObjectProvider;
import org.jetlinks.core.things.relation.RelationManager;
import org.jetlinks.community.relation.entity.RelatedEntity;
import org.jetlinks.community.relation.entity.RelationEntity;
import org.jetlinks.community.relation.impl.DefaultRelationManager;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@EnableConfigurationProperties(RelationProperties.class)
public class RelationConfiguration {

    @Bean
    @ConditionalOnMissingBean(RelationManager.class)
    public RelationManager relationManager(ReactiveRepository<RelatedEntity, String> relatedRepository,
                                           ReactiveRepository<RelationEntity, String> relationRepository,
                                           ObjectProvider<RelationObjectProvider> objectProvider) {
        DefaultRelationManager manager = new DefaultRelationManager(relatedRepository, relationRepository);
        objectProvider.forEach(manager::addProvider);
        return manager;
    }

    @Bean
    public RelationManagerInitializer relationManagerInitializer(RelationManager manager) {
        return new RelationManagerInitializer(manager);
    }

}
