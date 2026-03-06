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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import ai.tegmentum.wasmtime4j.tests.framework.DualRuntimeTest;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

/**
 * Integration tests for ExnRef and ContRef value types and type hierarchy.
 *
 * <p>Validates that exception handling (ExnRef) and stack switching (ContRef) value types are
 * correctly created and follow the expected type hierarchy.
 */
class ExnRefContRefTest extends DualRuntimeTest {

  private static final Logger LOGGER = Logger.getLogger(ExnRefContRefTest.class.getName());

  @AfterEach
  void tearDown() {
    clearRuntimeSelection();
  }

  // ========== ExnRef Tests ==========

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  void testExnRefCreation(final RuntimeType runtime) {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing ExnRef value creation");

    final WasmValue exn = WasmValue.exnref("exception-data");
    assertThat(exn.getType()).isEqualTo(WasmValueType.EXNREF);
    assertThat(exn.asExnref()).isEqualTo("exception-data");
    assertThat(exn.isReference()).as("ExnRef should be a reference type").isTrue();
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  void testNullExnRef(final RuntimeType runtime) {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing null ExnRef creation");

    final WasmValue nullExn = WasmValue.nullExnRef();
    assertThat(nullExn.getType()).isEqualTo(WasmValueType.EXNREF);
    assertThat(nullExn.asExnref()).isNull();
    assertThat(nullExn.getValue() == null).as("Null exnref should be null").isTrue();
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  void testNullNullExnRef(final RuntimeType runtime) {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing NULLEXNREF bottom type");

    final WasmValue nullExnBottom = WasmValue.nullNullExnRef();
    assertThat(nullExnBottom.getType()).isEqualTo(WasmValueType.NULLEXNREF);
    assertThat(nullExnBottom.getValue() == null).as("NULLEXNREF should be null").isTrue();
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  void testExnRefTypeHierarchy(final RuntimeType runtime) {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing ExnRef type hierarchy");

    // NULLEXNREF is a subtype of EXNREF
    assertThat(WasmValueType.NULLEXNREF.isSubtypeOf(WasmValueType.EXNREF))
        .as("NULLEXNREF should be subtype of EXNREF")
        .isTrue();

    // EXNREF is not a subtype of NULLEXNREF
    assertThat(WasmValueType.EXNREF.isSubtypeOf(WasmValueType.NULLEXNREF))
        .as("EXNREF should not be subtype of NULLEXNREF")
        .isFalse();

    // EXNREF is a reference type
    assertThat(WasmValueType.EXNREF.isReference()).as("EXNREF should be a reference type").isTrue();
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  void testExnRefCastError(final RuntimeType runtime) {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing ExnRef cast error on wrong type");

    final WasmValue i32Val = WasmValue.i32(42);
    assertThrows(
        ClassCastException.class,
        i32Val::asExnref,
        "Calling asExnref on i32 should throw ClassCastException");
  }

  // ========== ContRef Tests ==========

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  void testContRefCreation(final RuntimeType runtime) {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing ContRef value creation");

    final WasmValue cont = WasmValue.contref("continuation-data");
    assertThat(cont.getType()).isEqualTo(WasmValueType.CONTREF);
    assertThat(cont.asContref()).isEqualTo("continuation-data");
    assertThat(cont.isReference()).as("ContRef should be a reference type").isTrue();
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  void testNullContRef(final RuntimeType runtime) {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing null ContRef creation");

    final WasmValue nullCont = WasmValue.nullContRef();
    assertThat(nullCont.getType()).isEqualTo(WasmValueType.CONTREF);
    assertThat(nullCont.asContref()).isNull();
    assertThat(nullCont.getValue() == null).as("Null contref should be null").isTrue();
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  void testNullNullContRef(final RuntimeType runtime) {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing NULLCONTREF bottom type");

    final WasmValue nullContBottom = WasmValue.nullNullContRef();
    assertThat(nullContBottom.getType()).isEqualTo(WasmValueType.NULLCONTREF);
    assertThat(nullContBottom.getValue() == null).as("NULLCONTREF should be null").isTrue();
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  void testContRefTypeHierarchy(final RuntimeType runtime) {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing ContRef type hierarchy");

    // NULLCONTREF is a subtype of CONTREF
    assertThat(WasmValueType.NULLCONTREF.isSubtypeOf(WasmValueType.CONTREF))
        .as("NULLCONTREF should be subtype of CONTREF")
        .isTrue();

    // CONTREF is not a subtype of NULLCONTREF
    assertThat(WasmValueType.CONTREF.isSubtypeOf(WasmValueType.NULLCONTREF))
        .as("CONTREF should not be subtype of NULLCONTREF")
        .isFalse();

    // CONTREF is a reference type
    assertThat(WasmValueType.CONTREF.isReference())
        .as("CONTREF should be a reference type")
        .isTrue();
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  void testContRefCastError(final RuntimeType runtime) {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing ContRef cast error on wrong type");

    final WasmValue f64Val = WasmValue.f64(3.14);
    assertThrows(
        ClassCastException.class,
        f64Val::asContref,
        "Calling asContref on f64 should throw ClassCastException");
  }

  // ========== Cross-type Tests ==========

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  void testExnRefAndContRefAreDistinct(final RuntimeType runtime) {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing ExnRef and ContRef are distinct types");

    // ExnRef is not a subtype of ContRef and vice versa
    assertThat(WasmValueType.EXNREF.isSubtypeOf(WasmValueType.CONTREF))
        .as("EXNREF should not be subtype of CONTREF")
        .isFalse();
    assertThat(WasmValueType.CONTREF.isSubtypeOf(WasmValueType.EXNREF))
        .as("CONTREF should not be subtype of EXNREF")
        .isFalse();

    // Can't cast between them
    final WasmValue exn = WasmValue.exnref("data");
    assertThrows(ClassCastException.class, exn::asContref);

    final WasmValue cont = WasmValue.contref("data");
    assertThrows(ClassCastException.class, cont::asExnref);
  }
}
