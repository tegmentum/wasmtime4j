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
package ai.tegmentum.wasmtime4j.wasmtime.generated.custompagesizes;

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
 * Generated test from WAST file: custom-page-sizes/custom-page-sizes.wast
 *
 * <p>Tests custom page sizes for WebAssembly memories. Validates that page sizes of 1 byte and
 * 65536 bytes (default) are supported.
 *
 * <p>Requires: custom_page_sizes = true, multi_memory = true
 *
 * <p>Source:
 * https://github.com/bytecodealliance/wasmtime/blob/main/tests/misc_testsuite/custom-page-sizes/custom-page-sizes.wast
 */
public final class CustomPageSizesTest extends DualRuntimeTest {
  private static String loadResource(final String path) throws IOException {
    try (final InputStream is = CustomPageSizesTest.class.getResourceAsStream(path)) {
      if (is == null) {
        throw new IOException("Resource not found: " + path);
      }
      return new String(is.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Custom page size 1 byte - basic")
  public void testPageSize1Basic(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    try (final WastTestRunner runner = new WastTestRunner()) {
      final String moduleWat =
          loadResource(
              "/ai/tegmentum/wasmtime4j/wasmtime/generated/custompagesizes/CustomPageSizesTest_module1.wat");
      runner.compileAndInstantiate(moduleWat);
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Custom page size 65536 bytes - basic")
  public void testPageSize65536Basic(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    try (final WastTestRunner runner = new WastTestRunner()) {
      final String moduleWat =
          loadResource(
              "/ai/tegmentum/wasmtime4j/wasmtime/generated/custompagesizes/CustomPageSizesTest_module2.wat");
      runner.compileAndInstantiate(moduleWat);
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Custom page size 1 byte with maximum")
  public void testPageSize1WithMaximum(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    try (final WastTestRunner runner = new WastTestRunner()) {
      final String moduleWat =
          loadResource(
              "/ai/tegmentum/wasmtime4j/wasmtime/generated/custompagesizes/CustomPageSizesTest_module3.wat");
      runner.compileAndInstantiate(moduleWat);
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Custom page size 65536 with maximum")
  public void testPageSize65536WithMaximum(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    try (final WastTestRunner runner = new WastTestRunner()) {
      final String moduleWat =
          loadResource(
              "/ai/tegmentum/wasmtime4j/wasmtime/generated/custompagesizes/CustomPageSizesTest_module4.wat");
      runner.compileAndInstantiate(moduleWat);
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Custom page size 1 - memory operations")
  public void testPageSize1MemoryOperations(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    try (final WastTestRunner runner = new WastTestRunner()) {
      final String moduleWat =
          loadResource(
              "/ai/tegmentum/wasmtime4j/wasmtime/generated/custompagesizes/CustomPageSizesTest_module5.wat");
      runner.compileAndInstantiate(moduleWat);
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Custom page size 65536 - memory grow limits")
  public void testPageSize65536GrowLimits(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    try (final WastTestRunner runner = new WastTestRunner()) {
      final String moduleWat =
          loadResource(
              "/ai/tegmentum/wasmtime4j/wasmtime/generated/custompagesizes/CustomPageSizesTest_module6.wat");
      runner.compileAndInstantiate(moduleWat);
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Multi-memory with different page sizes - copy operations")
  public void testMultiMemoryDifferentPageSizes(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    try (final WastTestRunner runner = new WastTestRunner()) {
      final String moduleWat =
          loadResource(
              "/ai/tegmentum/wasmtime4j/wasmtime/generated/custompagesizes/CustomPageSizesTest_module7.wat");
      runner.compileAndInstantiate(moduleWat);
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Custom page size 1 - i64 load")
  public void testPageSize1I64Load(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    try (final WastTestRunner runner = new WastTestRunner()) {
      final String moduleWat =
          loadResource(
              "/ai/tegmentum/wasmtime4j/wasmtime/generated/custompagesizes/CustomPageSizesTest_module8.wat");
      runner.compileAndInstantiate(moduleWat);
    }
  }
}
