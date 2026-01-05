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

package ai.tegmentum.wasmtime4j.panama.performance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.foreign.Arena;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link PanaNativeObjectPool} class.
 *
 * <p>PanaNativeObjectPool provides Panama-optimized memory pooling for native objects.
 */
@DisplayName("PanaNativeObjectPool Tests")
class PanaNativeObjectPoolTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be public final class")
    void shouldBePublicFinalClass() {
      assertTrue(
          Modifier.isPublic(PanaNativeObjectPool.class.getModifiers()),
          "PanaNativeObjectPool should be public");
      assertTrue(
          Modifier.isFinal(PanaNativeObjectPool.class.getModifiers()),
          "PanaNativeObjectPool should be final");
    }

    @Test
    @DisplayName("should have DEFAULT_MAX_POOL_SIZE constant")
    void shouldHaveDefaultMaxPoolSizeConstant() throws NoSuchFieldException {
      final Field field = PanaNativeObjectPool.class.getField("DEFAULT_MAX_POOL_SIZE");
      assertNotNull(field, "DEFAULT_MAX_POOL_SIZE should exist");
      assertTrue(Modifier.isStatic(field.getModifiers()), "Should be static");
      assertTrue(Modifier.isFinal(field.getModifiers()), "Should be final");
      assertEquals(int.class, field.getType(), "Should be int");
    }

    @Test
    @DisplayName("should have DEFAULT_MIN_POOL_SIZE constant")
    void shouldHaveDefaultMinPoolSizeConstant() throws NoSuchFieldException {
      final Field field = PanaNativeObjectPool.class.getField("DEFAULT_MIN_POOL_SIZE");
      assertNotNull(field, "DEFAULT_MIN_POOL_SIZE should exist");
      assertTrue(Modifier.isStatic(field.getModifiers()), "Should be static");
      assertTrue(Modifier.isFinal(field.getModifiers()), "Should be final");
      assertEquals(int.class, field.getType(), "Should be int");
    }

    @Test
    @DisplayName("should have ArenaObjectFactory interface")
    void shouldHaveArenaObjectFactoryInterface() {
      assertNotNull(
          PanaNativeObjectPool.ArenaObjectFactory.class,
          "ArenaObjectFactory interface should exist");
      assertTrue(
          PanaNativeObjectPool.ArenaObjectFactory.class.isInterface(),
          "ArenaObjectFactory should be an interface");
    }
  }

  @Nested
  @DisplayName("Static Factory Method Tests")
  class StaticFactoryMethodTests {

    @Test
    @DisplayName("should have getPool with 3 parameters")
    void shouldHaveGetPoolWith3Params() throws NoSuchMethodException {
      final Method method =
          PanaNativeObjectPool.class.getMethod(
              "getPool", Class.class, PanaNativeObjectPool.ArenaObjectFactory.class, int.class);
      assertNotNull(method, "getPool method should exist");
      assertEquals(
          PanaNativeObjectPool.class, method.getReturnType(), "Should return PanaNativeObjectPool");
      assertTrue(Modifier.isStatic(method.getModifiers()), "Should be static");
    }

    @Test
    @DisplayName("should have getPool with 4 parameters")
    void shouldHaveGetPoolWith4Params() throws NoSuchMethodException {
      final Method method =
          PanaNativeObjectPool.class.getMethod(
              "getPool",
              Class.class,
              PanaNativeObjectPool.ArenaObjectFactory.class,
              int.class,
              int.class);
      assertNotNull(method, "getPool method should exist");
      assertEquals(
          PanaNativeObjectPool.class, method.getReturnType(), "Should return PanaNativeObjectPool");
      assertTrue(Modifier.isStatic(method.getModifiers()), "Should be static");
    }
  }

  @Nested
  @DisplayName("Pool Operation Method Tests")
  class PoolOperationMethodTests {

    @Test
    @DisplayName("should have borrow method")
    void shouldHaveBorrowMethod() throws NoSuchMethodException {
      final Method method = PanaNativeObjectPool.class.getMethod("borrow", Arena.class);
      assertNotNull(method, "borrow method should exist");
      assertEquals(Object.class, method.getReturnType(), "Should return generic type");
    }

    @Test
    @DisplayName("should have returnObject method")
    void shouldHaveReturnObjectMethod() throws NoSuchMethodException {
      final Method method = PanaNativeObjectPool.class.getMethod("returnObject", Object.class);
      assertNotNull(method, "returnObject method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have clear method")
    void shouldHaveClearMethod() throws NoSuchMethodException {
      final Method method = PanaNativeObjectPool.class.getMethod("clear");
      assertNotNull(method, "clear method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have close method")
    void shouldHaveCloseMethod() throws NoSuchMethodException {
      final Method method = PanaNativeObjectPool.class.getMethod("close");
      assertNotNull(method, "close method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }
  }

  @Nested
  @DisplayName("Pool Status Method Tests")
  class PoolStatusMethodTests {

    @Test
    @DisplayName("should have getAvailableCount method")
    void shouldHaveGetAvailableCountMethod() throws NoSuchMethodException {
      final Method method = PanaNativeObjectPool.class.getMethod("getAvailableCount");
      assertNotNull(method, "getAvailableCount method should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }

    @Test
    @DisplayName("should have getBorrowedCount method")
    void shouldHaveGetBorrowedCountMethod() throws NoSuchMethodException {
      final Method method = PanaNativeObjectPool.class.getMethod("getBorrowedCount");
      assertNotNull(method, "getBorrowedCount method should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }

    @Test
    @DisplayName("should have getMaxPoolSize method")
    void shouldHaveGetMaxPoolSizeMethod() throws NoSuchMethodException {
      final Method method = PanaNativeObjectPool.class.getMethod("getMaxPoolSize");
      assertNotNull(method, "getMaxPoolSize method should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }

    @Test
    @DisplayName("should have isClosed method")
    void shouldHaveIsClosedMethod() throws NoSuchMethodException {
      final Method method = PanaNativeObjectPool.class.getMethod("isClosed");
      assertNotNull(method, "isClosed method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have getObjectType method")
    void shouldHaveGetObjectTypeMethod() throws NoSuchMethodException {
      final Method method = PanaNativeObjectPool.class.getMethod("getObjectType");
      assertNotNull(method, "getObjectType method should exist");
      assertEquals(Class.class, method.getReturnType(), "Should return Class");
    }
  }

  @Nested
  @DisplayName("Statistics Method Tests")
  class StatisticsMethodTests {

    @Test
    @DisplayName("should have getHitRate method")
    void shouldHaveGetHitRateMethod() throws NoSuchMethodException {
      final Method method = PanaNativeObjectPool.class.getMethod("getHitRate");
      assertNotNull(method, "getHitRate method should exist");
      assertEquals(double.class, method.getReturnType(), "Should return double");
    }

    @Test
    @DisplayName("should have getArenaAllocations method")
    void shouldHaveGetArenaAllocationsMethod() throws NoSuchMethodException {
      final Method method = PanaNativeObjectPool.class.getMethod("getArenaAllocations");
      assertNotNull(method, "getArenaAllocations method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have getZeroCopyOperations method")
    void shouldHaveGetZeroCopyOperationsMethod() throws NoSuchMethodException {
      final Method method = PanaNativeObjectPool.class.getMethod("getZeroCopyOperations");
      assertNotNull(method, "getZeroCopyOperations method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have getMemorySegmentOperations method")
    void shouldHaveGetMemorySegmentOperationsMethod() throws NoSuchMethodException {
      final Method method = PanaNativeObjectPool.class.getMethod("getMemorySegmentOperations");
      assertNotNull(method, "getMemorySegmentOperations method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have getStats method")
    void shouldHaveGetStatsMethod() throws NoSuchMethodException {
      final Method method = PanaNativeObjectPool.class.getMethod("getStats");
      assertNotNull(method, "getStats method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }

    @Test
    @DisplayName("should have getPanamaPerformanceStats method")
    void shouldHaveGetPanamaPerformanceStatsMethod() throws NoSuchMethodException {
      final Method method = PanaNativeObjectPool.class.getMethod("getPanamaPerformanceStats");
      assertNotNull(method, "getPanamaPerformanceStats method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }
  }

  @Nested
  @DisplayName("Static Utility Method Tests")
  class StaticUtilityMethodTests {

    @Test
    @DisplayName("should have getAllPoolStats static method")
    void shouldHaveGetAllPoolStatsMethod() throws NoSuchMethodException {
      final Method method = PanaNativeObjectPool.class.getMethod("getAllPoolStats");
      assertNotNull(method, "getAllPoolStats method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
      assertTrue(Modifier.isStatic(method.getModifiers()), "Should be static");
    }

    @Test
    @DisplayName("should have clearAllPools static method")
    void shouldHaveClearAllPoolsMethod() throws NoSuchMethodException {
      final Method method = PanaNativeObjectPool.class.getMethod("clearAllPools");
      assertNotNull(method, "clearAllPools method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
      assertTrue(Modifier.isStatic(method.getModifiers()), "Should be static");
    }
  }
}
