package org.jetlinks.community.script;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import java.io.File;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public abstract class AbstractScriptFactory implements ScriptFactory {

    private final Utils utils = new Utils();

    static Class<?>[] DEFAULT_DENIES = {
        System.class,
        File.class,
        Paths.class,
        ObjectInputStream.class,
        ObjectOutputStream.class,
        Thread.class,
        Runtime.class,
        ScriptEngine.class,
        ScriptEngineFactory.class
    };

    static Class<?>[] DEFAULT_ALLOWS = {
        byte.class, short.class, int.class, long.class, char.class, float.class, double.class, boolean.class,
        Byte.class, Short.class, Integer.class, Long.class, Character.class, Float.class, Double.class, Boolean.class,
        BigDecimal.class, BigInteger.class,
        String.class,
        HashMap.class, ConcurrentHashMap.class, LinkedHashMap.class,
        Date.class, LocalDateTime.class,
        ArrayList.class,LinkedList.class
    };

    private final Set<String> denies = new HashSet<>();
    private final Set<String> allows = new HashSet<>();

    public AbstractScriptFactory() {
        denies.add("*");
        allows(DEFAULT_ALLOWS);
        //denies(DEFAULT_DENIES);
    }

    @Override
    public final void allows(Collection<Class<?>> allowTypes) {
        allows.addAll(allowTypes.stream().map(Class::getName).collect(Collectors.toList()));
    }

    @Override
    public final void allows(Class<?>... allowTypes) {
        allows(Arrays.asList(allowTypes));
    }

    @Override
    public final void denies(Collection<Class<?>> allowTypes) {
        denies.addAll(allowTypes.stream().map(Class::getName).collect(Collectors.toList()));
    }

    @Override
    public final void denies(Class<?>... allowTypes) {
        denies(Arrays.asList(allowTypes));
    }

    @Override
    public void allowsPattern(String... allowTypes) {
        allowsPattern(Arrays.asList(allowTypes));
    }

    @Override
    public void allowsPattern(Collection<String> allowTypes) {

    }

    @Override
    public void deniesPattern(String... allowTypes) {
        deniesPattern(Arrays.asList(allowTypes));
    }

    @Override
    public void deniesPattern(Collection<String> allowTypes) {

    }

    public final boolean isDenied(Class<?> type) {
        return isDenied(type.getName());
    }

    public final boolean isDenied(String typeName) {
        if (allows.contains(typeName)) {
            return false;
        }
        return denies.contains("*") || denies.contains(typeName);
    }


    public Utils getUtils(){
        return utils;
    }


    public class Utils {

        private Utils(){}

        public Object toJavaType(Object obj) {
            return AbstractScriptFactory.this.convertToJavaType(obj);
        }

    }

}
