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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.wasi.io.WasiPollable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Flow;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the {@link ComponentStream} interface.
 *
 * <p>This test class verifies the interface structure, method signatures, inner enums, and default
 * method implementations for the ComponentStream API.
 */
@DisplayName("ComponentStream Interface Tests")
class ComponentStreamTest {

  // ========================================================================
  // Interface Structure Tests
  // ========================================================================

  @Nested
  @DisplayName("Interface Structure Tests")
  class InterfaceStructureTests {

    @Test
    @DisplayName("ComponentStream should be an interface")
    void shouldBeAnInterface() {
      assertTrue(ComponentStream.class.isInterface(), "ComponentStream should be an interface");
    }

    @Test
    @DisplayName("ComponentStream should be a public interface")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(ComponentStream.class.getModifiers()),
          "ComponentStream should be public");
    }

    @Test
    @DisplayName("ComponentStream should be a generic interface with type parameter T")
    void shouldBeGeneric() {
      TypeVariable<?>[] typeParams = ComponentStream.class.getTypeParameters();
      assertEquals(1, typeParams.length, "ComponentStream should have one type parameter");
      assertEquals("T", typeParams[0].getName(), "Type parameter should be named T");
    }

    @Test
    @DisplayName("ComponentStream should extend AutoCloseable")
    void shouldExtendAutoCloseable() {
      Class<?>[] interfaces = ComponentStream.class.getInterfaces();
      boolean extendsAutoCloseable =
          Arrays.stream(interfaces).anyMatch(i -> i.equals(AutoCloseable.class));
      assertTrue(extendsAutoCloseable, "ComponentStream should extend AutoCloseable");
    }
  }

  // ========================================================================
  // Abstract Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Abstract Method Tests")
  class AbstractMethodTests {

    @Test
    @DisplayName("should have getElementType method")
    void shouldHaveGetElementTypeMethod() throws NoSuchMethodException {
      Method method = ComponentStream.class.getMethod("getElementType");
      assertNotNull(method, "getElementType method should exist");
      assertEquals(WitType.class, method.getReturnType(), "Return type should be WitType");
    }

    @Test
    @DisplayName("should have getDirection method")
    void shouldHaveGetDirectionMethod() throws NoSuchMethodException {
      Method method = ComponentStream.class.getMethod("getDirection");
      assertNotNull(method, "getDirection method should exist");
      assertEquals(
          ComponentStream.Direction.class,
          method.getReturnType(),
          "Return type should be Direction");
    }

    @Test
    @DisplayName("should have read method with int parameter")
    void shouldHaveReadMethod() throws NoSuchMethodException {
      Method method = ComponentStream.class.getMethod("read", int.class);
      assertNotNull(method, "read method should exist");
      assertEquals(List.class, method.getReturnType(), "Return type should be List");
    }

    @Test
    @DisplayName("should have readBlocking method")
    void shouldHaveReadBlockingMethod() throws NoSuchMethodException {
      Method method = ComponentStream.class.getMethod("readBlocking");
      assertNotNull(method, "readBlocking method should exist");
      assertEquals(Optional.class, method.getReturnType(), "Return type should be Optional");
    }

    @Test
    @DisplayName("should have write method with List parameter")
    void shouldHaveWriteListMethod() throws NoSuchMethodException {
      Method method = ComponentStream.class.getMethod("write", List.class);
      assertNotNull(method, "write(List) method should exist");
      assertEquals(int.class, method.getReturnType(), "Return type should be int");
    }

    @Test
    @DisplayName("should have writeBlocking method with element parameter")
    void shouldHaveWriteBlockingMethod() throws NoSuchMethodException {
      Method method = ComponentStream.class.getMethod("writeBlocking", Object.class);
      assertNotNull(method, "writeBlocking method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
    }

    @Test
    @DisplayName("should have end method")
    void shouldHaveEndMethod() throws NoSuchMethodException {
      Method method = ComponentStream.class.getMethod("end");
      assertNotNull(method, "end method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
    }

    @Test
    @DisplayName("should have isEnded method")
    void shouldHaveIsEndedMethod() throws NoSuchMethodException {
      Method method = ComponentStream.class.getMethod("isEnded");
      assertNotNull(method, "isEnded method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Return type should be boolean");
    }

    @Test
    @DisplayName("should have available method")
    void shouldHaveAvailableMethod() throws NoSuchMethodException {
      Method method = ComponentStream.class.getMethod("available");
      assertNotNull(method, "available method should exist");
      assertEquals(int.class, method.getReturnType(), "Return type should be int");
    }

    @Test
    @DisplayName("should have writeCapacity method")
    void shouldHaveWriteCapacityMethod() throws NoSuchMethodException {
      Method method = ComponentStream.class.getMethod("writeCapacity");
      assertNotNull(method, "writeCapacity method should exist");
      assertEquals(int.class, method.getReturnType(), "Return type should be int");
    }

    @Test
    @DisplayName("should have subscribe method")
    void shouldHaveSubscribeMethod() throws NoSuchMethodException {
      Method method = ComponentStream.class.getMethod("subscribe");
      assertNotNull(method, "subscribe method should exist");
      assertEquals(
          WasiPollable.class, method.getReturnType(), "Return type should be WasiPollable");
    }

    @Test
    @DisplayName("should have isOpen method")
    void shouldHaveIsOpenMethod() throws NoSuchMethodException {
      Method method = ComponentStream.class.getMethod("isOpen");
      assertNotNull(method, "isOpen method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Return type should be boolean");
    }

    @Test
    @DisplayName("should have getState method")
    void shouldHaveGetStateMethod() throws NoSuchMethodException {
      Method method = ComponentStream.class.getMethod("getState");
      assertNotNull(method, "getState method should exist");
      assertEquals(
          ComponentStream.State.class, method.getReturnType(), "Return type should be State");
    }

    @Test
    @DisplayName("should have iterator method")
    void shouldHaveIteratorMethod() throws NoSuchMethodException {
      Method method = ComponentStream.class.getMethod("iterator");
      assertNotNull(method, "iterator method should exist");
      assertEquals(Iterator.class, method.getReturnType(), "Return type should be Iterator");
    }

    @Test
    @DisplayName("should have toPublisher method")
    void shouldHaveToPublisherMethod() throws NoSuchMethodException {
      Method method = ComponentStream.class.getMethod("toPublisher");
      assertNotNull(method, "toPublisher method should exist");
      assertEquals(
          Flow.Publisher.class, method.getReturnType(), "Return type should be Flow.Publisher");
    }

    @Test
    @DisplayName("should have close method from AutoCloseable")
    void shouldHaveCloseMethod() throws NoSuchMethodException {
      Method method = ComponentStream.class.getMethod("close");
      assertNotNull(method, "close method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
    }
  }

  // ========================================================================
  // Default Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Default Method Tests")
  class DefaultMethodTests {

    @Test
    @DisplayName("isReadable should be a default method")
    void isReadableShouldBeDefaultMethod() throws NoSuchMethodException {
      Method method = ComponentStream.class.getMethod("isReadable");
      assertTrue(method.isDefault(), "isReadable should be a default method");
      assertEquals(boolean.class, method.getReturnType(), "Return type should be boolean");
    }

    @Test
    @DisplayName("isWritable should be a default method")
    void isWritableShouldBeDefaultMethod() throws NoSuchMethodException {
      Method method = ComponentStream.class.getMethod("isWritable");
      assertTrue(method.isDefault(), "isWritable should be a default method");
      assertEquals(boolean.class, method.getReturnType(), "Return type should be boolean");
    }

    @Test
    @DisplayName("readOne should be a default method")
    void readOneShouldBeDefaultMethod() throws NoSuchMethodException {
      Method method = ComponentStream.class.getMethod("readOne");
      assertTrue(method.isDefault(), "readOne should be a default method");
      assertEquals(Optional.class, method.getReturnType(), "Return type should be Optional");
    }

    @Test
    @DisplayName("write(T) should be a default method")
    void writeSingleElementShouldBeDefaultMethod() throws NoSuchMethodException {
      Method method = ComponentStream.class.getMethod("write", Object.class);
      assertTrue(method.isDefault(), "write(T) should be a default method");
      assertEquals(boolean.class, method.getReturnType(), "Return type should be boolean");
    }
  }

  // ========================================================================
  // Direction Enum Tests
  // ========================================================================

  @Nested
  @DisplayName("Direction Enum Tests")
  class DirectionEnumTests {

    @Test
    @DisplayName("Direction should be an inner enum")
    void directionShouldBeEnum() {
      Class<?>[] innerClasses = ComponentStream.class.getDeclaredClasses();
      boolean hasDirection =
          Arrays.stream(innerClasses)
              .anyMatch(c -> c.getSimpleName().equals("Direction") && c.isEnum());
      assertTrue(hasDirection, "Should have Direction inner enum");
    }

    @Test
    @DisplayName("Direction enum should have READ value")
    void directionShouldHaveReadValue() {
      ComponentStream.Direction[] values = ComponentStream.Direction.values();
      boolean hasRead = Arrays.stream(values).anyMatch(d -> d.name().equals("READ"));
      assertTrue(hasRead, "Direction should have READ value");
    }

    @Test
    @DisplayName("Direction enum should have WRITE value")
    void directionShouldHaveWriteValue() {
      ComponentStream.Direction[] values = ComponentStream.Direction.values();
      boolean hasWrite = Arrays.stream(values).anyMatch(d -> d.name().equals("WRITE"));
      assertTrue(hasWrite, "Direction should have WRITE value");
    }

    @Test
    @DisplayName("Direction enum should have BIDIRECTIONAL value")
    void directionShouldHaveBidirectionalValue() {
      ComponentStream.Direction[] values = ComponentStream.Direction.values();
      boolean hasBidirectional =
          Arrays.stream(values).anyMatch(d -> d.name().equals("BIDIRECTIONAL"));
      assertTrue(hasBidirectional, "Direction should have BIDIRECTIONAL value");
    }

    @Test
    @DisplayName("Direction enum should have exactly 3 values")
    void directionShouldHaveExactlyThreeValues() {
      ComponentStream.Direction[] values = ComponentStream.Direction.values();
      assertEquals(3, values.length, "Direction should have exactly 3 values");
    }

    @Test
    @DisplayName("Direction values should be in expected order")
    void directionValuesShouldBeInOrder() {
      ComponentStream.Direction[] values = ComponentStream.Direction.values();
      String[] expectedOrder = {"READ", "WRITE", "BIDIRECTIONAL"};
      String[] actualOrder = Arrays.stream(values).map(Enum::name).toArray(String[]::new);
      assertArrayEquals(expectedOrder, actualOrder, "Direction values should be in expected order");
    }
  }

  // ========================================================================
  // State Enum Tests
  // ========================================================================

  @Nested
  @DisplayName("State Enum Tests")
  class StateEnumTests {

    @Test
    @DisplayName("State should be an inner enum")
    void stateShouldBeEnum() {
      Class<?>[] innerClasses = ComponentStream.class.getDeclaredClasses();
      boolean hasState =
          Arrays.stream(innerClasses)
              .anyMatch(c -> c.getSimpleName().equals("State") && c.isEnum());
      assertTrue(hasState, "Should have State inner enum");
    }

    @Test
    @DisplayName("State enum should have OPEN value")
    void stateShouldHaveOpenValue() {
      ComponentStream.State[] values = ComponentStream.State.values();
      boolean hasOpen = Arrays.stream(values).anyMatch(s -> s.name().equals("OPEN"));
      assertTrue(hasOpen, "State should have OPEN value");
    }

    @Test
    @DisplayName("State enum should have ENDED value")
    void stateShouldHaveEndedValue() {
      ComponentStream.State[] values = ComponentStream.State.values();
      boolean hasEnded = Arrays.stream(values).anyMatch(s -> s.name().equals("ENDED"));
      assertTrue(hasEnded, "State should have ENDED value");
    }

    @Test
    @DisplayName("State enum should have ERROR value")
    void stateShouldHaveErrorValue() {
      ComponentStream.State[] values = ComponentStream.State.values();
      boolean hasError = Arrays.stream(values).anyMatch(s -> s.name().equals("ERROR"));
      assertTrue(hasError, "State should have ERROR value");
    }

    @Test
    @DisplayName("State enum should have CLOSED value")
    void stateShouldHaveClosedValue() {
      ComponentStream.State[] values = ComponentStream.State.values();
      boolean hasClosed = Arrays.stream(values).anyMatch(s -> s.name().equals("CLOSED"));
      assertTrue(hasClosed, "State should have CLOSED value");
    }

    @Test
    @DisplayName("State enum should have exactly 4 values")
    void stateShouldHaveExactlyFourValues() {
      ComponentStream.State[] values = ComponentStream.State.values();
      assertEquals(4, values.length, "State should have exactly 4 values");
    }

    @Test
    @DisplayName("State values should be in expected order")
    void stateValuesShouldBeInOrder() {
      ComponentStream.State[] values = ComponentStream.State.values();
      String[] expectedOrder = {"OPEN", "ENDED", "ERROR", "CLOSED"};
      String[] actualOrder = Arrays.stream(values).map(Enum::name).toArray(String[]::new);
      assertArrayEquals(expectedOrder, actualOrder, "State values should be in expected order");
    }
  }

  // ========================================================================
  // Exception Tests
  // ========================================================================

  @Nested
  @DisplayName("Exception Specification Tests")
  class ExceptionSpecificationTests {

    @Test
    @DisplayName("read method should throw WasmException")
    void readShouldThrowWasmException() throws NoSuchMethodException {
      Method method = ComponentStream.class.getMethod("read", int.class);
      Class<?>[] exceptionTypes = method.getExceptionTypes();
      boolean throwsWasmException =
          Arrays.stream(exceptionTypes).anyMatch(e -> e.getSimpleName().equals("WasmException"));
      assertTrue(throwsWasmException, "read should declare WasmException");
    }

    @Test
    @DisplayName("write(List) method should throw WasmException")
    void writeListShouldThrowWasmException() throws NoSuchMethodException {
      Method method = ComponentStream.class.getMethod("write", List.class);
      Class<?>[] exceptionTypes = method.getExceptionTypes();
      boolean throwsWasmException =
          Arrays.stream(exceptionTypes).anyMatch(e -> e.getSimpleName().equals("WasmException"));
      assertTrue(throwsWasmException, "write(List) should declare WasmException");
    }

    @Test
    @DisplayName("subscribe method should throw WasmException")
    void subscribeShouldThrowWasmException() throws NoSuchMethodException {
      Method method = ComponentStream.class.getMethod("subscribe");
      Class<?>[] exceptionTypes = method.getExceptionTypes();
      boolean throwsWasmException =
          Arrays.stream(exceptionTypes).anyMatch(e -> e.getSimpleName().equals("WasmException"));
      assertTrue(throwsWasmException, "subscribe should declare WasmException");
    }

    @Test
    @DisplayName("end method should throw WasmException")
    void endShouldThrowWasmException() throws NoSuchMethodException {
      Method method = ComponentStream.class.getMethod("end");
      Class<?>[] exceptionTypes = method.getExceptionTypes();
      boolean throwsWasmException =
          Arrays.stream(exceptionTypes).anyMatch(e -> e.getSimpleName().equals("WasmException"));
      assertTrue(throwsWasmException, "end should declare WasmException");
    }
  }

  // ========================================================================
  // Generic Type Tests
  // ========================================================================

  @Nested
  @DisplayName("Generic Type Tests")
  class GenericTypeTests {

    @Test
    @DisplayName("read method should return List<T>")
    void readShouldReturnListOfT() throws NoSuchMethodException {
      Method method = ComponentStream.class.getMethod("read", int.class);
      Type genericReturnType = method.getGenericReturnType();
      assertTrue(
          genericReturnType instanceof ParameterizedType, "Return type should be parameterized");

      ParameterizedType parameterizedType = (ParameterizedType) genericReturnType;
      assertEquals(List.class, parameterizedType.getRawType(), "Raw type should be List");

      Type[] typeArguments = parameterizedType.getActualTypeArguments();
      assertEquals(1, typeArguments.length, "Should have one type argument");
      assertTrue(
          typeArguments[0] instanceof TypeVariable, "Type argument should be a type variable");
      assertEquals(
          "T", ((TypeVariable<?>) typeArguments[0]).getName(), "Type variable should be T");
    }

    @Test
    @DisplayName("iterator method should return Iterator<T>")
    void iteratorShouldReturnIteratorOfT() throws NoSuchMethodException {
      Method method = ComponentStream.class.getMethod("iterator");
      Type genericReturnType = method.getGenericReturnType();
      assertTrue(
          genericReturnType instanceof ParameterizedType, "Return type should be parameterized");

      ParameterizedType parameterizedType = (ParameterizedType) genericReturnType;
      assertEquals(Iterator.class, parameterizedType.getRawType(), "Raw type should be Iterator");
    }

    @Test
    @DisplayName("toPublisher method should return Flow.Publisher<T>")
    void toPublisherShouldReturnPublisherOfT() throws NoSuchMethodException {
      Method method = ComponentStream.class.getMethod("toPublisher");
      Type genericReturnType = method.getGenericReturnType();
      assertTrue(
          genericReturnType instanceof ParameterizedType, "Return type should be parameterized");

      ParameterizedType parameterizedType = (ParameterizedType) genericReturnType;
      assertEquals(
          Flow.Publisher.class,
          parameterizedType.getRawType(),
          "Raw type should be Flow.Publisher");
    }
  }

  // ========================================================================
  // Package and Visibility Tests
  // ========================================================================

  @Nested
  @DisplayName("Package and Visibility Tests")
  class PackageAndVisibilityTests {

    @Test
    @DisplayName("ComponentStream should be in ai.tegmentum.wasmtime4j package")
    void shouldBeInCorrectPackage() {
      String packageName = ComponentStream.class.getPackage().getName();
      assertEquals(
          "ai.tegmentum.wasmtime4j", packageName, "Package should be ai.tegmentum.wasmtime4j");
    }

    @Test
    @DisplayName("Direction enum should be public")
    void directionEnumShouldBePublic() {
      assertTrue(
          Modifier.isPublic(ComponentStream.Direction.class.getModifiers()),
          "Direction enum should be public");
    }

    @Test
    @DisplayName("State enum should be public")
    void stateEnumShouldBePublic() {
      assertTrue(
          Modifier.isPublic(ComponentStream.State.class.getModifiers()),
          "State enum should be public");
    }

    @Test
    @DisplayName("Direction enum should be static")
    void directionEnumShouldBeStatic() {
      assertTrue(
          Modifier.isStatic(ComponentStream.Direction.class.getModifiers()),
          "Direction enum should be static");
    }

    @Test
    @DisplayName("State enum should be static")
    void stateEnumShouldBeStatic() {
      assertTrue(
          Modifier.isStatic(ComponentStream.State.class.getModifiers()),
          "State enum should be static");
    }
  }

  // ========================================================================
  // Method Count Tests
  // ========================================================================

  @Nested
  @DisplayName("Method Count Tests")
  class MethodCountTests {

    @Test
    @DisplayName("ComponentStream should have expected number of declared methods")
    void shouldHaveExpectedMethodCount() {
      Method[] methods = ComponentStream.class.getDeclaredMethods();
      // Count abstract + default methods
      // Abstract: getElementType, getDirection, read, readBlocking, write(List), writeBlocking,
      //           end, isEnded, available, writeCapacity, subscribe, isOpen, getState,
      //           iterator, toPublisher, close
      // Default: isReadable, isWritable, readOne, write(T)
      // Total should be around 20 methods
      assertTrue(
          methods.length >= 15 && methods.length <= 25,
          "ComponentStream should have between 15 and 25 declared methods, found: "
              + methods.length);
    }

    @Test
    @DisplayName("Should have at least 4 default methods")
    void shouldHaveDefaultMethods() {
      Method[] methods = ComponentStream.class.getDeclaredMethods();
      long defaultMethodCount = Arrays.stream(methods).filter(Method::isDefault).count();
      assertTrue(
          defaultMethodCount >= 4,
          "Should have at least 4 default methods, found: " + defaultMethodCount);
    }
  }
}
