package org.jetlinks.community.datasource.rdb;

import org.jetlinks.community.datasource.DataSourceType;

public enum RDBDataSourceType implements DataSourceType {
    rdb
    ;

    @Override
    public String getId() {
        return name();
    }

    @Override
    public String getName() {
        return "关系型数据库";
    }
}
