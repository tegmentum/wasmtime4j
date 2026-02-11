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

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.validation.ImportMap;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.InstancePre;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.validation.PreInstantiationStatistics;
import ai.tegmentum.wasmtime4j.Store;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link JniInstancePre} class.
 *
 * <p>JniInstancePre provides JNI implementation of pre-instantiated WebAssembly modules for fast
 * instantiation.
 */
@DisplayName("JniInstancePre Tests")
class JniInstancePreTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be public and final")
    void shouldBePublicAndFinal() {
      assertTrue(
          Modifier.isPublic(JniInstancePre.class.getModifiers()),
          "JniInstancePre should be public");
      assertTrue(
          Modifier.isFinal(JniInstancePre.class.getModifiers()), "JniInstancePre should be final");
    }

    @Test
    @DisplayName("should implement InstancePre interface")
    void shouldImplementInstancePreInterface() {
      assertTrue(
          InstancePre.class.isAssignableFrom(JniInstancePre.class),
          "JniInstancePre should implement InstancePre");
    }

    @Test
    @DisplayName("should have constructor with native handle, module, and engine")
    void shouldHaveConstructorWithAllParameters() throws NoSuchMethodException {
      final Constructor<?> constructor =
          JniInstancePre.class.getConstructor(long.class, Module.class, Engine.class);
      assertNotNull(constructor, "Constructor with all parameters should exist");
      assertTrue(Modifier.isPublic(constructor.getModifiers()), "Constructor should be public");
    }
  }

  @Nested
  @DisplayName("Instantiation Method Tests")
  class InstantiationMethodTests {

    @Test
    @DisplayName("should have instantiate method with store")
    void shouldHaveInstantiateMethodWithStore() throws NoSuchMethodException {
      final Method method = JniInstancePre.class.getMethod("instantiate", Store.class);
      assertNotNull(method, "instantiate method should exist");
      assertEquals(Instance.class, method.getReturnType(), "instantiate should return Instance");
    }

    @Test
    @DisplayName("should have instantiate method with store and imports")
    void shouldHaveInstantiateMethodWithStoreAndImports() throws NoSuchMethodException {
      final Method method =
          JniInstancePre.class.getMethod("instantiate", Store.class, ImportMap.class);
      assertNotNull(method, "instantiate method with imports should exist");
      assertEquals(Instance.class, method.getReturnType(), "instantiate should return Instance");
    }

    @Test
    @DisplayName("should have instantiateAsync method with store")
    void shouldHaveInstantiateAsyncMethodWithStore() throws NoSuchMethodException {
      final Method method = JniInstancePre.class.getMethod("instantiateAsync", Store.class);
      assertNotNull(method, "instantiateAsync method should exist");
      assertEquals(
          CompletableFuture.class,
          method.getReturnType(),
          "instantiateAsync should return CompletableFuture");
    }

    @Test
    @DisplayName("should have instantiateAsync method with store and imports")
    void shouldHaveInstantiateAsyncMethodWithStoreAndImports() throws NoSuchMethodException {
      final Method method =
          JniInstancePre.class.getMethod("instantiateAsync", Store.class, ImportMap.class);
      assertNotNull(method, "instantiateAsync method with imports should exist");
      assertEquals(
          CompletableFuture.class,
          method.getReturnType(),
          "instantiateAsync should return CompletableFuture");
    }
  }

  @Nested
  @DisplayName("Accessor Method Tests")
  class AccessorMethodTests {

    @Test
    @DisplayName("should have getModule method")
    void shouldHaveGetModuleMethod() throws NoSuchMethodException {
      final Method method = JniInstancePre.class.getMethod("getModule");
      assertNotNull(method, "getModule method should exist");
      assertEquals(Module.class, method.getReturnType(), "getModule should return Module");
    }

    @Test
    @DisplayName("should have getEngine method")
    void shouldHaveGetEngineMethod() throws NoSuchMethodException {
      final Method method = JniInstancePre.class.getMethod("getEngine");
      assertNotNull(method, "getEngine method should exist");
      assertEquals(Engine.class, method.getReturnType(), "getEngine should return Engine");
    }

    @Test
    @DisplayName("should have getNativeHandle method")
    void shouldHaveGetNativeHandleMethod() throws NoSuchMethodException {
      final Method method = JniInstancePre.class.getMethod("getNativeHandle");
      assertNotNull(method, "getNativeHandle method should exist");
      assertEquals(long.class, method.getReturnType(), "getNativeHandle should return long");
    }
  }

  @Nested
  @DisplayName("State Query Method Tests")
  class StateQueryMethodTests {

    @Test
    @DisplayName("should have isValid method")
    void shouldHaveIsValidMethod() throws NoSuchMethodException {
      final Method method = JniInstancePre.class.getMethod("isValid");
      assertNotNull(method, "isValid method should exist");
      assertEquals(boolean.class, method.getReturnType(), "isValid should return boolean");
    }

    @Test
    @DisplayName("should have getInstanceCount method")
    void shouldHaveGetInstanceCountMethod() throws NoSuchMethodException {
      final Method method = JniInstancePre.class.getMethod("getInstanceCount");
      assertNotNull(method, "getInstanceCount method should exist");
      assertEquals(long.class, method.getReturnType(), "getInstanceCount should return long");
    }

    @Test
    @DisplayName("should have getStatistics method")
    void shouldHaveGetStatisticsMethod() throws NoSuchMethodException {
      final Method method = JniInstancePre.class.getMethod("getStatistics");
      assertNotNull(method, "getStatistics method should exist");
      assertEquals(
          PreInstantiationStatistics.class,
          method.getReturnType(),
          "getStatistics should return PreInstantiationStatistics");
    }
  }

  @Nested
  @DisplayName("Lifecycle Method Tests")
  class LifecycleMethodTests {

    @Test
    @DisplayName("should have close method")
    void shouldHaveCloseMethod() throws NoSuchMethodException {
      final Method method = JniInstancePre.class.getMethod("close");
      assertNotNull(method, "close method should exist");
      assertEquals(void.class, method.getReturnType(), "close should return void");
    }
  }

  @Nested
  @DisplayName("Object Method Tests")
  class ObjectMethodTests {

    @Test
    @DisplayName("should have toString method")
    void shouldHaveToStringMethod() throws NoSuchMethodException {
      final Method method = JniInstancePre.class.getMethod("toString");
      assertNotNull(method, "toString method should exist");
      assertEquals(String.class, method.getReturnType(), "toString should return String");
    }
  }
}
