package org.jetlinks.community.datasource.rdb.command;

import lombok.AllArgsConstructor;
import org.hswebframework.ezorm.rdb.metadata.RDBSchemaMetadata;
import org.hswebframework.ezorm.rdb.operator.DatabaseOperator;
import org.jetlinks.core.metadata.FunctionMetadata;
import org.jetlinks.community.datasource.DataSourceConstants;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@AllArgsConstructor
public class Refresh implements RDBCommand<Mono<Void>> {

    @Override
    public Mono<Void> execute(DatabaseOperator operator) {
        return Flux
            .fromIterable(operator
                .getMetadata()
                .getSchemas())
            .concatMap(RDBSchemaMetadata::loadAllTableReactive)
            .then();
    }

    public static FunctionMetadata metadata() {
        return DataSourceConstants.Metadata
            .create(Refresh.class, func -> {
                func.setName("刷新RDB数据源信息");
            });
    }
}
