#!/usr/bin/env bash
java -jar $JAVA_OPTS -server \
-XX:+UnlockExperimentalVMOptions \
-XX:+UseCGroupMemoryLimitForHeap \
-XX:-OmitStackTraceInFastThrow \
-Djava.security.egd=file:/dev/./urandom \
/$APP_JAR