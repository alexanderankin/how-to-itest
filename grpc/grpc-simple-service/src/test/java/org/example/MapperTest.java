package org.example;

import com.example.tutorial.protos.AddressBookProtos;
import org.example.GrpcServiceApp.ModelMapper;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mapstruct.factory.Mappers;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;

public class MapperTest {
    @Nested
    class AddressBookRtTest {
        static ModelMapper mapper = Mappers.getMapper(ModelMapper.class);

        static List<AddressBookProtos.AddressBook> testCases() {
            return List.of(
                    AddressBookProtos.AddressBook.getDefaultInstance(),
                    AddressBookProtos.AddressBook.newBuilder().setId(1).build(),
                    AddressBookProtos.AddressBook.newBuilder().addAllPeople(List.of(
                            AddressBookProtos.Person.getDefaultInstance(),
                            AddressBookProtos.Person.newBuilder().setName("person").build()
                    )).build()
            );
        }

        @ParameterizedTest
        @MethodSource("testCases")
        void test_roundTrip(AddressBookProtos.AddressBook input) {
            assertThat(mapper.toProto(mapper.toModel(input)), Matchers.is(input));
        }
    }
}
