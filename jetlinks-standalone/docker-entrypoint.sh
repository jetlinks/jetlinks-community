#!/usr/bin/env bash
java $JAVA_OPTS -server \
--add-opens \
java.base/java.lang=ALL-UNNAMED \
--add-opens \
java.base/java.util=ALL-UNNAMED \
--add-opens \
java.base/java.util.concurrent=ALL-UNNAMED \
--add-opens \
java.base/java.io=ALL-UNNAMED \
--add-opens \
java.base/java.net=ALL-UNNAMED \
--add-opens \
java.base/java.text=ALL-UNNAMED \
--add-opens \
java.base/java.math=ALL-UNNAMED \
--add-opens \
java.scripting/javax.script=ALL-UNNAMED \
-Dreactor.schedulers.defaultBoundedElasticOnVirtualThreads=true \
-Djava.security.egd=file:/dev/./urandom \
org.springframework.boot.loader.launch.PropertiesLauncher