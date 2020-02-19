package org.jetlinks.community.logging.controller;

import org.hswebframework.ezorm.core.param.QueryParam;
import org.hswebframework.web.api.crud.entity.PagerResult;
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
public class AccessLoggerController {

    @Autowired
    private AccessLoggerService loggerService;

    @GetMapping("/_query")
    @QueryAction
    public Mono<PagerResult<SerializableAccessLog>> getAccessLogger(QueryParam queryParam) {
        return loggerService.getAccessLogger(queryParam);
    }



}
