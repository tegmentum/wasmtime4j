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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.Closeable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive test suite for the InstancePre interface.
 *
 * <p>InstancePre represents a pre-instantiated WebAssembly module optimized for fast instantiation.
 * This test verifies the interface structure and method signatures.
 */
@DisplayName("InstancePre Interface Tests")
class InstancePreTest {

  // ========================================================================
  // Type Definition Tests
  // ========================================================================

  @Nested
  @DisplayName("Type Definition Tests")
  class TypeDefinitionTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeAnInterface() {
      assertTrue(InstancePre.class.isInterface(), "InstancePre should be an interface");
    }

    @Test
    @DisplayName("should be public")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(InstancePre.class.getModifiers()), "InstancePre should be public");
    }

    @Test
    @DisplayName("should extend Closeable")
    void shouldExtendCloseable() {
      assertTrue(
          Closeable.class.isAssignableFrom(InstancePre.class),
          "InstancePre should extend Closeable");
    }
  }

  // ========================================================================
  // Instantiate Methods Tests
  // ========================================================================

  @Nested
  @DisplayName("Instantiate Methods Tests")
  class InstantiateMethodsTests {

    @Test
    @DisplayName("should have instantiate method with Store parameter")
    void shouldHaveInstantiateMethodWithStore() throws NoSuchMethodException {
      final Method method = InstancePre.class.getMethod("instantiate", Store.class);
      assertNotNull(method, "instantiate(Store) method should exist");
      assertEquals(Instance.class, method.getReturnType(), "instantiate should return Instance");
    }

    @Test
    @DisplayName("should have instantiate method with Store and ImportMap parameters")
    void shouldHaveInstantiateMethodWithStoreAndImportMap() throws NoSuchMethodException {
      final Method method =
          InstancePre.class.getMethod("instantiate", Store.class, ImportMap.class);
      assertNotNull(method, "instantiate(Store, ImportMap) method should exist");
      assertEquals(Instance.class, method.getReturnType(), "instantiate should return Instance");
    }

    @Test
    @DisplayName("instantiate methods should declare WasmException")
    void instantiateMethodsShouldDeclareWasmException() throws NoSuchMethodException {
      final Method method1 = InstancePre.class.getMethod("instantiate", Store.class);
      Class<?>[] exceptions1 = method1.getExceptionTypes();
      assertTrue(exceptions1.length >= 1, "instantiate(Store) should declare exceptions");
      assertEquals(
          "ai.tegmentum.wasmtime4j.exception.WasmException",
          exceptions1[0].getName(),
          "instantiate(Store) should declare WasmException");

      final Method method2 =
          InstancePre.class.getMethod("instantiate", Store.class, ImportMap.class);
      Class<?>[] exceptions2 = method2.getExceptionTypes();
      assertTrue(
          exceptions2.length >= 1, "instantiate(Store, ImportMap) should declare exceptions");
      assertEquals(
          "ai.tegmentum.wasmtime4j.exception.WasmException",
          exceptions2[0].getName(),
          "instantiate(Store, ImportMap) should declare WasmException");
    }
  }

  // ========================================================================
  // Async Instantiate Methods Tests
  // ========================================================================

  @Nested
  @DisplayName("Async Instantiate Methods Tests")
  class AsyncInstantiateMethodsTests {

    @Test
    @DisplayName("should have instantiateAsync method with Store parameter")
    void shouldHaveInstantiateAsyncMethodWithStore() throws NoSuchMethodException {
      final Method method = InstancePre.class.getMethod("instantiateAsync", Store.class);
      assertNotNull(method, "instantiateAsync(Store) method should exist");
      assertEquals(
          CompletableFuture.class,
          method.getReturnType(),
          "instantiateAsync should return CompletableFuture");
    }

    @Test
    @DisplayName("should have instantiateAsync method with Store and ImportMap parameters")
    void shouldHaveInstantiateAsyncMethodWithStoreAndImportMap() throws NoSuchMethodException {
      final Method method =
          InstancePre.class.getMethod("instantiateAsync", Store.class, ImportMap.class);
      assertNotNull(method, "instantiateAsync(Store, ImportMap) method should exist");
      assertEquals(
          CompletableFuture.class,
          method.getReturnType(),
          "instantiateAsync should return CompletableFuture");
    }

    @Test
    @DisplayName("instantiateAsync should return CompletableFuture of Instance")
    void instantiateAsyncShouldReturnCompletableFutureOfInstance() throws NoSuchMethodException {
      final Method method = InstancePre.class.getMethod("instantiateAsync", Store.class);
      Type genericReturnType = method.getGenericReturnType();
      assertTrue(
          genericReturnType instanceof ParameterizedType,
          "instantiateAsync return type should be parameterized");
      ParameterizedType paramType = (ParameterizedType) genericReturnType;
      assertEquals(
          CompletableFuture.class, paramType.getRawType(), "Raw type should be CompletableFuture");
      Type[] typeArgs = paramType.getActualTypeArguments();
      assertEquals(1, typeArgs.length, "Should have one type argument");
      assertEquals(Instance.class, typeArgs[0], "Type argument should be Instance");
    }
  }

  // ========================================================================
  // Accessor Methods Tests
  // ========================================================================

  @Nested
  @DisplayName("Accessor Methods Tests")
  class AccessorMethodsTests {

    @Test
    @DisplayName("should have getModule method")
    void shouldHaveGetModuleMethod() throws NoSuchMethodException {
      final Method method = InstancePre.class.getMethod("getModule");
      assertNotNull(method, "getModule method should exist");
      assertEquals(Module.class, method.getReturnType(), "getModule should return Module");
    }

    @Test
    @DisplayName("should have getEngine method")
    void shouldHaveGetEngineMethod() throws NoSuchMethodException {
      final Method method = InstancePre.class.getMethod("getEngine");
      assertNotNull(method, "getEngine method should exist");
      assertEquals(Engine.class, method.getReturnType(), "getEngine should return Engine");
    }

    @Test
    @DisplayName("should have isValid method")
    void shouldHaveIsValidMethod() throws NoSuchMethodException {
      final Method method = InstancePre.class.getMethod("isValid");
      assertNotNull(method, "isValid method should exist");
      assertEquals(boolean.class, method.getReturnType(), "isValid should return boolean");
    }

    @Test
    @DisplayName("should have getInstanceCount method")
    void shouldHaveGetInstanceCountMethod() throws NoSuchMethodException {
      final Method method = InstancePre.class.getMethod("getInstanceCount");
      assertNotNull(method, "getInstanceCount method should exist");
      assertEquals(long.class, method.getReturnType(), "getInstanceCount should return long");
    }

    @Test
    @DisplayName("should have getStatistics method")
    void shouldHaveGetStatisticsMethod() throws NoSuchMethodException {
      final Method method = InstancePre.class.getMethod("getStatistics");
      assertNotNull(method, "getStatistics method should exist");
      assertEquals(
          PreInstantiationStatistics.class,
          method.getReturnType(),
          "getStatistics should return PreInstantiationStatistics");
    }
  }

  // ========================================================================
  // Closeable Methods Tests
  // ========================================================================

  @Nested
  @DisplayName("Closeable Methods Tests")
  class CloseableMethodsTests {

    @Test
    @DisplayName("should have close method from Closeable")
    void shouldHaveCloseMethod() throws NoSuchMethodException {
      final Method method = InstancePre.class.getMethod("close");
      assertNotNull(method, "close method should exist");
      assertEquals(void.class, method.getReturnType(), "close should return void");
    }

    @Test
    @DisplayName("close method should not declare checked exceptions")
    void closeMethodShouldNotDeclareCheckedExceptions() throws NoSuchMethodException {
      final Method method = InstancePre.class.getMethod("close");
      Class<?>[] exceptions = method.getExceptionTypes();
      // InstancePre.close() overrides Closeable.close() with no exceptions
      assertEquals(0, exceptions.length, "close should not declare exceptions");
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
              "instantiate",
              "instantiateAsync",
              "getModule",
              "getEngine",
              "isValid",
              "getInstanceCount",
              "getStatistics",
              "close");

      Set<String> actualMethods =
          Arrays.stream(InstancePre.class.getDeclaredMethods())
              .map(Method::getName)
              .collect(Collectors.toSet());

      for (String expected : expectedMethods) {
        assertTrue(actualMethods.contains(expected), "InstancePre should have method: " + expected);
      }
    }

    @Test
    @DisplayName("should have correct number of declared methods")
    void shouldHaveCorrectNumberOfDeclaredMethods() {
      long methodCount = InstancePre.class.getDeclaredMethods().length;
      // instantiate(2), instantiateAsync(2), getModule, getEngine, isValid,
      // getInstanceCount, getStatistics, close = 10
      assertTrue(
          methodCount >= 10,
          "InstancePre should have at least 10 declared methods, found: " + methodCount);
    }
  }

  // ========================================================================
  // Method Signature Tests
  // ========================================================================

  @Nested
  @DisplayName("Method Signature Tests")
  class MethodSignatureTests {

    @Test
    @DisplayName("instantiate should have correct parameter count")
    void instantiateShouldHaveCorrectParameterCount() throws NoSuchMethodException {
      final Method method1 = InstancePre.class.getMethod("instantiate", Store.class);
      assertEquals(1, method1.getParameterCount(), "instantiate(Store) should have 1 parameter");

      final Method method2 =
          InstancePre.class.getMethod("instantiate", Store.class, ImportMap.class);
      assertEquals(
          2, method2.getParameterCount(), "instantiate(Store, ImportMap) should have 2 parameters");
    }

    @Test
    @DisplayName("instantiateAsync should have correct parameter count")
    void instantiateAsyncShouldHaveCorrectParameterCount() throws NoSuchMethodException {
      final Method method1 = InstancePre.class.getMethod("instantiateAsync", Store.class);
      assertEquals(
          1, method1.getParameterCount(), "instantiateAsync(Store) should have 1 parameter");

      final Method method2 =
          InstancePre.class.getMethod("instantiateAsync", Store.class, ImportMap.class);
      assertEquals(
          2,
          method2.getParameterCount(),
          "instantiateAsync(Store, ImportMap) should have 2 parameters");
    }

    @Test
    @DisplayName("accessor methods should have no parameters")
    void accessorMethodsShouldHaveNoParameters() throws NoSuchMethodException {
      assertEquals(
          0,
          InstancePre.class.getMethod("getModule").getParameterCount(),
          "getModule should have no parameters");
      assertEquals(
          0,
          InstancePre.class.getMethod("getEngine").getParameterCount(),
          "getEngine should have no parameters");
      assertEquals(
          0,
          InstancePre.class.getMethod("isValid").getParameterCount(),
          "isValid should have no parameters");
      assertEquals(
          0,
          InstancePre.class.getMethod("getInstanceCount").getParameterCount(),
          "getInstanceCount should have no parameters");
      assertEquals(
          0,
          InstancePre.class.getMethod("getStatistics").getParameterCount(),
          "getStatistics should have no parameters");
    }
  }

  // ========================================================================
  // Inheritance Tests
  // ========================================================================

  @Nested
  @DisplayName("Inheritance Tests")
  class InheritanceTests {

    @Test
    @DisplayName("should have Closeable as direct superinterface")
    void shouldHaveCloseableAsDirectSuperinterface() {
      Class<?>[] interfaces = InstancePre.class.getInterfaces();
      boolean extendsCloseable = Arrays.asList(interfaces).contains(Closeable.class);
      assertTrue(extendsCloseable, "InstancePre should directly extend Closeable");
    }

    @Test
    @DisplayName("should have exactly one superinterface")
    void shouldHaveExactlyOneSuperinterface() {
      Class<?>[] interfaces = InstancePre.class.getInterfaces();
      assertEquals(1, interfaces.length, "InstancePre should have exactly one superinterface");
    }
  }

  // ========================================================================
  // Abstract Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Abstract Method Tests")
  class AbstractMethodTests {

    @Test
    @DisplayName("all methods should be abstract (interface methods)")
    void allMethodsShouldBeAbstract() {
      for (Method method : InstancePre.class.getDeclaredMethods()) {
        // Interface methods are implicitly abstract unless they are default or static
        if (!method.isDefault() && !Modifier.isStatic(method.getModifiers())) {
          assertTrue(
              Modifier.isAbstract(method.getModifiers()),
              "Method " + method.getName() + " should be abstract");
        }
      }
    }

    @Test
    @DisplayName("should have no default methods")
    void shouldHaveNoDefaultMethods() {
      long defaultMethodCount =
          Arrays.stream(InstancePre.class.getDeclaredMethods()).filter(Method::isDefault).count();
      assertEquals(0, defaultMethodCount, "InstancePre should have no default methods");
    }

    @Test
    @DisplayName("should have no static methods")
    void shouldHaveNoStaticMethods() {
      long staticMethodCount =
          Arrays.stream(InstancePre.class.getDeclaredMethods())
              .filter(m -> Modifier.isStatic(m.getModifiers()))
              .count();
      assertEquals(0, staticMethodCount, "InstancePre should have no static methods");
    }
  }

  // ========================================================================
  // Return Type Tests
  // ========================================================================

  @Nested
  @DisplayName("Return Type Tests")
  class ReturnTypeTests {

    @Test
    @DisplayName("synchronous instantiate methods should return Instance")
    void synchronousInstantiateMethodsShouldReturnInstance() throws NoSuchMethodException {
      assertEquals(
          Instance.class,
          InstancePre.class.getMethod("instantiate", Store.class).getReturnType(),
          "instantiate(Store) should return Instance");
      assertEquals(
          Instance.class,
          InstancePre.class.getMethod("instantiate", Store.class, ImportMap.class).getReturnType(),
          "instantiate(Store, ImportMap) should return Instance");
    }

    @Test
    @DisplayName("async instantiate methods should return CompletableFuture")
    void asyncInstantiateMethodsShouldReturnCompletableFuture() throws NoSuchMethodException {
      assertEquals(
          CompletableFuture.class,
          InstancePre.class.getMethod("instantiateAsync", Store.class).getReturnType(),
          "instantiateAsync(Store) should return CompletableFuture");
      assertEquals(
          CompletableFuture.class,
          InstancePre.class
              .getMethod("instantiateAsync", Store.class, ImportMap.class)
              .getReturnType(),
          "instantiateAsync(Store, ImportMap) should return CompletableFuture");
    }
  }
}
