#!/usr/bin/env bash
java $JAVA_OPTS -server \
-XX:+UnlockExperimentalVMOptions \
-XX:+UseCGroupMemoryLimitForHeap \
-XX:-OmitStackTraceInFastThrow \
-Djava.security.egd=file:/dev/./urandom \
org.springframework.boot.loader.JarLauncher