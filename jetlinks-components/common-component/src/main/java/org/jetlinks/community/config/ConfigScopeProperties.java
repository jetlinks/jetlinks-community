package org.jetlinks.community.config;

import lombok.Getter;
import lombok.Setter;
import org.hswebframework.web.bean.FastBeanCopier;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "system.config")
public class ConfigScopeProperties implements ConfigScopeCustomizer{

    @Getter
    @Setter
    private List<Scope> scopes = new ArrayList<>();

    @Override
    public void custom(ConfigScopeManager manager) {
        for (Scope scope : scopes) {
            manager.addScope(FastBeanCopier.copy(scope,new ConfigScope()), scope.properties);
        }
    }

    @Getter
    @Setter
    public static class Scope extends ConfigScope {
        private List<ConfigPropertyDef> properties = new ArrayList<>();
    }

}
