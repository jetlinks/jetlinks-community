package org.jetlinks.community.notify.webhook.http;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONException;
import com.google.common.collect.Maps;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.jetlinks.community.notify.template.AbstractTemplate;
import org.jetlinks.core.Values;
import org.springframework.http.HttpMethod;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Getter
@Setter
public class HttpWebHookTemplate extends AbstractTemplate<HttpWebHookTemplate> {

    @Schema(description = "请求地址")
    private String url = "";

    @Schema(description = "请求头")
    private List<HttpWebHookProperties.Header> headers;

    @Schema(description = "请求方式,默认POST")
    private HttpMethod method = HttpMethod.POST;

    @Schema(description = "使用上下文作为请求体")
    private boolean contextAsBody;

    @Schema(description = "自定义请求体")
    private String body;

    @Hidden
    private Boolean bodyIsJson;

    //todo 增加认证类型, oauth2等

    public String resolveBody(Values context) {
        Map<String, Object> contextVal = renderMap(context.getAllValues());

        if (contextAsBody) {
            return JSON.toJSONString(contextVal);
        }

        try {
            if (bodyIsJson == null || bodyIsJson) {
                //解析为json再填充变量,防止变量值中含有",{等字符时导致json格式错误
                String _body = JSON.toJSONString(
                    resolveBody(JSON.parse(body), contextVal)
                );
                bodyIsJson = true;
                return _body;
            }
        } catch (JSONException ignore) {

        }
        bodyIsJson = false;
        return render(body, contextVal);
    }


    private Object resolveBody(Object val, Map<String, Object> context) {
        //字符串,支持变量:${var}
        if (val instanceof String) {
            return render(String.valueOf(val), context);
        }
        if (val instanceof Map) {
            return resolveBody(((Map<?, ?>) val), context);
        }
        if (val instanceof List) {
            return resolveBody(((List<?>) val), context);
        }
        return val;
    }

    private Map<Object, Object> resolveBody(Map<?, ?> obj, Map<String, Object> context) {
        Map<Object, Object> val = Maps.newLinkedHashMapWithExpectedSize(obj.size());

        for (Map.Entry<?, ?> entry : obj.entrySet()) {
            Object key = resolveBody(entry.getKey(), context);
            //空key,忽略
            if (ObjectUtils.isEmpty(key)) {
                continue;
            }
            val.put(key, resolveBody(entry.getValue(), context));
        }
        return val;
    }

    private List<Object> resolveBody(List<?> obj, Map<String, Object> context) {
        List<Object> array = new ArrayList<>(obj.size());
        for (Object val : obj) {
            array.add(resolveBody(val, context));
        }
        return array;
    }



}
