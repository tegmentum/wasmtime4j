package ai.tegmentum.wasmtime4j.comparison.generated.misctestsuite;

import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.comparison.framework.DualRuntimeTest;
import ai.tegmentum.wasmtime4j.comparison.framework.WastTestRunner;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

/**
 * Equivalent Java test for Wasmtime test: misc_testsuite::table_grow
 *
 * <p>Original source: table_grow.wast:1 Category: misc_testsuite
 *
 * <p>This test validates that wasmtime4j produces the same results as the upstream Wasmtime
 * implementation for this test case.
 */
public final class TableGrowTest extends DualRuntimeTest {

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("misc_testsuite::table_grow")
  public void testTableGrow(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);

    try (final WastTestRunner runner = new WastTestRunner()) {

      // Compile and instantiate module 1
      runner.compileAndInstantiate(
          """
        (module
          (table $t 0 externref)
          (func (export "get") (param $i i32) (result externref) (table.get $t (local.get $i)))
          (func (export "set") (param $i i32) (param $r externref) (table.set $t (local.get $i) (local.get $r)))
          (func (export "grow") (param $sz i32) (param $init externref) (result i32)
            (table.grow $t (local.get $init) (local.get $sz))
          )
          (func (export "size") (result i32) (table.size $t))
        )
      """);

      runner.assertReturn("size", new WasmValue[] {WasmValue.i32(0)});
      runner.assertTrap("set", null, WasmValue.i32(0), WasmValue.externref(2L));
      runner.assertTrap("get", null, WasmValue.i32(0));

      runner.assertReturn(
          "grow", new WasmValue[] {WasmValue.i32(0)}, WasmValue.i32(1), WasmValue.externref(null));
      runner.assertReturn("size", new WasmValue[] {WasmValue.i32(1)});
      runner.assertReturn("get", new WasmValue[] {WasmValue.externref(null)}, WasmValue.i32(0));
      runner.invoke("set", WasmValue.i32(0), WasmValue.externref(2L));
      runner.assertReturn("get", new WasmValue[] {WasmValue.externref(2L)}, WasmValue.i32(0));
      runner.assertTrap("set", null, WasmValue.i32(1), WasmValue.externref(2L));
      runner.assertTrap("get", null, WasmValue.i32(1));

      runner.assertReturn(
          "grow", new WasmValue[] {WasmValue.i32(1)}, WasmValue.i32(4), WasmValue.externref(3L));
      runner.assertReturn("size", new WasmValue[] {WasmValue.i32(5)});
      runner.assertReturn("get", new WasmValue[] {WasmValue.externref(2L)}, WasmValue.i32(0));
      runner.invoke("set", WasmValue.i32(0), WasmValue.externref(2L));
      runner.assertReturn("get", new WasmValue[] {WasmValue.externref(2L)}, WasmValue.i32(0));
      runner.assertReturn("get", new WasmValue[] {WasmValue.externref(3L)}, WasmValue.i32(1));
      runner.assertReturn("get", new WasmValue[] {WasmValue.externref(3L)}, WasmValue.i32(4));
      runner.invoke("set", WasmValue.i32(4), WasmValue.externref(4L));
      runner.assertReturn("get", new WasmValue[] {WasmValue.externref(4L)}, WasmValue.i32(4));
      runner.assertTrap("set", null, WasmValue.i32(5), WasmValue.externref(2L));
      runner.assertTrap("get", null, WasmValue.i32(5));

      // Compile and instantiate module 2
      runner.compileAndInstantiate(
          """
        (module
          (table $t 0x10 funcref)
          (elem declare func $f)
          (func $f (export "grow") (result i32)
            (table.grow $t (ref.func $f) (i32.const 0xffff_fff0))
          )
        )
      """);

      runner.assertReturn("grow", new WasmValue[] {WasmValue.i32(-1)});

      // Compile and instantiate module 3
      runner.compileAndInstantiate(
          """
        (module
          (table $t 0 externref)
          (func (export "grow") (param i32) (result i32)
            (table.grow $t (ref.null extern) (local.get 0))
          )
        )
      """);

      runner.assertReturn("grow", new WasmValue[] {WasmValue.i32(0)}, WasmValue.i32(0));
      runner.assertReturn("grow", new WasmValue[] {WasmValue.i32(0)}, WasmValue.i32(1));
      runner.assertReturn("grow", new WasmValue[] {WasmValue.i32(1)}, WasmValue.i32(0));
      runner.assertReturn("grow", new WasmValue[] {WasmValue.i32(1)}, WasmValue.i32(2));
      runner.assertReturn("grow", new WasmValue[] {WasmValue.i32(3)}, WasmValue.i32(800));

      // Compile and instantiate module 4
      runner.compileAndInstantiate(
          """
        (module
          (type $afunc (func))
          (table $t 0 (ref null $afunc))
          (func (export "grow") (param i32) (result i32)
            (table.grow $t (ref.null $afunc) (local.get 0))
          )
        )
      """);

      runner.assertReturn("grow", new WasmValue[] {WasmValue.i32(0)}, WasmValue.i32(0));
      runner.assertReturn("grow", new WasmValue[] {WasmValue.i32(0)}, WasmValue.i32(1));
      runner.assertReturn("grow", new WasmValue[] {WasmValue.i32(1)}, WasmValue.i32(0));
      runner.assertReturn("grow", new WasmValue[] {WasmValue.i32(1)}, WasmValue.i32(2));
      runner.assertReturn("grow", new WasmValue[] {WasmValue.i32(3)}, WasmValue.i32(800));

      // Compile and instantiate module 5
      runner.compileAndInstantiate(
          """
        (module
          (table $t 0 10 externref)
          (func (export "grow") (param i32) (result i32)
            (table.grow $t (ref.null extern) (local.get 0))
          )
        )
      """);

      runner.assertReturn("grow", new WasmValue[] {WasmValue.i32(0)}, WasmValue.i32(0));
      runner.assertReturn("grow", new WasmValue[] {WasmValue.i32(0)}, WasmValue.i32(1));
      runner.assertReturn("grow", new WasmValue[] {WasmValue.i32(1)}, WasmValue.i32(1));
      runner.assertReturn("grow", new WasmValue[] {WasmValue.i32(2)}, WasmValue.i32(2));
      runner.assertReturn("grow", new WasmValue[] {WasmValue.i32(4)}, WasmValue.i32(6));
      runner.assertReturn("grow", new WasmValue[] {WasmValue.i32(10)}, WasmValue.i32(0));
      runner.assertReturn("grow", new WasmValue[] {WasmValue.i32(-1)}, WasmValue.i32(1));
      runner.assertReturn("grow", new WasmValue[] {WasmValue.i32(-1)}, WasmValue.i32(0x10000));

      // Compile and instantiate module 6
      runner.compileAndInstantiate(
          """
        (module
          (table $t 10 funcref)
          (func (export "grow") (param i32) (result i32)
            (table.grow $t (ref.null func) (local.get 0))
          )
          (elem declare func 1)
          (func (export "check-table-null") (param i32 i32) (result funcref)
            (local funcref)
            (local.set 2 (ref.func 1))
            (block
              (loop
                (local.set 2 (table.get $t (local.get 0)))
                (br_if 1 (i32.eqz (ref.is_null (local.get 2))))
                (br_if 1 (i32.ge_u (local.get 0) (local.get 1)))
                (local.set 0 (i32.add (local.get 0) (i32.const 1)))
                (br_if 0 (i32.le_u (local.get 0) (local.get 1)))
              )
            )
            (local.get 2)
          )
        )
      """);

      runner.assertReturn(
          "check-table-null",
          new WasmValue[] {WasmValue.externref(null)},
          WasmValue.i32(0),
          WasmValue.i32(9));
      runner.assertReturn("grow", new WasmValue[] {WasmValue.i32(10)}, WasmValue.i32(10));
      runner.assertReturn(
          "check-table-null",
          new WasmValue[] {WasmValue.externref(null)},
          WasmValue.i32(0),
          WasmValue.i32(19));
    }
  }
}
