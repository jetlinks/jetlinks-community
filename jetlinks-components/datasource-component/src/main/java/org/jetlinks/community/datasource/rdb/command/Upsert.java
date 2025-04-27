package org.jetlinks.community.datasource.rdb.command;

import lombok.AllArgsConstructor;
import org.hswebframework.ezorm.rdb.operator.DatabaseOperator;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

@AllArgsConstructor
public class Upsert implements RDBCommand<Mono<Void>> {
    private final String table;

    private final List<Map<String, Object>> dataList;

    private final Set<String> ignoreUpdateColumn;

    public Upsert(String table, List<Map<String, Object>> dataList) {
        this(table, dataList, Collections.emptySet());
    }

    @Override
    public Mono<Void> execute(DatabaseOperator operator) {
        return operator
            .getMetadata()
            .getTableReactive(table)
            .flatMap(tableMetadata -> operator
                .dml()
                .upsert(tableMetadata)
                .values(dataList)
                .ignoreUpdate(ignoreUpdateColumn == null ? new String[0] : ignoreUpdateColumn.toArray(new String[0]))
                .execute()
                .reactive()
                .then()
            );
    }
}
