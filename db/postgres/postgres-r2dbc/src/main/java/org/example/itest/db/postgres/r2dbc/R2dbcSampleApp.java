package org.example.itest.db.postgres.r2dbc;

import io.r2dbc.spi.ConnectionFactoryOptions;
import lombok.Data;
import lombok.SneakyThrows;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.r2dbc.R2dbcProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.annotation.Id;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import static io.r2dbc.spi.ConnectionFactoryOptions.*;

@SpringBootApplication
public class R2dbcSampleApp {
    public static void main(String[] args) {
        SpringApplication.run(R2dbcSampleApp.class, args);
    }

    // use actual migration tool for prod usage
    @Configuration
    @Slf4j
    static class CreateTablesConfig {
        // todo use r2dbc
        // seems like a turing tarpit tbh
        @SneakyThrows
        @Autowired
        void initTables(R2dbcProperties r2dbcProperties) {
            String init = """
                    create table if not exists example
                    (
                        id          serial       not null primary key,
                        name        varchar(255) not null unique,
                        description text null
                    )
                    """;

            var options = ConnectionFactoryOptions.parse(r2dbcProperties.getUrl());
            var pb = new ProcessBuilder()
                    .command("psql",
                            "-h", options.getRequiredValue(HOST).toString(),
                            "-p", options.getRequiredValue(PORT).toString(),
                            "-d", options.getRequiredValue(DATABASE).toString(),
                            "-U", r2dbcProperties.getUsername(),
                            "-c", init)
                    .inheritIO();
            pb.environment().put("PGPASSWORD", r2dbcProperties.getPassword());
            pb.start().waitFor();
        }
    }

    @Configuration
    @EnableR2dbcRepositories(considerNestedRepositories = true)
    static class R2dbcConfig {
    }

    static class Examples {
        interface ExampleRepository extends ReactiveCrudRepository<Example, Long> {
        }

        @Data
        @Accessors(chain = true)
        static class Example {
            String name;
            String description;
            @Id
            private Long id;
        }
    }
}
