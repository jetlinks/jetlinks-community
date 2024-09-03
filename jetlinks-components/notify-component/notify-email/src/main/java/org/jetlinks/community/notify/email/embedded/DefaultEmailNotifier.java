package org.jetlinks.community.notify.email.embedded;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.hswebframework.web.bean.FastBeanCopier;
import org.hswebframework.web.exception.BusinessException;
import org.hswebframework.web.id.IDGenerator;
import org.hswebframework.web.validator.ValidatorUtils;
import org.jetlinks.community.io.file.FileManager;
import org.jetlinks.community.notify.*;
import org.jetlinks.community.notify.email.EmailProvider;
import org.jetlinks.community.notify.template.TemplateManager;
import org.jetlinks.core.Values;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.InputStreamSource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.NettyDataBuffer;
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import javax.annotation.Nonnull;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeUtility;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * 使用javax.mail进行邮件发送
 *
 * @author bsetfeng
 * @author zhouhao
 * @since 1.0
 **/
@Slf4j
public class DefaultEmailNotifier extends AbstractNotifier<EmailTemplate> {


    @Getter
    @Setter
    private JavaMailSender javaMailSender;

    @Getter
    @Setter
    private String sender;

    @Getter
    @Setter
    private String username;

    @Getter
    private final String notifierId;

    @Setter
    private boolean enableFileSystemAttachment = Boolean.getBoolean("email.attach.local-file.enabled");

    public static Scheduler scheduler = Schedulers.boundedElastic();


    private final FileManager fileManager;

    public DefaultEmailNotifier(NotifierProperties properties,
                                TemplateManager templateManager,
                                FileManager fileManager) {
        this(properties.getId(),
            FastBeanCopier.copy(properties.getConfiguration(), new DefaultEmailProperties()),
            templateManager,
            fileManager);

    }

    public DefaultEmailNotifier(String id,
                                DefaultEmailProperties properties,
                                TemplateManager templateManager,
                                FileManager fileManager) {
        super(templateManager);
        ValidatorUtils.tryValidate(properties);
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(properties.getHost());
        mailSender.setPort(properties.getPort());
        mailSender.setUsername(properties.getUsername());
        mailSender.setPassword(properties.getPassword());
        mailSender.setJavaMailProperties(properties.createJavaMailProperties());
        this.notifierId = id;
        this.sender = properties.getSender();
        this.username = properties.getUsername();
        this.javaMailSender = mailSender;
        this.fileManager = fileManager;
    }

    @Nonnull
    @Override
    public Mono<Void> send(@Nonnull EmailTemplate template, @Nonnull Values context) {
        return Mono.just(template)
            .flatMap(temp -> convert(temp, context.getAllValues()))
            .flatMap(this::doSend);
    }

    @Nonnull
    @Override
    public Mono<Void> close() {
        return Mono.empty();
    }

    @Nonnull
    @Override
    public NotifyType getType() {
        return DefaultNotifyType.email;
    }

    @Nonnull
    @Override
    public Provider getProvider() {
        return EmailProvider.embedded;
    }

