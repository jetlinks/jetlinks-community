package org.jetlinks.community.rule.engine.scene;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.collections4.CollectionUtils;
import org.hswebframework.ezorm.core.param.Term;
import org.hswebframework.ezorm.rdb.executor.EmptySqlRequest;
import org.hswebframework.ezorm.rdb.executor.SqlRequest;
import org.hswebframework.ezorm.rdb.operator.builder.fragments.SqlFragments;
import org.hswebframework.web.api.crud.entity.TermExpressionParser;
import org.hswebframework.web.bean.FastBeanCopier;
import org.hswebframework.web.i18n.LocaleUtils;
import org.hswebframework.web.validator.ValidatorUtils;
import org.jetlinks.community.rule.engine.scene.internal.triggers.ManualTriggerProvider;
import org.jetlinks.core.device.DeviceRegistry;
import org.jetlinks.core.metadata.types.DateTimeType;
import org.jetlinks.core.trace.MonoTracer;
import org.jetlinks.core.trace.TraceHolder;
import org.jetlinks.core.utils.Reactors;
import org.jetlinks.community.reactorql.term.TermTypes;
import org.jetlinks.community.rule.engine.commons.ShakeLimit;
import org.jetlinks.community.rule.engine.commons.TermsConditionEvaluator;
import org.jetlinks.community.rule.engine.scene.term.TermColumn;
import org.jetlinks.community.rule.engine.scene.term.limit.ShakeLimitGrouping;
import org.jetlinks.reactor.ql.DefaultReactorQLContext;
import org.jetlinks.reactor.ql.ReactorQL;
import org.jetlinks.reactor.ql.ReactorQLContext;
import org.jetlinks.rule.engine.api.RuleConstants;
import org.jetlinks.rule.engine.api.RuleData;
import org.jetlinks.rule.engine.api.model.RuleLink;
import org.jetlinks.rule.engine.api.model.RuleModel;
import org.jetlinks.rule.engine.api.model.RuleNodeModel;
import org.jetlinks.rule.engine.defaults.AbstractExecutionContext;
import reactor.core.Disposable;
import reactor.core.Disposables;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.function.Function3;
import reactor.util.concurrent.Queues;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.*;
import java.util.function.Function;

@Getter
@Setter
public class SceneRule implements Serializable {

    public static final String ACTION_KEY_BRANCH_INDEX = "_branchIndex";
    public static final String ACTION_KEY_GROUP_INDEX = "_groupIndex";
    public static final String ACTION_KEY_ACTION_INDEX = "_actionIndex";

    public static final String CONTEXT_KEY_SCENE_OUTPUT = "scene";

    public static final String SOURCE_TYPE_KEY = "sourceType";
    public static final String SOURCE_ID_KEY = "sourceId";
    public static final String SOURCE_NAME_KEY = "sourceName";


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
     * @see org.jetlinks.community.rule.engine.scene.term.TermColumn
     * @see org.jetlinks.community.reactorql.term.TermType
     * @see org.jetlinks.community.rule.engine.scene.value.TermValue
     */
    @Schema(description = "触发条件")
    private List<Term> terms;

    @Schema(description = "是否并行执行动作")
    private boolean parallel;

    @Schema(description = "执行动作")
    private List<SceneAction> actions;

    @Schema(description = "动作分支")
    private List<SceneConditionAction> branches;

    @Schema(description = "扩展配置")
    private Map<String, Object> options;

    @Schema(description = "说明")
    private String description;

    public SqlRequest createSql(boolean hasWhere) {
        if (trigger != null) {
            return trigger.createSql(getTermList(), hasWhere);
        }
        return EmptySqlRequest.INSTANCE;
    }

