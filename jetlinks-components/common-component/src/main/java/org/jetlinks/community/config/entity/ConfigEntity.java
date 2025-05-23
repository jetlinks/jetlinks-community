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
package org.jetlinks.community.config.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.hswebframework.ezorm.rdb.mapping.annotation.ColumnType;
import org.hswebframework.ezorm.rdb.mapping.annotation.JsonCodec;
import org.hswebframework.web.api.crud.entity.GenericEntity;
import org.hswebframework.web.crud.annotation.EnableEntityEvent;
import org.hswebframework.web.utils.DigestUtils;
import org.springframework.util.StringUtils;

import javax.persistence.Column;
import javax.persistence.Index;
import javax.persistence.Table;
import java.sql.JDBCType;
import java.util.Map;

@Table(name = "s_config", indexes = {
    @Index(name = "idx_conf_scope", columnList = "scope")
})
@Getter
@Setter
@EnableEntityEvent
public class ConfigEntity extends GenericEntity<String> {

    @Column(length = 64, nullable = false, updatable = false)
    @Schema(description = "作用域")
    private String scope;

    @Column(nullable = false)
    @Schema
    @JsonCodec
    @ColumnType(jdbcType = JDBCType.LONGVARCHAR)
    private Map<String,Object> properties;

    @Override
    public String getId() {
        if (!StringUtils.hasText(super.getId())) {
            setId(generateId(scope));
        }
        return super.getId();
    }

    public static String generateId(String scope) {
        return DigestUtils.md5Hex(String.join("|", scope));
    }
}