    protected Mono<Void> doSend(ParsedEmailTemplate template) {
        return Mono
            .fromCallable(() -> {
                MimeMessage mimeMessage = this.javaMailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "utf-8");

                if (StringUtils.isNotBlank(this.username)) {
                    helper.setFrom(this.sender + '<' + this.username + '>');
                } else {
                    helper.setFrom(this.sender);
                }
                helper.setTo(template.getSendTo().toArray(new String[0]));
                helper.setSubject(template.getSubject());
                helper.setText(new String(template.getText().getBytes(), StandardCharsets.UTF_8), true);

                return Flux
                    .fromIterable(template.getAttachments().entrySet())
                    .flatMap(entry -> Mono
                        .zip(Mono.just(entry.getKey()), convertResource(entry.getValue()))
                        .onErrorResume(err -> Mono
                            .error(() -> new BusinessException.NoStackTrace("error.load_attachment_failed",
                                                                            500,
                                                                            entry.getKey(),
                                                                            err.getMessage())))
                    )
                    .flatMap(tp2 -> Mono
                        .fromCallable(() -> {
                            //添加附件
                            helper.addAttachment(MimeUtility.encodeText(tp2.getT1()), tp2.getT2());
                            return helper;
                        }))
                    .then(
                        Flux.fromIterable(template.getImages().entrySet())
                            .flatMap(entry -> Mono.zip(Mono.just(entry.getKey()), convertResource(entry.getValue())))
                            .flatMap(tp2 -> Mono
                                .fromCallable(() -> {
                                    //添加图片资源
                                    helper.addInline(tp2.getT1(), tp2.getT2(), MediaType.APPLICATION_OCTET_STREAM_VALUE);
                                    return helper;
                                }))
                            .then()
                    )
                    .thenReturn(mimeMessage);

            })
            .flatMap(Function.identity())
            .doOnNext(message -> this.javaMailSender.send(message))
            .subscribeOn(scheduler)
            .then()
            ;
    }


    protected Mono<? extends InputStreamSource> convertResource(String resource) {
        if (resource.startsWith("http")) {
            return WebClient
                .create()
                .get()
                .uri(resource)
                .accept(MediaType.APPLICATION_OCTET_STREAM)
                .exchangeToMono(res -> res.bodyToMono(Resource.class));
        } else if (resource.startsWith("data:") && resource.contains(";base64,")) {
            String base64 = resource.substring(resource.indexOf(";base64,") + 8);
            return Mono.just(
                new ByteArrayResource(Base64.decodeBase64(base64))
            );
        } else if (enableFileSystemAttachment && resource.contains("/")) {
            return Mono.just(
                new FileSystemResource(resource)
            );
        } else {
            return fileManager
                .read(resource)
                .as(DataBufferUtils::join)
                .map(dataBuffer -> {
                    try {
                        ByteBuf buf = dataBuffer instanceof NettyDataBuffer
                            ? ((NettyDataBuffer) dataBuffer).getNativeBuffer()
                            : Unpooled.wrappedBuffer(dataBuffer.asByteBuffer());
                        return new ByteArrayResource(ByteBufUtil.getBytes(buf));
                    } finally {
                        DataBufferUtils.release(dataBuffer);
                    }
                })
                .onErrorMap(error -> new UnsupportedOperationException("不支持的文件地址:" + resource, error))
                .switchIfEmpty(Mono.error(() -> new UnsupportedOperationException("不支持的文件地址:" + resource)));
        }
    }


    public static Mono<ParsedEmailTemplate> convert(EmailTemplate template, Map<String, Object> context) {
        return template
            .getSendTo(context)
            .flatMapMany(Flux::fromIterable)
            .map(receiver -> template.render(receiver, context))
            .collectList()
            .map(sendToList -> {
                String subject = template.getSubject();
                String text = template.getText();
                if (CollectionUtils.isEmpty(sendToList) || ObjectUtils.isEmpty(subject) || ObjectUtils.isEmpty(text)) {
                    throw new BusinessException("模板内容错误，sendTo, text 或者 subject 不能为空.");
                }

                String sendText = template.render(text, context);
                List<EmailTemplate.Attachment> tempAttachments = template.getAttachments();

                Map<String, String> attachments = new LinkedHashMap<>();

                if (tempAttachments != null) {
                    int index = 0;
                    for (EmailTemplate.Attachment tempAttachment : tempAttachments) {
                        index++;

                        String name = template.render(tempAttachment.getName(), context);

                        String location = template.get(tempAttachment.getLocation(), EmailTemplate.Attachment.locationKey(index), context);

                        attachments.put(name, location);
                    }
                }

                Map<String, String> images = new HashMap<>();

                sendText = extractSendTextImage(sendText, images);

                return ParsedEmailTemplate
                    .builder()
                    .attachments(attachments)
                    .images(images)
                    .text(sendText)
                    .subject(template.render(subject, context))
                    .sendTo(sendToList)
                    .build();
            });
    }


    private static String extractSendTextImage(String sendText, Map<String, String> images) {
        if (!sendText.contains("<")) {
            return sendText;
        }
        boolean anyImage = false;

        Document doc = Jsoup.parse(sendText);
        for (Element src : doc.getElementsByTag("img")) {
            String s = src.attr("src");
            if (s.startsWith("http")) {
                continue;
            }
            anyImage = true;
            String tempKey = IDGenerator.MD5.generate();
            src.attr("src", "cid:".concat(tempKey));
            images.put(tempKey, s);
        }
        return anyImage ? doc.html() : sendText;
    }


}
