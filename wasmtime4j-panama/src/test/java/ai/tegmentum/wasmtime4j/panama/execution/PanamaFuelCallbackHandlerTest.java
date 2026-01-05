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

package ai.tegmentum.wasmtime4j.panama.execution;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.execution.FuelCallbackHandler;
import ai.tegmentum.wasmtime4j.execution.FuelCallbackStats;
import ai.tegmentum.wasmtime4j.execution.FuelExhaustionContext;
import ai.tegmentum.wasmtime4j.execution.FuelExhaustionResult;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link PanamaFuelCallbackHandler} class.
 *
 * <p>PanamaFuelCallbackHandler provides Panama FFI implementation of fuel callback handling.
 */
@DisplayName("PanamaFuelCallbackHandler Tests")
class PanamaFuelCallbackHandlerTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be public final class")
    void shouldBePublicFinalClass() {
      assertTrue(
          Modifier.isPublic(PanamaFuelCallbackHandler.class.getModifiers()),
          "PanamaFuelCallbackHandler should be public");
      assertTrue(
          Modifier.isFinal(PanamaFuelCallbackHandler.class.getModifiers()),
          "PanamaFuelCallbackHandler should be final");
    }

    @Test
    @DisplayName("should implement FuelCallbackHandler interface")
    void shouldImplementFuelCallbackHandlerInterface() {
      assertTrue(
          FuelCallbackHandler.class.isAssignableFrom(PanamaFuelCallbackHandler.class),
          "PanamaFuelCallbackHandler should implement FuelCallbackHandler");
    }
  }

  @Nested
  @DisplayName("Static Factory Method Tests")
  class StaticFactoryMethodTests {

    @Test
    @DisplayName("should have createAutoRefill static method")
    void shouldHaveCreateAutoRefillMethod() throws NoSuchMethodException {
      final Method method =
          PanamaFuelCallbackHandler.class.getMethod(
              "createAutoRefill", long.class, long.class, int.class);
      assertNotNull(method, "createAutoRefill method should exist");
      assertEquals(
          PanamaFuelCallbackHandler.class,
          method.getReturnType(),
          "Should return PanamaFuelCallbackHandler");
      assertTrue(Modifier.isStatic(method.getModifiers()), "Should be static");
    }
  }

  @Nested
  @DisplayName("Id Method Tests")
  class IdMethodTests {

    @Test
    @DisplayName("should have getId method")
    void shouldHaveGetIdMethod() throws NoSuchMethodException {
      final Method method = PanamaFuelCallbackHandler.class.getMethod("getId");
      assertNotNull(method, "getId method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have getStoreId method")
    void shouldHaveGetStoreIdMethod() throws NoSuchMethodException {
      final Method method = PanamaFuelCallbackHandler.class.getMethod("getStoreId");
      assertNotNull(method, "getStoreId method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }
  }

  @Nested
  @DisplayName("Exhaustion Handler Method Tests")
  class ExhaustionHandlerMethodTests {

    @Test
    @DisplayName("should have handleExhaustion method")
    void shouldHaveHandleExhaustionMethod() throws NoSuchMethodException {
      final Method method =
          PanamaFuelCallbackHandler.class.getMethod(
              "handleExhaustion", FuelExhaustionContext.class);
      assertNotNull(method, "handleExhaustion method should exist");
      assertEquals(
          FuelExhaustionResult.class, method.getReturnType(), "Should return FuelExhaustionResult");
    }
  }

  @Nested
  @DisplayName("Stats Method Tests")
  class StatsMethodTests {

    @Test
    @DisplayName("should have getStats method")
    void shouldHaveGetStatsMethod() throws NoSuchMethodException {
      final Method method = PanamaFuelCallbackHandler.class.getMethod("getStats");
      assertNotNull(method, "getStats method should exist");
      assertEquals(
          FuelCallbackStats.class, method.getReturnType(), "Should return FuelCallbackStats");
    }

    @Test
    @DisplayName("should have resetStats method")
    void shouldHaveResetStatsMethod() throws NoSuchMethodException {
      final Method method = PanamaFuelCallbackHandler.class.getMethod("resetStats");
      assertNotNull(method, "resetStats method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }
  }

  @Nested
  @DisplayName("Lifecycle Method Tests")
  class LifecycleMethodTests {

    @Test
    @DisplayName("should have close method")
    void shouldHaveCloseMethod() throws NoSuchMethodException {
      final Method method = PanamaFuelCallbackHandler.class.getMethod("close");
      assertNotNull(method, "close method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }
  }
}
