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
package org.jetlinks.community;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jetlinks.core.utils.SerializeUtils;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * 描述,用于对某些操作的通用描述.
 *
 * @author zhouhao
 * @since 2.0
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(staticName = "of")
public class Operation implements Externalizable {
    /**
     * 操作来源
     */
    private OperationSource source;

    /**
     * 操作类型，比如: transparent-codec等
     */
    private OperationType type;

    @Override
    public String toString() {
        return type.getId() + "(" + type.getName() + "):[" + source.getId() + "]";
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        source.writeExternal(out);
        SerializeUtils.writeObject(type.getId(), out);
        SerializeUtils.writeObject(type.getName(), out);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        source = new OperationSource();
        source.readExternal(in);
        type = new DynamicOperationType((String) SerializeUtils.readObject(in), (String) SerializeUtils.readObject(in));
    }
}
