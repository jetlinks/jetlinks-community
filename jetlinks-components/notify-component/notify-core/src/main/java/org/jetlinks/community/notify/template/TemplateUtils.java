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

import org.hswebframework.web.utils.TemplateParser;
import org.jetlinks.community.relation.utils.VariableSource;
import org.jsoup.Jsoup;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public class TemplateUtils {

    public static Set<String> getVariables(String templateText) {
        return getVariables(templateText, "${", "}");
    }

    public static Set<String> getVariables(String templateText, String suffix, String prefix) {
        final Set<String> variable = new LinkedHashSet<>();

        TemplateParser parser = new TemplateParser();
        parser.setTemplate(templateText);
        parser.setPrepareStartSymbol(suffix.toCharArray());
        parser.setPrepareEndSymbol(prefix.toCharArray());
        parser.parse(varName -> {
            //html ?
            if (varName.contains("<")) {
                varName = Jsoup
                    .parse(varName)
                    .text();
            }
            variable.add(varName);
            return "";
        });
        return variable;
    }

    public static String simpleRender(String templateText,
                                      Map<String, Object> context) {
        return simpleRender(templateText, context, var -> null);
    }

    public static String simpleRender(String templateText,
                                      Map<String, Object> context,
                                      Function<String, VariableDefinition> variables) {
        return TemplateParser
            .parse(templateText, varName -> {
                //html ?
                if (varName.contains("<")) {
                    varName = Jsoup
                        .parse(varName)
                        .text();
                }
                VariableDefinition def = variables.apply(varName);
                Object value = VariableSource.resolveValue(varName, context);
                if (def != null) {
                    return def.convertValue(value);
                }
                //没有定义变量,也没有值,应该报错?
                if (null == value) {
                    return "";
                }
                return String.valueOf(value);
            });
    }
}
