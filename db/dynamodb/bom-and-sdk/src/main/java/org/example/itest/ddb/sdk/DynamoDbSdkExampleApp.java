package org.example.itest.ddb.sdk;

import jakarta.annotation.PostConstruct;
import lombok.*;
import lombok.experimental.Accessors;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.model.ScanEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClientBuilder;
import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@SpringBootApplication
class DynamoDbSdkExampleApp {
    public static void main(String[] args) {
        SpringApplication.run(DynamoDbSdkExampleApp.class, args);
    }

    @Component
    @Data
    @Accessors(chain = true)
    @ConfigurationProperties(prefix = "ddb")
    static class DdbProps {
        String endpoint;
    }

    @Configuration
    @RequiredArgsConstructor
    static class DdbConfig {
        final DdbProps ddbProps;

        @Bean
        DynamoDbClient dynamoDbClient() {
            DynamoDbClientBuilder builder = DynamoDbClient.builder();

            if (StringUtils.hasText(ddbProps.getEndpoint()))
                builder.endpointOverride(URI.create(ddbProps.getEndpoint()))
                        .endpointDiscoveryEnabled(false);

            return builder.build();
        }

        @Bean
        DynamoDbEnhancedClient enhancedClient(DynamoDbClient dynamoDbClient) {
            return DynamoDbEnhancedClient.builder().dynamoDbClient(dynamoDbClient).build();
        }
    }

    @DynamoDbBean
    @Data
    @Accessors(chain = false) // must be false for aws sdk
    @Builder(toBuilder = true)
    @NoArgsConstructor
    @AllArgsConstructor
    public static class User {
        public static final String TABLE = "User";

        @Getter(onMethod_ = {@DynamoDbPartitionKey, @DynamoDbAttribute("Id")})
        UUID id;
        @Getter(onMethod_ = {@DynamoDbAttribute("Name")})
        String name;
        @Getter(onMethod_ = {@DynamoDbAttribute("Comment")})
        String comment;
    }

    @Component
    @RequiredArgsConstructor
    static class UserRepo {
        final DynamoDbEnhancedClient client;
        final TableSchema<User> userTableSchema = TableSchema.fromBean(User.class);
        private DynamoDbTable<User> table;

        @PostConstruct
        void init() {
            table = client.table(User.TABLE, userTableSchema);
            try {
                table.describeTable();
            } catch (ResourceNotFoundException ignored) {
                table.createTable();
            }
        }

        private DynamoDbTable<User> table() {
            return table;
        }

        List<User> findAll() {
            return table()
                    .scan(ScanEnhancedRequest.builder().limit(10).build())
                    .items().stream().toList();
        }

        User findById(UUID id) {
            return table().getItem(Key.builder().partitionValue(id.toString()).build());
        }

        User save(User user) {
            table().putItem(user);
            return user;
        }

        void delete(User user) {
            table().deleteItem(user);
        }
    }
}
