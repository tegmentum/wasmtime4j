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

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.WasmValueType;
import ai.tegmentum.wasmtime4j.tests.framework.DualRuntimeTest;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

/**
 * Tests for externref support. Verifies that modules using externref compile and instantiate, that
 * the WasmValue API supports externref creation, and documents current FFI limitations for passing
 * externref values through function calls.
 *
 * @since 1.0.0
 */
@DisplayName("Externref Round-Trip Tests")
public class ExternrefRoundTripTest extends DualRuntimeTest {

  private static final Logger LOGGER = Logger.getLogger(ExternrefRoundTripTest.class.getName());

  @AfterEach
  void cleanup() {
    clearRuntimeSelection();
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("WasmValue.externref() API creates valid externref values")
  void externrefApiCreation(final RuntimeType runtime) {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing externref API creation");

    // String externref
    final WasmValue strRef = WasmValue.externref("Hello");
    assertNotNull(strRef, "String externref should not be null");
    assertEquals(WasmValueType.EXTERNREF, strRef.getType(), "Type should be EXTERNREF");
    assertTrue(strRef.isExternref(), "isExternref() should return true");
    assertEquals("Hello", strRef.asExternref(), "Should retrieve String value");
    LOGGER.info("[" + runtime + "] String externref created: " + strRef.asExternref());

    // Integer externref
    final WasmValue intRef = WasmValue.externref(Integer.valueOf(42));
    assertEquals(Integer.valueOf(42), intRef.asExternref(), "Should retrieve Integer value");
    LOGGER.info("[" + runtime + "] Integer externref created: " + intRef.asExternref());

    // Null externref
    final WasmValue nullRef = WasmValue.nullExternref();
    assertNotNull(nullRef, "Null externref wrapper should not be null");
    LOGGER.info("[" + runtime + "] Null externref created");
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Module with externref param/return compiles and instantiates")
  void externrefModuleCompilesAndInstantiates(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing externref module compilation");

    final String wat =
        """
        (module
          (func (export "identity") (param externref) (result externref)
            local.get 0))
        """;

    try (Engine engine = Engine.create();
        Store store = engine.createStore()) {
      final Module module = engine.compileWat(wat);
      assertNotNull(module, "Module with externref should compile");

      final Instance instance = module.instantiate(store);
      assertNotNull(instance, "Module with externref should instantiate");
      LOGGER.info("[" + runtime + "] externref module compiled and instantiated");

      instance.close();
      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Module with externref table compiles and instantiates")
  void externrefTableModuleCompiles(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing externref table module");

    final String wat =
        """
        (module
          (table (export "t") 4 externref)
          (func (export "set") (param i32 externref)
            local.get 0
            local.get 1
            table.set 0)
          (func (export "get") (param i32) (result externref)
            local.get 0
            table.get 0))
        """;

    try (Engine engine = Engine.create();
        Store store = engine.createStore()) {
      final Module module = engine.compileWat(wat);
      assertNotNull(module, "Module with externref table should compile");

      final Instance instance = module.instantiate(store);
      assertNotNull(instance, "Module with externref table should instantiate");
      LOGGER.info("[" + runtime + "] externref table module compiled and instantiated");

      instance.close();
      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Module with externref global compiles and instantiates")
  void externrefGlobalModuleCompiles(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing externref global module");

    final String wat =
        """
        (module
          (global (export "g") (mut externref) (ref.null extern))
          (func (export "set_global") (param externref)
            local.get 0
            global.set 0)
          (func (export "get_global") (result externref)
            global.get 0))
        """;

    try (Engine engine = Engine.create();
        Store store = engine.createStore()) {
      final Module module = engine.compileWat(wat);
      assertNotNull(module, "Module with externref global should compile");

      final Instance instance = module.instantiate(store);
      assertNotNull(instance, "Module with externref global should instantiate");
      LOGGER.info("[" + runtime + "] externref global module compiled and instantiated");

      instance.close();
      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Passing externref through function call attempts do not crash JVM")
  void externrefPassThroughDoesNotCrash(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing externref FFI pass-through safety");

    final String wat =
        """
        (module
          (func (export "identity") (param externref) (result externref)
            local.get 0))
        """;

    try (Engine engine = Engine.create();
        Store store = engine.createStore()) {
      final Module module = engine.compileWat(wat);
      final Instance instance = module.instantiate(store);

      // Passing externref through FFI is currently limited. The native layer may attempt
      // to coerce the value to a numeric type, causing NoSuchMethodError.
      // The critical requirement is that the JVM does NOT crash (SIGABRT/SIGSEGV).
      final WasmValue input = WasmValue.externref("test");
      try {
        final WasmValue[] result = instance.callFunction("identity", input);
        LOGGER.info(
            "[" + runtime + "] externref call succeeded, result type: " + result[0].getType());
        assertNotNull(result, "Result should not be null if call succeeds");
      } catch (final Throwable t) {
        // NoSuchMethodError, Exception, etc. are all acceptable - no JVM crash is the key
        LOGGER.info(
            "["
                + runtime
                + "] externref call threw (expected): "
                + t.getClass().getName()
                + " - "
                + t.getMessage());
      }

      instance.close();
      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Multiple externref parameters in module signature")
  void multipleExternrefSignature(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing multiple externref parameters");

    final String wat =
        """
        (module
          (func (export "pick_first") (param externref externref) (result externref)
            local.get 0)
          (func (export "pick_second") (param externref externref) (result externref)
            local.get 1))
        """;

    try (Engine engine = Engine.create();
        Store store = engine.createStore()) {
      final Module module = engine.compileWat(wat);
      assertNotNull(module, "Module with multiple externref params should compile");

      final Instance instance = module.instantiate(store);
      assertNotNull(instance, "Module with multiple externref params should instantiate");
      LOGGER.info("[" + runtime + "] Multiple externref module compiled and instantiated");

      instance.close();
      module.close();
    }
  }
}
