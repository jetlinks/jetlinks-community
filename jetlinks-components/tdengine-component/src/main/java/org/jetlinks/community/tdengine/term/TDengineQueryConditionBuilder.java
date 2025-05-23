/*
 * Copyright 2025 JetLinks https://www.jetlinks.cn
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jetlinks.community.tdengine.term;

import org.hswebframework.ezorm.core.param.Term;
import org.hswebframework.ezorm.rdb.operator.builder.fragments.AbstractTermsFragmentBuilder;
import org.hswebframework.ezorm.rdb.operator.builder.fragments.SqlFragments;
import org.springframework.util.CollectionUtils;

import java.util.List;

public class TDengineQueryConditionBuilder extends AbstractTermsFragmentBuilder<Object> {


    public static String build(List<Term> terms) {

        if(CollectionUtils.isEmpty(terms)){
            return "";
        }
        SqlFragments fragments = new TDengineQueryConditionBuilder().createTermFragments(null, terms);

        if(fragments.isEmpty()){
            return "";
        }

        return fragments.toRequest().toString();

    }

    @Override
    protected SqlFragments createTermFragments(Object parameter, Term term) {
        String type = term.getTermType();
        TDengineTermType termType = TDengineTermType.valueOf(type.toLowerCase());

        return SqlFragments.single(termType.build("`"+term.getColumn()+"`", term.getValue()));
    }
}
