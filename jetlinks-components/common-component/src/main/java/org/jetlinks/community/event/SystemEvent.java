package org.jetlinks.community.event;

import lombok.Getter;
import lombok.Setter;
import org.jetlinks.core.utils.SerializeUtils;
import org.jetlinks.community.Operation;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

@Getter
@Setter
public class SystemEvent implements Externalizable {
    private static final long serialVersionUID = 1L;

    private Level level;

    private String code;

    private Operation operation;

    /**
     * 描述详情，不同的类型详情内容不同
     *
     * @see org.jetlinks.community.monitor.ExecutionMonitorInfo
     */
    private Object detail;

    private long timestamp;

    public SystemEvent(Level level, String code, Operation operation, Object detail) {
        this.level = level;
        this.code = code;
        this.operation = operation;
        this.detail = detail;
        this.timestamp = System.currentTimeMillis();
    }

    public SystemEvent() {
    }


    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeByte(level.ordinal());

        out.writeUTF(code);

        operation.writeExternal(out);

        SerializeUtils.writeObject(detail, out);

        out.writeLong(timestamp);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        level = Level.values()[in.readByte()];

        code = in.readUTF();

        operation = new Operation();
        operation.readExternal(in);

        detail = SerializeUtils.readObject(in);
        timestamp = in.readLong();
    }

    public enum Level {
        info,
        warn,
        error
    }

}
