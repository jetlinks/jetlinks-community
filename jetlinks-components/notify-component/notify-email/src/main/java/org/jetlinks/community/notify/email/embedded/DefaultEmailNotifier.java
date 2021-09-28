package org.jetlinks.community.notify.email.embedded;

import com.alibaba.fastjson.JSONObject;
import io.vavr.control.Try;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.hswebframework.web.exception.BusinessException;
import org.hswebframework.web.id.IDGenerator;
import org.hswebframework.web.utils.ExpressionUtils;
import org.hswebframework.web.utils.TemplateParser;
import org.hswebframework.web.validator.ValidatorUtils;
import org.jetlinks.community.notify.*;
import org.jetlinks.community.notify.email.EmailProvider;
import org.jetlinks.community.notify.template.TemplateManager;
import org.jetlinks.core.Values;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.core.io.*;
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import javax.annotation.Nonnull;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeUtility;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

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
    private final String notifierId;

    @Setter
    private boolean enableFileSystemAttachment = Boolean.getBoolean("email.attach.local-file.enabled");

    public static Scheduler scheduler = Schedulers.elastic();

    public DefaultEmailNotifier(NotifierProperties properties, TemplateManager templateManager) {
        this(properties.getId(),
             new JSONObject(properties.getConfiguration()).toJavaObject(DefaultEmailProperties.class),
             templateManager);
    }

    public DefaultEmailNotifier(String id,
                                DefaultEmailProperties properties,
                                TemplateManager templateManager) {
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
        this.javaMailSender = mailSender;
    }

    @Nonnull
    @Override
    public Mono<Void> send(@Nonnull EmailTemplate template, @Nonnull Values context) {
        return Mono.just(template)
                   .map(temp -> convert(temp, context.getAllValues()))
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

                helper.setFrom(this.sender);
                helper.setTo(template.getSendTo().toArray(new String[0]));
                helper.setSubject(template.getSubject());
                helper.setText(new String(template.getText().getBytes(), StandardCharsets.UTF_8), true);

                return Flux
                    .fromIterable(template.getAttachments().entrySet())
                    .flatMap(entry -> Mono.zip(Mono.just(entry.getKey()), convertResource(entry.getValue())))
                    .doOnNext(tp -> Try
                        .run(() -> helper.addAttachment(MimeUtility.encodeText(tp.getT1()), tp.getT2())).get())
                    .then(Flux
                              .fromIterable(template.getImages().entrySet())
                              .flatMap(entry -> Mono.zip(Mono.just(entry.getKey()), convertResource(entry.getValue())))
                              .doOnNext(tp -> Try
                                  .run(() -> helper.addInline(tp.getT1(), tp.getT2(), MediaType.APPLICATION_OCTET_STREAM_VALUE))
                                  .get())
                              .then()
                    ).thenReturn(mimeMessage)
                    ;

            })
            .flatMap(Function.identity())
            .doOnNext(message -> this.javaMailSender.send(message))
            .subscribeOn(scheduler)
            .then()
            ;
    }


    protected Mono<InputStreamSource> convertResource(String resource) {
        if (resource.startsWith("http")) {
            return WebClient
                .create()
                .get()
                .uri(resource)
                .accept(MediaType.APPLICATION_OCTET_STREAM)
                .exchange()
                .flatMap(rep -> rep.bodyToMono(Resource.class));
        } else if (resource.startsWith("data:") && resource.contains(";base64,")) {
            String base64 = resource.substring(resource.indexOf(";base64,") + 8);
            return Mono.just(
                new ByteArrayResource(Base64.decodeBase64(base64))
            );
        } else if (enableFileSystemAttachment) {
            return Mono.just(
                new FileSystemResource(resource)
            );
        } else {
            throw new UnsupportedOperationException("不支持的文件地址:" + resource);
        }
    }


    public static ParsedEmailTemplate convert(EmailTemplate template, Map<String, Object> context) {
        List<String> sendTo = template.getSendTo()
                                      .stream()
                                      .map(s -> render(s, context))
                                      .collect(Collectors.toList());
        String subject = template.getSubject();
        String text = template.getText();
        if (StringUtils.isEmpty(subject) || StringUtils.isEmpty(text)) {
            throw new BusinessException("模板内容错误，text 或者 subject 不能为空.");
        }
        String sendText = render(text, context, true);
        List<EmailTemplate.Attachment> tempAttachments = template.getAttachments();
        Map<String, String> attachments = new HashMap<>();

        if (tempAttachments != null) {
            List<EmailTemplate.Attachment> distinctAttachment = tempAttachments
                .stream()
                .distinct()
                .collect(Collectors.toList());
            for (EmailTemplate.Attachment tempAttachment : distinctAttachment) {
                attachments.put(tempAttachment.getName(), render(tempAttachment.getLocation(), context));
            }
        }


        return ParsedEmailTemplate
            .builder()
            .attachments(attachments)
            .images(extractSendTextImage(sendText))
            .text(sendText)
            .subject(render(subject, context))
            .sendTo(sendTo)
            .build();
    }


    private static Map<String, String> extractSendTextImage(String sendText) {
        Map<String, String> images = new HashMap<>();
        Document doc = Jsoup.parse(sendText);
        for (Element src : doc.getElementsByTag("img")) {
            String s = src.attr("src");
            if (s.startsWith("http")) {
                continue;
            }
            String tempKey = IDGenerator.MD5.generate();
            src.attr("src", "cid:".concat(tempKey));
            images.put(tempKey, s);
        }
        return images;
    }

    private static String render(String str, Map<String, Object> context) {
        return render(str, context, false);
    }

    private static String render(String str, Map<String, Object> context, boolean html) {
        if (StringUtils.isEmpty(str)) {
            return "";
        }
        if (html) {
            return TemplateParser.parse(str, expr ->
                ExpressionUtils.analytical("${" + Jsoup
                    .parse(expr)
                    .text() + "}", context, "spel"));
        }

        return ExpressionUtils.analytical(str, context, "spel");
    }

}
