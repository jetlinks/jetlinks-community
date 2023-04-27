package org.jetlinks.community.script.nashorn;

import org.jetlinks.community.script.JavaScriptFactoryTest;
import org.jetlinks.community.script.jsr223.JavaScriptFactory;

import static org.junit.jupiter.api.Assertions.assertNull;

class NashornScriptFactoryTest extends JavaScriptFactoryTest {
    NashornScriptFactory factory = new NashornScriptFactory();

    @Override
    protected JavaScriptFactory getFactory() {
        return factory;
    }
}