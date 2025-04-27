package org.jetlinks.community.datasource.rdb.command;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hswebframework.ezorm.rdb.metadata.TableOrViewMetadata;

import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Table {
    private String name;

    private List<Column> columns;

    public static Table of(TableOrViewMetadata metadata){
        Table table = new Table();
        table.setName(metadata.getName());
        table.setColumns(metadata.getColumns().stream().map(Column::of).collect(Collectors.toList()));
        return table;
    }
}
