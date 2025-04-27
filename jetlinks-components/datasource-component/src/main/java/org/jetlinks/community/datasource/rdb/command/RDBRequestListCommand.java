package org.jetlinks.community.datasource.rdb.command;

import org.hswebframework.web.bean.FastBeanCopier;
import org.jetlinks.core.command.CommandHandler;
import org.jetlinks.core.metadata.FunctionMetadata;
import org.jetlinks.core.metadata.PropertyMetadata;
import org.jetlinks.core.metadata.SimpleFunctionMetadata;
import org.jetlinks.sdk.server.commons.cmd.QueryListCommand;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;


public class RDBRequestListCommand extends QueryListCommand<Map<String, Object>> {

    private String commandId;

    @Override
    public String getCommandId() {
        return commandId;
    }

    public RDBRequestListCommand(String commandId) {
        this.commandId = commandId;
        withConverter(RDBRequestListCommand::convertMap);
    }

    public RDBRequestListCommand() {
        withConverter(RDBRequestListCommand::convertMap);
    }



    public static <T> CommandHandler<RDBRequestListCommand, Flux<Map<String, Object>>> createQueryHandler(
        String commandId,
        String commandName,
        Consumer<SimpleFunctionMetadata> custom,
        Function<RDBRequestListCommand, Flux<Map<String, Object>>> handler) {
        return CommandHandler.of(
            () -> metadata(commandId, commandName, custom),
            (cmd, ignore) -> handler.apply(cmd),
            () -> new RDBRequestListCommand(commandId)
        );

    }

    public static FunctionMetadata metadata(String commandId, String commandName, Consumer<SimpleFunctionMetadata> custom) {
        SimpleFunctionMetadata metadata = new SimpleFunctionMetadata();
        List<PropertyMetadata> queryParamMetadata = QueryListCommand.getQueryParamMetadata();
        metadata.setId(commandId);
        metadata.setName(commandName);
        metadata.setInputs(queryParamMetadata);
        custom.accept(metadata);
        return metadata;
    }

    public static Map<String, Object> convertMap(Object obj) {
        return obj instanceof Map ? (Map<String, Object>) obj : FastBeanCopier.copy(obj, new HashMap<>());
    }

}
