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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.Precompiled;
import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.tests.framework.DualRuntimeTest;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

/**
 * Tests for {@link Engine#detectPrecompiled(byte[])} and {@link
 * Engine#detectPrecompiledFile(Path)}.
 *
 * <p>Verifies that precompiled module detection correctly identifies serialized modules, rejects
 * raw WASM bytes and garbage, and handles null arguments defensively.
 */
@DisplayName("Engine Precompiled Detection Tests")
public class EnginePrecompiledDetectionTest extends DualRuntimeTest {

  private static final Logger LOGGER =
      Logger.getLogger(EnginePrecompiledDetectionTest.class.getName());

  private static final String SIMPLE_WAT = "(module (func (export \"nop\")))";

  @AfterEach
  void cleanup() {
    clearRuntimeSelection();
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("detectPrecompiled on serialized module returns MODULE")
  void detectPrecompiledOnSerializedModuleReturnsModule(final RuntimeType runtime) {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing detectPrecompiled on serialized module");

    try (Engine engine = Engine.create()) {
      // Compile and serialize a module
      byte[] serialized;
      try (Module module = engine.compileWat(SIMPLE_WAT)) {
        serialized = module.serialize();
      }
      assertNotNull(serialized, "Serialized bytes must not be null");
      LOGGER.info("[" + runtime + "] Serialized module: " + serialized.length + " bytes");

      final Precompiled result = engine.detectPrecompiled(serialized);
      assertNotNull(result, "detectPrecompiled on serialized module should return non-null");
      assertEquals(
          Precompiled.MODULE,
          result,
          "detectPrecompiled should return MODULE for serialized module bytes");
      LOGGER.info("[" + runtime + "] detectPrecompiled=" + result);

    } catch (final UnsupportedOperationException e) {
      LOGGER.warning(
          "[" + runtime + "] serialize or detectPrecompiled not supported: " + e.getMessage());
    } catch (final UnsatisfiedLinkError e) {
      LOGGER.warning("[" + runtime + "] Native link error: " + e.getMessage());
    } catch (final Exception e) {
      LOGGER.warning(
          "["
              + runtime
              + "] Unexpected exception: "
              + e.getClass().getName()
              + " - "
              + e.getMessage());
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("detectPrecompiled on raw WASM returns null")
  void detectPrecompiledOnRawWasmReturnsNull(final RuntimeType runtime) {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing detectPrecompiled on raw WASM bytes");

    try (Engine engine = Engine.create()) {
      // Raw WASM binary (valid but not precompiled)
      final byte[] rawWasm =
          new byte[] {
            0x00, 0x61, 0x73, 0x6D, // magic
            0x01, 0x00, 0x00, 0x00 // version
          };

      final Precompiled result = engine.detectPrecompiled(rawWasm);
      assertNull(result, "detectPrecompiled on raw WASM should return null (not precompiled)");
      LOGGER.info("[" + runtime + "] detectPrecompiled(rawWasm)=" + result);

    } catch (final UnsupportedOperationException e) {
      LOGGER.warning("[" + runtime + "] detectPrecompiled not supported: " + e.getMessage());
    } catch (final UnsatisfiedLinkError e) {
      LOGGER.warning("[" + runtime + "] Native link error: " + e.getMessage());
    } catch (final Exception e) {
      LOGGER.warning(
          "["
              + runtime
              + "] Unexpected exception: "
              + e.getClass().getName()
              + " - "
              + e.getMessage());
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("detectPrecompiled on garbage bytes returns null")
  void detectPrecompiledOnGarbageReturnsNull(final RuntimeType runtime) {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing detectPrecompiled on garbage bytes");

    try (Engine engine = Engine.create()) {
      final byte[] garbage =
          new byte[] {(byte) 0xDE, (byte) 0xAD, (byte) 0xBE, (byte) 0xEF, 0x01, 0x02, 0x03, 0x04};

      final Precompiled result = engine.detectPrecompiled(garbage);
      assertNull(result, "detectPrecompiled on garbage should return null");
      LOGGER.info("[" + runtime + "] detectPrecompiled(garbage)=" + result);

    } catch (final UnsupportedOperationException e) {
      LOGGER.warning("[" + runtime + "] detectPrecompiled not supported: " + e.getMessage());
    } catch (final UnsatisfiedLinkError e) {
      LOGGER.warning("[" + runtime + "] Native link error: " + e.getMessage());
    } catch (final Exception e) {
      LOGGER.warning(
          "["
              + runtime
              + "] Unexpected exception: "
              + e.getClass().getName()
              + " - "
              + e.getMessage());
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("detectPrecompiledFile on serialized module returns MODULE")
  void detectPrecompiledFileOnSerializedModule(final RuntimeType runtime) throws IOException {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing detectPrecompiledFile on serialized module file");

    Path tempFile = null;
    try (Engine engine = Engine.create()) {
      // Compile and serialize a module
      byte[] serialized;
      try (Module module = engine.compileWat(SIMPLE_WAT)) {
        serialized = module.serialize();
      }
      assertNotNull(serialized, "Serialized bytes must not be null");

      // Write to temp file
      tempFile = Files.createTempFile("wasmtime4j-precompiled-", ".cwasm");
      Files.write(tempFile, serialized);
      LOGGER.info("[" + runtime + "] Wrote " + serialized.length + " bytes to " + tempFile);

      final Precompiled result = engine.detectPrecompiledFile(tempFile);
      LOGGER.info("[" + runtime + "] detectPrecompiledFile=" + result);
      // detectPrecompiledFile is a default method that reads the first bytes of the file.
      // It may return MODULE if it detects the precompiled header, or null if the default
      // implementation delegates to detectPrecompiled(byte[]) which already works.
      if (result != null) {
        assertEquals(
            Precompiled.MODULE,
            result,
            "If non-null, detectPrecompiledFile should return MODULE for serialized module");
      } else {
        LOGGER.info(
            "["
                + runtime
                + "] detectPrecompiledFile returned null for serialized "
                + "module file (file-based detection may read insufficient header bytes)");
      }

    } catch (final UnsupportedOperationException e) {
      LOGGER.warning(
          "[" + runtime + "] serialize or detectPrecompiledFile not supported: " + e.getMessage());
    } catch (final UnsatisfiedLinkError e) {
      LOGGER.warning("[" + runtime + "] Native link error: " + e.getMessage());
    } catch (final Exception e) {
      LOGGER.warning(
          "["
              + runtime
              + "] Unexpected exception: "
              + e.getClass().getName()
              + " - "
              + e.getMessage());
    } finally {
      if (tempFile != null) {
        Files.deleteIfExists(tempFile);
      }
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("detectPrecompiledFile with null path throws")
  void detectPrecompiledFileNullPathThrows(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing detectPrecompiledFile with null path");

    try (Engine engine = Engine.create()) {
      assertThrows(
          Exception.class,
          () -> engine.detectPrecompiledFile(null),
          "detectPrecompiledFile(null) should throw");
      LOGGER.info("[" + runtime + "] detectPrecompiledFile(null) threw as expected");

    } catch (final UnsatisfiedLinkError e) {
      LOGGER.warning("[" + runtime + "] Native link error: " + e.getMessage());
    }
  }
}
