package org.example;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.MountableFile;

import java.util.Objects;

@SpringBootTest
class GrpcClientAppITest {
    static final String GRPC_APP_JAR = Objects.requireNonNull(System.getProperty("grpcAppJar"),
            "missing mandatory property 'grpcAppJar', needed for starting dependencies");

    @SuppressWarnings("resource")
    static final GenericContainer<?> loggedAppContainer =
            new GenericContainer<>("eclipse-temurin:21-jre-alpine")
                    .withExposedPorts(8080)
                    .withEnv("SERVER_PORT", "8080")
                    .withCopyFileToContainer(MountableFile.forHostPath(GRPC_APP_JAR), "/app.jar")
                    .withCommand("java", "-jar", "-Xmx512m", "-Xms128m", "/app.jar")
                    .waitingFor(Wait.forHttp("/").forPort(8080));

    @Test
    void test() {
    }
}
