/*
 * Copyright 2024 Tegmentum AI. All rights reserved.
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

package ai.tegmentum.wasmtime4j.bindgen.introspection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ai.tegmentum.wasmtime4j.bindgen.BindgenException;
import ai.tegmentum.wasmtime4j.bindgen.model.BindgenModel;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Tests for {@link WasmIntrospector}. */
@DisplayName("WasmIntrospector Tests")
class WasmIntrospectorTest {

  private static final Logger LOGGER = Logger.getLogger(WasmIntrospectorTest.class.getName());

  // Minimal valid WASM module (magic number + version + empty sections)
  private static final byte[] VALID_WASM_HEADER = {
    0x00, 0x61, 0x73, 0x6D, // \0asm magic
    0x01, 0x00, 0x00, 0x00 // version 1
  };

  private WasmIntrospector introspector;

  @BeforeEach
  void setUp() {
    introspector = new WasmIntrospector();
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should create introspector with default constructor")
    void shouldCreateIntrospectorWithDefaultConstructor() {
      LOGGER.info("Testing default constructor");

      WasmIntrospector inst = new WasmIntrospector();

      assertThat(inst).isNotNull();
    }
  }

  @Nested
  @DisplayName("Valid WASM Introspection Tests")
  class ValidWasmIntrospectionTests {

    @Test
    @DisplayName("should introspect valid WASM module")
    void shouldIntrospectValidWasmModule(@TempDir Path tempDir) throws Exception {
      LOGGER.info("Testing introspection of valid WASM module");

      Path wasmFile = tempDir.resolve("test-module.wasm");
      Files.write(wasmFile, VALID_WASM_HEADER);

      BindgenModel model = introspector.introspect(wasmFile);

      assertThat(model).isNotNull();
      assertThat(model.getName()).isEqualTo("TestModule");
      assertThat(model.getInterfaces()).isNotEmpty();
    }

    @Test
    @DisplayName("should extract module name from file path")
    void shouldExtractModuleNameFromFilePath(@TempDir Path tempDir) throws Exception {
      LOGGER.info("Testing module name extraction");

      Path wasmFile = tempDir.resolve("my-awesome-module.wasm");
      Files.write(wasmFile, VALID_WASM_HEADER);

      BindgenModel model = introspector.introspect(wasmFile);

      assertThat(model.getName()).isEqualTo("MyAwesomeModule");
    }

    @Test
    @DisplayName("should convert kebab-case filename to PascalCase")
    void shouldConvertKebabCaseToPascalCase(@TempDir Path tempDir) throws Exception {
      Path wasmFile = tempDir.resolve("my-kebab-case-name.wasm");
      Files.write(wasmFile, VALID_WASM_HEADER);

      BindgenModel model = introspector.introspect(wasmFile);

      assertThat(model.getName()).isEqualTo("MyKebabCaseName");
    }

    @Test
    @DisplayName("should convert snake_case filename to PascalCase")
    void shouldConvertSnakeCaseToPascalCase(@TempDir Path tempDir) throws Exception {
      Path wasmFile = tempDir.resolve("my_snake_case_name.wasm");
      Files.write(wasmFile, VALID_WASM_HEADER);

      BindgenModel model = introspector.introspect(wasmFile);

      assertThat(model.getName()).isEqualTo("MySnakeCaseName");
    }

    @Test
    @DisplayName("should create exports interface")
    void shouldCreateExportsInterface(@TempDir Path tempDir) throws Exception {
      LOGGER.info("Testing exports interface creation");

      Path wasmFile = tempDir.resolve("example.wasm");
      Files.write(wasmFile, VALID_WASM_HEADER);

      BindgenModel model = introspector.introspect(wasmFile);

      assertThat(model.getInterfaces()).anyMatch(iface -> iface.getName().equals("ExampleExports"));
    }

    @Test
    @DisplayName("should record source file in model")
    void shouldRecordSourceFileInModel(@TempDir Path tempDir) throws Exception {
      Path wasmFile = tempDir.resolve("module.wasm");
      Files.write(wasmFile, VALID_WASM_HEADER);

      BindgenModel model = introspector.introspect(wasmFile);

      assertThat(model.getSourceFile()).isPresent();
      assertThat(model.getSourceFile().get()).contains("module.wasm");
    }
  }

  @Nested
  @DisplayName("Invalid WASM Tests")
  class InvalidWasmTests {

    @Test
    @DisplayName("should throw BindgenException for file too small")
    void shouldThrowForFileTooSmall(@TempDir Path tempDir) throws IOException {
      LOGGER.info("Testing exception for file too small");

      Path wasmFile = tempDir.resolve("small.wasm");
      Files.write(wasmFile, new byte[] {0x00, 0x61, 0x73}); // Only 3 bytes

      assertThatThrownBy(() -> introspector.introspect(wasmFile))
          .isInstanceOf(BindgenException.class)
          .hasMessageContaining("too small");
    }

