package org.jetlinks.community.datasource.rdb.command;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.hswebframework.ezorm.rdb.metadata.TableOrViewMetadata;
import org.hswebframework.ezorm.rdb.metadata.parser.TableMetadataParser;
import org.hswebframework.ezorm.rdb.operator.DatabaseOperator;
import reactor.core.publisher.Flux;

@NoArgsConstructor
@AllArgsConstructor
public class GetTables implements RDBCommand<Flux<Table>> {

    private boolean includeColumns;

    @Override
    public Flux<Table> execute(DatabaseOperator operator) {

        TableMetadataParser parser = operator
            .getMetadata()
            .getCurrentSchema()
            .findFeatureNow(TableMetadataParser.id);

        return
            includeColumns
                ? parser.parseAllReactive().cast(TableOrViewMetadata.class).map(Table::of)
                : parser.parseAllTableNameReactive().map(name -> new Table(name, null));
    }
}
