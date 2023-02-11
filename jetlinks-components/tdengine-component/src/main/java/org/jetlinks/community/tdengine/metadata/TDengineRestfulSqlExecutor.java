package org.jetlinks.community.tdengine.metadata;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hswebframework.ezorm.rdb.executor.BatchSqlRequest;
import org.hswebframework.ezorm.rdb.executor.DefaultColumnWrapperContext;
import org.hswebframework.ezorm.rdb.executor.SqlRequest;
import org.hswebframework.ezorm.rdb.executor.reactive.ReactiveSqlExecutor;
import org.hswebframework.ezorm.rdb.executor.wrapper.ResultWrapper;
import org.jetlinks.community.tdengine.TDengineException;
import org.jetlinks.core.utils.Reactors;
import org.reactivestreams.Publisher;
import org.springframework.util.CollectionUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@AllArgsConstructor
public class TDengineRestfulSqlExecutor implements ReactiveSqlExecutor {

    private final WebClient client;

    @Override
    public Mono<Integer> update(Publisher<SqlRequest> request) {
        return this
            .doExecute(request)
            .then(Reactors.ALWAYS_ONE);
    }

    @Override
    public Mono<Void> execute(Publisher<SqlRequest> request) {
        return this
            .doExecute(request)
            .then();
    }

    @Override
    public <E> Flux<E> select(Publisher<SqlRequest> requests, ResultWrapper<E, ?> wrapper) {
        return this
            .doExecute(requests)
            .flatMap(response -> convertQueryResult(response, wrapper));
    }

    private Flux<JSONObject> doExecute(Publisher<SqlRequest> requests) {
        return Flux
            .from(requests)
            .expand(request -> {
                if (request instanceof BatchSqlRequest) {
                    return Flux.fromIterable(((BatchSqlRequest) request).getBatch());
                }
                return Flux.empty();
            })
            .filter(SqlRequest::isNotEmpty)
            .concatMap(request -> {
                String sql = request.toNativeSql();
                log.trace("Execute ==> {}", sql);
                return client
                    .post()
                    .uri("/rest/sql")
                    .bodyValue(sql)
                    .exchangeToMono(response -> response
                        .bodyToMono(String.class)
                        .map(json -> {
                            JSONObject result = JSON.parseObject(json);
                            checkExecuteResult(sql, result);
                            return result;
                        }));
            });
    }

    private void checkExecuteResult(String sql, JSONObject result) {
        if (result.getInteger("code") != 0) {
            String error = result.getString("desc");
            if (sql.startsWith("describe") && error.contains("does not exist")) {
                return;
            }
            log.warn("execute tdengine sql error [{}]: [{}]", error, sql);
            throw new TDengineException(sql, result.getString("desc"));
        }
    }

    protected <E> Flux<E> convertQueryResult(JSONObject result, ResultWrapper<E, ?> wrapper) {

        JSONArray head = result.getJSONArray("column_meta");
        JSONArray data = result.getJSONArray("data");

        if (CollectionUtils.isEmpty(head) || CollectionUtils.isEmpty(data)) {
            return Flux.empty();
        }
        List<String> columns = head.stream()
            .map(v-> ((JSONArray) v).getString(0))
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
