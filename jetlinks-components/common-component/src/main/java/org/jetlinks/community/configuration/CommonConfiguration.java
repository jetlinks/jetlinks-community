package org.jetlinks.community.configuration;

import com.alibaba.fastjson.JSON;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.apache.commons.beanutils.BeanUtilsBean;
import org.apache.commons.beanutils.Converter;
import org.springframework.context.annotation.Configuration;

@Configuration
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
    }

}
