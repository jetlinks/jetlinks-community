package org.jetlinks.community.terms;

import com.alibaba.fastjson.JSONObject;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.collections4.CollectionUtils;
import org.hswebframework.ezorm.core.param.Term;
import org.hswebframework.ezorm.core.param.TermType;
import org.hswebframework.ezorm.rdb.operator.builder.fragments.NativeSql;
import org.hswebframework.web.bean.FastBeanCopier;
import org.hswebframework.web.i18n.LocaleUtils;
import org.jetlinks.community.reactorql.term.TermTypeSupport;
import org.jetlinks.community.reactorql.term.TermTypes;
import org.jetlinks.core.metadata.Jsonable;
import org.jetlinks.core.utils.SerializeUtils;
import org.jetlinks.reactor.ql.supports.DefaultPropertyFeature;
import org.springframework.util.StringUtils;

import java.io.Serializable;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

@Getter
@Setter
@EqualsAndHashCode
public class TermSpec implements Jsonable, Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "列名")
    private String column;

    @Schema(description = "列显码")
    private I18nSpec displayCode;

    @Schema(description = "列显示名")
    private String displayName;

    @Schema(description = "条件类型")
    private String termType;

    @Schema(description = "关联类型")
    private Term.Type type;

    @Schema(description = "期望值")
    private Object expected;

    @Schema(description = "实际值")
    private Object actual;

    @Schema(description = "当前条件是否匹配")
    private Boolean matched;

    @Schema(description = "说明")
    private String description;

    @Schema(description = "嵌套条件")
    private List<TermSpec> children;

    private List<String> options;

    private boolean expectIsExpr;

    @Schema(description = "是否为物模型变量")
    private boolean metadata;

    @Override
    public JSONObject toJson() {
        @SuppressWarnings("all")
        Map<String, Object> map = (Map) SerializeUtils.convertToSafelySerializable(Jsonable.super.toJson());
        return new JSONObject(map);
    }

    public String getDisplayName() {
        if (displayCode != null) {
            return displayCode.resolveI18nMessage();
        }
        return displayName;
    }

    public String getActualDesc() {
        return String.join(";", parseTermSpecActualDesc(this));
    }

    public String getTriggerDesc() {
        return toString();
    }

    //解析TermSpec实际值说明;   如：温度=35；湿度=28
    public static Set<String> parseTermSpecActualDesc(TermSpec termSpec) {
        Set<String> actualDesc = new HashSet<>();

        //兼容直接通过termSpec.setActual(Value)设置了实际值，导致matched一直为false的情况
        if (termSpec.getMatched() == null && StringUtils.hasText(termSpec.getColumn())){
            TermTypes
                .lookupSupport(termSpec.getTermType())
                .ifPresent(support -> termSpec.matched = support.matchBlocking(termSpec.getExpected(),termSpec.getActual()));
        }
        if (termSpec.matched != null && termSpec.matched) {
            actualDesc.add(termSpec.getDisplayName() + " = " + termSpec.getActual());
        }
        if (CollectionUtils.isNotEmpty(termSpec.children)) {
            for (TermSpec child : termSpec.children) {
                actualDesc.addAll(parseTermSpecActualDesc(child));
            }
        }
        return actualDesc;
    }

    public static List<TermSpec> of(List<Term> terms) {
        return of(terms, (term, spec) -> {
        });
    }

    public static List<TermSpec> of(List<Term> terms, BiConsumer<Term, TermSpec> customizer) {
        if (terms == null) {
            return null;
        }
        return terms
            .stream()
            .map(spec -> of(spec, customizer))
            .collect(Collectors.toList());
    }

    public static TermSpec of(Term term, BiConsumer<Term, TermSpec> customizer) {
        TermSpec spec = new TermSpec();
        spec.setType(term.getType());
        spec.setColumn(term.getColumn());
        spec.setTermType(term.getTermType());
        spec.setExpected(term.getValue());
        spec.setDisplayName(term.getColumn());
        spec.setOptions(term.getOptions());
        if (term.getValue() instanceof NativeSql) {
            spec.setExpectIsExpr(true);
            //fixme 参数支持?
            spec.setExpected(((NativeSql) term.getValue()).getSql());
        }
        customizer.accept(term, spec);
        spec.children = of(term.getTerms(), customizer);
        return spec;
    }

    public String getTermType() {
        return termType == null ? TermType.eq : termType;
    }

    public Term.Type getType() {
        return type == null ? Term.Type.and : type;
    }

    private void apply0(Map<String, Object> context) {
        if (this.column != null) {
            this.actual = DefaultPropertyFeature
                .GLOBAL
                .getProperty(this.column, context)
                .orElse(null);
            if (expectIsExpr) {
                this.expected = DefaultPropertyFeature
                    .GLOBAL
                    .getProperty(String.valueOf(expected), context)
                    .orElse(null);
                expectIsExpr = false;
            }
            TermTypes
                .lookupSupport(getTermType())
                .ifPresent(support -> this.matched = support.matchBlocking(expected, actual));

        }
        if (this.children != null) {
            for (TermSpec child : this.children) {
                child.apply0(context);
            }
        }
    }

    public TermSpec apply(Map<String, Object> context) {
        TermSpec copy = copy();
        copy.apply0(context);
        return copy;
    }

    public TermSpec copy() {
        TermSpec spec = FastBeanCopier.copy(this, new TermSpec(), "children");
        if (this.children != null) {
            spec.children = this.children.stream().map(TermSpec::copy).collect(Collectors.toList());
        }
        return spec;
    }

    public static String toString(List<TermSpec> terms) {
        return toString(new StringBuilder(), terms).toString();
    }

    public static StringBuilder toString(StringBuilder builder, List<TermSpec> terms) {
        int lastLength = builder.length();

        for (TermSpec child : terms) {

            if (lastLength != builder.length()) {
                builder
                    .append(' ')
                    .append(LocaleUtils.resolveMessage("message.term-type-" + child.getType(), child
                        .getType()
                        .name()))
                    .append(' ');
                lastLength = builder.length();
            }
            child.toString(builder);

        }
        return builder;
    }

    public void toString(StringBuilder builder) {
        boolean hasSpec = false;
        if (column != null) {
            TermTypeSupport support = TermTypes
                .lookupSupport(getTermType())
                .orElse(null);
            if (support != null) {
                hasSpec = true;
                builder.append(support.createDesc(getDisplayName(), expected, actual));
            }
        }
        List<TermSpec> children = compressChildren();

        if (CollectionUtils.isNotEmpty(children)) {

            if (hasSpec) {
                builder
                    .append(' ')
                    .append(LocaleUtils.resolveMessage("message.term-type-" + getType(), getType().name()))
                    .append(' ');
            } else {
                //对整个嵌套结果取反?
                if (Objects.equals(getTermType(), TermType.not)) {
                    builder.append('!');
                }
            }
            builder.append("( ");
            toString(builder, children);
            builder.append(" )");
        }

    }


    public void setActual(Object actual) {
        this.actual = actual;
        this.setMatched(null);
    }

    public void setExpected(Object expected) {
        this.expected = expected;
        this.setMatched(null);
    }

    public void setTermType(String termType) {
        this.termType = termType;
        this.setMatched(null);
    }

    //压缩子节点,避免过多嵌套.
    protected List<TermSpec> compressChildren() {
        if (CollectionUtils.isEmpty(children)) {
            return children;
        }
        if (children.size() == 1) {
            TermSpec child = children.get(0);
            if (child.column == null && Objects.equals(child.getTermType(), TermType.eq)) {
                return child.compressChildren();
            }
        }
        return children;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        toString(sb);
        return sb.toString();
    }
}
