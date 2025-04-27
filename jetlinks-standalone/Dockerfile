FROM jetlinks/base-jdk-21:latest as builder
WORKDIR application
ARG JAR_FILE=target/application.jar
COPY ${JAR_FILE} app.jar
RUN java -Djarmode=tools -jar app.jar extract --layers --launcher
FROM jetlinks/base-jdk-21:latest

WORKDIR application
COPY --from=builder application/app/dependencies/ ./
COPY --from=builder application/app/snapshot-dependencies/ ./
COPY --from=builder application/app/spring-boot-loader/ ./
COPY --from=builder application/app/application/ ./
COPY docker-entrypoint.sh ./
RUN chmod +x docker-entrypoint.sh
ENTRYPOINT ["./docker-entrypoint.sh"]