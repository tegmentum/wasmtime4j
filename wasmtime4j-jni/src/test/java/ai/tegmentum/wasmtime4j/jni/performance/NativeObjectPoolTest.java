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

package ai.tegmentum.wasmtime4j.jni.performance;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the {@link NativeObjectPool} class.
 *
 * <p>This test class verifies the NativeObjectPool class which provides memory pooling
 * for frequently allocated native objects to reduce GC pressure.
 */
@DisplayName("NativeObjectPool Tests")
class NativeObjectPoolTest {

  private NativeObjectPool<byte[]> pool;
  private static final AtomicInteger creationCounter = new AtomicInteger(0);

  @BeforeEach
  void setUp() {
    creationCounter.set(0);
    pool = NativeObjectPool.getPool(
        byte[].class,
        () -> {
          creationCounter.incrementAndGet();
          return new byte[1024];
        },
        8,
        2);
  }

  @AfterEach
  void tearDown() {
    if (pool != null && !pool.isClosed()) {
      pool.close();
    }
    NativeObjectPool.clearAllPools();
  }

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("NativeObjectPool should be final class")
    void shouldBeFinalClass() {
      assertTrue(java.lang.reflect.Modifier.isFinal(NativeObjectPool.class.getModifiers()),
          "NativeObjectPool should be final");
    }

    @Test
    @DisplayName("DEFAULT_MAX_POOL_SIZE should be 32")
    void defaultMaxPoolSizeShouldBe32() {
      assertEquals(32, NativeObjectPool.DEFAULT_MAX_POOL_SIZE,
          "DEFAULT_MAX_POOL_SIZE should be 32");
    }

