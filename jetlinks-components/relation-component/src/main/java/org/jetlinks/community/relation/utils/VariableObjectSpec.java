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
package org.jetlinks.community.relation.utils;

import lombok.Getter;
import lombok.Setter;
import org.jetlinks.core.things.relation.ObjectSpec;
import org.jetlinks.reactor.ql.supports.DefaultPropertyFeature;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.util.Map;

/**
 * 变量关系描述,对象ID可通过变量来获取
 *
 * @author zhouhao
 * @since 2.0
 */
@Getter
@Setter
public class VariableObjectSpec extends ObjectSpec {

    /**
     * 对象变量来源,通过此来源来指定对象ID.
     * <p>
     * 此变量仅支持 fixed(固定值)和upper(来自上游)类型
     */
    private VariableSource objectSource;

    public void init(Map<String, Object> context) {
        if (objectSource != null) {
            switch (objectSource.getSource()) {
                case fixed:
                    setObjectId((String) objectSource.getValue());
                case upper:
                    DefaultPropertyFeature.GLOBAL
                        .getProperty(objectSource.getUpperKey(), context)
                        .map(String::valueOf)
                        .ifPresent(this::setObjectId);
            }
        }
    }

    public void validate() {
        Assert.hasText(getObjectType(), "objectType can not be null");
        if (!StringUtils.hasText(getObjectId())) {
            Assert.notNull(objectSource, "objectSource can not be null");
            Assert.isTrue(objectSource.getSource() != VariableSource.Source.relation,
                          "unsupported source relation");
            objectSource.validate();
        }
    }

    public static VariableObjectSpec ofSource(String type, VariableSource object) {
        VariableObjectSpec spec = new VariableObjectSpec();
        spec.objectSource = object;
        spec.setObjectType(type);
        return spec;
    }

    public static VariableObjectSpec of(ObjectSpec object) {
        VariableObjectSpec spec = new VariableObjectSpec();

        spec.setObjectType(object.getObjectType());
        spec.setObjectId(object.getObjectId());
        spec.setRelated(object.getRelated());

        return spec;

    }

}
