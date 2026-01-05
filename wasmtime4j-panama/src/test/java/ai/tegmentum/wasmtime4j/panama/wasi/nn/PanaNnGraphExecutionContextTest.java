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

import ai.tegmentum.wasmtime4j.wasi.nn.NnGraph;
import ai.tegmentum.wasmtime4j.wasi.nn.NnGraphExecutionContext;
import ai.tegmentum.wasmtime4j.wasi.nn.NnTensor;
import java.lang.foreign.MemorySegment;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link PanaNnGraphExecutionContext} class.
 *
 * <p>PanaNnGraphExecutionContext provides Panama FFI implementation of WASI-NN inference context.
 */
@DisplayName("PanaNnGraphExecutionContext Tests")
class PanaNnGraphExecutionContextTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be public final class")
    void shouldBePublicFinalClass() {
      assertTrue(
          Modifier.isPublic(PanaNnGraphExecutionContext.class.getModifiers()),
          "PanaNnGraphExecutionContext should be public");
      assertTrue(
          Modifier.isFinal(PanaNnGraphExecutionContext.class.getModifiers()),
          "PanaNnGraphExecutionContext should be final");
    }

    @Test
    @DisplayName("should implement NnGraphExecutionContext interface")
    void shouldImplementNnGraphExecutionContextInterface() {
      assertTrue(
          NnGraphExecutionContext.class.isAssignableFrom(PanaNnGraphExecutionContext.class),
          "PanaNnGraphExecutionContext should implement NnGraphExecutionContext");
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should have package-private constructor")
    void shouldHavePackagePrivateConstructor() throws NoSuchMethodException {
      final Constructor<?> constructor =
          PanaNnGraphExecutionContext.class.getDeclaredConstructor(
              MemorySegment.class, NnGraph.class);
      assertNotNull(constructor, "Constructor with MemorySegment and NnGraph should exist");
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
      final Method method = PanaNnGraphExecutionContext.class.getMethod("getNativeHandle");
      assertNotNull(method, "getNativeHandle method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have getGraph method")
    void shouldHaveGetGraphMethod() throws NoSuchMethodException {
      final Method method = PanaNnGraphExecutionContext.class.getMethod("getGraph");
      assertNotNull(method, "getGraph method should exist");
      assertEquals(NnGraph.class, method.getReturnType(), "Should return NnGraph");
    }
  }

  @Nested
  @DisplayName("Compute Method Tests")
  class ComputeMethodTests {

    @Test
    @DisplayName("should have compute method with list")
    void shouldHaveComputeWithList() throws NoSuchMethodException {
      final Method method = PanaNnGraphExecutionContext.class.getMethod("compute", List.class);
      assertNotNull(method, "compute with List should exist");
      assertEquals(List.class, method.getReturnType(), "Should return List");
    }

    @Test
    @DisplayName("should have computeByIndex method with varargs")
    void shouldHaveComputeByIndexMethod() throws NoSuchMethodException {
      final Method method =
          PanaNnGraphExecutionContext.class.getMethod("computeByIndex", NnTensor[].class);
      assertNotNull(method, "computeByIndex method should exist");
      assertEquals(List.class, method.getReturnType(), "Should return List");
    }

    @Test
    @DisplayName("should have computeNoInputs method")
    void shouldHaveComputeNoInputsMethod() throws NoSuchMethodException {
      final Method method = PanaNnGraphExecutionContext.class.getMethod("computeNoInputs");
      assertNotNull(method, "computeNoInputs method should exist");
      assertEquals(List.class, method.getReturnType(), "Should return List");
    }
  }

  @Nested
  @DisplayName("Input Method Tests")
  class InputMethodTests {

    @Test
    @DisplayName("should have setInput method with index")
    void shouldHaveSetInputByIndex() throws NoSuchMethodException {
      final Method method =
          PanaNnGraphExecutionContext.class.getMethod("setInput", int.class, NnTensor.class);
      assertNotNull(method, "setInput with index should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have setInput method with name")
    void shouldHaveSetInputByName() throws NoSuchMethodException {
      final Method method =
          PanaNnGraphExecutionContext.class.getMethod("setInput", String.class, NnTensor.class);
      assertNotNull(method, "setInput with name should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have getInputCount method")
    void shouldHaveGetInputCountMethod() throws NoSuchMethodException {
      final Method method = PanaNnGraphExecutionContext.class.getMethod("getInputCount");
      assertNotNull(method, "getInputCount method should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }

    @Test
    @DisplayName("should have getInputMetadata method")
    void shouldHaveGetInputMetadataMethod() throws NoSuchMethodException {
      final Method method = PanaNnGraphExecutionContext.class.getMethod("getInputMetadata");
      assertNotNull(method, "getInputMetadata method should exist");
      assertEquals(Map.class, method.getReturnType(), "Should return Map");
    }
  }

  @Nested
  @DisplayName("Output Method Tests")
  class OutputMethodTests {

    @Test
    @DisplayName("should have getOutput method with index")
    void shouldHaveGetOutputByIndex() throws NoSuchMethodException {
      final Method method = PanaNnGraphExecutionContext.class.getMethod("getOutput", int.class);
      assertNotNull(method, "getOutput with index should exist");
      assertEquals(NnTensor.class, method.getReturnType(), "Should return NnTensor");
    }

    @Test
    @DisplayName("should have getOutput method with name")
    void shouldHaveGetOutputByName() throws NoSuchMethodException {
      final Method method = PanaNnGraphExecutionContext.class.getMethod("getOutput", String.class);
      assertNotNull(method, "getOutput with name should exist");
      assertEquals(NnTensor.class, method.getReturnType(), "Should return NnTensor");
    }

    @Test
    @DisplayName("should have getOutputCount method")
    void shouldHaveGetOutputCountMethod() throws NoSuchMethodException {
      final Method method = PanaNnGraphExecutionContext.class.getMethod("getOutputCount");
      assertNotNull(method, "getOutputCount method should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }

    @Test
    @DisplayName("should have getOutputMetadata method")
    void shouldHaveGetOutputMetadataMethod() throws NoSuchMethodException {
      final Method method = PanaNnGraphExecutionContext.class.getMethod("getOutputMetadata");
      assertNotNull(method, "getOutputMetadata method should exist");
      assertEquals(Map.class, method.getReturnType(), "Should return Map");
    }
  }

  @Nested
  @DisplayName("Validation Method Tests")
  class ValidationMethodTests {

    @Test
    @DisplayName("should have isValid method")
    void shouldHaveIsValidMethod() throws NoSuchMethodException {
      final Method method = PanaNnGraphExecutionContext.class.getMethod("isValid");
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
      final Method method = PanaNnGraphExecutionContext.class.getMethod("close");
      assertNotNull(method, "close method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }
  }
}
