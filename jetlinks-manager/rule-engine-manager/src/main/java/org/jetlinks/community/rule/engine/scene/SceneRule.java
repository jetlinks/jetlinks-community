package org.jetlinks.community.rule.engine.scene;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.collections4.CollectionUtils;
import org.hswebframework.ezorm.core.param.Term;
import org.hswebframework.ezorm.rdb.executor.EmptySqlRequest;
import org.hswebframework.ezorm.rdb.executor.SqlRequest;
import org.hswebframework.web.api.crud.entity.TermExpressionParser;
import org.hswebframework.web.bean.FastBeanCopier;
import org.hswebframework.web.i18n.LocaleUtils;
import org.hswebframework.web.validator.ValidatorUtils;
import org.jetlinks.core.device.DeviceRegistry;
import org.jetlinks.core.metadata.types.DateTimeType;
import org.jetlinks.community.rule.engine.commons.TermsConditionEvaluator;
import org.jetlinks.community.rule.engine.scene.term.TermColumn;
import org.jetlinks.rule.engine.api.model.RuleLink;
import org.jetlinks.rule.engine.api.model.RuleModel;
import org.jetlinks.rule.engine.api.model.RuleNodeModel;
import org.jetlinks.rule.engine.defaults.AbstractExecutionContext;
import reactor.core.publisher.Flux;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

@Getter
@Setter
public class SceneRule implements Serializable {

    @Schema(description = "告警ID")
    @NotBlank(message = "error.scene_rule_id_cannot_be_blank")
    private String id;

    @Schema(description = "告警名称")
    @NotBlank(message = "error.scene_rule_name_cannot_be_blank")
    private String name;

    @Schema(description = "触发器")
    @NotNull(message = "error.scene_rule_trigger_cannot_be_null")
    private Trigger trigger;

    /**
     * @see TermColumn
     * @see org.jetlinks.community.rule.engine.scene.term.TermType
     * @see org.jetlinks.community.rule.engine.scene.value.TermValue
     */
    @Schema(description = "触发条件")
    private List<Term> terms;

    @Schema(description = "是否并行执行动作")
    private boolean parallel;

    @Schema(description = "执行动作")
    private List<SceneAction> actions;

    @Schema(description = "说明")
    private String description;

    public SqlRequest createSql() {
        if (trigger != null && trigger.getType() == TriggerType.device) {
            return trigger.getDevice().createSql(terms);
        }

        return EmptySqlRequest.INSTANCE;
    }

    private Flux<Variable> createSceneVariables(List<TermColumn> columns) {
        return LocaleUtils
            .currentReactive()
            .flatMapIterable(locale -> LocaleUtils
                .doWith(terms,
                        locale,
                        (terms, l) -> {
                            Variable variable = Variable
                                .of("scene", LocaleUtils.resolveMessage(
                                    "message.scene_trigger_" + trigger.getType().name() + "_output",
                                    trigger.getType().getText() + "输出"
                                ));

                            List<Variable> defaultVariables = createDefaultVariable();
                            List<Variable> termVar = SceneUtils.parseVariable(terms, columns);
                            List<Variable> variables = new ArrayList<>(defaultVariables.size() + termVar.size());

                            //设备触发但是没有指定条件,以下是内置的输出参数
                            if (CollectionUtils.isEmpty(termVar) && trigger.getType() == TriggerType.device) {
                                variables.add(Variable
                                                  .of("_now",
                                                      LocaleUtils.resolveMessage(
                                                          "message.scene_term_column_now",
                                                          "服务器时间"))
                                                  .withType(DateTimeType.ID));
                                variables.add(Variable
                                                  .of("timestamp",
                                                      LocaleUtils.resolveMessage(
                                                          "message.scene_term_column_timestamp",
                                                          "数据上报时间"))
                                                  .withType(DateTimeType.ID));
                            }

                            variables.addAll(defaultVariables);

                            variables.addAll(termVar);

                            variable.setChildren(variables);
                            return Collections.singletonList(variable);
                        }));
    }

    public Flux<Variable> createVariables(List<TermColumn> columns,
                                          Integer actionIndex,
                                          DeviceRegistry registry) {
        Flux<Variable> variables = createSceneVariables(columns);

        //执行动作会输出的变量,串行执行才会生效
        if (!parallel && actionIndex != null && CollectionUtils.isNotEmpty(actions)) {

            for (int i = 0; i < Math.min(actions.size(), actionIndex + 1); i++) {
                variables = variables.concatWith(actions.get(i).createVariables(registry, i));
            }

        }
        return variables
            .doOnNext(Variable::refactorPrefix);
    }

    public List<Variable> createDefaultVariable() {
        return trigger != null
            ? trigger.createDefaultVariable()
            : Collections.emptyList();
    }

    public SceneRule where(String expression) {
        setTerms(TermExpressionParser.parse(expression));
        return this;
    }

    public RuleModel toModel() {
        validate();
        RuleModel model = new RuleModel();
        model.setId(id);
        model.setName(name);
        model.setType("scene");

        RuleNodeModel sceneNode = new RuleNodeModel();
        sceneNode.setId(id);
        sceneNode.setName(name);
        sceneNode.setConfiguration(FastBeanCopier.copy(this, new HashMap<>()));
        sceneNode.setExecutor(SceneTaskExecutorProvider.EXECUTOR);

        //传递数据到下级节点
        sceneNode.addConfiguration(AbstractExecutionContext.RECORD_DATA_TO_HEADER, true);
        sceneNode.addConfiguration(AbstractExecutionContext.RECORD_DATA_TO_HEADER_KEY, "scene");

        //触发器
        trigger.applyModel(model, sceneNode);
        model.getNodes().add(sceneNode);
        if (CollectionUtils.isNotEmpty(actions)) {

            int index = 1;
            RuleNodeModel preNode = null;
            SceneAction preAction = null;
            for (SceneAction action : actions) {
                RuleNodeModel actionNode = new RuleNodeModel();
                actionNode.setId("action_" + index);
                actionNode.setName("动作_" + index);
                action.applyNode(actionNode);
                //并行
                if (parallel) {
                    model.link(sceneNode, actionNode);
                }
                //串行
                else {
                    //串行的时候 标记记录每一个动作的数据到header中，用于进行条件判断或者数据引用
                    actionNode.addConfiguration(AbstractExecutionContext.RECORD_DATA_TO_HEADER, true);
                    actionNode.addConfiguration(AbstractExecutionContext.RECORD_DATA_TO_HEADER_KEY, actionNode.getId());

                    if (preNode == null) {
                        //场景节点->第一个动作节点
                        model.link(sceneNode, preNode = actionNode);
                    } else {
                        //上一个节点->当前动作节点
                        RuleLink link = model.link(preNode, actionNode);
                        //设置上一个节点到此节点的输出条件
                        if (CollectionUtils.isNotEmpty(preAction.getTerms())) {
                            link.setCondition(TermsConditionEvaluator.createCondition(preAction.getTerms()));
                        }
                        preNode = actionNode;
                    }
                }
                model.getNodes().add(actionNode);
                preAction = action;
                index++;
            }
        }

        return model;

    }

    public void validate() {
        ValidatorUtils.tryValidate(this);
        trigger.validate();

    }
}
