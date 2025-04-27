package org.jetlinks.community.datasource;

import org.jetlinks.core.command.Command;
import org.jetlinks.core.command.CommandSupport;
import org.jetlinks.core.command.CommandUtils;
import org.jetlinks.core.metadata.FunctionMetadata;
import org.jetlinks.core.metadata.SimpleFunctionMetadata;
import org.jetlinks.community.command.CommandSupportManagerProviders;
import reactor.core.publisher.Mono;

import java.util.function.Consumer;

public interface DataSourceConstants {


    interface Commands {

        static String createCommandProvider(String dataSourceId) {
            return "datasource$" + dataSourceId;
        }

        static Mono<CommandSupport> getCommandSupport(String datasourceId) {
            return CommandSupportManagerProviders
                .getCommandSupport(createCommandProvider(datasourceId));
        }

        static Mono<CommandSupport> getCommandSupport(String datasourceId, String supportId) {
            return CommandSupportManagerProviders
                .getCommandSupport(createCommandProvider(datasourceId), supportId);
        }

    }

    interface Metadata {

        static FunctionMetadata create(@SuppressWarnings("all") Class<? extends Command> cmdType,
                                       Consumer<SimpleFunctionMetadata> handler) {
            SimpleFunctionMetadata metadata = new SimpleFunctionMetadata();
            metadata.setId(CommandUtils.getCommandIdByType(cmdType));
            metadata.setName(metadata.getId());
            handler.accept(metadata);
            return metadata;
        }

    }
}
