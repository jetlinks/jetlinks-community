package org.jetlinks.community.device.service.term;

import org.hswebframework.ezorm.core.param.Term;
import org.hswebframework.ezorm.rdb.metadata.RDBColumnMetadata;
import org.hswebframework.ezorm.rdb.operator.builder.fragments.PrepareSqlFragments;
import org.hswebframework.ezorm.rdb.operator.builder.fragments.SqlFragments;
import org.hswebframework.ezorm.rdb.operator.builder.fragments.term.AbstractTermFragmentBuilder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 根据产品分类查询与产品关联的数据,如: 查询某个分类下的产品列表.
 * <p>
 * <b>
 * 注意: 查询时指定列名是和产品ID关联的列或者实体类属性名.
 * 如: 查询设备列表时则使用productId.
 * 此条件仅支持关系型数据库中的查询.
 * </b>
 * <p>
 * 在通用查询接口中可以使用动态查询参数中的<code>term.termType</code>来使用此功能.
 * <a href="https://doc.jetlinks.cn/interface-guide/query-param.html">查看动态查询参数说明</a>
 * <p>
 * 在内部通用条件中,可以使用DSL方式创建条件,例如:
 * <pre>
 *     createQuery()
 *     .where()
 *     .and("productId","dev-prod-cat",cateId)
 *     .fetch()
 * </pre>
 *
 * @author zhouhao
 * @since 1.3
 */
@Component
public class DeviceCategoryTerm extends AbstractTermFragmentBuilder {

    public DeviceCategoryTerm() {
        super("dev-prod-cat", "按产品品类查询");
    }

    @Override
    public SqlFragments createFragments(String columnFullName, RDBColumnMetadata column, Term term) {

        PrepareSqlFragments sqlFragments = PrepareSqlFragments.of();

        List<Object> idList = convertList(column, term);

        sqlFragments.addSql("exists(select 1 from dev_product prod where prod.id =", columnFullName);

        sqlFragments.addSql("and exists(select 1 from dev_product_category g where g.id = prod.classified_id and ");
        sqlFragments.addSql(
            idList
                .stream()
                .map(r -> "path like (select concat(path,'%') from dev_product_category g2 where g2.id = ?)")
                .collect(Collectors.joining(" or ", "(", ")"))
            , ")")
                    .addParameter(idList);

        sqlFragments.addSql(")");

        return sqlFragments;
    }
}
