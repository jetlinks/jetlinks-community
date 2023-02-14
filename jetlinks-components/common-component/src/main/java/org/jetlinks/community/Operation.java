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
