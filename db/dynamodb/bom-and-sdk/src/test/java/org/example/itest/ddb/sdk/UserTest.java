package org.example.itest.ddb.sdk;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.mapper.BeanTableSchema;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

class UserTest {
    @Test
    void test_hasPartitionKey() {
        BeanTableSchema<DynamoDbSdkExampleApp.User> userBeanTableSchema = TableSchema.fromBean(DynamoDbSdkExampleApp.User.class);
        System.out.println(userBeanTableSchema.tableMetadata().primaryPartitionKey() != null);
        // when you use fluent setters, this number is 0 :(
        // this test ensures we are not using fluent setters
        assertThat(userBeanTableSchema.attributeNames().size(), is(3));
    }
}
