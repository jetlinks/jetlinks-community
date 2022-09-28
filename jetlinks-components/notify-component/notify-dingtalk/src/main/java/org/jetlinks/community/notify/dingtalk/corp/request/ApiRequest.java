package org.jetlinks.community.notify.dingtalk.corp.request;

import org.jetlinks.core.command.Command;
import org.springframework.web.reactive.function.client.WebClient;

public abstract class ApiRequest<Response> implements Command<Response> {

    public abstract Response execute(WebClient client);

}
