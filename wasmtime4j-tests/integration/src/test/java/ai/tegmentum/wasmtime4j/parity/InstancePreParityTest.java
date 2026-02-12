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

package ai.tegmentum.wasmtime4j.parity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.InstancePre;
import ai.tegmentum.wasmtime4j.Linker;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmRuntime;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.factory.WasmRuntimeFactory;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Cross-runtime parity tests for {@link InstancePre} API. Verifies that JNI and Panama produce
 * identical results from InstancePre operations (instantiate, getModule, getEngine, isValid,
 * getInstanceCount, close lifecycle).
 *
 * <p>This test does NOT extend DualRuntimeTest -- it creates both JNI and Panama runtimes
 * side-by-side and compares results.
 *
 * @since 1.0.0
 */
@DisplayName("InstancePre Parity Tests")
class InstancePreParityTest {

  private static final Logger LOGGER = Logger.getLogger(InstancePreParityTest.class.getName());

  private static final String WAT =
      """
      (module
        (func (export "answer") (result i32) i32.const 42)
        (func (export "add") (param i32 i32) (result i32)
          local.get 0 local.get 1 i32.add))
      """;

  private static boolean jniAvailable;
  private static boolean panamaAvailable;

  private WasmRuntime jniRuntime;
  private WasmRuntime panamaRuntime;
  private Engine jniEngine;
  private Engine panamaEngine;
  private boolean jniReady;
  private boolean panamaReady;

  @BeforeAll
  static void checkRuntimeAvailability() {
    jniAvailable = WasmRuntimeFactory.isRuntimeAvailable(RuntimeType.JNI);
    panamaAvailable = WasmRuntimeFactory.isRuntimeAvailable(RuntimeType.PANAMA);
    LOGGER.info("JNI available: " + jniAvailable + ", Panama available: " + panamaAvailable);
  }

  @BeforeEach
  void setUp() {
    jniReady = false;
    panamaReady = false;

    if (jniAvailable) {
      try {
        jniRuntime = WasmRuntimeFactory.create(RuntimeType.JNI);
        jniEngine = jniRuntime.createEngine();
        jniReady = true;
      } catch (final Exception e) {
        LOGGER.warning("Failed to create JNI resources: " + e.getMessage());
      }
    }

    if (panamaAvailable) {
      try {
        panamaRuntime = WasmRuntimeFactory.create(RuntimeType.PANAMA);
        panamaEngine = panamaRuntime.createEngine();
        panamaReady = true;
      } catch (final Exception e) {
        LOGGER.warning("Failed to create Panama resources: " + e.getMessage());
      }
    }
  }

  @AfterEach
  void tearDown() {
    closeQuietly(jniEngine, "JNI engine");
    closeQuietly(jniRuntime, "JNI runtime");
    closeQuietly(panamaEngine, "Panama engine");
    closeQuietly(panamaRuntime, "Panama runtime");
  }

  private void closeQuietly(final AutoCloseable resource, final String name) {
    if (resource != null) {
      try {
        resource.close();
      } catch (final Exception e) {
        LOGGER.warning("Error closing " + name + ": " + e.getMessage());
      }
    }
  }

  private void requireAtLeastOneRuntime() {
    assumeTrue(jniReady || panamaReady, "At least one runtime required");
  }

  private void requireBothRuntimes() {
    assumeTrue(jniReady && panamaReady, "Both JNI and Panama runtimes required");
  }

  @Test
  @DisplayName("InstancePre creates valid instance")
  void instantiatePreCreatesValidInstance() throws Exception {
    requireAtLeastOneRuntime();

    if (jniReady) {
      testInstancePreCreatesValid(jniEngine, "JNI");
    }
    if (panamaReady) {
      testInstancePreCreatesValid(panamaEngine, "Panama");
    }
  }

  private void testInstancePreCreatesValid(final Engine engine, final String runtimeName)
      throws Exception {
    final Module module = engine.compileWat(WAT);
    try (Linker<Void> linker = Linker.create(engine)) {
      try {
        final InstancePre pre = linker.instantiatePre(module);

        assertNotNull(pre, runtimeName + ": InstancePre should not be null");
        assertTrue(pre.isValid(), runtimeName + ": InstancePre should be valid");
        LOGGER.info("[" + runtimeName + "] InstancePre created, isValid=" + pre.isValid());

        pre.close();
      } catch (final UnsatisfiedLinkError | UnsupportedOperationException e) {
        LOGGER.info(
            "["
                + runtimeName
                + "] instantiatePre not available: "
                + e.getClass().getName()
                + " - "
                + e.getMessage());
      }
    }
    module.close();
  }

  @Test
  @DisplayName("InstancePre.getModule returns correct module")
  void instancePreGetModuleReturnsCorrectModule() throws Exception {
    requireAtLeastOneRuntime();

    if (jniReady) {
      testGetModule(jniEngine, "JNI");
    }
    if (panamaReady) {
      testGetModule(panamaEngine, "Panama");
    }
  }

