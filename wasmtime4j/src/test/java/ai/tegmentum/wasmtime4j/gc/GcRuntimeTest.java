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

package ai.tegmentum.wasmtime4j.gc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link GcRuntime} interface.
 *
 * <p>GcRuntime provides the WebAssembly GC runtime operations interface.
 */
@DisplayName("GcRuntime Tests")
class GcRuntimeTest {

  @Nested
  @DisplayName("Interface Structure Tests")
  class InterfaceStructureTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeInterface() {
      assertTrue(GcRuntime.class.isInterface(), "GcRuntime should be an interface");
    }

    @Test
    @DisplayName("should be public")
    void shouldBePublic() {
      assertTrue(Modifier.isPublic(GcRuntime.class.getModifiers()), "GcRuntime should be public");
    }
  }

  @Nested
  @DisplayName("Object Creation Method Tests")
  class ObjectCreationMethodTests {

    @Test
    @DisplayName("should have createStruct method with type and values")
    void shouldHaveCreateStructMethodWithTypeAndValues() throws NoSuchMethodException {
      final Method method = GcRuntime.class.getMethod("createStruct", StructType.class, List.class);
      assertNotNull(method, "createStruct method should exist");
      assertEquals(StructInstance.class, method.getReturnType(), "Should return StructInstance");
    }

    @Test
    @DisplayName("should have createStruct method with type only")
    void shouldHaveCreateStructMethodWithTypeOnly() throws NoSuchMethodException {
      final Method method = GcRuntime.class.getMethod("createStruct", StructType.class);
      assertNotNull(method, "createStruct method should exist");
      assertEquals(StructInstance.class, method.getReturnType(), "Should return StructInstance");
    }

    @Test
    @DisplayName("should have createArray method with type and elements")
    void shouldHaveCreateArrayMethodWithTypeAndElements() throws NoSuchMethodException {
      final Method method = GcRuntime.class.getMethod("createArray", ArrayType.class, List.class);
      assertNotNull(method, "createArray method should exist");
      assertEquals(ArrayInstance.class, method.getReturnType(), "Should return ArrayInstance");
    }

    @Test
    @DisplayName("should have createArray method with type and length")
    void shouldHaveCreateArrayMethodWithTypeAndLength() throws NoSuchMethodException {
      final Method method = GcRuntime.class.getMethod("createArray", ArrayType.class, int.class);
      assertNotNull(method, "createArray method should exist");
      assertEquals(ArrayInstance.class, method.getReturnType(), "Should return ArrayInstance");
    }

    @Test
    @DisplayName("should have createI31 method")
    void shouldHaveCreateI31Method() throws NoSuchMethodException {
      final Method method = GcRuntime.class.getMethod("createI31", int.class);
      assertNotNull(method, "createI31 method should exist");
      assertEquals(I31Instance.class, method.getReturnType(), "Should return I31Instance");
    }
  }

  @Nested
  @DisplayName("Field and Element Access Method Tests")
  class FieldAndElementAccessMethodTests {

    @Test
    @DisplayName("should have getStructField method")
    void shouldHaveGetStructFieldMethod() throws NoSuchMethodException {
      final Method method =
          GcRuntime.class.getMethod("getStructField", StructInstance.class, int.class);
      assertNotNull(method, "getStructField method should exist");
      assertEquals(GcValue.class, method.getReturnType(), "Should return GcValue");
    }

    @Test
    @DisplayName("should have setStructField method")
    void shouldHaveSetStructFieldMethod() throws NoSuchMethodException {
      final Method method =
          GcRuntime.class.getMethod(
              "setStructField", StructInstance.class, int.class, GcValue.class);
      assertNotNull(method, "setStructField method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have getArrayElement method")
    void shouldHaveGetArrayElementMethod() throws NoSuchMethodException {
      final Method method =
          GcRuntime.class.getMethod("getArrayElement", ArrayInstance.class, int.class);
      assertNotNull(method, "getArrayElement method should exist");
      assertEquals(GcValue.class, method.getReturnType(), "Should return GcValue");
    }

    @Test
    @DisplayName("should have setArrayElement method")
    void shouldHaveSetArrayElementMethod() throws NoSuchMethodException {
      final Method method =
          GcRuntime.class.getMethod(
              "setArrayElement", ArrayInstance.class, int.class, GcValue.class);
      assertNotNull(method, "setArrayElement method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have getArrayLength method")
    void shouldHaveGetArrayLengthMethod() throws NoSuchMethodException {
      final Method method = GcRuntime.class.getMethod("getArrayLength", ArrayInstance.class);
      assertNotNull(method, "getArrayLength method should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }
  }

  @Nested
  @DisplayName("Reference Type Operation Method Tests")
  class ReferenceTypeOperationMethodTests {

    @Test
    @DisplayName("should have refCast method")
    void shouldHaveRefCastMethod() throws NoSuchMethodException {
      final Method method =
          GcRuntime.class.getMethod("refCast", GcObject.class, GcReferenceType.class);
      assertNotNull(method, "refCast method should exist");
      assertEquals(GcObject.class, method.getReturnType(), "Should return GcObject");
    }

    @Test
    @DisplayName("should have refTest method")
    void shouldHaveRefTestMethod() throws NoSuchMethodException {
      final Method method =
          GcRuntime.class.getMethod("refTest", GcObject.class, GcReferenceType.class);
      assertNotNull(method, "refTest method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have refEquals method")
    void shouldHaveRefEqualsMethod() throws NoSuchMethodException {
      final Method method = GcRuntime.class.getMethod("refEquals", GcObject.class, GcObject.class);
      assertNotNull(method, "refEquals method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have isNull method")
    void shouldHaveIsNullMethod() throws NoSuchMethodException {
      final Method method = GcRuntime.class.getMethod("isNull", GcObject.class);
      assertNotNull(method, "isNull method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have getRuntimeType method")
    void shouldHaveGetRuntimeTypeMethod() throws NoSuchMethodException {
      final Method method = GcRuntime.class.getMethod("getRuntimeType", GcObject.class);
      assertNotNull(method, "getRuntimeType method should exist");
      assertEquals(GcReferenceType.class, method.getReturnType(), "Should return GcReferenceType");
    }

    @Test
    @DisplayName("should have refCastNullable method")
    void shouldHaveRefCastNullableMethod() throws NoSuchMethodException {
      final Method method =
          GcRuntime.class.getMethod("refCastNullable", GcObject.class, GcReferenceType.class);
      assertNotNull(method, "refCastNullable method should exist");
      assertEquals(Optional.class, method.getReturnType(), "Should return Optional");
    }
  }

  @Nested
  @DisplayName("Garbage Collection Method Tests")
  class GarbageCollectionMethodTests {

    @Test
    @DisplayName("should have collectGarbage method")
    void shouldHaveCollectGarbageMethod() throws NoSuchMethodException {
      final Method method = GcRuntime.class.getMethod("collectGarbage");
      assertNotNull(method, "collectGarbage method should exist");
      assertEquals(GcStats.class, method.getReturnType(), "Should return GcStats");
    }

    @Test
    @DisplayName("should have collectGarbageIncremental method")
    void shouldHaveCollectGarbageIncrementalMethod() throws NoSuchMethodException {
      final Method method = GcRuntime.class.getMethod("collectGarbageIncremental", long.class);
      assertNotNull(method, "collectGarbageIncremental method should exist");
      assertEquals(GcStats.class, method.getReturnType(), "Should return GcStats");
    }

    @Test
    @DisplayName("should have collectGarbageConcurrent method")
    void shouldHaveCollectGarbageConcurrentMethod() throws NoSuchMethodException {
      final Method method = GcRuntime.class.getMethod("collectGarbageConcurrent");
      assertNotNull(method, "collectGarbageConcurrent method should exist");
      assertEquals(GcStats.class, method.getReturnType(), "Should return GcStats");
    }

    @Test
    @DisplayName("should have getGcStats method")
    void shouldHaveGetGcStatsMethod() throws NoSuchMethodException {
      final Method method = GcRuntime.class.getMethod("getGcStats");
      assertNotNull(method, "getGcStats method should exist");
      assertEquals(GcStats.class, method.getReturnType(), "Should return GcStats");
    }

    @Test
    @DisplayName("should have configureGcStrategy method")
    void shouldHaveConfigureGcStrategyMethod() throws NoSuchMethodException {
      final Method method =
          GcRuntime.class.getMethod("configureGcStrategy", String.class, Map.class);
      assertNotNull(method, "configureGcStrategy method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }
  }

  @Nested
  @DisplayName("Type Registration Method Tests")
  class TypeRegistrationMethodTests {

    @Test
    @DisplayName("should have registerStructType method")
    void shouldHaveRegisterStructTypeMethod() throws NoSuchMethodException {
      final Method method = GcRuntime.class.getMethod("registerStructType", StructType.class);
      assertNotNull(method, "registerStructType method should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }

    @Test
    @DisplayName("should have registerArrayType method")
    void shouldHaveRegisterArrayTypeMethod() throws NoSuchMethodException {
      final Method method = GcRuntime.class.getMethod("registerArrayType", ArrayType.class);
      assertNotNull(method, "registerArrayType method should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }
  }

  @Nested
  @DisplayName("Debugging and Profiling Method Tests")
  class DebuggingAndProfilingMethodTests {

    @Test
    @DisplayName("should have inspectHeap method")
    void shouldHaveInspectHeapMethod() throws NoSuchMethodException {
      final Method method = GcRuntime.class.getMethod("inspectHeap");
      assertNotNull(method, "inspectHeap method should exist");
      assertEquals(
          GcHeapInspection.class, method.getReturnType(), "Should return GcHeapInspection");
    }

    @Test
    @DisplayName("should have detectMemoryLeaks method")
    void shouldHaveDetectMemoryLeaksMethod() throws NoSuchMethodException {
      final Method method = GcRuntime.class.getMethod("detectMemoryLeaks");
      assertNotNull(method, "detectMemoryLeaks method should exist");
      assertEquals(
          MemoryLeakAnalysis.class, method.getReturnType(), "Should return MemoryLeakAnalysis");
    }

    @Test
    @DisplayName("should have startProfiling method")
    void shouldHaveStartProfilingMethod() throws NoSuchMethodException {
      final Method method = GcRuntime.class.getMethod("startProfiling");
      assertNotNull(method, "startProfiling method should exist");
      assertEquals(GcProfiler.class, method.getReturnType(), "Should return GcProfiler");
    }
  }
}
