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

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ComponentValFactory} class.
 *
 * <p>ComponentValFactory is the factory for creating Component Model values.
 */
@DisplayName("ComponentValFactory Tests")
class ComponentValFactoryTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be public abstract class")
    void shouldBePublicAbstractClass() {
      assertTrue(
          Modifier.isPublic(ComponentValFactory.class.getModifiers()),
          "ComponentValFactory should be public");
      assertTrue(
          Modifier.isAbstract(ComponentValFactory.class.getModifiers()),
          "ComponentValFactory should be abstract");
      assertFalse(
          ComponentValFactory.class.isInterface(),
          "ComponentValFactory should not be an interface");
    }

    @Test
    @DisplayName("should have INSTANCE static field")
    void shouldHaveInstanceStaticField() throws NoSuchFieldException {
      final var field = ComponentValFactory.class.getDeclaredField("INSTANCE");
      assertNotNull(field, "INSTANCE field should exist");
      assertTrue(Modifier.isStatic(field.getModifiers()), "INSTANCE should be static");
      assertTrue(Modifier.isFinal(field.getModifiers()), "INSTANCE should be final");
    }
  }

  @Nested
  @DisplayName("Abstract Factory Method Tests")
  class FactoryMethodTests {

    @Test
    @DisplayName("should have createBool method")
    void shouldHaveCreateBoolMethod() throws NoSuchMethodException {
      final Method method = ComponentValFactory.class.getMethod("createBool", boolean.class);
      assertNotNull(method, "createBool method should exist");
      assertTrue(Modifier.isAbstract(method.getModifiers()), "createBool should be abstract");
      assertEquals(ComponentVal.class, method.getReturnType(), "Should return ComponentVal");
    }

    @Test
    @DisplayName("should have createS8 method")
    void shouldHaveCreateS8Method() throws NoSuchMethodException {
      final Method method = ComponentValFactory.class.getMethod("createS8", byte.class);
      assertNotNull(method, "createS8 method should exist");
      assertTrue(Modifier.isAbstract(method.getModifiers()), "createS8 should be abstract");
      assertEquals(ComponentVal.class, method.getReturnType(), "Should return ComponentVal");
    }

    @Test
    @DisplayName("should have createS16 method")
    void shouldHaveCreateS16Method() throws NoSuchMethodException {
      final Method method = ComponentValFactory.class.getMethod("createS16", short.class);
      assertNotNull(method, "createS16 method should exist");
      assertTrue(Modifier.isAbstract(method.getModifiers()), "createS16 should be abstract");
      assertEquals(ComponentVal.class, method.getReturnType(), "Should return ComponentVal");
    }

    @Test
    @DisplayName("should have createS32 method")
    void shouldHaveCreateS32Method() throws NoSuchMethodException {
      final Method method = ComponentValFactory.class.getMethod("createS32", int.class);
      assertNotNull(method, "createS32 method should exist");
      assertTrue(Modifier.isAbstract(method.getModifiers()), "createS32 should be abstract");
      assertEquals(ComponentVal.class, method.getReturnType(), "Should return ComponentVal");
    }

    @Test
    @DisplayName("should have createS64 method")
    void shouldHaveCreateS64Method() throws NoSuchMethodException {
      final Method method = ComponentValFactory.class.getMethod("createS64", long.class);
      assertNotNull(method, "createS64 method should exist");
      assertTrue(Modifier.isAbstract(method.getModifiers()), "createS64 should be abstract");
      assertEquals(ComponentVal.class, method.getReturnType(), "Should return ComponentVal");
    }

    @Test
    @DisplayName("should have createU8 method")
    void shouldHaveCreateU8Method() throws NoSuchMethodException {
      final Method method = ComponentValFactory.class.getMethod("createU8", short.class);
      assertNotNull(method, "createU8 method should exist");
      assertTrue(Modifier.isAbstract(method.getModifiers()), "createU8 should be abstract");
      assertEquals(ComponentVal.class, method.getReturnType(), "Should return ComponentVal");
    }

    @Test
    @DisplayName("should have createU16 method")
    void shouldHaveCreateU16Method() throws NoSuchMethodException {
      final Method method = ComponentValFactory.class.getMethod("createU16", int.class);
      assertNotNull(method, "createU16 method should exist");
      assertTrue(Modifier.isAbstract(method.getModifiers()), "createU16 should be abstract");
      assertEquals(ComponentVal.class, method.getReturnType(), "Should return ComponentVal");
    }

    @Test
    @DisplayName("should have createU32 method")
    void shouldHaveCreateU32Method() throws NoSuchMethodException {
      final Method method = ComponentValFactory.class.getMethod("createU32", long.class);
      assertNotNull(method, "createU32 method should exist");
      assertTrue(Modifier.isAbstract(method.getModifiers()), "createU32 should be abstract");
      assertEquals(ComponentVal.class, method.getReturnType(), "Should return ComponentVal");
    }

    @Test
    @DisplayName("should have createU64 method")
    void shouldHaveCreateU64Method() throws NoSuchMethodException {
      final Method method = ComponentValFactory.class.getMethod("createU64", long.class);
      assertNotNull(method, "createU64 method should exist");
      assertTrue(Modifier.isAbstract(method.getModifiers()), "createU64 should be abstract");
      assertEquals(ComponentVal.class, method.getReturnType(), "Should return ComponentVal");
    }

    @Test
    @DisplayName("should have createF32 method")
    void shouldHaveCreateF32Method() throws NoSuchMethodException {
      final Method method = ComponentValFactory.class.getMethod("createF32", float.class);
      assertNotNull(method, "createF32 method should exist");
      assertTrue(Modifier.isAbstract(method.getModifiers()), "createF32 should be abstract");
      assertEquals(ComponentVal.class, method.getReturnType(), "Should return ComponentVal");
    }

    @Test
    @DisplayName("should have createF64 method")
    void shouldHaveCreateF64Method() throws NoSuchMethodException {
      final Method method = ComponentValFactory.class.getMethod("createF64", double.class);
      assertNotNull(method, "createF64 method should exist");
      assertTrue(Modifier.isAbstract(method.getModifiers()), "createF64 should be abstract");
      assertEquals(ComponentVal.class, method.getReturnType(), "Should return ComponentVal");
    }

    @Test
    @DisplayName("should have createChar method")
    void shouldHaveCreateCharMethod() throws NoSuchMethodException {
      final Method method = ComponentValFactory.class.getMethod("createChar", char.class);
      assertNotNull(method, "createChar method should exist");
      assertTrue(Modifier.isAbstract(method.getModifiers()), "createChar should be abstract");
      assertEquals(ComponentVal.class, method.getReturnType(), "Should return ComponentVal");
    }

    @Test
    @DisplayName("should have createString method")
    void shouldHaveCreateStringMethod() throws NoSuchMethodException {
      final Method method = ComponentValFactory.class.getMethod("createString", String.class);
      assertNotNull(method, "createString method should exist");
      assertTrue(Modifier.isAbstract(method.getModifiers()), "createString should be abstract");
      assertEquals(ComponentVal.class, method.getReturnType(), "Should return ComponentVal");
    }

    @Test
    @DisplayName("should have createList method")
    void shouldHaveCreateListMethod() throws NoSuchMethodException {
      final Method method = ComponentValFactory.class.getMethod("createList", List.class);
      assertNotNull(method, "createList method should exist");
      assertTrue(Modifier.isAbstract(method.getModifiers()), "createList should be abstract");
      assertEquals(ComponentVal.class, method.getReturnType(), "Should return ComponentVal");
    }

    @Test
    @DisplayName("should have createRecord method")
    void shouldHaveCreateRecordMethod() throws NoSuchMethodException {
      final Method method = ComponentValFactory.class.getMethod("createRecord", Map.class);
      assertNotNull(method, "createRecord method should exist");
      assertTrue(Modifier.isAbstract(method.getModifiers()), "createRecord should be abstract");
      assertEquals(ComponentVal.class, method.getReturnType(), "Should return ComponentVal");
    }

    @Test
    @DisplayName("should have createTuple method")
    void shouldHaveCreateTupleMethod() throws NoSuchMethodException {
      final Method method = ComponentValFactory.class.getMethod("createTuple", List.class);
      assertNotNull(method, "createTuple method should exist");
      assertTrue(Modifier.isAbstract(method.getModifiers()), "createTuple should be abstract");
      assertEquals(ComponentVal.class, method.getReturnType(), "Should return ComponentVal");
    }

    @Test
    @DisplayName("should have createVariant method")
    void shouldHaveCreateVariantMethod() throws NoSuchMethodException {
      final Method method =
          ComponentValFactory.class.getMethod("createVariant", String.class, ComponentVal.class);
      assertNotNull(method, "createVariant method should exist");
      assertTrue(Modifier.isAbstract(method.getModifiers()), "createVariant should be abstract");
      assertEquals(ComponentVal.class, method.getReturnType(), "Should return ComponentVal");
    }

    @Test
    @DisplayName("should have createEnum method")
    void shouldHaveCreateEnumMethod() throws NoSuchMethodException {
      final Method method = ComponentValFactory.class.getMethod("createEnum", String.class);
      assertNotNull(method, "createEnum method should exist");
      assertTrue(Modifier.isAbstract(method.getModifiers()), "createEnum should be abstract");
      assertEquals(ComponentVal.class, method.getReturnType(), "Should return ComponentVal");
    }

    @Test
    @DisplayName("should have createSome method")
    void shouldHaveCreateSomeMethod() throws NoSuchMethodException {
      final Method method = ComponentValFactory.class.getMethod("createSome", ComponentVal.class);
      assertNotNull(method, "createSome method should exist");
      assertTrue(Modifier.isAbstract(method.getModifiers()), "createSome should be abstract");
      assertEquals(ComponentVal.class, method.getReturnType(), "Should return ComponentVal");
    }

    @Test
    @DisplayName("should have createNone method")
    void shouldHaveCreateNoneMethod() throws NoSuchMethodException {
      final Method method = ComponentValFactory.class.getMethod("createNone");
      assertNotNull(method, "createNone method should exist");
      assertTrue(Modifier.isAbstract(method.getModifiers()), "createNone should be abstract");
      assertEquals(ComponentVal.class, method.getReturnType(), "Should return ComponentVal");
    }

    @Test
    @DisplayName("should have createOk method")
    void shouldHaveCreateOkMethod() throws NoSuchMethodException {
      final Method method = ComponentValFactory.class.getMethod("createOk", ComponentVal.class);
      assertNotNull(method, "createOk method should exist");
      assertTrue(Modifier.isAbstract(method.getModifiers()), "createOk should be abstract");
      assertEquals(ComponentVal.class, method.getReturnType(), "Should return ComponentVal");
    }

    @Test
    @DisplayName("should have createErr method")
    void shouldHaveCreateErrMethod() throws NoSuchMethodException {
      final Method method = ComponentValFactory.class.getMethod("createErr", ComponentVal.class);
      assertNotNull(method, "createErr method should exist");
      assertTrue(Modifier.isAbstract(method.getModifiers()), "createErr should be abstract");
      assertEquals(ComponentVal.class, method.getReturnType(), "Should return ComponentVal");
    }

    @Test
    @DisplayName("should have createFlags method")
    void shouldHaveCreateFlagsMethod() throws NoSuchMethodException {
      final Method method = ComponentValFactory.class.getMethod("createFlags", Set.class);
      assertNotNull(method, "createFlags method should exist");
      assertTrue(Modifier.isAbstract(method.getModifiers()), "createFlags should be abstract");
      assertEquals(ComponentVal.class, method.getReturnType(), "Should return ComponentVal");
    }
  }

  @Nested
  @DisplayName("Nested Class Tests")
  class NestedClassTests {

    @Test
    @DisplayName("should have DefaultImpl nested class")
    void shouldHaveDefaultImplNestedClass() {
      final var nestedClasses = ComponentValFactory.class.getDeclaredClasses();
      boolean found = false;
      for (var nestedClass : nestedClasses) {
        if (nestedClass.getSimpleName().equals("DefaultImpl")) {
          found = true;
          assertTrue(
              ComponentValFactory.class.isAssignableFrom(nestedClass),
              "DefaultImpl should extend ComponentValFactory");
          break;
        }
      }
      assertTrue(found, "Should have DefaultImpl nested class");
    }

    @Test
    @DisplayName("should have SimpleVal nested class")
    void shouldHaveSimpleValNestedClass() {
      final var nestedClasses = ComponentValFactory.class.getDeclaredClasses();
      boolean found = false;
      for (var nestedClass : nestedClasses) {
        if (nestedClass.getSimpleName().equals("SimpleVal")) {
          found = true;
          assertTrue(
              ComponentVal.class.isAssignableFrom(nestedClass),
              "SimpleVal should implement ComponentVal");
          break;
        }
      }
      assertTrue(found, "Should have SimpleVal nested class");
    }
  }

  @Nested
  @DisplayName("Default Implementation Tests")
  class DefaultImplementationTests {

    @Test
    @DisplayName("default factory should create bool values")
    void defaultFactoryShouldCreateBoolValues() {
      final ComponentVal trueVal = ComponentVal.bool(true);
      assertNotNull(trueVal, "Should create bool value");
      assertTrue(trueVal.isBool(), "Should be a bool type");
      assertTrue(trueVal.asBool(), "Should be true");

      final ComponentVal falseVal = ComponentVal.bool(false);
      assertFalse(falseVal.asBool(), "Should be false");
    }

    @Test
    @DisplayName("default factory should create s32 values")
    void defaultFactoryShouldCreateS32Values() {
      final ComponentVal val = ComponentVal.s32(42);
      assertNotNull(val, "Should create s32 value");
      assertTrue(val.isS32(), "Should be an s32 type");
      assertEquals(42, val.asS32(), "Should have correct value");
    }

    @Test
    @DisplayName("default factory should create string values")
    void defaultFactoryShouldCreateStringValues() {
      final ComponentVal val = ComponentVal.string("hello");
      assertNotNull(val, "Should create string value");
      assertTrue(val.isString(), "Should be a string type");
      assertEquals("hello", val.asString(), "Should have correct value");
    }

    @Test
    @DisplayName("default factory should reject null string")
    void defaultFactoryShouldRejectNullString() {
      assertThrows(
          IllegalArgumentException.class,
          () -> ComponentVal.string(null),
          "Should throw for null string");
    }

    @Test
    @DisplayName("default factory should create list values")
    void defaultFactoryShouldCreateListValues() {
      final ComponentVal val = ComponentVal.list(ComponentVal.s32(1), ComponentVal.s32(2));
      assertNotNull(val, "Should create list value");
      assertTrue(val.isList(), "Should be a list type");
      assertEquals(2, val.asList().size(), "Should have 2 elements");
    }

    @Test
    @DisplayName("default factory should validate u8 range")
    void defaultFactoryShouldValidateU8Range() {
      // Valid values
      final ComponentVal min = ComponentVal.u8((short) 0);
      assertNotNull(min, "Should accept 0");

      final ComponentVal max = ComponentVal.u8((short) 255);
      assertNotNull(max, "Should accept 255");

      // Invalid values
      assertThrows(
          IllegalArgumentException.class,
          () -> ComponentVal.u8((short) -1),
          "Should reject negative value");

      assertThrows(
          IllegalArgumentException.class,
          () -> ComponentVal.u8((short) 256),
          "Should reject value > 255");
    }

    @Test
    @DisplayName("default factory should validate u16 range")
    void defaultFactoryShouldValidateU16Range() {
      // Valid values
      final ComponentVal min = ComponentVal.u16(0);
      assertNotNull(min, "Should accept 0");

      final ComponentVal max = ComponentVal.u16(65535);
      assertNotNull(max, "Should accept 65535");

      // Invalid values
      assertThrows(
          IllegalArgumentException.class,
          () -> ComponentVal.u16(-1),
          "Should reject negative value");

      assertThrows(
          IllegalArgumentException.class,
          () -> ComponentVal.u16(65536),
          "Should reject value > 65535");
    }

    @Test
    @DisplayName("default factory should validate u32 range")
    void defaultFactoryShouldValidateU32Range() {
      // Valid values
      final ComponentVal min = ComponentVal.u32(0L);
      assertNotNull(min, "Should accept 0");

      final ComponentVal max = ComponentVal.u32(4294967295L);
      assertNotNull(max, "Should accept max u32 value");

      // Invalid values
      assertThrows(
          IllegalArgumentException.class,
          () -> ComponentVal.u32(-1L),
          "Should reject negative value");

      assertThrows(
          IllegalArgumentException.class,
          () -> ComponentVal.u32(4294967296L),
          "Should reject value > max u32");
    }
  }
}
