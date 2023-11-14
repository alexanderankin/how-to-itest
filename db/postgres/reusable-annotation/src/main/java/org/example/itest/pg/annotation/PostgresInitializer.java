package org.example.itest.pg.annotation;

import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.lang.NonNull;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.Map;

public class PostgresInitializer
        implements ApplicationContextInitializer<ConfigurableApplicationContext> {
    @Override
    public void initialize(@NonNull ConfigurableApplicationContext applicationContext) {
        @SuppressWarnings("resource")
        PostgreSQLContainer<?> postgreSQLContainer = new PostgreSQLContainer<>(
                DockerImageName.parse("postgres:16-alpine")
        );
        postgreSQLContainer.start();

        TestPropertyValues.of(Map.ofEntries(
                Map.entry("spring.datasource.url", postgreSQLContainer.getJdbcUrl()),
                Map.entry("spring.datasource.username", postgreSQLContainer.getUsername()),
                Map.entry("spring.datasource.password", postgreSQLContainer.getPassword())
        )).applyTo(applicationContext.getEnvironment());
    }
}
