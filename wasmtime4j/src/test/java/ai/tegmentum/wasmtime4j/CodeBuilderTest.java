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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Tests for {@link CodeBuilder} fluent WASM module builder. */
@DisplayName("CodeBuilder")
final class CodeBuilderTest {

  private static final byte[] WASM_MAGIC = {0x00, 0x61, 0x73, 0x6D};
  private static final byte[] WASM_VERSION = {0x01, 0x00, 0x00, 0x00};

  @Nested
  @DisplayName("empty module build")
  final class EmptyModuleBuildTests {

    @Test
    @DisplayName("should build empty module with magic number and version")
    void shouldBuildEmptyModuleWithMagicAndVersion() throws WasmException {
      final byte[] bytes = new CodeBuilder().build();
      assertNotNull(bytes, "Built bytes should not be null");
      assertTrue(bytes.length >= 8, "Empty module should have at least 8 bytes (magic + version)");
      assertArrayEquals(
          WASM_MAGIC,
          new byte[] {bytes[0], bytes[1], bytes[2], bytes[3]},
          "First 4 bytes should be WASM magic number");
      assertArrayEquals(
          WASM_VERSION,
          new byte[] {bytes[4], bytes[5], bytes[6], bytes[7]},
          "Bytes 4-7 should be WASM version 1");
    }
  }

  @Nested
  @DisplayName("addType")
  final class AddTypeTests {

    @Test
    @DisplayName("should reject null funcType")
    void shouldRejectNullFuncType() {
      final CodeBuilder builder = new CodeBuilder();
      assertThrows(
          NullPointerException.class,
          () -> builder.addType(null),
          "Expected NullPointerException for null funcType");
    }
  }

  @Nested
  @DisplayName("addFunctionImport")
  final class AddFunctionImportTests {

    @Test
    @DisplayName("should reject null module name")
    void shouldRejectNullModuleName() {
      final CodeBuilder builder = new CodeBuilder();
      assertThrows(
          NullPointerException.class,
          () -> builder.addFunctionImport(null, "func", 0),
          "Expected NullPointerException for null module name");
    }

    @Test
    @DisplayName("should reject null field name")
    void shouldRejectNullFieldName() {
      final CodeBuilder builder = new CodeBuilder();
      assertThrows(
          NullPointerException.class,
          () -> builder.addFunctionImport("mod", null, 0),
          "Expected NullPointerException for null field name");
    }
  }

  @Nested
  @DisplayName("addMemoryImport")
  final class AddMemoryImportTests {

    @Test
    @DisplayName("should reject null module name for memory import")
    void shouldRejectNullModuleName() {
      final CodeBuilder builder = new CodeBuilder();
      assertThrows(
          NullPointerException.class,
          () -> builder.addMemoryImport(null, "memory", 1, -1),
          "Expected NullPointerException for null module name in memory import");
    }

    @Test
    @DisplayName("should accept valid memory import parameters")
    void shouldAcceptValidMemoryImport() {
      final CodeBuilder builder = new CodeBuilder();
      final CodeBuilder result = builder.addMemoryImport("env", "memory", 1, 10);
      assertNotNull(result, "addMemoryImport should return non-null builder for chaining");
    }
  }

  @Nested
  @DisplayName("addGlobalImport")
  final class AddGlobalImportTests {

    @Test
    @DisplayName("should reject null value type for global import")
    void shouldRejectNullValueType() {
      final CodeBuilder builder = new CodeBuilder();
      assertThrows(
          NullPointerException.class,
          () -> builder.addGlobalImport("env", "global", null, false),
          "Expected NullPointerException for null value type in global import");
    }

    @Test
    @DisplayName("should accept valid global import")
    void shouldAcceptValidGlobalImport() {
      final CodeBuilder builder = new CodeBuilder();
      final CodeBuilder result =
          builder.addGlobalImport("env", "myGlobal", WasmValueType.I32, true);
      assertNotNull(result, "addGlobalImport should return non-null builder");
    }
  }

  @Nested
  @DisplayName("addFunction")
  final class AddFunctionTests {

    @Test
    @DisplayName("should add function and build module with code section")
    void shouldAddFunctionAndBuildModule() throws WasmException {
      final byte[] body = new byte[] {0x20, 0x00, 0x20, 0x01, 0x6A}; // local.get 0, local.get 1, i32.add
      final byte[] module =
          new CodeBuilder()
              .addFunction(0, List.of(WasmValueType.I32, WasmValueType.I32), body)
              .build();
      assertNotNull(module, "Module with function should not be null");
      assertTrue(module.length > 8, "Module with function should be larger than empty module");
    }
  }

  @Nested
  @DisplayName("addMemory")
  final class AddMemoryTests {

    @Test
    @DisplayName("should add memory and build module")
    void shouldAddMemoryAndBuild() throws WasmException {
      final byte[] module = new CodeBuilder().addMemory(1, 10).build();
      assertNotNull(module, "Module with memory should not be null");
      assertTrue(module.length > 8, "Module with memory should be larger than empty module");
    }

    @Test
    @DisplayName("should add memory with no maximum")
    void shouldAddMemoryWithNoMaximum() throws WasmException {
      final byte[] module = new CodeBuilder().addMemory(1, -1).build();
      assertNotNull(module, "Module with unbounded memory should not be null");
    }
  }

