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

package ai.tegmentum.wasmtime4j.async;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.WasmValue;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.time.Duration;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link AsyncFunctionCall} class.
 *
 * <p>AsyncFunctionCall encapsulates all information needed to execute a WebAssembly function
 * asynchronously.
 */
@DisplayName("AsyncFunctionCall Tests")
class AsyncFunctionCallTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be public final class")
    void shouldBePublicFinalClass() {
      assertTrue(
          Modifier.isPublic(AsyncFunctionCall.class.getModifiers()),
          "AsyncFunctionCall should be public");
      assertTrue(
          Modifier.isFinal(AsyncFunctionCall.class.getModifiers()),
          "AsyncFunctionCall should be final");
      assertFalse(
          AsyncFunctionCall.class.isInterface(), "AsyncFunctionCall should not be an interface");
    }

    @Test
    @DisplayName("should have nested Builder class")
    void shouldHaveNestedBuilderClass() {
      final var nestedClasses = AsyncFunctionCall.class.getDeclaredClasses();
      boolean found = false;
      for (var nestedClass : nestedClasses) {
        if (nestedClass.getSimpleName().equals("Builder")) {
          found = true;
          assertTrue(Modifier.isFinal(nestedClass.getModifiers()), "Builder should be final");
          assertTrue(Modifier.isPublic(nestedClass.getModifiers()), "Builder should be public");
          assertTrue(Modifier.isStatic(nestedClass.getModifiers()), "Builder should be static");
          break;
        }
      }
      assertTrue(found, "Should have Builder nested class");
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("constructor should set all fields")
    void constructorShouldSetAllFields() {
      final WasmValue[] args = new WasmValue[] {WasmValue.i32(42)};
      final Duration timeout = Duration.ofSeconds(10);
      final Executor executor = Executors.newSingleThreadExecutor();

      final AsyncFunctionCall call =
          new AsyncFunctionCall("test-function", args, timeout, executor, true, "custom-id", 5);

      assertEquals("test-function", call.getFunctionName(), "Function name should match");
      assertEquals(1, call.getArgs().length, "Should have 1 argument");
      assertEquals(timeout, call.getTimeout(), "Timeout should match");
      assertEquals(executor, call.getExecutor(), "Executor should match");
      assertTrue(call.isCachingEnabled(), "Caching should be enabled");
      assertEquals("custom-id", call.getCallId(), "Call ID should match");
      assertEquals(5, call.getPriority(), "Priority should match");
    }

    @Test
    @DisplayName("constructor should generate call ID when null")
    void constructorShouldGenerateCallIdWhenNull() {
      final AsyncFunctionCall call =
          new AsyncFunctionCall("func", null, null, null, false, null, 0);

      assertNotNull(call.getCallId(), "Call ID should be generated");
      assertTrue(call.getCallId().startsWith("call-"), "Call ID should start with 'call-'");
    }

    @Test
    @DisplayName("constructor should create empty args array when null")
    void constructorShouldCreateEmptyArgsArrayWhenNull() {
      final AsyncFunctionCall call =
          new AsyncFunctionCall("func", null, null, null, false, null, 0);

      assertNotNull(call.getArgs(), "Args should not be null");
      assertEquals(0, call.getArgs().length, "Args should be empty");
    }

    @Test
    @DisplayName("constructor should clone args array for defensive copy")
    void constructorShouldCloneArgsArrayForDefensiveCopy() {
      final WasmValue[] args = new WasmValue[] {WasmValue.i32(1)};
      final AsyncFunctionCall call =
          new AsyncFunctionCall("func", args, null, null, false, null, 0);

      // Modify original
      args[0] = WasmValue.i32(999);

      // Verify call was not affected
      final WasmValue[] callArgs = call.getArgs();
      assertNotSame(args, callArgs, "Should be different array instance");
    }
  }

  @Nested
  @DisplayName("Static Factory Method Tests")
  class StaticFactoryMethodTests {

    @Test
    @DisplayName("of should create simple async call")
    void ofShouldCreateSimpleAsyncCall() {
      final AsyncFunctionCall call = AsyncFunctionCall.of("myFunc", WasmValue.i32(10));

      assertEquals("myFunc", call.getFunctionName(), "Function name should match");
      assertEquals(1, call.getArgs().length, "Should have 1 arg");
      assertNull(call.getTimeout(), "Timeout should be null");
      assertNull(call.getExecutor(), "Executor should be null");
      assertFalse(call.isCachingEnabled(), "Caching should be disabled");
      assertEquals(0, call.getPriority(), "Priority should be 0");
    }

    @Test
    @DisplayName("withTimeout should create call with timeout")
    void withTimeoutShouldCreateCallWithTimeout() {
      final Duration timeout = Duration.ofSeconds(30);
      final AsyncFunctionCall call =
          AsyncFunctionCall.withTimeout("func", timeout, WasmValue.i64(100L));

      assertEquals("func", call.getFunctionName(), "Function name should match");
      assertEquals(timeout, call.getTimeout(), "Timeout should match");
      assertTrue(call.hasTimeout(), "Should have timeout");
    }

    @Test
    @DisplayName("withExecutor should create call with custom executor")
    void withExecutorShouldCreateCallWithCustomExecutor() {
      final Executor executor = Executors.newCachedThreadPool();
      final AsyncFunctionCall call = AsyncFunctionCall.withExecutor("func", executor);

      assertEquals(executor, call.getExecutor(), "Executor should match");
      assertTrue(call.hasCustomExecutor(), "Should have custom executor");
    }

    @Test
    @DisplayName("withCaching should create call with caching enabled")
    void withCachingShouldCreateCallWithCachingEnabled() {
      final AsyncFunctionCall call = AsyncFunctionCall.withCaching("func");

      assertTrue(call.isCachingEnabled(), "Caching should be enabled");
    }

    @Test
    @DisplayName("withPriority should create call with specified priority")
    void withPriorityShouldCreateCallWithSpecifiedPriority() {
      final AsyncFunctionCall call =
          AsyncFunctionCall.withPriority("func", 10, WasmValue.f32(3.14f));

      assertEquals(10, call.getPriority(), "Priority should match");
    }
  }

  @Nested
  @DisplayName("Getter Method Tests")
  class GetterMethodTests {

    @Test
    @DisplayName("should have getFunctionName method")
    void shouldHaveGetFunctionNameMethod() throws NoSuchMethodException {
      final Method method = AsyncFunctionCall.class.getMethod("getFunctionName");
      assertNotNull(method, "getFunctionName method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }

    @Test
    @DisplayName("should have getArgs method")
    void shouldHaveGetArgsMethod() throws NoSuchMethodException {
      final Method method = AsyncFunctionCall.class.getMethod("getArgs");
      assertNotNull(method, "getArgs method should exist");
      assertEquals(WasmValue[].class, method.getReturnType(), "Should return WasmValue[]");
    }

    @Test
    @DisplayName("getArgs should return defensive copy")
    void getArgsShouldReturnDefensiveCopy() {
      final AsyncFunctionCall call =
          AsyncFunctionCall.of("func", WasmValue.i32(1), WasmValue.i32(2));

      final WasmValue[] args1 = call.getArgs();
      final WasmValue[] args2 = call.getArgs();

      assertNotSame(args1, args2, "Each call should return a new array");
      assertArrayEquals(args1, args2, "Contents should be the same");
    }

    @Test
    @DisplayName("should have getTimeout method")
    void shouldHaveGetTimeoutMethod() throws NoSuchMethodException {
      final Method method = AsyncFunctionCall.class.getMethod("getTimeout");
      assertNotNull(method, "getTimeout method should exist");
      assertEquals(Duration.class, method.getReturnType(), "Should return Duration");
    }

    @Test
    @DisplayName("should have getExecutor method")
    void shouldHaveGetExecutorMethod() throws NoSuchMethodException {
      final Method method = AsyncFunctionCall.class.getMethod("getExecutor");
      assertNotNull(method, "getExecutor method should exist");
      assertEquals(Executor.class, method.getReturnType(), "Should return Executor");
    }

    @Test
    @DisplayName("should have isCachingEnabled method")
    void shouldHaveIsCachingEnabledMethod() throws NoSuchMethodException {
      final Method method = AsyncFunctionCall.class.getMethod("isCachingEnabled");
      assertNotNull(method, "isCachingEnabled method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have getCallId method")
    void shouldHaveGetCallIdMethod() throws NoSuchMethodException {
      final Method method = AsyncFunctionCall.class.getMethod("getCallId");
      assertNotNull(method, "getCallId method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }

    @Test
    @DisplayName("should have getPriority method")
    void shouldHaveGetPriorityMethod() throws NoSuchMethodException {
      final Method method = AsyncFunctionCall.class.getMethod("getPriority");
      assertNotNull(method, "getPriority method should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }

    @Test
    @DisplayName("should have hasTimeout method")
    void shouldHaveHasTimeoutMethod() throws NoSuchMethodException {
      final Method method = AsyncFunctionCall.class.getMethod("hasTimeout");
      assertNotNull(method, "hasTimeout method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have hasCustomExecutor method")
    void shouldHaveHasCustomExecutorMethod() throws NoSuchMethodException {
      final Method method = AsyncFunctionCall.class.getMethod("hasCustomExecutor");
      assertNotNull(method, "hasCustomExecutor method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }
  }

  @Nested
  @DisplayName("Builder Tests")
  class BuilderTests {

    @Test
    @DisplayName("toBuilder should create builder with all values")
    void toBuilderShouldCreateBuilderWithAllValues() {
      final Duration timeout = Duration.ofSeconds(20);
      final Executor executor = Executors.newFixedThreadPool(2);

      final AsyncFunctionCall original =
          new AsyncFunctionCall(
              "original-func",
              new WasmValue[] {WasmValue.i32(5)},
              timeout,
              executor,
              true,
              "original-id",
              7);

      final AsyncFunctionCall copy = original.toBuilder().build();

      assertEquals(
          original.getFunctionName(), copy.getFunctionName(), "Function name should match");
      assertEquals(original.getTimeout(), copy.getTimeout(), "Timeout should match");
      assertEquals(original.getExecutor(), copy.getExecutor(), "Executor should match");
      assertEquals(original.isCachingEnabled(), copy.isCachingEnabled(), "Caching should match");
      assertEquals(original.getCallId(), copy.getCallId(), "Call ID should match");
      assertEquals(original.getPriority(), copy.getPriority(), "Priority should match");
    }

    @Test
    @DisplayName("builder should allow modification of all fields")
    void builderShouldAllowModificationOfAllFields() {
      final Duration newTimeout = Duration.ofMinutes(1);
      final Executor newExecutor = Executors.newWorkStealingPool();

      final AsyncFunctionCall call =
          new AsyncFunctionCall.Builder("builder-func")
              .args(WasmValue.i32(1), WasmValue.i32(2))
              .timeout(newTimeout)
              .executor(newExecutor)
              .caching(true)
              .callId("new-id")
              .priority(100)
              .build();

      assertEquals("builder-func", call.getFunctionName(), "Function name should match");
      assertEquals(2, call.getArgs().length, "Should have 2 args");
      assertEquals(newTimeout, call.getTimeout(), "Timeout should match");
      assertEquals(newExecutor, call.getExecutor(), "Executor should match");
      assertTrue(call.isCachingEnabled(), "Caching should be enabled");
      assertEquals("new-id", call.getCallId(), "Call ID should match");
      assertEquals(100, call.getPriority(), "Priority should match");
    }

    @Test
    @DisplayName("builder args should handle null")
    void builderArgsShouldHandleNull() {
      final AsyncFunctionCall call =
          new AsyncFunctionCall.Builder("func").args((WasmValue[]) null).build();

      assertNotNull(call.getArgs(), "Args should not be null");
      assertEquals(0, call.getArgs().length, "Args should be empty");
    }
  }

  @Nested
  @DisplayName("toString Tests")
  class ToStringTests {

    @Test
    @DisplayName("toString should include key fields")
    void toStringShouldIncludeKeyFields() {
      final AsyncFunctionCall call =
          new AsyncFunctionCall(
              "my-function",
              new WasmValue[] {WasmValue.i32(1), WasmValue.i32(2)},
              Duration.ofSeconds(5),
              null,
              true,
              "test-id",
              3);

      final String str = call.toString();

      assertTrue(str.contains("my-function"), "Should contain function name");
      assertTrue(str.contains("2"), "Should contain args count");
      assertTrue(str.contains("3"), "Should contain priority");
      assertTrue(str.contains("true"), "Should contain caching status");
    }
  }
}
