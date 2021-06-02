package org.jetlinks.community.auth.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hswebframework.web.system.authorization.api.entity.UserEntity;

@Getter
@Setter
@NoArgsConstructor
public class UserDetail {

    private String id;

    private String name;

    private String email;

    private String telephone;

    private String avatar;

    private String description;

    private String username;

    private long createTime;

    public static UserDetail of(UserEntity entity,UserDetailEntity detailEntity) {
        return new UserDetail().with(entity).with(detailEntity);
    }

    public UserDetail with(UserDetailEntity entity) {
        this.setAvatar(entity.getAvatar());
        this.setDescription(entity.getDescription());
        this.setTelephone(entity.getTelephone());
        this.setEmail(entity.getEmail());

        return this;
    }

    public UserDetail with(UserEntity entity) {
        this.setId(entity.getId());
        this.setName(entity.getName());
        if (entity.getCreateTime() != null) {
            setCreateTime(entity.getCreateTime());
        }
        return this;
    }

}
