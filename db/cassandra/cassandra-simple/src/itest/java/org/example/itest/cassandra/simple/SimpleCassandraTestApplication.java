package org.example.itest.cassandra.simple;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.CassandraContainer;
import org.testcontainers.utility.DockerImageName;

public class SimpleCassandraTestApplication {
    public static void main(String[] args) {
        SpringApplication.from(SimpleCassandraApp::main)
                .with(CassandraTestcontainersConfig.class)
                .run(args);
    }

    @TestConfiguration
    static class CassandraTestcontainersConfig {
        @SuppressWarnings("resource")
        @ServiceConnection
        @Bean
        CassandraContainer<?> cassandraContainer() {
            return new CassandraContainer<>(
                    DockerImageName.parse("cassandra:4")
            )
                    .withReuse(true);
        }
    }
}
