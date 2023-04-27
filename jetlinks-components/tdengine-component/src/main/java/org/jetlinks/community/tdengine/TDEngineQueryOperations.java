package org.jetlinks.community.tdengine;

import org.hswebframework.ezorm.rdb.executor.wrapper.ResultWrapper;
import reactor.core.publisher.Flux;

import java.util.Map;

public interface TDEngineQueryOperations {

    <E> Flux<E> query(String sql, ResultWrapper<E,?> wrapper);

}
