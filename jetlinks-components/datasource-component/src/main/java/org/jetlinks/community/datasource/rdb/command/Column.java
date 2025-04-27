package org.jetlinks.community.datasource.rdb.command;

import lombok.*;
import org.hswebframework.ezorm.rdb.metadata.RDBColumnMetadata;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Column {

    private String name;
    private String previousName;

    private String type;

    private boolean primaryKey;
    private int length;
    private int scale;
    private int precision;
    private boolean notnull;

    private String comment;

    public static Column of(RDBColumnMetadata columnMetadata) {
        Column column = new Column();
        column.setPreviousName(columnMetadata.getName());
        column.setName(columnMetadata.getName());
        column.setType(columnMetadata.getType().getSqlType().getName());
        column.setLength(columnMetadata.getLength() == 0 ? columnMetadata.getPrecision() : columnMetadata.getLength());
        column.setScale(columnMetadata.getScale());
        column.setPrecision(columnMetadata.getPrecision());
        column.setNotnull(columnMetadata.isNotNull());
        column.setPrimaryKey(columnMetadata.isPrimaryKey());
        column.setComment(columnMetadata.getComment());
        return column;
    }
}
