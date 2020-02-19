package org.jetlinks.community.logging.controller;

import org.hswebframework.ezorm.core.param.QueryParam;
import org.hswebframework.web.api.crud.entity.PagerResult;
import org.hswebframework.web.authorization.annotation.QueryAction;
import org.hswebframework.web.authorization.annotation.Resource;
import org.jetlinks.community.logging.service.SystemLoggerService;
import org.jetlinks.community.logging.system.SerializableSystemLog;
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
@RequestMapping("/logger/system")
@Resource(id="system-logger",name = "系统日志")
public class SystemLoggerController {

    @Autowired
    private SystemLoggerService loggerService;

    @GetMapping("/_query")
    @QueryAction
    public Mono<PagerResult<SerializableSystemLog>> getSystemLogger(QueryParam queryParam) {
        return loggerService.getSystemLogger(queryParam);
    }


}
