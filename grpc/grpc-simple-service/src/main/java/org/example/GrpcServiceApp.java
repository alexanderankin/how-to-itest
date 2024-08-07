package org.example;

import com.example.tutorial.protos.AddressBookProtos;
import com.example.tutorial.protos.AddressBookProtos.AddressBook;
import com.example.tutorial.protos.AddressBookProtos.Person.PhoneType;
import com.example.tutorial.protos.AddressBookServiceGrpc;
import com.google.protobuf.Empty;
import com.google.protobuf.Int32Value;
import io.grpc.*;
import io.grpc.stub.StreamObserver;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.mapstruct.CollectionMappingStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.NullValueCheckStrategy;
import org.mapstruct.ValueMapping;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

@SpringBootApplication
class GrpcServiceApp {
    @SneakyThrows
    public static void main(String[] args) {
        new SpringApplicationBuilder(GrpcServiceApp.class)
                .web(WebApplicationType.REACTIVE)
                // .web(WebApplicationType.NONE)
                .run(args);

    }

    @Mapper(componentModel = "spring",
            collectionMappingStrategy = CollectionMappingStrategy.ADDER_PREFERRED,
            nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
    interface ModelMapper {
        Models.AddressBook toModel(AddressBook addressBook);

        AddressBook toProto(Models.AddressBook addressBook);

        @ValueMapping(source = "PHONE_TYPE_UNSPECIFIED", target = "PHONE_TYPE_UNSPECIFIED")
        @ValueMapping(source = "PHONE_TYPE_MOBILE", target = "PHONE_TYPE_MOBILE")
        @ValueMapping(source = "PHONE_TYPE_HOME", target = "PHONE_TYPE_HOME")
        @ValueMapping(source = "PHONE_TYPE_WORK", target = "PHONE_TYPE_WORK")
        Models.Person.PhoneType toModel(PhoneType phoneType);

        @ValueMapping(source = "PHONE_TYPE_UNSPECIFIED", target = "PHONE_TYPE_UNSPECIFIED")
        @ValueMapping(source = "PHONE_TYPE_MOBILE", target = "PHONE_TYPE_MOBILE")
        @ValueMapping(source = "PHONE_TYPE_HOME", target = "PHONE_TYPE_HOME")
        @ValueMapping(source = "PHONE_TYPE_WORK", target = "PHONE_TYPE_WORK")
        PhoneType toProto(Models.Person.PhoneType phoneType);
    }

    @Slf4j
    @RequiredArgsConstructor
    @Configuration
    static class GrpcConfig {
        final ServerProperties serverProperties;

        @SneakyThrows
        @Bean
        Server grpcServer(List<BindableService> bindableServices) {
            ServerCredentials creds = InsecureServerCredentials.create();

            var spp = serverProperties.getPort();
            int port = (spp != null && spp != 0) ? spp + 1 : 0;
            log.info("Netty (for gRPC) starting on port {}", port);

            ServerBuilder<?> serverBuilder = Grpc.newServerBuilderForPort(port, creds);
            bindableServices.forEach(serverBuilder::addService);
            Server server = serverBuilder.build();
            server.start();
            log.info("Netty (for gRPC) started on port {}", server.getPort());
            return server;
        }
    }

    static class Models {
        @Data
        @Accessors(chain = true)
        static class Person {
            String name;
            Integer id;
            String email;
            List<PhoneNumber> phones;

            enum PhoneType {
                PHONE_TYPE_UNSPECIFIED,
                PHONE_TYPE_MOBILE,
                PHONE_TYPE_HOME,
                PHONE_TYPE_WORK,
            }

            @Data
            @Accessors(chain = true)
            static class PhoneNumber {
                String number;
                PhoneType type;
            }
        }

        @Data
        @Accessors(chain = true)
        static class AddressBook {
            Integer id;
            List<Person> people;
        }
    }

    @RequiredArgsConstructor
    @Service
    static class AddressBookService {
        final Map<Integer, Models.AddressBook> addressBooks = new HashMap<>();
        final AtomicInteger addressBooksSequence = new AtomicInteger();
        final Map<Integer, Models.Person> people = new HashMap<>();
        final AtomicInteger peopleSequence = new AtomicInteger();
        final Map<String, Models.Person> peopleByName = new HashMap<>();
        final ModelMapper modelMapper;

        List<AddressBookProtos.AddressBook> addressBooks() {
            return addressBooks.values().stream().map(modelMapper::toProto).toList();
        }

        public AddressBook addressBook(int value) {
            return modelMapper.toProto(addressBooks.get(value));
        }

        public synchronized int createAddressBook(AddressBook request) {
            Models.AddressBook model = modelMapper.toModel(request);
            if (model.getPeople().stream().map(Models.Person::getName).anyMatch(peopleByName::containsKey))
                return -1;
            int key = addressBooksSequence.incrementAndGet();
            addressBooks.put(key, model);
            model.getPeople().stream()
                    .map(p -> p.setId(peopleSequence.incrementAndGet()))
                    .forEach(p -> people.put(p.getId(), p));
            model.getPeople().stream()
                    .filter(p -> Objects.nonNull(p.getName()))
                    .forEach(p -> peopleByName.put(p.getName(), p));
            return key;
        }

        public synchronized void deleteAddressBook(int value) {
            Models.AddressBook removed = addressBooks.remove(value);
            if (removed != null) {
                if (removed.getPeople() != null) {
                    removed.getPeople().stream().map(Models.Person::getId).filter(Objects::nonNull).forEach(people::remove);
                    removed.getPeople().stream().map(Models.Person::getName).filter(Objects::nonNull).forEach(peopleByName::remove);
                }
            }
        }
    }

    @RequiredArgsConstructor
    @Service
    static class AddressBookProtoService extends AddressBookServiceGrpc.AddressBookServiceImplBase {
        private final AddressBookService addressBookService;

        @Override
        public void getAddressBook(Int32Value request, StreamObserver<AddressBookProtos.AddressBook> responseObserver) {
            responseObserver.onNext(addressBookService.addressBook(request.getValue()));
            responseObserver.onCompleted();
        }

        @Override
        public void getAddressBooks(Empty request, StreamObserver<AddressBookProtos.AddressBook> responseObserver) {
            addressBookService.addressBooks().forEach(responseObserver::onNext);
            responseObserver.onCompleted();
        }

        @Override
        public void createAddressBook(AddressBook request, StreamObserver<Int32Value> responseObserver) {
            responseObserver.onNext(Int32Value.of(addressBookService.createAddressBook(request)));
            responseObserver.onCompleted();
        }

        @Override
        public void deleteAddressBook(Int32Value request, StreamObserver<Empty> responseObserver) {
            addressBookService.deleteAddressBook(request.getValue());
            responseObserver.onCompleted();
        }
    }
}



