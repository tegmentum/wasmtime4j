/*
 * Copyright (c) 2024 Tegmentum AI. All rights reserved.
 */

package ai.tegmentum.wasmtime4j.panama.pool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.pool.PoolStatistics;
import ai.tegmentum.wasmtime4j.pool.PoolingAllocatorConfig;
import ai.tegmentum.wasmtime4j.pool.PoolingAllocatorConfigBuilder;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive test suite for the Panama pool package.
 *
 * <p>This test covers all classes in the ai.tegmentum.wasmtime4j.panama.pool package including
 * PanamaPoolStatistics, PanamaPoolingAllocator, PanamaPoolingAllocatorConfig, and
 * PanamaPoolingAllocatorConfigBuilder.
 */
@DisplayName("Panama Pool Package Tests")
class PanamaPoolPackageTest {

  // ========================================================================
  // PanamaPoolStatistics Tests
  // ========================================================================

  @Nested
  @DisplayName("PanamaPoolStatistics Tests")
  class PanamaPoolStatisticsTests {

    @Test
    @DisplayName("PanamaPoolStatistics should be a final class")
    void panamaPoolStatisticsShouldBeFinalClass() {
      assertTrue(
          Modifier.isFinal(PanamaPoolStatistics.class.getModifiers()),
          "PanamaPoolStatistics should be final");
    }

    @Test
    @DisplayName("PanamaPoolStatistics should implement PoolStatistics interface")
    void panamaPoolStatisticsShouldImplementPoolStatisticsInterface() {
      assertTrue(
          PoolStatistics.class.isAssignableFrom(PanamaPoolStatistics.class),
          "PanamaPoolStatistics should implement PoolStatistics");
    }

    @Test
    @DisplayName("PanamaPoolStatistics default constructor should create empty statistics")
    void panamaPoolStatisticsDefaultConstructorShouldCreateEmptyStats() {
      PanamaPoolStatistics stats = new PanamaPoolStatistics();

      assertNotNull(stats, "PanamaPoolStatistics should be created");
      assertEquals(0, stats.getInstancesAllocated(), "Instances allocated should be 0");
      assertEquals(0, stats.getInstancesReused(), "Instances reused should be 0");
      assertEquals(0, stats.getInstancesCreated(), "Instances created should be 0");
      assertEquals(0, stats.getMemoryPoolsAllocated(), "Memory pools allocated should be 0");
      assertEquals(0, stats.getMemoryPoolsReused(), "Memory pools reused should be 0");
      assertEquals(0, stats.getStackPoolsAllocated(), "Stack pools allocated should be 0");
      assertEquals(0, stats.getStackPoolsReused(), "Stack pools reused should be 0");
      assertEquals(0, stats.getTablePoolsAllocated(), "Table pools allocated should be 0");
      assertEquals(0, stats.getTablePoolsReused(), "Table pools reused should be 0");
      assertEquals(0, stats.getPeakMemoryUsage(), "Peak memory usage should be 0");
      assertEquals(0, stats.getCurrentMemoryUsage(), "Current memory usage should be 0");
      assertEquals(0, stats.getAllocationFailures(), "Allocation failures should be 0");
    }

    @Test
    @DisplayName("PanamaPoolStatistics full constructor should set all values")
    void panamaPoolStatisticsFullConstructorShouldSetAllValues() {
      PanamaPoolStatistics stats =
          new PanamaPoolStatistics(
              100, // instancesAllocated
              50, // instancesReused
              50, // instancesCreated
              200, // memoryPoolsAllocated
              100, // memoryPoolsReused
              300, // stackPoolsAllocated
              150, // stackPoolsReused
              400, // tablePoolsAllocated
              200, // tablePoolsReused
              1000000, // peakMemoryUsage
              500000, // currentMemoryUsage
              5, // allocationFailures
              1000000000, // poolWarmingTimeNanos (1 second)
              1000000 // averageAllocationTimeNanos (1 millisecond)
              );

      assertEquals(100, stats.getInstancesAllocated(), "Instances allocated should match");
      assertEquals(50, stats.getInstancesReused(), "Instances reused should match");
      assertEquals(50, stats.getInstancesCreated(), "Instances created should match");
      assertEquals(200, stats.getMemoryPoolsAllocated(), "Memory pools allocated should match");
      assertEquals(100, stats.getMemoryPoolsReused(), "Memory pools reused should match");
      assertEquals(300, stats.getStackPoolsAllocated(), "Stack pools allocated should match");
      assertEquals(150, stats.getStackPoolsReused(), "Stack pools reused should match");
      assertEquals(400, stats.getTablePoolsAllocated(), "Table pools allocated should match");
      assertEquals(200, stats.getTablePoolsReused(), "Table pools reused should match");
      assertEquals(1000000, stats.getPeakMemoryUsage(), "Peak memory usage should match");
      assertEquals(500000, stats.getCurrentMemoryUsage(), "Current memory usage should match");
      assertEquals(5, stats.getAllocationFailures(), "Allocation failures should match");
    }

