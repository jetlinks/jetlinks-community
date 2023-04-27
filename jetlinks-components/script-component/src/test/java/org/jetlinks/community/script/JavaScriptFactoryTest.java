package org.jetlinks.community.script;

import jdk.nashorn.internal.objects.Global;
import lombok.SneakyThrows;
import lombok.extern.java.Log;
import org.jetlinks.community.script.jsr223.JavaScriptFactory;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

public abstract class JavaScriptFactoryTest {

    protected abstract JavaScriptFactory getFactory();

    @Test
    void testBadAccess() {
        JavaScriptFactory factory = getFactory();

        {
            CompiledScript script = factory.compile(Script.of("test", "return this.engine"));
            Object resul = script.call(Collections.emptyMap());
            assertNull(resul);
        }

        {
            CompiledScript script = factory.compile(Script.of("test", "return global"));
            Object resul = script.call(Collections.emptyMap());
            assertNull(resul);
        }

        {
            CompiledScript script = factory.compile(Script.of("test", "return context"));
            Object resul = script.call(Collections.emptyMap());
            assertNull(resul);
        }

        {
            CompiledScript script = factory.compile(Script.of("test", "delete this.engine;return this.engine"));
            Object resul = script.call(Collections.emptyMap());
            assertNull(resul);
        }

        {
            CompiledScript script = factory.compile(Script.of("test", "delete quit;return quit()"));
            Object resul = script.call(Collections.emptyMap());
            assertNull(resul);
        }

        {
            CompiledScript script = factory.compile(Script.of("test", "return exit()"));
            Object resul = script.call(Collections.emptyMap());
            assertNull(resul);
        }

        {
            CompiledScript script = factory.compile(Script.of("test", "return exit(0)"));
            Object resul = script.call(Collections.emptyMap());
            assertNull(resul);
        }

        {
            CompiledScript script = factory.compile(Script.of("test", "delete this.eval; return eval('1')"));
            Object resul = script.call(Collections.emptyMap());
            assertNull(resul);
        }
        {
            CompiledScript script = factory.compile(Script.of("test", "return eval('1')"));
            Object resul = script.call(Collections.emptyMap());
            assertNull(resul);
        }

        {
            CompiledScript script = factory.compile(Script.of("test", "return eval('1')"));
            Object resul = script.call(Collections.emptyMap());
            assertNull(resul);
        }

        {
            CompiledScript script = factory.compile(Script.of("test", "return Function('return 1')()"));
            Object resul = script.call(Collections.emptyMap());
            assertNull(resul);
        }

        {
            CompiledScript script = factory.compile(Script.of("test", "return this.eval('1')"));
            Object resul = script.call(Collections.emptyMap());
            assertNull(resul);
        }

        {
            CompiledScript script = factory.compile(Script.of("test", "const func = function() { return eval(1); };  return func();"));
            Object resul = script.call(Collections.emptyMap());
            assertNull(resul);
        }

        {
            CompiledScript script = factory.compile(Script.of("test", "return (function(){ return eval('return 1') })()"));
            Object resul = script.call(Collections.emptyMap());
            assertNull(resul);
        }


    }


    @Test
    void testArray() {
        JavaScriptFactory factory = getFactory();
        Object val = factory.compile(Script.of("test", "var arr = []; arr.push({a:1,b:2}); return arr;"))
                            .call(Collections.emptyMap());
        System.out.println(val);
        assertNotNull(val);
        assertTrue(val instanceof List);
    }

    @Test
    void testPrint() {
        JavaScriptFactory factory = getFactory();
        factory.compile(Script
                            .of("test", "print(123);console.log('test 123');"))
               .call(Collections.emptyMap());

    }

    @Test
    void testTernary() {
        JavaScriptFactory factory = getFactory();
        Object val = factory.compile(Script.of("test", "return 1 + (1>0?2:1);"))
                            .call(Collections.emptyMap());
        System.out.println(val);
        assertEquals(3, val);
    }

