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

package ai.tegmentum.wasmtime4j.coredump;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive test suite for the CoreDumpMemory interface.
 *
 * <p>CoreDumpMemory represents a WebAssembly memory snapshot captured in a core dump, providing
 * access to memory size, configuration, and segment data.
 */
@DisplayName("CoreDumpMemory Interface Tests")
class CoreDumpMemoryTest {

  // ========================================================================
  // Type Definition Tests
  // ========================================================================

  @Nested
  @DisplayName("Type Definition Tests")
  class TypeDefinitionTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeAnInterface() {
      assertTrue(CoreDumpMemory.class.isInterface(), "CoreDumpMemory should be an interface");
    }

    @Test
    @DisplayName("should be public")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(CoreDumpMemory.class.getModifiers()),
          "CoreDumpMemory should be public");
    }

    @Test
    @DisplayName("should not be final")
    void shouldNotBeFinal() {
      assertFalse(
          Modifier.isFinal(CoreDumpMemory.class.getModifiers()),
          "CoreDumpMemory should not be final (interfaces cannot be final)");
    }
  }

  // ========================================================================
  // Inheritance Tests
  // ========================================================================

  @Nested
  @DisplayName("Inheritance Tests")
  class InheritanceTests {

    @Test
    @DisplayName("should not extend any interfaces")
    void shouldNotExtendAnyInterfaces() {
      assertEquals(
          0,
          CoreDumpMemory.class.getInterfaces().length,
          "CoreDumpMemory should not extend any interfaces");
    }
  }

  // ========================================================================
  // Abstract Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Abstract Method Tests")
  class AbstractMethodTests {

    @Test
    @DisplayName("should have getInstanceIndex method")
    void shouldHaveGetInstanceIndexMethod() throws NoSuchMethodException {
      Method method = CoreDumpMemory.class.getMethod("getInstanceIndex");
      assertNotNull(method, "getInstanceIndex method should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
      assertFalse(method.isDefault(), "getInstanceIndex should be abstract");
    }

    @Test
    @DisplayName("should have getMemoryIndex method")
    void shouldHaveGetMemoryIndexMethod() throws NoSuchMethodException {
      Method method = CoreDumpMemory.class.getMethod("getMemoryIndex");
      assertNotNull(method, "getMemoryIndex method should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
      assertFalse(method.isDefault(), "getMemoryIndex should be abstract");
    }

    @Test
    @DisplayName("should have getName method")
    void shouldHaveGetNameMethod() throws NoSuchMethodException {
      Method method = CoreDumpMemory.class.getMethod("getName");
      assertNotNull(method, "getName method should exist");
      assertEquals(Optional.class, method.getReturnType(), "Should return Optional");
      assertFalse(method.isDefault(), "getName should be abstract");
    }

    @Test
    @DisplayName("should have getSizeInPages method")
    void shouldHaveGetSizeInPagesMethod() throws NoSuchMethodException {
      Method method = CoreDumpMemory.class.getMethod("getSizeInPages");
      assertNotNull(method, "getSizeInPages method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
      assertFalse(method.isDefault(), "getSizeInPages should be abstract");
    }

    @Test
    @DisplayName("should have getSizeInBytes method")
    void shouldHaveGetSizeInBytesMethod() throws NoSuchMethodException {
      Method method = CoreDumpMemory.class.getMethod("getSizeInBytes");
      assertNotNull(method, "getSizeInBytes method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
      assertFalse(method.isDefault(), "getSizeInBytes should be abstract");
    }

    @Test
    @DisplayName("should have isMemory64 method")
    void shouldHaveIsMemory64Method() throws NoSuchMethodException {
      Method method = CoreDumpMemory.class.getMethod("isMemory64");
      assertNotNull(method, "isMemory64 method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
      assertFalse(method.isDefault(), "isMemory64 should be abstract");
    }

    @Test
    @DisplayName("should have getMinPages method")
    void shouldHaveGetMinPagesMethod() throws NoSuchMethodException {
      Method method = CoreDumpMemory.class.getMethod("getMinPages");
      assertNotNull(method, "getMinPages method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
      assertFalse(method.isDefault(), "getMinPages should be abstract");
    }

    @Test
    @DisplayName("should have getMaxPages method")
    void shouldHaveGetMaxPagesMethod() throws NoSuchMethodException {
      Method method = CoreDumpMemory.class.getMethod("getMaxPages");
      assertNotNull(method, "getMaxPages method should exist");
      assertEquals(Optional.class, method.getReturnType(), "Should return Optional");
      assertFalse(method.isDefault(), "getMaxPages should be abstract");
    }

    @Test
    @DisplayName("should have getSegments method")
    void shouldHaveGetSegmentsMethod() throws NoSuchMethodException {
      Method method = CoreDumpMemory.class.getMethod("getSegments");
      assertNotNull(method, "getSegments method should exist");
      assertEquals(List.class, method.getReturnType(), "Should return List");
      assertFalse(method.isDefault(), "getSegments should be abstract");
    }

    @Test
    @DisplayName("should have read method")
    void shouldHaveReadMethod() throws NoSuchMethodException {
      Method method = CoreDumpMemory.class.getMethod("read", long.class, int.class);
      assertNotNull(method, "read method should exist");
      assertEquals(byte[].class, method.getReturnType(), "Should return byte[]");
      assertFalse(method.isDefault(), "read should be abstract");
    }

    @Test
    @DisplayName("should have exactly 10 abstract methods")
    void shouldHaveExactly10AbstractMethods() {
      long abstractMethods =
          Arrays.stream(CoreDumpMemory.class.getDeclaredMethods())
              .filter(m -> !m.isSynthetic())
              .filter(m -> !m.isDefault())
              .filter(m -> Modifier.isAbstract(m.getModifiers()))
              .count();
      assertEquals(10, abstractMethods, "CoreDumpMemory should have exactly 10 abstract methods");
    }
  }

  // ========================================================================
  // Method Count Tests
  // ========================================================================

  @Nested
  @DisplayName("Method Count Tests")
  class MethodCountTests {

    @Test
    @DisplayName("should have all expected methods")
    void shouldHaveAllExpectedMethods() {
      Set<String> expectedMethods =
          Set.of(
              "getInstanceIndex",
              "getMemoryIndex",
              "getName",
              "getSizeInPages",
              "getSizeInBytes",
              "isMemory64",
              "getMinPages",
              "getMaxPages",
              "getSegments",
              "read");

      Set<String> actualMethods =
          Arrays.stream(CoreDumpMemory.class.getDeclaredMethods())
              .filter(m -> !m.isSynthetic())
              .map(Method::getName)
              .collect(Collectors.toSet());

      for (String expected : expectedMethods) {
        assertTrue(
            actualMethods.contains(expected), "CoreDumpMemory should have method: " + expected);
      }
    }

    @Test
    @DisplayName("should have exactly 10 declared methods")
    void shouldHaveExactly10DeclaredMethods() {
      long methodCount =
          Arrays.stream(CoreDumpMemory.class.getDeclaredMethods())
              .filter(m -> !m.isSynthetic())
              .count();
      assertEquals(10, methodCount, "CoreDumpMemory should have exactly 10 declared methods");
    }
  }

  // ========================================================================
  // Default Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Default Method Tests")
  class DefaultMethodTests {

    @Test
    @DisplayName("should have no default methods")
    void shouldHaveNoDefaultMethods() {
      long defaultMethods =
          Arrays.stream(CoreDumpMemory.class.getDeclaredMethods())
              .filter(m -> !m.isSynthetic())
              .filter(Method::isDefault)
              .count();
      assertEquals(0, defaultMethods, "CoreDumpMemory should have no default methods");
    }
  }

  // ========================================================================
  // Static Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Static Method Tests")
  class StaticMethodTests {

    @Test
    @DisplayName("should have no static methods")
    void shouldHaveNoStaticMethods() {
      long staticMethods =
          Arrays.stream(CoreDumpMemory.class.getDeclaredMethods())
              .filter(m -> !m.isSynthetic())
              .filter(m -> Modifier.isStatic(m.getModifiers()))
              .count();
      assertEquals(0, staticMethods, "CoreDumpMemory should have no static methods");
    }
  }

  // ========================================================================
  // Field Tests
  // ========================================================================

  @Nested
  @DisplayName("Field Tests")
  class FieldTests {

    @Test
    @DisplayName("should have no declared fields")
    void shouldHaveNoDeclaredFields() {
      assertEquals(
          0,
          CoreDumpMemory.class.getDeclaredFields().length,
          "CoreDumpMemory should have no declared fields");
    }
  }

  // ========================================================================
  // Nested Classes Tests
  // ========================================================================

  @Nested
  @DisplayName("Nested Classes Tests")
  class NestedClassesTests {

    @Test
    @DisplayName("should have exactly 1 nested class")
    void shouldHaveExactly1NestedClass() {
      assertEquals(
          1,
          CoreDumpMemory.class.getDeclaredClasses().length,
          "CoreDumpMemory should have exactly 1 nested class");
    }

    @Test
    @DisplayName("should have MemorySegment nested interface")
    void shouldHaveMemorySegmentNestedInterface() {
      Class<?>[] nestedClasses = CoreDumpMemory.class.getDeclaredClasses();
      assertEquals(1, nestedClasses.length, "Should have 1 nested class");
      assertEquals(
          "MemorySegment",
          nestedClasses[0].getSimpleName(),
          "Nested class should be MemorySegment");
      assertTrue(nestedClasses[0].isInterface(), "MemorySegment should be an interface");
    }
  }

  // ========================================================================
  // Method Parameter Tests
  // ========================================================================

  @Nested
  @DisplayName("Method Parameter Tests")
  class MethodParameterTests {

    @Test
    @DisplayName("getInstanceIndex should have no parameters")
    void getInstanceIndexShouldHaveNoParameters() throws NoSuchMethodException {
      Method method = CoreDumpMemory.class.getMethod("getInstanceIndex");
      assertEquals(0, method.getParameterCount(), "getInstanceIndex should have no parameters");
    }

    @Test
    @DisplayName("getMemoryIndex should have no parameters")
    void getMemoryIndexShouldHaveNoParameters() throws NoSuchMethodException {
      Method method = CoreDumpMemory.class.getMethod("getMemoryIndex");
      assertEquals(0, method.getParameterCount(), "getMemoryIndex should have no parameters");
    }

    @Test
    @DisplayName("getName should have no parameters")
    void getNameShouldHaveNoParameters() throws NoSuchMethodException {
      Method method = CoreDumpMemory.class.getMethod("getName");
      assertEquals(0, method.getParameterCount(), "getName should have no parameters");
    }

    @Test
    @DisplayName("getSizeInPages should have no parameters")
    void getSizeInPagesShouldHaveNoParameters() throws NoSuchMethodException {
      Method method = CoreDumpMemory.class.getMethod("getSizeInPages");
      assertEquals(0, method.getParameterCount(), "getSizeInPages should have no parameters");
    }

    @Test
    @DisplayName("getSizeInBytes should have no parameters")
    void getSizeInBytesShouldHaveNoParameters() throws NoSuchMethodException {
      Method method = CoreDumpMemory.class.getMethod("getSizeInBytes");
      assertEquals(0, method.getParameterCount(), "getSizeInBytes should have no parameters");
    }

    @Test
    @DisplayName("isMemory64 should have no parameters")
    void isMemory64ShouldHaveNoParameters() throws NoSuchMethodException {
      Method method = CoreDumpMemory.class.getMethod("isMemory64");
      assertEquals(0, method.getParameterCount(), "isMemory64 should have no parameters");
    }

    @Test
    @DisplayName("getMinPages should have no parameters")
    void getMinPagesShouldHaveNoParameters() throws NoSuchMethodException {
      Method method = CoreDumpMemory.class.getMethod("getMinPages");
      assertEquals(0, method.getParameterCount(), "getMinPages should have no parameters");
    }

    @Test
    @DisplayName("getMaxPages should have no parameters")
    void getMaxPagesShouldHaveNoParameters() throws NoSuchMethodException {
      Method method = CoreDumpMemory.class.getMethod("getMaxPages");
      assertEquals(0, method.getParameterCount(), "getMaxPages should have no parameters");
    }

    @Test
    @DisplayName("getSegments should have no parameters")
    void getSegmentsShouldHaveNoParameters() throws NoSuchMethodException {
      Method method = CoreDumpMemory.class.getMethod("getSegments");
      assertEquals(0, method.getParameterCount(), "getSegments should have no parameters");
    }

    @Test
    @DisplayName("read should have 2 parameters")
    void readShouldHave2Parameters() throws NoSuchMethodException {
      Method method = CoreDumpMemory.class.getMethod("read", long.class, int.class);
      assertEquals(2, method.getParameterCount(), "read should have 2 parameters");
    }

    @Test
    @DisplayName("read parameters should have correct types")
    void readParametersShouldHaveCorrectTypes() throws NoSuchMethodException {
      Method method = CoreDumpMemory.class.getMethod("read", long.class, int.class);
      Class<?>[] paramTypes = method.getParameterTypes();
      assertEquals(long.class, paramTypes[0], "First param should be long (offset)");
      assertEquals(int.class, paramTypes[1], "Second param should be int (length)");
    }
  }

  // ========================================================================
  // Method Visibility Tests
  // ========================================================================

  @Nested
  @DisplayName("Method Visibility Tests")
  class MethodVisibilityTests {

    @Test
    @DisplayName("all methods should be public")
    void allMethodsShouldBePublic() {
      Arrays.stream(CoreDumpMemory.class.getDeclaredMethods())
          .filter(m -> !m.isSynthetic())
          .forEach(
              m ->
                  assertTrue(
                      Modifier.isPublic(m.getModifiers()),
                      "Method " + m.getName() + " should be public"));
    }
  }

  // ========================================================================
  // Generic Return Type Tests
  // ========================================================================

  @Nested
  @DisplayName("Generic Return Type Tests")
  class GenericReturnTypeTests {

    @Test
    @DisplayName("getName should return Optional<String>")
    void getNameShouldReturnOptionalString() throws NoSuchMethodException {
      Method method = CoreDumpMemory.class.getMethod("getName");
      Type returnType = method.getGenericReturnType();
      assertTrue(returnType instanceof ParameterizedType, "Return type should be parameterized");
      ParameterizedType paramType = (ParameterizedType) returnType;
      assertEquals(Optional.class, paramType.getRawType(), "Raw type should be Optional");
      assertEquals(
          String.class, paramType.getActualTypeArguments()[0], "Type argument should be String");
    }

    @Test
    @DisplayName("getSegments should return List<MemorySegment>")
    void getSegmentsShouldReturnListMemorySegment() throws NoSuchMethodException {
      Method method = CoreDumpMemory.class.getMethod("getSegments");
      Type returnType = method.getGenericReturnType();
      assertTrue(returnType instanceof ParameterizedType, "Return type should be parameterized");
      ParameterizedType paramType = (ParameterizedType) returnType;
      assertEquals(List.class, paramType.getRawType(), "Raw type should be List");
      Type typeArg = paramType.getActualTypeArguments()[0];
      assertTrue(
          typeArg.getTypeName().contains("MemorySegment"), "Type argument should be MemorySegment");
    }
  }

  // ========================================================================
  // Semantic Tests
  // ========================================================================

  @Nested
  @DisplayName("Semantic Tests")
  class SemanticTests {

    @Test
    @DisplayName("getInstanceIndex should return primitive int")
    void getInstanceIndexShouldReturnPrimitiveInt() throws NoSuchMethodException {
      Method method = CoreDumpMemory.class.getMethod("getInstanceIndex");
      assertEquals(
          int.class,
          method.getReturnType(),
          "getInstanceIndex should return primitive int, not Integer");
    }

    @Test
    @DisplayName("getMemoryIndex should return primitive int")
    void getMemoryIndexShouldReturnPrimitiveInt() throws NoSuchMethodException {
      Method method = CoreDumpMemory.class.getMethod("getMemoryIndex");
      assertEquals(
          int.class,
          method.getReturnType(),
          "getMemoryIndex should return primitive int, not Integer");
    }

    @Test
    @DisplayName("getSizeInPages should return primitive long")
    void getSizeInPagesShouldReturnPrimitiveLong() throws NoSuchMethodException {
      Method method = CoreDumpMemory.class.getMethod("getSizeInPages");
      assertEquals(
          long.class,
          method.getReturnType(),
          "getSizeInPages should return primitive long, not Long");
    }

    @Test
    @DisplayName("getSizeInBytes should return primitive long")
    void getSizeInBytesShouldReturnPrimitiveLong() throws NoSuchMethodException {
      Method method = CoreDumpMemory.class.getMethod("getSizeInBytes");
      assertEquals(
          long.class,
          method.getReturnType(),
          "getSizeInBytes should return primitive long, not Long");
    }

    @Test
    @DisplayName("isMemory64 should return primitive boolean")
    void isMemory64ShouldReturnPrimitiveBoolean() throws NoSuchMethodException {
      Method method = CoreDumpMemory.class.getMethod("isMemory64");
      assertEquals(
          boolean.class,
          method.getReturnType(),
          "isMemory64 should return primitive boolean, not Boolean");
    }

    @Test
    @DisplayName("getMinPages should return primitive long")
    void getMinPagesShouldReturnPrimitiveLong() throws NoSuchMethodException {
      Method method = CoreDumpMemory.class.getMethod("getMinPages");
      assertEquals(
          long.class, method.getReturnType(), "getMinPages should return primitive long, not Long");
    }

    @Test
    @DisplayName("getMaxPages should return Optional<Long>")
    void getMaxPagesShouldReturnOptionalLong() throws NoSuchMethodException {
      Method method = CoreDumpMemory.class.getMethod("getMaxPages");
      assertEquals(Optional.class, method.getReturnType(), "getMaxPages should return Optional");
      Type returnType = method.getGenericReturnType();
      assertTrue(returnType instanceof ParameterizedType, "Return type should be parameterized");
      ParameterizedType paramType = (ParameterizedType) returnType;
      assertEquals(Optional.class, paramType.getRawType(), "Raw type should be Optional");
      assertEquals(
          Long.class, paramType.getActualTypeArguments()[0], "Type argument should be Long");
    }

    @Test
    @DisplayName("read should return byte array")
    void readShouldReturnByteArray() throws NoSuchMethodException {
      Method method = CoreDumpMemory.class.getMethod("read", long.class, int.class);
      assertEquals(byte[].class, method.getReturnType(), "read should return byte[]");
    }
  }

  // ========================================================================
  // MemorySegment Nested Interface Tests
  // ========================================================================

  @Nested
  @DisplayName("MemorySegment Nested Interface Tests")
  class MemorySegmentNestedInterfaceTests {

    @Test
    @DisplayName("MemorySegment should be an interface")
    void memorySegmentShouldBeAnInterface() {
      assertTrue(
          CoreDumpMemory.MemorySegment.class.isInterface(), "MemorySegment should be an interface");
    }

    @Test
    @DisplayName("MemorySegment should be public")
    void memorySegmentShouldBePublic() {
      assertTrue(
          Modifier.isPublic(CoreDumpMemory.MemorySegment.class.getModifiers()),
          "MemorySegment should be public");
    }

    @Test
    @DisplayName("MemorySegment should be static")
    void memorySegmentShouldBeStatic() {
      assertTrue(
          Modifier.isStatic(CoreDumpMemory.MemorySegment.class.getModifiers()),
          "MemorySegment should be static");
    }

    @Test
    @DisplayName("MemorySegment should have getOffset method")
    void memorySegmentShouldHaveGetOffsetMethod() throws NoSuchMethodException {
      Method method = CoreDumpMemory.MemorySegment.class.getMethod("getOffset");
      assertNotNull(method, "getOffset method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
      assertFalse(method.isDefault(), "getOffset should be abstract");
    }

    @Test
    @DisplayName("MemorySegment should have getData method")
    void memorySegmentShouldHaveGetDataMethod() throws NoSuchMethodException {
      Method method = CoreDumpMemory.MemorySegment.class.getMethod("getData");
      assertNotNull(method, "getData method should exist");
      assertEquals(byte[].class, method.getReturnType(), "Should return byte[]");
      assertFalse(method.isDefault(), "getData should be abstract");
    }

    @Test
    @DisplayName("MemorySegment should have getSize default method")
    void memorySegmentShouldHaveGetSizeDefaultMethod() throws NoSuchMethodException {
      Method method = CoreDumpMemory.MemorySegment.class.getMethod("getSize");
      assertNotNull(method, "getSize method should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
      assertTrue(method.isDefault(), "getSize should be a default method");
    }

    @Test
    @DisplayName("MemorySegment should have exactly 3 methods")
    void memorySegmentShouldHaveExactly3Methods() {
      long methodCount =
          Arrays.stream(CoreDumpMemory.MemorySegment.class.getDeclaredMethods())
              .filter(m -> !m.isSynthetic())
              .count();
      assertEquals(3, methodCount, "MemorySegment should have exactly 3 declared methods");
    }

    @Test
    @DisplayName("MemorySegment should have 2 abstract and 1 default method")
    void memorySegmentShouldHave2AbstractAnd1DefaultMethod() {
      long abstractMethods =
          Arrays.stream(CoreDumpMemory.MemorySegment.class.getDeclaredMethods())
              .filter(m -> !m.isSynthetic())
              .filter(m -> !m.isDefault())
              .filter(m -> Modifier.isAbstract(m.getModifiers()))
              .count();
      long defaultMethods =
          Arrays.stream(CoreDumpMemory.MemorySegment.class.getDeclaredMethods())
              .filter(m -> !m.isSynthetic())
              .filter(Method::isDefault)
              .count();
      assertEquals(2, abstractMethods, "MemorySegment should have 2 abstract methods");
      assertEquals(1, defaultMethods, "MemorySegment should have 1 default method");
    }

    @Test
    @DisplayName("MemorySegment should have no fields")
    void memorySegmentShouldHaveNoFields() {
      assertEquals(
          0,
          CoreDumpMemory.MemorySegment.class.getDeclaredFields().length,
          "MemorySegment should have no declared fields");
    }

    @Test
    @DisplayName("MemorySegment should not extend any interfaces")
    void memorySegmentShouldNotExtendAnyInterfaces() {
      assertEquals(
          0,
          CoreDumpMemory.MemorySegment.class.getInterfaces().length,
          "MemorySegment should not extend any interfaces");
    }
  }
}
