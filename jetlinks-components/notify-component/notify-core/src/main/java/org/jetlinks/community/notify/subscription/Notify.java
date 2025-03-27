package org.jetlinks.community.notify.subscription;

import lombok.*;
import org.jetlinks.core.utils.SerializeUtils;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

@AllArgsConstructor(staticName = "of")
@Getter
@Setter
@NoArgsConstructor
@Generated
public class Notify implements Externalizable {
    private String message;

    private String dataId;

    private long notifyTime;

    private String code;

    private Object detail;


    public static Notify of(String message, String dataId, long timestamp) {
        return new Notify(message, dataId, timestamp, null, null);
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeUTF(message);
        out.writeUTF(dataId);
        out.writeLong(notifyTime);
        SerializeUtils.writeNullableUTF(code,out);
        SerializeUtils.writeObject(detail,out);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        message = in.readUTF();
        dataId = in.readUTF();
        notifyTime = in.readLong();
        code = SerializeUtils.readNullableUTF(in);
        detail = SerializeUtils.readObject(in);
    }
}
