package org.jetlinks.community.rule.engine.executor;

import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import org.hswebframework.ezorm.rdb.executor.SqlRequests;
import org.hswebframework.ezorm.rdb.executor.reactive.ReactiveSqlExecutor;
import org.hswebframework.ezorm.rdb.executor.wrapper.ResultWrappers;
import org.hswebframework.web.bean.FastBeanCopier;
import org.hswebframework.web.utils.ExpressionUtils;
import org.jetlinks.rule.engine.api.RuleData;
import org.jetlinks.rule.engine.api.RuleDataHelper;
import org.jetlinks.rule.engine.api.model.NodeType;
import org.jetlinks.rule.engine.api.task.ExecutionContext;
import org.jetlinks.rule.engine.api.task.TaskExecutor;
import org.jetlinks.rule.engine.api.task.TaskExecutorProvider;
import org.jetlinks.rule.engine.defaults.LambdaTaskExecutor;
import org.reactivestreams.Publisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Component
public class SqlExecutorTaskExecutorProvider implements TaskExecutorProvider {

    @Autowired
    private ReactiveSqlExecutor sqlExecutor;

    public String getExecutor() {
        return "sql";
    }

    public Function<RuleData, Publisher<?>> createExecutor(ExecutionContext context, Config config) {

        if (config.isQuery()) {
            return (data) -> Flux.defer(() -> {
                String sql = config.getSql(data);
                List<Flux<Map<String, Object>>> fluxes = new ArrayList<>();
                data.acceptMap(map -> fluxes.add(sqlExecutor.select(Mono.just(SqlRequests.template(sql, map)), ResultWrappers.map())));
                return Flux.concat(fluxes);
            });
        } else {
            return data -> Mono.defer(() -> {
                String sql = config.getSql(data);
                List<Mono<Integer>> fluxes = new ArrayList<>();
                data.acceptMap(map -> fluxes.add(sqlExecutor.update(Mono.just(SqlRequests.template(sql, map)))));

                return Flux.concat(fluxes).reduce(Math::addExact);
            });
        }

    }

    @Override
    public Mono<TaskExecutor> createTask(ExecutionContext context) {
        return Mono.just(new LambdaTaskExecutor("SQL",context, () -> createExecutor(context, FastBeanCopier.copy(context.getJob().getConfiguration(),new Config()))));
    }


    @Getter
    @Setter
    public static class Config {

        private String dataSourceId;

        private NodeType nodeType = NodeType.MAP;

        private String sql;

        private boolean stream;

        private boolean transaction;

        public boolean isQuery() {

            return sql.trim().startsWith("SELECT") ||
                sql.trim().startsWith("select");
        }

        @SneakyThrows
        public String getSql(RuleData data) {
            if (!sql.contains("${")) {
                return sql;
            }

            return ExpressionUtils.analytical(sql, RuleDataHelper.toContextMap(data), "spel");
        }

    }

}
