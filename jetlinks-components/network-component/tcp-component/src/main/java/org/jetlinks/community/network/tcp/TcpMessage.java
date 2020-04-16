package org.jetlinks.community.network.tcp;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jetlinks.core.message.codec.EncodedMessage;

/**
 * @author bsetfeng
 * @author zhouhao
 * @since 1.0
 **/
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class TcpMessage implements EncodedMessage {

    private ByteBuf payload;

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();

        ByteBufUtil.appendPrettyHexDump(builder,payload);

        return builder.toString();
    }
}
