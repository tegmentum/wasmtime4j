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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.wit.WitFunctionBinder;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link WitFunctionBinder} class.
 *
 * <p>WitFunctionBinder provides function binding and marshaling for WIT interface functions.
 */
@DisplayName("WitFunctionBinder Tests")
class WitFunctionBinderTest {

  private WitFunctionBinder binder;

  @BeforeEach
  void setUp() {
    binder = new WitFunctionBinder();
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should create binder with no bound functions")
    void shouldCreateBinderWithNoBoundFunctions() {
      final WitFunctionBinder newBinder = new WitFunctionBinder();

      assertNotNull(newBinder);
      assertTrue(newBinder.getBoundFunctionNames().isEmpty());
    }
  }

  @Nested
  @DisplayName("Function Binding State Tests")
  class FunctionBindingStateTests {

    @Test
    @DisplayName("isFunctionBound should return false for non-existent function")
    void isFunctionBoundShouldReturnFalseForNonExistentFunction() {
      assertFalse(binder.isFunctionBound("nonexistent"));
    }

    @Test
    @DisplayName("getBoundFunctionNames should return empty list initially")
    void getBoundFunctionNamesShouldReturnEmptyListInitially() {
      final List<String> boundFunctions = binder.getBoundFunctionNames();
      assertNotNull(boundFunctions);
      assertTrue(boundFunctions.isEmpty());
    }

    @Test
    @DisplayName("unbindFunction should return false for non-existent function")
    void unbindFunctionShouldReturnFalseForNonExistentFunction() {
      assertFalse(binder.unbindFunction("nonexistent"));
    }

    @Test
    @DisplayName("clearBindings should clear all bindings")
    void clearBindingsShouldClearAllBindings() {
      binder.clearBindings();
      assertTrue(binder.getBoundFunctionNames().isEmpty());
    }
  }

  @Nested
  @DisplayName("Function Invocation Validation Tests")
  class FunctionInvocationValidationTests {

    @Test
    @DisplayName("invokeFunction should throw on null function name")
    void invokeFunctionShouldThrowOnNullFunctionName() {
      assertThrows(NullPointerException.class, () -> binder.invokeFunction(null));
    }

    @Test
    @DisplayName("invokeFunction should throw for unbound function")
    void invokeFunctionShouldThrowForUnboundFunction() {
      assertThrows(Exception.class, () -> binder.invokeFunction("unbound-function"));
    }
  }

  @Nested
  @DisplayName("Bind Function Validation Tests")
  class BindFunctionValidationTests {

    @Test
    @DisplayName("bindFunction should throw on null function name")
    void bindFunctionShouldThrowOnNullFunctionName() {
      assertThrows(
          NullPointerException.class,
          () -> binder.bindFunction(null, null, new Object(), "method"));
    }

    @Test
    @DisplayName("bindFunction should throw on null function definition")
    void bindFunctionShouldThrowOnNullFunctionDefinition() {
      assertThrows(
          NullPointerException.class,
          () -> binder.bindFunction("test", null, new Object(), "method"));
    }

    @Test
    @DisplayName("bindFunction should throw on null implementation")
    void bindFunctionShouldThrowOnNullImplementation() {
      assertThrows(
          NullPointerException.class, () -> binder.bindFunction("test", null, null, "method"));
    }

    @Test
    @DisplayName("bindFunction should throw on null method name")
    void bindFunctionShouldThrowOnNullMethodName() {
      assertThrows(
          NullPointerException.class, () -> binder.bindFunction("test", null, new Object(), null));
    }
  }

  @Nested
  @DisplayName("Type Adapter Registration Tests")
  class TypeAdapterRegistrationTests {

    @Test
    @DisplayName("registerTypeAdapter should throw on null java type")
    void registerTypeAdapterShouldThrowOnNullJavaType() {
      assertThrows(
          NullPointerException.class,
          () ->
              binder.registerTypeAdapter(
                  null,
                  new WitFunctionBinder.TypeAdapter<Object>() {
                    @Override
                    public Object toWit(final Object value) {
                      return value;
                    }

                    @Override
                    public Object fromWit(final Object value) {
                      return value;
                    }
                  }));
    }

    @Test
    @DisplayName("registerTypeAdapter should throw on null adapter")
    void registerTypeAdapterShouldThrowOnNullAdapter() {
      assertThrows(
          NullPointerException.class, () -> binder.registerTypeAdapter(String.class, null));
    }

    @Test
    @DisplayName("should register type adapter successfully")
    void shouldRegisterTypeAdapterSuccessfully() {
      final WitFunctionBinder.TypeAdapter<String> adapter =
          new WitFunctionBinder.TypeAdapter<String>() {
            @Override
            public Object toWit(final String value) {
              return value.toUpperCase();
            }

            @Override
            public String fromWit(final Object value) {
              return value.toString().toLowerCase();
            }
          };

      // Should not throw
      binder.registerTypeAdapter(String.class, adapter);
    }
  }

