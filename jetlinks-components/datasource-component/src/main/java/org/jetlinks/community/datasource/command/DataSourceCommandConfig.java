package org.jetlinks.community.datasource.command;

import lombok.Getter;
import lombok.Setter;
import org.jetlinks.core.command.Command;
import org.jetlinks.community.datasource.DataSource;

import java.util.Map;

@Getter
@Setter
public class DataSourceCommandConfig {

    /**
     * 数据源类型
     *
     * @see DataSource#getType()
     */
    private String datasourceType;
    /**
     * 数据源ID
     *
     * @see DataSource#getId()
     */
    private String datasourceId;

    /**
     * 命令支持ID,比如一个数据源命令分类.
     *
     * @see org.jetlinks.community.command.CommandSupportManagerProvider#getCommandSupport(String, Map)
     */
    private String supportId;

    /**
     * 命令ID
     *
     * @see Command#getCommandId()
     */
    private String commandId;

    private String commandName;

    /**
     * 命令配置信息
     */
    private Map<String, Object> configuration;

}