    @Test
    @DisplayName("should throw BindgenException for invalid magic number")
    void shouldThrowForInvalidMagicNumber(@TempDir Path tempDir) throws IOException {
      LOGGER.info("Testing exception for invalid magic number");

      Path wasmFile = tempDir.resolve("invalid.wasm");
      byte[] invalidWasm = {
        0x01, 0x02, 0x03, 0x04, // Wrong magic
        0x01, 0x00, 0x00, 0x00 // version
      };
      Files.write(wasmFile, invalidWasm);

      assertThatThrownBy(() -> introspector.introspect(wasmFile))
          .isInstanceOf(BindgenException.class)
          .hasMessageContaining("Invalid WASM magic number");
    }

    @Test
    @DisplayName("should throw BindgenException for non-existent file")
    void shouldThrowForNonExistentFile(@TempDir Path tempDir) {
      LOGGER.info("Testing exception for non-existent file");

      Path nonExistentFile = tempDir.resolve("does-not-exist.wasm");

      assertThatThrownBy(() -> introspector.introspect(nonExistentFile))
          .isInstanceOf(BindgenException.class)
          .hasMessageContaining("Failed to introspect WASM module");
    }

    @Test
    @DisplayName("should include file name in error message")
    void shouldIncludeFileNameInErrorMessage(@TempDir Path tempDir) throws IOException {
      Path wasmFile = tempDir.resolve("specific-file-name.wasm");
      Files.write(wasmFile, new byte[] {0x00}); // Too small

      assertThatThrownBy(() -> introspector.introspect(wasmFile))
          .isInstanceOf(BindgenException.class)
          .hasMessageContaining("specific-file-name.wasm");
    }
  }

  @Nested
  @DisplayName("Edge Cases Tests")
  class EdgeCasesTests {

    @Test
    @DisplayName("should handle WASM file without .wasm extension in path")
    void shouldHandleFileWithoutWasmExtension(@TempDir Path tempDir) throws Exception {
      LOGGER.info("Testing file without standard extension");

      // Even though filename doesn't end in .wasm, content is valid
      Path wasmFile = tempDir.resolve("module");
      Files.write(wasmFile, VALID_WASM_HEADER);

      BindgenModel model = introspector.introspect(wasmFile);

      assertThat(model).isNotNull();
      assertThat(model.getName()).isEqualTo("Module");
    }

    @Test
    @DisplayName("should handle single character module name")
    void shouldHandleSingleCharModuleName(@TempDir Path tempDir) throws Exception {
      Path wasmFile = tempDir.resolve("a.wasm");
      Files.write(wasmFile, VALID_WASM_HEADER);

      BindgenModel model = introspector.introspect(wasmFile);

      assertThat(model.getName()).isEqualTo("A");
    }

    @Test
    @DisplayName("should handle module name with multiple consecutive delimiters")
    void shouldHandleMultipleDelimiters(@TempDir Path tempDir) throws Exception {
      Path wasmFile = tempDir.resolve("my--module__name.wasm");
      Files.write(wasmFile, VALID_WASM_HEADER);

      BindgenModel model = introspector.introspect(wasmFile);

      // Multiple delimiters should just be treated as single capitalizations
      assertThat(model.getName()).isNotEmpty();
    }

    @Test
    @DisplayName("should handle WASM with only header (minimal valid)")
    void shouldHandleMinimalValidWasm(@TempDir Path tempDir) throws Exception {
      LOGGER.info("Testing minimal valid WASM");

      Path wasmFile = tempDir.resolve("minimal.wasm");
      Files.write(wasmFile, VALID_WASM_HEADER);

      BindgenModel model = introspector.introspect(wasmFile);

      assertThat(model).isNotNull();
      // Should have at least the exports interface (even if empty)
      assertThat(model.getInterfaces()).isNotEmpty();
    }
  }

  @Nested
  @DisplayName("Integration Tests")
  class IntegrationTests {

    @Test
    @DisplayName("should produce model suitable for code generation")
    void shouldProduceModelSuitableForCodeGeneration(@TempDir Path tempDir) throws Exception {
      LOGGER.info("Testing model structure for code generation");

      Path wasmFile = tempDir.resolve("codegen-test.wasm");
      Files.write(wasmFile, VALID_WASM_HEADER);

      BindgenModel model = introspector.introspect(wasmFile);

      // Model should have all required fields for code generation
      assertThat(model.getName()).isNotNull();
      assertThat(model.getName()).isNotEmpty();
      assertThat(model.getInterfaces()).isNotNull();
      assertThat(model.getTypes()).isNotNull();
      assertThat(model.getFunctions()).isNotNull();
    }

    @Test
    @DisplayName("should create documentation for exports interface")
    void shouldCreateDocumentationForExportsInterface(@TempDir Path tempDir) throws Exception {
      Path wasmFile = tempDir.resolve("documented.wasm");
      Files.write(wasmFile, VALID_WASM_HEADER);

      BindgenModel model = introspector.introspect(wasmFile);

      assertThat(model.getInterfaces())
          .filteredOn(iface -> iface.getName().contains("Exports"))
          .allMatch(iface -> iface.getDocumentation().isPresent());
    }
  }
}
