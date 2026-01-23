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

package ai.tegmentum.wasmtime4j.wasmtime.generated.gc;

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
 * Generated test from WAST file: gc/func-refs-in-gc-heap.wast
 *
 * <p>Tests function references stored in GC heap structures. The module tests both typed and
 * untyped function references, as well as nofunc references stored in struct fields. It defines:
 *
 * <ul>
 *   <li>A function type $f0 returning i32
 *   <li>Three struct types with mutable fields: $s0 (funcref), $s1 (typed ref to $f0), $s2
 *       (nullable nofunc ref)
 *   <li>Two functions $f and $g returning different i32 constants
 *   <li>Allocation functions that create structs with function references
 *   <li>Getter functions that retrieve and call function references via call_indirect
 *   <li>Setter functions that modify function references in struct fields
 * </ul>
 *
 * <p>The full WAST file includes assertions for:
 *
 * <ul>
 *   <li>Successful struct allocation with function references
 *   <li>Getting and calling stored function references (returns expected i32 values)
 *   <li>Setting function references in struct fields
 *   <li>Traps when calling null function references ("uninitialized element")
 * </ul>
 *
 * <p>Requires: gc = true
 *
 * <p>Source:
 * https://github.com/bytecodealliance/wasmtime/blob/main/tests/misc_testsuite/gc/func-refs-in-gc-heap.wast
 */
public final class FuncRefsInGcHeapTest extends DualRuntimeTest {
  private static String loadResource(final String path) throws IOException {
    try (final InputStream is = FuncRefsInGcHeapTest.class.getResourceAsStream(path)) {
      if (is == null) {
        throw new IOException("Resource not found: " + path);
      }
      return new String(is.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("function references in GC heap compile correctly")
  public void testFuncRefsInGcHeapCompile(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);

    try (final WastTestRunner runner = new WastTestRunner()) {
      final String moduleWat =
          loadResource(
              "/ai/tegmentum/wasmtime4j/wasmtime/generated/gc/FuncRefsInGcHeapTest_module1.wat");
      // Verifies that function references stored in GC heap structures compile and instantiate
      runner.compileAndInstantiate(moduleWat);
    }
  }
}
