package org.jetlinks.community.device.events.handler;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.jetlinks.core.metadata.Converter;
import org.jetlinks.core.metadata.DataType;

/**
 * @author bsetfeng
 * @since 1.0
 **/
@Slf4j
public class ValueTypeTranslator {

    public static Object translator(Object value, DataType dataType) {
        try {
           if (dataType instanceof Converter<?>) {
                return ((Converter<?>) dataType).convert(value);
            } else {
                return value;
            }
        } catch (Exception e) {
            log.error("设备上报值与物模型不匹配.value:{},type:{}", value, JSON.toJSONString(dataType), e);
            return value;
        }
    }

}
