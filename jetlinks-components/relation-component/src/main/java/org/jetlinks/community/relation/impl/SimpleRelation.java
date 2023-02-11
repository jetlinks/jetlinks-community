package org.jetlinks.community.relation.impl;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jetlinks.core.things.relation.Relation;
import org.jetlinks.core.utils.SerializeUtils;
import org.jetlinks.community.relation.entity.RelationEntity;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Map;

@Getter
@Setter
@AllArgsConstructor(staticName = "of")
@NoArgsConstructor
public class SimpleRelation implements Relation , Externalizable {
    private String id;
    private String name;
    private Map<String,Object> expands;


    public static SimpleRelation of(RelationEntity entity){
        return of(entity.getId(),entity.getName(),entity.getExpands());
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeUTF(id);
        out.writeUTF(name);
        SerializeUtils.writeObject(expands,out);
    }

    @Override
    @SuppressWarnings("all")
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        id = in.readUTF();
        name = in.readUTF();
        expands = (Map<String,Object>)SerializeUtils.readObject(in);
    }
}
