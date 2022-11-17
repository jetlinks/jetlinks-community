package org.jetlinks.community.tdengine.metadata;

import org.hswebframework.ezorm.rdb.metadata.RDBSchemaMetadata;

public class TDengineSchema extends RDBSchemaMetadata {

    public TDengineSchema(String name) {
        super(name);
        addFeature(new TDengineMetadataParser(this));
        addFeature(new TDengineCreateTableSqlBuilder());
        addFeature(new TDengineAlterTableSqlBuilder());
    }


}
