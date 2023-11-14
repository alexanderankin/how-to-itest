package org.example.itest.db.mdbk1;

import lombok.extern.slf4j.Slf4j;
import org.example.itest.db.mdbk1.MongoDbKafkaDemoApp.Example;
import org.junit.jupiter.api.Test;
import org.rnorth.ducttape.unreliables.Unreliables;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import java.util.concurrent.TimeUnit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

@Slf4j
public class ExampleITest extends BaseITest {
    @Autowired
    MongoTemplate mongoTemplate;

    @Autowired
    KafkaTemplate<String, Example> kafkaTemplate;

    @Test
    void test() {
        String name = "ExampleITest_name";
        Message<Example> message = MessageBuilder.withPayload(new Example().setName(name))
                .setHeader(KafkaHeaders.TOPIC, MongoDbKafkaDemoApp.Listeners.EXAMPLES_TOPIC)
                .build();
        var send = kafkaTemplate.send(message);
        var result = send.join();
        log.info("sent: {}", result);
        Unreliables.retryUntilTrue(10,
                TimeUnit.SECONDS,
                () -> null != mongoTemplate.findOne(Query.query(Criteria.where("name").is(name)), Example.class));

        Example example = mongoTemplate.findOne(Query.query(Criteria.where("name").is(name)), Example.class);
        assertThat(example, is(notNullValue()));
        assertThat(example.getName(), is(name));
        assertThat(example.getId(), is(notNullValue()));
        log.info("found: {}", example);
    }
}