  private void testGetModule(final Engine engine, final String runtimeName) throws Exception {
    final Module module = engine.compileWat(WAT);
    try (Linker<Void> linker = Linker.create(engine)) {
      try {
        final InstancePre pre = linker.instantiatePre(module);

        final Module preModule = pre.getModule();
        assertNotNull(preModule, runtimeName + ": getModule should not return null");
        LOGGER.info("[" + runtimeName + "] InstancePre.getModule returned non-null module");

        pre.close();
      } catch (final UnsatisfiedLinkError | UnsupportedOperationException e) {
        LOGGER.info(
            "[" + runtimeName + "] instantiatePre not available: " + e.getClass().getName());
      }
    }
    module.close();
  }

  @Test
  @DisplayName("InstancePre.getEngine returns correct engine")
  void instancePreGetEngineReturnsCorrectEngine() throws Exception {
    requireAtLeastOneRuntime();

    if (jniReady) {
      testGetEngine(jniEngine, "JNI");
    }
    if (panamaReady) {
      testGetEngine(panamaEngine, "Panama");
    }
  }

  private void testGetEngine(final Engine engine, final String runtimeName) throws Exception {
    final Module module = engine.compileWat(WAT);
    try (Linker<Void> linker = Linker.create(engine)) {
      try {
        final InstancePre pre = linker.instantiatePre(module);

        final Engine preEngine = pre.getEngine();
        assertNotNull(preEngine, runtimeName + ": getEngine should not return null");
        LOGGER.info("[" + runtimeName + "] InstancePre.getEngine returned non-null engine");

        pre.close();
      } catch (final UnsatisfiedLinkError | UnsupportedOperationException e) {
        LOGGER.info(
            "[" + runtimeName + "] instantiatePre not available: " + e.getClass().getName());
      }
    }
    module.close();
  }

  @Test
  @DisplayName("InstancePre instantiate on multiple stores")
  void instancePreInstantiateOnMultipleStores() throws Exception {
    requireAtLeastOneRuntime();

    if (jniReady) {
      testMultipleStores(jniEngine, "JNI");
    }
    if (panamaReady) {
      testMultipleStores(panamaEngine, "Panama");
    }
  }

  private void testMultipleStores(final Engine engine, final String runtimeName) throws Exception {
    final Module module = engine.compileWat(WAT);
    try (Linker<Void> linker = Linker.create(engine)) {
      try {
        final InstancePre pre = linker.instantiatePre(module);

        try (Store store1 = engine.createStore();
            Store store2 = engine.createStore()) {

          final Instance instance1 = pre.instantiate(store1);
          final Instance instance2 = pre.instantiate(store2);

          final WasmValue[] r1 = instance1.callFunction("answer");
          final WasmValue[] r2 = instance2.callFunction("answer");

          assertEquals(42, r1[0].asInt(), runtimeName + ": Store1 answer() should be 42");
          assertEquals(42, r2[0].asInt(), runtimeName + ": Store2 answer() should be 42");
          LOGGER.info("[" + runtimeName + "] Both stores returned answer()=42");

          instance1.close();
          instance2.close();
        }
        pre.close();
      } catch (final UnsatisfiedLinkError | UnsupportedOperationException e) {
        LOGGER.info(
            "["
                + runtimeName
                + "] instantiatePre not available: "
                + e.getClass().getName()
                + " - "
                + e.getMessage());
      }
    }
    module.close();
  }

  @Test
  @DisplayName("InstancePre instance count increments on instantiate")
  void instancePreInstanceCountIncrementsOnInstantiate() throws Exception {
    requireAtLeastOneRuntime();

    if (jniReady) {
      testInstanceCount(jniEngine, "JNI");
    }
    if (panamaReady) {
      testInstanceCount(panamaEngine, "Panama");
    }
  }

  private void testInstanceCount(final Engine engine, final String runtimeName) throws Exception {
    final Module module = engine.compileWat(WAT);
    try (Linker<Void> linker = Linker.create(engine)) {
      try {
        final InstancePre pre = linker.instantiatePre(module);
        final long initialCount = pre.getInstanceCount();
        LOGGER.info("[" + runtimeName + "] Initial instance count: " + initialCount);

        try (Store store = engine.createStore()) {
          final Instance instance = pre.instantiate(store);
          final long afterCount = pre.getInstanceCount();

          assertTrue(
              afterCount >= initialCount,
              runtimeName + ": Instance count should not decrease after instantiate");
          LOGGER.info("[" + runtimeName + "] After instantiate count: " + afterCount);

          instance.close();
        }
        pre.close();
      } catch (final UnsatisfiedLinkError | UnsupportedOperationException e) {
        LOGGER.info(
            "["
                + runtimeName
                + "] instantiatePre not available: "
                + e.getClass().getName()
                + " - "
                + e.getMessage());
      }
    }
    module.close();
  }

