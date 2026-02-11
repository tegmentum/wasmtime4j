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

import ai.tegmentum.wasmtime4j.func.Caller;
import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.ModuleExport;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link JniCaller} class.
 *
 * <p>JniCaller provides JNI implementation of the Caller interface for accessing WebAssembly
 * instance context from within host function callbacks.
 */
@DisplayName("JniCaller Tests")
class JniCallerTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be package-private and final")
    void shouldBePackagePrivateAndFinal() {
      // JniCaller is package-private (no public modifier)
      assertTrue(
          !Modifier.isPublic(JniCaller.class.getModifiers())
              && !Modifier.isProtected(JniCaller.class.getModifiers())
              && !Modifier.isPrivate(JniCaller.class.getModifiers()),
          "JniCaller should be package-private");
      assertTrue(Modifier.isFinal(JniCaller.class.getModifiers()), "JniCaller should be final");
    }

    @Test
    @DisplayName("should implement Caller interface")
    void shouldImplementCallerInterface() {
      assertTrue(
          Caller.class.isAssignableFrom(JniCaller.class),
          "JniCaller should implement Caller interface");
    }

    @Test
    @DisplayName("should be a generic class")
    void shouldBeGenericClass() {
      final Type[] typeParams = JniCaller.class.getTypeParameters();
      assertEquals(1, typeParams.length, "JniCaller should have one type parameter");
      assertEquals("T", typeParams[0].getTypeName(), "Type parameter should be named T");
    }
  }

  @Nested
  @DisplayName("Data Method Tests")
  class DataMethodTests {

    @Test
    @DisplayName("should have data method")
    void shouldHaveDataMethod() throws NoSuchMethodException {
      final Method method = JniCaller.class.getMethod("data");
      assertNotNull(method, "data method should exist");
      assertEquals(Object.class, method.getReturnType(), "data should return Object (type T)");
    }
  }

  @Nested
  @DisplayName("Export Access Method Tests")
  class ExportAccessMethodTests {

    @Test
    @DisplayName("should have getExport method")
    void shouldHaveGetExportMethod() throws NoSuchMethodException {
      final Method method = JniCaller.class.getMethod("getExport", String.class);
      assertNotNull(method, "getExport method should exist");
      assertEquals(Optional.class, method.getReturnType(), "getExport should return Optional");
    }

    @Test
    @DisplayName("should have getFunction method")
    void shouldHaveGetFunctionMethod() throws NoSuchMethodException {
      final Method method = JniCaller.class.getMethod("getFunction", String.class);
      assertNotNull(method, "getFunction method should exist");
      assertEquals(Optional.class, method.getReturnType(), "getFunction should return Optional");
    }

    @Test
    @DisplayName("should have getMemory method")
    void shouldHaveGetMemoryMethod() throws NoSuchMethodException {
      final Method method = JniCaller.class.getMethod("getMemory", String.class);
      assertNotNull(method, "getMemory method should exist");
      assertEquals(Optional.class, method.getReturnType(), "getMemory should return Optional");
    }

    @Test
    @DisplayName("should have getTable method")
    void shouldHaveGetTableMethod() throws NoSuchMethodException {
      final Method method = JniCaller.class.getMethod("getTable", String.class);
      assertNotNull(method, "getTable method should exist");
      assertEquals(Optional.class, method.getReturnType(), "getTable should return Optional");
    }

    @Test
    @DisplayName("should have getGlobal method")
    void shouldHaveGetGlobalMethod() throws NoSuchMethodException {
      final Method method = JniCaller.class.getMethod("getGlobal", String.class);
      assertNotNull(method, "getGlobal method should exist");
      assertEquals(Optional.class, method.getReturnType(), "getGlobal should return Optional");
    }

    @Test
    @DisplayName("should have hasExport method")
    void shouldHaveHasExportMethod() throws NoSuchMethodException {
      final Method method = JniCaller.class.getMethod("hasExport", String.class);
      assertNotNull(method, "hasExport method should exist");
      assertEquals(boolean.class, method.getReturnType(), "hasExport should return boolean");
    }

    @Test
    @DisplayName("should have getExportByModuleExport method")
    void shouldHaveGetExportByModuleExportMethod() throws NoSuchMethodException {
      final Method method =
          JniCaller.class.getMethod("getExportByModuleExport", ModuleExport.class);
      assertNotNull(method, "getExportByModuleExport method should exist");
      assertEquals(
          Optional.class, method.getReturnType(), "getExportByModuleExport should return Optional");
    }
  }

  @Nested
  @DisplayName("Fuel Method Tests")
  class FuelMethodTests {

    @Test
    @DisplayName("should have fuelConsumed method")
    void shouldHaveFuelConsumedMethod() throws NoSuchMethodException {
      final Method method = JniCaller.class.getMethod("fuelConsumed");
      assertNotNull(method, "fuelConsumed method should exist");
      assertEquals(Optional.class, method.getReturnType(), "fuelConsumed should return Optional");
    }

    @Test
    @DisplayName("should have fuelRemaining method")
    void shouldHaveFuelRemainingMethod() throws NoSuchMethodException {
      final Method method = JniCaller.class.getMethod("fuelRemaining");
      assertNotNull(method, "fuelRemaining method should exist");
      assertEquals(Optional.class, method.getReturnType(), "fuelRemaining should return Optional");
    }

    @Test
    @DisplayName("should have addFuel method")
    void shouldHaveAddFuelMethod() throws NoSuchMethodException {
      final Method method = JniCaller.class.getMethod("addFuel", long.class);
      assertNotNull(method, "addFuel method should exist");
      assertEquals(void.class, method.getReturnType(), "addFuel should return void");
    }

    @Test
    @DisplayName("should have fuelAsyncYieldInterval method")
    void shouldHaveFuelAsyncYieldIntervalMethod() throws NoSuchMethodException {
      final Method method = JniCaller.class.getMethod("fuelAsyncYieldInterval");
      assertNotNull(method, "fuelAsyncYieldInterval method should exist");
      assertEquals(
          Optional.class, method.getReturnType(), "fuelAsyncYieldInterval should return Optional");
    }

    @Test
    @DisplayName("should have setFuelAsyncYieldInterval method")
    void shouldHaveSetFuelAsyncYieldIntervalMethod() throws NoSuchMethodException {
      final Method method = JniCaller.class.getMethod("setFuelAsyncYieldInterval", long.class);
      assertNotNull(method, "setFuelAsyncYieldInterval method should exist");
      assertEquals(
          void.class, method.getReturnType(), "setFuelAsyncYieldInterval should return void");
    }
  }

  @Nested
  @DisplayName("Epoch Method Tests")
  class EpochMethodTests {

    @Test
    @DisplayName("should have hasEpochDeadline method")
    void shouldHaveHasEpochDeadlineMethod() throws NoSuchMethodException {
      final Method method = JniCaller.class.getMethod("hasEpochDeadline");
      assertNotNull(method, "hasEpochDeadline method should exist");
      assertEquals(boolean.class, method.getReturnType(), "hasEpochDeadline should return boolean");
    }

    @Test
    @DisplayName("should have epochDeadline method")
    void shouldHaveEpochDeadlineMethod() throws NoSuchMethodException {
      final Method method = JniCaller.class.getMethod("epochDeadline");
      assertNotNull(method, "epochDeadline method should exist");
      assertEquals(Optional.class, method.getReturnType(), "epochDeadline should return Optional");
    }

    @Test
    @DisplayName("should have setEpochDeadline method")
    void shouldHaveSetEpochDeadlineMethod() throws NoSuchMethodException {
      final Method method = JniCaller.class.getMethod("setEpochDeadline", long.class);
      assertNotNull(method, "setEpochDeadline method should exist");
      assertEquals(void.class, method.getReturnType(), "setEpochDeadline should return void");
    }
  }

  @Nested
  @DisplayName("Engine and GC Method Tests")
  class EngineAndGcMethodTests {

    @Test
    @DisplayName("should have engine method")
    void shouldHaveEngineMethod() throws NoSuchMethodException {
      final Method method = JniCaller.class.getMethod("engine");
      assertNotNull(method, "engine method should exist");
      assertEquals(Engine.class, method.getReturnType(), "engine should return Engine");
    }

    @Test
    @DisplayName("should have gc method")
    void shouldHaveGcMethod() throws NoSuchMethodException {
      final Method method = JniCaller.class.getMethod("gc");
      assertNotNull(method, "gc method should exist");
      assertEquals(void.class, method.getReturnType(), "gc should return void");
    }
  }

  @Nested
  @DisplayName("Internal Method Tests")
  class InternalMethodTests {

    @Test
    @DisplayName("should have getCallerHandle method")
    void shouldHaveGetCallerHandleMethod() throws NoSuchMethodException {
      final Method method = JniCaller.class.getDeclaredMethod("getCallerHandle");
      assertNotNull(method, "getCallerHandle method should exist");
      assertEquals(long.class, method.getReturnType(), "getCallerHandle should return long");
    }

    @Test
    @DisplayName("should have getStore method")
    void shouldHaveGetStoreMethod() throws NoSuchMethodException {
      final Method method = JniCaller.class.getDeclaredMethod("getStore");
      assertNotNull(method, "getStore method should exist");
      assertEquals(JniStore.class, method.getReturnType(), "getStore should return JniStore");
    }
  }

  @Nested
  @DisplayName("Object Method Tests")
  class ObjectMethodTests {

    @Test
    @DisplayName("should have toString method")
    void shouldHaveToStringMethod() throws NoSuchMethodException {
      final Method method = JniCaller.class.getMethod("toString");
      assertNotNull(method, "toString method should exist");
      assertEquals(String.class, method.getReturnType(), "toString should return String");
    }
  }
}
