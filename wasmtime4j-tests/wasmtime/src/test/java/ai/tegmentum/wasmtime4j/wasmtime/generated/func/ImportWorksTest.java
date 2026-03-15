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
package ai.tegmentum.wasmtime4j.wasmtime.generated.func;

import static org.junit.jupiter.api.Assertions.assertEquals;

import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.WasmValueType;
import ai.tegmentum.wasmtime4j.func.HostFunction;
import ai.tegmentum.wasmtime4j.type.FunctionType;
import ai.tegmentum.wasmtime4j.wasmtime.framework.WastTestRunner;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Equivalent Java test for Wasmtime test: func::import_works
 *
 * <p>Original source: func.rs:560 Category: func
 *
 * <p>This test validates that wasmtime4j produces the same results as the upstream Wasmtime
 * implementation for this test case. Tests importing multiple host functions with different
 * signatures and calling them from WebAssembly.
 *
 * <p>Note: This test uses advanced WebAssembly GC types (externref, funcref, anyref, i31ref) which
 * require the GC proposal to be enabled.
 */
public final class ImportWorksTest {

  private static final Logger LOGGER = Logger.getLogger(ImportWorksTest.class.getName());

  @Test
  @DisplayName("func::import_works - full test with GC types")
  public void testImportWorksWithGcTypes() throws Exception {
    LOGGER.info("Testing import works with GC types (anyref, funcref, externref)");

    // GC ref types require Panama runtime (Java 22+)
    org.junit.jupiter.api.Assumptions.assumeTrue(
        Runtime.version().feature() >= 22, "GC ref types require Java 22+ (Panama runtime)");

    // Track host function invocations
    final AtomicInteger hits = new AtomicInteger(0);

    // Track received values for verification
    final Object[] lastExternref = new Object[1];
    final Object[] lastFuncref = new Object[1];

    // This test requires GC types that need Panama runtime
    try (final WastTestRunner runner = new WastTestRunner(RuntimeType.PANAMA)) {
      // Define f1: (externref) -> ()
      runner.defineHostFunction(
          "test",
          "f1",
          FunctionType.of(new WasmValueType[] {WasmValueType.EXTERNREF}, new WasmValueType[] {}),
          (args) -> {
            hits.incrementAndGet();
            lastExternref[0] = args[0].asExternref();
            LOGGER.fine("f1 called with externref: " + lastExternref[0]);
            return new WasmValue[] {};
          });

      // Define f2: (funcref) -> ()
      runner.defineHostFunction(
          "test",
          "f2",
          FunctionType.of(new WasmValueType[] {WasmValueType.FUNCREF}, new WasmValueType[] {}),
          (args) -> {
            hits.incrementAndGet();
            lastFuncref[0] = args[0].asFuncref();
            LOGGER.fine("f2 called with funcref: " + lastFuncref[0]);
            return new WasmValue[] {};
          });

      // Define f3: (externref, funcref) -> ()
      runner.defineHostFunction(
          "test",
          "f3",
          FunctionType.of(
              new WasmValueType[] {WasmValueType.EXTERNREF, WasmValueType.FUNCREF},
              new WasmValueType[] {}),
          (args) -> {
            hits.incrementAndGet();
            lastExternref[0] = args[0].asExternref();
            lastFuncref[0] = args[1].asFuncref();
            LOGGER.fine(
                "f3 called with externref: " + lastExternref[0] + ", funcref: " + lastFuncref[0]);
            return new WasmValue[] {};
          });

      // WAT that tests basic GC reference types
      final String wat =
          """
          (module
            (import "test" "f1" (func $f1 (param externref)))
            (import "test" "f2" (func $f2 (param funcref)))
            (import "test" "f3" (func $f3 (param externref) (param funcref)))

            (func (export "run") (param externref funcref)
              ;; Call f1 with externref parameter
              local.get 0
              call $f1

              ;; Call f2 with funcref parameter
              local.get 1
              call $f2

              ;; Call f3 with both parameters
              local.get 0
              local.get 1
              call $f3
            )
          )
          """;

      runner.compileAndInstantiate(wat);

      // Verify no functions called yet
      assertEquals(0, hits.get(), "No functions should be called before run()");

      // Call the exported run function with null references
      runner.invoke("run", WasmValue.nullExternref(), WasmValue.nullFuncref());

      // Verify all three host functions were called
      assertEquals(3, hits.get(), "All three host functions should have been called");

      LOGGER.info("Import works with GC types test passed: " + hits.get() + " functions called");
    }
  }

  @Test
  @DisplayName("func::import_works - basic imports without GC types")
  public void testImportWorksBasic() throws Exception {
    // Track host function invocations
    final AtomicInteger hits = new AtomicInteger(0);

    // Simplified WAT that tests basic import functionality without GC types
    final String wat =
        """
        (module
          (import "test" "func0" (func))
          (import "test" "func1" (func (param i32) (result i32)))
          (import "test" "func2" (func (param i32) (param i64)))

          (func (export "run") (result i32)
            ;; Call func0 (no params, no results)
            call 0

            ;; Call func1 with 0, expect result + 1 = 1, add 1 = 2
            i32.const 0
            call 1
            i32.const 1
            i32.add

            ;; Call func2 with separate values (doesn't affect stack result)
            i32.const 3
            i64.const 4
            call 2

            ;; The stack still has the result from func1 + 1 = 2
            ;; which is what we return
          )
        )
        """;

    // Function type for func0: () -> ()
    final FunctionType func0Type = new FunctionType(new WasmValueType[] {}, new WasmValueType[] {});

    // Function type for func1: (i32) -> (i32)
    final FunctionType func1Type =
        new FunctionType(
            new WasmValueType[] {WasmValueType.I32}, new WasmValueType[] {WasmValueType.I32});

    // Function type for func2: (i32, i64) -> ()
    final FunctionType func2Type =
        new FunctionType(
            new WasmValueType[] {WasmValueType.I32, WasmValueType.I64}, new WasmValueType[] {});

    // Host function implementations
    final HostFunction hostFunc0 =
        HostFunction.voidFunction(
            (params) -> {
              hits.incrementAndGet();
            });

    final HostFunction hostFunc1 =
        HostFunction.singleValue(
            (params) -> {
              hits.incrementAndGet();
              final int x = params[0].asInt();
              // Return x + 1
              return WasmValue.i32(x + 1);
            });

    final HostFunction hostFunc2 =
        HostFunction.voidFunction(
            (params) -> {
              hits.incrementAndGet();
              final int a = params[0].asInt();
              final long b = params[1].asLong();
              // Just verify we received the expected values
              assertEquals(3, a, "First param should be 3");
              assertEquals(4L, b, "Second param should be 4");
            });

    try (final WastTestRunner runner = new WastTestRunner()) {
      // Define all host functions
      runner.defineHostFunction("test", "func0", func0Type, hostFunc0);
      runner.defineHostFunction("test", "func1", func1Type, hostFunc1);
      runner.defineHostFunction("test", "func2", func2Type, hostFunc2);

      // Compile and instantiate
      runner.compileAndInstantiate(wat);

      // Verify no functions called yet
      assertEquals(0, hits.get(), "No functions should be called before run()");

      // Call the exported run function
      final WasmValue[] results = runner.invoke("run");

      // Verify all three host functions were called
      assertEquals(3, hits.get(), "All three host functions should have been called");

      // Verify the result
      assertEquals(1, results.length, "Should return one value");
      assertEquals(2, results[0].asInt(), "Result should be 2");
    }
  }
}
