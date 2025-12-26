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

package ai.tegmentum.wasmtime4j.execution;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ResourceLimiterAsync} interface.
 *
 * <p>ResourceLimiterAsync provides asynchronous resource limiting for WebAssembly execution.
 */
@DisplayName("ResourceLimiterAsync Tests")
class ResourceLimiterAsyncTest {

  @Nested
  @DisplayName("Interface Structure Tests")
  class InterfaceStructureTests {

    @Test
    @DisplayName("should be public interface")
    void shouldBePublicInterface() {
      assertTrue(
          Modifier.isPublic(ResourceLimiterAsync.class.getModifiers()),
          "ResourceLimiterAsync should be public");
      assertTrue(
          ResourceLimiterAsync.class.isInterface(), "ResourceLimiterAsync should be an interface");
    }

    @Test
    @DisplayName("should extend AutoCloseable")
    void shouldExtendAutoCloseable() {
      assertTrue(
          AutoCloseable.class.isAssignableFrom(ResourceLimiterAsync.class),
          "ResourceLimiterAsync should extend AutoCloseable");
    }
  }

  @Nested
  @DisplayName("Async Method Definition Tests")
  class AsyncMethodDefinitionTests {

    @Test
    @DisplayName("should have getId method")
    void shouldHaveGetIdMethod() throws NoSuchMethodException {
      final Method method = ResourceLimiterAsync.class.getMethod("getId");
      assertNotNull(method, "getId method should exist");
      assertEquals(long.class, method.getReturnType(), "getId should return long");
    }

    @Test
    @DisplayName("should have getConfigAsync method")
    void shouldHaveGetConfigAsyncMethod() throws NoSuchMethodException {
      final Method method = ResourceLimiterAsync.class.getMethod("getConfigAsync");
      assertNotNull(method, "getConfigAsync method should exist");
      assertEquals(
          CompletableFuture.class,
          method.getReturnType(),
          "getConfigAsync should return CompletableFuture");
    }

    @Test
    @DisplayName("should have allowMemoryGrowAsync method")
    void shouldHaveAllowMemoryGrowAsyncMethod() throws NoSuchMethodException {
      final Method method =
          ResourceLimiterAsync.class.getMethod("allowMemoryGrowAsync", long.class, long.class);
      assertNotNull(method, "allowMemoryGrowAsync method should exist");
      assertEquals(
          CompletableFuture.class,
          method.getReturnType(),
          "allowMemoryGrowAsync should return CompletableFuture");
    }

    @Test
    @DisplayName("should have allowTableGrowAsync method")
    void shouldHaveAllowTableGrowAsyncMethod() throws NoSuchMethodException {
      final Method method =
          ResourceLimiterAsync.class.getMethod("allowTableGrowAsync", long.class, long.class);
      assertNotNull(method, "allowTableGrowAsync method should exist");
      assertEquals(
          CompletableFuture.class,
          method.getReturnType(),
          "allowTableGrowAsync should return CompletableFuture");
    }

    @Test
    @DisplayName("should have getStatsAsync method")
    void shouldHaveGetStatsAsyncMethod() throws NoSuchMethodException {
      final Method method = ResourceLimiterAsync.class.getMethod("getStatsAsync");
      assertNotNull(method, "getStatsAsync method should exist");
      assertEquals(
          CompletableFuture.class,
          method.getReturnType(),
          "getStatsAsync should return CompletableFuture");
    }

    @Test
    @DisplayName("should have resetStatsAsync method")
    void shouldHaveResetStatsAsyncMethod() throws NoSuchMethodException {
      final Method method = ResourceLimiterAsync.class.getMethod("resetStatsAsync");
      assertNotNull(method, "resetStatsAsync method should exist");
      assertEquals(
          CompletableFuture.class,
          method.getReturnType(),
          "resetStatsAsync should return CompletableFuture");
    }
  }

  @Nested
  @DisplayName("Sync Wrapper Method Tests")
  class SyncWrapperMethodTests {

    @Test
    @DisplayName("should have allowMemoryGrow default method")
    void shouldHaveAllowMemoryGrowDefaultMethod() throws NoSuchMethodException {
      final Method method =
          ResourceLimiterAsync.class.getMethod("allowMemoryGrow", long.class, long.class);
      assertNotNull(method, "allowMemoryGrow method should exist");
      assertEquals(boolean.class, method.getReturnType(), "allowMemoryGrow should return boolean");
      assertTrue(method.isDefault(), "allowMemoryGrow should be a default method");
    }

    @Test
    @DisplayName("should have allowTableGrow default method")
    void shouldHaveAllowTableGrowDefaultMethod() throws NoSuchMethodException {
      final Method method =
          ResourceLimiterAsync.class.getMethod("allowTableGrow", long.class, long.class);
      assertNotNull(method, "allowTableGrow method should exist");
      assertEquals(boolean.class, method.getReturnType(), "allowTableGrow should return boolean");
      assertTrue(method.isDefault(), "allowTableGrow should be a default method");
    }

