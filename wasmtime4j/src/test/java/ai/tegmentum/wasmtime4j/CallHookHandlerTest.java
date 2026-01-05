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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.exception.TrapException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive test suite for the CallHookHandler interface.
 *
 * <p>The CallHookHandler is a functional interface used with Store#setCallHook to receive
 * notifications on every transition between WebAssembly and host code. This test verifies the
 * interface structure and API conformance.
 */
@DisplayName("CallHookHandler Interface Tests")
class CallHookHandlerTest {

  // ========================================================================
  // Type Definition Tests
  // ========================================================================

  @Nested
  @DisplayName("Type Definition Tests")
  class TypeDefinitionTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeAnInterface() {
      assertTrue(CallHookHandler.class.isInterface(), "CallHookHandler should be an interface");
    }

    @Test
    @DisplayName("should be public")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(CallHookHandler.class.getModifiers()),
          "CallHookHandler should be public");
    }

    @Test
    @DisplayName("should be annotated with @FunctionalInterface")
    void shouldBeFunctionalInterface() {
      assertTrue(
          CallHookHandler.class.isAnnotationPresent(FunctionalInterface.class),
          "CallHookHandler should be annotated with @FunctionalInterface");
    }
  }

  // ========================================================================
  // Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Method Tests")
  class MethodTests {

    @Test
    @DisplayName("should have exactly one abstract method")
    void shouldHaveExactlyOneAbstractMethod() {
      long abstractMethodCount =
          Arrays.stream(CallHookHandler.class.getDeclaredMethods())
              .filter(m -> Modifier.isAbstract(m.getModifiers()))
              .count();
      assertEquals(
          1,
          abstractMethodCount,
          "CallHookHandler should have exactly one abstract method as a functional interface");
    }

    @Test
    @DisplayName("should have onCallHook method")
    void shouldHaveOnCallHookMethod() throws NoSuchMethodException {
      final Method method = CallHookHandler.class.getMethod("onCallHook", CallHook.class);
      assertNotNull(method, "onCallHook method should exist");
    }

    @Test
    @DisplayName("onCallHook should return void")
    void onCallHookShouldReturnVoid() throws NoSuchMethodException {
      final Method method = CallHookHandler.class.getMethod("onCallHook", CallHook.class);
      assertEquals(void.class, method.getReturnType(), "onCallHook should return void");
    }

    @Test
    @DisplayName("onCallHook should accept CallHook parameter")
    void onCallHookShouldAcceptCallHookParameter() throws NoSuchMethodException {
      final Method method = CallHookHandler.class.getMethod("onCallHook", CallHook.class);
      Class<?>[] parameterTypes = method.getParameterTypes();
      assertEquals(1, parameterTypes.length, "onCallHook should have exactly one parameter");
      assertEquals(
          CallHook.class, parameterTypes[0], "onCallHook parameter should be of type CallHook");
    }

    @Test
    @DisplayName("onCallHook should declare TrapException")
    void onCallHookShouldDeclareTrapException() throws NoSuchMethodException {
      final Method method = CallHookHandler.class.getMethod("onCallHook", CallHook.class);
      Class<?>[] exceptionTypes = method.getExceptionTypes();
      assertTrue(
          Arrays.asList(exceptionTypes).contains(TrapException.class),
          "onCallHook should declare TrapException");
    }

    @Test
    @DisplayName("onCallHook should be abstract")
    void onCallHookShouldBeAbstract() throws NoSuchMethodException {
      final Method method = CallHookHandler.class.getMethod("onCallHook", CallHook.class);
      assertTrue(
          Modifier.isAbstract(method.getModifiers()), "onCallHook should be an abstract method");
    }

    @Test
    @DisplayName("onCallHook should be public")
    void onCallHookShouldBePublic() throws NoSuchMethodException {
      final Method method = CallHookHandler.class.getMethod("onCallHook", CallHook.class);
      assertTrue(Modifier.isPublic(method.getModifiers()), "onCallHook should be public");
    }
  }

  // ========================================================================
  // Exception Declaration Tests
  // ========================================================================

  @Nested
  @DisplayName("Exception Declaration Tests")
  class ExceptionDeclarationTests {

    @Test
    @DisplayName("onCallHook should declare only TrapException")
    void onCallHookShouldDeclareOnlyTrapException() throws NoSuchMethodException {
      final Method method = CallHookHandler.class.getMethod("onCallHook", CallHook.class);
      Class<?>[] exceptionTypes = method.getExceptionTypes();
      assertArrayEquals(
          new Class<?>[] {TrapException.class},
          exceptionTypes,
          "onCallHook should declare only TrapException");
    }
  }

  // ========================================================================
  // Lambda Implementation Tests
  // ========================================================================

  @Nested
  @DisplayName("Lambda Implementation Tests")
  class LambdaImplementationTests {

    @Test
    @DisplayName("should be implementable as lambda expression")
    void shouldBeImplementableAsLambda() {
      // This test verifies that CallHookHandler can be used as a lambda
      // which is the primary use case for functional interfaces
      CallHookHandler handler = (hook) -> {};
      assertNotNull(handler, "CallHookHandler should be implementable as lambda");
    }

    @Test
    @DisplayName("lambda should be able to access hook parameter")
    void lambdaShouldBeAbleToAccessHookParameter() {
      final CallHook[] capturedHook = new CallHook[1];
      CallHookHandler handler =
          (hook) -> {
            capturedHook[0] = hook;
          };

      // Verify the handler can be called with each CallHook value
      for (CallHook hook : CallHook.values()) {
        try {
          handler.onCallHook(hook);
          assertEquals(hook, capturedHook[0], "Handler should capture the hook value");
        } catch (TrapException e) {
          // Not expected in this test
          throw new RuntimeException("Unexpected TrapException", e);
        }
      }
    }

    @Test
    @DisplayName("should be implementable as method reference")
    void shouldBeImplementableAsMethodReference() {
      CallHookHandler handler = this::handleCallHook;
      assertNotNull(handler, "CallHookHandler should be implementable as method reference");
    }

    private void handleCallHook(final CallHook hook) throws TrapException {
      // Method reference target
    }
  }

  // ========================================================================
  // Method Count Tests
  // ========================================================================

  @Nested
  @DisplayName("Method Count Tests")
  class MethodCountTests {

    @Test
    @DisplayName("should have exactly one declared method")
    void shouldHaveExactlyOneDeclaredMethod() {
      // Functional interfaces should have exactly one abstract method
      int methodCount = CallHookHandler.class.getDeclaredMethods().length;
      assertEquals(
          1,
          methodCount,
          "CallHookHandler should have exactly one declared method, found: " + methodCount);
    }
  }
}
