package org.jetlinks.community.network.channel;

import reactor.core.publisher.Mono;

public interface ChannelProvider {

    String getChannel();

    Mono<ChannelInfo> getChannelInfo(String channelId);

}
