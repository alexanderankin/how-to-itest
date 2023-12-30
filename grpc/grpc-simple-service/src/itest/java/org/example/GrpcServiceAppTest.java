package org.example;

import com.example.tutorial.protos.AddressBookProtos;
import com.example.tutorial.protos.AddressBookServiceGrpc;
import com.google.protobuf.Int32Value;
import io.grpc.ManagedChannelBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestConfig.class)
class GrpcServiceAppTest {
    @Autowired
    AddressBookServiceGrpc.AddressBookServiceBlockingStub blockingStub;

    @Test
    void test() {
        Int32Value addressBook = blockingStub.createAddressBook(AddressBookProtos.AddressBook.newBuilder()
                .addPeople(AddressBookProtos.Person.newBuilder()
                        .setName("test.1")
                        .setEmail("test@localhost")
                        .build())
                .build());

        AddressBookProtos.AddressBook read = blockingStub.getAddressBook(addressBook);
        assertThat(read.getId(), is(0));
        assertThat(read.getPeopleCount(), is(1));
        assertThat(read.getPeopleList().get(0), is(not(nullValue())));
        assertThat(read.getPeopleList().get(0).getName(), is("test.1"));
        assertThat(read.getPeopleList().get(0).getEmail(), is("test@localhost"));
    }
}

@TestConfiguration
class TestConfig {
    @Bean
    AddressBookServiceGrpc.AddressBookServiceBlockingStub addressBookServiceStub() {
        return com.example.tutorial.protos.AddressBookServiceGrpc.newBlockingStub(
                ManagedChannelBuilder.forAddress("localhost", 8081)
                        .usePlaintext()
                        .build()
        );
    }
}
