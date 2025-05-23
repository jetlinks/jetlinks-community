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
package org.jetlinks.community.datasource.rdb;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.hswebframework.web.crud.configuration.DialectProvider;
import org.hswebframework.web.crud.configuration.DialectProviders;
import org.hswebframework.web.crud.configuration.EasyormProperties;
import org.hswebframework.web.exception.ValidationException;
import org.hswebframework.web.validator.ValidatorUtils;

import jakarta.validation.constraints.NotBlank;
import java.net.URI;
import java.util.Map;

@Getter
@Setter
public class RDBDataSourceProperties {

    private Type type;

    @Schema(description = "url")
    @NotBlank
    private String url;

    @NotBlank
    @Schema(description = "数据库")
    private String schema;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "密码")
    private String password;

    private Map<String, Object> others;

    @Schema(description = "数据库方言")
    private String dialect;

    @Getter(AccessLevel.PRIVATE)
    @Setter(AccessLevel.PRIVATE)
    private transient DialectProvider dialectProvider;

    public String getValidateQuery() {
        return dialectProvider().getValidationSql();
    }

    public DialectProvider dialectProvider() {
        URI uri = URI.create(getUrl());

        if (null == dialectProvider) {
            if (null != dialect) {
                dialectProvider = DialectProviders.lookup(dialect);
            } else if (getUrl().contains("mysql") || getUrl().contains("mariadb")) {
                return EasyormProperties.DialectEnum.mysql;
            } else if (getUrl().contains("postgresql")) {
                return EasyormProperties.DialectEnum.postgres;
            } else if (getUrl().contains("oracle")) {
                return EasyormProperties.DialectEnum.oracle;
            } else if (getUrl().contains("mssql") || getUrl().contains("sqlserver")) {
                return EasyormProperties.DialectEnum.mssql;
            } else if (getUrl().contains("h2")) {
                return EasyormProperties.DialectEnum.h2;
            } else if (getUrl().contains("dm")) {
                return DialectProviders.lookup("dm");
            } else {
                throw new ValidationException("url", "error.unsupported_database_type", uri.getFragment());
            }
        }
        return dialectProvider;
    }

    public RDBDataSourceProperties validate() {
        ValidatorUtils.tryValidate(this);
        return this;
    }

    public Type getType() {
        if (type == null) {
            type = url.startsWith("r2dbc") ? Type.r2dbc : Type.jdbc;
        }
        return type;
    }


    public enum Type {
        jdbc,
        r2dbc;
    }

}
