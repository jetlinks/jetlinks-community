package org.jetlinks.community.script;

import lombok.*;

@Getter
@AllArgsConstructor(staticName = "of")
public class Script {

    @NonNull
    private final String name;
    @NonNull
    private final String content;

    private final Object source;

    private final ExecuteLimit limit;

    private final ScriptLogger logger;

    public boolean limitIsEnabled() {
        return limit != null && limit.isEnabled();
    }

    public static Script of(String name, String content) {
        return Script.of(name, content, null, ExecuteLimit.defaultLimit(),null);
    }

    public static Script of(String name, String content, Object source) {
        return Script.of(name, content, source, ExecuteLimit.defaultLimit(),null);
    }


    public Script content(String content) {
        return of(name, content, source, limit,logger);
    }

    public Script limit(ExecuteLimit limit) {
        return of(name, content, source, limit,logger );
    }

    public Script logger(ScriptLogger logger) {
        return of(name, content, source, limit,logger );
    }

}
