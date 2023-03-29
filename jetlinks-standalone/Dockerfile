FROM openjdk:8u272-jdk as builder
WORKDIR application
ARG JAR_FILE=target/jetlinks-standalone.jar
COPY ${JAR_FILE} application.jar
RUN java -Djarmode=layertools -jar application.jar extract
ENTRYPOINT ["/bin/sh","-c","java -Dfile.encoding=utf8 -Djava.security.egd=file:/qa/./urandom -jar application.jar "]