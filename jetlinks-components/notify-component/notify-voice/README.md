# 语音通知

用于发送电话语音通知.



## API

1. org.jetlinks.community.notify.voice.VoiceNotifierManager

例子:
```java

motifierManager.getNotifier(notifierId) //获取通知器
            .flatMap(notifier->notifier.send(templateId,contextMap))//发送
            .doOnError(err->log.error("发送失败",err)) //失败
            .subscribe(ignore->log.info("发送成功")); //成功

```
 
## 拓展自定义服务商

实现接口: `org.jetlinks.community.notify.voice.provider.VoiceNotifierProvider`


## 阿里云(`aliyun`)

系统已实现阿里云语音通知`org.jetlinks.community.notify.voice.supports.aliyun.AliyunVoiceNotifierProvider`

配置(`NotifierProperties.configuration`):

|  Key   |  示例 |
|  ----  | ----  |
| regionId  |  cn-hangzhou |
| accessKeyId | LTAI4Dj2oThQnZYMAwTSj1F8 |
| secret  |  FeZ2nQZKM635IsPDudZOaQ5aDHMFeT |

模版配置(`TemplateProperties.template`)

|  Key   |  示例 |
|  ----  | ----  |
| ttsCode  |  TTS_123456 |
| calledShowNumbers | 02387673801 |
| calledNumber  |  18502114022 |
| playTimes  |  1 |

