package org.jetlinks.community.rule.engine.scene.term;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * @deprecated
 * @see org.jetlinks.community.reactorql.term.TermType
 */
@Getter
@Setter
@AllArgsConstructor(staticName = "of")
@NoArgsConstructor
public class TermType {
    private String id;

    private String name;
}