    @Test
    @DisplayName("allowMemoryGrow should declare WasmException")
    void allowMemoryGrowShouldDeclareWasmException() throws NoSuchMethodException {
      final Method method =
          ResourceLimiterAsync.class.getMethod("allowMemoryGrow", long.class, long.class);
      final Class<?>[] exceptions = method.getExceptionTypes();
      assertTrue(exceptions.length > 0, "allowMemoryGrow should declare exceptions");
      assertEquals(
          WasmException.class, exceptions[0], "allowMemoryGrow should declare WasmException");
    }

    @Test
    @DisplayName("allowTableGrow should declare WasmException")
    void allowTableGrowShouldDeclareWasmException() throws NoSuchMethodException {
      final Method method =
          ResourceLimiterAsync.class.getMethod("allowTableGrow", long.class, long.class);
      final Class<?>[] exceptions = method.getExceptionTypes();
      assertTrue(exceptions.length > 0, "allowTableGrow should declare exceptions");
      assertEquals(
          WasmException.class, exceptions[0], "allowTableGrow should declare WasmException");
    }
  }

  @Nested
  @DisplayName("Static Factory Method Tests")
  class StaticFactoryMethodTests {

    @Test
    @DisplayName("should have fromSync static method")
    void shouldHaveFromSyncStaticMethod() throws NoSuchMethodException {
      final Method method = ResourceLimiterAsync.class.getMethod("fromSync", ResourceLimiter.class);
      assertNotNull(method, "fromSync method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "fromSync should be static");
      assertEquals(
          ResourceLimiterAsync.class,
          method.getReturnType(),
          "fromSync should return ResourceLimiterAsync");
    }

    @Test
    @DisplayName("fromSync should have one ResourceLimiter parameter")
    void fromSyncShouldHaveOneLimiterParameter() throws NoSuchMethodException {
      final Method method = ResourceLimiterAsync.class.getMethod("fromSync", ResourceLimiter.class);
      assertEquals(1, method.getParameterCount(), "fromSync should have 1 parameter");
      assertEquals(
          ResourceLimiter.class,
          method.getParameterTypes()[0],
          "Parameter should be ResourceLimiter");
    }
  }

  @Nested
  @DisplayName("Generic Return Type Tests")
  class GenericReturnTypeTests {

    @Test
    @DisplayName("getConfigAsync should return CompletableFuture<ResourceLimiterConfig>")
    void getConfigAsyncShouldReturnCorrectGenericType() throws NoSuchMethodException {
      final Method method = ResourceLimiterAsync.class.getMethod("getConfigAsync");
      final Type genericReturnType = method.getGenericReturnType();
      assertTrue(
          genericReturnType instanceof ParameterizedType, "Return type should be parameterized");
      final ParameterizedType pt = (ParameterizedType) genericReturnType;
      assertEquals(
          CompletableFuture.class, pt.getRawType(), "Raw type should be CompletableFuture");
      assertEquals(
          ResourceLimiterConfig.class,
          pt.getActualTypeArguments()[0],
          "Type argument should be ResourceLimiterConfig");
    }

    @Test
    @DisplayName("allowMemoryGrowAsync should return CompletableFuture<Boolean>")
    void allowMemoryGrowAsyncShouldReturnCorrectGenericType() throws NoSuchMethodException {
      final Method method =
          ResourceLimiterAsync.class.getMethod("allowMemoryGrowAsync", long.class, long.class);
      final Type genericReturnType = method.getGenericReturnType();
      assertTrue(
          genericReturnType instanceof ParameterizedType, "Return type should be parameterized");
      final ParameterizedType pt = (ParameterizedType) genericReturnType;
      assertEquals(
          CompletableFuture.class, pt.getRawType(), "Raw type should be CompletableFuture");
      assertEquals(
          Boolean.class, pt.getActualTypeArguments()[0], "Type argument should be Boolean");
    }

    @Test
    @DisplayName("allowTableGrowAsync should return CompletableFuture<Boolean>")
    void allowTableGrowAsyncShouldReturnCorrectGenericType() throws NoSuchMethodException {
      final Method method =
          ResourceLimiterAsync.class.getMethod("allowTableGrowAsync", long.class, long.class);
      final Type genericReturnType = method.getGenericReturnType();
      assertTrue(
          genericReturnType instanceof ParameterizedType, "Return type should be parameterized");
      final ParameterizedType pt = (ParameterizedType) genericReturnType;
      assertEquals(
          Boolean.class, pt.getActualTypeArguments()[0], "Type argument should be Boolean");
    }

