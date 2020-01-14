package org.jetlinks.community.gateway;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.BiFunction;

@Getter
@Setter
@EqualsAndHashCode(of = "part")
public class TopicPart {

    private TopicPart parent;

    private String part;

    private volatile String topic;

    private int depth;

    private ConcurrentMap<String, TopicPart> child = new ConcurrentHashMap<>();

    private Set<String> sessionId = new CopyOnWriteArraySet<>();

    private static final AntPathMatcher matcher = new AntPathMatcher();

    public TopicPart(TopicPart parent, String part) {

        if (StringUtils.isEmpty(part) || part.equals("/")) {
            this.part = "";
        } else {
            if (part.contains("/")) {
                this.ofTopic(part);
            } else {
                this.part = part;
            }
        }
        this.parent = parent;
        if (null != parent) {
            this.depth = parent.depth + 1;
        }
    }

    public String getTopic() {
        if (topic == null) {
            TopicPart parent = getParent();
            StringBuilder builder = new StringBuilder();
            if (parent != null) {
                String parentTopic = parent.getTopic();
                builder.append(parentTopic).append(parentTopic.equals("/") ? "" : "/");
            } else {
                builder.append("/");
            }
            return topic = builder.append(part).toString();
        }
        return topic;
    }


    public TopicPart subscribe(String topic) {
        return getOrDefault(topic, TopicPart::new);
    }

    public void addSessionId(String... sessionId) {
        this.sessionId.addAll(Arrays.asList(sessionId));
    }

    public void removeSession(String... sessionId) {
        this.sessionId.removeAll(Arrays.asList(sessionId));
    }

    private void ofTopic(String topic) {
        String[] parts = topic.split("[/]", 2);
        this.part = parts[0];
        if (parts.length > 1) {
            TopicPart part = new TopicPart(this, parts[1]);
            this.child.put(part.part, part);
        }
    }

    private TopicPart getOrDefault(String topic, BiFunction<TopicPart, String, TopicPart> mapping) {
        if (topic.startsWith("/")) {
            topic = topic.substring(1);
        }
        String[] parts = topic.split("[/]");
        TopicPart part = child.computeIfAbsent(parts[0], _topic -> mapping.apply(this, _topic));
        for (int i = 1; i < parts.length && part != null; i++) {
            TopicPart parent = part;
            part = part.child.computeIfAbsent(parts[i], _topic -> mapping.apply(parent, _topic));
        }
        return part;
    }

    public Mono<TopicPart> get(String topic) {
        return Mono.justOrEmpty(getOrDefault(topic, ((topicPart, s) -> null)));
    }

    public Flux<TopicPart> find(String topic) {
        return find(topic, this);
    }

    @Override
    public String toString() {
        return "topic: " + getTopic() + ", sessions: " + sessionId.size();
    }


    public static Flux<TopicPart> find(String topic,
                                       TopicPart topicPart) {
        return Flux.create(sink -> {
            ArrayDeque<TopicPart> cache = new ArrayDeque<>();
            cache.add(topicPart);
            String[] topicParts = topic.split("[/]");
            String nextPart = null;
            while (!cache.isEmpty() && !sink.isCancelled()) {
                TopicPart part = cache.poll();
                if (part == null) {
                    break;
                }
                if (part.part.equals("**")
//                    || part.part.equals("*")
                    || matcher.match(part.getTopic(), topic)
                    || (matcher.match(topic, part.getTopic()))) {
                    sink.next(part);
                }

                //订阅了如 /device/**/event/*
                if (part.part.equals("**")) {
                    TopicPart tmp = null;
                    for (int i = part.depth; i < topicParts.length; i++) {
                        tmp = part.child.get(topicParts[i]);
                        if (tmp != null) {
                            cache.add(tmp);
                        }
                    }
                    if (null != tmp) {
                        continue;
                    }
                }
                if ("**".equals(nextPart) || "*".equals(nextPart)) {
                    cache.addAll(part.child.values());
                    continue;
                }
                TopicPart next = part.child.get("**");
                if (next != null) {
                    cache.add(next);
                }
                next = part.child.get("*");
                if (next != null) {
                    cache.add(next);
                }

                if (part.depth + 1 >= topicParts.length) {
                    continue;
                }
                nextPart = topicParts[part.depth + 1];
                if (nextPart.equals("*") || nextPart.equals("**")) {
                    cache.addAll(part.child.values());
                    continue;
                }
                next = part.child.get(nextPart);
                if (next != null) {
                    cache.add(next);
                }

            }
            sink.complete();
        });

    }


}
