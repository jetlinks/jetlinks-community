package org.jetlinks.community.notify.template;

import lombok.Getter;
import lombok.Setter;
import org.jetlinks.community.notify.DefaultNotifyType;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

@Getter
@Setter
public class TemplateProperties implements Serializable {
    private static final long serialVersionUID = -6849794470754667710L;

    /**
     * 模版ID
     */
    private String id;

    /**
     * 模版名称
     */
    private String name;

    /**
     * 固定配置ID
     */
    private String configId;

    /**
     * 类型
     *
     * @see DefaultNotifyType
     */
    private String type;

    /**
     * 服务商
     *
     * @see TemplateProvider#getProvider()
     */
    private String provider;

    /**
     * 模版内容,不同服务商格式不同
     */
    private Map<String,Object> template;

    /**
     * 变量定义,用于声明此模版所需要的变量信息,方便在使用模版时能知道应该传入的参数.
     */
    private List<VariableDefinition> variableDefinitions;

    /**
     * 模版说明
     */
    private String description;

}