  @Nested
  @DisplayName("addTable")
  final class AddTableTests {

    @Test
    @DisplayName("should add table with funcref element type")
    void shouldAddTableWithFuncref() throws WasmException {
      final byte[] module =
          new CodeBuilder().addTable(WasmValueType.FUNCREF, 0, 10).build();
      assertNotNull(module, "Module with table should not be null");
      assertTrue(module.length > 8, "Module with table should be larger than empty module");
    }
  }

  @Nested
  @DisplayName("addGlobal")
  final class AddGlobalTests {

    @Test
    @DisplayName("should add i32 global with initial value")
    void shouldAddI32Global() throws WasmException {
      final byte[] module =
          new CodeBuilder().addGlobal(WasmValueType.I32, false, 42).build();
      assertNotNull(module, "Module with global should not be null");
      assertTrue(module.length > 8, "Module with global should be larger than empty module");
    }

    @Test
    @DisplayName("should add i64 global")
    void shouldAddI64Global() throws WasmException {
      final byte[] module =
          new CodeBuilder().addGlobal(WasmValueType.I64, true, 100L).build();
      assertNotNull(module, "Module with i64 global should not be null");
    }

    @Test
    @DisplayName("should add f32 global")
    void shouldAddF32Global() throws WasmException {
      final byte[] module =
          new CodeBuilder().addGlobal(WasmValueType.F32, false, 0).build();
      assertNotNull(module, "Module with f32 global should not be null");
    }

    @Test
    @DisplayName("should add f64 global")
    void shouldAddF64Global() throws WasmException {
      final byte[] module =
          new CodeBuilder().addGlobal(WasmValueType.F64, false, 0).build();
      assertNotNull(module, "Module with f64 global should not be null");
    }
  }

  @Nested
  @DisplayName("addExport")
  final class AddExportTests {

    @Test
    @DisplayName("should reject null export name")
    void shouldRejectNullExportName() {
      final CodeBuilder builder = new CodeBuilder();
      assertThrows(
          NullPointerException.class,
          () -> builder.addExport(null, CodeBuilder.ExportKind.FUNCTION, 0),
          "Expected NullPointerException for null export name");
    }

    @Test
    @DisplayName("should reject null export kind")
    void shouldRejectNullExportKind() {
      final CodeBuilder builder = new CodeBuilder();
      assertThrows(
          NullPointerException.class,
          () -> builder.addExport("test", null, 0),
          "Expected NullPointerException for null export kind");
    }

    @Test
    @DisplayName("should add export with valid parameters")
    void shouldAddExportWithValidParams() throws WasmException {
      final byte[] module =
          new CodeBuilder()
              .addMemory(1, -1)
              .addExport("memory", CodeBuilder.ExportKind.MEMORY, 0)
              .build();
      assertNotNull(module, "Module with export should not be null");
    }
  }

  @Nested
  @DisplayName("writeTo OutputStream")
  final class WriteToOutputStreamTests {

    @Test
    @DisplayName("should write to output stream")
    void shouldWriteToOutputStream() throws IOException {
      final ByteArrayOutputStream baos = new ByteArrayOutputStream();
      new CodeBuilder().writeTo(baos);
      final byte[] bytes = baos.toByteArray();
      assertTrue(bytes.length >= 8, "Written output should contain at least magic + version");
    }
  }

  @Nested
  @DisplayName("writeTo Path")
  final class WriteToPathTests {

    @Test
    @DisplayName("should write module to file")
    void shouldWriteModuleToFile(@TempDir final Path tempDir) throws WasmException {
      final Path outputFile = tempDir.resolve("test.wasm");
      new CodeBuilder().addMemory(1, -1).writeTo(outputFile);
      assertTrue(Files.exists(outputFile), "Output file should exist");
    }

    @Test
    @DisplayName("should reject null path")
    void shouldRejectNullPath() {
      final CodeBuilder builder = new CodeBuilder();
      assertThrows(
          NullPointerException.class,
          () -> builder.writeTo((Path) null),
          "Expected NullPointerException for null path");
    }
  }

  @Nested
  @DisplayName("fluent builder chaining")
  final class FluentBuilderChainingTests {

    @Test
    @DisplayName("should support chaining multiple operations")
    void shouldSupportChainingMultipleOperations() {
      assertDoesNotThrow(
          () -> {
            final byte[] module =
                new CodeBuilder()
                    .addMemory(1, 10)
                    .addGlobal(WasmValueType.I32, false, 0)
                    .addExport("memory", CodeBuilder.ExportKind.MEMORY, 0)
                    .build();
            assertNotNull(module, "Chained builder should produce non-null module");
          },
          "Chaining multiple builder operations should not throw");
    }
  }

  @Nested
  @DisplayName("ExportKind and ImportKind enums")
  final class EnumTests {

    @Test
    @DisplayName("ExportKind should have four values")
    void exportKindShouldHaveFourValues() {
      assertEquals(
          4,
          CodeBuilder.ExportKind.values().length,
          "ExportKind should have 4 values: FUNCTION, TABLE, MEMORY, GLOBAL");
    }

    @Test
    @DisplayName("ImportKind should have four values")
    void importKindShouldHaveFourValues() {
      assertEquals(
          4,
          CodeBuilder.ImportKind.values().length,
          "ImportKind should have 4 values: FUNCTION, TABLE, MEMORY, GLOBAL");
    }
  }
}
