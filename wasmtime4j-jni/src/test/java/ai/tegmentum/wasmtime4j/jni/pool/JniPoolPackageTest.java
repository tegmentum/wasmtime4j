/*
 * Copyright (c) 2024 Tegmentum AI. All rights reserved.
 */

package ai.tegmentum.wasmtime4j.jni.pool;

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
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive test suite for the JNI pool package.
 *
 * <p>This test covers all classes in the ai.tegmentum.wasmtime4j.jni.pool package including
 * JniPoolStatistics, JniPoolingAllocator, JniPoolingAllocatorConfig, and
 * JniPoolingAllocatorConfigBuilder.
 */
@DisplayName("JNI Pool Package Tests")
class JniPoolPackageTest {

  // ========================================================================
  // JniPoolStatistics Tests
  // ========================================================================

  @Nested
  @DisplayName("JniPoolStatistics Tests")
  class JniPoolStatisticsTests {

    @Test
    @DisplayName("JniPoolStatistics should be a final class")
    void jniPoolStatisticsShouldBeFinalClass() {
      assertTrue(
          Modifier.isFinal(JniPoolStatistics.class.getModifiers()),
          "JniPoolStatistics should be final");
    }

    @Test
    @DisplayName("JniPoolStatistics should implement PoolStatistics interface")
    void jniPoolStatisticsShouldImplementPoolStatisticsInterface() {
      assertTrue(
          PoolStatistics.class.isAssignableFrom(JniPoolStatistics.class),
          "JniPoolStatistics should implement PoolStatistics");
    }

