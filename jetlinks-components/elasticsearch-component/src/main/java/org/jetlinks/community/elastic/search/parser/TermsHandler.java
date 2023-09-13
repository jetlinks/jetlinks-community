package org.jetlinks.community.elastic.search.parser;

import lombok.Getter;
import lombok.Setter;
import org.hswebframework.ezorm.core.param.Term;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 根据terms条件的type分组
 * 如：条件 where a = 1 or b = 2 and c = 3 or d = 4 or e = 5 and f = 6
 * 分组为
 * (a = 1)
 * OR
 * (b = 2 AND c = 3)
 * OR
 * (d = 4)
 * OR
 * (e = 5 AND f = 6)
 *
 * @author bestfeng
 */
public class TermsHandler {

    public static Set<TermGroup> groupTerms(List<Term> terms) {
        Set<TermGroup> groups = new HashSet<>();
        TermGroup currentGroup = null;

        for (int i = 0; i < terms.size(); i++) {
            Term currentTerm = terms.get(i);
            Term nextTerm = (i + 1 < terms.size()) ? terms.get(i + 1) : null;

            if (currentTerm.getType() == Term.Type.or){
                if (currentGroup == null || currentGroup.type == Term.Type.and){
                    currentGroup = new TermGroup(Term.Type.or);
                }
                // 如果下一个Term为"AND"，创建一个新分组
                if (nextTerm != null && nextTerm.getType() == Term.Type.and){
                    currentGroup = new TermGroup(Term.Type.and);
                }
            }else {
                if (currentGroup == null){
                    currentGroup = new TermGroup(Term.Type.and);
                }
            }
            currentGroup.addTerm(currentTerm);
            groups.add(currentGroup);
        }
        return groups;
    }

    @Getter
    @Setter
    public static class TermGroup {
        public TermGroup(Term.Type type) {
            this.type = type;
            this.terms = new ArrayList<>();
        }

        List<Term> terms;

        Term.Type type;
        public void addTerm(Term term) {
            terms.add(term);
        }
    }

}
