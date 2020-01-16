package org.jetlinks.community.network.tcp;

import io.netty.buffer.ByteBuf;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jetlinks.core.message.codec.EncodedMessage;
import org.jetlinks.core.message.codec.MessagePayloadType;
import org.jetlinks.rule.engine.executor.PayloadType;

import javax.annotation.Nonnull;

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

    //private MessagePayloadType payloadType;


}
