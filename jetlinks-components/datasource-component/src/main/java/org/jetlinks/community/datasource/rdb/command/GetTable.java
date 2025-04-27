package org.jetlinks.community.datasource.rdb.command;

import lombok.AllArgsConstructor;
import org.hswebframework.ezorm.rdb.metadata.TableOrViewMetadata;
import org.hswebframework.ezorm.rdb.metadata.parser.TableMetadataParser;
import org.hswebframework.ezorm.rdb.operator.DatabaseOperator;
import reactor.core.publisher.Mono;

@AllArgsConstructor
public class GetTable implements RDBCommand<Mono<Table>> {

    private final String name;

    @Override
    public Mono<Table> execute(DatabaseOperator operator) {
        return operator
            .getMetadata()
            .getCurrentSchema()
            .<TableMetadataParser>findFeatureNow(TableMetadataParser.id)
            .parseByNameReactive(name)
            .cast(TableOrViewMetadata.class)
            .map(Table::of);
    }
}
