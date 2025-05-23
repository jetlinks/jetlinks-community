/*
 * Copyright 2025 JetLinks https://www.jetlinks.cn
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jetlinks.community.auth.service;

import lombok.Generated;
import lombok.Getter;
import lombok.Setter;
import org.hswebframework.web.exception.ValidationException;
import org.hswebframework.web.system.authorization.api.PasswordValidator;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.HashSet;
import java.util.Set;

/**
 * 密码验证器，用于过滤简单密码.
 *
 * @author zhouhao
 * @see PasswordValidator
 * @since 1.0
 */
@Component
@ConfigurationProperties(prefix = "hsweb.user.password.validator")
@Getter
@Setter
@Generated
public class PasswordStrengthValidator implements PasswordValidator {

    private String[] regex = {
        ".*\\d+.*", //数字
        ".*[A-Z]+.*", //大写字母
        ".*[a-z]+.*", //小写字母
        ".*[^x00-xff]+.*", //2位字符(中文?)
        ".*[~!@#$%^&*()_+|<>,.?/:;'\\[\\]{}\"]+.*", //特殊符号
    };

    private Set<String> blackList = new HashSet<>();

    private int minLength = 8;

    private int maxLength = 64;

    private int level = 2;

    private String message = "密码必须由数字和字母组成";

    @Override
    public void validate(String password) {
        if (StringUtils.isEmpty(password) || password.length() < minLength || password.length() > maxLength) {
            throw new ValidationException("","error.password_length", minLength, maxLength);
        }
        if (blackList.contains(password)) {
            throw new ValidationException("error.insufficient_password_strength");
        }
        int _level = 0;
        for (String s : regex) {
            if (password.matches(s)) {
                _level++;
            }
        }
        if (_level <= level) {
            throw new ValidationException("error.insufficient_password_strength");
        }
    }
}
