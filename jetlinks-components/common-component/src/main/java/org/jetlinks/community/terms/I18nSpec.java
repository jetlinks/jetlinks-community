package org.jetlinks.community.terms;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.hswebframework.web.bean.FastBeanCopier;
import org.hswebframework.web.i18n.LocaleUtils;
import org.jetlinks.reactor.ql.utils.CastUtils;
import org.springframework.util.CollectionUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 支持国际化参数嵌套的国际化编码类。适用于实现国际化表达式的参数也需要国际化的场景。
 *
 * <pre>{@code
 *
 * 国际化表达式携带的参数也需要国际化示例
 * 示例参数:
 *
 * {
 *     "code": "message.scene.term.full_name",
 *     "args": [
 *         {
 *             "defaultMessage": "温度"
 *         },
 *         {
 *             "code": "message.property.recent"
 *         }
 *     ]
 * }
 * 在国际化配置文件中的配置示例:
 * message.scene.term.full_name=属性:{0}/{1}
 * message.property.recent=当前值
 *
 * 国际化完成后的结果为：属性:温度/当前值
 * }</pre>
 *
 * @author bestfeng
 */
@Getter
@Setter
@EqualsAndHashCode
public class I18nSpec implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 国际化编码为空时，表示当前对象不需要国际化。直接返回defaultMessage值
     */
    @Schema(description = "国际化编码")
    private String code;

    @Schema(description = "默认值")
    private Object defaultMessage;

    @Schema(description = "支持国际化的嵌套参数")
    private List<I18nSpec> args;

    public static I18nSpec of(String code, String defaultMessage, Object... args) {
        I18nSpec i18NSpec = new I18nSpec();
        i18NSpec.setCode(code);
        i18NSpec.setDefaultMessage(defaultMessage);
        i18NSpec.setArgs(of(args));
        return i18NSpec;
    }

    public I18nSpec withArgs(String code, String defaultMessage, Object... args) {
        if (CollectionUtils.isEmpty(this.args)) {
            this.setArgs(new ArrayList<>());
        }
        this.getArgs().add(I18nSpec.of(code, defaultMessage, args));
        return this;
    }

    public I18nSpec withArgs(I18nSpec i18NSpec) {
        if (CollectionUtils.isEmpty(this.args)) {
            this.setArgs(new ArrayList<>());
        }
        this.getArgs().add(i18NSpec);
        return this;
    }

    public String resolveI18nMessage() {
        if (CollectionUtils.isEmpty(args)) {
            return CastUtils.castString(resolveI18nMessage(code, defaultMessage));
        }
        return CastUtils.castString(resolveI18nMessage(code, defaultMessage, parseI18nCodeParams().toArray()));
    }


    public I18nSpec copy() {
        return FastBeanCopier.copy(this, new I18nSpec());
    }


    private static List<I18nSpec> of(Object... args) {
        List<I18nSpec> codes = new ArrayList<>();
        for (Object arg : args) {
            I18nSpec i18NSpec = new I18nSpec();
            i18NSpec.setDefaultMessage(arg);
            codes.add(i18NSpec);
        }
        return codes;
    }

    protected List<Object> parseI18nCodeParams() {
        if (CollectionUtils.isEmpty(args)) {
            return new ArrayList<>();
        }
        List<Object> params = new ArrayList<>();
        for (I18nSpec code : args) {
            params.add(code.resolveI18nMessage());
        }
        return params;
    }

    private static Object resolveI18nMessage(String code, Object name, Object... args) {
        if (StringUtils.isEmpty(code)){
            return name;
        }
        if (name == null){
            return LocaleUtils.resolveMessage(code, args);
        }
        return LocaleUtils.resolveMessage(code, CastUtils.castString(name), args);
    }
}