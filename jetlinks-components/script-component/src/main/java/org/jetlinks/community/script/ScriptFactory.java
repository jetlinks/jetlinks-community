package org.jetlinks.community.script;

import java.util.Collection;
import java.util.Map;

public interface ScriptFactory {

    void allows(Collection<Class<?>> allowTypes);

    void allows(Class<?>... allowTypes);

    void denies(Collection<Class<?>> allowTypes);

    void denies(Class<?>... allowTypes);

    void allowsPattern(Collection<String> allowTypes);

    void allowsPattern(String... allowTypes);

    void deniesPattern(Collection<String> allowTypes);

    void deniesPattern(String... allowTypes);

    Object convertToJavaType(Object data);

    /**
     * 编译脚本,编译后通过@{@link CompiledScript#call(Map)}来执行脚本.
     *
     * <pre>{@code
     *     CompiledScript script = factory.compile(Script.of("test","return arg0+2;"));
     *
     *     // val = 12
     *     Object val =  script.call(Collections.singletonMap("arg0",10));
     * }</pre>
     *
     * @param script 脚本
     * @return 编译后的可执行脚本
     */
    CompiledScript compile(Script script);

    /**
     * 编译脚本并将指定的类的方法暴露到脚本中,在脚本中可以直接调用内嵌对象的方法.比如:
     *
     * <pre>{@code
     *
     *  public class Helper{
     *      public int max(int a,int b){
     *          return Math.max(a,b);
     *      }
     *  }
     *
     *  CompiledScript script = factory.compile(Script.of("test","return max(1,2);"),Helper.class)
     *
     *  Object val = script.call(new Helper());
     *
     * }</pre>
     *
     * @param script 脚本
     * @param expose 要暴露的方法
     * @return CompiledScript
     */
    <T> ExposedScript<T> compileExpose(Script script, Class<? super T> expose);

    /**
     * 将脚本构造为一个接口的实现,在脚本中定义方法,然后将脚本的方法绑定到接口上.
     * <p>
     * 如果在脚本中没有定义方法的实现,调用方法后将返回<code>null</code>
     *
     * <pre>{@code
     *
     *   public interface MyInterface{
     *
     *       Object encode(Object data);
     *
     *   }
     *
     *  MyInterface inf = factory.bind(Script.of("function encode(data){  return 1;    }"),MyInterface.class);
     *
     *  //执行，将调用脚本中的encode方法
     *  Object val = inf.encode(arg);
     *
     * }</pre>
     *
     * @param script        脚本
     * @param interfaceType 接口类型
     * @param <T>           泛型
     * @return 接口代理实现
     */
    <T> T bind(Script script,
               Class<T> interfaceType);

}