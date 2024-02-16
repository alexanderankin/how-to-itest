package org.example.itest.cassandra.simple;

import com.datastax.oss.driver.api.core.CqlIdentifier;
import com.datastax.oss.driver.api.core.cql.SimpleStatement;
import com.datastax.oss.driver.internal.core.session.DefaultSession;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.cassandra.SessionFactory;
import org.springframework.data.cassandra.config.CqlSessionFactoryBean;
import org.springframework.data.cassandra.core.CassandraTemplate;
import org.springframework.data.cassandra.core.InsertOptions;
import org.springframework.data.cassandra.core.cql.CqlTemplate;
import org.springframework.data.cassandra.core.cql.PrimaryKeyType;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyColumn;
import org.springframework.data.cassandra.core.mapping.Table;
import org.springframework.data.cassandra.core.query.CassandraPageRequest;
import org.springframework.data.cassandra.core.query.Query;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;

import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@SpringBootApplication
class SimpleCassandraApp {
    public static void main(String[] args) {
        SpringApplication.run(SimpleCassandraApp.class, args);
    }

    @Component
    static class SimpleMigrator {
        @Autowired
        void migrate(CassandraTemplate cassandraTemplate) {
            // language=cql
            cassandraTemplate.execute(SimpleStatement.builder("""
                    CREATE KEYSPACE if not exists example
                        WITH REPLICATION = {
                            'class' : 'SimpleStrategy',
                            'replication_factor' : 1
                        };
                    """).build());

            // language=cql
            cassandraTemplate.execute(SimpleStatement.builder("""
                    CREATE TABLE if not exists example.example
                    (
                        source text,
                        id uuid,
                        created_at timestamp,
                        name text,
                        primary key ((source, id), created_at)
                    );
                    """).build());

            SessionFactory sessionFactory = ((CqlTemplate) cassandraTemplate.getCqlOperations()).getSessionFactory();
            Assert.notNull(sessionFactory, "sessionFactory");
            ((DefaultSession) sessionFactory.getSession()).setKeyspace(CqlIdentifier.fromCql("example"));
        }
    }

    @RequiredArgsConstructor
    @RestController
    @RequestMapping("/api/example")
    static class ExampleController {
        final CassandraTemplate cassandraTemplate;

        @GetMapping
        CassandraPage<Example> getExamples(
                Pageable pageable,
                @RequestParam(name = "pagingState") Optional<String> pagingState
        ) {
            Slice<Example> slice = cassandraTemplate.slice(
                    SimpleStatement.builder("select * from example.example")
                            .setPageSize(pageable.getPageSize())
                            .setPagingState(pagingState
                                    .map(p -> ByteBuffer.wrap(HexFormat.of().parseHex(p)))
                                    .orElse(null))
                            .build(),
                    Example.class
            );
            Optional<String> s = Optional.ofNullable(slice.hasNext() ? slice.nextPageable() : null)
                    .map(CassandraPageRequest.class::cast)
                    .map(CassandraPageRequest::getPagingState)
                    .map(b -> {
                        var bb = new byte[b.remaining()];
                        b.get(bb);
                        return HexFormat.of().formatHex(bb);
                    });
            return new CassandraPage<>(slice.getContent(), s.orElse(null));
        }

        @ResponseStatus(HttpStatus.CREATED)
        @PostMapping
        Example createExample(@RequestBody Example example) {
            example.setSource("rest");
            example.setId(UUID.randomUUID());
            example.setCreatedAt(Instant.now());

            return cassandraTemplate.insert(example);
        }
    }

    @Data
    @Accessors(chain = true)
    @Table("example")
    static class Example {
        @PrimaryKeyColumn(ordinal = 1, type = PrimaryKeyType.PARTITIONED)
        String source;
        @PrimaryKeyColumn(ordinal = 2, type = PrimaryKeyType.PARTITIONED)
        UUID id;
        @PrimaryKeyColumn(name = "created_at", ordinal = 3, type = PrimaryKeyType.CLUSTERED)
        Instant createdAt;
        String name;
    }

    record CassandraPage<T>(List<T> elements, String pagingState) {
    }
}
