package org.jetlinks.community.network.manager.test.utils;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

public class ContainerUtils {

    public static GenericContainer<?> newRedis() {
        return new GenericContainer<>(DockerImageName.parse("redis:5"))
            .withEnv("TZ", "Asia/Shanghai")
            .withExposedPorts(6379)
            .waitingFor(Wait.forListeningPort());
    }

//    public static GenericContainer<?> newMysql() {
//        return new GenericContainer<>(DockerImageName.parse("mysql:" + System.getProperty("test.mysql.version", "5.7")))
//            .withEnv("TZ", "Asia/Shanghai")
//            .withEnv("MYSQL_ROOT_PASSWORD", "password")
//            .withEnv("MYSQL_DATABASE", "jetlinks")
//            .withCommand("--character-set-server=utf8mb4")
//            .withExposedPorts(3306)
//            .waitingFor(Wait.forListeningPort());
//    }

    public static GenericContainer<?> newPostgresql() {
        return new GenericContainer<>(DockerImageName.parse("postgres:" + System.getProperty("test.postgres.version", "11")) + "-alpine")
            .withEnv("TZ", "Asia/Shanghai")
            .withEnv("POSTGRES_PASSWORD", "jetlinks")
            .withEnv("POSTGRES_DB", "jetlinks")
            .withCommand("postgres", "-c", "max_connections=500")
            .withExposedPorts(5432)
            .waitingFor(Wait.forListeningPort());
    }

    public static GenericContainer<?> newElasticSearch() {
        return newElasticSearch(System.getProperty("test.elasticsearch.version", "6.8.11"));
    }

    public static GenericContainer<?> newElasticSearch(String version) {
        return new GenericContainer<>(DockerImageName.parse("elasticsearch:" + version))
            .withEnv("TZ", "Asia/Shanghai")
            .withEnv("ES_JAVA_OPTS","-Djava.net.preferIPv4Stack=true -Xms1g -Xmx1g")
//            .withSharedMemorySize(DataSize.ofGigabytes(1).toBytes())
            .withEnv("transport.host", "0.0.0.0")
            .withEnv("discovery.type", "single-node")
//            .withEnv("bootstrap.memory_lock", "true")
            .withExposedPorts(9200)
            .waitingFor(Wait.forHttp("/").forStatusCode(200));
    }

}
