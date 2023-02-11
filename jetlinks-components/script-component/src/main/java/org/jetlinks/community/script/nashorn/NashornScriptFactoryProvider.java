package org.jetlinks.community.script.nashorn;

import org.jetlinks.community.script.AbstractScriptFactoryProvider;
import org.jetlinks.community.script.ScriptFactory;

public class NashornScriptFactoryProvider extends AbstractScriptFactoryProvider {

    public NashornScriptFactoryProvider() {
        super("js", "javascript", "nashorn");
    }

    @Override
    public ScriptFactory factory() {
        return new NashornScriptFactory();
    }
}
