package org.jetlinks.community.rule.engine.cmd;

import org.jetlinks.core.command.AbstractCommandSupport;
import org.jetlinks.community.rule.engine.entity.AlarmHistoryInfo;
import org.jetlinks.community.rule.engine.service.AlarmHistoryService;
import org.jetlinks.sdk.server.commons.cmd.QueryPagerCommand;

/**
 * @author liusq
 * @date 2024/4/12
 */
public class AlarmHistoryCommandSupport extends AbstractCommandSupport {
    public AlarmHistoryCommandSupport(AlarmHistoryService service) {
        //分页查询
        registerHandler(
            QueryPagerCommand
                .<AlarmHistoryInfo>createHandler(
                    metadata -> {
                    },
                    cmd -> service.queryPager(cmd.asQueryParam()))
        );
    }
}
