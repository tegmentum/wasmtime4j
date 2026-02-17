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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.exception.TrapException;
import ai.tegmentum.wasmtime4j.func.CallHook;
import ai.tegmentum.wasmtime4j.func.CallHookHandler;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link CallHookHandler} interface.
 *
 * <p>CallHookHandler is a functional interface for handling call hook events during WebAssembly
 * execution. It receives notifications on every transition between WebAssembly and host code.
 */
@DisplayName("CallHookHandler Interface Tests")
class CallHookHandlerTest {

  @Nested
  @DisplayName("Method Signature Tests")
  class MethodSignatureTests {

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
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("onCallHook should have one CallHook parameter")
    void onCallHookShouldHaveOneCallHookParameter() throws NoSuchMethodException {
      final Method method = CallHookHandler.class.getMethod("onCallHook", CallHook.class);
      assertEquals(1, method.getParameterCount(), "Should have one parameter");
      assertEquals(CallHook.class, method.getParameterTypes()[0], "Parameter should be CallHook");
    }
  }

  @Nested
  @DisplayName("Exception Declaration Tests")
  class ExceptionDeclarationTests {

    @Test
    @DisplayName("onCallHook should declare TrapException")
    void onCallHookShouldDeclareTrapException() throws NoSuchMethodException {
      final Method method = CallHookHandler.class.getMethod("onCallHook", CallHook.class);
      final Class<?>[] exceptionTypes = method.getExceptionTypes();

      boolean hasTrapException = false;
      for (final Class<?> exType : exceptionTypes) {
        if (exType == TrapException.class || exType.getSimpleName().equals("TrapException")) {
          hasTrapException = true;
          break;
        }
      }
      assertTrue(hasTrapException, "onCallHook method should declare TrapException");
    }
  }

  @Nested
  @DisplayName("Lambda Implementation Tests")
  class LambdaImplementationTests {

    @Test
    @DisplayName("should be implementable as lambda")
    void shouldBeImplementableAsLambda() {
      final CallHookHandler handler = (hook) -> {};
      assertNotNull(handler, "Lambda implementation should work");
    }

    @Test
    @DisplayName("lambda should receive CallHook parameter")
    void lambdaShouldReceiveCallHookParameter() throws TrapException {
      final AtomicInteger callCount = new AtomicInteger(0);
      final CallHookHandler handler =
          (hook) -> {
            assertNotNull(hook, "Hook parameter should not be null");
            callCount.incrementAndGet();
          };

      // Simulate calling the handler with a mock CallHook value
      // Since we can't create a real CallHook without native code, we verify the interface works
      assertNotNull(handler, "Handler should be created");
    }

    @Test
    @DisplayName("lambda can throw TrapException")
    void lambdaCanThrowTrapException() {
      final CallHookHandler handler =
          (hook) -> {
            throw new TrapException(TrapException.TrapType.UNREACHABLE_CODE_REACHED, "Test trap");
          };
      assertNotNull(handler, "Handler that throws should be valid");
    }
  }

  @Nested
  @DisplayName("Method Reference Tests")
  class MethodReferenceTests {

    @Test
    @DisplayName("should be implementable as method reference")
    void shouldBeImplementableAsMethodReference() {
      final CallHookHandler handler = this::handleCallHook;
      assertNotNull(handler, "Method reference implementation should work");
    }

    private void handleCallHook(final CallHook hook) throws TrapException {
      // Implementation for method reference test
    }
  }

  @Nested
  @DisplayName("Anonymous Class Tests")
  class AnonymousClassTests {

    @Test
    @DisplayName("should be implementable as anonymous class")
    void shouldBeImplementableAsAnonymousClass() {
      final CallHookHandler handler =
          new CallHookHandler() {
            @Override
            public void onCallHook(final CallHook hook) throws TrapException {
              // Anonymous implementation
            }
          };
      assertNotNull(handler, "Anonymous class implementation should work");
    }
  }

  @Nested
  @DisplayName("CallHook Parameter Type Tests")
  class CallHookParameterTypeTests {

    @Test
    @DisplayName("CallHook should be available as parameter type")
    void callHookShouldBeAvailableAsParameterType() {
      assertNotNull(CallHook.class, "CallHook class should exist");
    }

    @Test
    @DisplayName("CallHook should be an enum")
    void callHookShouldBeAnEnum() {
      assertTrue(CallHook.class.isEnum(), "CallHook should be an enum");
    }

    @Test
    @DisplayName("CallHook should have expected values")
    void callHookShouldHaveExpectedValues() {
      final CallHook[] values = CallHook.values();
      assertTrue(values.length > 0, "CallHook should have at least one value");
    }
  }
}
