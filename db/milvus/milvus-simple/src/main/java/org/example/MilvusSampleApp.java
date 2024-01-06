package org.example;

import io.milvus.client.MilvusServiceClient;
import io.milvus.param.ConnectParam;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

@SpringBootApplication
class MilvusSampleApp {
    public static void main(String[] args) {
        SpringApplication.run(MilvusSampleApp.class, args);
    }

    @Component
    @ConfigurationProperties("milvus")
    @Data
    @Accessors(chain = true)
    static class MilvusProperties {
        String address;
    }

    @Configuration
    @RequiredArgsConstructor
    static class Config {
        final MilvusProperties milvusProperties;

        @Bean
        MilvusServiceClient milvusClient() {
            String host;
            int port;
            if (null != milvusProperties.getAddress()) {
                var parts = milvusProperties.getAddress().split(":");
                host = parts[0];
                port = Integer.parseInt(parts[1]);
            } else {
                host = "localhost";
                port = 19530;
            }

            return new MilvusServiceClient(
                    ConnectParam.newBuilder()
                            .withHost(host)
                            .withPort(port)
                            .build()
            );

        }
    }
}
