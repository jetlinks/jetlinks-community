package org.jetlinks.community.rule.engine.scene;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.hswebframework.ezorm.core.param.Term;
import org.hswebframework.ezorm.core.param.TermType;
import org.hswebframework.ezorm.rdb.executor.EmptySqlRequest;
import org.hswebframework.ezorm.rdb.executor.SqlRequest;
import org.hswebframework.ezorm.rdb.operator.builder.fragments.NativeSql;
import org.hswebframework.ezorm.rdb.operator.builder.fragments.SqlFragments;
import org.hswebframework.web.api.crud.entity.TermExpressionParser;
import org.hswebframework.web.bean.FastBeanCopier;
import org.hswebframework.web.i18n.LocaleUtils;
import org.hswebframework.web.utils.DigestUtils;
import org.hswebframework.web.validator.ValidatorUtils;
import org.jetlinks.core.trace.MonoTracer;
import org.jetlinks.core.trace.TraceHolder;
import org.jetlinks.core.utils.NamedFunction;
import org.jetlinks.core.utils.Reactors;
import org.jetlinks.community.rule.engine.commons.ShakeLimit;
import org.jetlinks.community.rule.engine.commons.TermsConditionEvaluator;
import org.jetlinks.community.rule.engine.enums.SceneFeature;
import org.jetlinks.community.rule.engine.scene.internal.triggers.ManualTriggerProvider;
import org.jetlinks.community.rule.engine.scene.term.TermColumn;
import org.jetlinks.community.terms.TermSpec;
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
@Slf4j
public class SceneRule implements Serializable {
    static final Function<Map<String, Object>, Mono<Boolean>> FILTER_TRUE =
        NamedFunction.of("true", ignore -> Reactors.ALWAYS_TRUE);

    public static final String ACTION_KEY_BRANCH_ID = "_branchId";
    public static final String ACTION_KEY_BRANCH_INDEX = "_branchIndex";
    public static final String ACTION_KEY_GROUP_INDEX = "_groupIndex";
    public static final String ACTION_KEY_ACTION_ID = "_actionId";
    public static final String ACTION_KEY_ACTION_INDEX = "_actionIndex";

    public static final String CONTEXT_KEY_SCENE_OUTPUT = "scene";

    public static final String SOURCE_TYPE_KEY = "sourceType";
    public static final String SOURCE_ID_KEY = "sourceId";
    public static final String SOURCE_NAME_KEY = "sourceName";


    @Schema(description = "场景ID")
    @NotBlank(message = "error.scene_rule_id_cannot_be_blank")
    private String id;

    @Schema(description = "场景名称")
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

    @Schema(description = "扩展配置")
    private Map<String, Object> options;

    @Schema(description = "场景特性")
    private SceneFeature[] features;

    @Schema(description = "说明")
    private String description;

    public SqlRequest createSql(boolean hasWhere) {
        if (trigger != null) {
            return TraceHolder
                .traceBlocking("/scene/create-sql", span -> {
                    SqlRequest request = trigger.createSql(getTermList(), hasWhere);
                    if (!(request instanceof EmptySqlRequest)) {
                        span.setAttribute("sql", request.toNativeSql());
                    }
                    return request;
                });
        }
        return EmptySqlRequest.INSTANCE;
    }

    public Function<Map<String, Object>, Mono<Boolean>> createDefaultFilter(List<Term> terms) {
        if (trigger != null) {
            return createDefaultFilter(trigger.createFilter(terms));
        }
        return FILTER_TRUE;
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
            return NamedFunction
                .of(sqlString, map -> {
                    ReactorQLContext context = new DefaultReactorQLContext((t) -> Flux.just(map), args);
                    return ql
                        .start(context)
                        .hasElements();
                });
        }
        return FILTER_TRUE;
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

