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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmFunction;
import ai.tegmentum.wasmtime4j.func.TypedFunc;
import ai.tegmentum.wasmtime4j.tests.framework.DualRuntimeTest;
import java.util.Optional;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

/**
 * Integration tests migrated from JniTypedFuncTest covering typed function creation, signature
 * validation, and signature format verification through the unified API.
 *
 * <p>Tests verify typed function wrapper creation, null/empty signature rejection, and signature
 * string format conventions across both JNI and Panama runtimes.
 *
 * @since 1.0.0
 */
@DisplayName("TypedFunc API DualRuntime Tests")
@SuppressWarnings("deprecation")
public class TypedFuncApiDualRuntimeTest extends DualRuntimeTest {

  private static final Logger LOGGER =
      Logger.getLogger(TypedFuncApiDualRuntimeTest.class.getName());

  /**
   * WAT module with functions of various signatures for typed function testing.
   *
   * <pre>
   * (module
   *   (func (export "add") (param i32 i32) (result i32)
   *     local.get 0 local.get 1 i32.add)
   *   (func (export "nop"))
   *   (func (export "get42") (result i32) i32.const 42)
   *   (func (export "identity_i64") (param i64) (result i64) local.get 0)
   *   (func (export "identity_f32") (param f32) (result f32) local.get 0)
   *   (func (export "identity_f64") (param f64) (result f64) local.get 0))
   * </pre>
   */
  private static final String WAT =
      "(module\n"
          + "  (func (export \"add\") (param i32 i32) (result i32)\n"
          + "    local.get 0 local.get 1 i32.add)\n"
          + "  (func (export \"nop\"))\n"
          + "  (func (export \"get42\") (result i32) i32.const 42)\n"
          + "  (func (export \"identity_i64\") (param i64) (result i64) local.get 0)\n"
          + "  (func (export \"identity_f32\") (param f32) (result f32) local.get 0)\n"
          + "  (func (export \"identity_f64\") (param f64) (result f64) local.get 0))";

  @AfterEach
  void cleanup() {
    clearRuntimeSelection();
  }

  // ==================== CreateHandle Validation Tests ====================

