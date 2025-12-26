package ai.tegmentum.wasmtime4j.util;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link LibraryValidator} utility class.
 *
 * <p>LibraryValidator is a utility class for validating native library loading and runtime
 * availability. It is used in Docker containers and deployment environments to verify that
 * Wasmtime4j can successfully load and initialize.
 */
@DisplayName("LibraryValidator Tests")
class LibraryValidatorTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be a final class")
    void shouldBeFinalClass() {
      assertTrue(
          Modifier.isFinal(LibraryValidator.class.getModifiers()),
          "LibraryValidator should be a final class");
    }

    @Test
    @DisplayName("should have private constructor")
    void shouldHavePrivateConstructor() throws NoSuchMethodException {
      final Constructor<?> constructor = LibraryValidator.class.getDeclaredConstructor();
      assertTrue(
          Modifier.isPrivate(constructor.getModifiers()),
          "Constructor should be private to prevent instantiation");
    }

    @Test
    @DisplayName("should throw AssertionError when constructor is invoked via reflection")
    void shouldThrowAssertionErrorWhenConstructorInvoked() throws NoSuchMethodException {
      final Constructor<?> constructor = LibraryValidator.class.getDeclaredConstructor();
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
  @DisplayName("Public Methods Tests")
  class PublicMethodsTests {

    @Test
    @DisplayName("should have main method with String[] parameter")
    void shouldHaveMainMethod() throws NoSuchMethodException {
      final Method mainMethod = LibraryValidator.class.getMethod("main", String[].class);
      assertNotNull(mainMethod, "main method should exist");
      assertTrue(Modifier.isStatic(mainMethod.getModifiers()), "main method should be static");
      assertTrue(Modifier.isPublic(mainMethod.getModifiers()), "main method should be public");
      assertEquals(void.class, mainMethod.getReturnType(), "main should return void");
    }

    @Test
    @DisplayName("should have validateLibraries method")
    void shouldHaveValidateLibrariesMethod() throws NoSuchMethodException {
      final Method method = LibraryValidator.class.getMethod("validateLibraries");
      assertNotNull(method, "validateLibraries method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "Method should be static");
      assertTrue(Modifier.isPublic(method.getModifiers()), "Method should be public");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have canLoadLibraries method")
    void shouldHaveCanLoadLibrariesMethod() throws NoSuchMethodException {
      final Method method = LibraryValidator.class.getMethod("canLoadLibraries");
      assertNotNull(method, "canLoadLibraries method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "Method should be static");
      assertTrue(Modifier.isPublic(method.getModifiers()), "Method should be public");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have getRuntimeSummary method")
    void shouldHaveGetRuntimeSummaryMethod() throws NoSuchMethodException {
      final Method method = LibraryValidator.class.getMethod("getRuntimeSummary");
      assertNotNull(method, "getRuntimeSummary method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "Method should be static");
      assertTrue(Modifier.isPublic(method.getModifiers()), "Method should be public");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }
  }

  @Nested
  @DisplayName("canLoadLibraries Tests")
  class CanLoadLibrariesTests {

    @Test
    @DisplayName("should return boolean without throwing exception")
    void shouldReturnBooleanWithoutThrowing() {
      assertDoesNotThrow(
          () -> LibraryValidator.canLoadLibraries(), "canLoadLibraries should not throw exception");
    }

    @Test
    @DisplayName("should return consistent results on multiple calls")
    void shouldReturnConsistentResults() {
      final boolean firstResult = LibraryValidator.canLoadLibraries();
      final boolean secondResult = LibraryValidator.canLoadLibraries();
      assertEquals(
          firstResult,
          secondResult,
          "canLoadLibraries should return consistent results on repeated calls");
    }
  }

  @Nested
  @DisplayName("getRuntimeSummary Tests")
  class GetRuntimeSummaryTests {

    @Test
    @DisplayName("should return non-null summary")
    void shouldReturnNonNullSummary() {
      final String summary = LibraryValidator.getRuntimeSummary();
      assertNotNull(summary, "getRuntimeSummary should never return null");
    }

    @Test
    @DisplayName("should return non-empty summary")
    void shouldReturnNonEmptySummary() {
      final String summary = LibraryValidator.getRuntimeSummary();
      assertFalse(summary.isEmpty(), "getRuntimeSummary should return non-empty string");
    }

    @Test
    @DisplayName("should start with 'Runtime Summary:'")
    void shouldStartWithRuntimeSummary() {
      final String summary = LibraryValidator.getRuntimeSummary();
      assertTrue(
          summary.startsWith("Runtime Summary:"),
          "Summary should start with 'Runtime Summary:' prefix");
    }

    @Test
    @DisplayName("should contain runtime availability information")
    void shouldContainRuntimeAvailabilityInfo() {
      final String summary = LibraryValidator.getRuntimeSummary();
      // The summary should mention at least one of: JNI, Panama, available, not available, or error
      final boolean containsRelevantInfo =
          summary.contains("JNI")
              || summary.contains("Panama")
              || summary.contains("available")
              || summary.contains("runtimes")
              || summary.contains("Error");
      assertTrue(containsRelevantInfo, "Summary should contain runtime availability information");
    }
  }

  @Nested
  @DisplayName("validateLibraries Tests")
  class ValidateLibrariesTests {

    @Test
    @DisplayName("should return boolean without throwing exception")
    void shouldReturnBooleanWithoutThrowing() {
      assertDoesNotThrow(
          () -> LibraryValidator.validateLibraries(),
          "validateLibraries should not throw exception");
    }

    @Test
    @DisplayName("should return consistent results on multiple calls")
    void shouldReturnConsistentResults() {
      final boolean firstResult = LibraryValidator.validateLibraries();
      final boolean secondResult = LibraryValidator.validateLibraries();
      assertEquals(
          firstResult,
          secondResult,
          "validateLibraries should return consistent results on repeated calls");
    }

    @Test
    @DisplayName("canLoadLibraries should be consistent with validateLibraries")
    void shouldBeConsistentWithCanLoadLibraries() {
      // If canLoadLibraries returns false, validateLibraries should also return false
      // (but not necessarily vice versa, as validateLibraries does more comprehensive checks)
      final boolean canLoad = LibraryValidator.canLoadLibraries();
      final boolean validates = LibraryValidator.validateLibraries();

      if (!canLoad) {
        assertFalse(
            validates,
            "If canLoadLibraries returns false, validateLibraries should also return false");
      }
    }
  }

  @Nested
  @DisplayName("Private Method Tests via Reflection")
  class PrivateMethodTests {

    @Test
    @DisplayName("should have printSystemInformation private method")
    void shouldHavePrintSystemInformationMethod() throws NoSuchMethodException {
      final Method method = LibraryValidator.class.getDeclaredMethod("printSystemInformation");
      assertNotNull(method, "printSystemInformation method should exist");
      assertTrue(Modifier.isPrivate(method.getModifiers()), "Method should be private");
      assertTrue(Modifier.isStatic(method.getModifiers()), "Method should be static");
    }

    @Test
    @DisplayName("should have validateRuntimeAvailability private method")
    void shouldHaveValidateRuntimeAvailabilityMethod() throws NoSuchMethodException {
      final Method method = LibraryValidator.class.getDeclaredMethod("validateRuntimeAvailability");
      assertNotNull(method, "validateRuntimeAvailability method should exist");
      assertTrue(Modifier.isPrivate(method.getModifiers()), "Method should be private");
      assertTrue(Modifier.isStatic(method.getModifiers()), "Method should be static");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have validateRuntimeCreation private method")
    void shouldHaveValidateRuntimeCreationMethod() throws NoSuchMethodException {
      final Method method = LibraryValidator.class.getDeclaredMethod("validateRuntimeCreation");
      assertNotNull(method, "validateRuntimeCreation method should exist");
      assertTrue(Modifier.isPrivate(method.getModifiers()), "Method should be private");
      assertTrue(Modifier.isStatic(method.getModifiers()), "Method should be static");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have validateRuntimeFunctionality private method")
    void shouldHaveValidateRuntimeFunctionalityMethod() throws NoSuchMethodException {
      final Method method =
          LibraryValidator.class.getDeclaredMethod("validateRuntimeFunctionality");
      assertNotNull(method, "validateRuntimeFunctionality method should exist");
      assertTrue(Modifier.isPrivate(method.getModifiers()), "Method should be private");
      assertTrue(Modifier.isStatic(method.getModifiers()), "Method should be static");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }
  }

  @Nested
  @DisplayName("Output Tests")
  class OutputTests {

    @Test
    @DisplayName("printSystemInformation should produce output")
    void printSystemInformationShouldProduceOutput() throws Exception {
      // Capture stdout
      final PrintStream originalOut = System.out;
      final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      try {
        System.setOut(new PrintStream(outputStream));

        final Method method = LibraryValidator.class.getDeclaredMethod("printSystemInformation");
        method.setAccessible(true);
        method.invoke(null);

        final String output = outputStream.toString();
        assertTrue(output.contains("System Information:"), "Output should contain header");
        assertTrue(output.contains("Java Version:"), "Output should contain Java version");
        assertTrue(output.contains("OS Name:"), "Output should contain OS name");
      } finally {
        System.setOut(originalOut);
      }
    }
  }
}
