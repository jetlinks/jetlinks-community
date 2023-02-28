

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

    public static Script of(String name, String content) {
        return Script.of(name, content, null);
    }

    public Script content(String content) {
        return of(name, content, source);
    }

}