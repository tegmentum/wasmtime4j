/*
 * Copyright 2025 Tegmentum AI
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ai.tegmentum.wasmtime4j;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.component.ComponentType;
import ai.tegmentum.wasmtime4j.component.ComponentVal;
import ai.tegmentum.wasmtime4j.component.ComponentValFactory;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Tests for {@link ComponentValFactory} abstract factory and default implementation. */
@DisplayName("ComponentValFactory")
final class ComponentValFactoryTest {

  private final ComponentValFactory factory = ComponentValFactory.INSTANCE;

  @Nested
  @DisplayName("singleton instance")
  final class SingletonTests {

    @Test
    @DisplayName("should have non-null singleton INSTANCE")
    void shouldHaveNonNullInstance() {
      assertNotNull(ComponentValFactory.INSTANCE, "INSTANCE should not be null");
    }
  }

  @Nested
  @DisplayName("primitive type creation")
  final class PrimitiveTypeCreationTests {

    @Test
    @DisplayName("should create bool value true")
    void shouldCreateBoolTrue() {
      final ComponentVal val = factory.createBool(true);
      assertTrue(val.isBool(), "Value should be bool type");
      assertTrue(val.asBool(), "Value should be true");
    }

    @Test
    @DisplayName("should create bool value false")
    void shouldCreateBoolFalse() {
      final ComponentVal val = factory.createBool(false);
      assertTrue(val.isBool(), "Value should be bool type");
      assertFalse(val.asBool(), "Value should be false");
    }

    @Test
    @DisplayName("should create s8 value")
    void shouldCreateS8() {
      final ComponentVal val = factory.createS8((byte) -42);
      assertTrue(val.isS8(), "Value should be s8 type");
      assertEquals((byte) -42, val.asS8(), "Value should be -42");
    }

    @Test
    @DisplayName("should create s16 value")
    void shouldCreateS16() {
      final ComponentVal val = factory.createS16((short) 1000);
      assertTrue(val.isS16(), "Value should be s16 type");
      assertEquals((short) 1000, val.asS16(), "Value should be 1000");
    }

    @Test
    @DisplayName("should create s32 value")
    void shouldCreateS32() {
      final ComponentVal val = factory.createS32(100000);
      assertTrue(val.isS32(), "Value should be s32 type");
      assertEquals(100000, val.asS32(), "Value should be 100000");
    }

    @Test
    @DisplayName("should create s64 value")
    void shouldCreateS64() {
      final ComponentVal val = factory.createS64(9999999999L);
      assertTrue(val.isS64(), "Value should be s64 type");
      assertEquals(9999999999L, val.asS64(), "Value should be 9999999999");
    }

    @Test
    @DisplayName("should create f32 value")
    void shouldCreateF32() {
      final ComponentVal val = factory.createF32(3.14f);
      assertTrue(val.isF32(), "Value should be f32 type");
      assertEquals(3.14f, val.asF32(), 0.001f, "Value should be 3.14f");
    }

    @Test
    @DisplayName("should create f64 value")
    void shouldCreateF64() {
      final ComponentVal val = factory.createF64(2.71828);
      assertTrue(val.isF64(), "Value should be f64 type");
      assertEquals(2.71828, val.asF64(), 0.00001, "Value should be 2.71828");
    }

    @Test
    @DisplayName("should create char value")
    void shouldCreateChar() {
      final ComponentVal val = factory.createChar('A');
      assertTrue(val.isChar(), "Value should be char type");
      assertEquals('A', val.asChar(), "Value should be 'A'");
    }
  }

  @Nested
  @DisplayName("unsigned integer creation")
  final class UnsignedIntegerCreationTests {

    @Test
    @DisplayName("should create u8 value")
    void shouldCreateU8() {
      final ComponentVal val = factory.createU8((short) 200);
      assertTrue(val.isU8(), "Value should be u8 type");
      assertEquals((short) 200, val.asU8(), "Value should be 200");
    }

    @Test
    @DisplayName("should reject u8 out of range")
    void shouldRejectU8OutOfRange() {
      assertThrows(
          IllegalArgumentException.class,
          () -> factory.createU8((short) 256),
          "Expected IllegalArgumentException for u8 value 256");
      assertThrows(
          IllegalArgumentException.class,
          () -> factory.createU8((short) -1),
          "Expected IllegalArgumentException for u8 value -1");
    }

    @Test
    @DisplayName("should create u16 value")
    void shouldCreateU16() {
      final ComponentVal val = factory.createU16(60000);
      assertTrue(val.isU16(), "Value should be u16 type");
      assertEquals(60000, val.asU16(), "Value should be 60000");
    }

    @Test
    @DisplayName("should reject u16 out of range")
    void shouldRejectU16OutOfRange() {
      assertThrows(
          IllegalArgumentException.class,
          () -> factory.createU16(65536),
          "Expected IllegalArgumentException for u16 value 65536");
    }

    @Test
    @DisplayName("should create u32 value")
    void shouldCreateU32() {
      final ComponentVal val = factory.createU32(4000000000L);
      assertTrue(val.isU32(), "Value should be u32 type");
      assertEquals(4000000000L, val.asU32(), "Value should be 4000000000");
    }

    @Test
    @DisplayName("should reject u32 out of range")
    void shouldRejectU32OutOfRange() {
      assertThrows(
          IllegalArgumentException.class,
          () -> factory.createU32(4294967296L),
          "Expected IllegalArgumentException for u32 value exceeding max");
    }

    @Test
    @DisplayName("should create u64 value")
    void shouldCreateU64() {
      final ComponentVal val = factory.createU64(Long.MAX_VALUE);
      assertTrue(val.isU64(), "Value should be u64 type");
      assertEquals(Long.MAX_VALUE, val.asU64(), "Value should be Long.MAX_VALUE");
    }
  }

