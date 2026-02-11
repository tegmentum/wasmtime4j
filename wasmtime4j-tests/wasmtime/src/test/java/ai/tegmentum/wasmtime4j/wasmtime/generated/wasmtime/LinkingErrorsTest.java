/*
 * Copyright 2024 Tegmentum AI
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

package ai.tegmentum.wasmtime4j.wasmtime.generated.wasmtime;

import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.tests.framework.DualRuntimeTest;
import ai.tegmentum.wasmtime4j.wasmtime.framework.WastTestRunner;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

/**
 * Generated test from WAST file: linking-errors.wast
 *
 * <p>This test validates that linking errors are properly detected when importing incompatible
 * types (global, table, memory, and function mismatches).
 *
 * <p>Source:
 * https://github.com/bytecodealliance/wasmtime/blob/main/tests/misc_testsuite/linking-errors.wast
 */
public final class LinkingErrorsTest extends DualRuntimeTest {
  private static String loadResource(final String path) throws IOException {
    try (final InputStream is = LinkingErrorsTest.class.getResourceAsStream(path)) {
      if (is == null) {
        throw new IOException("Resource not found: " + path);
      }
      return new String(is.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("global type mismatch - i32 vs i64")
  public void testGlobalTypeMismatch(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);

    try (final WastTestRunner runner = new WastTestRunner()) {
      // First compile and instantiate the exporter module
      final String exporterWat =
          loadResource(
              "/ai/tegmentum/wasmtime4j/wasmtime/generated/wasmtime/"
                  + "LinkingErrorsTest_exporter.wat");
      runner.compileAndInstantiate(exporterWat);
      runner.registerModule("m");

      // Try to import with wrong global type - should fail
      final String importerWat = "(module (import \"m\" \"g i32\" (global i64)))";
      runner.assertUnlinkable(importerWat, null);
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("global mutability mismatch")
  public void testGlobalMutabilityMismatch(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);

    try (final WastTestRunner runner = new WastTestRunner()) {
      final String exporterWat =
          loadResource(
              "/ai/tegmentum/wasmtime4j/wasmtime/generated/wasmtime/"
                  + "LinkingErrorsTest_exporter.wat");
      runner.compileAndInstantiate(exporterWat);
      runner.registerModule("m");

      // Try to import mutable global as immutable - should fail
      final String importerWat = "(module (import \"m\" \"g mut i32\" (global i32)))";
      runner.assertUnlinkable(importerWat, null);
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("table type mismatch - funcref vs externref")
  public void testTableTypeMismatch(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);

    try (final WastTestRunner runner = new WastTestRunner()) {
      final String exporterWat =
          loadResource(
              "/ai/tegmentum/wasmtime4j/wasmtime/generated/wasmtime/"
                  + "LinkingErrorsTest_exporter.wat");
      runner.compileAndInstantiate(exporterWat);
      runner.registerModule("m");

      // Try to import funcref table as externref - should fail
      final String importerWat = "(module (import \"m\" \"t funcref\" (table 0 externref)))";
      runner.assertUnlinkable(importerWat, null);
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("memory limits mismatch")
  public void testMemoryLimitsMismatch(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);

    try (final WastTestRunner runner = new WastTestRunner()) {
      final String exporterWat =
          loadResource(
              "/ai/tegmentum/wasmtime4j/wasmtime/generated/wasmtime/"
                  + "LinkingErrorsTest_exporter.wat");
      runner.compileAndInstantiate(exporterWat);
      runner.registerModule("m");

      // Try to import memory with larger minimum size than available - should fail
      final String importerWat = "(module (import \"m\" \"mem\" (memory 2)))";
      runner.assertUnlinkable(importerWat, null);
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("function signature mismatch - parameter type")
  public void testFunctionParamTypeMismatch(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);

    try (final WastTestRunner runner = new WastTestRunner()) {
      final String exporterWat =
          loadResource(
              "/ai/tegmentum/wasmtime4j/wasmtime/generated/wasmtime/"
                  + "LinkingErrorsTest_exporter.wat");
      runner.compileAndInstantiate(exporterWat);
      runner.registerModule("m");

      // Try to import function with wrong parameter type - should fail
      final String importerWat =
          "(module (import \"m\" \"f p1r2\" (func (param i32) (result i32 i64))))";
      runner.assertUnlinkable(importerWat, null);
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("function signature mismatch - return type")
  public void testFunctionReturnTypeMismatch(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);

    try (final WastTestRunner runner = new WastTestRunner()) {
      final String exporterWat =
          loadResource(
              "/ai/tegmentum/wasmtime4j/wasmtime/generated/wasmtime/"
                  + "LinkingErrorsTest_exporter.wat");
      runner.compileAndInstantiate(exporterWat);
      runner.registerModule("m");

      // Try to import function with wrong return type - should fail
      final String importerWat =
          "(module (import \"m\" \"f p1r2\" (func (param f32) (result i64 i64))))";
      runner.assertUnlinkable(importerWat, null);
    }
  }
}