    private List<Term> getTermList() {
        List<Term> terms = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(this.terms)) {
            terms.addAll(this.terms);
        }
        if (CollectionUtils.isNotEmpty(this.branches)) {
            for (SceneConditionAction branch : branches) {
                terms.addAll(branch.createContextTerm());
            }
        }
        return terms;
    }

    public Function<Map<String, Object>, Mono<Boolean>> createDefaultFilter(List<Term> terms) {
        if (trigger != null) {
            return createDefaultFilter(trigger.createFilter(terms));
        }
        return ignore -> Reactors.ALWAYS_TRUE;
    }

    public static String DEFAULT_FILTER_TABLE = "t";

    public Function<Map<String, Object>, Mono<Boolean>> createDefaultFilter(SqlFragments fragments) {
        if (!fragments.isEmpty()) {
            SqlRequest request = fragments.toRequest();
            String sql = "select 1 from " + DEFAULT_FILTER_TABLE + " where " + request.getSql();
            ReactorQL ql = ReactorQL
                .builder()
                .sql(sql)
                .build();
            List<Object> args = Arrays.asList(request.getParameters());
            String sqlString = request.toNativeSql();
            return new Function<Map<String, Object>, Mono<Boolean>>() {
                @Override
                public Mono<Boolean> apply(Map<String, Object> map) {
                    ReactorQLContext context = new DefaultReactorQLContext((t) -> Flux.just(map), args);
                    return ql
                        .start(context)
                        .hasElements();
                }

                @Override
                public String toString() {
                    return sqlString;
                }
            };
        }

        return ignore -> Reactors.ALWAYS_TRUE;
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
                                    "message.scene_trigger_" + trigger.getType() + "_output",
                                    trigger.getTypeName() + "输出的数据"
                                ));

                            List<Variable> defaultVariables = createDefaultVariable();
                            List<Variable> termVar = SceneUtils.parseVariable(terms, columns);
                            List<Variable> variables = new ArrayList<>(defaultVariables.size() + termVar.size());

                            variables.addAll(defaultVariables);

                            variables.addAll(termVar);

                            variable.setChildren(variables);
                            return Collections.singletonList(variable);
                        }));
    }

    public Flux<Variable> createVariables(List<TermColumn> columns,
                                          Integer branchIndex,
                                          Integer branchGroupIndex,
                                          Integer actionIndex) {
        Flux<Variable> variables = createSceneVariables(columns);

        //执行动作会输出的变量,串行执行才会生效
        if (branchIndex == null && !parallel && actionIndex != null && CollectionUtils.isNotEmpty(actions)) {

            for (int i = 0; i < Math.min(actions.size(), actionIndex + 1); i++) {
                variables = variables.concatWith(actions
                                                     .get(i)
                                                     .createVariables(null, branchGroupIndex, i + 1));
            }
        }
        //分支条件
        if (branchIndex != null && branchGroupIndex != null && CollectionUtils.isNotEmpty(branches) && branches.size() > branchIndex) {
            SceneConditionAction branch = branches.get(branchIndex);
            SceneActions then = branch.getThen() != null && branch.getThen().size() > branchGroupIndex
                ? branch.getThen().get(branchGroupIndex) : null;
            List<SceneAction> actionList;
            if (then != null && !then.isParallel() &&
                CollectionUtils.isNotEmpty(actionList = then.getActions())) {

                for (int i = 0; i < Math.min(actionList.size(), actionIndex + 1); i++) {
                    variables = variables.concatWith(actionList
                                                         .get(i)
                                                         .createVariables(branchIndex + 1, branchGroupIndex + 1, i + 1));
                }

            }
        }

        return variables
            .doOnNext(Variable::refactorPrefix);
    }

    public static String createBranchActionId(int branchIndex, int groupId, int actionIndex) {
        return "branch_" + branchIndex + "_group_" + groupId + "_action_" + actionIndex;
    }

    public Disposable createBranchHandler(Flux<Map<String, Object>> sourceData,
                                          Function3<Integer, String, Map<String, Object>, Mono<Void>> output) {
        if (CollectionUtils.isEmpty(branches)) {
            return Disposables.disposed();
        }

        String id = this.id;
        String name = this.name;

        Function<Map<String, Object>, Mono<Boolean>> last = null;

        Disposable.Composite disposable = Disposables.composite();
        int branchIndex = 0;
        for (SceneConditionAction branch : branches) {
            int _branchIndex = ++branchIndex;
            //执行条件
            Function<Map<String, Object>, Mono<Boolean>> filter = createDefaultFilter(branch.getWhen());
            //满足条件后的输出操作
            List<Function<Map<String, Object>, Mono<Void>>> outs = new ArrayList<>();

            List<SceneActions> groups = branch.getThen();
            int thenIndex = 0;
            if (CollectionUtils.isNotEmpty(groups)) {

                for (SceneActions then : groups) {
                    thenIndex++;

                    Function<Map<String, Object>, Mono<Void>> out;

                    int size = then.getActions().size();
                    if (size == 0) {
                        continue;
                    }
                    //串行，只传递到第一个动作
                    if (!then.isParallel() || size == 1) {
                        String nodeId = createBranchActionId(_branchIndex, thenIndex, 1);
                        out = data -> output.apply(_branchIndex, nodeId, data);
                    } else {
                        //多个并行执行动作
                        String[] nodeIds = new String[size];
                        for (int i = 0; i < nodeIds.length; i++) {
                            nodeIds[i] = createBranchActionId(_branchIndex, thenIndex, (i + 1));
                        }
                        Flux<String> nodeIdFlux = Flux.fromArray(nodeIds);
                        //并行
                        out = data -> nodeIdFlux
                            .flatMap(nodeId -> output.apply(_branchIndex, nodeId, data))
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
                                                  .group(stream)//先按自定义分组再按时间窗口进行分组
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
                    outs.add(out);
                }
            }


            Flux<Function<Map<String, Object>, Mono<Void>>> outFlux = Flux.fromIterable(outs);

            Function<Map<String, Object>, Mono<Void>> fOut = out -> outFlux.flatMap(fun -> fun.apply(out)).then();


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
        Function<Map<String, Object>, Mono<Boolean>> fLast = last;

        MonoTracer<Boolean> tracer = MonoTracer.create(
            "/rule-runtime/scene/" + id,
            builder -> builder.setAttribute(RuleConstants.Trace.name, name));

        disposable.add(
            sourceData
                .flatMap(data -> fLast
                    .apply(data)
                    .as(tracer)
                    .contextWrite(ctx -> TraceHolder.readToContext(ctx, data)))
                .subscribe()
        );

        return disposable;
    }

    public SceneRule where(String expression) {
        setTerms(TermExpressionParser.parse(expression));
        return this;
    }

    public List<Variable> createDefaultVariable() {
        return trigger != null
            ? trigger.createDefaultVariable()
            : Collections.emptyList();
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
        sceneNode.addConfiguration(AbstractExecutionContext.RECORD_DATA_TO_HEADER_KEY, CONTEXT_KEY_SCENE_OUTPUT);

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
                            link.setCondition(TermsConditionEvaluator.createCondition(trigger.refactorTerm("this", preAction.getTerms())));
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

                List<SceneActions> group = branch.getThen();

                if (CollectionUtils.isNotEmpty(group)) {
                    int groupIndex = 0;
                    for (SceneActions actions : group) {
                        groupIndex++;
                        if (actions != null && CollectionUtils.isNotEmpty(actions.getActions())) {
                            int actionIndex = 1;
                            RuleNodeModel preNode = null;
                            SceneAction preAction = null;
                            for (SceneAction action : actions.getActions()) {
                                RuleNodeModel actionNode = new RuleNodeModel();
                                actionNode.setId(createBranchActionId(branchIndex, groupIndex, actionIndex));
                                actionNode.setName("条件" + branchIndex + "_分组" + groupIndex + "_动作" + actionIndex);

                                action.applyNode(actionNode);
                                //串行
                                if (!actions.isParallel()) {
                                    //串行的时候 标记记录每一个动作的数据到header中，用于进行条件判断或者数据引用
                                    actionNode.addConfiguration(RuleData.RECORD_DATA_TO_HEADER, true);
                                    actionNode.addConfiguration(RuleData.RECORD_DATA_TO_HEADER_KEY, actionNode.getId());
                                    actionNode.addConfiguration(ACTION_KEY_BRANCH_INDEX, branchIndex);
                                    actionNode.addConfiguration(ACTION_KEY_GROUP_INDEX, groupIndex);
                                    actionNode.addConfiguration(ACTION_KEY_ACTION_INDEX, actionIndex);

                                    if (preNode != null) {
                                        //上一个节点->当前动作节点
                                        RuleLink link = model.link(preNode, actionNode);
                                        //设置上一个节点到此节点的输出条件
                                        if (CollectionUtils.isNotEmpty(preAction.getTerms())) {
                                            link.setCondition(TermsConditionEvaluator.createCondition(trigger.refactorTerm("this", preAction.getTerms())));
                                        }
                                    } else if (Objects.equals(trigger.getType(), ManualTriggerProvider.PROVIDER)) {
                                        model.link(sceneNode, actionNode);
                                    }

                                    preNode = actionNode;
                                } else {
                                    if (Objects.equals(trigger.getType(), ManualTriggerProvider.PROVIDER)) {
                                        model.link(sceneNode, actionNode);
                                    }
                                }

                                model.getNodes().add(actionNode);
                                preAction = action;
                                actionIndex++;
                            }
                        }
                    }
                }


            }
        }

        return model;

    }

    public void validate() {
        ValidatorUtils.tryValidate(this);
    }
}