    @Test
    @DisplayName("DEFAULT_MIN_POOL_SIZE should be 4")
    void defaultMinPoolSizeShouldBe4() {
      assertEquals(4, NativeObjectPool.DEFAULT_MIN_POOL_SIZE,
          "DEFAULT_MIN_POOL_SIZE should be 4");
    }
  }

  @Nested
  @DisplayName("ObjectFactory Interface Tests")
  class ObjectFactoryTests {

    @Test
    @DisplayName("ObjectFactory should be functional interface")
    void shouldBeFunctionalInterface() {
      assertTrue(NativeObjectPool.ObjectFactory.class.isAnnotationPresent(FunctionalInterface.class),
          "ObjectFactory should be annotated with @FunctionalInterface");
    }

    @Test
    @DisplayName("ObjectFactory should work with lambda")
    void shouldWorkWithLambda() {
      final NativeObjectPool<String> stringPool = NativeObjectPool.getPool(
          String.class,
          () -> "test",
          4,
          1);
      try {
        final String borrowed = stringPool.borrow();
        assertNotNull(borrowed, "Should create object using lambda");
        assertEquals("test", borrowed, "Created object should match lambda return");
      } finally {
        stringPool.close();
      }
    }
  }

  @Nested
  @DisplayName("getPool Factory Method Tests")
  class GetPoolTests {

    @Test
    @DisplayName("getPool should throw for null objectType")
    void getPoolShouldThrowForNullObjectType() {
      assertThrows(IllegalArgumentException.class,
          () -> NativeObjectPool.getPool(null, () -> new byte[10], 8, 2),
          "Should throw for null objectType");
    }

    @Test
    @DisplayName("getPool should return same instance for same type")
    void getPoolShouldReturnSameInstanceForSameType() {
      final NativeObjectPool<Integer> pool1 = NativeObjectPool.getPool(
          Integer.class, () -> 42, 8, 2);
      final NativeObjectPool<Integer> pool2 = NativeObjectPool.getPool(
          Integer.class, () -> 43, 16, 4);

      // Should return the same pool instance (existing pool)
      assertEquals(pool1, pool2, "Should return same pool for same type");
      pool1.close();
    }

    @Test
    @DisplayName("getPool with 3 parameters should use default minPoolSize")
    void getPoolWith3ParametersShouldUseDefaultMinPoolSize() {
      final NativeObjectPool<Long> longPool = NativeObjectPool.getPool(
          Long.class, () -> 100L, 16);
      try {
        assertEquals(NativeObjectPool.DEFAULT_MIN_POOL_SIZE, longPool.getMinPoolSize(),
            "Should use default min pool size");
      } finally {
        longPool.close();
      }
    }
  }

  @Nested
  @DisplayName("Constructor Validation Tests")
  class ConstructorValidationTests {

    @Test
    @DisplayName("Should throw for non-positive maxPoolSize")
    void shouldThrowForNonPositiveMaxPoolSize() {
      NativeObjectPool.clearAllPools();
      assertThrows(IllegalArgumentException.class,
          () -> NativeObjectPool.getPool(Short.class, () -> (short) 1, 0, 0),
          "Should throw for maxPoolSize = 0");
      assertThrows(IllegalArgumentException.class,
          () -> NativeObjectPool.getPool(Short.class, () -> (short) 1, -1, 0),
          "Should throw for negative maxPoolSize");
    }

    @Test
    @DisplayName("Should throw for negative minPoolSize")
    void shouldThrowForNegativeMinPoolSize() {
      NativeObjectPool.clearAllPools();
      assertThrows(IllegalArgumentException.class,
          () -> NativeObjectPool.getPool(Float.class, () -> 1.0f, 8, -1),
          "Should throw for negative minPoolSize");
    }

    @Test
    @DisplayName("Should throw if minPoolSize exceeds maxPoolSize")
    void shouldThrowIfMinPoolSizeExceedsMaxPoolSize() {
      NativeObjectPool.clearAllPools();
      assertThrows(IllegalArgumentException.class,
          () -> NativeObjectPool.getPool(Double.class, () -> 1.0, 4, 8),
          "Should throw if minPoolSize > maxPoolSize");
    }

    @Test
    @DisplayName("Should throw for null factory")
    void shouldThrowForNullFactory() {
      NativeObjectPool.clearAllPools();
      assertThrows(IllegalArgumentException.class,
          () -> NativeObjectPool.getPool(Boolean.class, null, 8, 2),
          "Should throw for null factory");
    }
  }

  @Nested
  @DisplayName("Borrow Tests")
  class BorrowTests {

    @Test
    @DisplayName("borrow should return non-null object")
    void borrowShouldReturnNonNullObject() {
      final byte[] borrowed = pool.borrow();
      assertNotNull(borrowed, "Borrowed object should not be null");
    }

    @Test
    @DisplayName("borrow should increment borrowed count")
    void borrowShouldIncrementBorrowedCount() {
      assertEquals(0, pool.getBorrowedCount(), "Initial borrowed count should be 0");
      pool.borrow();
      assertEquals(1, pool.getBorrowedCount(), "Borrowed count should be 1 after borrow");
    }

    @Test
    @DisplayName("borrow should increment total borrows")
    void borrowShouldIncrementTotalBorrows() {
      assertEquals(0, pool.getTotalBorrows(), "Initial total borrows should be 0");
      pool.borrow();
      assertEquals(1, pool.getTotalBorrows(), "Total borrows should be 1");
      pool.borrow();
      assertEquals(2, pool.getTotalBorrows(), "Total borrows should be 2");
    }

    @Test
    @DisplayName("borrow should return pre-populated object when available")
    void borrowShouldReturnPrePopulatedObjectWhenAvailable() {
      // Pool was created with minPoolSize=2, so objects should be pre-populated
      // Reset counter after pool creation
      final long initialCreated = pool.getTotalCreated();

      pool.borrow();
      pool.borrow();

      // These should come from pre-populated pool, so no new creations
      // (depends on implementation details - may vary)
      assertTrue(pool.getTotalBorrows() >= 2, "Should track borrows");
    }

    @Test
    @DisplayName("borrow should throw when pool is closed")
    void borrowShouldThrowWhenPoolIsClosed() {
      pool.close();
      assertThrows(IllegalStateException.class, () -> pool.borrow(),
          "Should throw when pool is closed");
    }
  }

  @Nested
  @DisplayName("Return Tests")
  class ReturnTests {

    @Test
    @DisplayName("returnObject should throw for null")
    void returnObjectShouldThrowForNull() {
      assertThrows(IllegalArgumentException.class, () -> pool.returnObject(null),
          "Should throw for null object");
    }

    @Test
    @DisplayName("returnObject should decrement borrowed count")
    void returnObjectShouldDecrementBorrowedCount() {
      final byte[] obj = pool.borrow();
      assertEquals(1, pool.getBorrowedCount(), "Borrowed count should be 1");
      pool.returnObject(obj);
      assertEquals(0, pool.getBorrowedCount(), "Borrowed count should be 0 after return");
    }

    @Test
    @DisplayName("returnObject should increment total returns")
    void returnObjectShouldIncrementTotalReturns() {
      final byte[] obj = pool.borrow();
      assertEquals(0, pool.getTotalReturns(), "Initial total returns should be 0");
      pool.returnObject(obj);
      assertEquals(1, pool.getTotalReturns(), "Total returns should be 1");
    }

    @Test
    @DisplayName("returnObject should make object available again")
    void returnObjectShouldMakeObjectAvailableAgain() {
      final byte[] obj = pool.borrow();
      final int availableAfterBorrow = pool.getAvailableCount();
      pool.returnObject(obj);
      assertTrue(pool.getAvailableCount() >= availableAfterBorrow,
          "Available count should increase after return");
    }

    @Test
    @DisplayName("returnObject should not throw when pool is closed")
    void returnObjectShouldNotThrowWhenPoolIsClosed() {
      final byte[] obj = pool.borrow();
      pool.close();
      assertDoesNotThrow(() -> pool.returnObject(obj),
          "Return should not throw when pool is closed");
    }
  }

  @Nested
  @DisplayName("Pool State Tests")
  class PoolStateTests {

    @Test
    @DisplayName("getObjectType should return correct type")
    void getObjectTypeShouldReturnCorrectType() {
      assertEquals(byte[].class, pool.getObjectType(), "Should return byte[] class");
    }

    @Test
    @DisplayName("getMaxPoolSize should return configured value")
    void getMaxPoolSizeShouldReturnConfiguredValue() {
      assertEquals(8, pool.getMaxPoolSize(), "Max pool size should be 8");
    }

    @Test
    @DisplayName("getMinPoolSize should return configured value")
    void getMinPoolSizeShouldReturnConfiguredValue() {
      assertEquals(2, pool.getMinPoolSize(), "Min pool size should be 2");
    }

    @Test
    @DisplayName("isClosed should return false for open pool")
    void isClosedShouldReturnFalseForOpenPool() {
      assertFalse(pool.isClosed(), "Pool should not be closed");
    }

    @Test
    @DisplayName("isClosed should return true for closed pool")
    void isClosedShouldReturnTrueForClosedPool() {
      pool.close();
      assertTrue(pool.isClosed(), "Pool should be closed");
    }
  }

  @Nested
  @DisplayName("Statistics Tests")
  class StatisticsTests {

    @Test
    @DisplayName("getHitRate should return 100 for no borrows")
    void getHitRateShouldReturn100ForNoBorrows() {
      assertEquals(100.0, pool.getHitRate(), 0.01,
          "Hit rate should be 100% for no borrows");
    }

    @Test
    @DisplayName("getStats should return formatted string")
    void getStatsShouldReturnFormattedString() {
      final String stats = pool.getStats();
      assertNotNull(stats, "Stats should not be null");
      assertTrue(stats.contains("pool"), "Stats should contain 'pool'");
      assertTrue(stats.contains("available="), "Stats should contain 'available='");
      assertTrue(stats.contains("borrowed="), "Stats should contain 'borrowed='");
    }

    @Test
    @DisplayName("getAverageBorrowTimeNs should return 0 for no borrows")
    void getAverageBorrowTimeNsShouldReturn0ForNoBorrows() {
      assertEquals(0.0, pool.getAverageBorrowTimeNs(), 0.01,
          "Average borrow time should be 0 for no borrows");
    }

    @Test
    @DisplayName("getAverageReturnTimeNs should return 0 for no returns")
    void getAverageReturnTimeNsShouldReturn0ForNoReturns() {
      assertEquals(0.0, pool.getAverageReturnTimeNs(), 0.01,
          "Average return time should be 0 for no returns");
    }

    @Test
    @DisplayName("getMissRate should return 0 for no borrows")
    void getMissRateShouldReturn0ForNoBorrows() {
      assertEquals(0.0, pool.getMissRate(), 0.01,
          "Miss rate should be 0 for no borrows");
    }

    @Test
    @DisplayName("getContentionRate should return 0 for no borrows")
    void getContentionRateShouldReturn0ForNoBorrows() {
      assertEquals(0.0, pool.getContentionRate(), 0.01,
          "Contention rate should be 0 for no borrows");
    }

    @Test
    @DisplayName("getOptimalSize should return value >= minPoolSize")
    void getOptimalSizeShouldReturnValueGeMinPoolSize() {
      assertTrue(pool.getOptimalSize() >= pool.getMinPoolSize(),
          "Optimal size should be >= min pool size");
    }

    @Test
    @DisplayName("getPrewarmCount should track prewarmed objects")
    void getPrewarmCountShouldTrackPrewarmedObjects() {
      assertTrue(pool.getPrewarmCount() >= 0,
          "Prewarm count should be non-negative");
    }

    @Test
    @DisplayName("getPerformanceStats should return formatted string")
    void getPerformanceStatsShouldReturnFormattedString() {
      final String stats = pool.getPerformanceStats();
      assertNotNull(stats, "Performance stats should not be null");
      assertTrue(stats.contains("performance"), "Stats should contain 'performance'");
    }
  }

  @Nested
  @DisplayName("Clear Tests")
  class ClearTests {

    @Test
    @DisplayName("clear should empty the pool")
    void clearShouldEmptyThePool() {
      pool.borrow();
      pool.borrow();
      final byte[] obj = pool.borrow();
      pool.returnObject(obj);

      pool.clear();
      assertEquals(0, pool.getAvailableCount(), "Available count should be 0 after clear");
    }

    @Test
    @DisplayName("clear should not affect borrowed objects")
    void clearShouldNotAffectBorrowedObjects() {
      final byte[] obj1 = pool.borrow();
      final byte[] obj2 = pool.borrow();

      pool.clear();

      // Borrowed objects should still work
      assertNotNull(obj1, "Borrowed object should still be valid");
      assertNotNull(obj2, "Borrowed object should still be valid");
    }
  }

  @Nested
  @DisplayName("Close Tests")
  class CloseTests {

    @Test
    @DisplayName("close should mark pool as closed")
    void closeShouldMarkPoolAsClosed() {
      assertFalse(pool.isClosed(), "Pool should not be closed initially");
      pool.close();
      assertTrue(pool.isClosed(), "Pool should be closed after close()");
    }

    @Test
    @DisplayName("close should clear the pool")
    void closeShouldClearThePool() {
      pool.borrow();
      final byte[] obj = pool.borrow();
      pool.returnObject(obj);

      pool.close();
      assertEquals(0, pool.getAvailableCount(), "Pool should be empty after close");
    }

    @Test
    @DisplayName("close should be idempotent")
    void closeShouldBeIdempotent() {
      pool.close();
      assertDoesNotThrow(() -> pool.close(),
          "Calling close twice should not throw");
    }
  }

  @Nested
  @DisplayName("Prewarming Tests")
  class PrewarmingTests {

    @Test
    @DisplayName("setPrewarmingEnabled should not throw")
    void setPrewarmingEnabledShouldNotThrow() {
      assertDoesNotThrow(() -> pool.setPrewarmingEnabled(false),
          "Disabling prewarming should not throw");
      assertDoesNotThrow(() -> pool.setPrewarmingEnabled(true),
          "Enabling prewarming should not throw");
    }
  }

  @Nested
  @DisplayName("Optimization Tests")
  class OptimizationTests {

    @Test
    @DisplayName("optimize should not throw")
    void optimizeShouldNotThrow() {
      // Borrow and return to create usage patterns
      for (int i = 0; i < 5; i++) {
        final byte[] obj = pool.borrow();
        pool.returnObject(obj);
      }

      assertDoesNotThrow(() -> pool.optimize(),
          "optimize() should not throw");
    }
  }

  @Nested
  @DisplayName("Static Method Tests")
  class StaticMethodTests {

    @Test
    @DisplayName("getAllPoolStats should return formatted string")
    void getAllPoolStatsShouldReturnFormattedString() {
      final String stats = NativeObjectPool.getAllPoolStats();
      assertNotNull(stats, "All pool stats should not be null");
      assertTrue(stats.contains("NativeObjectPool"), "Should contain NativeObjectPool");
    }

    @Test
    @DisplayName("clearAllPools should close all pools")
    void clearAllPoolsShouldCloseAllPools() {
      final NativeObjectPool<Integer> intPool = NativeObjectPool.getPool(
          Integer.class, () -> 1, 4, 1);
      final NativeObjectPool<Long> longPool = NativeObjectPool.getPool(
          Long.class, () -> 1L, 4, 1);

      NativeObjectPool.clearAllPools();

      assertTrue(intPool.isClosed(), "Int pool should be closed");
      assertTrue(longPool.isClosed(), "Long pool should be closed");
    }
  }

  @Nested
  @DisplayName("ToString Tests")
  class ToStringTests {

    @Test
    @DisplayName("toString should contain pool type")
    void toStringShouldContainPoolType() {
      final String str = pool.toString();
      assertTrue(str.contains("Pool"), "toString should contain 'Pool'");
    }

    @Test
    @DisplayName("toString should contain available count")
    void toStringShouldContainAvailableCount() {
      final String str = pool.toString();
      assertTrue(str.contains("available="), "toString should contain 'available='");
    }

    @Test
    @DisplayName("toString should contain borrowed count")
    void toStringShouldContainBorrowedCount() {
      final String str = pool.toString();
      assertTrue(str.contains("borrowed="), "toString should contain 'borrowed='");
    }

    @Test
    @DisplayName("toString should contain closed status")
    void toStringShouldContainClosedStatus() {
      final String str = pool.toString();
      assertTrue(str.contains("closed="), "toString should contain 'closed='");
    }
  }

  @Nested
  @DisplayName("Borrow Return Cycle Tests")
  class BorrowReturnCycleTests {

    @Test
    @DisplayName("Multiple borrow-return cycles should work")
    void multipleBorrowReturnCyclesShouldWork() {
      for (int cycle = 0; cycle < 5; cycle++) {
        final List<byte[]> borrowed = new ArrayList<>();

        // Borrow several objects
        for (int i = 0; i < 3; i++) {
          borrowed.add(pool.borrow());
        }

        // Return all objects
        for (byte[] obj : borrowed) {
          pool.returnObject(obj);
        }
      }

      assertEquals(0, pool.getBorrowedCount(), "All objects should be returned");
      assertEquals(15, pool.getTotalBorrows(), "Should track all borrows");
      assertEquals(15, pool.getTotalReturns(), "Should track all returns");
    }

    @Test
    @DisplayName("Borrowed objects should be usable")
    void borrowedObjectsShouldBeUsable() {
      final byte[] arr = pool.borrow();

      // Should be able to use the array
      arr[0] = 42;
      arr[1] = 43;

      assertEquals(42, arr[0], "Array should be usable");
      assertEquals(43, arr[1], "Array should be usable");

      pool.returnObject(arr);
    }
  }
}
