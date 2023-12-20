package org.example.itest.db.postgres.multidb;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.experimental.Accessors;
import org.postgresql.Driver;
import org.postgresql.PGProperty;
import org.postgresql.ds.PGSimpleDataSource;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

@SpringBootApplication
class MultiDbApp {
    public static void main(String[] args) {
        SpringApplication.run(MultiDbApp.class, args);
    }

    @Data
    @Accessors(chain = true)
    @Component
    @ConfigurationProperties("postgres-dbs")
    static class PostgresDbsProperties {
        List<String> dbs = List.of("db1", "db2", "db3", "db4");
    }

    @RequiredArgsConstructor
    @Component
    static class PostgresDbs {
        final PostgresDbsProperties props;
        final HikariDataSource dataSource;
        final Map<String, DataSource> dsCache = new ConcurrentHashMap<>();

        DataSource forDb(String name) {
            if (!props.getDbs().contains(name))
                // validation. smrt.
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "begone, hacker");

            return dsCache.computeIfAbsent(name, this::doForDb);
        }

        // https://stackoverflow.com/a/65628828
        @SneakyThrows
        private DataSource doForDb(String s) {
            String originalUrl = dataSource.getJdbcUrl();
            Properties props;
            {
                props = Driver.parseURL(originalUrl, null);
                Assert.notNull(props, "couldn't parse url while accessing db " + s);
                props.setProperty(PGProperty.PG_DBNAME.getName(), s);
            }

            String newUrl;
            {
                var b = new PGSimpleDataSource();
                for (var e : props.entrySet()) {
                    b.setProperty(e.getKey().toString(), e.getValue().toString());
                }
                newUrl = b.getUrl();
            }

            HikariConfig config = new HikariConfig();
            dataSource.copyStateTo(config);
            config.setJdbcUrl(newUrl);
            config.setPoolName("HikariPool-" + s);
            var newDs = new HikariDataSource(config);

            // won't work for prod where multiple instance of app running
            // use db migrator which can lock or something
            try (Connection connection = newDs.getConnection()) {
                connection.createStatement().execute("create table item(id serial not null primary key, item text)");
            }

            return newDs;
        }
    }

    @RequiredArgsConstructor
    @RestController
    @RequestMapping("/")
    static class Controller {
        private final PostgresDbs dbs;

        @GetMapping("/{db}/items")
        List<String> items(@PathVariable("db") String db) {
            return JdbcClient.create(dbs.forDb(db)).sql("select id, item from item").query(Item.class).stream().map(Item::item).toList();
        }

        @PostMapping("/{db}/items")
        @ResponseStatus(HttpStatus.CREATED)
        Item createItem(@PathVariable("db") String db, @RequestBody Item item) {
            var kh = new GeneratedKeyHolder();
            JdbcClient.create(dbs.forDb(db)).sql("insert into item(item) values (?)").param(item.item()).update(kh, "id");
            return new Item(kh.getKeyAs(Integer.class), item.item());
        }
    }

    record Item(Integer id, String item) {
    }
}
