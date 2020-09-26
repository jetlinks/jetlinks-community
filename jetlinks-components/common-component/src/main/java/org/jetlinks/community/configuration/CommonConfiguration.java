package org.jetlinks.community.configuration;

import com.alibaba.fastjson.JSON;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.apache.commons.beanutils.BeanUtilsBean;
import org.apache.commons.beanutils.Converter;
import org.jetlinks.community.Interval;
import org.jetlinks.community.utils.TimeUtils;
import org.jetlinks.reactor.ql.feature.Feature;
import org.jetlinks.reactor.ql.supports.DefaultReactorQLMetadata;
import org.jetlinks.reactor.ql.utils.CastUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.util.unit.DataSize;

import javax.annotation.Nonnull;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Date;

@Configuration
@SuppressWarnings("all")
public class CommonConfiguration {

    static {
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
    }

    @Bean
    public BeanPostProcessor globalReactorQlFeatureRegister() {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(@Nonnull Object bean, @Nonnull String beanName) throws BeansException {
                if (bean instanceof Feature) {
                    DefaultReactorQLMetadata.addGlobal(((Feature) bean));
                }
                return bean;
            }
        };
    }

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jackson2ObjectMapperBuilderCustomizer(){
        return builder->{
            builder.deserializerByType(Date.class,new SmartDateDeserializer());
        };
    }

}
