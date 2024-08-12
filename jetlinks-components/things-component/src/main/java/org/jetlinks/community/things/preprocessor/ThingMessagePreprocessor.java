package org.jetlinks.community.things.preprocessor;

import lombok.Getter;
import lombok.Setter;
import org.hswebframework.web.bean.FastBeanCopier;
import org.jetlinks.core.Wrapper;
import org.jetlinks.core.message.ThingMessage;
import org.jetlinks.core.message.property.Property;
import org.jetlinks.core.things.MetadataId;
import org.jetlinks.core.things.Thing;
import org.jetlinks.community.ValueObject;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * 物消息预处理器,用于执行消息预处理逻辑,如: 校验数据,转换数据,触发告警等操作.
 * <p>
 * 可通过实现{@link Provider}接口来实现不同的处理逻辑.
 *
 * @author zhouhao
 * @see org.jetlinks.pro.things.impl.preprocessor.internal.MatcherThingMessagePreprocessor
 * @since 2.2
 */
public interface ThingMessagePreprocessor extends Wrapper {

    /**
     * 执行处理
     *
     * @param context 上下文
     * @return 处理后的消息
     * @see Context#unwrap(Class)
     * @see PropertiesContext
     */
    Mono<ThingMessage> process(Context context);


    /**
     * @see PropertiesContext
     */
    interface Context extends Wrapper {

        /**
         * @return 物信息
         */
        Thing getThing();

        /**
         * @return 原始消息
         */
        ThingMessage getMessage();

        /**
         * 设置消息并返回新的上下文,通常用于复制上下文.
         *
         * @param message 消息
         * @return 新的上下文
         */
        Context with(ThingMessage message);
    }

    /**
     * 属性相关的上下文
     */
    interface PropertiesContext extends Context {
        //属性信息
        Property getProperty();

        @Override
        PropertiesContext with(ThingMessage message);
    }

    interface Provider {

        org.jetlinks.community.spi.Provider<Provider> supports = org.jetlinks.community.spi.Provider.create(Provider.class);

        String getId();

        Mono<ThingMessagePreprocessor> create(Config config);

    }


    @Getter
    @Setter
    class Config implements ValueObject {
        private String thingType;
        private String templateId;
        private String thingId;
        private MetadataId metadataId;
        private String preprocessId;

        private Map<String, Object> configuration;

        @Override
        public Map<String, Object> values() {
            return configuration;
        }

        public Config copy() {
            return FastBeanCopier.copy(this, new Config());
        }
    }
}
