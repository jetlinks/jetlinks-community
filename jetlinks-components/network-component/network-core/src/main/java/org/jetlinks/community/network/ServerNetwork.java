package org.jetlinks.community.network;

import javax.annotation.Nullable;
import java.net.InetSocketAddress;

/**
 * 网络服务
 *
 * @author zhouhao
 * @since 1.2
 */
public interface ServerNetwork extends Network {

    /**
     * 获取网络服务绑定的套接字端口信息
     *
     * @return InetSocketAddress
     */
    @Nullable
    InetSocketAddress getBindAddress();

}
