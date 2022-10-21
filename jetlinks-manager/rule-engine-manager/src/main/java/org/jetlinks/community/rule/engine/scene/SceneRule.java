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
import org.jetlinks.core.utils.Reactors;
import org.jetlinks.community.rule.engine.commons.ShakeLimit;
import org.jetlinks.community.rule.engine.commons.TermsConditionEvaluator;
import org.jetlinks.community.rule.engine.scene.term.TermColumn;
import org.jetlinks.community.rule.engine.scene.term.limit.ShakeLimitGrouping;
import org.jetlinks.rule.engine.api.model.RuleLink;
import org.jetlinks.rule.engine.api.model.RuleModel;
import org.jetlinks.rule.engine.api.model.RuleNodeModel;
import org.jetlinks.rule.engine.defaults.AbstractExecutionContext;
import reactor.core.Disposable;
import reactor.core.Disposables;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.util.concurrent.Queues;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;

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

    @Schema(description = "触发条件")
    private List<Term> terms;

    @Schema(description = "是否并行执行动作")
    private boolean parallel;

    @Schema(description = "执行动作")
    private List<SceneAction> actions;

    @Schema(description = "动作分支")
    private List<SceneConditionAction> branches;

    @Schema(description = "说明")
    private String description;

    public SqlRequest createSql(boolean hasWhere) {
        if (trigger != null && trigger.getType() == TriggerType.device) {
            return trigger.getDevice().createSql(terms, hasWhere);
        }

        return EmptySqlRequest.INSTANCE;
    }

    public Function<Map<String, Object>, Mono<Boolean>> createFilter(List<Term> terms) {
        if (trigger != null && trigger.getType() == TriggerType.device) {
            return trigger.getDevice().createFilter(terms);
        }

        return ignore -> Reactors.ALWAYS_TRUE;
    }

    String createFilterDescription(List<Term> terms) {
        if (trigger != null && trigger.getType() == TriggerType.device) {
            return trigger.getDevice().createFilterDescription(terms);
        }

        return "true";
    }

    public ShakeLimitGrouping<Map<String, Object>> createGrouping() {
        //todo 其他分组方式实现
        return flux -> flux
            .groupBy(map -> map.getOrDefault("deviceId", "null"), Integer.MAX_VALUE);
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

                            //设备触发但是没有指定条件,或者其它触发类型,以下是内置的输出参数
                            if (trigger.getType() != TriggerType.device || CollectionUtils.isEmpty(termVar)) {
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
                                          Integer branchIndex,
                                          Integer actionIndex,
                                          DeviceRegistry registry) {
        Flux<Variable> variables = createSceneVariables(columns);

        //执行动作会输出的变量,串行执行才会生效
        if (branchIndex == null && !parallel && actionIndex != null && CollectionUtils.isNotEmpty(actions)) {

            for (int i = 0; i < Math.min(actions.size(), actionIndex + 1); i++) {
                variables = variables.concatWith(actions.get(i).createVariables(registry, i));
            }
        }
        //分支条件
        if (branchIndex != null && CollectionUtils.isNotEmpty(branches) && branches.size() > branchIndex) {
            SceneConditionAction branch = branches.get(branchIndex);
            List<SceneAction> actionList;
            if (branch.getThen() != null && !branch.getThen().isParallel() &&

                CollectionUtils.isNotEmpty(actionList = branch.getThen().getActions())) {

                for (int i = 0; i < Math.min(actionList.size(), actionIndex + 1); i++) {
                    variables = variables.concatWith(actionList.get(i).createVariables(registry, i));
                }

            }
        }

        return variables
            .doOnNext(Variable::refactorPrefix);
    }

    public Disposable createBranchHandler(Flux<Map<String, Object>> sourceData,
                                          BiFunction<String, Map<String, Object>, Mono<Void>> output) {
        if (CollectionUtils.isEmpty(branches)) {
            return Disposables.disposed();
        }

        Function<Map<String, Object>, Mono<Boolean>> last = null;

        Disposable.Composite disposable = Disposables.composite();
        int branchIndex = 0;
        for (SceneConditionAction branch : branches) {
            int _branchIndex = ++branchIndex;
            //执行条件
            Function<Map<String, Object>, Mono<Boolean>> filter = createFilter(branch.getWhen());
            //满足条件后的输出操作
            Function<Map<String, Object>, Mono<Void>> out;

            SceneActions then = branch.getThen();
            //执行动作
            if (then != null && CollectionUtils.isNotEmpty(then.getActions())) {

                int size = then.getActions().size();
                //串行，只传递到第一个动作
                if (!then.isParallel() || size == 1) {
                    String nodeId = "branch_" + _branchIndex + "_action_1";
                    out = data -> output.apply(nodeId, data);
                } else {
                    //多个并行执行动作
                    String[] nodeIds = new String[size];
                    for (int i = 0; i < nodeIds.length; i++) {
                        nodeIds[0] = "branch_" + _branchIndex + "_action_" + (i + 1);
                    }
                    Flux<String> nodeIdFlux = Flux.fromArray(nodeIds);
                    //并行
                    out = data -> nodeIdFlux
                        .flatMap(nodeId -> output.apply(nodeId, data))
                        .then();
                }
                //防抖
                ShakeLimit shakeLimit = branch.getShakeLimit();
                if (shakeLimit != null && shakeLimit.isEnabled()) {

                    Sinks.Many<Map<String, Object>> sinks = Sinks
                        .many()
                        .unicast()
                        .onBackpressureBuffer(Queues.<Map<String, Object>>unboundedMultiproducer().get());

                    //分组方式,比如设备触发时,应该按设备分组,每个设备都走独立的防抖策略
                    ShakeLimitGrouping<Map<String, Object>> grouping = createGrouping();

                    Function<Map<String, Object>, Mono<Void>> handler = out;

                    disposable.add(
                        shakeLimit
                            .transfer(sinks.asFlux(),
                                      (duration, stream) ->
                                          grouping
                                              .group(stream)//先按自定义分组再按事件窗口进行分组
                                              .flatMap(group -> group.window(duration), Integer.MAX_VALUE),
                                      (map, total) -> map.put("_total", total))
                            .flatMap(handler)
                            .subscribe()
                    );
                    //输出到sink进行防抖控制
                    out = data -> {
                        sinks.emitNext(data, Reactors.emitFailureHandler());
                        return Mono.empty();
                    };
                }
            } else {
                out = ignore -> Mono.empty();
            }

            Function<Map<String, Object>, Mono<Void>> fOut = out;


            Function<Map<String, Object>, Mono<Boolean>> handler =
                data -> filter
                    .apply(data)
                    .flatMap(match -> {
                        // 满足条件后执行输出
                        if (match) {
                            return fOut.apply(data).thenReturn(true);
                        }
                        return Reactors.ALWAYS_FALSE;
                    });

            if (last == null) {
                last = handler;
            } else {
                Function<Map<String, Object>, Mono<Boolean>> _last = last;

                last = data -> _last
                    .apply(data)
                    .flatMap(match -> {
                        //上一个分支满足了则返回,不执行此分支逻辑
                        if (match) {
                            return Reactors.ALWAYS_FALSE;
                        }
                        return handler.apply(data);
                    });
            }
        }
        //never happen
        if (last == null) {
            disposable.dispose();
            throw new IllegalArgumentException();
        }

        disposable.add(
            sourceData.flatMap(last).subscribe()
        );

        return disposable;
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

        //使用分支条件时
        if (CollectionUtils.isNotEmpty(branches)) {
            int branchIndex = 0;
            for (SceneConditionAction branch : branches) {
                branchIndex++;

                SceneActions actions = branch.getThen();
                if (actions != null && CollectionUtils.isNotEmpty(actions.getActions())) {
                    int actionIndex = 1;
                    RuleNodeModel preNode = null;
                    SceneAction preAction = null;
                    for (SceneAction action : actions.getActions()) {
                        RuleNodeModel actionNode = new RuleNodeModel();
                        actionNode.setId("branch_" + branchIndex + "_action_" + actionIndex);
                        actionNode.setName("条件_" + branchIndex + "_动作_" + actionIndex);

                        action.applyNode(actionNode);
                        //串行
                        if (!actions.isParallel()) {
                            //串行的时候 标记记录每一个动作的数据到header中，用于进行条件判断或者数据引用
                            actionNode.addConfiguration(AbstractExecutionContext.RECORD_DATA_TO_HEADER, true);
                            actionNode.addConfiguration(AbstractExecutionContext.RECORD_DATA_TO_HEADER_KEY, actionNode.getId());

                            if (preNode != null) {
                                //上一个节点->当前动作节点
                                RuleLink link = model.link(preNode, actionNode);
                                //设置上一个节点到此节点的输出条件
                                if (CollectionUtils.isNotEmpty(preAction.getTerms())) {
                                    link.setCondition(TermsConditionEvaluator.createCondition(preAction.getTerms()));
                                }
                            }

                            preNode = actionNode;
                        }

                        model.getNodes().add(actionNode);
                        preAction = action;
                        actionIndex++;
                    }
                }
            }
        }

        return model;

    }

    public void validate() {
        ValidatorUtils.tryValidate(this);
        trigger.validate();

    }
}
