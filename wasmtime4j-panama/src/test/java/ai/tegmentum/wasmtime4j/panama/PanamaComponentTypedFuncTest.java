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

package ai.tegmentum.wasmtime4j.panama;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.component.ComponentFunc;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive tests for {@link PanamaComponentTypedFunc}.
 *
 * <p>These tests verify constructor validation, instance behavior, and lifecycle management.
 */
@DisplayName("PanamaComponentTypedFunc Tests")
class PanamaComponentTypedFuncTest {

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("Constructor should throw on null function")
    void constructorShouldThrowOnNullFunction() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new PanamaComponentTypedFunc(null, "s32->s32"),
          "Should throw on null function");
    }

    @Test
    @DisplayName("Constructor should throw on null signature")
    void constructorShouldThrowOnNullSignature() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new PanamaComponentTypedFunc(new StubComponentFunc(), null),
          "Should throw on null signature");
    }

    @Test
    @DisplayName("Constructor should throw on empty signature")
    void constructorShouldThrowOnEmptySignature() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new PanamaComponentTypedFunc(new StubComponentFunc(), ""),
          "Should throw on empty signature");
    }
  }

  @Nested
  @DisplayName("Instance Behavior Tests")
  class InstanceBehaviorTests {

    @Test
    @DisplayName("getSignature should return signature provided in constructor")
    void getSignatureShouldReturnProvidedSignature() {
      StubComponentFunc stubFunc = new StubComponentFunc();
      String signature = "s32,s32->s32";
      PanamaComponentTypedFunc typedFunc = new PanamaComponentTypedFunc(stubFunc, signature);

      assertEquals(signature, typedFunc.getSignature(), "Should return provided signature");
    }

    @Test
    @DisplayName("getFunction should return function provided in constructor")
    void getFunctionShouldReturnProvidedFunction() {
      StubComponentFunc stubFunc = new StubComponentFunc();
      String signature = "s32->s32";
      PanamaComponentTypedFunc typedFunc = new PanamaComponentTypedFunc(stubFunc, signature);

      assertEquals(stubFunc, typedFunc.getFunction(), "Should return provided function");
    }

    @Test
    @DisplayName("close should not throw")
    void closeShouldNotThrow() {
      StubComponentFunc stubFunc = new StubComponentFunc();
      PanamaComponentTypedFunc typedFunc = new PanamaComponentTypedFunc(stubFunc, "s32->s32");

      typedFunc.close(); // Should not throw
    }

    @Test
    @DisplayName("close should be idempotent")
    void closeShouldBeIdempotent() {
      StubComponentFunc stubFunc = new StubComponentFunc();
      PanamaComponentTypedFunc typedFunc = new PanamaComponentTypedFunc(stubFunc, "s32->s32");

      typedFunc.close();
      typedFunc.close(); // Should not throw
    }

    @Test
    @DisplayName("toString should contain signature")
    void toStringShouldContainSignature() {
      StubComponentFunc stubFunc = new StubComponentFunc();
      String signature = "s32,s32->s32";
      PanamaComponentTypedFunc typedFunc = new PanamaComponentTypedFunc(stubFunc, signature);

      String result = typedFunc.toString();
      assertTrue(result.contains(signature), "toString should contain signature");
    }

    @Test
    @DisplayName("toString should contain class name")
    void toStringShouldContainClassName() {
      StubComponentFunc stubFunc = new StubComponentFunc();
      PanamaComponentTypedFunc typedFunc = new PanamaComponentTypedFunc(stubFunc, "s32->s32");

      String result = typedFunc.toString();
      assertTrue(result.contains("PanamaComponentTypedFunc"), "toString should contain class name");
    }

    @Test
    @DisplayName("closed state should be tracked")
    void closedStateShouldBeTracked() throws Exception {
      StubComponentFunc stubFunc = new StubComponentFunc();
      PanamaComponentTypedFunc typedFunc = new PanamaComponentTypedFunc(stubFunc, "s32->s32");

      Field resourceHandleField = PanamaComponentTypedFunc.class.getDeclaredField("resourceHandle");
      resourceHandleField.setAccessible(true);
      Object resourceHandle = resourceHandleField.get(typedFunc);
      Method isClosedMethod = resourceHandle.getClass().getMethod("isClosed");

      assertFalse(
          (boolean) isClosedMethod.invoke(resourceHandle), "Should not be closed initially");

      typedFunc.close();

      assertTrue((boolean) isClosedMethod.invoke(resourceHandle), "Should be closed after close()");
    }
  }

  /** Stub implementation of ComponentFunc for testing constructor behavior. */
  private static class StubComponentFunc implements ComponentFunc {

    @Override
    public String getName() {
      return "stub";
    }

    @Override
    public java.util.List<ai.tegmentum.wasmtime4j.component.ComponentTypeDescriptor>
        getParameterTypes() {
      return java.util.Collections.emptyList();
    }

    @Override
    public java.util.List<ai.tegmentum.wasmtime4j.component.ComponentTypeDescriptor>
        getResultTypes() {
      return java.util.Collections.emptyList();
    }

    @Override
    public java.util.List<ai.tegmentum.wasmtime4j.component.ComponentVal> call(
        final ai.tegmentum.wasmtime4j.component.ComponentVal... args) {
      return java.util.Collections.emptyList();
    }

    @Override
    public java.util.List<ai.tegmentum.wasmtime4j.component.ComponentVal> call(
        final java.util.List<ai.tegmentum.wasmtime4j.component.ComponentVal> args) {
      return java.util.Collections.emptyList();
    }

    @Override
    public boolean isValid() {
      return true;
    }
  }
}