    @Test
    @DisplayName("getStatsAsync should return CompletableFuture<ResourceLimiterStats>")
    void getStatsAsyncShouldReturnCorrectGenericType() throws NoSuchMethodException {
      final Method method = ResourceLimiterAsync.class.getMethod("getStatsAsync");
      final Type genericReturnType = method.getGenericReturnType();
      assertTrue(
          genericReturnType instanceof ParameterizedType, "Return type should be parameterized");
      final ParameterizedType pt = (ParameterizedType) genericReturnType;
      assertEquals(
          ResourceLimiterStats.class,
          pt.getActualTypeArguments()[0],
          "Type argument should be ResourceLimiterStats");
    }

    @Test
    @DisplayName("resetStatsAsync should return CompletableFuture<Void>")
    void resetStatsAsyncShouldReturnCorrectGenericType() throws NoSuchMethodException {
      final Method method = ResourceLimiterAsync.class.getMethod("resetStatsAsync");
      final Type genericReturnType = method.getGenericReturnType();
      assertTrue(
          genericReturnType instanceof ParameterizedType, "Return type should be parameterized");
      final ParameterizedType pt = (ParameterizedType) genericReturnType;
      assertEquals(Void.class, pt.getActualTypeArguments()[0], "Type argument should be Void");
    }
  }

  @Nested
  @DisplayName("Close Method Tests")
  class CloseMethodTests {

    @Test
    @DisplayName("should have close method")
    void shouldHaveCloseMethod() throws NoSuchMethodException {
      final Method method = ResourceLimiterAsync.class.getMethod("close");
      assertNotNull(method, "close method should exist");
      assertEquals(void.class, method.getReturnType(), "close should return void");
    }

    @Test
    @DisplayName("close should declare WasmException")
    void closeShouldDeclareWasmException() throws NoSuchMethodException {
      final Method method = ResourceLimiterAsync.class.getMethod("close");
      final Class<?>[] exceptions = method.getExceptionTypes();
      assertTrue(exceptions.length > 0, "close should declare exceptions");
      assertEquals(WasmException.class, exceptions[0], "close should declare WasmException");
    }
  }

  @Nested
  @DisplayName("Method Count Tests")
  class MethodCountTests {

    @Test
    @DisplayName("should have expected number of declared methods")
    void shouldHaveExpectedDeclaredMethods() {
      final Method[] methods = ResourceLimiterAsync.class.getDeclaredMethods();
      // getId, getConfigAsync, allowMemoryGrowAsync, allowTableGrowAsync,
      // getStatsAsync, resetStatsAsync, allowMemoryGrow (default),
      // allowTableGrow (default), fromSync (static), close
      assertTrue(
          methods.length >= 9,
          "ResourceLimiterAsync should have at least 9 methods, found: " + methods.length);
    }

    @Test
    @DisplayName("should have correct number of async methods")
    void shouldHaveCorrectNumberOfAsyncMethods() {
      int asyncMethodCount = 0;
      for (final Method method : ResourceLimiterAsync.class.getDeclaredMethods()) {
        if (method.getName().endsWith("Async")) {
          asyncMethodCount++;
        }
      }
      assertEquals(5, asyncMethodCount, "Should have 5 async methods");
    }
  }

  @Nested
  @DisplayName("Default Method Tests")
  class DefaultMethodTests {

    @Test
    @DisplayName("should have exactly 2 default methods")
    void shouldHave2DefaultMethods() {
      int defaultMethodCount = 0;
      for (final Method method : ResourceLimiterAsync.class.getDeclaredMethods()) {
        if (method.isDefault()) {
          defaultMethodCount++;
        }
      }
      assertEquals(2, defaultMethodCount, "Should have 2 default methods");
    }

    @Test
    @DisplayName("default methods should not be abstract")
    void defaultMethodsShouldNotBeAbstract() {
      for (final Method method : ResourceLimiterAsync.class.getDeclaredMethods()) {
        if (method.isDefault()) {
          assertFalse(
              Modifier.isAbstract(method.getModifiers()),
              "Default method " + method.getName() + " should not be abstract");
        }
      }
    }
  }

  @Nested
  @DisplayName("Parameter Type Tests")
  class ParameterTypeTests {

    @Test
    @DisplayName("allowMemoryGrowAsync parameters should be long")
    void allowMemoryGrowAsyncParametersShouldBeLong() throws NoSuchMethodException {
      final Method method =
          ResourceLimiterAsync.class.getMethod("allowMemoryGrowAsync", long.class, long.class);
      final Class<?>[] paramTypes = method.getParameterTypes();
      assertEquals(long.class, paramTypes[0], "First parameter should be long");
      assertEquals(long.class, paramTypes[1], "Second parameter should be long");
    }

    @Test
    @DisplayName("allowTableGrowAsync parameters should be long")
    void allowTableGrowAsyncParametersShouldBeLong() throws NoSuchMethodException {
      final Method method =
          ResourceLimiterAsync.class.getMethod("allowTableGrowAsync", long.class, long.class);
      final Class<?>[] paramTypes = method.getParameterTypes();
      assertEquals(long.class, paramTypes[0], "First parameter should be long");
      assertEquals(long.class, paramTypes[1], "Second parameter should be long");
    }
  }
}
