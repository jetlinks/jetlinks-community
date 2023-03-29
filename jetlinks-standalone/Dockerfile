FROM openjdk:8u272-jdk as builder
WORKDIR application
ARG JAR_FILE=target/jetlinks-standalone.jar
COPY ${JAR_FILE} application.jar
RUN java -Djarmode=layertools -jar application.jar extract

FROM openjdk:8u272-jdk
WORKDIR application
COPY --from=builder application/dependencies/ ./
RUN true
COPY --from=builder application/snapshot-dependencies/ ./
RUN true
COPY --from=builder application/spring-boot-loader/ ./
RUN true
COPY --from=builder application/application/ ./
RUN true
COPY docker-entrypoint.sh ./
RUN true
RUN chmod +x docker-entrypoint.sh
RUN true
ENTRYPOINT ["./docker-entrypoint.sh"]