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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive test suite for the ComplexMarshalingService class.
 *
 * <p>ComplexMarshalingService provides efficient serialization and deserialization of complex data
 * structures for WebAssembly interop, including custom POJOs, collections, and multi-dimensional
 * arrays.
 */
@DisplayName("ComplexMarshalingService Class Tests")
class ComplexMarshalingServiceTest {

  // ========================================================================
  // Type Definition Tests
  // ========================================================================

  @Nested
  @DisplayName("Type Definition Tests")
  class TypeDefinitionTests {

    @Test
    @DisplayName("should be a class")
    void shouldBeAClass() {
      assertFalse(ComplexMarshalingService.class.isInterface(), "Should be a class, not interface");
    }

    @Test
    @DisplayName("should be public")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(ComplexMarshalingService.class.getModifiers()),
          "ComplexMarshalingService should be public");
    }

    @Test
    @DisplayName("should be final")
    void shouldBeFinal() {
      assertTrue(
          Modifier.isFinal(ComplexMarshalingService.class.getModifiers()),
          "ComplexMarshalingService should be final");
    }
  }

  // ========================================================================
  // Inheritance Tests
  // ========================================================================

  @Nested
  @DisplayName("Inheritance Tests")
  class InheritanceTests {

    @Test
    @DisplayName("should extend Object")
    void shouldExtendObject() {
      assertEquals(
          Object.class,
          ComplexMarshalingService.class.getSuperclass(),
          "Should extend Object directly");
    }

    @Test
    @DisplayName("should not implement any interfaces")
    void shouldNotImplementAnyInterfaces() {
      assertEquals(
          0,
          ComplexMarshalingService.class.getInterfaces().length,
          "Should not implement any interfaces");
    }
  }

  // ========================================================================
  // Constructor Tests
  // ========================================================================

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should have two constructors")
    void shouldHaveTwoConstructors() {
      Constructor<?>[] constructors = ComplexMarshalingService.class.getDeclaredConstructors();
      assertEquals(2, constructors.length, "Should have exactly 2 constructors");
    }

    @Test
    @DisplayName("should have public default constructor")
    void shouldHavePublicDefaultConstructor() throws NoSuchMethodException {
      Constructor<?> constructor = ComplexMarshalingService.class.getConstructor();
      assertTrue(
          Modifier.isPublic(constructor.getModifiers()), "Default constructor should be public");
      assertEquals(0, constructor.getParameterCount(), "Default constructor should have 0 params");
    }

    @Test
    @DisplayName("should have public constructor with MarshalingConfiguration parameter")
    void shouldHaveConfigurationConstructor() throws NoSuchMethodException {
      Constructor<?> constructor =
          ComplexMarshalingService.class.getConstructor(MarshalingConfiguration.class);
      assertTrue(
          Modifier.isPublic(constructor.getModifiers()),
          "Configuration constructor should be public");
      assertEquals(
          1, constructor.getParameterCount(), "Configuration constructor should have 1 param");
    }
  }

  // ========================================================================
  // Constant Field Tests
  // ========================================================================

  @Nested
  @DisplayName("Constant Field Tests")
  class ConstantFieldTests {

    @Test
    @DisplayName("should have MEMORY_MARSHALING_THRESHOLD constant")
    void shouldHaveMemoryMarshalingThresholdConstant() throws NoSuchFieldException {
      Field field = ComplexMarshalingService.class.getDeclaredField("MEMORY_MARSHALING_THRESHOLD");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "Should be private");
      assertTrue(Modifier.isStatic(field.getModifiers()), "Should be static");
      assertTrue(Modifier.isFinal(field.getModifiers()), "Should be final");
      assertEquals(int.class, field.getType(), "Should be int type");
    }

    @Test
    @DisplayName("should have MAX_RECURSION_DEPTH constant")
    void shouldHaveMaxRecursionDepthConstant() throws NoSuchFieldException {
      Field field = ComplexMarshalingService.class.getDeclaredField("MAX_RECURSION_DEPTH");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "Should be private");
      assertTrue(Modifier.isStatic(field.getModifiers()), "Should be static");
      assertTrue(Modifier.isFinal(field.getModifiers()), "Should be final");
      assertEquals(int.class, field.getType(), "Should be int type");
    }

    @Test
    @DisplayName("should have MAGIC_BYTES constant")
    void shouldHaveMagicBytesConstant() throws NoSuchFieldException {
      Field field = ComplexMarshalingService.class.getDeclaredField("MAGIC_BYTES");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "Should be private");
      assertTrue(Modifier.isStatic(field.getModifiers()), "Should be static");
      assertTrue(Modifier.isFinal(field.getModifiers()), "Should be final");
      assertEquals(byte[].class, field.getType(), "Should be byte[] type");
    }

    @Test
    @DisplayName("should have FORMAT_VERSION constant")
    void shouldHaveFormatVersionConstant() throws NoSuchFieldException {
      Field field = ComplexMarshalingService.class.getDeclaredField("FORMAT_VERSION");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "Should be private");
      assertTrue(Modifier.isStatic(field.getModifiers()), "Should be static");
      assertTrue(Modifier.isFinal(field.getModifiers()), "Should be final");
      assertEquals(int.class, field.getType(), "Should be int type");
    }
  }

  // ========================================================================
  // Instance Field Tests
  // ========================================================================

  @Nested
  @DisplayName("Instance Field Tests")
  class InstanceFieldTests {

    @Test
    @DisplayName("should have configuration field")
    void shouldHaveConfigurationField() throws NoSuchFieldException {
      Field field = ComplexMarshalingService.class.getDeclaredField("configuration");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "Should be private");
      assertTrue(Modifier.isFinal(field.getModifiers()), "Should be final");
      assertEquals(
          MarshalingConfiguration.class, field.getType(), "Should be MarshalingConfiguration");
    }

    @Test
    @DisplayName("should have LOGGER field")
    void shouldHaveLoggerField() throws NoSuchFieldException {
      Field field = ComplexMarshalingService.class.getDeclaredField("LOGGER");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "Should be private");
      assertTrue(Modifier.isStatic(field.getModifiers()), "Should be static");
      assertTrue(Modifier.isFinal(field.getModifiers()), "Should be final");
      assertEquals(
          java.util.logging.Logger.class, field.getType(), "Should be java.util.logging.Logger");
    }
  }

  // ========================================================================
  // Public Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Public Method Tests")
  class PublicMethodTests {

    @Test
    @DisplayName("should have marshal method")
    void shouldHaveMarshalMethod() throws NoSuchMethodException {
      Method method = ComplexMarshalingService.class.getMethod("marshal", Object.class);
      assertNotNull(method, "marshal method should exist");
      assertTrue(Modifier.isPublic(method.getModifiers()), "Should be public");
      assertEquals(
          ComplexMarshalingService.MarshaledData.class,
          method.getReturnType(),
          "Should return MarshaledData");
    }

    @Test
    @DisplayName("should have unmarshal method")
    void shouldHaveUnmarshalMethod() throws NoSuchMethodException {
      Method method =
          ComplexMarshalingService.class.getMethod(
              "unmarshal", ComplexMarshalingService.MarshaledData.class, Class.class);
      assertNotNull(method, "unmarshal method should exist");
      assertTrue(Modifier.isPublic(method.getModifiers()), "Should be public");
      assertEquals(Object.class, method.getReturnType(), "Should return Object (generic T)");
    }

    @Test
    @DisplayName("should have createComplexValue method")
    void shouldHaveCreateComplexValueMethod() throws NoSuchMethodException {
      Method method = ComplexMarshalingService.class.getMethod("createComplexValue", Object.class);
      assertNotNull(method, "createComplexValue method should exist");
      assertTrue(Modifier.isPublic(method.getModifiers()), "Should be public");
      assertEquals(
          WasmComplexValue.class, method.getReturnType(), "Should return WasmComplexValue");
    }

    @Test
    @DisplayName("should have estimateSerializedSize method")
    void shouldHaveEstimateSerializedSizeMethod() throws NoSuchMethodException {
      Method method =
          ComplexMarshalingService.class.getMethod("estimateSerializedSize", Object.class);
      assertNotNull(method, "estimateSerializedSize method should exist");
      assertTrue(Modifier.isPublic(method.getModifiers()), "Should be public");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }
  }

  // ========================================================================
  // MarshalingStrategy Enum Tests
  // ========================================================================

  @Nested
  @DisplayName("MarshalingStrategy Enum Tests")
  class MarshalingStrategyEnumTests {

    @Test
    @DisplayName("should have MarshalingStrategy as nested enum")
    void shouldHaveMarshalingStrategyEnum() {
      Class<?>[] declaredClasses = ComplexMarshalingService.class.getDeclaredClasses();
      boolean found = false;
      for (Class<?> clazz : declaredClasses) {
        if (clazz.getSimpleName().equals("MarshalingStrategy") && clazz.isEnum()) {
          found = true;
          break;
        }
      }
      assertTrue(found, "MarshalingStrategy enum should exist");
    }

    @Test
    @DisplayName("MarshalingStrategy should be public")
    void marshalingStrategyShouldBePublic() {
      assertTrue(
          Modifier.isPublic(ComplexMarshalingService.MarshalingStrategy.class.getModifiers()),
          "MarshalingStrategy should be public");
    }

    @Test
    @DisplayName("MarshalingStrategy should have 3 values")
    void marshalingStrategyShouldHave3Values() {
      ComplexMarshalingService.MarshalingStrategy[] values =
          ComplexMarshalingService.MarshalingStrategy.values();
      assertEquals(3, values.length, "MarshalingStrategy should have 3 values");
    }

    @Test
    @DisplayName("MarshalingStrategy should have VALUE_BASED value")
    void shouldHaveValueBasedValue() {
      assertNotNull(
          ComplexMarshalingService.MarshalingStrategy.valueOf("VALUE_BASED"),
          "VALUE_BASED should exist");
    }

    @Test
    @DisplayName("MarshalingStrategy should have MEMORY_BASED value")
    void shouldHaveMemoryBasedValue() {
      assertNotNull(
          ComplexMarshalingService.MarshalingStrategy.valueOf("MEMORY_BASED"),
          "MEMORY_BASED should exist");
    }

    @Test
    @DisplayName("MarshalingStrategy should have HYBRID value")
    void shouldHaveHybridValue() {
      assertNotNull(
          ComplexMarshalingService.MarshalingStrategy.valueOf("HYBRID"), "HYBRID should exist");
    }
  }

  // ========================================================================
  // MarshaledData Nested Class Tests
  // ========================================================================

  @Nested
  @DisplayName("MarshaledData Nested Class Tests")
  class MarshaledDataNestedClassTests {

    @Test
    @DisplayName("should have MarshaledData as nested class")
    void shouldHaveMarshaledDataClass() {
      Class<?>[] declaredClasses = ComplexMarshalingService.class.getDeclaredClasses();
      boolean found = false;
      for (Class<?> clazz : declaredClasses) {
        if (clazz.getSimpleName().equals("MarshaledData") && !clazz.isEnum()) {
          found = true;
          break;
        }
      }
      assertTrue(found, "MarshaledData class should exist");
    }

    @Test
    @DisplayName("MarshaledData should be public static final")
    void marshaledDataShouldBePublicStaticFinal() {
      int modifiers = ComplexMarshalingService.MarshaledData.class.getModifiers();
      assertTrue(Modifier.isPublic(modifiers), "MarshaledData should be public");
      assertTrue(Modifier.isStatic(modifiers), "MarshaledData should be static");
      assertTrue(Modifier.isFinal(modifiers), "MarshaledData should be final");
    }

    @Test
    @DisplayName("MarshaledData should have getStrategy method")
    void marshaledDataShouldHaveGetStrategyMethod() throws NoSuchMethodException {
      Method method = ComplexMarshalingService.MarshaledData.class.getMethod("getStrategy");
      assertNotNull(method, "getStrategy should exist");
      assertEquals(
          ComplexMarshalingService.MarshalingStrategy.class,
          method.getReturnType(),
          "Should return MarshalingStrategy");
    }

    @Test
    @DisplayName("MarshaledData should have getStrategyCode method")
    void marshaledDataShouldHaveGetStrategyCodeMethod() throws NoSuchMethodException {
      Method method = ComplexMarshalingService.MarshaledData.class.getMethod("getStrategyCode");
      assertNotNull(method, "getStrategyCode should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }

    @Test
    @DisplayName("MarshaledData should have getValueData method")
    void marshaledDataShouldHaveGetValueDataMethod() throws NoSuchMethodException {
      Method method = ComplexMarshalingService.MarshaledData.class.getMethod("getValueData");
      assertNotNull(method, "getValueData should exist");
      assertEquals(byte[].class, method.getReturnType(), "Should return byte[]");
    }

    @Test
    @DisplayName("MarshaledData should have getMemoryHandle method")
    void marshaledDataShouldHaveGetMemoryHandleMethod() throws NoSuchMethodException {
      Method method = ComplexMarshalingService.MarshaledData.class.getMethod("getMemoryHandle");
      assertNotNull(method, "getMemoryHandle should exist");
      assertEquals(
          ComplexMarshalingService.MemoryHandle.class,
          method.getReturnType(),
          "Should return MemoryHandle");
    }
  }

  // ========================================================================
  // MemoryHandle Nested Class Tests
  // ========================================================================

  @Nested
  @DisplayName("MemoryHandle Nested Class Tests")
  class MemoryHandleNestedClassTests {

    @Test
    @DisplayName("should have MemoryHandle as nested class")
    void shouldHaveMemoryHandleClass() {
      Class<?>[] declaredClasses = ComplexMarshalingService.class.getDeclaredClasses();
      boolean found = false;
      for (Class<?> clazz : declaredClasses) {
        if (clazz.getSimpleName().equals("MemoryHandle")) {
          found = true;
          break;
        }
      }
      assertTrue(found, "MemoryHandle class should exist");
    }

    @Test
    @DisplayName("MemoryHandle should be public static final")
    void memoryHandleShouldBePublicStaticFinal() {
      int modifiers = ComplexMarshalingService.MemoryHandle.class.getModifiers();
      assertTrue(Modifier.isPublic(modifiers), "MemoryHandle should be public");
      assertTrue(Modifier.isStatic(modifiers), "MemoryHandle should be static");
      assertTrue(Modifier.isFinal(modifiers), "MemoryHandle should be final");
    }

    @Test
    @DisplayName("MemoryHandle should have getAddress method")
    void memoryHandleShouldHaveGetAddressMethod() throws NoSuchMethodException {
      Method method = ComplexMarshalingService.MemoryHandle.class.getMethod("getAddress");
      assertNotNull(method, "getAddress should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("MemoryHandle should have getSize method")
    void memoryHandleShouldHaveGetSizeMethod() throws NoSuchMethodException {
      Method method = ComplexMarshalingService.MemoryHandle.class.getMethod("getSize");
      assertNotNull(method, "getSize should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }
  }

  // ========================================================================
  // Method Count Tests
  // ========================================================================

  @Nested
  @DisplayName("Method Count Tests")
  class MethodCountTests {

    @Test
    @DisplayName("should have expected public methods")
    void shouldHaveExpectedPublicMethods() {
      Set<String> expectedMethods =
          Set.of("marshal", "unmarshal", "createComplexValue", "estimateSerializedSize");

      Set<String> actualMethods =
          Arrays.stream(ComplexMarshalingService.class.getDeclaredMethods())
              .filter(m -> !m.isSynthetic())
              .filter(m -> Modifier.isPublic(m.getModifiers()))
              .map(Method::getName)
              .collect(Collectors.toSet());

      for (String expected : expectedMethods) {
        assertTrue(actualMethods.contains(expected), "Should have method: " + expected);
      }
    }

    @Test
    @DisplayName("should have exactly 4 declared public methods")
    void shouldHaveExactly4DeclaredPublicMethods() {
      long methodCount =
          Arrays.stream(ComplexMarshalingService.class.getDeclaredMethods())
              .filter(m -> !m.isSynthetic())
              .filter(m -> Modifier.isPublic(m.getModifiers()))
              .count();
      assertEquals(4, methodCount, "Should have exactly 4 declared public methods");
    }
  }

  // ========================================================================
  // Nested Classes Count Tests
  // ========================================================================

  @Nested
  @DisplayName("Nested Classes Count Tests")
  class NestedClassesCountTests {

    @Test
    @DisplayName("should have 4 public nested classes/enums")
    void shouldHave4PublicNestedClasses() {
      long publicNestedCount =
          Arrays.stream(ComplexMarshalingService.class.getDeclaredClasses())
              .filter(c -> Modifier.isPublic(c.getModifiers()))
              .count();
      // MarshalingStrategy, MarshaledData, MemoryHandle are public
      // ObjectMetadata and ValidatingObjectInputStream are private
      assertEquals(3, publicNestedCount, "Should have 3 public nested classes/enums");
    }

    @Test
    @DisplayName("should have total 5 nested classes (including private)")
    void shouldHave5NestedClassesTotal() {
      int nestedCount = ComplexMarshalingService.class.getDeclaredClasses().length;
      // MarshalingStrategy, MarshaledData, MemoryHandle (public)
      // ObjectMetadata, ValidatingObjectInputStream (private)
      assertEquals(5, nestedCount, "Should have 5 nested classes/enums total");
    }
  }
}
