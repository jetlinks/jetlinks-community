package org.jetlinks.community.elastic.search.manager;

import org.springframework.stereotype.Component;

import java.util.concurrent.ScheduledExecutorService;

/**
 * @author bsetfeng
 * @since 1.0
 **/
@Component
public class StandardsIndexInit {

    private final ScheduledExecutorService executorService;

    public StandardsIndexInit(ScheduledExecutorService executorService) {
        this.executorService = executorService;
    }


}
