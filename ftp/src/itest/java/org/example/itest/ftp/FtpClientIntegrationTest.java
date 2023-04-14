package org.example.itest.ftp;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;
import lombok.SneakyThrows;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.util.Assert;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.WaitStrategy;
import org.testcontainers.containers.wait.strategy.WaitStrategyTarget;
import org.testcontainers.utility.DockerImageName;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.stream.IntStream;

@Retention(RetentionPolicy.RUNTIME)
@ContextConfiguration(initializers = FtpClientIntegrationTest.FtpServerContextInitializer.class)
public @interface FtpClientIntegrationTest {

    class FtpServerContextInitializer
            implements ApplicationContextInitializer<ConfigurableApplicationContext> {

        @SuppressWarnings("resource")
        @SneakyThrows
        @Override
        public void initialize(@NonNull ConfigurableApplicationContext ctx) {
            String username = "test-user";
            String password = "test-user-" + Instant.now().toString();

            DockerImageName image = DockerImageName.parse("vsftpd");
            GenericContainer<?> container = new GenericContainer<>(image);

            container.setWaitStrategy(new FixedDurationWaitStrategy(Duration.ofSeconds(1)));

            // ftp ports - 21 plus range
            IntStream.Builder portBuilder = IntStream.builder().add(20).add(21);
            IntStream.range(21100, 21110 + 1).forEach(portBuilder::add);

            int[] ports = portBuilder.build().toArray();
            container.addExposedPorts(ports);

            container.start();


            var adduser = container.execInContainer("adduser",
                    "--disabled-password",
                    "--gecos",
                    "",
                    username);
            Assert.isTrue(adduser.getExitCode() == 0,
                    () -> "command adduser not successful: " + adduser);

            String changePasswordCmd = "chpasswd <<< \"%s:%s\"".formatted(username, password);
            var chPasswd = container.execInContainer("bash", "-c", changePasswordCmd);
            Assert.isTrue(chPasswd.getExitCode() == 0,
                    () -> "command chPasswd not successful: " + chPasswd);

            Map<String, String> values = Map.ofEntries(
                    Map.entry("example.ftp.host", container.getHost()),
                    Map.entry("example.ftp.port", String.valueOf(container.getMappedPort(21))),
                    Map.entry("example.ftp.username", username),
                    Map.entry("example.ftp.password", password),
                    Map.entry("example.ftp.folder", "/home/" + username)
            );

            TestPropertyValues.of(values).applyTo(ctx.getEnvironment());
        }

        @AllArgsConstructor
        @Data
        private static class FixedDurationWaitStrategy implements WaitStrategy {
            Duration duration;

            @SneakyThrows
            @Override
            public void waitUntilReady(WaitStrategyTarget waitStrategyTarget) {
                Thread.sleep(duration);
            }

            @Override
            public WaitStrategy withStartupTimeout(Duration startupTimeout) {
                duration = startupTimeout;
                return this;
            }
        }
    }

}