    @Test
    @DisplayName("PanamaPoolStatistics getPoolWarmingTime should return Duration")
    void panamaPoolStatisticsGetPoolWarmingTimeShouldReturnDuration() {
      PanamaPoolStatistics stats =
          new PanamaPoolStatistics(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1000000000, 0);

      Duration warmingTime = stats.getPoolWarmingTime();
      assertNotNull(warmingTime, "Pool warming time should not be null");
      assertEquals(Duration.ofSeconds(1), warmingTime, "Pool warming time should be 1 second");
    }

    @Test
    @DisplayName("PanamaPoolStatistics getAverageAllocationTime should return Duration")
    void panamaPoolStatisticsGetAverageAllocationTimeShouldReturnDuration() {
      PanamaPoolStatistics stats =
          new PanamaPoolStatistics(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1000000);

      Duration avgTime = stats.getAverageAllocationTime();
      assertNotNull(avgTime, "Average allocation time should not be null");
      assertEquals(
          Duration.ofMillis(1), avgTime, "Average allocation time should be 1 millisecond");
    }

    @Test
    @DisplayName("PanamaPoolStatistics getReuseRatio should calculate correctly")
    void panamaPoolStatisticsGetReuseRatioShouldCalculateCorrectly() {
      // Formula: reused / (allocated + reused) = 100 / (100 + 100) = 0.5
      PanamaPoolStatistics stats =
          new PanamaPoolStatistics(100, 100, 50, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);

      double reuseRatio = stats.getReuseRatio();
      assertEquals(0.5, reuseRatio, 0.001, "Reuse ratio should be 0.5 (100/200)");
    }

    @Test
    @DisplayName("PanamaPoolStatistics getReuseRatio should handle zero allocations")
    void panamaPoolStatisticsGetReuseRatioShouldHandleZeroAllocations() {
      PanamaPoolStatistics stats = new PanamaPoolStatistics();

      double reuseRatio = stats.getReuseRatio();
      assertEquals(0.0, reuseRatio, "Reuse ratio should be 0 when no allocations");
    }

    @Test
    @DisplayName("PanamaPoolStatistics getMemoryUtilization should calculate correctly")
    void panamaPoolStatisticsGetMemoryUtilizationShouldCalculateCorrectly() {
      PanamaPoolStatistics stats =
          new PanamaPoolStatistics(0, 0, 0, 0, 0, 0, 0, 0, 0, 1000000, 500000, 0, 0, 0);

      double utilization = stats.getMemoryUtilization();
      assertEquals(0.5, utilization, 0.001, "Memory utilization should be 0.5 (500000/1000000)");
    }

    @Test
    @DisplayName("PanamaPoolStatistics getMemoryUtilization should handle zero peak")
    void panamaPoolStatisticsGetMemoryUtilizationShouldHandleZeroPeak() {
      PanamaPoolStatistics stats = new PanamaPoolStatistics();

      double utilization = stats.getMemoryUtilization();
      assertEquals(0.0, utilization, "Memory utilization should be 0 when no peak usage");
    }

    @Test
    @DisplayName("PanamaPoolStatistics toString should contain relevant info")
    void panamaPoolStatisticsToStringShouldContainRelevantInfo() {
      PanamaPoolStatistics stats =
          new PanamaPoolStatistics(
              100, 50, 50, 200, 100, 300, 150, 400, 200, 1000000, 500000, 5, 0, 0);

      String str = stats.toString();
      assertNotNull(str, "toString should not return null");
      assertTrue(str.contains("PanamaPoolStatistics"), "Should contain class name");
      assertTrue(str.contains("instancesAllocated"), "Should contain instancesAllocated");
      assertTrue(str.contains("instancesReused"), "Should contain instancesReused");
      assertTrue(str.contains("reuseRatio"), "Should contain reuseRatio");
      assertTrue(str.contains("memoryUtilization"), "Should contain memoryUtilization");
    }
  }

