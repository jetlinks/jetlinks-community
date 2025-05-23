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
package org.jetlinks.community.notify.configuration;

import lombok.Getter;
import lombok.Setter;
import org.jetlinks.community.notify.NotifierProperties;
import org.jetlinks.community.notify.NotifyType;
import org.jetlinks.community.notify.template.TemplateProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <pre>
 * jetlinks:
 *   notify:
 *      configs:
 *          sms:
 *            - id: login
 *              provider: aliyun
 *              configuration:
 *                  regionId: cn-hangzhou
 *                  accessKeyId: accessKeyId
 *                  secret: secret
 *
 * </pre>
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "jetlinks.notify")
public class StaticNotifyProperties {

    private Map<String, List<NotifierProperties>> configs = new ConcurrentHashMap<>();

    private Map<String, List<TemplateProperties>> templates = new ConcurrentHashMap<>();

    public Optional<NotifierProperties> getNotifierProperties(NotifyType type,String id){
        List<NotifierProperties> properties= configs.get(type.getId());
        if(properties == null){
            return Optional.empty();
        }
        return properties
            .stream()
            .filter(prop-> {
                prop.setType(type.getId());
                return Objects.equals(id,prop.getId());
            })
            .findAny();
    }

    public Optional<TemplateProperties> getTemplateProperties(NotifyType type,String id){
        List<TemplateProperties> properties= templates.get(type.getId());
        if(properties==null){
            return Optional.empty();
        }
        return properties
            .stream()
            .filter(prop-> {
                prop.setType(type.getId());
                return Objects.equals(id,prop.getId());
            })
            .findAny();
    }

    @Setter
    @Getter
    public static class ObjectTemplateProperties extends TemplateProperties{
         private Map<String,Object> templateObject;

        public void setTemplateObject(Map<String, Object> templateObject) {
            this.templateObject = templateObject;
            this.setTemplate(templateObject);
        }
    }

}
