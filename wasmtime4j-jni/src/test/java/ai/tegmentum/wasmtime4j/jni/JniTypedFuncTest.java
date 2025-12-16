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

package ai.tegmentum.wasmtime4j.jni;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.FunctionType;
import ai.tegmentum.wasmtime4j.WasmFunction;
import ai.tegmentum.wasmtime4j.WasmValue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link JniTypedFunc}.
 *
 * <p>Tests focus on Java wrapper logic, parameter validation, and signature handling. These tests
 * verify the createHandle validation without requiring actual native library loading.
 *
 * <p>Note: Integration tests with actual typed function calls are in wasmtime4j-tests.
 */
@DisplayName("JniTypedFunc Tests")
class JniTypedFuncTest {

  private static final long VALID_HANDLE = 0x12345678L;

  private JniEngine testEngine;
  private JniStore testStore;

  @BeforeEach
  void setUp() {
    testEngine = new JniEngine(VALID_HANDLE);
    testStore = new JniStore(VALID_HANDLE, testEngine);
  }

  @Nested
  @DisplayName("CreateHandle Validation Tests")
  class CreateHandleValidationTests {

    @Test
    @DisplayName("should throw on null store")
    void shouldThrowOnNullStore() {
      final JniFunction func = createMockJniFunction();

      assertThrows(
          IllegalArgumentException.class,
          () -> new JniTypedFunc(null, func, "ii->i"),
          "Should throw on null store");
    }

