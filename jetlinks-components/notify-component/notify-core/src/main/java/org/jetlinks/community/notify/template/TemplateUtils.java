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
