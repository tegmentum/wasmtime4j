/*
 * Copyright (c) 2024 Tegmentum AI. All rights reserved.
 */

package ai.tegmentum.wasmtime4j;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive test suite for the CallbackRegistry interface and its nested types.
 *
 * <p>The CallbackRegistry provides a centralized mechanism for managing function references,
 * callback invocations, and asynchronous operations between Java and WebAssembly.
 */
@DisplayName("CallbackRegistry Interface Tests")
class CallbackRegistryTest {

  // ========================================================================
  // CallbackRegistry Interface Tests
  // ========================================================================

  @Nested
  @DisplayName("CallbackRegistry Interface Definition Tests")
  class InterfaceDefinitionTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeAnInterface() {
      assertTrue(CallbackRegistry.class.isInterface(), "CallbackRegistry should be an interface");
    }

    @Test
    @DisplayName("should be public")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(CallbackRegistry.class.getModifiers()),
          "CallbackRegistry should be public");
    }
  }

  // ========================================================================
  // Registration Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Registration Method Tests")
  class RegistrationMethodTests {

    @Test
    @DisplayName("should have registerCallback method")
    void shouldHaveRegisterCallbackMethod() throws NoSuchMethodException {
      Method method =
          CallbackRegistry.class.getMethod(
              "registerCallback", String.class, HostFunction.class, FunctionType.class);
      assertNotNull(method, "registerCallback method should exist");
      assertEquals(
          CallbackRegistry.CallbackHandle.class,
          method.getReturnType(),
          "Return type should be CallbackHandle");
    }

    @Test
    @DisplayName("should have registerAsyncCallback method")
    void shouldHaveRegisterAsyncCallbackMethod() throws NoSuchMethodException {
      Method method =
          CallbackRegistry.class.getMethod(
              "registerAsyncCallback",
              String.class,
              CallbackRegistry.AsyncHostFunction.class,
              FunctionType.class);
      assertNotNull(method, "registerAsyncCallback method should exist");
      assertEquals(
          CallbackRegistry.AsyncCallbackHandle.class,
          method.getReturnType(),
          "Return type should be AsyncCallbackHandle");
    }

    @Test
    @DisplayName("should have unregisterCallback method")
    void shouldHaveUnregisterCallbackMethod() throws NoSuchMethodException {
      Method method =
          CallbackRegistry.class.getMethod(
              "unregisterCallback", CallbackRegistry.CallbackHandle.class);
      assertNotNull(method, "unregisterCallback method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
    }
  }

  // ========================================================================
  // Function Reference Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Function Reference Method Tests")
  class FunctionReferenceMethodTests {

    @Test
    @DisplayName("should have createFunctionReference method")
    void shouldHaveCreateFunctionReferenceMethod() throws NoSuchMethodException {
      Method method =
          CallbackRegistry.class.getMethod(
              "createFunctionReference", CallbackRegistry.CallbackHandle.class);
      assertNotNull(method, "createFunctionReference method should exist");
      assertEquals(
          FunctionReference.class,
          method.getReturnType(),
          "Return type should be FunctionReference");
    }
  }

  // ========================================================================
  // Invocation Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Invocation Method Tests")
  class InvocationMethodTests {

    @Test
    @DisplayName("should have invokeCallback method")
    void shouldHaveInvokeCallbackMethod() throws NoSuchMethodException {
      Method method =
          CallbackRegistry.class.getMethod(
              "invokeCallback", CallbackRegistry.CallbackHandle.class, WasmValue[].class);
      assertNotNull(method, "invokeCallback method should exist");
      assertEquals(WasmValue[].class, method.getReturnType(), "Return type should be WasmValue[]");
    }

    @Test
    @DisplayName("should have invokeAsyncCallback method")
    void shouldHaveInvokeAsyncCallbackMethod() throws NoSuchMethodException {
      Method method =
          CallbackRegistry.class.getMethod(
              "invokeAsyncCallback", CallbackRegistry.AsyncCallbackHandle.class, WasmValue[].class);
      assertNotNull(method, "invokeAsyncCallback method should exist");
      assertEquals(
          CompletableFuture.class,
          method.getReturnType(),
          "Return type should be CompletableFuture");
    }
  }

  // ========================================================================
  // Query Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Query Method Tests")
  class QueryMethodTests {

    @Test
    @DisplayName("should have getMetrics method")
    void shouldHaveGetMetricsMethod() throws NoSuchMethodException {
      Method method = CallbackRegistry.class.getMethod("getMetrics");
      assertNotNull(method, "getMetrics method should exist");
      assertEquals(
          CallbackRegistry.CallbackMetrics.class,
          method.getReturnType(),
          "Return type should be CallbackMetrics");
    }

    @Test
    @DisplayName("should have getCallbackCount method")
    void shouldHaveGetCallbackCountMethod() throws NoSuchMethodException {
      Method method = CallbackRegistry.class.getMethod("getCallbackCount");
      assertNotNull(method, "getCallbackCount method should exist");
      assertEquals(int.class, method.getReturnType(), "Return type should be int");
    }

    @Test
    @DisplayName("should have hasCallback method")
    void shouldHaveHasCallbackMethod() throws NoSuchMethodException {
      Method method = CallbackRegistry.class.getMethod("hasCallback", String.class);
      assertNotNull(method, "hasCallback method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Return type should be boolean");
    }
  }

  // ========================================================================
  // Lifecycle Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Lifecycle Method Tests")
  class LifecycleMethodTests {

    @Test
    @DisplayName("should have close method")
    void shouldHaveCloseMethod() throws NoSuchMethodException {
      Method method = CallbackRegistry.class.getMethod("close");
      assertNotNull(method, "close method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
    }

    @Test
    @DisplayName("close method should declare WasmException")
    void closeMethodShouldDeclareWasmException() throws NoSuchMethodException {
      Method method = CallbackRegistry.class.getMethod("close");
      Class<?>[] exceptions = method.getExceptionTypes();
      assertTrue(
          Arrays.asList(exceptions).contains(ai.tegmentum.wasmtime4j.exception.WasmException.class),
          "close should declare WasmException");
    }
  }

  // ========================================================================
  // CallbackHandle Interface Tests
  // ========================================================================

  @Nested
  @DisplayName("CallbackHandle Interface Tests")
  class CallbackHandleTests {

    @Test
    @DisplayName("should be a nested interface")
    void shouldBeNestedInterface() {
      assertTrue(
          CallbackRegistry.CallbackHandle.class.isInterface(),
          "CallbackHandle should be an interface");
      assertTrue(
          CallbackRegistry.CallbackHandle.class.isMemberClass(),
          "CallbackHandle should be a member class");
    }

    @Test
    @DisplayName("should have getId method")
    void shouldHaveGetIdMethod() throws NoSuchMethodException {
      Method method = CallbackRegistry.CallbackHandle.class.getMethod("getId");
      assertNotNull(method, "getId method should exist");
      assertEquals(long.class, method.getReturnType(), "Return type should be long");
    }

    @Test
    @DisplayName("should have getName method")
    void shouldHaveGetNameMethod() throws NoSuchMethodException {
      Method method = CallbackRegistry.CallbackHandle.class.getMethod("getName");
      assertNotNull(method, "getName method should exist");
      assertEquals(String.class, method.getReturnType(), "Return type should be String");
    }

    @Test
    @DisplayName("should have getFunctionType method")
    void shouldHaveGetFunctionTypeMethod() throws NoSuchMethodException {
      Method method = CallbackRegistry.CallbackHandle.class.getMethod("getFunctionType");
      assertNotNull(method, "getFunctionType method should exist");
      assertEquals(
          FunctionType.class, method.getReturnType(), "Return type should be FunctionType");
    }

    @Test
    @DisplayName("should have isValid method")
    void shouldHaveIsValidMethod() throws NoSuchMethodException {
      Method method = CallbackRegistry.CallbackHandle.class.getMethod("isValid");
      assertNotNull(method, "isValid method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Return type should be boolean");
    }
  }

  // ========================================================================
  // AsyncCallbackHandle Interface Tests
  // ========================================================================

  @Nested
  @DisplayName("AsyncCallbackHandle Interface Tests")
  class AsyncCallbackHandleTests {

    @Test
    @DisplayName("should be a nested interface")
    void shouldBeNestedInterface() {
      assertTrue(
          CallbackRegistry.AsyncCallbackHandle.class.isInterface(),
          "AsyncCallbackHandle should be an interface");
    }

    @Test
    @DisplayName("should extend CallbackHandle")
    void shouldExtendCallbackHandle() {
      assertTrue(
          CallbackRegistry.CallbackHandle.class.isAssignableFrom(
              CallbackRegistry.AsyncCallbackHandle.class),
          "AsyncCallbackHandle should extend CallbackHandle");
    }

    @Test
    @DisplayName("should have getTimeoutMillis method")
    void shouldHaveGetTimeoutMillisMethod() throws NoSuchMethodException {
      Method method = CallbackRegistry.AsyncCallbackHandle.class.getMethod("getTimeoutMillis");
      assertNotNull(method, "getTimeoutMillis method should exist");
      assertEquals(long.class, method.getReturnType(), "Return type should be long");
    }

    @Test
    @DisplayName("should have setTimeoutMillis method")
    void shouldHaveSetTimeoutMillisMethod() throws NoSuchMethodException {
      Method method =
          CallbackRegistry.AsyncCallbackHandle.class.getMethod("setTimeoutMillis", long.class);
      assertNotNull(method, "setTimeoutMillis method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
    }
  }

  // ========================================================================
  // AsyncHostFunction Interface Tests
  // ========================================================================

  @Nested
  @DisplayName("AsyncHostFunction Interface Tests")
  class AsyncHostFunctionTests {

    @Test
    @DisplayName("should be a nested interface")
    void shouldBeNestedInterface() {
      assertTrue(
          CallbackRegistry.AsyncHostFunction.class.isInterface(),
          "AsyncHostFunction should be an interface");
    }

    @Test
    @DisplayName("should be a functional interface")
    void shouldBeFunctionalInterface() {
      assertTrue(
          CallbackRegistry.AsyncHostFunction.class.isAnnotationPresent(FunctionalInterface.class),
          "AsyncHostFunction should be annotated with @FunctionalInterface");
    }

    @Test
    @DisplayName("should have executeAsync method")
    void shouldHaveExecuteAsyncMethod() throws NoSuchMethodException {
      Method method =
          CallbackRegistry.AsyncHostFunction.class.getMethod("executeAsync", WasmValue[].class);
      assertNotNull(method, "executeAsync method should exist");
      assertEquals(
          CompletableFuture.class,
          method.getReturnType(),
          "Return type should be CompletableFuture");
    }

    @Test
    @DisplayName("should have exactly one abstract method")
    void shouldHaveExactlyOneAbstractMethod() {
      long abstractMethodCount =
          Arrays.stream(CallbackRegistry.AsyncHostFunction.class.getDeclaredMethods())
              .filter(m -> Modifier.isAbstract(m.getModifiers()))
              .count();
      assertEquals(
          1, abstractMethodCount, "Functional interface should have exactly one abstract method");
    }
  }

  // ========================================================================
  // CallbackMetrics Interface Tests
  // ========================================================================

  @Nested
  @DisplayName("CallbackMetrics Interface Tests")
  class CallbackMetricsTests {

    @Test
    @DisplayName("should be a nested interface")
    void shouldBeNestedInterface() {
      assertTrue(
          CallbackRegistry.CallbackMetrics.class.isInterface(),
          "CallbackMetrics should be an interface");
    }

    @Test
    @DisplayName("should have getTotalInvocations method")
    void shouldHaveGetTotalInvocationsMethod() throws NoSuchMethodException {
      Method method = CallbackRegistry.CallbackMetrics.class.getMethod("getTotalInvocations");
      assertNotNull(method, "getTotalInvocations method should exist");
      assertEquals(long.class, method.getReturnType(), "Return type should be long");
    }

    @Test
    @DisplayName("should have getAverageExecutionTimeNanos method")
    void shouldHaveGetAverageExecutionTimeNanosMethod() throws NoSuchMethodException {
      Method method =
          CallbackRegistry.CallbackMetrics.class.getMethod("getAverageExecutionTimeNanos");
      assertNotNull(method, "getAverageExecutionTimeNanos method should exist");
      assertEquals(double.class, method.getReturnType(), "Return type should be double");
    }

    @Test
    @DisplayName("should have getTotalExecutionTimeNanos method")
    void shouldHaveGetTotalExecutionTimeNanosMethod() throws NoSuchMethodException {
      Method method =
          CallbackRegistry.CallbackMetrics.class.getMethod("getTotalExecutionTimeNanos");
      assertNotNull(method, "getTotalExecutionTimeNanos method should exist");
      assertEquals(long.class, method.getReturnType(), "Return type should be long");
    }

    @Test
    @DisplayName("should have getFailureCount method")
    void shouldHaveGetFailureCountMethod() throws NoSuchMethodException {
      Method method = CallbackRegistry.CallbackMetrics.class.getMethod("getFailureCount");
      assertNotNull(method, "getFailureCount method should exist");
      assertEquals(long.class, method.getReturnType(), "Return type should be long");
    }

    @Test
    @DisplayName("should have getTimeoutCount method")
    void shouldHaveGetTimeoutCountMethod() throws NoSuchMethodException {
      Method method = CallbackRegistry.CallbackMetrics.class.getMethod("getTimeoutCount");
      assertNotNull(method, "getTimeoutCount method should exist");
      assertEquals(long.class, method.getReturnType(), "Return type should be long");
    }
  }

  // ========================================================================
  // Method Completeness Tests
  // ========================================================================

  @Nested
  @DisplayName("Method Completeness Tests")
  class MethodCompletenessTests {

    @Test
    @DisplayName("CallbackRegistry should have all expected methods")
    void shouldHaveAllExpectedMethods() {
      Set<String> expectedMethods =
          Set.of(
              "registerCallback",
              "registerAsyncCallback",
              "createFunctionReference",
              "unregisterCallback",
              "invokeCallback",
              "invokeAsyncCallback",
              "getMetrics",
              "getCallbackCount",
              "hasCallback",
              "close");

      Set<String> actualMethods =
          Arrays.stream(CallbackRegistry.class.getDeclaredMethods())
              .map(Method::getName)
              .collect(Collectors.toSet());

      for (String expected : expectedMethods) {
        assertTrue(
            actualMethods.contains(expected), "CallbackRegistry should have method: " + expected);
      }
    }

    @Test
    @DisplayName("should have reasonable method count")
    void shouldHaveReasonableMethodCount() {
      int methodCount = CallbackRegistry.class.getDeclaredMethods().length;
      assertTrue(
          methodCount >= 10,
          "CallbackRegistry should have at least 10 methods, found: " + methodCount);
    }
  }

  // ========================================================================
  // Nested Type Completeness Tests
  // ========================================================================

  @Nested
  @DisplayName("Nested Type Completeness Tests")
  class NestedTypeCompletenessTests {

    @Test
    @DisplayName("should have CallbackHandle nested interface")
    void shouldHaveCallbackHandleNestedInterface() {
      Class<?>[] declaredClasses = CallbackRegistry.class.getDeclaredClasses();
      boolean hasCallbackHandle =
          Arrays.stream(declaredClasses).anyMatch(c -> c.getSimpleName().equals("CallbackHandle"));
      assertTrue(hasCallbackHandle, "CallbackRegistry should have CallbackHandle nested interface");
    }

    @Test
    @DisplayName("should have AsyncCallbackHandle nested interface")
    void shouldHaveAsyncCallbackHandleNestedInterface() {
      Class<?>[] declaredClasses = CallbackRegistry.class.getDeclaredClasses();
      boolean hasAsyncCallbackHandle =
          Arrays.stream(declaredClasses)
              .anyMatch(c -> c.getSimpleName().equals("AsyncCallbackHandle"));
      assertTrue(
          hasAsyncCallbackHandle,
          "CallbackRegistry should have AsyncCallbackHandle nested interface");
    }

    @Test
    @DisplayName("should have AsyncHostFunction nested interface")
    void shouldHaveAsyncHostFunctionNestedInterface() {
      Class<?>[] declaredClasses = CallbackRegistry.class.getDeclaredClasses();
      boolean hasAsyncHostFunction =
          Arrays.stream(declaredClasses)
              .anyMatch(c -> c.getSimpleName().equals("AsyncHostFunction"));
      assertTrue(
          hasAsyncHostFunction, "CallbackRegistry should have AsyncHostFunction nested interface");
    }

    @Test
    @DisplayName("should have CallbackMetrics nested interface")
    void shouldHaveCallbackMetricsNestedInterface() {
      Class<?>[] declaredClasses = CallbackRegistry.class.getDeclaredClasses();
      boolean hasCallbackMetrics =
          Arrays.stream(declaredClasses).anyMatch(c -> c.getSimpleName().equals("CallbackMetrics"));
      assertTrue(
          hasCallbackMetrics, "CallbackRegistry should have CallbackMetrics nested interface");
    }

    @Test
    @DisplayName("should have exactly 4 nested types")
    void shouldHaveCorrectNestedTypeCount() {
      Class<?>[] declaredClasses = CallbackRegistry.class.getDeclaredClasses();
      assertEquals(4, declaredClasses.length, "CallbackRegistry should have 4 nested types");
    }
  }
}