    @Test
    @DisplayName("JniPoolStatistics default constructor should create empty statistics")
    void jniPoolStatisticsDefaultConstructorShouldCreateEmptyStats() {
      JniPoolStatistics stats = new JniPoolStatistics();

      assertNotNull(stats, "JniPoolStatistics should be created");
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
    @DisplayName("JniPoolStatistics full constructor should set all values")
    void jniPoolStatisticsFullConstructorShouldSetAllValues() {
      JniPoolStatistics stats =
          new JniPoolStatistics(
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
    @DisplayName("JniPoolStatistics getPoolWarmingTime should return Duration")
    void jniPoolStatisticsGetPoolWarmingTimeShouldReturnDuration() {
      JniPoolStatistics stats =
          new JniPoolStatistics(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1000000000, 0);

      Duration warmingTime = stats.getPoolWarmingTime();
      assertNotNull(warmingTime, "Pool warming time should not be null");
      assertEquals(Duration.ofSeconds(1), warmingTime, "Pool warming time should be 1 second");
    }

    @Test
    @DisplayName("JniPoolStatistics getAverageAllocationTime should return Duration")
    void jniPoolStatisticsGetAverageAllocationTimeShouldReturnDuration() {
      JniPoolStatistics stats =
          new JniPoolStatistics(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1000000);

      Duration avgTime = stats.getAverageAllocationTime();
      assertNotNull(avgTime, "Average allocation time should not be null");
      assertEquals(
          Duration.ofMillis(1), avgTime, "Average allocation time should be 1 millisecond");
    }

    @Test
    @DisplayName("JniPoolStatistics getReuseRatio should calculate correctly")
    void jniPoolStatisticsGetReuseRatioShouldCalculateCorrectly() {
      // Formula: reused / (allocated + reused) = 100 / (100 + 100) = 0.5
      JniPoolStatistics stats =
          new JniPoolStatistics(100, 100, 50, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);

      double reuseRatio = stats.getReuseRatio();
      assertEquals(0.5, reuseRatio, 0.001, "Reuse ratio should be 0.5 (100/200)");
    }

    @Test
    @DisplayName("JniPoolStatistics getReuseRatio should handle zero allocations")
    void jniPoolStatisticsGetReuseRatioShouldHandleZeroAllocations() {
      JniPoolStatistics stats = new JniPoolStatistics();

      double reuseRatio = stats.getReuseRatio();
      assertEquals(0.0, reuseRatio, "Reuse ratio should be 0 when no allocations");
    }

    @Test
    @DisplayName("JniPoolStatistics getMemoryUtilization should calculate correctly")
    void jniPoolStatisticsGetMemoryUtilizationShouldCalculateCorrectly() {
      JniPoolStatistics stats =
          new JniPoolStatistics(0, 0, 0, 0, 0, 0, 0, 0, 0, 1000000, 500000, 0, 0, 0);

      double utilization = stats.getMemoryUtilization();
      assertEquals(0.5, utilization, 0.001, "Memory utilization should be 0.5 (500000/1000000)");
    }

    @Test
    @DisplayName("JniPoolStatistics getMemoryUtilization should handle zero peak")
    void jniPoolStatisticsGetMemoryUtilizationShouldHandleZeroPeak() {
      JniPoolStatistics stats = new JniPoolStatistics();

      double utilization = stats.getMemoryUtilization();
      assertEquals(0.0, utilization, "Memory utilization should be 0 when no peak usage");
    }

    @Test
    @DisplayName("JniPoolStatistics toString should contain relevant info")
    void jniPoolStatisticsToStringShouldContainRelevantInfo() {
      JniPoolStatistics stats =
          new JniPoolStatistics(100, 50, 50, 200, 100, 300, 150, 400, 200, 1000000, 500000, 5, 0, 0);

      String str = stats.toString();
      assertNotNull(str, "toString should not return null");
      assertTrue(str.contains("JniPoolStatistics"), "Should contain class name");
      assertTrue(str.contains("instancesAllocated"), "Should contain instancesAllocated");
      assertTrue(str.contains("instancesReused"), "Should contain instancesReused");
      assertTrue(str.contains("reuseRatio"), "Should contain reuseRatio");
      assertTrue(str.contains("memoryUtilization"), "Should contain memoryUtilization");
    }
  }

  // ========================================================================
  // JniPoolingAllocatorConfig Tests
  // ========================================================================

  @Nested
  @DisplayName("JniPoolingAllocatorConfig Tests")
  class JniPoolingAllocatorConfigTests {

    @Test
    @DisplayName("JniPoolingAllocatorConfig should be a final class")
    void jniPoolingAllocatorConfigShouldBeFinalClass() {
      assertTrue(
          Modifier.isFinal(JniPoolingAllocatorConfig.class.getModifiers()),
          "JniPoolingAllocatorConfig should be final");
    }

    @Test
    @DisplayName("JniPoolingAllocatorConfig should implement PoolingAllocatorConfig interface")
    void jniPoolingAllocatorConfigShouldImplementPoolingAllocatorConfigInterface() {
      assertTrue(
          PoolingAllocatorConfig.class.isAssignableFrom(JniPoolingAllocatorConfig.class),
          "JniPoolingAllocatorConfig should implement PoolingAllocatorConfig");
    }

    @Test
    @DisplayName("JniPoolingAllocatorConfig should have getInstancePoolSize method")
    void jniPoolingAllocatorConfigShouldHaveGetInstancePoolSizeMethod()
        throws NoSuchMethodException {
      Method method = JniPoolingAllocatorConfig.class.getMethod("getInstancePoolSize");
      assertNotNull(method, "getInstancePoolSize method should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }

    @Test
    @DisplayName("JniPoolingAllocatorConfig should have getMaxMemorySize method")
    void jniPoolingAllocatorConfigShouldHaveGetMaxMemorySizeMethod() throws NoSuchMethodException {
      Method method = JniPoolingAllocatorConfig.class.getMethod("getMaxMemorySize");
      assertNotNull(method, "getMaxMemorySize method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("JniPoolingAllocatorConfig should have getMaxTablesPerInstance method")
    void jniPoolingAllocatorConfigShouldHaveGetMaxTablesPerInstanceMethod()
        throws NoSuchMethodException {
      Method method = JniPoolingAllocatorConfig.class.getMethod("getMaxTablesPerInstance");
      assertNotNull(method, "getMaxTablesPerInstance method should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }

    @Test
    @DisplayName("JniPoolingAllocatorConfig should have getTableElements method")
    void jniPoolingAllocatorConfigShouldHaveGetTableElementsMethod()
        throws NoSuchMethodException {
      Method method = JniPoolingAllocatorConfig.class.getMethod("getTableElements");
      assertNotNull(method, "getTableElements method should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }

    @Test
    @DisplayName("JniPoolingAllocatorConfig should be in correct package")
    void jniPoolingAllocatorConfigShouldBeInCorrectPackage() {
      assertEquals(
          "ai.tegmentum.wasmtime4j.jni.pool",
          JniPoolingAllocatorConfig.class.getPackage().getName(),
          "JniPoolingAllocatorConfig should be in ai.tegmentum.wasmtime4j.jni.pool package");
    }
  }

  // ========================================================================
  // JniPoolingAllocatorConfigBuilder Tests
  // ========================================================================

  @Nested
  @DisplayName("JniPoolingAllocatorConfigBuilder Tests")
  class JniPoolingAllocatorConfigBuilderTests {

    @Test
    @DisplayName("JniPoolingAllocatorConfigBuilder should be a final class")
    void jniPoolingAllocatorConfigBuilderShouldBeFinalClass() {
      assertTrue(
          Modifier.isFinal(JniPoolingAllocatorConfigBuilder.class.getModifiers()),
          "JniPoolingAllocatorConfigBuilder should be final");
    }

    @Test
    @DisplayName(
        "JniPoolingAllocatorConfigBuilder should implement PoolingAllocatorConfigBuilder interface")
    void jniPoolingAllocatorConfigBuilderShouldImplementInterface() {
      assertTrue(
          PoolingAllocatorConfigBuilder.class.isAssignableFrom(
              JniPoolingAllocatorConfigBuilder.class),
          "JniPoolingAllocatorConfigBuilder should implement PoolingAllocatorConfigBuilder");
    }

    @Test
    @DisplayName("JniPoolingAllocatorConfigBuilder should have public constructor")
    void jniPoolingAllocatorConfigBuilderShouldHavePublicConstructor() {
      Constructor<?>[] constructors = JniPoolingAllocatorConfigBuilder.class.getConstructors();
      assertTrue(
          constructors.length > 0, "JniPoolingAllocatorConfigBuilder should have public constructor");
    }

    @Test
    @DisplayName("JniPoolingAllocatorConfigBuilder should have instancePoolSize method")
    void jniPoolingAllocatorConfigBuilderShouldHaveInstancePoolSizeMethod()
        throws NoSuchMethodException {
      Method method =
          JniPoolingAllocatorConfigBuilder.class.getMethod("instancePoolSize", int.class);
      assertNotNull(method, "instancePoolSize method should exist");
      assertEquals(
          PoolingAllocatorConfigBuilder.class,
          method.getReturnType(),
          "Should return builder interface for chaining");
    }

    @Test
    @DisplayName("JniPoolingAllocatorConfigBuilder should have maxMemorySize method")
    void jniPoolingAllocatorConfigBuilderShouldHaveMaxMemorySizeMethod()
        throws NoSuchMethodException {
      Method method =
          JniPoolingAllocatorConfigBuilder.class.getMethod("maxMemorySize", long.class);
      assertNotNull(method, "maxMemorySize method should exist");
      assertEquals(
          PoolingAllocatorConfigBuilder.class,
          method.getReturnType(),
          "Should return builder interface for chaining");
    }

    @Test
    @DisplayName("JniPoolingAllocatorConfigBuilder should have build method")
    void jniPoolingAllocatorConfigBuilderShouldHaveBuildMethod() throws NoSuchMethodException {
      Method method = JniPoolingAllocatorConfigBuilder.class.getMethod("build");
      assertNotNull(method, "build method should exist");
      assertEquals(
          PoolingAllocatorConfig.class,
          method.getReturnType(),
          "Should return PoolingAllocatorConfig");
    }

    @Test
    @DisplayName("JniPoolingAllocatorConfigBuilder build should create config")
    void jniPoolingAllocatorConfigBuilderBuildShouldCreateConfig() {
      JniPoolingAllocatorConfigBuilder builder = new JniPoolingAllocatorConfigBuilder();
      PoolingAllocatorConfig config = builder.build();

      assertNotNull(config, "Build should create config");
      assertTrue(
          config instanceof JniPoolingAllocatorConfig,
          "Build should create JniPoolingAllocatorConfig");
    }

    @Test
    @DisplayName("JniPoolingAllocatorConfigBuilder should support method chaining")
    void jniPoolingAllocatorConfigBuilderShouldSupportMethodChaining() {
      JniPoolingAllocatorConfigBuilder builder = new JniPoolingAllocatorConfigBuilder();

      PoolingAllocatorConfig config =
          builder.instancePoolSize(1000).maxMemorySize(1024L * 1024 * 100).build();

      assertNotNull(config, "Chained build should create config");
      assertEquals(1000, config.getInstancePoolSize(), "Instance pool size should be set");
      assertEquals(1024L * 1024 * 100, config.getMaxMemorySize(), "Max memory size should be set");
    }
  }

  // ========================================================================
  // JniPoolingAllocator Tests
  // ========================================================================

  @Nested
  @DisplayName("JniPoolingAllocator Tests")
  class JniPoolingAllocatorTests {

    @Test
    @DisplayName("JniPoolingAllocator should be a final class")
    void jniPoolingAllocatorShouldBeFinalClass() {
      assertTrue(
          Modifier.isFinal(JniPoolingAllocator.class.getModifiers()),
          "JniPoolingAllocator should be final");
    }

    @Test
    @DisplayName("JniPoolingAllocator should have public constructor with PoolingAllocatorConfig")
    void jniPoolingAllocatorShouldHavePublicConstructor() throws NoSuchMethodException {
      Constructor<?> constructor =
          JniPoolingAllocator.class.getConstructor(PoolingAllocatorConfig.class);
      assertNotNull(constructor, "Constructor should exist");
      assertTrue(
          Modifier.isPublic(constructor.getModifiers()), "Constructor should be public");
    }

    @Test
    @DisplayName("JniPoolingAllocator should have getStatistics method")
    void jniPoolingAllocatorShouldHaveGetStatisticsMethod() throws NoSuchMethodException {
      Method method = JniPoolingAllocator.class.getMethod("getStatistics");
      assertNotNull(method, "getStatistics method should exist");
      assertTrue(
          PoolStatistics.class.isAssignableFrom(method.getReturnType()),
          "Should return PoolStatistics");
    }

    @Test
    @DisplayName("JniPoolingAllocator should have close method")
    void jniPoolingAllocatorShouldHaveCloseMethod() throws NoSuchMethodException {
      Method method = JniPoolingAllocator.class.getMethod("close");
      assertNotNull(method, "close method should exist");
      assertEquals(void.class, method.getReturnType(), "close should return void");
    }

    @Test
    @DisplayName("JniPoolingAllocator should implement AutoCloseable")
    void jniPoolingAllocatorShouldImplementAutoCloseable() {
      assertTrue(
          AutoCloseable.class.isAssignableFrom(JniPoolingAllocator.class),
          "JniPoolingAllocator should implement AutoCloseable");
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
        JniPoolStatistics.class,
        JniPoolingAllocator.class,
        JniPoolingAllocatorConfig.class,
        JniPoolingAllocatorConfigBuilder.class
      };

      String expectedPackage = "ai.tegmentum.wasmtime4j.jni.pool";
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
        JniPoolStatistics.class,
        JniPoolingAllocator.class,
        JniPoolingAllocatorConfig.class,
        JniPoolingAllocatorConfigBuilder.class
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
        JniPoolStatistics.class,
        JniPoolingAllocator.class,
        JniPoolingAllocatorConfig.class,
        JniPoolingAllocatorConfigBuilder.class
      };

      for (Class<?> clazz : poolClasses) {
        assertFalse(clazz.isInterface(), clazz.getSimpleName() + " should not be an interface");
      }
    }

    @Test
    @DisplayName("Pool classes should have public constructors or factory methods")
    void poolClassesShouldHavePublicConstructorsOrFactoryMethods() {
      // JniPoolStatistics should have public constructor
      Constructor<?>[] statsConstructors = JniPoolStatistics.class.getConstructors();
      assertTrue(statsConstructors.length > 0, "JniPoolStatistics should have public constructor");

      // JniPoolingAllocatorConfigBuilder should have public constructor
      Constructor<?>[] builderConstructors =
          JniPoolingAllocatorConfigBuilder.class.getConstructors();
      assertTrue(
          builderConstructors.length > 0,
          "JniPoolingAllocatorConfigBuilder should have public constructor");
    }
  }
}
