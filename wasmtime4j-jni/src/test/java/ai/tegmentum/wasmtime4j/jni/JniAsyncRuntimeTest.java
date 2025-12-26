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

package ai.tegmentum.wasmtime4j.jni;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.async.AsyncRuntime;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.function.Consumer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link JniAsyncRuntime} class.
 *
 * <p>JniAsyncRuntime provides JNI implementation of asynchronous WebAssembly execution.
 */
@DisplayName("JniAsyncRuntime Tests")
class JniAsyncRuntimeTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be public and final")
    void shouldBePublicAndFinal() {
      assertTrue(
          Modifier.isPublic(JniAsyncRuntime.class.getModifiers()), "JniAsyncRuntime should be public");
      assertTrue(
          Modifier.isFinal(JniAsyncRuntime.class.getModifiers()), "JniAsyncRuntime should be final");
    }

    @Test
    @DisplayName("should implement AsyncRuntime interface")
    void shouldImplementAsyncRuntimeInterface() {
      assertTrue(
          AsyncRuntime.class.isAssignableFrom(JniAsyncRuntime.class),
          "JniAsyncRuntime should implement AsyncRuntime");
    }

    @Test
    @DisplayName("should have default constructor")
    void shouldHaveDefaultConstructor() throws NoSuchMethodException {
      final Constructor<?> constructor = JniAsyncRuntime.class.getConstructor();
      assertNotNull(constructor, "Default constructor should exist");
      assertTrue(Modifier.isPublic(constructor.getModifiers()), "Constructor should be public");
    }
  }

  @Nested
  @DisplayName("Lifecycle Method Tests")
  class LifecycleMethodTests {

    @Test
    @DisplayName("should have initialize method")
    void shouldHaveInitializeMethod() throws NoSuchMethodException {
      final Method method = JniAsyncRuntime.class.getMethod("initialize");
      assertNotNull(method, "initialize method should exist");
      assertEquals(void.class, method.getReturnType(), "initialize should return void");
    }

    @Test
    @DisplayName("should have isInitialized method")
    void shouldHaveIsInitializedMethod() throws NoSuchMethodException {
      final Method method = JniAsyncRuntime.class.getMethod("isInitialized");
      assertNotNull(method, "isInitialized method should exist");
      assertEquals(boolean.class, method.getReturnType(), "isInitialized should return boolean");
    }

    @Test
    @DisplayName("should have getRuntimeInfo method")
    void shouldHaveGetRuntimeInfoMethod() throws NoSuchMethodException {
      final Method method = JniAsyncRuntime.class.getMethod("getRuntimeInfo");
      assertNotNull(method, "getRuntimeInfo method should exist");
      assertEquals(String.class, method.getReturnType(), "getRuntimeInfo should return String");
    }

    @Test
    @DisplayName("should have shutdown method")
    void shouldHaveShutdownMethod() throws NoSuchMethodException {
      final Method method = JniAsyncRuntime.class.getMethod("shutdown");
      assertNotNull(method, "shutdown method should exist");
      assertEquals(void.class, method.getReturnType(), "shutdown should return void");
    }

    @Test
    @DisplayName("should have close method")
    void shouldHaveCloseMethod() throws NoSuchMethodException {
      final Method method = JniAsyncRuntime.class.getMethod("close");
      assertNotNull(method, "close method should exist");
      assertEquals(void.class, method.getReturnType(), "close should return void");
    }
  }

  @Nested
  @DisplayName("Async Execution Method Tests")
  class AsyncExecutionMethodTests {

    @Test
    @DisplayName("should have executeAsync method")
    void shouldHaveExecuteAsyncMethod() throws NoSuchMethodException {
      final Method method =
          JniAsyncRuntime.class.getMethod(
              "executeAsync", long.class, String.class, Object[].class, long.class, Consumer.class);
      assertNotNull(method, "executeAsync method should exist");
      assertEquals(long.class, method.getReturnType(), "executeAsync should return long");
    }

    @Test
    @DisplayName("should have compileAsync method")
    void shouldHaveCompileAsyncMethod() throws NoSuchMethodException {
      final Method method =
          JniAsyncRuntime.class.getMethod(
              "compileAsync", byte[].class, long.class, Consumer.class, Consumer.class);
      assertNotNull(method, "compileAsync method should exist");
      assertEquals(long.class, method.getReturnType(), "compileAsync should return long");
    }
  }

  @Nested
  @DisplayName("Operation Management Method Tests")
  class OperationManagementMethodTests {

    @Test
    @DisplayName("should have cancelOperation method")
    void shouldHaveCancelOperationMethod() throws NoSuchMethodException {
      final Method method = JniAsyncRuntime.class.getMethod("cancelOperation", long.class);
      assertNotNull(method, "cancelOperation method should exist");
      assertEquals(boolean.class, method.getReturnType(), "cancelOperation should return boolean");
    }

    @Test
    @DisplayName("should have getOperationStatus method")
    void shouldHaveGetOperationStatusMethod() throws NoSuchMethodException {
      final Method method = JniAsyncRuntime.class.getMethod("getOperationStatus", long.class);
      assertNotNull(method, "getOperationStatus method should exist");
      assertEquals(
          AsyncRuntime.OperationStatus.class,
          method.getReturnType(),
          "getOperationStatus should return OperationStatus");
    }

    @Test
    @DisplayName("should have waitForOperation method")
    void shouldHaveWaitForOperationMethod() throws NoSuchMethodException {
      final Method method =
          JniAsyncRuntime.class.getMethod("waitForOperation", long.class, long.class);
      assertNotNull(method, "waitForOperation method should exist");
      assertEquals(
          AsyncRuntime.OperationStatus.class,
          method.getReturnType(),
          "waitForOperation should return OperationStatus");
    }

    @Test
    @DisplayName("should have getActiveOperationCount method")
    void shouldHaveGetActiveOperationCountMethod() throws NoSuchMethodException {
      final Method method = JniAsyncRuntime.class.getMethod("getActiveOperationCount");
      assertNotNull(method, "getActiveOperationCount method should exist");
      assertEquals(int.class, method.getReturnType(), "getActiveOperationCount should return int");
    }
  }

  @Nested
  @DisplayName("Object Method Tests")
  class ObjectMethodTests {

    @Test
    @DisplayName("should have toString method")
    void shouldHaveToStringMethod() throws NoSuchMethodException {
      final Method method = JniAsyncRuntime.class.getMethod("toString");
      assertNotNull(method, "toString method should exist");
      assertEquals(String.class, method.getReturnType(), "toString should return String");
    }
  }
}
