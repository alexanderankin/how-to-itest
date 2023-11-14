package org.example.itest.db.mdbk1;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.containers.wait.strategy.WaitStrategy;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;

// @Log4j2
@Slf4j
@ActiveProfiles("test")
// @TestPropertySource(locations = "classpath:application-test.properties")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT/* , classes = {Application.class} */)
// @AutoConfigureWireMock(port = 0)
public abstract class BaseITest {
    public static final Network integrationTestNetwork = Network.newNetwork();

    public static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.2.2.arm64"))
            .withEnv("KAFKA_AUTO_CREATE_TOPICS_ENABLE", "false")
            .withEmbeddedZookeeper()
            .withEnv("KAFKA_ADVERTISED_LISTENERS", "PLAINTEXT://kafka:9092")
            .withNetworkAliases("kafka")
            .withNetwork(integrationTestNetwork);

    public static final WaitStrategy WAIT = Wait.forHttp("/subjects").forStatusCode(200).withStartupTimeout(Duration.ofMinutes(2));

    /* public static SchemaRegistryContainer schemaRegistry = new SchemaRegistryContainer()
            .withEnv("SCHEMA_REGISTRY_HOST_NAME", "schema-registry")
            .withEnv("SCHEMA_REGISTRY_LISTENERS", "http://0.0.0.0:8081")
            .withEnv("SCHEMA_REGISTRY_KAFKASTORE_BOOTSTRAP_SERVERS", "PLAINTEXT://kafka:9092")
            .withNetworkAliases("schema-registry")
            .waitingFor(WAIT)
            .withNetwork(integrationTestNetwork)
            .withExposedPorts(8081)
            .dependsOn(kafka); */

    public static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo")
            .withExposedPorts(27017);

    static {
        kafka.start();
        // schemaRegistry.start();
        mongoDBContainer.start();

        /*
        System.setProperty("test-container.mongodb.port", mongoDBContainer.getMappedPort(27017).toString());
        System.setProperty("test-container.kafka.port", kafka.getMappedPort(9093).toString());
        System.setProperty("test-container.schema-registry.port", schemaRegistry.getMappedPort(8081).toString());
        */
    }

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        // System.setProperty("test-container.schema-registry.port", schemaRegistry.getMappedPort(8081).toString());
    }
}
