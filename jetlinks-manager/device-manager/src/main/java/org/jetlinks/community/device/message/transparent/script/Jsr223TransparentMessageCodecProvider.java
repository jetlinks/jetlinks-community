package org.jetlinks.community.device.message.transparent.script;

import org.hswebframework.web.exception.ValidationException;
import org.jetlinks.community.device.message.transparent.SimpleTransparentMessageCodec;
import org.jetlinks.community.device.message.transparent.TransparentMessageCodec;
import org.jetlinks.community.device.message.transparent.TransparentMessageCodecProvider;
import org.jetlinks.community.script.Script;
import org.jetlinks.community.script.Scripts;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.function.Function;

@Component
public class Jsr223TransparentMessageCodecProvider implements TransparentMessageCodecProvider {

    @Override
    public String getProvider() {
        return "jsr223";
    }

    @Override
    public Mono<TransparentMessageCodec> createCodec(Map<String, Object> configuration) {
        String lang = (String) configuration.getOrDefault("lang", "js");
        String script = (String) configuration.get("script");
        Assert.hasText(lang, "lang can not be null");
        Assert.hasText(script, "script can not be null");

        CodecContext context = new CodecContext();

        SimpleTransparentMessageCodec.Codec codec = Scripts
            .getFactory(lang)
            .bind(Script.of("jsr223-transparent", script),
                  SimpleTransparentMessageCodec.Codec.class);

        if (context.encoder == null && codec != null) {
            context.onDownstream(codec::encode);
        }
        if (context.decoder == null && codec != null) {
            context.onUpstream(codec::decode);
        }

        if (codec == null && context.encoder == null && context.decoder == null) {
            return Mono.error(new ValidationException("script", "error.codec_message_undefined"));
        }
        return Mono
            .deferContextual(ctx -> Mono
                .just(
                    new SimpleTransparentMessageCodec(context)
                ));
    }

    public static class CodecContext implements SimpleTransparentMessageCodec.Codec {

        private Function<SimpleTransparentMessageCodec.EncodeContext, Object> encoder;
        private Function<SimpleTransparentMessageCodec.DecodeContext, Object> decoder;

        public void onDownstream(Function<SimpleTransparentMessageCodec.EncodeContext, Object> encoder) {
            this.encoder = encoder;
        }

        public void onUpstream(Function<SimpleTransparentMessageCodec.DecodeContext, Object> decoder) {
            this.decoder = decoder;
        }

        @Override
        public Object decode(SimpleTransparentMessageCodec.DecodeContext context) {
            if (decoder == null) {
                return null;
            }
            return decoder.apply(context);
        }

        @Override
        public Object encode(SimpleTransparentMessageCodec.EncodeContext context) {
            if (encoder == null) {
                return null;
            }
            return encoder.apply(context);
        }

    }

}