  // ========================================================================
  // PanamaPoolingAllocatorConfig Tests
  // ========================================================================

  @Nested
  @DisplayName("PanamaPoolingAllocatorConfig Tests")
  class PanamaPoolingAllocatorConfigTests {

    @Test
    @DisplayName("PanamaPoolingAllocatorConfig should be a final class")
    void panamaPoolingAllocatorConfigShouldBeFinalClass() {
      assertTrue(
          Modifier.isFinal(PanamaPoolingAllocatorConfig.class.getModifiers()),
          "PanamaPoolingAllocatorConfig should be final");
    }

    @Test
    @DisplayName(
        "PanamaPoolingAllocatorConfig should implement PoolingAllocatorConfig interface")
    void panamaPoolingAllocatorConfigShouldImplementPoolingAllocatorConfigInterface() {
      assertTrue(
          PoolingAllocatorConfig.class.isAssignableFrom(PanamaPoolingAllocatorConfig.class),
          "PanamaPoolingAllocatorConfig should implement PoolingAllocatorConfig");
    }

    @Test
    @DisplayName("PanamaPoolingAllocatorConfig should have getInstancePoolSize method")
    void panamaPoolingAllocatorConfigShouldHaveGetInstancePoolSizeMethod()
        throws NoSuchMethodException {
      Method method = PanamaPoolingAllocatorConfig.class.getMethod("getInstancePoolSize");
      assertNotNull(method, "getInstancePoolSize method should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }

    @Test
    @DisplayName("PanamaPoolingAllocatorConfig should have getMaxMemorySize method")
    void panamaPoolingAllocatorConfigShouldHaveGetMaxMemorySizeMethod()
        throws NoSuchMethodException {
      Method method = PanamaPoolingAllocatorConfig.class.getMethod("getMaxMemorySize");
      assertNotNull(method, "getMaxMemorySize method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("PanamaPoolingAllocatorConfig should have getMaxTablesPerInstance method")
    void panamaPoolingAllocatorConfigShouldHaveGetMaxTablesPerInstanceMethod()
        throws NoSuchMethodException {
      Method method = PanamaPoolingAllocatorConfig.class.getMethod("getMaxTablesPerInstance");
      assertNotNull(method, "getMaxTablesPerInstance method should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }

    @Test
    @DisplayName("PanamaPoolingAllocatorConfig should have getTableElements method")
    void panamaPoolingAllocatorConfigShouldHaveGetTableElementsMethod()
        throws NoSuchMethodException {
      Method method = PanamaPoolingAllocatorConfig.class.getMethod("getTableElements");
      assertNotNull(method, "getTableElements method should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }

    @Test
    @DisplayName("PanamaPoolingAllocatorConfig should be in correct package")
    void panamaPoolingAllocatorConfigShouldBeInCorrectPackage() {
      assertEquals(
          "ai.tegmentum.wasmtime4j.panama.pool",
          PanamaPoolingAllocatorConfig.class.getPackage().getName(),
          "PanamaPoolingAllocatorConfig should be in ai.tegmentum.wasmtime4j.panama.pool package");
    }
  }

  // ========================================================================
  // PanamaPoolingAllocatorConfigBuilder Tests
  // ========================================================================

  @Nested
  @DisplayName("PanamaPoolingAllocatorConfigBuilder Tests")
  class PanamaPoolingAllocatorConfigBuilderTests {

    @Test
    @DisplayName("PanamaPoolingAllocatorConfigBuilder should be a final class")
    void panamaPoolingAllocatorConfigBuilderShouldBeFinalClass() {
      assertTrue(
          Modifier.isFinal(PanamaPoolingAllocatorConfigBuilder.class.getModifiers()),
          "PanamaPoolingAllocatorConfigBuilder should be final");
    }

    @Test
    @DisplayName(
        "PanamaPoolingAllocatorConfigBuilder should implement PoolingAllocatorConfigBuilder")
    void panamaPoolingAllocatorConfigBuilderShouldImplementInterface() {
      assertTrue(
          PoolingAllocatorConfigBuilder.class.isAssignableFrom(
              PanamaPoolingAllocatorConfigBuilder.class),
          "PanamaPoolingAllocatorConfigBuilder should implement PoolingAllocatorConfigBuilder");
    }

    @Test
    @DisplayName("PanamaPoolingAllocatorConfigBuilder should have public constructor")
    void panamaPoolingAllocatorConfigBuilderShouldHavePublicConstructor() {
      Constructor<?>[] constructors = PanamaPoolingAllocatorConfigBuilder.class.getConstructors();
      assertTrue(
          constructors.length > 0,
          "PanamaPoolingAllocatorConfigBuilder should have public constructor");
    }

    @Test
    @DisplayName("PanamaPoolingAllocatorConfigBuilder should have instancePoolSize method")
    void panamaPoolingAllocatorConfigBuilderShouldHaveInstancePoolSizeMethod()
        throws NoSuchMethodException {
      Method method =
          PanamaPoolingAllocatorConfigBuilder.class.getMethod("instancePoolSize", int.class);
      assertNotNull(method, "instancePoolSize method should exist");
      assertEquals(
          PoolingAllocatorConfigBuilder.class,
          method.getReturnType(),
          "Should return builder interface for chaining");
    }

    @Test
    @DisplayName("PanamaPoolingAllocatorConfigBuilder should have maxMemorySize method")
    void panamaPoolingAllocatorConfigBuilderShouldHaveMaxMemorySizeMethod()
        throws NoSuchMethodException {
      Method method =
          PanamaPoolingAllocatorConfigBuilder.class.getMethod("maxMemorySize", long.class);
      assertNotNull(method, "maxMemorySize method should exist");
      assertEquals(
          PoolingAllocatorConfigBuilder.class,
          method.getReturnType(),
          "Should return builder interface for chaining");
    }

    @Test
    @DisplayName("PanamaPoolingAllocatorConfigBuilder should have build method")
    void panamaPoolingAllocatorConfigBuilderShouldHaveBuildMethod() throws NoSuchMethodException {
      Method method = PanamaPoolingAllocatorConfigBuilder.class.getMethod("build");
      assertNotNull(method, "build method should exist");
      assertEquals(
          PoolingAllocatorConfig.class,
          method.getReturnType(),
          "Should return PoolingAllocatorConfig");
    }

    @Test
    @DisplayName("PanamaPoolingAllocatorConfigBuilder build should create config")
    void panamaPoolingAllocatorConfigBuilderBuildShouldCreateConfig() {
      PanamaPoolingAllocatorConfigBuilder builder = new PanamaPoolingAllocatorConfigBuilder();
      PoolingAllocatorConfig config = builder.build();

      assertNotNull(config, "Build should create config");
      assertTrue(
          config instanceof PanamaPoolingAllocatorConfig,
          "Build should create PanamaPoolingAllocatorConfig");
    }

    @Test
    @DisplayName("PanamaPoolingAllocatorConfigBuilder should support method chaining")
    void panamaPoolingAllocatorConfigBuilderShouldSupportMethodChaining() {
      PanamaPoolingAllocatorConfigBuilder builder = new PanamaPoolingAllocatorConfigBuilder();

      PoolingAllocatorConfig config =
          builder.instancePoolSize(1000).maxMemorySize(1024L * 1024 * 100).build();

      assertNotNull(config, "Chained build should create config");
      assertEquals(1000, config.getInstancePoolSize(), "Instance pool size should be set");
      assertEquals(1024L * 1024 * 100, config.getMaxMemorySize(), "Max memory size should be set");
    }
  }

  // ========================================================================
  // PanamaPoolingAllocator Tests
  // ========================================================================

  @Nested
  @DisplayName("PanamaPoolingAllocator Tests")
  class PanamaPoolingAllocatorTests {

    @Test
    @DisplayName("PanamaPoolingAllocator should be a final class")
    void panamaPoolingAllocatorShouldBeFinalClass() {
      assertTrue(
          Modifier.isFinal(PanamaPoolingAllocator.class.getModifiers()),
          "PanamaPoolingAllocator should be final");
    }

    @Test
    @DisplayName("PanamaPoolingAllocator should have public constructor with PoolingAllocatorConfig")
    void panamaPoolingAllocatorShouldHavePublicConstructor() throws NoSuchMethodException {
      Constructor<?> constructor =
          PanamaPoolingAllocator.class.getConstructor(PoolingAllocatorConfig.class);
      assertNotNull(constructor, "Constructor should exist");
      assertTrue(Modifier.isPublic(constructor.getModifiers()), "Constructor should be public");
    }

    @Test
    @DisplayName("PanamaPoolingAllocator should have getStatistics method")
    void panamaPoolingAllocatorShouldHaveGetStatisticsMethod() throws NoSuchMethodException {
      Method method = PanamaPoolingAllocator.class.getMethod("getStatistics");
      assertNotNull(method, "getStatistics method should exist");
      assertTrue(
          PoolStatistics.class.isAssignableFrom(method.getReturnType()),
          "Should return PoolStatistics");
    }

    @Test
    @DisplayName("PanamaPoolingAllocator should have close method")
    void panamaPoolingAllocatorShouldHaveCloseMethod() throws NoSuchMethodException {
      Method method = PanamaPoolingAllocator.class.getMethod("close");
      assertNotNull(method, "close method should exist");
      assertEquals(void.class, method.getReturnType(), "close should return void");
    }

    @Test
    @DisplayName("PanamaPoolingAllocator should implement AutoCloseable")
    void panamaPoolingAllocatorShouldImplementAutoCloseable() {
      assertTrue(
          AutoCloseable.class.isAssignableFrom(PanamaPoolingAllocator.class),
          "PanamaPoolingAllocator should implement AutoCloseable");
    }
  }

  // ========================================================================
  // Package-Level Tests
  // ========================================================================

  @Nested
  @DisplayName("Package-Level Tests")
  class PackageLevelTests {

    @Test
    @DisplayName("All pool classes should be in correct package")
    void allPoolClassesShouldBeInCorrectPackage() {
      Class<?>[] poolClasses = {
        PanamaPoolStatistics.class,
        PanamaPoolingAllocator.class,
        PanamaPoolingAllocatorConfig.class,
        PanamaPoolingAllocatorConfigBuilder.class
      };

      String expectedPackage = "ai.tegmentum.wasmtime4j.panama.pool";
      for (Class<?> clazz : poolClasses) {
        assertEquals(
            expectedPackage,
            clazz.getPackage().getName(),
            clazz.getSimpleName() + " should be in " + expectedPackage);
      }
    }

    @Test
    @DisplayName("All pool implementation classes should be final")
    void allPoolImplementationClassesShouldBeFinal() {
      Class<?>[] poolClasses = {
        PanamaPoolStatistics.class,
        PanamaPoolingAllocator.class,
        PanamaPoolingAllocatorConfig.class,
        PanamaPoolingAllocatorConfigBuilder.class
      };

      for (Class<?> clazz : poolClasses) {
        assertTrue(
            Modifier.isFinal(clazz.getModifiers()), clazz.getSimpleName() + " should be final");
      }
    }

    @Test
    @DisplayName("All pool classes should not be interfaces")
    void allPoolClassesShouldNotBeInterfaces() {
      Class<?>[] poolClasses = {
        PanamaPoolStatistics.class,
        PanamaPoolingAllocator.class,
        PanamaPoolingAllocatorConfig.class,
        PanamaPoolingAllocatorConfigBuilder.class
      };

      for (Class<?> clazz : poolClasses) {
        assertFalse(clazz.isInterface(), clazz.getSimpleName() + " should not be an interface");
      }
    }

    @Test
    @DisplayName("Pool classes should have public constructors or factory methods")
    void poolClassesShouldHavePublicConstructorsOrFactoryMethods() {
      // PanamaPoolStatistics should have public constructor
      Constructor<?>[] statsConstructors = PanamaPoolStatistics.class.getConstructors();
      assertTrue(
          statsConstructors.length > 0, "PanamaPoolStatistics should have public constructor");

      // PanamaPoolingAllocatorConfigBuilder should have public constructor
      Constructor<?>[] builderConstructors =
          PanamaPoolingAllocatorConfigBuilder.class.getConstructors();
      assertTrue(
          builderConstructors.length > 0,
          "PanamaPoolingAllocatorConfigBuilder should have public constructor");
    }
  }
}
