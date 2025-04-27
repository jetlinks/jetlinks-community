package org.jetlinks.community.datasource.rdb.command;

import lombok.AllArgsConstructor;
import org.hswebframework.ezorm.rdb.operator.DatabaseOperator;
import org.hswebframework.ezorm.rdb.operator.ddl.TableBuilder;
import reactor.core.publisher.Mono;

import java.util.Set;

@AllArgsConstructor
public class DropColumn implements RDBCommand<Mono<Void>> {

    private final String table;
    private final Set<String> columns;

    @Override
    public Mono<Void> execute(DatabaseOperator operator) {
        TableBuilder builder = operator
            .ddl()
            .createOrAlter(table);
        columns.forEach(builder::dropColumn);
        return builder
            .commit()
            .reactive()
            .then();
    }
}
