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

import ai.tegmentum.wasmtime4j.execution.ResourceLimiter;
import ai.tegmentum.wasmtime4j.execution.ResourceLimiterConfig;
import ai.tegmentum.wasmtime4j.execution.ResourceLimiterStats;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link PanamaResourceLimiter} class.
 *
 * <p>PanamaResourceLimiter provides Panama FFI implementation of resource limiting.
 */
@DisplayName("PanamaResourceLimiter Tests")
class PanamaResourceLimiterTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be public final class")
    void shouldBePublicFinalClass() {
      assertTrue(
          Modifier.isPublic(PanamaResourceLimiter.class.getModifiers()),
          "PanamaResourceLimiter should be public");
      assertTrue(
          Modifier.isFinal(PanamaResourceLimiter.class.getModifiers()),
          "PanamaResourceLimiter should be final");
    }

    @Test
    @DisplayName("should implement ResourceLimiter interface")
    void shouldImplementResourceLimiterInterface() {
      assertTrue(
          ResourceLimiter.class.isAssignableFrom(PanamaResourceLimiter.class),
          "PanamaResourceLimiter should implement ResourceLimiter");
    }
  }

  @Nested
  @DisplayName("Static Factory Method Tests")
  class StaticFactoryMethodTests {

    @Test
    @DisplayName("should have create static method")
    void shouldHaveCreateMethod() throws NoSuchMethodException {
      final Method method =
          PanamaResourceLimiter.class.getMethod("create", ResourceLimiterConfig.class);
      assertNotNull(method, "create method should exist");
      assertEquals(
          PanamaResourceLimiter.class,
          method.getReturnType(),
          "Should return PanamaResourceLimiter");
      assertTrue(Modifier.isStatic(method.getModifiers()), "Should be static");
    }

    @Test
    @DisplayName("should have createDefault static method")
    void shouldHaveCreateDefaultMethod() throws NoSuchMethodException {
      final Method method = PanamaResourceLimiter.class.getMethod("createDefault");
      assertNotNull(method, "createDefault method should exist");
      assertEquals(
          PanamaResourceLimiter.class,
          method.getReturnType(),
          "Should return PanamaResourceLimiter");
      assertTrue(Modifier.isStatic(method.getModifiers()), "Should be static");
    }

    @Test
    @DisplayName("should have getLimiterCount static method")
    void shouldHaveGetLimiterCountMethod() throws NoSuchMethodException {
      final Method method = PanamaResourceLimiter.class.getMethod("getLimiterCount");
      assertNotNull(method, "getLimiterCount method should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
      assertTrue(Modifier.isStatic(method.getModifiers()), "Should be static");
    }
  }

  @Nested
  @DisplayName("Id Method Tests")
  class IdMethodTests {

    @Test
    @DisplayName("should have getId method")
    void shouldHaveGetIdMethod() throws NoSuchMethodException {
      final Method method = PanamaResourceLimiter.class.getMethod("getId");
      assertNotNull(method, "getId method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }
  }

  @Nested
  @DisplayName("Config Method Tests")
  class ConfigMethodTests {

    @Test
    @DisplayName("should have getConfig method")
    void shouldHaveGetConfigMethod() throws NoSuchMethodException {
      final Method method = PanamaResourceLimiter.class.getMethod("getConfig");
      assertNotNull(method, "getConfig method should exist");
      assertEquals(
          ResourceLimiterConfig.class,
          method.getReturnType(),
          "Should return ResourceLimiterConfig");
    }
  }

  @Nested
  @DisplayName("Memory Grow Method Tests")
  class MemoryGrowMethodTests {

    @Test
    @DisplayName("should have allowMemoryGrow method")
    void shouldHaveAllowMemoryGrowMethod() throws NoSuchMethodException {
      final Method method =
          PanamaResourceLimiter.class.getMethod("allowMemoryGrow", long.class, long.class);
      assertNotNull(method, "allowMemoryGrow method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }
  }

  @Nested
  @DisplayName("Table Grow Method Tests")
  class TableGrowMethodTests {

    @Test
    @DisplayName("should have allowTableGrow method")
    void shouldHaveAllowTableGrowMethod() throws NoSuchMethodException {
      final Method method =
          PanamaResourceLimiter.class.getMethod("allowTableGrow", long.class, long.class);
      assertNotNull(method, "allowTableGrow method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }
  }

  @Nested
  @DisplayName("Stats Method Tests")
  class StatsMethodTests {

    @Test
    @DisplayName("should have getStats method")
    void shouldHaveGetStatsMethod() throws NoSuchMethodException {
      final Method method = PanamaResourceLimiter.class.getMethod("getStats");
      assertNotNull(method, "getStats method should exist");
      assertEquals(
          ResourceLimiterStats.class, method.getReturnType(), "Should return ResourceLimiterStats");
    }

    @Test
    @DisplayName("should have resetStats method")
    void shouldHaveResetStatsMethod() throws NoSuchMethodException {
      final Method method = PanamaResourceLimiter.class.getMethod("resetStats");
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
      final Method method = PanamaResourceLimiter.class.getMethod("close");
      assertNotNull(method, "close method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }
  }
}
