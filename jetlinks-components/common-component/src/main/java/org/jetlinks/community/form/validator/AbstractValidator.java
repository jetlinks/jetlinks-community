package org.jetlinks.community.form.validator;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.collections4.CollectionUtils;
import org.hswebframework.web.i18n.LocaleUtils;
import org.hswebframework.web.validator.CreateGroup;
import org.hswebframework.web.validator.UpdateGroup;

import javax.validation.groups.Default;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author gyl
 * @since 2.2
 */
public abstract class AbstractValidator implements Validator {

    private final String customMessage;

    /**
     * 约束所属的组
     */
    List<Class<?>> group = Collections.emptyList();

    public AbstractValidator(String customMessage, List<String> group) {
        this.customMessage = customMessage;
        initGroup(group);
    }

    private void initGroup(List<String> group) {
        if (CollectionUtils.isNotEmpty(group)) {
            this.group = group
                .stream()
                .map(type -> GroupType.valueOf(type).getGroupClass())
                .collect(Collectors.toList());
        }
    }

    /**
     * 错误code
     */
    public String getCode() {
        return LocaleUtils
            .resolveMessage("message.validator_not_passed");
    }

    abstract public Result doValidate(Object value);

    @Override
    public Result validate(Object value, Collection<Class<?>> groups) {
        if (isMatch(groups)) {
            Result result = doValidate(value);
            if (!result.isPassed()) {
                result.setCode(getCode());
                result.setMessage(customMessage);
            }
            return result;
        }
        //不是所属组直接通过
        return Result.pass();
    }


    private boolean isMatch(Collection<Class<?>> groups) {
        boolean match = group
            .stream()
            .anyMatch(groups::contains);
        if (!match) {
            //没有指定分组，即为默认分组
            return (group.isEmpty() || group.contains(Default.class)) && (groups.isEmpty() || groups.contains(Default.class));
        }
        return true;
    }

    @Getter
    @AllArgsConstructor
    public enum GroupType {
        update(UpdateGroup.class),
        insert(CreateGroup.class),
        save(CreateGroup.class);

        private final Class<?> groupClass;
    }

}
