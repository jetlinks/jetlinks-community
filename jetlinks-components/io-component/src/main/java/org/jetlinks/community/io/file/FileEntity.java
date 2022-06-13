package org.jetlinks.community.io.file;

import lombok.Getter;
import lombok.Setter;
import org.hswebframework.ezorm.rdb.mapping.annotation.ColumnType;
import org.hswebframework.ezorm.rdb.mapping.annotation.DefaultValue;
import org.hswebframework.ezorm.rdb.mapping.annotation.EnumCodec;
import org.hswebframework.ezorm.rdb.mapping.annotation.JsonCodec;
import org.hswebframework.web.api.crud.entity.GenericEntity;
import org.hswebframework.web.api.crud.entity.RecordCreationEntity;
import org.hswebframework.web.crud.generator.Generators;

import javax.persistence.Column;
import javax.persistence.Table;
import java.sql.JDBCType;
import java.util.Map;

@Getter
@Setter
@Table(name = "s_file")
public class FileEntity extends GenericEntity<String> implements RecordCreationEntity {

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String extension;

    @Column(nullable = false)
    private Long length;

    @Column(nullable = false, length = 32)
    private String md5;

    @Column(nullable = false, length = 64)
    private String sha256;

    @Column(nullable = false)
    @DefaultValue(generator = Generators.CURRENT_TIME)
    private Long createTime;

    @Column(length = 64)
    private String creatorId;

    @Column(length = 64, nullable = false)
    private String serverNodeId;

    @Column(length = 512, nullable = false)
    private String storagePath;

    @Column
    @EnumCodec(toMask = true)
    @ColumnType(jdbcType = JDBCType.BIGINT, javaType = Long.class)
    private FileOption[] options;

    @Column
    @JsonCodec
    @ColumnType(jdbcType = JDBCType.LONGVARCHAR, javaType = String.class)
    private Map<String, Object> others;


    public FileInfo toInfo() {
        return copyTo(new FileInfo());
    }

    public static FileEntity of(FileInfo fileInfo,String storagePath,String serverNodeId) {
        FileEntity fileEntity = new FileEntity().copyFrom(fileInfo);
        fileEntity.setStoragePath(storagePath);
        fileEntity.setServerNodeId(serverNodeId);
        return fileEntity;
    }

}
