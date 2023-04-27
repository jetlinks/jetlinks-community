package org.jetlinks.community.notify.manager.entity;

import lombok.Getter;
import lombok.Setter;
import org.hswebframework.web.bean.FastBeanCopier;
import org.hswebframework.web.id.IDGenerator;
import org.jetlinks.community.notify.manager.subscriber.Notify;

import java.io.Serializable;
import java.util.List;

@Getter
@Setter
public class Notification implements Serializable {
    private static final long serialVersionUID = -1L;

    private String id;

    private String subscribeId;

    private String subscriberType;

    private String subscriber;

    private String topicProvider;

    private String topicName;

    private String message;


    private Object detail;

    private String code;

    private String dataId;

    private long notifyTime;

    private List<String> notifyChannels;

    public static Notification from(NotifySubscriberEntity entity) {
        Notification notification = new Notification();

        notification.subscribeId = entity.getId();
        notification.subscriberType = entity.getSubscriberType();
        notification.subscriber = entity.getSubscriber();
        notification.topicName = entity.getTopicName();
        notification.setTopicProvider(entity.getTopicProvider());
        notification.setNotifyChannels(entity.getNotifyChannels());

        return notification;
    }

    public Notification copyWithMessage(Notify message) {
        Notification target = FastBeanCopier.copy(this, new Notification());
        target.setId(IDGenerator.SNOW_FLAKE_STRING.generate());
        target.setMessage(message.getMessage());
        target.setDataId(message.getDataId());
        target.setNotifyTime(message.getNotifyTime());
        target.setDetail(message.getDetail());
        target.setCode(message.getCode());

        return target;
    }

    public String createTopic() {
        //      /notifications/{订阅者类型:user}/{订阅者ID:userId}/{主题类型}/{订阅ID}
        return "/notifications/" + getSubscriberType() + "/" + getSubscriber() + "/" + getTopicProvider() + "/" + getSubscribeId();
    }
}
