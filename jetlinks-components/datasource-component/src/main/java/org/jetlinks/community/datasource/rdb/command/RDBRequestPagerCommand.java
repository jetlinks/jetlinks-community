package org.jetlinks.community.datasource.rdb.command;

import org.hswebframework.web.api.crud.entity.PagerResult;
import org.jetlinks.core.command.CommandHandler;
import org.jetlinks.core.metadata.FunctionMetadata;
import org.jetlinks.core.metadata.PropertyMetadata;
import org.jetlinks.core.metadata.SimpleFunctionMetadata;
import org.jetlinks.sdk.server.commons.cmd.QueryPagerCommand;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;


public class RDBRequestPagerCommand extends QueryPagerCommand<Map<String, Object>> {

    private String commandId;

    @Override
    public String getCommandId() {
        return commandId;
    }

    public RDBRequestPagerCommand(String commandId) {
        this.commandId = commandId;
        withConverter(RDBRequestListCommand::convertMap);
    }

    public RDBRequestPagerCommand() {
        withConverter(RDBRequestListCommand::convertMap);
    }

    public static <T> CommandHandler<RDBRequestPagerCommand, Mono<PagerResult<Map<String, Object>>>> createQueryHandler(
        String commandId,
        String commandName,
        Consumer<SimpleFunctionMetadata> custom,
        Function<RDBRequestPagerCommand, Mono<PagerResult<Map<String, Object>>>> handler) {
        return CommandHandler.of(
            () -> metadata(commandId, commandName, custom),
            (cmd, ignore) -> handler.apply(cmd),
            () -> new RDBRequestPagerCommand(commandId)
        );

    }

    public static FunctionMetadata metadata(String commandId, String commandName,Consumer<SimpleFunctionMetadata> custom) {
        SimpleFunctionMetadata metadata = new SimpleFunctionMetadata();
        List<PropertyMetadata> queryParamMetadata = QueryPagerCommand.getQueryParamMetadata();
        metadata.setId(commandId);
        metadata.setName(commandName);
        metadata.setInputs(queryParamMetadata);
        custom.accept(metadata);
        return metadata;
    }

}
