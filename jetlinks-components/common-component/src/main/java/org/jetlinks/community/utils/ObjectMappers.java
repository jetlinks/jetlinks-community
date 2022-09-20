package org.jetlinks.community.utils;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

public class ObjectMappers {

    public static final ObjectMapper JSON_MAPPER;
    public static final ObjectMapper CBOR_MAPPER;
    public static final ObjectMapper SMILE_MAPPER;

    static {
        JSON_MAPPER = Jackson2ObjectMapperBuilder
            .json()
            .build()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .setSerializationInclusion(JsonInclude.Include.NON_NULL)
        ;
        {
            ObjectMapper cbor;

            try {
                cbor = Jackson2ObjectMapperBuilder
                    .cbor()
                    .build()
                    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                    .setSerializationInclusion(JsonInclude.Include.NON_NULL);
            } catch (Throwable ignore) {
                cbor = null;
            }
            CBOR_MAPPER = cbor;
        }
        {
            ObjectMapper smile;

            try {

                smile = Jackson2ObjectMapperBuilder
                    .smile()
                    .build()
                    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                    .setSerializationInclusion(JsonInclude.Include.NON_NULL);
            } catch (Throwable ignore) {
                smile = null;
            }
            SMILE_MAPPER = smile;
        }


    }


    @SneakyThrows
    public static String toJsonString(Object data){
        return JSON_MAPPER.writeValueAsString(data);
    }

}
