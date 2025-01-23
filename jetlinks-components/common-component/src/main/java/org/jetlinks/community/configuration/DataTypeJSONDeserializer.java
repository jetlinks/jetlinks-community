package org.jetlinks.community.configuration;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import org.apache.commons.beanutils.BeanUtilsBean;
import org.jetlinks.core.metadata.DataType;

import java.io.IOException;
import java.util.Map;

/**
 *
 * @author zhangji 2025/1/23
 * @since 2.3
 */
public class DataTypeJSONDeserializer extends JsonDeserializer<DataType> {
    @Override
    public DataType deserialize(JsonParser parser, DeserializationContext ctxt) throws IOException, JsonProcessingException {

        Map<String,Object> map= ctxt.readValue(parser, Map.class);

        return (DataType) BeanUtilsBean.getInstance().getConvertUtils().convert(map, DataType.class);
    }
}
