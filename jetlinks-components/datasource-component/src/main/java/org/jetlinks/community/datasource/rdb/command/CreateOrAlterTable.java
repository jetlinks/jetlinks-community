package org.jetlinks.community.datasource.rdb.command;

import lombok.AllArgsConstructor;
import org.hswebframework.ezorm.rdb.operator.DatabaseOperator;
import org.hswebframework.ezorm.rdb.operator.ddl.TableBuilder;
import reactor.core.publisher.Mono;

@AllArgsConstructor
public class CreateOrAlterTable implements RDBCommand<Mono<Void>> {

    private final Table table;

    @Override
    public Mono<Void> execute(DatabaseOperator operator) {

        TableBuilder builder = operator
            .ddl()
            .createOrAlter(table.getName());

        for (Column column : table.getColumns()) {
            builder
                .addColumn(column.getName())
                .custom(rdb -> {
                    rdb.setPrimaryKey(column.isPrimaryKey());
                    rdb.setNotNull(column.isNotnull());
                    rdb.setPreviousName(column.getPreviousName());
                })
                .length(column.getLength(), column.getScale())
                .type(column.getType())
                .comment(column.getComment())
                .commit();
        }

        return builder
            .commit()
            .reactive()
            .then();
    }
}
