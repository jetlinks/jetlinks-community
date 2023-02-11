package org.jetlinks.community.reactorql;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.context.index.CandidateComponentsIndex;
import org.springframework.context.index.CandidateComponentsIndexLoader;
import org.springframework.core.type.AnnotationMetadata;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
public class ReactorQLBeanDefinitionRegistrar implements ImportBeanDefinitionRegistrar {

    @Override
    @SneakyThrows
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, @Nonnull BeanDefinitionRegistry registry) {
        Map<String, Object> attr = importingClassMetadata.getAnnotationAttributes(EnableReactorQL.class.getName());
        if (attr == null) {
            return;
        }
        String[] packages = (String[]) attr.get("value");

        CandidateComponentsIndex index = CandidateComponentsIndexLoader.loadIndex(org.springframework.util.ClassUtils.getDefaultClassLoader());
        if (null == index) {
            return;
        }
        Set<String> path = Arrays.stream(packages)
                                 .flatMap(str -> index
                                     .getCandidateTypes(str, ReactorQLOperation.class.getName())
                                     .stream())
                                 .collect(Collectors.toSet());

        for (String className : path) {
            Class<?> type = org.springframework.util.ClassUtils.forName(className, null);
            if (!type.isInterface() || type.getAnnotation(ReactorQLOperation.class) == null) {
                continue;
            }
            RootBeanDefinition definition = new RootBeanDefinition();
            definition.setTargetType(type);
            definition.setBeanClass(ReactorQLFactoryBean.class);
            definition.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_BY_TYPE);
            definition.getPropertyValues().add("target", type);
            if (!registry.containsBeanDefinition(type.getName())) {
                log.debug("register ReactorQL Operator {}", type);
                registry.registerBeanDefinition(type.getName(), definition);
            }
        }

    }

}
