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

package ai.tegmentum.wasmtime4j.wasmtime.generated.custompagesizes;

import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.tests.framework.DualRuntimeTest;
import ai.tegmentum.wasmtime4j.wasmtime.framework.WastTestRunner;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

/**
 * Generated test from WAST file: custom-page-sizes/custom-page-sizes.wast (linking portion)
 *
 * <p>Tests linking modules that export and import memories with custom page sizes.
 *
 * <p>Requires: custom_page_sizes = true, multi_memory = true
 *
 * <p>Source:
 * https://github.com/bytecodealliance/wasmtime/blob/main/tests/misc_testsuite/custom-page-sizes/custom-page-sizes.wast
 */
public final class CustomPageSizesLinkingTest extends DualRuntimeTest {

  private static final String MODULE_EXPORTER =
      "(module\n"
          + "  (memory (export \"small-pages-memory\") 0 (pagesize 1))\n"
          + "  (memory (export \"large-pages-memory\") 0 (pagesize 65536))\n"
          + ")";

  private static final String MODULE_IMPORT_SMALL =
      "(module\n" + "  (memory (import \"m\" \"small-pages-memory\") 0 (pagesize 1))\n" + ")";

  private static final String MODULE_IMPORT_LARGE =
      "(module\n" + "  (memory (import \"m\" \"large-pages-memory\") 0 (pagesize 65536))\n" + ")";

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Link module importing small-pages-memory (pagesize 1)")
  public void testLinkSmallPagesMemory(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    try (final WastTestRunner runner = new WastTestRunner()) {
      // Create and register the exporter module
      runner.compileAndInstantiate("m", MODULE_EXPORTER);
      runner.registerModule("m");

      // Import the small-pages memory - should succeed
      runner.compileAndInstantiate(MODULE_IMPORT_SMALL);
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Link module importing large-pages-memory (pagesize 65536)")
  public void testLinkLargePagesMemory(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    try (final WastTestRunner runner = new WastTestRunner()) {
      // Create and register the exporter module
      runner.compileAndInstantiate("m", MODULE_EXPORTER);
      runner.registerModule("m");

      // Import the large-pages memory - should succeed
      runner.compileAndInstantiate(MODULE_IMPORT_LARGE);
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Page size mismatch - import small as large fails")
  public void testPageSizeMismatchSmallAsLarge(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    try (final WastTestRunner runner = new WastTestRunner()) {
      // Create and register the exporter module
      runner.compileAndInstantiate("m", MODULE_EXPORTER);
      runner.registerModule("m");

      // Try to import small-pages-memory as large-pages - should fail
      final String badImport =
          "(module\n"
              + "  (memory (import \"m\" \"small-pages-memory\") 0 (pagesize 65536))\n"
              + ")";
      runner.assertUnlinkable(badImport, "incompatible");
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Page size mismatch - import large as small fails")
  public void testPageSizeMismatchLargeAsSmall(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    try (final WastTestRunner runner = new WastTestRunner()) {
      // Create and register the exporter module
      runner.compileAndInstantiate("m", MODULE_EXPORTER);
      runner.registerModule("m");

      // Try to import large-pages-memory as small-pages - should fail
      final String badImport =
          "(module\n" + "  (memory (import \"m\" \"large-pages-memory\") 0 (pagesize 1))\n" + ")";
      runner.assertUnlinkable(badImport, "incompatible");
    }
  }
}