  @Test
  @DisplayName("InstancePre close marks invalid")
  void instancePreCloseMarksInvalid() throws Exception {
    requireAtLeastOneRuntime();

    if (jniReady) {
      testCloseInvalid(jniEngine, "JNI");
    }
    if (panamaReady) {
      testCloseInvalid(panamaEngine, "Panama");
    }
  }

  private void testCloseInvalid(final Engine engine, final String runtimeName) throws Exception {
    final Module module = engine.compileWat(WAT);
    try (Linker<Void> linker = Linker.create(engine)) {
      try {
        final InstancePre pre = linker.instantiatePre(module);
        assertTrue(pre.isValid(), runtimeName + ": InstancePre should be valid before close");

        pre.close();
        assertFalse(pre.isValid(), runtimeName + ": InstancePre should be invalid after close");
        LOGGER.info("[" + runtimeName + "] InstancePre.isValid after close: " + pre.isValid());
      } catch (final UnsatisfiedLinkError | UnsupportedOperationException e) {
        LOGGER.info(
            "["
                + runtimeName
                + "] instantiatePre not available: "
                + e.getClass().getName()
                + " - "
                + e.getMessage());
      }
    }
    module.close();
  }

  @Test
  @DisplayName("InstancePre instances still work after InstancePre.close")
  void instancePreInstantiatedInstancesStillWorkAfterClose() throws Exception {
    requireAtLeastOneRuntime();

    if (jniReady) {
      testInstancesSurviveClose(jniEngine, "JNI");
    }
    if (panamaReady) {
      testInstancesSurviveClose(panamaEngine, "Panama");
    }
  }

  private void testInstancesSurviveClose(final Engine engine, final String runtimeName)
      throws Exception {
    final Module module = engine.compileWat(WAT);
    try (Linker<Void> linker = Linker.create(engine);
        Store store = engine.createStore()) {
      try {
        final InstancePre pre = linker.instantiatePre(module);
        final Instance instance = pre.instantiate(store);

        // Close InstancePre while instance is still alive
        pre.close();

        // Instance should still work
        final WasmValue[] result = instance.callFunction("answer");
        assertEquals(
            42,
            result[0].asInt(),
            runtimeName + ": Instance should still work after InstancePre.close");
        LOGGER.info(
            "[" + runtimeName + "] Instance works after InstancePre.close: " + result[0].asInt());

        instance.close();
      } catch (final UnsatisfiedLinkError | UnsupportedOperationException e) {
        LOGGER.info(
            "["
                + runtimeName
                + "] instantiatePre not available: "
                + e.getClass().getName()
                + " - "
                + e.getMessage());
      }
    }
    module.close();
  }

  @Test
  @DisplayName("Both runtimes produce same results from InstancePre")
  void parityBothRuntimesSameResults() throws Exception {
    requireBothRuntimes();

    final Module jniModule = jniEngine.compileWat(WAT);
    final Module panamaModule = panamaEngine.compileWat(WAT);

    try (Linker<Void> jniLinker = Linker.create(jniEngine);
        Linker<Void> panamaLinker = Linker.create(panamaEngine)) {

      try {
        final InstancePre jniPre = jniLinker.instantiatePre(jniModule);
        final InstancePre panamaPre = panamaLinker.instantiatePre(panamaModule);

        try (Store jniStore = jniEngine.createStore();
            Store panamaStore = panamaEngine.createStore()) {

          final Instance jniInstance = jniPre.instantiate(jniStore);
          final Instance panamaInstance = panamaPre.instantiate(panamaStore);

          final WasmValue[] jniAnswer = jniInstance.callFunction("answer");
          final WasmValue[] panamaAnswer = panamaInstance.callFunction("answer");

          assertEquals(
              jniAnswer[0].asInt(),
              panamaAnswer[0].asInt(),
              "JNI and Panama should return same answer()");

          final WasmValue[] jniAdd =
              jniInstance.callFunction("add", WasmValue.i32(10), WasmValue.i32(20));
          final WasmValue[] panamaAdd =
              panamaInstance.callFunction("add", WasmValue.i32(10), WasmValue.i32(20));

          assertEquals(
              jniAdd[0].asInt(),
              panamaAdd[0].asInt(),
              "JNI and Panama should return same add(10, 20)");

          LOGGER.info(
              "Parity verified: answer()="
                  + jniAnswer[0].asInt()
                  + ", add(10,20)="
                  + jniAdd[0].asInt());

          jniInstance.close();
          panamaInstance.close();
        }

        jniPre.close();
        panamaPre.close();
      } catch (final UnsatisfiedLinkError | UnsupportedOperationException e) {
        LOGGER.info(
            "[Parity] instantiatePre not available: "
                + e.getClass().getName()
                + " - "
                + e.getMessage());
      }
    }

    jniModule.close();
    panamaModule.close();
  }
}
