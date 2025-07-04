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
package org.jetlinks.community.notify.template;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.Getter;
import org.hswebframework.web.bean.FastBeanCopier;
import org.hswebframework.web.validator.ValidatorUtils;

import javax.annotation.Nonnull;
import java.util.*;

public abstract class AbstractTemplate<Self extends AbstractTemplate<Self>> implements Template {

    private Map<String, VariableDefinition> variables;

    @Getter
    private String configId;

    public AbstractTemplate() {

    }

    public Self with(TemplateProperties properties) {
        if (this.variables == null) {
            this.variables = new LinkedHashMap<>();
        }
        FastBeanCopier.copy(properties.getTemplate(), this);
        this.configId = properties.getConfigId();
        //内置变量
        for (VariableDefinition variable : getEmbeddedVariables()) {
            this.variables.put(variable.getId(), variable);
        }
        //动态配置的变量
        if (null != properties.getVariableDefinitions()) {
            for (VariableDefinition embeddedVariable : properties.getVariableDefinitions()) {
                this.variables.put(embeddedVariable.getId(), embeddedVariable);
            }
        }
        return toSelf();
    }

    @SuppressWarnings("all")
    protected Self toSelf() {
        return (Self) this;
    }

    protected void addVariable(String key, String name, String... descriptions) {
        addVariable(
            VariableDefinition
                .builder()
                .id(key)
                .name(name)
                .description(String.join("", descriptions))
                .build()
        );
    }

    protected synchronized void addVariable(VariableDefinition def) {
        if (null == variables) {
            variables = new HashMap<>();
        }
        variables.put(def.getId(), def);
    }

    //获取单个变量信息
    public final Optional<VariableDefinition> getVariable(String key) {
        return Optional.ofNullable(this.getVariables().get(key));
    }

    @Override
    @Nonnull
    public final Map<String, VariableDefinition> getVariables() {
        return this.variables == null ? Collections.emptyMap() : Collections.unmodifiableMap(this.variables);
    }

    @Nonnull
    protected List<VariableDefinition> getEmbeddedVariables() {
        return Collections.emptyList();
    }

    public Self validate() {
        ValidatorUtils.tryValidate(this);
        return toSelf();
    }

    public Map<String,Object> toMap(){
        return (JSONObject)JSON.toJSON(this);
    }

}
