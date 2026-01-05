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

import ai.tegmentum.wasmtime4j.wasi.nn.NnExecutionTarget;
import ai.tegmentum.wasmtime4j.wasi.nn.NnGraph;
import ai.tegmentum.wasmtime4j.wasi.nn.NnGraphEncoding;
import ai.tegmentum.wasmtime4j.wasi.nn.NnGraphExecutionContext;
import java.lang.foreign.MemorySegment;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link PanaNnGraph} class.
 *
 * <p>PanaNnGraph provides Panama FFI implementation of WASI-NN graph.
 */
@DisplayName("PanaNnGraph Tests")
class PanaNnGraphTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be public final class")
    void shouldBePublicFinalClass() {
      assertTrue(
          Modifier.isPublic(PanaNnGraph.class.getModifiers()), "PanaNnGraph should be public");
      assertTrue(Modifier.isFinal(PanaNnGraph.class.getModifiers()), "PanaNnGraph should be final");
    }

    @Test
    @DisplayName("should implement NnGraph interface")
    void shouldImplementNnGraphInterface() {
      assertTrue(
          NnGraph.class.isAssignableFrom(PanaNnGraph.class),
          "PanaNnGraph should implement NnGraph");
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should have package-private constructor")
    void shouldHavePackagePrivateConstructor() throws NoSuchMethodException {
      final Constructor<?> constructor =
          PanaNnGraph.class.getDeclaredConstructor(
              MemorySegment.class, NnGraphEncoding.class, NnExecutionTarget.class);
      assertNotNull(constructor, "Constructor with MemorySegment, encoding, target should exist");
      // Package-private constructor
      assertTrue(
          !Modifier.isPublic(constructor.getModifiers()), "Constructor should be package-private");
    }
  }

  @Nested
  @DisplayName("Handle Access Method Tests")
  class HandleAccessMethodTests {

    @Test
    @DisplayName("should have getNativeHandle method")
    void shouldHaveGetNativeHandleMethod() throws NoSuchMethodException {
      final Method method = PanaNnGraph.class.getMethod("getNativeHandle");
      assertNotNull(method, "getNativeHandle method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have package-private getNativeSegment method")
    void shouldHaveGetNativeSegmentMethod() throws NoSuchMethodException {
      final Method method = PanaNnGraph.class.getDeclaredMethod("getNativeSegment");
      assertNotNull(method, "getNativeSegment method should exist");
      assertEquals(MemorySegment.class, method.getReturnType(), "Should return MemorySegment");
      // Package-private method
      assertTrue(
          !Modifier.isPublic(method.getModifiers()), "getNativeSegment should be package-private");
    }
  }

  @Nested
  @DisplayName("Graph Info Method Tests")
  class GraphInfoMethodTests {

    @Test
    @DisplayName("should have getEncoding method")
    void shouldHaveGetEncodingMethod() throws NoSuchMethodException {
      final Method method = PanaNnGraph.class.getMethod("getEncoding");
      assertNotNull(method, "getEncoding method should exist");
      assertEquals(NnGraphEncoding.class, method.getReturnType(), "Should return NnGraphEncoding");
    }

    @Test
    @DisplayName("should have getExecutionTarget method")
    void shouldHaveGetExecutionTargetMethod() throws NoSuchMethodException {
      final Method method = PanaNnGraph.class.getMethod("getExecutionTarget");
      assertNotNull(method, "getExecutionTarget method should exist");
      assertEquals(
          NnExecutionTarget.class, method.getReturnType(), "Should return NnExecutionTarget");
    }

    @Test
    @DisplayName("should have getModelName method")
    void shouldHaveGetModelNameMethod() throws NoSuchMethodException {
      final Method method = PanaNnGraph.class.getMethod("getModelName");
      assertNotNull(method, "getModelName method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }
  }

  @Nested
  @DisplayName("Execution Context Method Tests")
  class ExecutionContextMethodTests {

    @Test
    @DisplayName("should have createExecutionContext method")
    void shouldHaveCreateExecutionContextMethod() throws NoSuchMethodException {
      final Method method = PanaNnGraph.class.getMethod("createExecutionContext");
      assertNotNull(method, "createExecutionContext method should exist");
      assertEquals(
          NnGraphExecutionContext.class,
          method.getReturnType(),
          "Should return NnGraphExecutionContext");
    }
  }

  @Nested
  @DisplayName("Validation Method Tests")
  class ValidationMethodTests {

    @Test
    @DisplayName("should have isValid method")
    void shouldHaveIsValidMethod() throws NoSuchMethodException {
      final Method method = PanaNnGraph.class.getMethod("isValid");
      assertNotNull(method, "isValid method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }
  }

  @Nested
  @DisplayName("Close Method Tests")
  class CloseMethodTests {

    @Test
    @DisplayName("should have close method")
    void shouldHaveCloseMethod() throws NoSuchMethodException {
      final Method method = PanaNnGraph.class.getMethod("close");
      assertNotNull(method, "close method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }
  }
}
