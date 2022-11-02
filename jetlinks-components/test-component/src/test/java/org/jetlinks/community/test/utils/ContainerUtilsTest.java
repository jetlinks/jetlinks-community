package org.jetlinks.community.test.utils;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
class ContainerUtilsTest {

    @Container
    GenericContainer<?> redis = ContainerUtils.newRedis();

    @Container
    GenericContainer<?> mysql = ContainerUtils.newMysql();

    @Container
    GenericContainer<?> postgresql = ContainerUtils.newPostgresql();

    @Container
    GenericContainer<?> elasticsearch = ContainerUtils.newElasticSearch();


    @Test
    void test(){

        assertTrue(redis.isRunning());
        assertTrue(mysql.isRunning());
        assertTrue(postgresql.isRunning());
        assertTrue(elasticsearch.isRunning());

    }

}