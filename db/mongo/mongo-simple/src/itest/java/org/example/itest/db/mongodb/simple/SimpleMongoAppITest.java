package org.example.itest.db.mongodb.simple;

import org.bson.Document;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;

class SimpleMongoAppITest extends BaseSimpleMongoITest {
    @Test
    void test() {
        mongoTemplate.insert(new Document("name", "test"), "test");
        List<Document> list = mongoTemplate.findAll(Document.class, "test");
        assertThat(list, hasSize(1));
    }
}
