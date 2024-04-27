package org.example.itest.mysql.customurl;

import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.testcontainers.containers.MySQLContainer;

import javax.sql.DataSource;

@Configuration
// @Testcontainers
public class DatabaseConfig {

    public static MySQLContainer<?> MY_SQL_CONTAINER;

    @Bean
    public MySQLContainer<?> mysqlSQLContainer() {
        MY_SQL_CONTAINER = new MySQLContainer<>("mysql:latest")
                .withDatabaseName("test").withUsername("test").withPassword("test")
                .withEnv("MYSQL_SSL_MODE", "DISABLED");
        MY_SQL_CONTAINER.start();
        return MY_SQL_CONTAINER;
    }

    @Bean
    public DataSource dataSource(MySQLContainer<?> mysqlSQLContainer) {
        String jdbcUrl = "jdbc:mysql://" + mysqlSQLContainer.getHost() + ":" +
                mysqlSQLContainer.getFirstMappedPort() + "/" + mysqlSQLContainer.getDatabaseName();

        return DataSourceBuilder.create()
                .url(jdbcUrl.concat(" ?createDatabaseIfNotExist=true&allowPublicKeyRetrieval=true&useSSL=false"))
                .username(mysqlSQLContainer.getUsername())
                .password(mysqlSQLContainer.getPassword())
                .build();
    }
}
