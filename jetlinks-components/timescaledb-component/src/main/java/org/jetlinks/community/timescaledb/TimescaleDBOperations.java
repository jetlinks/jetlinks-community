package org.jetlinks.community.timescaledb;

import org.hswebframework.ezorm.rdb.operator.DatabaseOperator;

public interface TimescaleDBOperations {

    DatabaseOperator database();

    TimescaleDBDataWriter writer();

}