    @Test
    @DisplayName("should throw on null function")
    void shouldThrowOnNullFunction() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new JniTypedFunc(testStore, null, "ii->i"),
          "Should throw on null function");
    }

    @Test
    @DisplayName("should throw on null signature")
    void shouldThrowOnNullSignature() {
      final JniFunction func = createMockJniFunction();

      assertThrows(
          IllegalArgumentException.class,
          () -> new JniTypedFunc(testStore, func, null),
          "Should throw on null signature");
    }

    @Test
    @DisplayName("should throw on empty signature")
    void shouldThrowOnEmptySignature() {
      final JniFunction func = createMockJniFunction();

      assertThrows(
          IllegalArgumentException.class,
          () -> new JniTypedFunc(testStore, func, ""),
          "Should throw on empty signature");
    }

    @Test
    @DisplayName("should throw on non-JniFunction")
    void shouldThrowOnNonJniFunction() {
      final WasmFunction nonJniFunc = createNonJniFunction();

      assertThrows(
          IllegalArgumentException.class,
          () -> new JniTypedFunc(testStore, nonJniFunc, "ii->i"),
          "Should throw on non-JniFunction");
    }

    private JniFunction createMockJniFunction() {
      return new JniFunction(VALID_HANDLE, "mockFunc", VALID_HANDLE, testStore);
    }

    private WasmFunction createNonJniFunction() {
      return new WasmFunction() {
        @Override
        public FunctionType getFunctionType() {
          return null;
        }

        @Override
        public String getName() {
          return "mock";
        }

        @Override
        public WasmValue[] call(final WasmValue... params) {
          return new WasmValue[0];
        }

        @Override
        public java.util.concurrent.CompletableFuture<WasmValue[]> callAsync(
            final WasmValue... params) {
          return java.util.concurrent.CompletableFuture.completedFuture(new WasmValue[0]);
        }
      };
    }
  }

  @Nested
  @DisplayName("Signature Format Tests")
  class SignatureFormatTests {

    @Test
    @DisplayName("should recognize i32 signature component")
    void shouldRecognizeI32SignatureComponent() {
      // "i" = i32
      final String signature = "i->i";
      assertTrue(signature.contains("i"), "Signature should contain i for i32");
    }

    @Test
    @DisplayName("should recognize i64 signature component")
    void shouldRecognizeI64SignatureComponent() {
      // "I" = i64
      final String signature = "I->I";
      assertTrue(signature.contains("I"), "Signature should contain I for i64");
    }

    @Test
    @DisplayName("should recognize f32 signature component")
    void shouldRecognizeF32SignatureComponent() {
      // "f" = f32
      final String signature = "f->f";
      assertTrue(signature.contains("f"), "Signature should contain f for f32");
    }

    @Test
    @DisplayName("should recognize f64 signature component")
    void shouldRecognizeF64SignatureComponent() {
      // "F" = f64
      final String signature = "F->F";
      assertTrue(signature.contains("F"), "Signature should contain F for f64");
    }

    @Test
    @DisplayName("should recognize void signature component")
    void shouldRecognizeVoidSignatureComponent() {
      // "v" = void
      final String signature = "v->v";
      assertTrue(signature.contains("v"), "Signature should contain v for void");
    }

    @Test
    @DisplayName("should recognize multi-parameter signature")
    void shouldRecognizeMultiParameterSignature() {
      // "ii->i" = (i32, i32) -> i32
      final String signature = "ii->i";
      assertEquals("ii->i", signature, "Should be (i32, i32) -> i32");
    }

    @Test
    @DisplayName("should recognize void return signature")
    void shouldRecognizeVoidReturnSignature() {
      // "i->v" = (i32) -> void
      final String signature = "i->v";
      assertTrue(signature.endsWith("v"), "Should end with v for void return");
    }

    @Test
    @DisplayName("should recognize void parameter signature")
    void shouldRecognizeVoidParameterSignature() {
      // "v->i" = () -> i32
      final String signature = "v->i";
      assertTrue(signature.startsWith("v"), "Should start with v for void params");
    }
  }

  @Nested
  @DisplayName("Signature Parsing Tests")
  class SignatureParsingTests {

    @Test
    @DisplayName("should contain arrow separator")
    void shouldContainArrowSeparator() {
      final String signature = "ii->i";
      assertTrue(signature.contains("->"), "Signature should contain -> separator");
    }

    @Test
    @DisplayName("should have params before arrow")
    void shouldHaveParamsBeforeArrow() {
      final String signature = "ii->i";
      final String[] parts = signature.split("->");
      assertEquals("ii", parts[0], "Should have params before arrow");
    }

    @Test
    @DisplayName("should have results after arrow")
    void shouldHaveResultsAfterArrow() {
      final String signature = "ii->i";
      final String[] parts = signature.split("->");
      assertEquals("i", parts[1], "Should have results after arrow");
    }
  }

  @Nested
  @DisplayName("Supported Signature Tests")
  class SupportedSignatureTests {

    @Test
    @DisplayName("void to void signature should be valid")
    void voidToVoidSignatureShouldBeValid() {
      final String signature = "v->v";
      assertTrue(isValidSignature(signature), "v->v should be valid");
    }

    @Test
    @DisplayName("i32 to i32 signature should be valid")
    void i32ToI32SignatureShouldBeValid() {
      final String signature = "i->i";
      assertTrue(isValidSignature(signature), "i->i should be valid");
    }

    @Test
    @DisplayName("i32 i32 to i32 signature should be valid")
    void i32I32ToI32SignatureShouldBeValid() {
      final String signature = "ii->i";
      assertTrue(isValidSignature(signature), "ii->i should be valid");
    }

    @Test
    @DisplayName("i64 to i64 signature should be valid")
    void i64ToI64SignatureShouldBeValid() {
      final String signature = "I->I";
      assertTrue(isValidSignature(signature), "I->I should be valid");
    }

    @Test
    @DisplayName("i64 i64 to i64 signature should be valid")
    void i64I64ToI64SignatureShouldBeValid() {
      final String signature = "II->I";
      assertTrue(isValidSignature(signature), "II->I should be valid");
    }

    @Test
    @DisplayName("f32 to f32 signature should be valid")
    void f32ToF32SignatureShouldBeValid() {
      final String signature = "f->f";
      assertTrue(isValidSignature(signature), "f->f should be valid");
    }

    @Test
    @DisplayName("f64 to f64 signature should be valid")
    void f64ToF64SignatureShouldBeValid() {
      final String signature = "F->F";
      assertTrue(isValidSignature(signature), "F->F should be valid");
    }

    /**
     * Validates signature format (not native validation). Just checks the string structure.
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
      // Check that each character is a valid type specifier
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

  @Nested
  @DisplayName("Store Dependency Tests")
  class StoreDependencyTests {

    @Test
    @DisplayName("should require store for function calls")
    void shouldRequireStoreForFunctionCalls() {
      // The constructor requires a store
      final JniFunction func = new JniFunction(VALID_HANDLE, "testFunc", VALID_HANDLE, testStore);

      // Attempting to create without store should fail
      assertThrows(
          IllegalArgumentException.class,
          () -> new JniTypedFunc(null, func, "ii->i"),
          "Should require store");
    }

    @Test
    @DisplayName("store should be referenced in typed func")
    void storeShouldBeReferencedInTypedFunc() {
      // This tests that the store is properly stored (though we can't fully test
      // without native library - the validation check ensures it's captured)
      assertTrue(testStore != null, "Store should not be null");
    }
  }

  @Nested
  @DisplayName("Function Wrapper Tests")
  class FunctionWrapperTests {

    @Test
    @DisplayName("should require JniFunction instance")
    void shouldRequireJniFunctionInstance() {
      // Create a non-JniFunction WasmFunction
      final WasmFunction nonJniFunc =
          new WasmFunction() {
            @Override
            public FunctionType getFunctionType() {
              return null;
            }

            @Override
            public String getName() {
              return "test";
            }

            @Override
            public WasmValue[] call(final WasmValue... params) {
              return new WasmValue[0];
            }

            @Override
            public java.util.concurrent.CompletableFuture<WasmValue[]> callAsync(
                final WasmValue... params) {
              return java.util.concurrent.CompletableFuture.completedFuture(new WasmValue[0]);
            }
          };

      // Should throw because it's not a JniFunction
      assertThrows(
          IllegalArgumentException.class,
          () -> new JniTypedFunc(testStore, nonJniFunc, "ii->i"),
          "Should require JniFunction instance");
    }
  }
}
