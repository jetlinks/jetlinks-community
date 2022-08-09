package org.jetlinks.community.rule.engine.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.hswebframework.web.api.crud.entity.GenericEntity;
import org.springframework.data.annotation.Id;
import org.springframework.stereotype.Component;

import javax.persistence.Column;
import javax.persistence.Table;


@Table(name="s_test")
@Getter
@Setter
public class TestEntity extends GenericEntity<String> {

//    @Id
//    @Column(nullable = false, updatable = false)
//    @Schema(description = "key")
//    private String id;

    @Column(name="svalue")
    @Schema(description = "value")
    private String value;

    @Override
    public String toString() {
        return "TestEntity{" +
            "id='"+ getId()+"'\n"+
            "value='" + value + '\'' +
            '}';
    }
}
