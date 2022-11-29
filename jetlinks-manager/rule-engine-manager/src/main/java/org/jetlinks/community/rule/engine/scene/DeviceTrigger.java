package org.jetlinks.community.rule.engine.scene;

import com.google.common.collect.Sets;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.hswebframework.ezorm.core.param.Term;
import org.hswebframework.ezorm.rdb.executor.PrepareSqlRequest;
import org.hswebframework.ezorm.rdb.executor.SqlRequest;
import org.hswebframework.ezorm.rdb.operator.builder.fragments.AbstractTermsFragmentBuilder;
import org.hswebframework.ezorm.rdb.operator.builder.fragments.EmptySqlFragments;
import org.hswebframework.ezorm.rdb.operator.builder.fragments.NativeSql;
import org.hswebframework.ezorm.rdb.operator.builder.fragments.SqlFragments;
import org.hswebframework.web.bean.FastBeanCopier;
import org.hswebframework.web.i18n.LocaleUtils;
import org.hswebframework.web.validator.ValidatorUtils;
import org.jetlinks.core.device.DeviceRegistry;
import org.jetlinks.core.metadata.DeviceMetadata;
import org.jetlinks.core.utils.Reactors;
import org.jetlinks.community.TimerSpec;
import org.jetlinks.community.rule.engine.executor.DeviceMessageSendTaskExecutorProvider;
import org.jetlinks.community.rule.engine.executor.device.DeviceSelectorSpec;
import org.jetlinks.community.rule.engine.executor.device.SelectorValue;
import org.jetlinks.community.rule.engine.scene.term.TermColumn;
import org.jetlinks.community.rule.engine.scene.term.TermTypeSupport;
import org.jetlinks.community.rule.engine.scene.term.TermTypes;
import org.jetlinks.community.rule.engine.scene.value.TermValue;
import org.jetlinks.reactor.ql.DefaultReactorQLContext;
import org.jetlinks.reactor.ql.ReactorQL;
import org.jetlinks.reactor.ql.ReactorQLContext;
import org.jetlinks.rule.engine.api.model.RuleModel;
import org.jetlinks.rule.engine.api.model.RuleNodeModel;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Getter
@Setter
public class DeviceTrigger extends DeviceSelectorSpec implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "error.scene_rule_trigger_device_product_cannot_be_null")
    @Schema(description = "产品ID")
    private String productId;

    @Schema(description = "操作方式")
    @NotNull(message = "error.scene_rule_trigger_device_operation_cannot_be_null")
    private DeviceOperation operation;

    @Schema(description = "拓展信息")
    private Map<String,Object> options;

    public SqlRequest createSql(List<Term> terms) {
        return createSql(terms, true);
    }

    public SqlRequest createSql(List<Term> terms, boolean hasWhere) {

        Map<String, Term> termsMap = SceneUtils.expandTerm(terms);
        // select * from (
        //   select
        //      this.deviceId deviceId,
        //      now() _now,
        //      this.timestamp timestamp,
        //      this.headers.deviceName deviceName,
        //      this.headers.productId productId,
        //      this.headers.productName productName,
        //      this.properties.temp temp_current,
        //      coalesce(this.properties.temp,device.property.recent(deviceId,'temp')) temp_recent,
        //      property.metric('device',deviceId,'temp','max') temp_metric_max
        // ) t where t.temp_current > t.temp_metric_max and t._now between ? and ?
        Set<String> selectColumns = Sets.newLinkedHashSetWithExpectedSize(10 + termsMap.size());
        selectColumns.add("now() \"_now\"");
        selectColumns.add("this.timestamp \"timestamp\"");
        selectColumns.add("this.deviceId \"deviceId\"");
        selectColumns.add("this.headers.deviceName \"deviceName\"");
        selectColumns.add("this.headers.productId \"productId\"");
        selectColumns.add("this.headers.productName \"productName\"");
        //消息唯一ID
        selectColumns.add("this.headers._uid \"_uid\"");
        //维度绑定信息,如部门等
        selectColumns.add("this.headers.bindings \"_bindings\"");
        //链路追踪ID
        selectColumns.add("this.headers.traceparent \"traceparent\"");

        switch (this.operation.getOperator()) {
            case readProperty:
            case writeProperty:
                selectColumns.add("this.success \"success\"");
            case reportProperty:
                selectColumns.add("this.properties \"properties\"");
                break;
            case reportEvent:
                selectColumns.add("this.data \"data\"");
                break;
            case invokeFunction:
                selectColumns.add("this.success \"success\"");
                selectColumns.add("this['output'] \"output\"");
                break;
        }
        for (Term value : termsMap.values()) {
            String column = value.getColumn();
            if (StringUtils.hasText(value.getColumn())) {
                String selectColumn = createSelectColumn(column);
                if (selectColumn == null) {
                    continue;
                }
                String alias = createColumnAlias(value.getColumn());
                List<TermValue> termValues = TermValue.of(value);
                selectColumns.add(selectColumn + " " + alias);
                for (TermValue termValue : termValues) {
                    if (termValue != null && termValue.getSource() == TermValue.Source.metric) {
                        String property = parseProperty(column);
                        if (null != property) {
                            selectColumns.add(String
                                                  .format("property.metric('device',deviceId,'%s','%s') %s_metric_%s",
                                                          property,
                                                          termValue.getMetric(),
                                                          property,
                                                          termValue.getMetric()));
                        }
                    }
                }
            }
        }

        StringBuilder builder = new StringBuilder();
        builder.append("select * from (\n");
        builder.append("\tselect\n");
        int idx = 0;
        for (String selectColumn : selectColumns) {
            if (idx++ > 0) {
                builder.append(",\n");
            }
            builder.append("\t").append(selectColumn);
        }
        builder.append("\t\nfrom ").append(createFromTable());
        builder.append("\n) t \n");

        if (hasWhere) {
            SqlFragments fragments = terms == null ? EmptySqlFragments.INSTANCE : termBuilder.createTermFragments(this, terms);
            if (!fragments.isEmpty()) {
                SqlRequest request = fragments.toRequest();
                builder.append("where ").append(request.getSql());
            }
            return PrepareSqlRequest.of(builder.toString(), fragments.getParameters().toArray());
        }

        return PrepareSqlRequest.of(builder.toString(), new Object[0]);

    }

    String createFilterDescription(List<Term> terms) {
        SqlFragments fragments = CollectionUtils.isEmpty(terms) ? EmptySqlFragments.INSTANCE : termBuilder.createTermFragments(this, terms);
        return fragments.isEmpty() ? "true" : fragments.toRequest().toNativeSql();
    }

    Function<Map<String, Object>, Mono<Boolean>> createFilter(List<Term> terms) {
        SqlFragments fragments = CollectionUtils.isEmpty(terms) ? EmptySqlFragments.INSTANCE : termBuilder.createTermFragments(this, terms);
        if (!fragments.isEmpty()) {
            SqlRequest request = fragments.toRequest();
            String sql = "select 1 from t where " + request.getSql();
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

    private String createFromTable() {
        String topic = null;

        switch (operation.getOperator()) {
            case reportProperty:
                topic = "/device/" + productId + "/%s/message/property/report";
                break;
            case reportEvent:
                topic = "/device/" + productId + "/%s/message/event/" + operation.getEventId();
                break;
            case online:
                topic = "/device/" + productId + "/%s/online";
                break;
            case offline:
                topic = "/device/" + productId + "/%s/offline";
                break;
        }
        if (null == topic) {
            return "dual";
        }
        String selector = getSelector();
        if (!StringUtils.hasText(selector)) {
            selector = "all";
        }
        switch (selector) {
            case "all":
                topic = String.format(topic, "*");
                break;
            case "fixed":
                String scope = getSelectorValues()
                    .stream()
                    .map(SelectorValue::getValue)
                    .map(String::valueOf)
                    .collect(Collectors.joining(","));
                topic = String.format(topic, scope);
                break;
            default:
                scope = getSelectorValues()
                    .stream()
                    .map(SelectorValue::getValue)
                    .map(String::valueOf)
                    .collect(Collectors.joining(","));
                topic = "/" + selector + "/" + scope + String.format(topic, "*");
                break;
        }
        return "\"" + topic + "\"";
    }

    public static final TermBuilder termBuilder = new TermBuilder();

    static class TermBuilder extends AbstractTermsFragmentBuilder<DeviceTrigger> {

        @Override
        public SqlFragments createTermFragments(DeviceTrigger parameter, List<Term> terms) {
            return super.createTermFragments(parameter, terms);
        }

        @Override
        protected SqlFragments createTermFragments(DeviceTrigger trigger, Term term) {
            String termType = StringUtils.hasText(term.getTermType()) ? term.getTermType() : "is";
            TermTypeSupport support = TermTypes
                .lookupSupport(termType)
                .orElseThrow(() -> new UnsupportedOperationException("unsupported termType " + termType));

            String[] arr = term.getColumn().split("[.]");

            String column;
            if (arr.length > 3 && arr[0].equals("properties")) {
                column = "t['" + createColumnAlias(term.getColumn(), false) + "." + String.join(".", Arrays.copyOfRange(arr, 2, arr.length - 1)) + "']";
            } else {
                column = "t['" + createColumnAlias(term.getColumn(), false) + "']";
            }

            List<TermValue> values = TermValue.of(term);
            if (values.size() == 0) {
                return support.createSql(column, null);
            }
            Object val;
            Function<TermValue, Object> parser = value -> {
                if (value.getSource() == TermValue.Source.manual) {
                    return value.getValue();
                } else {
                    return NativeSql.of("t." + arr[1] + "_metric_" + value.getMetric());
                }
            };
            if (values.size() == 1) {
                val = parser.apply(values.get(0));
            } else {
                val = values
                    .stream()
                    .map(parser)
                    .collect(Collectors.toList());
            }

            return support.createSql(column, val);
        }
    }

    static String parseProperty(String column) {
        String[] arr = column.split("[.]");

        if ("properties".equals(arr[0])) {
            return arr[1];
        }
        return null;
    }

    static String createSelectColumn(String column) {
        if (!column.contains(".")) {
            return null;
        }
        String[] arr = column.split("[.]");
        //properties.temp.current
        if ("properties".equals(arr[0])) {
            try {
                DeviceOperation.PropertyValueType valueType = DeviceOperation.PropertyValueType.valueOf(arr[arr.length - 1]);
                String property = arr[1];
                switch (valueType) {
                    case current:
                        return "this['properties." + property + "']";
                    case recent:
                        return "coalesce(this['properties." + property + "']" + ",device.property.recent(deviceId,'" + property + "',timestamp))";
                    case last:
                        return "device.property.recent(deviceId,'" + property + "',timestamp)";
                }
            } catch (IllegalArgumentException ignore) {

            }
        }
        return "this['" + String.join(".", Arrays.copyOfRange(arr, 1, arr.length)) + "']";
    }

    static String createColumnAlias(String column, boolean wrapColumn) {
        if (!column.contains(".")) {
            return wrapColumnName(column);
        }
        String[] arr = column.split("[.]");
        String alias;
        //properties.temp.current
        if ("properties".equals(arr[0])) {
            String property = arr[1];
            alias = property + "_" + arr[arr.length - 1];
        } else {
            if (arr.length > 1) {
                alias = String.join("_", Arrays.copyOfRange(arr, 1, arr.length));
            } else {
                alias = column.replace(".", "_");
            }
        }
        return wrapColumn ? wrapColumnName(alias) : alias;
    }

    static String createColumnAlias(String column) {
        return createColumnAlias(column, true);
    }

    static String wrapColumnName(String column) {
        if (column.startsWith("\"") && column.endsWith("\"")) {
            return column;
        }
        return "\"" + (column.replace("\"", "\\\"")) + "\"";
    }

    public List<Variable> createDefaultVariable() {
        return Arrays.asList(
            Variable.of("deviceId", "设备ID").withOption(Variable.OPTION_PRODUCT_ID,productId),
            Variable.of("deviceName", "设备名称"),
            Variable.of("productId", "产品ID"),
            Variable.of("productName", "产品名称")
        );
    }


    public Flux<TermColumn> parseTermColumns(DeviceRegistry registry) {
        if (!StringUtils.hasText(productId)) {
            return Flux.empty();
        }
        return this
            .getDeviceMetadata(registry, productId)
            .as(this::parseTermColumns);
    }

    public Flux<TermColumn> parseTermColumns(Mono<DeviceMetadata> metadataMono) {
        if (operation == null) {
            return Flux.empty();
        }
        return Mono
            .zip(LocaleUtils.currentReactive(),
                 metadataMono,
                 (locale, metadata) -> LocaleUtils
                     .doWith(metadata, locale, (m, l) -> operation.parseTermColumns(m)))
            .flatMapIterable(Function.identity());
    }

    void applyModel(RuleModel model, RuleNodeModel sceneNode) {
        //实时数据不做任何处理
        switch (operation.getOperator()) {
            case online:
            case offline:
            case reportEvent:
            case reportProperty:
                return;
        }

        //设备指令
        RuleNodeModel deviceNode = new RuleNodeModel();
        deviceNode.setId("scene:device:message");
        deviceNode.setName("下发设备指令");
        deviceNode.setExecutor(DeviceMessageSendTaskExecutorProvider.EXECUTOR);
        DeviceMessageSendTaskExecutorProvider.DeviceMessageSendConfig config = new DeviceMessageSendTaskExecutorProvider.DeviceMessageSendConfig();

        config.setProductId(productId);
        config.setMessage(operation.toMessageTemplate());
        config.setSelectorSpec(FastBeanCopier.copy(this, new DeviceSelectorSpec()));
        config.validate();

        deviceNode.setConfiguration(config.toMap());
        model.getNodes().add(deviceNode);

        //定时
        TimerSpec timer = operation.getTimer();
        Assert.notNull(timer, "timer can not be null");
        RuleNodeModel timerNode = new RuleNodeModel();
        timerNode.setId("scene:device:timer");
        timerNode.setName("定时下发指令");
        timerNode.setExecutor("timer");
        //使用最小负载节点来执行定时
        // timerNode.setSchedulingRule(SchedulerSelectorStrategy.minimumLoad());
        timerNode.setConfiguration(FastBeanCopier.copy(timer, new HashMap<>()));
        model.getNodes().add(timerNode);

        // 定时->设备指令->场景
        model.link(timerNode, deviceNode);
        model.link(deviceNode, sceneNode);

    }

    public void validate() {
        //实时数据不做任何处理
        ValidatorUtils.tryValidate(this);
        operation.validate();

    }

}