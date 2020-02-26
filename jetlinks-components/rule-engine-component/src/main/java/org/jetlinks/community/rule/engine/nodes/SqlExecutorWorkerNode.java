package org.jetlinks.community.rule.engine.nodes;

import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import org.hswebframework.ezorm.rdb.executor.SqlRequests;
import org.hswebframework.ezorm.rdb.executor.reactive.ReactiveSqlExecutor;
import org.hswebframework.ezorm.rdb.executor.wrapper.ResultWrappers;
import org.hswebframework.web.utils.ExpressionUtils;
import org.jetlinks.rule.engine.api.RuleData;
import org.jetlinks.rule.engine.api.executor.ExecutionContext;
import org.jetlinks.rule.engine.api.model.NodeType;
import org.jetlinks.rule.engine.executor.CommonExecutableRuleNodeFactoryStrategy;
import org.jetlinks.rule.engine.executor.node.RuleNodeConfig;
import org.reactivestreams.Publisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Component
public class SqlExecutorWorkerNode extends CommonExecutableRuleNodeFactoryStrategy<SqlExecutorWorkerNode.Config> {

    @Autowired
    private ReactiveSqlExecutor sqlExecutor;

    @Override
    public String getSupportType() {
        return "sql";
    }

    @Override
    public Function<RuleData, Publisher<Object>> createExecutor(ExecutionContext context, Config config) {

        if (config.isQuery()) {
            return (data) -> Flux.defer(() -> {
                String sql = config.getSql(data);
                List<Flux<Map<String, Object>>> fluxes = new ArrayList<>();
                data.acceptMap(map -> fluxes.add(sqlExecutor.select(Mono.just(SqlRequests.template(sql, map)), ResultWrappers.map())));
                return Flux.concat(fluxes) ;
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


    @Getter
    @Setter
    public static class Config implements RuleNodeConfig {

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
            Map<String, Object> map = new HashMap<>();
            map.put("data", data.getData());
            map.put("ruleData", data);
            map.put("attr", data.getAttributes());
            return ExpressionUtils.analytical(sql, map, "spel");
        }

        public void switchDataSource() {

        }

        public void resetDataSource() {

        }
    }

}
