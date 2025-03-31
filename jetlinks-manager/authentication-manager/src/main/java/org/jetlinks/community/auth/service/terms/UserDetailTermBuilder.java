package org.jetlinks.community.auth.service.terms;

import org.jetlinks.community.terms.SubTableTermFragmentBuilder;
import org.springframework.stereotype.Component;

/**
 * 根据用户详情条件 查询用户.
 * <p>
 * 将用户详情的条件（手机号、邮箱）嵌套到此条件中
 * <pre>{@code
 * "terms": [
 *      {
 *          "column": "id$user-detail",
 *          "value": [
 *              {
 *              "column": "telephone",
 *              "termType": "like",
 *              "value": "%888%"
 *              },
 *              {
 *              "column": "email",
 *              "termType": "like",
 *              "value": "%123%"
 *              }
 *          ]
 *      }
 *  ]
 * }</pre>
 * @author zhangji 2022/6/29
 */
@Component
public class UserDetailTermBuilder extends SubTableTermFragmentBuilder {
    public UserDetailTermBuilder() {
        super("user-detail", "按用户详情查询");
    }

    @Override
    protected String getTableAlias() {
        return "_detail";
    }

    @Override
    protected String getSubTableName() {
        return "s_user_detail";
    }

}
