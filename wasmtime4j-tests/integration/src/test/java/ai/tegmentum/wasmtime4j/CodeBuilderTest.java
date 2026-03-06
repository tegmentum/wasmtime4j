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
import static org.junit.jupiter.api.Assertions.assertThrows;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.tests.framework.DualRuntimeTest;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

/**
 * Integration tests for the {@link CodeBuilder} API.
 *
 * <p>Validates that CodeBuilder can compile WebAssembly modules from binary and text formats, that
 * code hints work correctly, and that error cases are properly handled.
 */
class CodeBuilderTest extends DualRuntimeTest {

  private static final Logger LOGGER = Logger.getLogger(CodeBuilderTest.class.getName());

  private static final String SIMPLE_WAT =
      "(module\n" + "  (func (export \"get42\") (result i32) i32.const 42)\n" + ")";

  private static final String MEMORY_WAT =
      "(module\n"
          + "  (memory (export \"memory\") 1)\n"
          + "  (func (export \"load\") (param $offset i32) (result i32)\n"
          + "    local.get $offset\n"
          + "    i32.load\n"
          + "  )\n"
          + ")";

  @AfterEach
  void tearDown() {
    clearRuntimeSelection();
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  void testCompileModuleFromWasmBinary(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing CodeBuilder.wasmBinary() + compileModule()");
    try (Engine engine = Engine.create()) {
      // Minimal valid wasm binary: magic + version (empty module)
      final byte[] binary = new byte[] {0x00, 0x61, 0x73, 0x6d, 0x01, 0x00, 0x00, 0x00};

      try (CodeBuilder builder = engine.codeBuilder()) {
        assertThat(builder).isNotNull();
        final CodeBuilder returned = builder.wasmBinary(binary);
        assertThat(returned).as("wasmBinary should return the same builder").isSameAs(builder);

        final Module module = builder.compileModule();
        assertThat(module).isNotNull();
        module.close();
      }
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  void testCompileModuleFromWasmBinaryOrText(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing CodeBuilder.wasmBinaryOrText() with WAT text");
    try (Engine engine = Engine.create()) {
      try (CodeBuilder builder = engine.codeBuilder()) {
        builder.wasmBinaryOrText(SIMPLE_WAT.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        final Module module = builder.compileModule();
        assertThat(module).isNotNull();
        assertThat(module.hasExport("get42")).as("Module should export 'get42'").isTrue();
        module.close();
      }
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  void testCompileModuleWithMemory(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing CodeBuilder with memory module");
    try (Engine engine = Engine.create()) {
      try (CodeBuilder builder = engine.codeBuilder()) {
        builder.wasmBinaryOrText(MEMORY_WAT.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        builder.hint(CodeHint.MODULE);
        final Module module = builder.compileModule();
        assertThat(module).isNotNull();
        assertThat(module.hasExport("memory")).as("Module should export 'memory'").isTrue();
        assertThat(module.hasExport("load")).as("Module should export 'load'").isTrue();
        module.close();
      }
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  void testHintModule(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing CodeBuilder.hint(MODULE)");
    try (Engine engine = Engine.create()) {
      try (CodeBuilder builder = engine.codeBuilder()) {
        final CodeBuilder returned = builder.hint(CodeHint.MODULE);
        assertThat(returned).as("hint should return the same builder").isSameAs(builder);
        builder.wasmBinaryOrText(SIMPLE_WAT.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        final Module module = builder.compileModule();
        assertThat(module).isNotNull();
        module.close();
      }
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  void testCompileWithInvalidBinaryFails(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing CodeBuilder with invalid binary");
    try (Engine engine = Engine.create()) {
      try (CodeBuilder builder = engine.codeBuilder()) {
        builder.wasmBinary(new byte[] {0x00, 0x01, 0x02, 0x03});
        assertThrows(
            WasmException.class,
            builder::compileModule,
            "Compiling invalid binary should throw WasmException");
      }
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  void testCompileWithoutSourceFails(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing CodeBuilder compile without setting source");
    try (Engine engine = Engine.create()) {
      try (CodeBuilder builder = engine.codeBuilder()) {
        assertThrows(
            Exception.class,
            builder::compileModule,
            "Compiling without source should throw an exception");
      }
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  void testCloseReleasesResources(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing CodeBuilder close releases resources");
    try (Engine engine = Engine.create()) {
      final CodeBuilder builder = engine.codeBuilder();
      builder.close();
      // After close, operations should fail
      assertThrows(
          Exception.class,
          () ->
              builder.wasmBinaryOrText(
                  SIMPLE_WAT.getBytes(java.nio.charset.StandardCharsets.UTF_8)),
          "Using CodeBuilder after close should throw");
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  void testFluentApiChaining(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing CodeBuilder fluent API chaining");
    try (Engine engine = Engine.create()) {
      try (CodeBuilder builder = engine.codeBuilder()) {
        // All setters should return the same builder for chaining
        final Module module =
            builder
                .hint(CodeHint.MODULE)
                .wasmBinaryOrText(SIMPLE_WAT.getBytes(java.nio.charset.StandardCharsets.UTF_8))
                .compileModule();
        assertThat(module).isNotNull();
        module.close();
      }
    }
  }
}
