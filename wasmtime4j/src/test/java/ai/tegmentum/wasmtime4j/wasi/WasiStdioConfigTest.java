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

package ai.tegmentum.wasmtime4j.wasi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Modifier;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link WasiStdioConfig} class.
 *
 * <p>WasiStdioConfig provides configuration options for WASI standard I/O streams.
 */
@DisplayName("WasiStdioConfig Tests")
class WasiStdioConfigTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be public final class")
    void shouldBePublicFinalClass() {
      assertTrue(
          Modifier.isPublic(WasiStdioConfig.class.getModifiers()),
          "WasiStdioConfig should be public");
      assertTrue(
          Modifier.isFinal(WasiStdioConfig.class.getModifiers()),
          "WasiStdioConfig should be final");
      assertFalse(
          WasiStdioConfig.class.isInterface(), "WasiStdioConfig should not be an interface");
    }

    @Test
    @DisplayName("should have Type nested enum")
    void shouldHaveTypeNestedEnum() {
      final var nestedClasses = WasiStdioConfig.class.getDeclaredClasses();
      boolean found = false;
      for (var nestedClass : nestedClasses) {
        if (nestedClass.getSimpleName().equals("Type")) {
          found = true;
          assertTrue(nestedClass.isEnum(), "Type should be an enum");
          break;
        }
      }
      assertTrue(found, "Should have Type nested enum");
    }
  }

  @Nested
  @DisplayName("Type Enum Tests")
  class TypeEnumTests {

    @Test
    @DisplayName("Type should have all expected values")
    void typeShouldHaveAllExpectedValues() {
      final WasiStdioConfig.Type[] values = WasiStdioConfig.Type.values();
      assertEquals(6, values.length, "Type should have 6 values");

      assertNotNull(WasiStdioConfig.Type.valueOf("INHERIT"), "INHERIT should exist");
      assertNotNull(WasiStdioConfig.Type.valueOf("INPUT_STREAM"), "INPUT_STREAM should exist");
      assertNotNull(WasiStdioConfig.Type.valueOf("OUTPUT_STREAM"), "OUTPUT_STREAM should exist");
      assertNotNull(WasiStdioConfig.Type.valueOf("FILE"), "FILE should exist");
      assertNotNull(WasiStdioConfig.Type.valueOf("FILE_APPEND"), "FILE_APPEND should exist");
      assertNotNull(WasiStdioConfig.Type.valueOf("NULL"), "NULL should exist");
    }
  }

  @Nested
  @DisplayName("Static Factory Method Tests")
  class StaticFactoryMethodTests {

    @Test
    @DisplayName("inherit should create INHERIT config")
    void inheritShouldCreateInheritConfig() {
      final WasiStdioConfig config = WasiStdioConfig.inherit();

      assertNotNull(config, "Config should not be null");
      assertEquals(WasiStdioConfig.Type.INHERIT, config.getType(), "Type should be INHERIT");
      assertNull(config.getTarget(), "Target should be null for inherit");
    }

    @Test
    @DisplayName("nulled should create NULL config")
    void nulledShouldCreateNullConfig() {
      final WasiStdioConfig config = WasiStdioConfig.nulled();

      assertNotNull(config, "Config should not be null");
      assertEquals(WasiStdioConfig.Type.NULL, config.getType(), "Type should be NULL");
      assertNull(config.getTarget(), "Target should be null for nulled");
    }

    @Test
    @DisplayName("fromInputStream should create INPUT_STREAM config")
    void fromInputStreamShouldCreateInputStreamConfig() {
      final InputStream is = new ByteArrayInputStream("test".getBytes());
      final WasiStdioConfig config = WasiStdioConfig.fromInputStream(is);

      assertNotNull(config, "Config should not be null");
      assertEquals(
          WasiStdioConfig.Type.INPUT_STREAM, config.getType(), "Type should be INPUT_STREAM");
      assertSame(is, config.getTarget(), "Target should be the input stream");
    }

    @Test
    @DisplayName("fromInputStream should throw for null")
    void fromInputStreamShouldThrowForNull() {
      assertThrows(
          IllegalArgumentException.class,
          () -> WasiStdioConfig.fromInputStream(null),
          "Should throw for null input stream");
    }

    @Test
    @DisplayName("fromOutputStream should create OUTPUT_STREAM config")
    void fromOutputStreamShouldCreateOutputStreamConfig() {
      final OutputStream os = new ByteArrayOutputStream();
      final WasiStdioConfig config = WasiStdioConfig.fromOutputStream(os);

      assertNotNull(config, "Config should not be null");
      assertEquals(
          WasiStdioConfig.Type.OUTPUT_STREAM, config.getType(), "Type should be OUTPUT_STREAM");
      assertSame(os, config.getTarget(), "Target should be the output stream");
    }

    @Test
    @DisplayName("fromOutputStream should throw for null")
    void fromOutputStreamShouldThrowForNull() {
      assertThrows(
          IllegalArgumentException.class,
          () -> WasiStdioConfig.fromOutputStream(null),
          "Should throw for null output stream");
    }

    @Test
    @DisplayName("fromFile should create FILE config")
    void fromFileShouldCreateFileConfig() {
      final Path path = Paths.get("/tmp/test.txt");
      final WasiStdioConfig config = WasiStdioConfig.fromFile(path);

      assertNotNull(config, "Config should not be null");
      assertEquals(WasiStdioConfig.Type.FILE, config.getType(), "Type should be FILE");
      assertSame(path, config.getTarget(), "Target should be the path");
    }

    @Test
    @DisplayName("fromFile should throw for null")
    void fromFileShouldThrowForNull() {
      assertThrows(
          IllegalArgumentException.class,
          () -> WasiStdioConfig.fromFile(null),
          "Should throw for null file path");
    }

    @Test
    @DisplayName("appendToFile should create FILE_APPEND config")
    void appendToFileShouldCreateFileAppendConfig() {
      final Path path = Paths.get("/tmp/append.txt");
      final WasiStdioConfig config = WasiStdioConfig.appendToFile(path);

      assertNotNull(config, "Config should not be null");
      assertEquals(
          WasiStdioConfig.Type.FILE_APPEND, config.getType(), "Type should be FILE_APPEND");
      assertSame(path, config.getTarget(), "Target should be the path");
    }

    @Test
    @DisplayName("appendToFile should throw for null")
    void appendToFileShouldThrowForNull() {
      assertThrows(
          IllegalArgumentException.class,
          () -> WasiStdioConfig.appendToFile(null),
          "Should throw for null file path");
    }
  }

  @Nested
  @DisplayName("Type-Specific Getter Tests")
  class TypeSpecificGetterTests {

    @Test
    @DisplayName("getInputStream should return input stream for INPUT_STREAM type")
    void getInputStreamShouldReturnInputStreamForInputStreamType() {
      final InputStream is = new ByteArrayInputStream("data".getBytes());
      final WasiStdioConfig config = WasiStdioConfig.fromInputStream(is);

      assertSame(is, config.getInputStream(), "Should return the input stream");
    }

    @Test
    @DisplayName("getInputStream should throw for non-INPUT_STREAM type")
    void getInputStreamShouldThrowForNonInputStreamType() {
      final WasiStdioConfig config = WasiStdioConfig.inherit();

      assertThrows(
          IllegalStateException.class,
          () -> config.getInputStream(),
          "Should throw for non-INPUT_STREAM type");
    }

    @Test
    @DisplayName("getOutputStream should return output stream for OUTPUT_STREAM type")
    void getOutputStreamShouldReturnOutputStreamForOutputStreamType() {
      final OutputStream os = new ByteArrayOutputStream();
      final WasiStdioConfig config = WasiStdioConfig.fromOutputStream(os);

      assertSame(os, config.getOutputStream(), "Should return the output stream");
    }

    @Test
    @DisplayName("getOutputStream should throw for non-OUTPUT_STREAM type")
    void getOutputStreamShouldThrowForNonOutputStreamType() {
      final WasiStdioConfig config = WasiStdioConfig.inherit();

      assertThrows(
          IllegalStateException.class,
          () -> config.getOutputStream(),
          "Should throw for non-OUTPUT_STREAM type");
    }

    @Test
    @DisplayName("getFilePath should return path for FILE type")
    void getFilePathShouldReturnPathForFileType() {
      final Path path = Paths.get("/tmp/file.log");
      final WasiStdioConfig config = WasiStdioConfig.fromFile(path);

      assertSame(path, config.getFilePath(), "Should return the file path");
    }

    @Test
    @DisplayName("getFilePath should return path for FILE_APPEND type")
    void getFilePathShouldReturnPathForFileAppendType() {
      final Path path = Paths.get("/tmp/append.log");
      final WasiStdioConfig config = WasiStdioConfig.appendToFile(path);

      assertSame(path, config.getFilePath(), "Should return the file path");
    }

    @Test
    @DisplayName("getFilePath should throw for non-file type")
    void getFilePathShouldThrowForNonFileType() {
      final WasiStdioConfig config = WasiStdioConfig.inherit();

      assertThrows(
          IllegalStateException.class,
          () -> config.getFilePath(),
          "Should throw for non-file type");
    }
  }

  @Nested
  @DisplayName("toString Tests")
  class ToStringTests {

    @Test
    @DisplayName("toString should include type")
    void toStringShouldIncludeType() {
      final WasiStdioConfig config = WasiStdioConfig.inherit();
      final String str = config.toString();

      assertTrue(str.contains("INHERIT"), "Should contain type");
      assertTrue(str.contains("WasiStdioConfig"), "Should contain class name");
    }

    @Test
    @DisplayName("toString should include target for file config")
    void toStringShouldIncludeTargetForFileConfig() {
      final Path path = Paths.get("/tmp/test.txt");
      final WasiStdioConfig config = WasiStdioConfig.fromFile(path);
      final String str = config.toString();

      assertTrue(str.contains("FILE"), "Should contain type");
      assertTrue(str.contains(path.toString()), "Should contain file path");
    }
  }
}
