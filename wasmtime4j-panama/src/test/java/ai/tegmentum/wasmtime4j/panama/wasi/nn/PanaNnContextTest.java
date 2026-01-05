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

package ai.tegmentum.wasmtime4j.panama.wasi.nn;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.wasi.nn.NnContext;
import ai.tegmentum.wasmtime4j.wasi.nn.NnExecutionTarget;
import ai.tegmentum.wasmtime4j.wasi.nn.NnGraph;
import ai.tegmentum.wasmtime4j.wasi.nn.NnGraphEncoding;
import java.lang.foreign.MemorySegment;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link PanaNnContext} class.
 *
 * <p>PanaNnContext provides Panama FFI implementation of WASI-NN context.
 */
@DisplayName("PanaNnContext Tests")
class PanaNnContextTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be public final class")
    void shouldBePublicFinalClass() {
      assertTrue(
          Modifier.isPublic(PanaNnContext.class.getModifiers()), "PanaNnContext should be public");
      assertTrue(
          Modifier.isFinal(PanaNnContext.class.getModifiers()), "PanaNnContext should be final");
    }

    @Test
    @DisplayName("should implement NnContext interface")
    void shouldImplementNnContextInterface() {
      assertTrue(
          NnContext.class.isAssignableFrom(PanaNnContext.class),
          "PanaNnContext should implement NnContext");
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should have package-private constructor with MemorySegment")
    void shouldHavePackagePrivateConstructor() throws NoSuchMethodException {
      final Constructor<?> constructor =
          PanaNnContext.class.getDeclaredConstructor(MemorySegment.class);
      assertNotNull(constructor, "Constructor with MemorySegment should exist");
      // Package-private constructor
      assertTrue(
          !Modifier.isPublic(constructor.getModifiers()), "Constructor should be package-private");
    }
  }

  @Nested
  @DisplayName("Load Graph Method Tests")
  class LoadGraphMethodTests {

    @Test
    @DisplayName("should have loadGraph method with byte array")
    void shouldHaveLoadGraphWithByteArray() throws NoSuchMethodException {
      final Method method =
          PanaNnContext.class.getMethod(
              "loadGraph", byte[].class, NnGraphEncoding.class, NnExecutionTarget.class);
      assertNotNull(method, "loadGraph with byte[] should exist");
      assertEquals(NnGraph.class, method.getReturnType(), "Should return NnGraph");
    }

    @Test
    @DisplayName("should have loadGraph method with list of byte arrays")
    void shouldHaveLoadGraphWithList() throws NoSuchMethodException {
      final Method method =
          PanaNnContext.class.getMethod(
              "loadGraph", List.class, NnGraphEncoding.class, NnExecutionTarget.class);
      assertNotNull(method, "loadGraph with List should exist");
      assertEquals(NnGraph.class, method.getReturnType(), "Should return NnGraph");
    }

    @Test
    @DisplayName("should have loadGraphFromFile method")
    void shouldHaveLoadGraphFromFileMethod() throws NoSuchMethodException {
      final Method method =
          PanaNnContext.class.getMethod(
              "loadGraphFromFile", Path.class, NnGraphEncoding.class, NnExecutionTarget.class);
      assertNotNull(method, "loadGraphFromFile method should exist");
      assertEquals(NnGraph.class, method.getReturnType(), "Should return NnGraph");
    }

    @Test
    @DisplayName("should have loadGraphByName method")
    void shouldHaveLoadGraphByNameMethod() throws NoSuchMethodException {
      final Method method = PanaNnContext.class.getMethod("loadGraphByName", String.class);
      assertNotNull(method, "loadGraphByName method should exist");
      assertEquals(NnGraph.class, method.getReturnType(), "Should return NnGraph");
    }
  }

  @Nested
  @DisplayName("Encoding Support Method Tests")
  class EncodingSupportMethodTests {

    @Test
    @DisplayName("should have getSupportedEncodings method")
    void shouldHaveGetSupportedEncodingsMethod() throws NoSuchMethodException {
      final Method method = PanaNnContext.class.getMethod("getSupportedEncodings");
      assertNotNull(method, "getSupportedEncodings method should exist");
      assertEquals(Set.class, method.getReturnType(), "Should return Set");
    }

    @Test
    @DisplayName("should have isEncodingSupported method")
    void shouldHaveIsEncodingSupportedMethod() throws NoSuchMethodException {
      final Method method =
          PanaNnContext.class.getMethod("isEncodingSupported", NnGraphEncoding.class);
      assertNotNull(method, "isEncodingSupported method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }
  }

  @Nested
  @DisplayName("Target Support Method Tests")
  class TargetSupportMethodTests {

    @Test
    @DisplayName("should have getSupportedTargets method")
    void shouldHaveGetSupportedTargetsMethod() throws NoSuchMethodException {
      final Method method = PanaNnContext.class.getMethod("getSupportedTargets");
      assertNotNull(method, "getSupportedTargets method should exist");
      assertEquals(Set.class, method.getReturnType(), "Should return Set");
    }

    @Test
    @DisplayName("should have isTargetSupported method")
    void shouldHaveIsTargetSupportedMethod() throws NoSuchMethodException {
      final Method method =
          PanaNnContext.class.getMethod("isTargetSupported", NnExecutionTarget.class);
      assertNotNull(method, "isTargetSupported method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }
  }

  @Nested
  @DisplayName("Availability Method Tests")
  class AvailabilityMethodTests {

    @Test
    @DisplayName("should have isAvailable method")
    void shouldHaveIsAvailableMethod() throws NoSuchMethodException {
      final Method method = PanaNnContext.class.getMethod("isAvailable");
      assertNotNull(method, "isAvailable method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have isValid method")
    void shouldHaveIsValidMethod() throws NoSuchMethodException {
      final Method method = PanaNnContext.class.getMethod("isValid");
      assertNotNull(method, "isValid method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }
  }

  @Nested
  @DisplayName("Implementation Info Method Tests")
  class ImplementationInfoMethodTests {

    @Test
    @DisplayName("should have getImplementationInfo method")
    void shouldHaveGetImplementationInfoMethod() throws NoSuchMethodException {
      final Method method = PanaNnContext.class.getMethod("getImplementationInfo");
      assertNotNull(method, "getImplementationInfo method should exist");
      assertEquals(
          NnContext.NnImplementationInfo.class,
          method.getReturnType(),
          "Should return NnImplementationInfo");
    }
  }

  @Nested
  @DisplayName("Close Method Tests")
  class CloseMethodTests {

    @Test
    @DisplayName("should have close method")
    void shouldHaveCloseMethod() throws NoSuchMethodException {
      final Method method = PanaNnContext.class.getMethod("close");
      assertNotNull(method, "close method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }
  }
}