  @Nested
  @DisplayName("string creation")
  final class StringCreationTests {

    @Test
    @DisplayName("should create string value")
    void shouldCreateString() {
      final ComponentVal val = factory.createString("hello");
      assertTrue(val.isString(), "Value should be string type");
      assertEquals("hello", val.asString(), "Value should be 'hello'");
    }

    @Test
    @DisplayName("should reject null string")
    void shouldRejectNullString() {
      assertThrows(
          IllegalArgumentException.class,
          () -> factory.createString(null),
          "Expected IllegalArgumentException for null string");
    }
  }

  @Nested
  @DisplayName("compound type creation")
  final class CompoundTypeCreationTests {

    @Test
    @DisplayName("should create list value")
    void shouldCreateList() {
      final List<ComponentVal> elements = List.of(factory.createS32(1), factory.createS32(2));
      final ComponentVal val = factory.createList(elements);
      assertTrue(val.isList(), "Value should be list type");
      assertEquals(2, val.asList().size(), "List should have 2 elements");
    }

    @Test
    @DisplayName("should reject null list")
    void shouldRejectNullList() {
      assertThrows(
          IllegalArgumentException.class,
          () -> factory.createList(null),
          "Expected IllegalArgumentException for null list");
    }

    @Test
    @DisplayName("should create record value")
    void shouldCreateRecord() {
      final Map<String, ComponentVal> fields =
          Map.of("name", factory.createString("test"), "age", factory.createS32(25));
      final ComponentVal val = factory.createRecord(fields);
      assertTrue(val.isRecord(), "Value should be record type");
      assertEquals(2, val.asRecord().size(), "Record should have 2 fields");
    }

    @Test
    @DisplayName("should create tuple value")
    void shouldCreateTuple() {
      final List<ComponentVal> elements =
          List.of(factory.createString("hi"), factory.createS32(42));
      final ComponentVal val = factory.createTuple(elements);
      assertTrue(val.isTuple(), "Value should be tuple type");
      assertEquals(2, val.asTuple().size(), "Tuple should have 2 elements");
    }

    @Test
    @DisplayName("should create enum value")
    void shouldCreateEnum() {
      final ComponentVal val = factory.createEnum("RED");
      assertTrue(val.isEnum(), "Value should be enum type");
      assertEquals("RED", val.asEnum(), "Enum case should be RED");
    }

    @Test
    @DisplayName("should reject null enum case name")
    void shouldRejectNullEnumCaseName() {
      assertThrows(
          IllegalArgumentException.class,
          () -> factory.createEnum(null),
          "Expected IllegalArgumentException for null enum case name");
    }

    @Test
    @DisplayName("should create flags value")
    void shouldCreateFlags() {
      final Set<String> flags = Set.of("read", "write");
      final ComponentVal val = factory.createFlags(flags);
      assertTrue(val.isFlags(), "Value should be flags type");
      assertEquals(2, val.asFlags().size(), "Flags should have 2 entries");
    }
  }

  @Nested
  @DisplayName("option type creation")
  final class OptionTypeCreationTests {

    @Test
    @DisplayName("should create some value")
    void shouldCreateSome() {
      final ComponentVal inner = factory.createS32(42);
      final ComponentVal val = factory.createSome(inner);
      assertTrue(val.isOption(), "Value should be option type");
      final Optional<ComponentVal> opt = val.asSome();
      assertTrue(opt.isPresent(), "Option should be present");
    }

    @Test
    @DisplayName("should create none value")
    void shouldCreateNone() {
      final ComponentVal val = factory.createNone();
      assertTrue(val.isOption(), "Value should be option type");
      final Optional<ComponentVal> opt = val.asSome();
      assertFalse(opt.isPresent(), "Option should be empty");
    }

    @Test
    @DisplayName("should reject null some value")
    void shouldRejectNullSomeValue() {
      assertThrows(
          IllegalArgumentException.class,
          () -> factory.createSome(null),
          "Expected IllegalArgumentException for null some value");
    }
  }

  @Nested
  @DisplayName("result type creation")
  final class ResultTypeCreationTests {

    @Test
    @DisplayName("should create ok result")
    void shouldCreateOk() {
      final ComponentVal val = factory.createOk(factory.createString("success"));
      assertTrue(val.isResult(), "Value should be result type");
      assertTrue(val.asResult().isOk(), "Result should be ok");
    }

    @Test
    @DisplayName("should create err result")
    void shouldCreateErr() {
      final ComponentVal val = factory.createErr(factory.createString("failure"));
      assertTrue(val.isResult(), "Value should be result type");
      assertTrue(val.asResult().isErr(), "Result should be err");
    }
  }

  @Nested
  @DisplayName("SimpleVal type checking")
  final class SimpleValTypeCheckingTests {

    @Test
    @DisplayName("should throw when accessing wrong type")
    void shouldThrowWhenAccessingWrongType() {
      final ComponentVal val = factory.createS32(42);
      assertThrows(
          IllegalStateException.class,
          () -> val.asString(),
          "Accessing string on s32 value should throw IllegalStateException");
    }

    @Test
    @DisplayName("should have correct type for each value")
    void shouldHaveCorrectType() {
      assertEquals(
          ComponentType.BOOL,
          factory.createBool(true).getType(),
          "Bool value should have BOOL type");
      assertEquals(
          ComponentType.STRING,
          factory.createString("test").getType(),
          "String value should have STRING type");
      assertEquals(
          ComponentType.S32, factory.createS32(0).getType(), "S32 value should have S32 type");
    }
  }
}
