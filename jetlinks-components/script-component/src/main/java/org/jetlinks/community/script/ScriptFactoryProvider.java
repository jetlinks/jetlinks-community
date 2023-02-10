package org.jetlinks.community.script;

public interface ScriptFactoryProvider {

    boolean isSupport(String langOrMediaType);

    ScriptFactory factory();

}
