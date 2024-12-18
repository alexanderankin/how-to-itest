package org.example;

import com.fasterxml.uuid.Generators;
import com.google.protobuf.StringValue;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.example.proto.example.ExampleProtos;
import org.example.proto.example.ExampleServiceGrpc.ExampleServiceBlockingStub;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ExampleServiceTest extends GrpcServiceWithStarterBaseTest {
    @Autowired
    ExampleServiceBlockingStub stub;

    @Test
    void test_createRead() {
        String prefix = "ExampleServiceTest.test_createRead";

        StringValue id = stub.createExample(ExampleProtos.Example.newBuilder().setName(prefix).build());
        ExampleProtos.Example example = stub.getExample(id);
        assertThat(example, is(notNullValue()));
        assertThat(example.getId(), is(notNullValue()));
    }

    @Test
    void test_notFound() {
        String prefix = "ExampleServiceTest.test_notFound";
        UUID id = Generators.nameBasedGenerator().generate(prefix);

        UUID finalId1 = id;
        var ex = assertThrows(StatusRuntimeException.class, () -> stub.getExample(StringValue.newBuilder().setValue(finalId1.toString()).build()));
        assertThat(ex.getStatus(), is(Status.NOT_FOUND));

        id = UUID.fromString(stub.createExample(ExampleProtos.Example.newBuilder()
                .setId(id.toString())
                .setName(prefix)
                .build()).getValue());

        UUID finalId2 = id;
        assertDoesNotThrow(() -> stub.getExample(StringValue.newBuilder().setValue(finalId2.toString()).build()));
    }
}
