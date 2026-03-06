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
package ai.tegmentum.wasmtime4j;

import static org.assertj.core.api.Assertions.assertThat;

import ai.tegmentum.wasmtime4j.tests.framework.DualRuntimeTest;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

/**
 * Integration tests for Module property accessors (imageRange, exports, serialization).
 *
 * <p>Validates that module metadata methods return correct results across both JNI and Panama
 * implementations.
 */
class ModulePropertiesTest extends DualRuntimeTest {

  private static final Logger LOGGER = Logger.getLogger(ModulePropertiesTest.class.getName());

  private static final String SIMPLE_WAT =
      "(module\n" + "  (func (export \"get42\") (result i32) i32.const 42)\n" + ")";

  private static final String MULTI_EXPORT_WAT =
      "(module\n"
          + "  (memory (export \"memory\") 1)\n"
          + "  (func (export \"add\") (param i32 i32) (result i32)\n"
          + "    local.get 0\n"
          + "    local.get 1\n"
          + "    i32.add\n"
          + "  )\n"
          + "  (func (export \"sub\") (param i32 i32) (result i32)\n"
          + "    local.get 0\n"
          + "    local.get 1\n"
          + "    i32.sub\n"
          + "  )\n"
          + "  (global (export \"answer\") i32 (i32.const 42))\n"
          + ")";

  @AfterEach
  void tearDown() {
    clearRuntimeSelection();
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  void testModuleImageRange(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing Module.imageRange()");
    try (Engine engine = Engine.create()) {
      final Module module = Module.compile(engine, SIMPLE_WAT.getBytes(StandardCharsets.UTF_8));
      final ImageRange range = module.imageRange();

      assertThat(range).as("imageRange should not be null").isNotNull();
      LOGGER.info(
          "["
              + runtime
              + "] ImageRange: start=0x"
              + Long.toHexString(range.getStart())
              + ", end=0x"
              + Long.toHexString(range.getEnd())
              + ", size="
              + range.getSize());

      assertThat(range.getSize()).as("Image range size should be positive").isGreaterThan(0);
      assertThat(range.getEnd())
          .as("End should be >= start")
          .isGreaterThanOrEqualTo(range.getStart());

      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  void testModuleHasExport(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing Module.hasExport()");
    try (Engine engine = Engine.create()) {
      final Module module =
          Module.compile(engine, MULTI_EXPORT_WAT.getBytes(StandardCharsets.UTF_8));

      assertThat(module.hasExport("add")).as("Module should export 'add'").isTrue();
      assertThat(module.hasExport("sub")).as("Module should export 'sub'").isTrue();
      assertThat(module.hasExport("memory")).as("Module should export 'memory'").isTrue();
      assertThat(module.hasExport("answer")).as("Module should export 'answer'").isTrue();
      assertThat(module.hasExport("nonexistent"))
          .as("Module should not export 'nonexistent'")
          .isFalse();

      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  void testModuleSerializeDeserialize(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing Module serialize/deserialize round-trip");
    try (Engine engine = Engine.create()) {
      final Module original = Module.compile(engine, SIMPLE_WAT.getBytes(StandardCharsets.UTF_8));

      final byte[] serialized = original.serialize();
      assertThat(serialized)
          .as("Serialized bytes should not be null or empty")
          .isNotNull()
          .isNotEmpty();
      LOGGER.info("[" + runtime + "] Serialized module size: " + serialized.length + " bytes");

      final Module deserialized = Module.deserialize(engine, serialized);
      assertThat(deserialized).as("Deserialized module should not be null").isNotNull();
      assertThat(deserialized.isValid()).as("Deserialized module should be valid").isTrue();

      // Verify the deserialized module can be instantiated
      try (Store store = Store.create(engine)) {
        final Instance instance = Instance.create(store, deserialized);
        assertThat(instance).as("Should instantiate from deserialized module").isNotNull();
      }

      original.close();
      deserialized.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  void testModuleExports(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing Module.getExports()");
    try (Engine engine = Engine.create()) {
      final Module module =
          Module.compile(engine, MULTI_EXPORT_WAT.getBytes(StandardCharsets.UTF_8));

      final java.util.List<ai.tegmentum.wasmtime4j.type.ExportType> exports = module.getExports();
      assertThat(exports).as("Module should have 4 exports").hasSize(4);

      final java.util.List<String> names =
          exports.stream()
              .map(ai.tegmentum.wasmtime4j.type.ExportType::getName)
              .collect(java.util.stream.Collectors.toList());
      assertThat(names)
          .as("Export names should match")
          .containsExactlyInAnyOrder("memory", "add", "sub", "answer");

      LOGGER.info("[" + runtime + "] Export names: " + names);
      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  void testModuleIsValid(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing Module.isValid() lifecycle");
    try (Engine engine = Engine.create()) {
      final Module module = Module.compile(engine, SIMPLE_WAT.getBytes(StandardCharsets.UTF_8));

      assertThat(module.isValid()).as("Module should be valid after compilation").isTrue();

      module.close();
      assertThat(module.isValid()).as("Module should be invalid after close").isFalse();
    }
  }
}
