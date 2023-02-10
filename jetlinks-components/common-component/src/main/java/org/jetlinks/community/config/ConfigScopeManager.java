package org.jetlinks.community.config;

import java.util.List;

public interface ConfigScopeManager {

    void addScope(ConfigScope scope, List<ConfigPropertyDef> properties);

}
