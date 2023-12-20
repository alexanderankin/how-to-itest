package org.example.itest.db.postgres.multidb;

import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.PostgreSQLContainer;

import java.sql.Connection;
import java.sql.Statement;

public class TestMultiDbApp {
    public static void main(String[] args) {
        SpringApplication.from(MultiDbApp::main)
                .with(TestConfig.class)
                .run(args);
    }

    @TestConfiguration
    static class TestConfig {
        @Autowired
        MultiDbApp.PostgresDbsProperties props;

        @SneakyThrows
        @ServiceConnection
        @Bean
        PostgreSQLContainer<?> psqlContainer() {
            PostgreSQLContainer<?> p = new PostgreSQLContainer<>("postgres:16-alpine");
            p.start();

            var ds = DataSourceBuilder.create().url(p.getJdbcUrl()).username(p.getUsername()).password(p.getPassword()).build();
            try (Connection connection = ds.getConnection()) {
                Statement statement = connection.createStatement();
                for (String db : props.getDbs()) {
                    statement.execute("create database " + db);
                    // probably incorrect
                    // https://serverfault.com/q/198002
                    statement.execute("GRANT ALL PRIVILEGES ON DATABASE " + db + " TO " + p.getUsername());
                }
            }
            if (ds instanceof AutoCloseable a) a.close();

            return p;
        }
    }
}
