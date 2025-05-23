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
package org.jetlinks.community.relation.impl;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
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
public class SimpleRelation implements Relation, Externalizable {
    private String id;
    private String name;
    private String reverseName;
    private boolean reverse;
    private Map<String, Object> expands;

    @Deprecated
    public SimpleRelation(String id, String name, Map<String, Object> expands) {
        this.id = id;
        this.name = name;
        this.reverseName = StringUtils.EMPTY;
        this.expands = expands;
        this.reverse = false;
    }

    public static SimpleRelation of(String objectType, RelationEntity entity) {
        return of(entity.getId(), entity.getName(), entity.getReverseName(), objectType.equals(entity.getTargetType()), entity.getExpands());
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeUTF(id);
        out.writeUTF(name);
        SerializeUtils.writeObject(expands,out);
        out.writeUTF(reverseName);
        out.writeBoolean(reverse);
    }

    @Override
    @SuppressWarnings("all")
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        id = in.readUTF();
        name = in.readUTF();
        expands = (Map<String,Object>)SerializeUtils.readObject(in);
        reverseName = in.readUTF();
        reverse = in.readBoolean();
    }
}
