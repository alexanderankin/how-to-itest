package org.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

class GrpcServiceWithStarterTestApp {
    public static void main(String[] args) {
        SpringApplication.from(GrpcServiceWithStarter::main)
                .with(TestcontainersConfig.class)
                .run(args);
    }

    @TestConfiguration
    static class TestcontainersConfig {
        @Bean
        @ServiceConnection
        PostgreSQLContainer<?> postgreSQLContainer() {
            return new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"));
        }
    }
}
