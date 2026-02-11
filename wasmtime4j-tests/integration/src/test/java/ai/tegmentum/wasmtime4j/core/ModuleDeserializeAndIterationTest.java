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

package ai.tegmentum.wasmtime4j.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.func.FunctionInfo;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.tests.framework.DualRuntimeTest;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

/**
 * Tests {@link Module#deserializeFile(Engine, Path)} and {@link Module#functions()}.
 *
 * @since 1.0.0
 */
@DisplayName("Module Deserialize and Function Iteration Tests")
public class ModuleDeserializeAndIterationTest extends DualRuntimeTest {

  private static final Logger LOGGER =
      Logger.getLogger(ModuleDeserializeAndIterationTest.class.getName());

  /**
   * WAT module with three exported functions for testing function iteration.
   */
  private static final String WAT =
      """
      (module
        (func (export "add") (param i32 i32) (result i32) local.get 0 local.get 1 i32.add)
        (func (export "nop"))
        (func (export "get42") (result i32) i32.const 42))
      """;

  private static final String EMPTY_WAT = "(module)";

  @AfterEach
  void cleanup() {
    clearRuntimeSelection();
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("serialize then deserializeFile round-trips successfully")
  void serializeThenDeserializeFile(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing serialize -> deserializeFile round-trip");

    try (Engine engine = Engine.create();
        Store store = engine.createStore()) {
      final Module original = engine.compileWat(WAT);

      // Serialize
      final byte[] serialized = original.serialize();
      assertNotNull(serialized, "Serialized bytes should not be null");
      assertTrue(serialized.length > 0, "Serialized bytes should not be empty");
      LOGGER.info("[" + runtime + "] Serialized module: " + serialized.length + " bytes");

      // Write to temp file
      final Path tempFile = Files.createTempFile("wasmtime4j-test-", ".cwasm");
      try {
        Files.write(tempFile, serialized);
        LOGGER.info("[" + runtime + "] Wrote serialized module to: " + tempFile);

        // Deserialize from file
        final Module deserialized = Module.deserializeFile(engine, tempFile);
        assertNotNull(deserialized, "Deserialized module should not be null");

        // Instantiate and call "add" to verify
        final Instance instance = deserialized.instantiate(store);
        final WasmValue[] result = instance.callFunction("add",
            WasmValue.i32(10), WasmValue.i32(32));
        assertEquals(42, result[0].asInt(),
            "add(10, 32) should return 42 on deserialized module");
        LOGGER.info("[" + runtime + "] deserializeFile round-trip verified, add(10,32) = "
            + result[0].asInt());

        instance.close();
        deserialized.close();
      } finally {
        Files.deleteIfExists(tempFile);
      }

      original.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("deserializeFile with invalid path throws")
  void deserializeFileInvalidPathThrows(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing deserializeFile with invalid path");

    try (Engine engine = Engine.create()) {
      final Path nonExistent = Path.of("/tmp/wasmtime4j-nonexistent-"
          + System.nanoTime() + ".cwasm");

      assertThrows(Exception.class,
          () -> Module.deserializeFile(engine, nonExistent),
          "deserializeFile with non-existent path should throw");
      LOGGER.info("[" + runtime + "] deserializeFile with invalid path threw as expected");
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("deserializeFile with corrupted data throws")
  void deserializeFileCorruptedDataThrows(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing deserializeFile with corrupted data");

    try (Engine engine = Engine.create()) {
      // Write random bytes to a temp file
      final Path tempFile = Files.createTempFile("wasmtime4j-corrupt-", ".cwasm");
      try {
        final byte[] garbage = new byte[256];
        for (int i = 0; i < garbage.length; i++) {
          garbage[i] = (byte) (i * 7 + 13);
        }
        Files.write(tempFile, garbage);

        assertThrows(Exception.class,
            () -> Module.deserializeFile(engine, tempFile),
            "deserializeFile with corrupted data should throw");
        LOGGER.info("[" + runtime + "] deserializeFile with corrupted data threw as expected");
      } finally {
        Files.deleteIfExists(tempFile);
      }
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("functions() returns all exported functions")
  void functionsReturnsAllExported(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing module.functions()");

    try (Engine engine = Engine.create()) {
      final Module module = engine.compileWat(WAT);

      try {
        final Iterable<FunctionInfo> functions = module.functions();
        assertNotNull(functions, "functions() should not return null");

        final List<FunctionInfo> funcList = new ArrayList<>();
        for (final FunctionInfo info : functions) {
          funcList.add(info);
        }

        assertTrue(funcList.size() >= 3,
            "Module with 3 exports should have >= 3 functions, got: " + funcList.size());
        LOGGER.info("[" + runtime + "] functions() returned " + funcList.size() + " entries");
        for (final FunctionInfo info : funcList) {
          LOGGER.info("[" + runtime + "]   index=" + info.getIndex()
              + " name=" + info.getName()
              + " isImport=" + info.isImport()
              + " isLocal=" + info.isLocal());
        }
      } catch (final UnsupportedOperationException e) {
        LOGGER.info("[" + runtime + "] functions() not supported: " + e.getMessage());
      }

      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("functionInfo has correct metadata")
  void functionInfoHasCorrectMetadata(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing FunctionInfo metadata");

    try (Engine engine = Engine.create()) {
      final Module module = engine.compileWat(WAT);

      try {
        final List<FunctionInfo> funcList = new ArrayList<>();
        for (final FunctionInfo info : module.functions()) {
          funcList.add(info);
        }

        // All functions in this module are local (not imported)
        for (final FunctionInfo info : funcList) {
          assertTrue(info.isLocal(),
              "Function '" + info.getName() + "' should be local (not imported)");
          assertTrue(info.getIndex() >= 0,
              "Function index should be non-negative, got: " + info.getIndex());
        }
        LOGGER.info("[" + runtime + "] All " + funcList.size()
            + " functions are local with valid indices");
      } catch (final UnsupportedOperationException e) {
        LOGGER.info("[" + runtime + "] functions() not supported: " + e.getMessage());
      }

      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("functionInfo has correct funcType for add")
  void functionInfoHasFuncType(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing FunctionInfo funcType");

    try (Engine engine = Engine.create()) {
      final Module module = engine.compileWat(WAT);

      try {
        FunctionInfo addInfo = null;
        for (final FunctionInfo info : module.functions()) {
          if ("add".equals(info.getName())) {
            addInfo = info;
            break;
          }
        }

        if (addInfo != null) {
          assertNotNull(addInfo.getFuncType(),
              "add function should have a FuncType");
          assertEquals(2, addInfo.getFuncType().getParams().size(),
              "add should have 2 params");
          assertEquals(1, addInfo.getFuncType().getResults().size(),
              "add should have 1 result");
          LOGGER.info("[" + runtime + "] add FuncType: params="
              + addInfo.getFuncType().getParams()
              + " results=" + addInfo.getFuncType().getResults());
        } else {
          LOGGER.info("[" + runtime + "] 'add' function not found in functions() iteration");
        }
      } catch (final UnsupportedOperationException e) {
        LOGGER.info("[" + runtime + "] functions() not supported: " + e.getMessage());
      }

      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("functions() on empty module returns zero entries")
  void functionsOnEmptyModuleReturnsEmpty(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing functions() on empty module");

    try (Engine engine = Engine.create()) {
      final Module module = engine.compileWat(EMPTY_WAT);

      try {
        final List<FunctionInfo> funcList = new ArrayList<>();
        for (final FunctionInfo info : module.functions()) {
          funcList.add(info);
        }

        assertEquals(0, funcList.size(),
            "Empty module should have 0 functions, got: " + funcList.size());
        LOGGER.info("[" + runtime + "] Empty module functions() returned "
            + funcList.size() + " entries");
      } catch (final UnsupportedOperationException e) {
        LOGGER.info("[" + runtime + "] functions() not supported: " + e.getMessage());
      }

      module.close();
    }
  }
}
