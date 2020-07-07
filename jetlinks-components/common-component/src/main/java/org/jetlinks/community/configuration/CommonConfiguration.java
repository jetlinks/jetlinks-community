package org.jetlinks.community.configuration;

import com.alibaba.fastjson.JSON;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.apache.commons.beanutils.BeanUtilsBean;
import org.apache.commons.beanutils.Converter;
import org.jetlinks.community.Interval;
import org.jetlinks.community.utils.TimeUtils;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.util.unit.DataSize;

import java.time.Duration;

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
                return (T) DataSize.parse(String.valueOf(value));
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

    }

}
