package org.jetlinks.community.logging.controller;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.hswebframework.ezorm.core.param.QueryParam;
import org.hswebframework.web.api.crud.entity.PagerResult;
import org.hswebframework.web.api.crud.entity.QueryOperation;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.hswebframework.web.authorization.annotation.QueryAction;
import org.hswebframework.web.authorization.annotation.Resource;
import org.jetlinks.community.logging.access.SerializableAccessLog;
import org.jetlinks.community.logging.service.AccessLoggerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * @author bsetfeng
 * @since 1.0
 **/
@RestController
@RequestMapping("/logger/access")
@Resource(id="access-logger",name = "访问日志")
@Tag(name = "日志管理")
public class AccessLoggerController {

    @Autowired
    private AccessLoggerService loggerService;

    @GetMapping("/_query")
    @QueryAction
    @QueryOperation(summary = "查询访问日志")
    public Mono<PagerResult<SerializableAccessLog>> getAccessLogger(@Parameter(hidden = true) QueryParamEntity queryParam) {
        return loggerService.getAccessLogger(queryParam);
    }



}
