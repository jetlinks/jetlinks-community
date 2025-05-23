/*
 * Copyright 2025 JetLinks https://www.jetlinks.cn
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jetlinks.community.configuration;

import com.alibaba.fastjson.JSON;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.Generated;
import org.apache.commons.beanutils.BeanUtilsBean;
import org.apache.commons.beanutils.Converter;
import org.hswebframework.ezorm.rdb.mapping.ReactiveRepository;
import org.hswebframework.web.bean.FastBeanCopier;
import org.hswebframework.web.cache.ReactiveCacheManager;
import org.hswebframework.web.dict.EnumDict;
import org.hswebframework.web.dict.defaults.DefaultItemDefine;
import org.jetlinks.community.Interval;
import org.jetlinks.community.JvmErrorException;
import org.jetlinks.community.command.CommandSupportManagerProvider;
import org.jetlinks.community.command.CommandSupportManagerProviders;
import org.jetlinks.community.command.register.CommandServiceEndpointRegister;
import org.jetlinks.community.config.ConfigManager;
import org.jetlinks.community.config.ConfigScopeCustomizer;
import org.jetlinks.community.config.ConfigScopeProperties;
import org.jetlinks.community.config.SimpleConfigManager;
import org.jetlinks.community.config.entity.ConfigEntity;
import org.jetlinks.community.dictionary.DictionaryJsonDeserializer;
import org.jetlinks.community.form.type.FieldTypeProvider;
import org.jetlinks.community.reactorql.aggregation.InternalAggregationSupports;
import org.jetlinks.community.reactorql.function.InternalFunctionSupport;
import org.jetlinks.community.reactorql.term.TermTypeSupport;
import org.jetlinks.community.reactorql.term.TermTypes;
import org.jetlinks.community.reference.DataReferenceManager;
import org.jetlinks.community.reference.DataReferenceProvider;
import org.jetlinks.community.reference.DefaultDataReferenceManager;
import org.jetlinks.community.resource.DefaultResourceManager;
import org.jetlinks.community.resource.ResourceManager;
import org.jetlinks.community.resource.ResourceProvider;
import org.jetlinks.community.resource.TypeScriptDeclareResourceProvider;
import org.jetlinks.community.resource.initialize.PermissionResourceProvider;
import org.jetlinks.community.service.DefaultUserBindService;
import org.jetlinks.community.utils.TimeUtils;
import org.jetlinks.core.metadata.DataType;
import org.jetlinks.core.metadata.types.DataTypes;
import org.jetlinks.reactor.ql.feature.Feature;
import org.jetlinks.reactor.ql.supports.DefaultReactorQLMetadata;
import org.jetlinks.reactor.ql.utils.CastUtils;
import org.jetlinks.supports.official.JetLinksDataTypeCodecs;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.ReactiveRedisOperations;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.util.unit.DataSize;
import reactor.core.Exceptions;
import reactor.core.publisher.Hooks;

import javax.annotation.Nonnull;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Map;

@AutoConfiguration
@SuppressWarnings("all")
@EnableConfigurationProperties({ConfigScopeProperties.class})
public class CommonConfiguration {

    static {
        InternalAggregationSupports.register();
        InternalFunctionSupport.register();

        BeanUtilsBean.getInstance().getConvertUtils().register(new Converter() {
            @Override
            public <T> T convert(Class<T> aClass, Object o) {
                if (o instanceof String) {
                    o = ((String) o).getBytes();
                }
                if (o instanceof byte[]) {
                    o = Unpooled.wrappedBuffer(((byte[]) o));
                }
                if (o instanceof ByteBuf) {
                    return (T) o;
                }
                return convert(aClass, JSON.toJSONBytes(o));
            }
        }, ByteBuf.class);

        BeanUtilsBean.getInstance().getConvertUtils().register(new Converter() {
            @Override
            public <T> T convert(Class<T> aClass, Object o) {
                return (T) MediaType.valueOf(String.valueOf(o));
            }
        }, MediaType.class);

        BeanUtilsBean.getInstance().getConvertUtils().register(new Converter() {
            @Override
            public <T> T convert(Class<T> type, Object value) {
                return (T)DataSize.parse(String.valueOf(value));
            }
        }, DataSize.class);

        BeanUtilsBean.getInstance().getConvertUtils().register(new Converter() {
            @Override
            public <T> T convert(Class<T> type, Object value) {
                return (T) TimeUtils.parse(String.valueOf(value));
            }
        }, Duration.class);

        BeanUtilsBean.getInstance().getConvertUtils().register(new Converter() {
            @Override
            public <T> T convert(Class<T> type, Object value) {
                return (T) Interval.of(String.valueOf(value));
            }
        }, Interval.class);

        BeanUtilsBean.getInstance().getConvertUtils().register(new Converter() {
            @Override
            public <T> T convert(Class<T> type, Object value) {
                return (T) TimeUtils.parseUnit(String.valueOf(value));
            }
        }, ChronoUnit.class);

        BeanUtilsBean.getInstance().getConvertUtils().register(new Converter() {
            @Override
            public <T> T convert(Class<T> type, Object value) {

                return (T)((Long)CastUtils.castNumber(value).longValue());
            }
        }, long.class);

        BeanUtilsBean.getInstance().getConvertUtils().register(new Converter() {
            @Override
            public <T> T convert(Class<T> type, Object value) {

                return (T)((Long) CastUtils.castNumber(value).longValue());
            }
        }, Long.class);

        BeanUtilsBean.getInstance().getConvertUtils().register(new Converter() {
            @Override
            @Generated
            public <T> T convert(Class<T> type, Object value) {

                if (value instanceof String) {
                    return (T) DefaultItemDefine.builder()
                                                .value(String.valueOf(value))
                                                .build();
                }

                return (T) FastBeanCopier.copy(value, new DefaultItemDefine());

            }
        }, EnumDict.class);

        BeanUtilsBean.getInstance().getConvertUtils().register(new Converter() {
            @Override
            @Generated
            public <T> T convert(Class<T> type, Object value) {
                if (value instanceof Map) {
                    Map<String, Object> map = ((Map) value);
                    String typeId = (String) map.get("type");
                    if (StringUtils.isEmpty(typeId)) {
                        return null;
                    }
                    return (T) JetLinksDataTypeCodecs.decode(DataTypes.lookup(typeId).get(), map);
                }
                return null;

            }
        }, DataType.class);

        //捕获jvm错误,防止Flux被挂起
        Hooks.onOperatorError((err, val) -> {
            if (Exceptions.isJvmFatal(err)) {
                return new JvmErrorException(err);
            }
            return err;
        });
        Hooks.onNextError((err, val) -> {
            if (Exceptions.isJvmFatal(err)) {
                return new JvmErrorException(err);
            }
            return err;
        });
    }

    @Bean
    public ApplicationContextAware staticBeanRegister() {

        return ctx -> {
            ctx.getBeanProvider(Feature.class)
               .forEach(DefaultReactorQLMetadata::addGlobal);

            ctx.getBeanProvider(CommandSupportManagerProvider.class)
               .forEach(CommandSupportManagerProviders::register);

            ctx.getBeanProvider(TermTypeSupport.class)
               .forEach(TermTypes::register);

            ctx.getBeanProvider(FieldTypeProvider.class)
               .forEach(provider -> FieldTypeProvider.supports.register(provider.getProvider(), provider));
        };
    }

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jackson2ObjectMapperBuilderCustomizer(){
        return builder->{
            builder.deserializerByType(DataType.class, new DataTypeJSONDeserializer());
            builder.deserializerByType(Date.class,new SmartDateDeserializer());
            builder.deserializerByType(EnumDict.class, new DictionaryJsonDeserializer());
        };
    }

    @Bean
    public ConfigManager configManager(ObjectProvider<ConfigScopeCustomizer> configScopeCustomizers,
                                       ReactiveRepository<ConfigEntity, String> repository,
                                       ReactiveCacheManager cacheManager) {

        SimpleConfigManager configManager = new SimpleConfigManager(repository,cacheManager);
        for (ConfigScopeCustomizer customizer : configScopeCustomizers) {
            customizer.custom(configManager);
        }
        return configManager;
    }

    @Bean
    public PermissionResourceProvider permissionResourceProvider(){
        return new PermissionResourceProvider();
    }

    @Bean
    public TypeScriptDeclareResourceProvider typeScriptDeclareResourceProvider() {
        return new TypeScriptDeclareResourceProvider();
    }

    @Bean
    public ResourceManager resourceManager(ObjectProvider<ResourceProvider> providers) {
        DefaultResourceManager manager = new DefaultResourceManager();
        providers.forEach(manager::addProvider);
        return manager;
    }

    @Bean
    public DataReferenceManager dataReferenceManager(ObjectProvider<DataReferenceProvider> provider) {
        DefaultDataReferenceManager referenceManager = new DefaultDataReferenceManager();

        provider.forEach(referenceManager::addStrategy);

        return referenceManager;
    }

    @Bean
    public CommandServiceEndpointRegister commandServiceEndpointRegister() {
        return new CommandServiceEndpointRegister();
    }


    @Configuration
    @ConditionalOnClass(ReactiveRedisOperations.class)
    static class DefaultUserBindServiceConfiguration {
        @Bean
        public DefaultUserBindService defaultUserBindService(ReactiveRedisOperations<Object, Object> redis) {
            return new DefaultUserBindService(redis);
        }
    }
}