    static String createBranchActionId(int branchIndex, int groupId, int actionIndex) {
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
            Function<Map<String, Object>, Mono<Boolean>> filter = TraceHolder
                .traceBlocking("/scene/create-branch-filter",
                               span -> {
                                   Function<Map<String, Object>, Mono<Boolean>>
                                       f = createDefaultFilter(branch.getWhen());
                                   span.setAttribute("filter", f.toString());
                                   span.setAttribute("branch", _branchIndex);
                                   return f;
                               });
            //满足条件后的输出操作
            List<Function<Map<String, Object>, Mono<Void>>> outs = new ArrayList<>();

            List<SceneActions> groups = branch.getThen();
            int thenIndex = 0;
            if (CollectionUtils.isNotEmpty(groups)) {
                List<Function<Map<String, Object>, Mono<Void>>> actionOuts = new ArrayList<>();
                //执行动作
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
                    actionOuts.add(out);
                }

                //防抖
                ShakeLimit shakeLimit = branch.getShakeLimit();
                if (shakeLimit != null && shakeLimit.isEnabled()) {
                    Sinks.Many<Map<String, Object>> sinks = Sinks
                        .many()
                        .unicast()
                        .onBackpressureBuffer(Queues.<Map<String, Object>>unboundedMultiproducer().get());

                    //动作输出
                    Flux<Function<Map<String, Object>, Mono<Void>>> _outs = Flux.fromIterable(new ArrayList<>(actionOuts));
                    Function<Map<String, Object>, Mono<Void>> handler =
                        map -> _outs.flatMap(call -> call.apply(map)).then();

                    //防抖
                    disposable.add(
                        trigger
                            .provider()
                            .shakeLimit(DigestUtils.md5Hex(id + ":" + _branchIndex),
                                        sinks.asFlux(),
                                        shakeLimit)
                            .flatMap(res -> {
                                res.getElement().put("_total", res.getTimes());
                                return handler.apply(res.getElement());
                            })
                            .subscribe()
                    );
                    //满足输出给防抖策略
                    actionOuts.clear();
                    actionOuts.add(data -> {
                        sinks.emitNext(data, Reactors.emitFailureHandler());
                        return Mono.empty();
                    });
                }

                //动作输出
                outs.addAll(actionOuts);
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
                boolean executeAnyway = branch.isExecuteAnyway();
                Function<Map<String, Object>, Mono<Boolean>> _last = last;

                last = data -> _last
                    .apply(data)
                    .flatMap(match -> {
                        //无论如何都尝试执行当前分支
                        if (executeAnyway) {
                            return handler.apply(data);
                        }
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

    public List<Variable> createDefaultVariable() {
        return trigger != null
            ? trigger.createDefaultVariable()
            : Collections.emptyList();
    }

    public SceneRule where(String expression) {
        setTerms(TermExpressionParser.parse(expression));
        return this;
    }


    public Mono<RuleModel> toModel() {
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
                            link.setCondition(TermsConditionEvaluator.createCondition(trigger.refactorTerm("this", preAction
                                .getTerms())));
                        }
                        preNode = actionNode;
                    }
                }
                model.getNodes().add(actionNode);
                preAction = action;
                index++;
            }
        }

        List<Mono<?>> async = new ArrayList<>();

        Mono<List<TermColumn>> columns = trigger
            .parseTermColumns()
            .collectList()
            .cache();

        Mono<Map<String, Variable>> columnMapping =
            columns
                .flatMapMany(this::createSceneVariables)
                .expand(var -> var.getChildren() == null ? Flux.empty() : Flux.fromIterable(var.getChildren()))
                .collectMap(Variable::getColumn, Function.identity())
                .cache();


        //使用分支条件时
        if (CollectionUtils.isNotEmpty(branches)) {
            int branchIndex = 0;
            Mono<List<TermSpec>> branchTermSpec = null;
            for (SceneConditionAction branch : branches) {
                int branchId = branch.getBranchId() == null ? branchIndex : branch.getBranchId();
                branchIndex++;

                List<SceneActions> group = branch.getThen();
                Mono<List<TermSpec>> lastTerm = branchTermSpec;
                //场景输出的条件描述
                branchTermSpec = columnMapping
                    .flatMap(mapping -> trigger
                        .createFilterSpec(
                            branch.getWhen(),
                            (term, spec) -> applyTermSpec(term, spec, mapping)))
                    .flatMap(list -> {
                        TermSpec spec = new TermSpec();
                        //最后一个分支? else
                        if (branch.getWhen().isEmpty()
                            && lastTerm != null
                            && !branch.isExecuteAnyway()) {
                            spec.setTermType(TermType.not);
                            return lastTerm
                                .doOnNext(spec::setChildren)
                                .thenReturn(Collections.singletonList(spec));
                        }
                        spec.setChildren(list);
                        return Mono.just(Collections.singletonList(spec));
                    })
                    .cache();


                if (CollectionUtils.isNotEmpty(group)) {
                    int groupIndex = 0;
                    for (SceneActions actions : group) {
                        groupIndex++;
                        if (actions != null && CollectionUtils.isNotEmpty(actions.getActions())) {
                            int actionIndex = 1;
                            RuleNodeModel preNode = null;
                            SceneAction preAction = null;
                            Mono<List<TermSpec>> groupTerm = branchTermSpec;

                            for (SceneAction action : actions.getActions()) {

                                int finalBranchIndex = branchIndex - 1,
                                    finalGroupIndex = groupIndex - 1,
                                    finalActionIndex = actionIndex - 1;

                                int actionId = action.getActionId() == null ? actionIndex : action.getActionId();

                                //变量信息
                                Mono<Map<String, Variable>> groupVar = columns
                                    .flatMapMany(termColumns -> this
                                        .createVariables(termColumns, finalBranchIndex, finalGroupIndex, finalActionIndex))
                                    .expand(var -> var.getChildren() == null
                                        ? Flux.empty()
                                        : Flux.fromIterable(var.getChildren()))
                                    .collectMap(Variable::getColumn, Function.identity());

                                RuleNodeModel actionNode = new RuleNodeModel();
                                actionNode.setId(createBranchActionId(branchIndex, groupIndex, actionIndex));
                                actionNode.setName("条件" + branchIndex + "_分组" + groupIndex + "_动作" + actionIndex);

                                action.applyNode(actionNode);
                                //串行
                                actionNode.addConfiguration(ACTION_KEY_BRANCH_ID, branchId);
                                actionNode.addConfiguration(ACTION_KEY_ACTION_ID, actionId);
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
                                            List<Term> termList = preAction.getTerms();
                                            //合并上一个节点输出的变量
                                            groupTerm = Mono.zip(
                                                groupTerm.<List<TermSpec>>map(ArrayList::new),
                                                groupVar,
                                                (parent, mapping) -> {
                                                    TermSpec childSpec = new TermSpec();
                                                    childSpec
                                                        .setChildren(
                                                            TermSpec.of(
                                                                termList,
                                                                (term, spec) -> applyTermSpec(term, spec, mapping))
                                                        );
                                                    parent.add(childSpec);
                                                    return parent;
                                                });

                                            link.setCondition(TermsConditionEvaluator.createCondition(
                                                trigger.refactorTerm("this", termList)));
                                        }

                                    } else if (Objects.equals(trigger.getType(), ManualTriggerProvider.PROVIDER)) {
                                        model.link(sceneNode, actionNode);
                                    }
                                } else {
                                    if (Objects.equals(trigger.getType(), ManualTriggerProvider.PROVIDER)) {
                                        model.link(sceneNode, actionNode);
                                    }
                                }

                                groupTerm = groupTerm
                                    .doOnNext(arr -> {
                                        log.debug(
                                            "scene[{}] action[{}] term spec: {}",
                                            getId(),
                                            actionNode.getId(),
                                            arr);
                                        action.applyFilterSpec(actionNode, arr);
                                    })
                                    .as(MonoTracer.create(
                                        "/scene/branch_filter_spec",
                                        (span, next) -> {
                                            span.setAttribute("spec", TermSpec.toString(next));
                                            span.setAttribute("actionId", actionNode.getId());
                                        }))
                                    .cache();

                                preNode = actionNode;

                                model.getNodes().add(actionNode);
                                preAction = action;
                                actionIndex++;
                            }
                            async.add(groupTerm);
                        }
                    }

                }
            }
        }

        return Flux
            .concat(async)
            .then(Mono.just(model));

    }

    public void validate() {
        ValidatorUtils.tryValidate(this);
    }

    private void applyTermSpec(Term term, TermSpec spec, Map<String, Variable> mapping) {
        Variable var = mapping.get(term.getColumn());
        Term newTerm = trigger.refactorTerm("this", term.clone());

        Object newValue = newTerm.getValue();
        //期望值是另外一个变量
        if (newValue instanceof NativeSql) {
            spec.setExpectIsExpr(true);
            String sql = ((NativeSql) newValue).getSql();
            //sql语法格式特殊如: this['temp_current']
            //只需要里面的temp_current作为变量表达式
            if (sql.contains("['") && sql.endsWith("']")) {
                spec.setExpected(sql.substring(sql.indexOf("['") + 2, sql.length() - 2));
            }
        } else {
            spec.setExpected(newTerm.getValue());
        }

        if (var != null) {
            spec.setColumn(var.getId());
            spec.setMetadata(var.isMetadata());
            spec.setDisplayCode(var.getFullNameCode());
        }

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

}
