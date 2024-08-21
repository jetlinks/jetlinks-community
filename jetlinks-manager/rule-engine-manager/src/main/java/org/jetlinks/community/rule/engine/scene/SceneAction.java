package org.jetlinks.community.rule.engine.scene;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.hswebframework.ezorm.core.param.Term;
import org.hswebframework.web.bean.FastBeanCopier;
import org.hswebframework.web.i18n.LocaleUtils;
import org.jetlinks.core.metadata.DataType;
import org.jetlinks.core.metadata.PropertyMetadata;
import org.jetlinks.core.metadata.types.ObjectType;
import org.jetlinks.community.reactorql.term.TermTypes;
import org.jetlinks.community.rule.engine.scene.internal.actions.*;
import org.jetlinks.community.terms.TermSpec;
import org.jetlinks.community.utils.ConverterUtils;
import org.jetlinks.rule.engine.api.model.RuleNodeModel;
import reactor.core.publisher.Flux;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static org.hswebframework.web.i18n.LocaleUtils.resolveMessage;
import static org.jetlinks.community.rule.engine.scene.SceneRule.createBranchActionId;

@Getter
@Setter
public class SceneAction implements Serializable {

    @Schema(description = "执行器类型")
    @NotNull
    private String executor;

    @Schema(description = "执行器类型为[notify]时不能为空")
    private NotifyAction notify;

    @Schema(description = "执行器类型为[delay]时不能为空")
    private DelayAction delay;

    @Schema(description = "执行器类型为[device]时不能为空")
    private DeviceAction device;

    @Schema(description = "执行器类型为[alarm]时不能为空")
    private AlarmAction alarm;

    @Schema(description = "输出过滤条件,串行执行动作时,满足条件才会进入下一个节点")
    private List<Term> terms;

    @Schema(description = "其他执行器配置")
    private Map<String, Object> configuration;

    @Schema(description = "拓展信息")
    private Map<String, Object> options;

    @Schema(description = "执行动作ID")
    private Integer actionId;

    /**
     * 从拓展信息中获取需要查询的列,用于在设备触发等场景需要在sql中获取对应的数据.
     *
     * @param options 拓展信息
     * @return terms
     */
    public static List<String> parseColumnFromOptions(Map<String, Object> options) {
        Object columns;
        if (MapUtils.isEmpty(options) || (columns = options.get("columns")) == null) {
            return Collections.emptyList();
        }

        //获取前端设置的columns
        return ConverterUtils.convertToList(columns, String::valueOf);
    }

    /**
     * 尝试从动作的变量中提取出需要动态获取的列信息
     *
     * @return 条件
     */
    private List<String> parseActionTerms() {

        return SceneProviders
            .getActionProviderNow(executor)
            .parseColumns(actionConfig());
    }

    public List<String> createContextColumns() {
        List<String> termList = new ArrayList<>();
        termList.addAll(parseColumnFromOptions(options));
        termList.addAll(parseActionTerms());
        return termList;
    }


    public Object actionConfig() {
        switch (executor) {
            case Executor.device:
                return device;
            case Executor.notify:
                return notify;
            case Executor.delay:
                return delay;
            case Executor.alarm:
                return alarm;
            default:
                Object conf = SceneProviders.getActionProviderNow(executor).newConfig();
                return configuration == null ? conf : FastBeanCopier.copy(configuration, conf);
        }
    }

    public Flux<Variable> createVariables(Integer branchIndex,
                                          Integer group,
                                          int index) {
        return SceneProviders
            .getActionProviderNow(executor)
            .createVariable(actionConfig())
            .collectList()
            .filter(CollectionUtils::isNotEmpty)
            .map(list -> SceneAction.createVariable(branchIndex, group, index, list))
            .flux()
            .as(LocaleUtils::transform);

    }

    public static Variable createVariable(Integer branchIndex, Integer group, int actionIndex, List<Variable> children) {

        String varId = "action_" + actionIndex;

        if (branchIndex != null) {
            varId = createBranchActionId(branchIndex, group, actionIndex);
        }

        String name = resolveMessage(
            "message.action_var_index",
            String.format("动作[%s]", actionIndex),
            actionIndex
        );

        String fullName = resolveMessage(
            "message.action_var_index_full",
            String.format("动作[%s]输出", actionIndex),
            actionIndex
        );

        String description = resolveMessage(
            "message.action_var_output_description",
            String.format("动作[%s]执行的输出结果", actionIndex),
            actionIndex
        );

        Variable variable = Variable.of(varId, name);
        variable.setFullName(fullName);
        variable.setDescription(description);
        variable.setChildren(children);

        return variable;
    }

    public static SceneAction notify(String notifyType,
                                     String notifierId,
                                     String templateId,
                                     Consumer<NotifyAction> consumer) {
        SceneAction action = new SceneAction();
        action.executor = Executor.notify;
        action.notify = new NotifyAction();
        action.notify.setNotifierId(notifierId);
        action.notify.setNotifyType(notifyType);
        action.notify.setTemplateId(templateId);
        consumer.accept(action.notify);
        return action;
    }


    public void applyNode(RuleNodeModel node) {

        SceneProviders
            .getActionProviderNow(executor)
            .applyRuleNode(actionConfig(), node);

    }

    public void applyFilterSpec(RuleNodeModel node, List<TermSpec> specs) {
        SceneProviders
                .getActionProviderNow(executor)
                .applyFilterSpec(node, specs);
    }

    public static Variable toVariable(String prefix,
                                      PropertyMetadata metadata,
                                      String i18nKey,
                                      String msgPattern) {
        return toVariable(prefix.concat(".").concat(metadata.getId()),
                          metadata.getName(),
                          metadata.getValueType(),
                          i18nKey,
                          msgPattern,
                          null);
    }

    public static Variable toVariable(String id,
                                      String metadataName,
                                      DataType dataType,
                                      String i18nKey,
                                      String msgPattern,
                                      String parentName) {

        String fullName = parentName == null ? metadataName : parentName + "." + metadataName;
        Variable variable = Variable.of(id, LocaleUtils.resolveMessage(i18nKey,
                                                                       String.format(msgPattern, fullName),
                                                                       fullName));
        variable.setType(dataType.getType());
        variable.setTermTypes(TermTypes.lookup(dataType));
        variable.setColumn(id);
        if (dataType instanceof ObjectType) {
            List<Variable> children = new ArrayList<>();
            for (PropertyMetadata property : ((ObjectType) dataType).getProperties()) {
                children.add(
                    toVariable(id + "." + property.getId(),
                               property.getName(),
                               property.getValueType(),
                               i18nKey,
                               msgPattern,
                               fullName)
                );
            }
            variable.setChildren(children);
        }

        return variable;

    }

    public interface Executor {
        String notify = NotifyActionProvider.PROVIDER;
        String delay = DelayActionProvider.PROVIDER;
        String device = DeviceActionProvider.PROVIDER;
        String alarm = AlarmActionProvider.PROVIDER;
    }

}