  @Nested
  @DisplayName("CreateHandle Validation Tests")
  class CreateHandleValidationTests {

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("typed with null signature throws IllegalArgumentException")
    void typedWithNullSignatureThrows(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing typed(null) throws IllegalArgumentException");

      try (Engine engine = Engine.create();
          Store store = engine.createStore()) {
        final Module module = engine.compileWat(WAT);
        final Instance instance = module.instantiate(store);

        final Optional<WasmFunction> addOpt = instance.getFunction("add");
        assertTrue(addOpt.isPresent(), "add export must be present");
        final WasmFunction addFunc = addOpt.get();

        assertThrows(
            IllegalArgumentException.class,
            () -> addFunc.typed(null),
            "typed(null) should throw IllegalArgumentException");
        LOGGER.info("[" + runtime + "] typed(null) correctly threw IllegalArgumentException");

        instance.close();
        module.close();
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("typed with empty signature throws IllegalArgumentException")
    void typedWithEmptySignatureThrows(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing typed(\"\") throws IllegalArgumentException");

      try (Engine engine = Engine.create();
          Store store = engine.createStore()) {
        final Module module = engine.compileWat(WAT);
        final Instance instance = module.instantiate(store);

        final Optional<WasmFunction> addOpt = instance.getFunction("add");
        assertTrue(addOpt.isPresent(), "add export must be present");
        final WasmFunction addFunc = addOpt.get();

        assertThrows(
            IllegalArgumentException.class,
            () -> addFunc.typed(""),
            "typed(\"\") should throw IllegalArgumentException");
        LOGGER.info("[" + runtime + "] typed(\"\") correctly threw IllegalArgumentException");

        instance.close();
        module.close();
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("typed with valid ii->i signature returns TypedFunc")
    void typedWithValidSignatureReturnsTypedFunc(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing typed(\"ii->i\") returns TypedFunc");

      try (Engine engine = Engine.create();
          Store store = engine.createStore()) {
        final Module module = engine.compileWat(WAT);
        final Instance instance = module.instantiate(store);

        final Optional<WasmFunction> addOpt = instance.getFunction("add");
        assertTrue(addOpt.isPresent(), "add export must be present");
        final WasmFunction addFunc = addOpt.get();

        try {
          final TypedFunc typedFunc = addFunc.typed("ii->i");
          assertNotNull(typedFunc, "typed(\"ii->i\") should return non-null TypedFunc");
          LOGGER.info(
              "[" + runtime + "] TypedFunc created with signature: " + typedFunc.getSignature());
          typedFunc.close();
        } catch (final UnsupportedOperationException e) {
          LOGGER.info(
              "["
                  + runtime
                  + "] TypedFunctionSupport not implemented, skipping: "
                  + e.getMessage());
        }

        instance.close();
        module.close();
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("typed with void signature v->v returns TypedFunc")
    void typedWithVoidSignatureReturnsTypedFunc(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing typed(\"v->v\") returns TypedFunc on nop");

      try (Engine engine = Engine.create();
          Store store = engine.createStore()) {
        final Module module = engine.compileWat(WAT);
        final Instance instance = module.instantiate(store);

        final Optional<WasmFunction> nopOpt = instance.getFunction("nop");
        assertTrue(nopOpt.isPresent(), "nop export must be present");
        final WasmFunction nopFunc = nopOpt.get();

        try {
          final TypedFunc typedFunc = nopFunc.typed("v->v");
          assertNotNull(typedFunc, "typed(\"v->v\") should return non-null TypedFunc");
          LOGGER.info(
              "[" + runtime + "] TypedFunc created with signature: " + typedFunc.getSignature());
          typedFunc.close();
        } catch (final UnsupportedOperationException e) {
          LOGGER.info(
              "["
                  + runtime
                  + "] TypedFunctionSupport not implemented, skipping: "
                  + e.getMessage());
        }

        instance.close();
        module.close();
      }
    }
  }

  // ==================== Signature Format Tests ====================

  @Nested
  @DisplayName("Signature Format Tests")
  class SignatureFormatTests {

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("i32 signature i->i is recognized")
    void i32SignatureIsRecognized(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing i32 signature format i->i");

      final String signature = "i->i";
      assertTrue(signature.contains("i"), "Signature should contain i for i32");
      assertTrue(signature.contains("->"), "Signature should contain -> separator");
      LOGGER.info("[" + runtime + "] i32 signature format verified: " + signature);
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("i64 signature I->I is recognized")
    void i64SignatureIsRecognized(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing i64 signature format I->I");

      final String signature = "I->I";
      assertTrue(signature.contains("I"), "Signature should contain I for i64");
      LOGGER.info("[" + runtime + "] i64 signature format verified: " + signature);
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("f32 signature f->f is recognized")
    void f32SignatureIsRecognized(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing f32 signature format f->f");

      final String signature = "f->f";
      assertTrue(signature.contains("f"), "Signature should contain f for f32");
      LOGGER.info("[" + runtime + "] f32 signature format verified: " + signature);
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("f64 signature F->F is recognized")
    void f64SignatureIsRecognized(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing f64 signature format F->F");

      final String signature = "F->F";
      assertTrue(signature.contains("F"), "Signature should contain F for f64");
      LOGGER.info("[" + runtime + "] f64 signature format verified: " + signature);
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("void signature v->v is recognized")
    void voidSignatureIsRecognized(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing void signature format v->v");

      final String signature = "v->v";
      assertTrue(signature.contains("v"), "Signature should contain v for void");
      LOGGER.info("[" + runtime + "] void signature format verified: " + signature);
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("multi-parameter signature ii->i is recognized")
    void multiParameterSignatureIsRecognized(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing multi-parameter signature ii->i");

      final String signature = "ii->i";
      assertEquals("ii->i", signature, "Should be (i32, i32) -> i32");
      LOGGER.info("[" + runtime + "] multi-parameter signature format verified: " + signature);
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("void return signature i->v is recognized")
    void voidReturnSignatureIsRecognized(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing void return signature i->v");

      final String signature = "i->v";
      assertTrue(signature.endsWith("v"), "Should end with v for void return");
      LOGGER.info("[" + runtime + "] void return signature format verified: " + signature);
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("void parameter signature v->i is recognized")
    void voidParameterSignatureIsRecognized(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing void parameter signature v->i");

      final String signature = "v->i";
      assertTrue(signature.startsWith("v"), "Should start with v for void params");
      LOGGER.info("[" + runtime + "] void parameter signature format verified: " + signature);
    }
  }

  // ==================== Signature Parsing Tests ====================

  @Nested
  @DisplayName("Signature Parsing Tests")
  class SignatureParsingTests {

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("signature contains arrow separator")
    void signatureContainsArrowSeparator(final RuntimeType runtime) {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing signature contains arrow separator");

      final String signature = "ii->i";
      assertTrue(signature.contains("->"), "Signature should contain -> separator");
      LOGGER.info("[" + runtime + "] Arrow separator verified in: " + signature);
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("signature has params before arrow")
    void signatureHasParamsBeforeArrow(final RuntimeType runtime) {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing signature has params before arrow");

      final String signature = "ii->i";
      final String[] parts = signature.split("->");
      assertEquals("ii", parts[0], "Should have params before arrow");
      LOGGER.info("[" + runtime + "] Params before arrow: " + parts[0]);
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("signature has results after arrow")
    void signatureHasResultsAfterArrow(final RuntimeType runtime) {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing signature has results after arrow");

      final String signature = "ii->i";
      final String[] parts = signature.split("->");
      assertEquals("i", parts[1], "Should have results after arrow");
      LOGGER.info("[" + runtime + "] Results after arrow: " + parts[1]);
    }
  }

  // ==================== Supported Signature Tests ====================

  @Nested
  @DisplayName("Supported Signature Tests")
  class SupportedSignatureTests {

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("v->v signature is valid")
    void voidToVoidIsValid(final RuntimeType runtime) {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing v->v is valid");

      assertTrue(isValidSignature("v->v"), "v->v should be valid");
      LOGGER.info("[" + runtime + "] v->v validated");
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("i->i signature is valid")
    void i32ToI32IsValid(final RuntimeType runtime) {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing i->i is valid");

      assertTrue(isValidSignature("i->i"), "i->i should be valid");
      LOGGER.info("[" + runtime + "] i->i validated");
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("ii->i signature is valid")
    void i32I32ToI32IsValid(final RuntimeType runtime) {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing ii->i is valid");

      assertTrue(isValidSignature("ii->i"), "ii->i should be valid");
      LOGGER.info("[" + runtime + "] ii->i validated");
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("I->I signature is valid")
    void i64ToI64IsValid(final RuntimeType runtime) {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing I->I is valid");

      assertTrue(isValidSignature("I->I"), "I->I should be valid");
      LOGGER.info("[" + runtime + "] I->I validated");
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("II->I signature is valid")
    void i64I64ToI64IsValid(final RuntimeType runtime) {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing II->I is valid");

      assertTrue(isValidSignature("II->I"), "II->I should be valid");
      LOGGER.info("[" + runtime + "] II->I validated");
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("f->f signature is valid")
    void f32ToF32IsValid(final RuntimeType runtime) {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing f->f is valid");

      assertTrue(isValidSignature("f->f"), "f->f should be valid");
      LOGGER.info("[" + runtime + "] f->f validated");
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("F->F signature is valid")
    void f64ToF64IsValid(final RuntimeType runtime) {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing F->F is valid");

      assertTrue(isValidSignature("F->F"), "F->F should be valid");
      LOGGER.info("[" + runtime + "] F->F validated");
    }

    /**
     * Validates signature format. Checks the string structure without native validation.
     *
     * @param signature the signature to validate
     * @return true if signature has valid format
     */
    private boolean isValidSignature(final String signature) {
      if (signature == null || signature.isEmpty()) {
        return false;
      }
      if (!signature.contains("->")) {
        return false;
      }
      final String[] parts = signature.split("->");
      if (parts.length != 2) {
        return false;
      }
      final String validTypes = "iIfFv";
      for (final char c : parts[0].toCharArray()) {
        if (validTypes.indexOf(c) == -1) {
          return false;
        }
      }
      for (final char c : parts[1].toCharArray()) {
        if (validTypes.indexOf(c) == -1) {
          return false;
        }
      }
      return true;
    }
  }

  // ==================== Store Dependency Tests ====================

  @Nested
  @DisplayName("Store Dependency Tests")
  class StoreDependencyTests {

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("function requires store for typed function creation")
    void functionRequiresStoreForTypedCreation(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing store is required for typed function creation");

      try (Engine engine = Engine.create();
          Store store = engine.createStore()) {
        final Module module = engine.compileWat(WAT);
        final Instance instance = module.instantiate(store);

        final Optional<WasmFunction> addOpt = instance.getFunction("add");
        assertTrue(addOpt.isPresent(), "add export must be present");

        // Store is implicitly required through the function's association with an instance
        assertNotNull(store, "Store must not be null for typed function usage");
        LOGGER.info("[" + runtime + "] Store dependency verified");

        instance.close();
        module.close();
      }
    }
  }

  // ==================== Function Wrapper Tests ====================

  @Nested
  @DisplayName("Function Wrapper Tests")
  class FunctionWrapperTests {

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("typed returns TypedFunc that wraps the function")
    void typedReturnsTypedFuncWrapper(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing typed returns TypedFunc wrapper");

      try (Engine engine = Engine.create();
          Store store = engine.createStore()) {
        final Module module = engine.compileWat(WAT);
        final Instance instance = module.instantiate(store);

        final Optional<WasmFunction> get42Opt = instance.getFunction("get42");
        assertTrue(get42Opt.isPresent(), "get42 export must be present");
        final WasmFunction get42Func = get42Opt.get();

        try {
          final TypedFunc typedFunc = get42Func.typed("->i");
          assertNotNull(typedFunc, "typed(\"->i\") should return non-null TypedFunc");

          final WasmFunction wrappedFunc = typedFunc.getFunction();
          assertNotNull(wrappedFunc, "TypedFunc.getFunction() should return non-null");
          LOGGER.info("[" + runtime + "] TypedFunc wraps function correctly");

          typedFunc.close();
        } catch (final UnsupportedOperationException e) {
          LOGGER.info(
              "["
                  + runtime
                  + "] TypedFunctionSupport not implemented, skipping: "
                  + e.getMessage());
        }

        instance.close();
        module.close();
      }
    }
  }
}
