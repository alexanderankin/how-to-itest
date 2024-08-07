package org.example;

import com.example.tutorial.protos.AddressBookProtos;
import com.example.tutorial.protos.AddressBookServiceGrpc;
import com.google.common.collect.Lists;
import com.google.protobuf.Empty;
import com.google.protobuf.Int32Value;
import io.grpc.Channel;
import io.grpc.ManagedChannelBuilder;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@SpringBootApplication
class GrpcClientApp {
    public static void main(String[] args) {
        SpringApplication.run(GrpcClientApp.class, args);
    }

    @Component
    @ConfigurationProperties(prefix = "grpc-server")
    @Data
    @Accessors(chain = true)
    @Validated
    static class GrpcServerProperties {
        @NotNull
        Integer port;
    }

    @AllArgsConstructor
    @Configuration
    static class GrpcClientConfig {
        GrpcServerProperties server;

        @Bean
        Channel channel() {
            return ManagedChannelBuilder.forAddress("localhost", server.getPort())
                    .usePlaintext()
                    .build();
        }

        @Bean
        AddressBookServiceGrpc.AddressBookServiceBlockingStub stub(Channel channel) {
            return AddressBookServiceGrpc.newBlockingStub(channel);
        }
    }

    @AllArgsConstructor
    @Service
    static class GrpcClient {
        AddressBookServiceGrpc.AddressBookServiceBlockingStub stub;

        int create(String name, String email) {
            Int32Value addressBook = stub.createAddressBook(AddressBookProtos.AddressBook.newBuilder()
                    .addPeople(AddressBookProtos.Person.newBuilder()
                            .setName(name)
                            .setEmail(email)
                            .build())
                    .build());
            return addressBook.getDefaultInstanceForType().getValue();
        }

        List<AddressBook> list() {
            Iterator<AddressBookProtos.AddressBook> addressBooksIt =
                    stub.getAddressBooks(Empty.getDefaultInstance());

            ArrayList<AddressBookProtos.AddressBook> addressBooks
                    = Lists.newArrayList(addressBooksIt);

            return addressBooks.stream().map(AddressBook::from).toList();
        }
    }

    @Data
    @Accessors(chain = true)
    static class AddressBook {
        Integer id;
        List<Person> people;

        static AddressBook from(AddressBookProtos.AddressBook proto) {
            return new AddressBook()
                    .setId(proto.getId())
                    .setPeople(proto.getPeopleList().stream()
                            .map(protoPerson -> new Person()
                                    .setId(protoPerson.getId())
                                    .setName(protoPerson.getName())
                                    .setEmail(protoPerson.getEmail())
                                    .setPhones(protoPerson.getPhonesList().stream()
                                            .map(ph -> new Person.PhoneNumber()
                                                    .setNumber(ph.getNumber())
                                                    .setType(Person.PhoneNumber.PhoneType.from(ph.getType())))
                                            .toList()))
                            .toList());
        }

        @Data
        @Accessors(chain = true)
        static class Person {
            String name;
            Integer id;
            String email;
            List<PhoneNumber> phones;

            @Data
            @Accessors(chain = true)
            static class PhoneNumber {
                String number;
                PhoneType type;

                enum PhoneType {
                    PHONE_TYPE_UNSPECIFIED,
                    PHONE_TYPE_MOBILE,
                    PHONE_TYPE_HOME,
                    PHONE_TYPE_WORK,
                    ;

                    static PhoneType from(AddressBookProtos.Person.PhoneType type) {
                        return switch (type) {
                            case PHONE_TYPE_UNSPECIFIED ->
                                    PhoneType.PHONE_TYPE_UNSPECIFIED;
                            case PHONE_TYPE_MOBILE -> PhoneType.PHONE_TYPE_MOBILE;
                            case PHONE_TYPE_HOME -> PhoneType.PHONE_TYPE_HOME;
                            case PHONE_TYPE_WORK -> PhoneType.PHONE_TYPE_WORK;
                        };
                    }
                }
            }
        }
    }
}
