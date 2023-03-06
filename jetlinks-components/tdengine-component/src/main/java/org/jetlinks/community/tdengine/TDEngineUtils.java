package org.jetlinks.community.tdengine;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.reactive.function.client.ClientResponse;
import reactor.core.publisher.Mono;

@Slf4j
public class TDEngineUtils {


    public static Mono<JSONObject> checkExecuteResult(ClientResponse response) {
        if (response.statusCode().isError()) {
            return response
                .bodyToMono(String.class)
                .doOnNext(str -> {
                    throw new TDengineException(null, str);
                })
                .switchIfEmpty(Mono.error(() -> new TDengineException(null, response.statusCode().getReasonPhrase())))
                .then(Mono.empty());

        }
        return response
            .bodyToMono(String.class)
            .map(json -> {
                JSONObject obj = JSON.parseObject(json);
                checkExecuteResult(null, obj);
                return obj;
            });


    }

    public static void checkExecuteResult(String sql, JSONObject result) {
        if (result.getInteger("code") != 0) {
            String error = result.getString("desc");
            if (sql != null && (sql.startsWith("describe")
                || sql.startsWith("select")
                || sql.startsWith("SELECT"))
                && error.contains("does not exist")) {
                return;
            }
            if (sql != null) {
                log.warn("execute tdengine sql error [{}]: [{}]", error, sql);
            }

            throw new TDengineException(sql, result.getString("desc"));
        }
    }

}
