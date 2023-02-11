package org.jetlinks.community.relation.terms;

import org.hswebframework.ezorm.core.param.Term;
import org.hswebframework.ezorm.rdb.metadata.RDBColumnMetadata;
import org.hswebframework.ezorm.rdb.operator.builder.fragments.PrepareSqlFragments;
import org.hswebframework.ezorm.rdb.operator.builder.fragments.SqlFragments;
import org.hswebframework.ezorm.rdb.operator.builder.fragments.term.AbstractTermFragmentBuilder;
import org.hswebframework.web.bean.FastBeanCopier;
import org.jetlinks.core.things.relation.ObjectSpec;
import org.jetlinks.core.things.relation.RelationSpec;
import reactor.function.Consumer3;

import java.util.Map;

public class RelatedTermBuilder extends AbstractTermFragmentBuilder {

    public RelatedTermBuilder() {
        super("relation", "按关系信息查询");
    }

    @Override
    public SqlFragments createFragments(String columnFullName, RDBColumnMetadata column, Term term) {

        ObjectSpec spec = convertSpec(term);

        PrepareSqlFragments outside = PrepareSqlFragments.of();

        PrepareSqlFragments fragments=  acceptRelation(PrepareSqlFragments.of(), spec.getRelated(), 1, (last, index, frag) -> {
            String tName = "r" + index;
            String preName = "r" + (index - 1);

            boolean reverse = frag.isEmpty() != last.isReverse();

            frag.addSql("exists(select 1 from s_object_related", tName, "where ");

            if (reverse) {
                frag.addSql(
                    tName + ".relation = ?",
                    "and", tName + ".related_type =", preName + ".related_type",
                    "and", tName + ".related_id =", preName + ".related_id",
                    "and", tName + ".object_type = ? and", tName + ".object_id=?"
                );

            } else {
                frag.addSql(
                    tName + ".relation = ?",
                    "and", tName + ".object_type =", preName + ".object_type",
                    "and", tName + ".related_id =", preName + ".related_id",
                    "and", tName + ".related_type = ? and", tName + ".related_id=?"
                );
            }


            frag.addParameter(last.getRelation(), spec.getObjectType(), spec.getObjectId());
            frag.addSql(")");
            outside
                .addSql("exists(select 1 from s_object_related r0 where r0.object_id =", columnFullName, " and r0.object_type =?")
                .addParameter(last.getObjectType())
                .addSql("and")
                ;
        });
        outside.addFragments(fragments).addSql(")");

        /*

          exists(select 1 from s_object_related r1 where object_id = t.id and r.object_type = 'device'
             and exists(
                 select 1 from s_object_related r2
                     where r2.relation='manager'
                     and r2.object_type = 'user'
                     and r2.related_type = r.object_type
                     and r2.related_id = r.object_id
                  and exists(
                      select 1 from s_object_related r2 where r2.relation='manager' and r.object_type = 'user'
                )
          )

        * */

        return outside;
    }

    protected PrepareSqlFragments acceptRelation(PrepareSqlFragments sqlFragments,
                                                 RelationSpec spec,
                                                 int index,
                                                 Consumer3<RelationSpec, Integer, PrepareSqlFragments> last) {

        if (spec.getNext() != null) {
            String tName = "r" + index;
            String preName = "r" + (index - 1);
            boolean reverse = spec.isReverse();

            sqlFragments
                .addSql("exists(select 1 from s_object_related", tName, "where ");

            if (reverse) {
                sqlFragments.addSql(
                    tName + ".relation = ?",
                    "and", tName + ".related_type =", preName + ".object_type",
                    "and", tName + ".related_id =", preName + ".object_id",
                    "and", tName + ".related_type = ?"
                );
            } else {
                sqlFragments.addSql(
                    tName + ".relation = ?",
                    "and", tName + ".object_type =", preName + ".object_type",
                    "and", tName + ".object_id =", preName + ".object_id",
                    "and", tName + ".related_type = ?"
                );

            }
            sqlFragments.addParameter(spec.getRelation(), spec.getObjectType());

            sqlFragments.addSql("and");
            acceptRelation(sqlFragments, spec.getNext(), ++index, last);
            sqlFragments.addSql(")");
        } else {
            last.accept(spec, index, sqlFragments);
        }
        return sqlFragments;
    }

    private ObjectSpec convertSpec(Term term) {
        Object value = term.getValue();
        if (value instanceof ObjectSpec) {
            return ((ObjectSpec) value);
        }
        if (value instanceof String) {
            return ObjectSpec.parse(String.valueOf(value));
        }
        if (value instanceof Map) {
            return FastBeanCopier.copy(value, new ObjectSpec());
        }
        throw new UnsupportedOperationException("unsupported relation term value:" + value);
    }
}
