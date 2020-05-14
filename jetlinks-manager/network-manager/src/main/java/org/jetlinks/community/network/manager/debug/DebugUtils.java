package org.jetlinks.community.network.manager.debug;

import io.netty.buffer.ByteBufUtil;
import org.springframework.util.StringUtils;

public class DebugUtils {

    static byte[] stringToBytes(String text){
        byte[] payload;
        if (StringUtils.isEmpty(text)) {
            payload = new byte[0];
        } else {
            if (text.startsWith("0x")) {
                payload = ByteBufUtil.decodeHexDump(text, 2, text.length()-2);
            } else {
                payload = text.getBytes();
            }
        }
        return payload;
    }

}