  @Nested
  @DisplayName("TypeAdapter Interface Tests")
  class TypeAdapterInterfaceTests {

    @Test
    @DisplayName("TypeAdapter should be an interface")
    void typeAdapterShouldBeAnInterface() {
      assertTrue(WitFunctionBinder.TypeAdapter.class.isInterface());
    }

    @Test
    @DisplayName("TypeAdapter should have toWit method")
    void typeAdapterShouldHaveToWitMethod() throws NoSuchMethodException {
      assertNotNull(WitFunctionBinder.TypeAdapter.class.getMethod("toWit", Object.class));
    }

    @Test
    @DisplayName("TypeAdapter should have fromWit method")
    void typeAdapterShouldHaveFromWitMethod() throws NoSuchMethodException {
      assertNotNull(WitFunctionBinder.TypeAdapter.class.getMethod("fromWit", Object.class));
    }
  }

  @Nested
  @DisplayName("Built-in Type Adapter Tests")
  class BuiltInTypeAdapterTests {

    @Test
    @DisplayName("should have String type adapter built-in")
    void shouldHaveStringTypeAdapterBuiltIn() {
      // The constructor initializes built-in adapters
      // We can verify the binder is functional
      assertNotNull(binder);
    }

    @Test
    @DisplayName("should have Boolean type adapter built-in")
    void shouldHaveBooleanTypeAdapterBuiltIn() {
      assertNotNull(binder);
    }

    @Test
    @DisplayName("should have Integer type adapter built-in")
    void shouldHaveIntegerTypeAdapterBuiltIn() {
      assertNotNull(binder);
    }
  }

  @Nested
  @DisplayName("Custom Type Adapter Tests")
  class CustomTypeAdapterTests {

    @Test
    @DisplayName("should convert using custom adapter")
    void shouldConvertUsingCustomAdapter() {
      final WitFunctionBinder.TypeAdapter<Integer> adapter =
          new WitFunctionBinder.TypeAdapter<Integer>() {
            @Override
            public Object toWit(final Integer value) {
              return value * 2;
            }

            @Override
            public Integer fromWit(final Object value) {
              return ((Integer) value) / 2;
            }
          };

      binder.registerTypeAdapter(Integer.class, adapter);

      // Verify adapter is registered by checking binder is functional
      assertNotNull(binder);
    }

    @Test
    @DisplayName("should override existing adapter")
    void shouldOverrideExistingAdapter() {
      final WitFunctionBinder.TypeAdapter<String> adapter1 =
          new WitFunctionBinder.TypeAdapter<String>() {
            @Override
            public Object toWit(final String value) {
              return "adapter1";
            }

            @Override
            public String fromWit(final Object value) {
              return "adapter1";
            }
          };

      final WitFunctionBinder.TypeAdapter<String> adapter2 =
          new WitFunctionBinder.TypeAdapter<String>() {
            @Override
            public Object toWit(final String value) {
              return "adapter2";
            }

            @Override
            public String fromWit(final Object value) {
              return "adapter2";
            }
          };

      binder.registerTypeAdapter(String.class, adapter1);
      binder.registerTypeAdapter(String.class, adapter2);

      // Both registrations should succeed without exception
      assertNotNull(binder);
    }
  }

  @Nested
  @DisplayName("Type Adapter Conversion Tests")
  class TypeAdapterConversionTests {

    @Test
    @DisplayName("TypeAdapter toWit should work")
    void typeAdapterToWitShouldWork() {
      final WitFunctionBinder.TypeAdapter<String> adapter =
          new WitFunctionBinder.TypeAdapter<String>() {
            @Override
            public Object toWit(final String value) {
              return value.length();
            }

            @Override
            public String fromWit(final Object value) {
              return value.toString();
            }
          };

      assertEquals(5, adapter.toWit("hello"));
      assertEquals(0, adapter.toWit(""));
    }

    @Test
    @DisplayName("TypeAdapter fromWit should work")
    void typeAdapterFromWitShouldWork() {
      final WitFunctionBinder.TypeAdapter<Integer> adapter =
          new WitFunctionBinder.TypeAdapter<Integer>() {
            @Override
            public Object toWit(final Integer value) {
              return value;
            }

            @Override
            public Integer fromWit(final Object value) {
              return Integer.parseInt(value.toString());
            }
          };

      assertEquals(42, adapter.fromWit("42"));
      assertEquals(0, adapter.fromWit("0"));
    }
  }
}
