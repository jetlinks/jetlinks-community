package org.jetlinks.community.datasource.rdb.command;

import org.hswebframework.ezorm.rdb.operator.DatabaseOperator;
import org.jetlinks.core.command.Command;

public interface RDBCommand<T> extends Command<T> {

    T execute(DatabaseOperator operator);
}
