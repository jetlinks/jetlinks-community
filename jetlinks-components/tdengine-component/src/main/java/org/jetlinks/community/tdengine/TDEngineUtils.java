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
package org.jetlinks.community.tdengine;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.web.reactive.function.client.ClientResponse;
import reactor.core.publisher.Mono;

@Slf4j
public class TDEngineUtils {

    public static final DateTimeFormatter format = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss.SSS");


    public static String formatTime(long timestamp) {
        return new DateTime(timestamp).toString(format);
    }


    public static Mono<JSONObject> checkExecuteResult(ClientResponse response) {
        if (response.statusCode().isError()) {
            return response
                .bodyToMono(String.class)
                .doOnNext(str -> {
                    throw new TDengineException(null, str);
                })
                .switchIfEmpty(Mono.error(() -> new TDengineException(null, response.statusCode().toString())))
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
