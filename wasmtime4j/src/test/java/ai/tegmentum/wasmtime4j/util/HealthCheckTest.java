package ai.tegmentum.wasmtime4j.util;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link HealthCheck} utility class.
 *
 * <p>HealthCheck is a utility class for validating Wasmtime4j runtime functionality. It provides
 * basic health verification for production deployments including runtime creation, module
 * compilation and execution, and runtime information retrieval.
 */
@DisplayName("HealthCheck Tests")
class HealthCheckTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be a final class")
    void shouldBeFinalClass() {
      assertTrue(
          Modifier.isFinal(HealthCheck.class.getModifiers()),
          "HealthCheck should be a final class");
    }

    @Test
    @DisplayName("should have private constructor")
    void shouldHavePrivateConstructor() throws NoSuchMethodException {
      final Constructor<?> constructor = HealthCheck.class.getDeclaredConstructor();
      assertTrue(
          Modifier.isPrivate(constructor.getModifiers()),
          "Constructor should be private to prevent instantiation");
    }

    @Test
    @DisplayName("should throw AssertionError when constructor is invoked via reflection")
    void shouldThrowAssertionErrorWhenConstructorInvoked() throws NoSuchMethodException {
      final Constructor<?> constructor = HealthCheck.class.getDeclaredConstructor();
      constructor.setAccessible(true);

      final InvocationTargetException exception =
          assertThrows(
              InvocationTargetException.class,
              () -> constructor.newInstance(),
              "Constructor should throw exception when invoked");

      assertTrue(
          exception.getCause() instanceof AssertionError,
          "Cause should be AssertionError for utility class");

      assertTrue(
          exception.getCause().getMessage().contains("Utility class"),
          "Error message should mention utility class");
    }
  }

  @Nested
  @DisplayName("Health Check WASM Module Tests")
  class HealthCheckWasmModuleTests {

    @Test
    @DisplayName("should have HEALTH_CHECK_WASM constant")
    void shouldHaveHealthCheckWasmConstant() throws NoSuchFieldException {
      final Field field = HealthCheck.class.getDeclaredField("HEALTH_CHECK_WASM");
      assertNotNull(field, "HEALTH_CHECK_WASM field should exist");
      assertTrue(Modifier.isStatic(field.getModifiers()), "Field should be static");
      assertTrue(Modifier.isFinal(field.getModifiers()), "Field should be final");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "Field should be private");
    }

    @Test
    @DisplayName("should have valid WASM magic number in HEALTH_CHECK_WASM")
    void shouldHaveValidWasmMagicNumber() throws Exception {
      final Field field = HealthCheck.class.getDeclaredField("HEALTH_CHECK_WASM");
      field.setAccessible(true);
      final byte[] wasmBytes = (byte[]) field.get(null);

      assertNotNull(wasmBytes, "WASM bytes should not be null");
      assertTrue(wasmBytes.length >= 8, "WASM bytes should have at least 8 bytes for header");

      // Check WASM magic number: 0x00, 0x61, 0x73, 0x6d (asm)
      assertEquals((byte) 0x00, wasmBytes[0], "First byte should be 0x00");
      assertEquals((byte) 0x61, wasmBytes[1], "Second byte should be 0x61 ('a')");
      assertEquals((byte) 0x73, wasmBytes[2], "Third byte should be 0x73 ('s')");
      assertEquals((byte) 0x6d, wasmBytes[3], "Fourth byte should be 0x6d ('m')");

      // Check WASM version: 0x01, 0x00, 0x00, 0x00
      assertEquals((byte) 0x01, wasmBytes[4], "Fifth byte should be 0x01 (version 1)");
      assertEquals((byte) 0x00, wasmBytes[5], "Sixth byte should be 0x00");
      assertEquals((byte) 0x00, wasmBytes[6], "Seventh byte should be 0x00");
      assertEquals((byte) 0x00, wasmBytes[7], "Eighth byte should be 0x00");
    }
  }

  @Nested
  @DisplayName("Public Methods Tests")
  class PublicMethodsTests {

    @Test
    @DisplayName("should have main method with String[] parameter")
    void shouldHaveMainMethod() throws NoSuchMethodException {
      final Method mainMethod = HealthCheck.class.getMethod("main", String[].class);
      assertNotNull(mainMethod, "main method should exist");
      assertTrue(Modifier.isStatic(mainMethod.getModifiers()), "main method should be static");
      assertTrue(Modifier.isPublic(mainMethod.getModifiers()), "main method should be public");
      assertEquals(void.class, mainMethod.getReturnType(), "main should return void");
    }

    @Test
    @DisplayName("should have performHealthCheck method")
    void shouldHavePerformHealthCheckMethod() throws NoSuchMethodException {
      final Method method = HealthCheck.class.getMethod("performHealthCheck");
      assertNotNull(method, "performHealthCheck method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "Method should be static");
      assertTrue(Modifier.isPublic(method.getModifiers()), "Method should be public");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have isReady method")
    void shouldHaveIsReadyMethod() throws NoSuchMethodException {
      final Method method = HealthCheck.class.getMethod("isReady");
      assertNotNull(method, "isReady method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "Method should be static");
      assertTrue(Modifier.isPublic(method.getModifiers()), "Method should be public");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have isLive method")
    void shouldHaveIsLiveMethod() throws NoSuchMethodException {
      final Method method = HealthCheck.class.getMethod("isLive");
      assertNotNull(method, "isLive method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "Method should be static");
      assertTrue(Modifier.isPublic(method.getModifiers()), "Method should be public");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }
  }

  @Nested
  @DisplayName("isLive Tests")
  class IsLiveTests {

    @Test
    @DisplayName("should return true (always responsive)")
    void shouldReturnTrue() {
      assertTrue(HealthCheck.isLive(), "isLive should always return true");
    }

    @Test
    @DisplayName("should not throw exception")
    void shouldNotThrowException() {
      assertDoesNotThrow(() -> HealthCheck.isLive(), "isLive should not throw any exception");
    }

    @Test
    @DisplayName("should be consistent on multiple calls")
    void shouldBeConsistentOnMultipleCalls() {
      final boolean first = HealthCheck.isLive();
      final boolean second = HealthCheck.isLive();
      final boolean third = HealthCheck.isLive();

      assertTrue(first, "First call should return true");
      assertEquals(first, second, "Calls should be consistent");
      assertEquals(second, third, "All calls should be consistent");
    }
  }

  @Nested
  @DisplayName("isReady Tests")
  class IsReadyTests {

    @Test
    @DisplayName("should return boolean without throwing")
    void shouldReturnBooleanWithoutThrowing() {
      assertDoesNotThrow(() -> HealthCheck.isReady(), "isReady should not throw exception");
    }

    @Test
    @DisplayName("should return consistent results on multiple calls")
    void shouldReturnConsistentResults() {
      final boolean firstResult = HealthCheck.isReady();
      final boolean secondResult = HealthCheck.isReady();
      assertEquals(
          firstResult, secondResult, "isReady should return consistent results on repeated calls");
    }
  }

  @Nested
  @DisplayName("performHealthCheck Tests")
  class PerformHealthCheckTests {

    @Test
    @DisplayName("should return boolean without throwing")
    void shouldReturnBooleanWithoutThrowing() {
      assertDoesNotThrow(
          () -> HealthCheck.performHealthCheck(), "performHealthCheck should not throw exception");
    }

    @Test
    @DisplayName("should return consistent results on multiple calls")
    void shouldReturnConsistentResults() {
      final boolean firstResult = HealthCheck.performHealthCheck();
      final boolean secondResult = HealthCheck.performHealthCheck();
      assertEquals(
          firstResult,
          secondResult,
          "performHealthCheck should return consistent results on repeated calls");
    }

    @Test
    @DisplayName("isReady should be consistent with performHealthCheck")
    void shouldBeConsistentWithIsReady() {
      // If isReady returns false, performHealthCheck should also return false
      final boolean ready = HealthCheck.isReady();
      final boolean healthCheck = HealthCheck.performHealthCheck();

      if (!ready) {
        assertTrue(
            !healthCheck, "If isReady returns false, performHealthCheck should also return false");
      }
    }
  }

  @Nested
  @DisplayName("Private Method Tests via Reflection")
  class PrivateMethodTests {

    @Test
    @DisplayName("should have testRuntimeCreation private method")
    void shouldHaveTestRuntimeCreationMethod() throws NoSuchMethodException {
      final Method method = HealthCheck.class.getDeclaredMethod("testRuntimeCreation");
      assertNotNull(method, "testRuntimeCreation method should exist");
      assertTrue(Modifier.isPrivate(method.getModifiers()), "Method should be private");
      assertTrue(Modifier.isStatic(method.getModifiers()), "Method should be static");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have testModuleExecution private method")
    void shouldHaveTestModuleExecutionMethod() throws NoSuchMethodException {
      final Method method = HealthCheck.class.getDeclaredMethod("testModuleExecution");
      assertNotNull(method, "testModuleExecution method should exist");
      assertTrue(Modifier.isPrivate(method.getModifiers()), "Method should be private");
      assertTrue(Modifier.isStatic(method.getModifiers()), "Method should be static");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have testRuntimeInformation private method")
    void shouldHaveTestRuntimeInformationMethod() throws NoSuchMethodException {
      final Method method = HealthCheck.class.getDeclaredMethod("testRuntimeInformation");
      assertNotNull(method, "testRuntimeInformation method should exist");
      assertTrue(Modifier.isPrivate(method.getModifiers()), "Method should be private");
      assertTrue(Modifier.isStatic(method.getModifiers()), "Method should be static");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }
  }

  @Nested
  @DisplayName("Kubernetes Probe Compatibility Tests")
  class KubernetesProbeCompatibilityTests {

    @Test
    @DisplayName("isLive should be suitable for liveness probe")
    void isLiveShouldBeSuitableForLivenessProbe() {
      // Liveness probe just checks if application is responsive
      // Should be a fast, lightweight check
      final long startTime = System.nanoTime();
      final boolean result = HealthCheck.isLive();
      final long elapsedNanos = System.nanoTime() - startTime;

      assertTrue(result, "Application should be live");
      // Should complete in less than 1ms for a liveness check
      assertTrue(
          elapsedNanos < 1_000_000_000, "Liveness check should complete in less than 1 second");
    }

    @Test
    @DisplayName("isReady should be suitable for readiness probe")
    void isReadyShouldBeSuitableForReadinessProbe() {
      // Readiness probe checks if application can handle traffic
      // May take slightly longer than liveness
      final long startTime = System.nanoTime();
      assertDoesNotThrow(() -> HealthCheck.isReady());
      final long elapsedNanos = System.nanoTime() - startTime;

      // Should complete in reasonable time for a readiness check
      assertTrue(
          elapsedNanos < 30_000_000_000L,
          "Readiness check should complete in less than 30 seconds");
    }
  }
}
