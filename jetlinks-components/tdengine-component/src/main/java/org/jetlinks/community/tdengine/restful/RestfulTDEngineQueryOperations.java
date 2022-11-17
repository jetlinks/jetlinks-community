package org.jetlinks.community.tdengine.restful;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hswebframework.ezorm.rdb.executor.DefaultColumnWrapperContext;
import org.hswebframework.ezorm.rdb.executor.wrapper.ResultWrapper;
import org.jetlinks.community.tdengine.TDEngineQueryOperations;
import org.springframework.util.CollectionUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.stream.Collectors;

import static org.jetlinks.community.tdengine.TDEngineUtils.checkExecuteResult;

@AllArgsConstructor
@Slf4j
public class RestfulTDEngineQueryOperations implements TDEngineQueryOperations {

    private final WebClient client;

    private final String database;
    @Override
    public <E> Flux<E> query(String sql, ResultWrapper<E, ?> wrapper) {
        log.trace("Execute ==> {}", sql);
        return client
            .post()
            .uri("/rest/sql/"+database)
            .bodyValue(sql)
            .exchangeToFlux(response -> response
                .bodyToMono(String.class)
                .flatMapMany(json -> {
                    JSONObject result = JSON.parseObject(json);
                    checkExecuteResult(sql, result);
                    return convertQueryResult(result, wrapper);
                }));
    }

    protected <E> Flux<E> convertQueryResult(JSONObject result, ResultWrapper<E, ?> wrapper) {

        JSONArray head = result.getJSONArray("column_meta");
        JSONArray data = result.getJSONArray("data");

        if (CollectionUtils.isEmpty(head) || CollectionUtils.isEmpty(data)) {
            return Flux.empty();
        }
        List<String> columns = head.stream()
            .map(v -> ((JSONArray) v).getString(0))
            .collect(Collectors.toList());

        return Flux.create(sink -> {
            wrapper.beforeWrap(() -> columns);

            for (Object rowo : data) {
                E rowInstance = wrapper.newRowInstance();
                JSONArray row = (JSONArray) rowo;
                for (int i = 0; i < columns.size(); i++) {
                    String property = columns.get(i);
                    Object value = row.get(i);
                    DefaultColumnWrapperContext<E> context = new DefaultColumnWrapperContext<>(i, property, value, rowInstance);
                    wrapper.wrapColumn(context);
                    rowInstance = context.getRowInstance();
                }
                if (!wrapper.completedWrapRow(rowInstance)) {
                    break;
                }
                if (rowInstance != null) {
                    sink.next(rowInstance);
                }
            }
            wrapper.completedWrap();
            sink.complete();
        });
    }


}
