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
import reactor.util.context.Context;
import reactor.util.context.ContextView;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Optional;

@AllArgsConstructor(staticName = "of")
@NoArgsConstructor
@Getter
@Setter
public class OperationSource implements Externalizable {

    private static final long serialVersionUID = 1L;

    /**
     * ID,type对应操作的唯一标识
     */
    private String id;

    /**
     * 操作源名称
     */
    private String name;

    /**
     * 操作目标,通常为ID对应的详情数据
     */
    private Object data;

    public static OperationSource of(String id, Object data) {
        return of(id, id, data);
    }

    public static Context ofContext(String id, String name, Object data) {
        return Context.of(OperationSource.class, of(id, name, data));
    }

    public static Optional<OperationSource> fromContext(ContextView ctx) {
        return ctx.getOrEmpty(OperationSource.class);
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeUTF(id);
        SerializeUtils.writeObject(name, out);
        SerializeUtils.writeObject(data, out);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        id = in.readUTF();
        name = (String) SerializeUtils.readObject(in);
        data = SerializeUtils.readObject(in);
    }
}
