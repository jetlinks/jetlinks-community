package org.jetlinks.community.reactorql.term;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor(staticName = "of")
@NoArgsConstructor
public class TermType {

    public static final String OPTIONS_NATIVE_SQL = "native";

    private String id;

    private String name;
}
