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

package ai.tegmentum.wasmtime4j.wasi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.io.Closeable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the {@link WasiResource} interface.
 *
 * <p>This test class verifies the interface structure and method signatures for the WASI resource
 * API using reflection-based testing.
 */
@DisplayName("WasiResource Interface Tests")
class WasiResourceTest {

  // ========================================================================
  // Interface Structure Tests
  // ========================================================================

  @Nested
  @DisplayName("Interface Structure Tests")
  class InterfaceStructureTests {

    @Test
    @DisplayName("WasiResource should be an interface")
    void shouldBeAnInterface() {
      assertTrue(WasiResource.class.isInterface(), "WasiResource should be an interface");
    }

    @Test
    @DisplayName("WasiResource should be a public interface")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(WasiResource.class.getModifiers()), "WasiResource should be public");
    }

    @Test
    @DisplayName("WasiResource should extend Closeable")
    void shouldExtendCloseable() {
      Class<?>[] interfaces = WasiResource.class.getInterfaces();
      assertEquals(1, interfaces.length, "WasiResource should extend 1 interface");
      assertEquals(Closeable.class, interfaces[0], "WasiResource should extend Closeable");
    }
  }

  // ========================================================================
  // Identification Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Identification Method Tests")
  class IdentificationMethodTests {

    @Test
    @DisplayName("should have getId method returning long")
    void shouldHaveGetIdMethod() throws NoSuchMethodException {
      Method method = WasiResource.class.getMethod("getId");
      assertNotNull(method, "getId method should exist");
      assertEquals(long.class, method.getReturnType(), "Return type should be long");
      assertEquals(0, method.getParameterCount(), "getId should have no parameters");
    }

    @Test
    @DisplayName("should have getType method returning String")
    void shouldHaveGetTypeMethod() throws NoSuchMethodException {
      Method method = WasiResource.class.getMethod("getType");
      assertNotNull(method, "getType method should exist");
      assertEquals(String.class, method.getReturnType(), "Return type should be String");
      assertEquals(0, method.getParameterCount(), "getType should have no parameters");
    }
  }

  // ========================================================================
  // Ownership Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Ownership Method Tests")
  class OwnershipMethodTests {

    @Test
    @DisplayName("should have getOwner method returning WasiInstance")
    void shouldHaveGetOwnerMethod() throws NoSuchMethodException {
      Method method = WasiResource.class.getMethod("getOwner");
      assertNotNull(method, "getOwner method should exist");
      assertEquals(
          WasiInstance.class, method.getReturnType(), "Return type should be WasiInstance");
      assertEquals(0, method.getParameterCount(), "getOwner should have no parameters");
    }

    @Test
    @DisplayName("should have isOwned method returning boolean")
    void shouldHaveIsOwnedMethod() throws NoSuchMethodException {
      Method method = WasiResource.class.getMethod("isOwned");
      assertNotNull(method, "isOwned method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Return type should be boolean");
      assertEquals(0, method.getParameterCount(), "isOwned should have no parameters");
    }

    @Test
    @DisplayName("should have transferOwnership method with WasiInstance parameter")
    void shouldHaveTransferOwnershipMethod() throws NoSuchMethodException {
      Method method = WasiResource.class.getMethod("transferOwnership", WasiInstance.class);
      assertNotNull(method, "transferOwnership method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
      assertEquals(1, method.getParameterCount(), "transferOwnership should have 1 parameter");
      assertEquals(
          WasiInstance.class, method.getParameterTypes()[0], "Parameter should be WasiInstance");

      Class<?>[] exceptionTypes = method.getExceptionTypes();
      assertEquals(1, exceptionTypes.length, "transferOwnership should throw 1 exception");
      assertEquals(WasmException.class, exceptionTypes[0], "Should throw WasmException");
    }
  }

  // ========================================================================
  // State Method Tests
  // ========================================================================

  @Nested
  @DisplayName("State Method Tests")
  class StateMethodTests {

    @Test
    @DisplayName("should have isValid method returning boolean")
    void shouldHaveIsValidMethod() throws NoSuchMethodException {
      Method method = WasiResource.class.getMethod("isValid");
      assertNotNull(method, "isValid method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Return type should be boolean");
      assertEquals(0, method.getParameterCount(), "isValid should have no parameters");
    }

    @Test
    @DisplayName("should have getState method returning WasiResourceState")
    void shouldHaveGetStateMethod() throws NoSuchMethodException {
      Method method = WasiResource.class.getMethod("getState");
      assertNotNull(method, "getState method should exist");
      assertEquals(
          WasiResourceState.class,
          method.getReturnType(),
          "Return type should be WasiResourceState");
      assertEquals(0, method.getParameterCount(), "getState should have no parameters");

      Class<?>[] exceptionTypes = method.getExceptionTypes();
      assertEquals(1, exceptionTypes.length, "getState should throw 1 exception");
      assertEquals(WasmException.class, exceptionTypes[0], "Should throw WasmException");
    }
  }

  // ========================================================================
  // Timestamp Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Timestamp Method Tests")
  class TimestampMethodTests {

    @Test
    @DisplayName("should have getCreatedAt method returning Instant")
    void shouldHaveGetCreatedAtMethod() throws NoSuchMethodException {
      Method method = WasiResource.class.getMethod("getCreatedAt");
      assertNotNull(method, "getCreatedAt method should exist");
      assertEquals(Instant.class, method.getReturnType(), "Return type should be Instant");
      assertEquals(0, method.getParameterCount(), "getCreatedAt should have no parameters");
    }

    @Test
    @DisplayName("should have getLastAccessedAt method returning Optional<Instant>")
    void shouldHaveGetLastAccessedAtMethod() throws NoSuchMethodException {
      Method method = WasiResource.class.getMethod("getLastAccessedAt");
      assertNotNull(method, "getLastAccessedAt method should exist");
      assertEquals(Optional.class, method.getReturnType(), "Return type should be Optional");
      assertEquals(0, method.getParameterCount(), "getLastAccessedAt should have no parameters");

      // Verify generic type
      Type genericReturnType = method.getGenericReturnType();
      assertTrue(
          genericReturnType instanceof ParameterizedType, "Return type should be parameterized");
      ParameterizedType parameterizedType = (ParameterizedType) genericReturnType;
      assertEquals(
          Instant.class,
          parameterizedType.getActualTypeArguments()[0],
          "Optional should contain Instant");
    }
  }

  // ========================================================================
  // Metadata Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Metadata Method Tests")
  class MetadataMethodTests {

    @Test
    @DisplayName("should have getMetadata method returning WasiResourceMetadata")
    void shouldHaveGetMetadataMethod() throws NoSuchMethodException {
      Method method = WasiResource.class.getMethod("getMetadata");
      assertNotNull(method, "getMetadata method should exist");
      assertEquals(
          WasiResourceMetadata.class,
          method.getReturnType(),
          "Return type should be WasiResourceMetadata");
      assertEquals(0, method.getParameterCount(), "getMetadata should have no parameters");

      Class<?>[] exceptionTypes = method.getExceptionTypes();
      assertEquals(1, exceptionTypes.length, "getMetadata should throw 1 exception");
      assertEquals(WasmException.class, exceptionTypes[0], "Should throw WasmException");
    }

    @Test
    @DisplayName("should have getStats method returning WasiResourceStats")
    void shouldHaveGetStatsMethod() throws NoSuchMethodException {
      Method method = WasiResource.class.getMethod("getStats");
      assertNotNull(method, "getStats method should exist");
      assertEquals(
          WasiResourceStats.class,
          method.getReturnType(),
          "Return type should be WasiResourceStats");
      assertEquals(0, method.getParameterCount(), "getStats should have no parameters");
    }
  }

  // ========================================================================
  // Operations Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Operations Method Tests")
  class OperationsMethodTests {

    @Test
    @DisplayName("should have invoke method with String and Object[] parameters")
    void shouldHaveInvokeMethod() throws NoSuchMethodException {
      Method method = WasiResource.class.getMethod("invoke", String.class, Object[].class);
      assertNotNull(method, "invoke method should exist");
      assertEquals(Object.class, method.getReturnType(), "Return type should be Object");
      assertEquals(2, method.getParameterCount(), "invoke should have 2 parameters");
      assertEquals(String.class, method.getParameterTypes()[0], "First parameter should be String");
      assertEquals(
          Object[].class, method.getParameterTypes()[1], "Second parameter should be Object[]");

      Class<?>[] exceptionTypes = method.getExceptionTypes();
      assertEquals(1, exceptionTypes.length, "invoke should throw 1 exception");
      assertEquals(WasmException.class, exceptionTypes[0], "Should throw WasmException");
    }

    @Test
    @DisplayName("should have getAvailableOperations method returning List<String>")
    void shouldHaveGetAvailableOperationsMethod() throws NoSuchMethodException {
      Method method = WasiResource.class.getMethod("getAvailableOperations");
      assertNotNull(method, "getAvailableOperations method should exist");
      assertEquals(List.class, method.getReturnType(), "Return type should be List");
      assertEquals(
          0, method.getParameterCount(), "getAvailableOperations should have no parameters");

      // Verify generic type
      Type genericReturnType = method.getGenericReturnType();
      assertTrue(
          genericReturnType instanceof ParameterizedType, "Return type should be parameterized");
      ParameterizedType parameterizedType = (ParameterizedType) genericReturnType;
      assertEquals(
          String.class,
          parameterizedType.getActualTypeArguments()[0],
          "List should contain String");
    }
  }

  // ========================================================================
  // Handle Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Handle Method Tests")
  class HandleMethodTests {

    @Test
    @DisplayName("should have createHandle method returning WasiResourceHandle")
    void shouldHaveCreateHandleMethod() throws NoSuchMethodException {
      Method method = WasiResource.class.getMethod("createHandle");
      assertNotNull(method, "createHandle method should exist");
      assertEquals(
          WasiResourceHandle.class,
          method.getReturnType(),
          "Return type should be WasiResourceHandle");
      assertEquals(0, method.getParameterCount(), "createHandle should have no parameters");

      Class<?>[] exceptionTypes = method.getExceptionTypes();
      assertEquals(1, exceptionTypes.length, "createHandle should throw 1 exception");
      assertEquals(WasmException.class, exceptionTypes[0], "Should throw WasmException");
    }
  }

  // ========================================================================
  // Close Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Close Method Tests")
  class CloseMethodTests {

    @Test
    @DisplayName("should have close method")
    void shouldHaveCloseMethod() throws NoSuchMethodException {
      Method method = WasiResource.class.getMethod("close");
      assertNotNull(method, "close method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
      assertEquals(0, method.getParameterCount(), "close should have no parameters");
    }
  }

  // ========================================================================
  // Method Count Tests
  // ========================================================================

  @Nested
  @DisplayName("Method Count Tests")
  class MethodCountTests {

    @Test
    @DisplayName("WasiResource should have exactly 15 declared methods")
    void shouldHaveExactMethodCount() {
      Method[] methods = WasiResource.class.getDeclaredMethods();
      assertEquals(15, methods.length, "WasiResource should have exactly 15 methods");
    }
  }

  // ========================================================================
  // Method Signature Verification Tests
  // ========================================================================

  @Nested
  @DisplayName("Method Signature Verification Tests")
  class MethodSignatureVerificationTests {

    @Test
    @DisplayName("All WasiResource methods should be public")
    void allMethodsShouldBePublic() {
      Method[] methods = WasiResource.class.getDeclaredMethods();

      for (Method method : methods) {
        int modifiers = method.getModifiers();
        assertTrue(
            Modifier.isPublic(modifiers), "Method " + method.getName() + " should be public");
      }
    }

    @Test
    @DisplayName("All WasiResource methods should be abstract")
    void allMethodsShouldBeAbstract() {
      Method[] methods = WasiResource.class.getDeclaredMethods();

      for (Method method : methods) {
        int modifiers = method.getModifiers();
        assertTrue(
            Modifier.isAbstract(modifiers), "Method " + method.getName() + " should be abstract");
      }
    }
  }

  // ========================================================================
  // Exception Verification Tests
  // ========================================================================

  @Nested
  @DisplayName("Exception Verification Tests")
  class ExceptionVerificationTests {

    @Test
    @DisplayName("Methods that can fail should throw WasmException")
    void methodsThatCanFailShouldThrowWasmException() throws NoSuchMethodException {
      // Methods that should throw WasmException
      String[] methodNames = {
        "getMetadata", "getState", "invoke", "createHandle", "transferOwnership"
      };

      for (String methodName : methodNames) {
        Method method = null;
        for (Method m : WasiResource.class.getDeclaredMethods()) {
          if (m.getName().equals(methodName)) {
            method = m;
            break;
          }
        }
        assertNotNull(method, methodName + " method should exist");
        Class<?>[] exceptionTypes = method.getExceptionTypes();
        assertTrue(exceptionTypes.length > 0, "Method " + methodName + " should throw exceptions");
        assertEquals(
            WasmException.class,
            exceptionTypes[0],
            "Method " + methodName + " should throw WasmException");
      }
    }
  }
}