    @Test
    void testNullSafe() {
        JavaScriptFactory factory = getFactory();
        Object val = factory.compile(Script.of("test", "return temp1==null? 10 :0"))
                            .call(Collections.emptyMap());
        assertEquals(10, val);
    }

    @Test
    void testBenchmark() {
        JavaScriptFactory factory = getFactory();
        CompiledScript script = factory.compile(Script.of("test", "if(temp1==null) {return temp} else {return temp+1}"));
        script.call(Collections.singletonMap("temp", 10));

        long time = System.currentTimeMillis();
        for (long i = 0; i < 10_0000; i++) {
            script.call(Collections.singletonMap("temp", 10));
        }
        System.out.println(System.currentTimeMillis() - time);
    }

    @Test
    void testAnonymous() {
        JavaScriptFactory factory = getFactory();
        factory.compile(Script.of("test", "parser.fixed(4)\n" +
            "       .handler(function(buffer){\n" +
            "            var len = buffer.getShort(2);\n" +
            "            parser.fixed(len).result(buffer);\n" +
            "        })\n" +
            "       .handler(function(buffer){\n" +
            "            parser.result(buffer)\n" +
            "                   .complete();\n" +
            "        });"));

    }

    @Test
    void testVarNest() {
        JavaScriptFactory factory = getFactory();
        TestNest nest = new TestNest();

        factory.compile(Script.of("test", "const test = this.test;  test.setup(function(e){ return e+test.data() })"))
               .call(Collections.singletonMap("test", nest));

        assertNotNull(nest.func);

        System.out.println(nest.func.apply(10));
    }

    public static class TestNest {
        Function<Object, Object> func;

        public Object data() {
            return 2;
        }

        public TestNest setup(Function<Object, Object> func) {
            this.func = func;
            return this;
        }
    }

    @Test
    @SneakyThrows
    void testUtils() {
        {
            JavaScriptFactory factory = getFactory();
            MyClazz utils = new MyClazz();

            ExposedScript<MyClazz> script = factory.compileExpose(Script.of("test", "return $recent($recent(),$recent(temp))"), TestExtend.class);

            assertEquals(utils.$recent(utils.$recent(), utils.$recent(2)), script.call(utils, Collections.singletonMap("temp", 2)));
        }
    }

    @Test
    void testNestFunction() {
        JavaScriptFactory factory = getFactory();

        Map<String, Object> ctx = new HashMap<>();
        ctx.put("val", 1);

        ExposedScript<TestExtend> script = factory.compileExpose(
            Script.of("test", "var $val = val; function decode(){ return $recent()+$val }; return call(decode);"),
            TestExtend.class);

        TestExtend extend = new TestExtend();
        script.call(extend, ctx);


        assertNotNull(extend.call);
        assertEquals(2.0, extend.call.get());

    }

    public static class MyClazz extends TestExtend {

    }

    public static class TestExtend {
        Supplier<Object> call;

        public int max(int i, int j) {
            return Math.max(i, j);
        }

        public int $recent() {
            return 1;
        }

        public int $recent(int i) {
            return i;
        }

        public int $recent(int i, int j) {
            return i + j;
        }

        public Object call(Supplier<Object> call) {
            this.call = call;
            return 0;
        }

    }

    @Test
    void testMake() {

        Api api = getFactory().bind(Script.of(Api.class.getName(), "function add(a,b){return a+b};"), Api.class);

        assertEquals(3, api.add(1, 2));

        assertNull(api.reduce(1, 2));

        assertNull(api.reduce(1, 2));

    }

    @Test
    @SneakyThrows
    void testLog() {
        {
            JavaScriptFactory factory = getFactory();

            factory.compile(Script.of("test","console.log(123)"))
                .call();

        }
    }

    public interface Api {
        int add(int a, int b);

        Object reduce(int a, int b);
    }
}
