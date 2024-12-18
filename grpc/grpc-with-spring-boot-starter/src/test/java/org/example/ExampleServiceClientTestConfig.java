package org.example;

import org.example.proto.example.ExampleServiceGrpc;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.grpc.client.GrpcChannelFactory;

@TestConfiguration
public class ExampleServiceClientTestConfig {
    @Bean
    ExampleServiceGrpc.ExampleServiceBlockingStub blockingStub(GrpcChannelFactory factory) {
        return ExampleServiceGrpc.newBlockingStub(factory.createChannel("localhost:9090").build());
    }
}
