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
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.tests.framework.DualRuntimeTest;
import ai.tegmentum.wasmtime4j.wasmtime.framework.WastTestRunner;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

/**
 * Generated test from WAST file: gc/ref-test.wast
 *
 * <p>Tests the ref.test instruction with various reference types including nullable and
 * non-nullable types, top types, middle types, concrete types, and bottom types.
 *
 * <p>Requires: gc = true
 *
 * <p>Source:
 * https://github.com/bytecodealliance/wasmtime/blob/main/tests/misc_testsuite/gc/ref-test.wast
 */
public final class RefTestTest extends DualRuntimeTest {

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("ref.test nulls to nullable tops")
  public void testNullsToNullableTops(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    try (final WastTestRunner runner = new WastTestRunner()) {
      final String wat =
          "(module\n"
              + "  (func (export \"nulls-to-nullable-tops\") (result i32)\n"
              + "    (ref.test anyref (ref.null any))\n"
              + "    (ref.test anyref (ref.null none))\n"
              + "    i32.and\n"
              + "    (ref.test externref (ref.null extern))\n"
              + "    i32.and\n"
              + "    (ref.test externref (ref.null noextern))\n"
              + "    i32.and\n"
              + "    (ref.test funcref (ref.null func))\n"
              + "    i32.and\n"
              + "    (ref.test funcref (ref.null nofunc))\n"
              + "    i32.and\n"
              + "  )\n"
              + ")";
      runner.compileAndInstantiate(wat);
      runner.assertReturn("nulls-to-nullable-tops", new WasmValue[] {WasmValue.i32(1)});
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("ref.test non-nulls to nullable tops")
  public void testNonNullsToNullableTops(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    try (final WastTestRunner runner = new WastTestRunner()) {
      final String wat =
          "(module\n"
              + "  (type $s (struct))\n"
              + "  (func $f (export \"non-nulls-to-nullable-tops\") (param externref) (result"
              + " i32)\n"
              + "    (ref.test anyref (struct.new_default $s))\n"
              + "    (ref.test anyref (ref.i31 (i32.const 42)))\n"
              + "    i32.and\n"
              + "    (ref.test externref (local.get 0))\n"
              + "    i32.and\n"
              + "    (ref.test funcref (ref.func $f))\n"
              + "    i32.and\n"
              + "  )\n"
              + ")";
      runner.compileAndInstantiate(wat);
      runner.assertReturn(
          "non-nulls-to-nullable-tops",
          new WasmValue[] {WasmValue.i32(1)},
          WasmValue.externref(99));
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("ref.test nulls to non-nullable tops")
  public void testNullsToNonNullableTops(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    try (final WastTestRunner runner = new WastTestRunner()) {
      final String wat =
          "(module\n"
              + "  (func (export \"nulls-to-non-nullable-tops\") (result i32)\n"
              + "    (ref.test (ref any) (ref.null any))\n"
              + "    (ref.test (ref any) (ref.null none))\n"
              + "    i32.or\n"
              + "    (ref.test (ref extern) (ref.null extern))\n"
              + "    i32.or\n"
              + "    (ref.test (ref extern) (ref.null noextern))\n"
              + "    i32.or\n"
              + "    (ref.test (ref func) (ref.null func))\n"
              + "    i32.or\n"
              + "    (ref.test (ref func) (ref.null nofunc))\n"
              + "    i32.or\n"
              + "  )\n"
              + ")";
      runner.compileAndInstantiate(wat);
      runner.assertReturn("nulls-to-non-nullable-tops", new WasmValue[] {WasmValue.i32(0)});
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("ref.test non-nulls to non-nullable tops")
  public void testNonNullsToNonNullableTops(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    try (final WastTestRunner runner = new WastTestRunner()) {
      final String wat =
          "(module\n"
              + "  (type $s (struct))\n"
              + "  (func $f (export \"non-nulls-to-non-nullable-tops\") (param externref) (result"
              + " i32)\n"
              + "    (ref.test (ref any) (struct.new_default $s))\n"
              + "    (ref.test (ref any) (ref.i31 (i32.const 42)))\n"
              + "    i32.and\n"
              + "    (ref.test (ref extern) (local.get 0))\n"
              + "    i32.and\n"
              + "    (ref.test (ref func) (ref.func $f))\n"
              + "    i32.and\n"
              + "  )\n"
              + ")";
      runner.compileAndInstantiate(wat);
      runner.assertReturn(
          "non-nulls-to-non-nullable-tops",
          new WasmValue[] {WasmValue.i32(1)},
          WasmValue.externref(1));
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("ref.test null to nullable i31")
  public void testNullToNullableI31(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    try (final WastTestRunner runner = new WastTestRunner()) {
      final String wat =
          "(module\n"
              + "  (func (export \"null-to-nullable-i31\") (result i32)\n"
              + "    (ref.test i31ref (ref.null none))\n"
              + "    (ref.test i31ref (ref.null i31))\n"
              + "    i32.and\n"
              + "    (ref.test i31ref (ref.null struct))\n"
              + "    i32.and\n"
              + "    (ref.test i31ref (ref.null array))\n"
              + "    i32.and\n"
              + "    (ref.test i31ref (ref.null eq))\n"
              + "    i32.and\n"
              + "    (ref.test i31ref (ref.null any))\n"
              + "    i32.and\n"
              + "  )\n"
              + ")";
      runner.compileAndInstantiate(wat);
      runner.assertReturn("null-to-nullable-i31", new WasmValue[] {WasmValue.i32(1)});
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("ref.test truthy non-null to nullable i31")
  public void testTruthyNonNullToNullableI31(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    try (final WastTestRunner runner = new WastTestRunner()) {
      final String wat =
          "(module\n"
              + "  (func (export \"truthy-non-null-to-nullable-i31\") (result i32)\n"
              + "    (ref.test i31ref (ref.i31 (i32.const 42)))\n"
              + "  )\n"
              + ")";
      runner.compileAndInstantiate(wat);
      runner.assertReturn("truthy-non-null-to-nullable-i31", new WasmValue[] {WasmValue.i32(1)});
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("ref.test falsey non-null to nullable i31")
  public void testFalseyNonNullToNullableI31(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    try (final WastTestRunner runner = new WastTestRunner()) {
      final String wat =
          "(module\n"
              + "  (type $s (struct))\n"
              + "  (type $a (array i32))\n"
              + "  (func (export \"falsey-non-null-to-nullable-i31\") (result i32)\n"
              + "    (ref.test i31ref (struct.new_default $s))\n"
              + "    (ref.test i31ref (array.new_default $a (i32.const 3)))\n"
              + "    i32.or\n"
              + "  )\n"
              + ")";
      runner.compileAndInstantiate(wat);
      runner.assertReturn("falsey-non-null-to-nullable-i31", new WasmValue[] {WasmValue.i32(0)});
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("ref.test null to non-nullable i31")
  public void testNullToNonNullableI31(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    try (final WastTestRunner runner = new WastTestRunner()) {
      final String wat =
          "(module\n"
              + "  (func (export \"null-to-non-nullable-i31\") (result i32)\n"
              + "    (ref.test (ref i31) (ref.null none))\n"
              + "    (ref.test (ref i31) (ref.null i31))\n"
              + "    i32.or\n"
              + "    (ref.test (ref i31) (ref.null struct))\n"
              + "    i32.or\n"
              + "    (ref.test (ref i31) (ref.null array))\n"
              + "    i32.or\n"
              + "    (ref.test (ref i31) (ref.null eq))\n"
              + "    i32.or\n"
              + "    (ref.test (ref i31) (ref.null any))\n"
              + "    i32.or\n"
              + "  )\n"
              + ")";
      runner.compileAndInstantiate(wat);
      runner.assertReturn("null-to-non-nullable-i31", new WasmValue[] {WasmValue.i32(0)});
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("ref.test truthy non-null to non-nullable i31")
  public void testTruthyNonNullToNonNullableI31(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    try (final WastTestRunner runner = new WastTestRunner()) {
      final String wat =
          "(module\n"
              + "  (func (export \"truthy-non-null-to-non-nullable-i31\") (result i32)\n"
              + "    (ref.test (ref i31) (ref.i31 (i32.const 42)))\n"
              + "  )\n"
              + ")";
      runner.compileAndInstantiate(wat);
      runner.assertReturn(
          "truthy-non-null-to-non-nullable-i31", new WasmValue[] {WasmValue.i32(1)});
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("ref.test null to nullable middle types")
  public void testNullToNullableMiddleTypes(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    try (final WastTestRunner runner = new WastTestRunner()) {
      final String wat =
          "(module\n"
              + "  (func (export \"null-to-nullable-middle-types\") (result i32)\n"
              + "    (ref.test structref (ref.null any))\n"
              + "    (ref.test structref (ref.null eq))\n"
              + "    i32.and\n"
              + "    (ref.test structref (ref.null struct))\n"
              + "    i32.and\n"
              + "    (ref.test structref (ref.null none))\n"
              + "    i32.and\n"
              + "    (ref.test arrayref (ref.null any))\n"
              + "    i32.and\n"
              + "    (ref.test arrayref (ref.null eq))\n"
              + "    i32.and\n"
              + "    (ref.test arrayref (ref.null array))\n"
              + "    i32.and\n"
              + "    (ref.test arrayref (ref.null none))\n"
              + "    i32.and\n"
              + "  )\n"
              + ")";
      runner.compileAndInstantiate(wat);
      runner.assertReturn("null-to-nullable-middle-types", new WasmValue[] {WasmValue.i32(1)});
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("ref.test truthy non-null to nullable middle types")
  public void testTruthyNonNullToNullableMiddleTypes(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    try (final WastTestRunner runner = new WastTestRunner()) {
      final String wat =
          "(module\n"
              + "  (type $s (struct))\n"
              + "  (type $a (array i32))\n"
              + "  (func (export \"truthy-non-null-to-nullable-middle-types\") (result i32)\n"
              + "    (ref.test eqref (ref.i31 (i32.const 42)))\n"
              + "    (ref.test eqref (struct.new_default $s))\n"
              + "    i32.and\n"
              + "    (ref.test eqref (array.new_default $a (i32.const 3)))\n"
              + "    i32.and\n"
              + "    (ref.test structref (struct.new_default $s))\n"
              + "    i32.and\n"
              + "    (ref.test arrayref (array.new_default $a (i32.const 3)))\n"
              + "    i32.and\n"
              + "  )\n"
              + ")";
      runner.compileAndInstantiate(wat);
      runner.assertReturn(
          "truthy-non-null-to-nullable-middle-types", new WasmValue[] {WasmValue.i32(1)});
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("ref.test falsey non-null to nullable middle types")
  public void testFalseyNonNullToNullableMiddleTypes(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    try (final WastTestRunner runner = new WastTestRunner()) {
      final String wat =
          "(module\n"
              + "  (type $s (struct))\n"
              + "  (type $a (array i32))\n"
              + "  (func (export \"falsey-non-null-to-nullable-middle-types\") (result i32)\n"
              + "    (ref.test structref (ref.i31 (i32.const 42)))\n"
              + "    (ref.test structref (array.new_default $a (i32.const 3)))\n"
              + "    i32.or\n"
              + "    (ref.test arrayref (ref.i31 (i32.const 42)))\n"
              + "    i32.or\n"
              + "    (ref.test arrayref (struct.new_default $s))\n"
              + "    i32.or\n"
              + "  )\n"
              + ")";
      runner.compileAndInstantiate(wat);
      runner.assertReturn(
          "falsey-non-null-to-nullable-middle-types", new WasmValue[] {WasmValue.i32(0)});
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("ref.test null to nullable bottom type")
  public void testNullToNullableBottomType(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    try (final WastTestRunner runner = new WastTestRunner()) {
      final String wat =
          "(module\n"
              + "  (func (export \"null-to-nullable-bottom-type\") (result i32)\n"
              + "    (ref.test nullref (ref.null any))\n"
              + "    (ref.test nullref (ref.null eq))\n"
              + "    i32.and\n"
              + "    (ref.test nullref (ref.null i31))\n"
              + "    i32.and\n"
              + "    (ref.test nullref (ref.null struct))\n"
              + "    i32.and\n"
              + "    (ref.test nullref (ref.null array))\n"
              + "    i32.and\n"
              + "    (ref.test nullref (ref.null none))\n"
              + "    i32.and\n"
              + "    (ref.test nullexternref (ref.null extern))\n"
              + "    i32.and\n"
              + "    (ref.test nullexternref (ref.null noextern))\n"
              + "    i32.and\n"
              + "    (ref.test nullfuncref (ref.null func))\n"
              + "    i32.and\n"
              + "    (ref.test nullfuncref (ref.null nofunc))\n"
              + "    i32.and\n"
              + "  )\n"
              + ")";
      runner.compileAndInstantiate(wat);
      runner.assertReturn("null-to-nullable-bottom-type", new WasmValue[] {WasmValue.i32(1)});
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("ref.test non-null to nullable bottom type")
  public void testNonNullToNullableBottomType(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    try (final WastTestRunner runner = new WastTestRunner()) {
      final String wat =
          "(module\n"
              + "  (type $s (struct))\n"
              + "  (func $f (export \"non-null-to-nullable-bottom-type\") (param externref) (result"
              + " i32)\n"
              + "    (ref.test nullref (struct.new_default $s))\n"
              + "    (ref.test nullexternref (local.get 0))\n"
              + "    i32.or\n"
              + "    (ref.test nullfuncref (ref.func $f))\n"
              + "    i32.or\n"
              + "  )\n"
              + ")";
      runner.compileAndInstantiate(wat);
      runner.assertReturn(
          "non-null-to-nullable-bottom-type",
          new WasmValue[] {WasmValue.i32(0)},
          WasmValue.externref(1));
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("ref.test null to non-nullable bottom type")
  public void testNullToNonNullableBottomType(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    try (final WastTestRunner runner = new WastTestRunner()) {
      final String wat =
          "(module\n"
              + "  (func (export \"null-to-non-nullable-bottom-type\") (result i32)\n"
              + "    (ref.test (ref none) (ref.null any))\n"
              + "    (ref.test (ref none) (ref.null eq))\n"
              + "    i32.or\n"
              + "    (ref.test (ref none) (ref.null i31))\n"
              + "    i32.or\n"
              + "    (ref.test (ref none) (ref.null struct))\n"
              + "    i32.or\n"
              + "    (ref.test (ref none) (ref.null array))\n"
              + "    i32.or\n"
              + "    (ref.test (ref none) (ref.null none))\n"
              + "    i32.or\n"
              + "    (ref.test (ref noextern) (ref.null extern))\n"
              + "    i32.or\n"
              + "    (ref.test (ref noextern) (ref.null noextern))\n"
              + "    i32.or\n"
              + "    (ref.test (ref nofunc) (ref.null func))\n"
              + "    i32.or\n"
              + "    (ref.test (ref nofunc) (ref.null nofunc))\n"
              + "    i32.or\n"
              + "  )\n"
              + ")";
      runner.compileAndInstantiate(wat);
      runner.assertReturn("null-to-non-nullable-bottom-type", new WasmValue[] {WasmValue.i32(0)});
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("ref.test non-null to non-nullable bottom type")
  public void testNonNullToNonNullableBottomType(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    try (final WastTestRunner runner = new WastTestRunner()) {
      final String wat =
          "(module\n"
              + "  (type $s (struct))\n"
              + "  (func $f (export \"non-null-to-non-nullable-bottom-type\") (param externref)"
              + " (result i32)\n"
              + "    (ref.test (ref none) (struct.new_default $s))\n"
              + "    (ref.test (ref noextern) (local.get 0))\n"
              + "    i32.or\n"
              + "    (ref.test (ref nofunc) (ref.func $f))\n"
              + "    i32.or\n"
              + "  )\n"
              + ")";
      runner.compileAndInstantiate(wat);
      runner.assertReturn(
          "non-null-to-non-nullable-bottom-type",
          new WasmValue[] {WasmValue.i32(0)},
          WasmValue.externref(1));
    }
  }
}
