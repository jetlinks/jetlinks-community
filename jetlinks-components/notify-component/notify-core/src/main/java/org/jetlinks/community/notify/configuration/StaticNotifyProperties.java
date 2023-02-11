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
