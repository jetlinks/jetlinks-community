package org.jetlinks.community.network.http;

import io.netty.buffer.ByteBuf;
import lombok.AllArgsConstructor;
import lombok.Generated;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jetlinks.core.message.codec.http.Header;
import org.jetlinks.core.message.codec.http.HttpResponseMessage;
import org.springframework.http.MediaType;

import java.util.ArrayList;
import java.util.List;

/**
 * @author bsetfeng
 * @since 1.0
 **/
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Generated
public class DefaultHttpResponseMessage implements HttpResponseMessage {

    private int status;

    private MediaType contentType;

    private List<Header> headers = new ArrayList<>();

    private ByteBuf payload;

    @Override
    public String toString() {
        return print();
    }
}
