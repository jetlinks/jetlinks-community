package org.jetlinks.community.logging.access;


import org.hswebframework.web.api.crud.entity.PagerResult;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * 访问日志服务,用于记录和查询平台的接口访问日志
 *
 * @version 2.3
 **/
public interface AccessLoggerService {

    /**
     * 保存访问日志
     *
     * @param log 日志内容
     * @return void
     */
    Mono<Void> save(SerializableAccessLog log);

    /**
     * 分页查询访问日志
     *
     * @param queryParam 动态查询参数
     * @return 查询结果
     */
    Mono<PagerResult<SerializableAccessLog>> query(QueryParamEntity queryParam);

    /**
     * 不分页查询访问日志
     *
     * @param queryParam 查询参数
     * @return 查询结果
     */
    Flux<SerializableAccessLog> queryNoPaging(QueryParamEntity queryParam);

}
