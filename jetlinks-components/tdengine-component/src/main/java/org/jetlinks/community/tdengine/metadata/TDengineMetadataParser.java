package org.jetlinks.community.tdengine.metadata;

import lombok.AllArgsConstructor;
import org.hswebframework.ezorm.core.meta.ObjectMetadata;
import org.hswebframework.ezorm.rdb.executor.reactive.ReactiveSqlExecutor;
import org.hswebframework.ezorm.rdb.executor.wrapper.ResultWrappers;
import org.hswebframework.ezorm.rdb.metadata.RDBColumnMetadata;
import org.hswebframework.ezorm.rdb.metadata.RDBSchemaMetadata;
import org.hswebframework.ezorm.rdb.metadata.RDBTableMetadata;
import org.hswebframework.ezorm.rdb.metadata.parser.TableMetadataParser;
import org.jetlinks.community.tdengine.TDengineConstants;
import org.jetlinks.reactor.ql.utils.CastUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@AllArgsConstructor
public class TDengineMetadataParser implements TableMetadataParser {

    private final RDBSchemaMetadata schema;

    private ReactiveSqlExecutor sql() {
        return schema.findFeatureNow(ReactiveSqlExecutor.ID);
    }

    @Override
    public List<String> parseAllTableName() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Flux<String> parseAllTableNameReactive() {
        return sql()
            .select("show stables", ResultWrappers.map())
            .mapNotNull(map -> (String) map.get("stable_name"));
    }

    @Override
    public boolean tableExists(String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Mono<Boolean> tableExistsReactive(String name) {
        return parseAllTableNameReactive()
            .hasElement(name);
    }

    @Override
    public Optional<? extends ObjectMetadata> parseByName(String s) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<? extends ObjectMetadata> parseAll() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Mono<RDBTableMetadata> parseByNameReactive(String name) {
        RDBTableMetadata table = schema.newTable(name);
        return sql()
            .select("describe "+table.getFullName(), ResultWrappers.map())
            .doOnNext(column -> table.addColumn(convertToColumn(column)))
            .then(Mono.fromSupplier(() -> table.getColumns().isEmpty() ? null : table));
    }

    private RDBColumnMetadata convertToColumn(Map<String, Object> columnInfo) {
        String note = (String) columnInfo.getOrDefault("Note", "");
        String column = (String) columnInfo.get("Field");
        String type = (String) columnInfo.get("Type");
        int length = CastUtils.castNumber(columnInfo.get("Length")).intValue();

        RDBColumnMetadata metadata = new RDBColumnMetadata();
        metadata.setName(column);
        metadata.setProperty(TDengineConstants.COLUMN_IS_TAG, "tag".equalsIgnoreCase(note));
        metadata.setLength(length);
        metadata.setType(schema.getDialect().convertDataType(type));
        return metadata;
    }

    @Override
    public Flux<RDBTableMetadata> parseAllReactive() {
        return parseAllTableNameReactive()
            .flatMap(this::parseByNameReactive);
    }
}
