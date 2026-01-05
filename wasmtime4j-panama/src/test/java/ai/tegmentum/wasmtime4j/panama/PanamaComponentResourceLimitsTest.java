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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.ComponentResourceLimits;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link PanamaComponentResourceLimits} class.
 *
 * <p>PanamaComponentResourceLimits provides resource limiting for WebAssembly components.
 */
@DisplayName("PanamaComponentResourceLimits Tests")
class PanamaComponentResourceLimitsTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be package-private final class")
    void shouldBePackagePrivateFinalClass() {
      // Not public - package-private
      assertTrue(
          !Modifier.isPublic(PanamaComponentResourceLimits.class.getModifiers()),
          "PanamaComponentResourceLimits should be package-private");
      assertTrue(
          Modifier.isFinal(PanamaComponentResourceLimits.class.getModifiers()),
          "PanamaComponentResourceLimits should be final");
    }

    @Test
    @DisplayName("should implement ComponentResourceLimits interface")
    void shouldImplementComponentResourceLimitsInterface() {
      assertTrue(
          ComponentResourceLimits.class.isAssignableFrom(PanamaComponentResourceLimits.class),
          "PanamaComponentResourceLimits should implement ComponentResourceLimits");
    }
  }

  @Nested
  @DisplayName("Limits Getter Method Tests")
  class LimitsGetterMethodTests {

    @Test
    @DisplayName("should have getMemoryLimits method")
    void shouldHaveGetMemoryLimitsMethod() throws NoSuchMethodException {
      final Method method = PanamaComponentResourceLimits.class.getMethod("getMemoryLimits");
      assertNotNull(method, "getMemoryLimits method should exist");
      assertEquals(
          ComponentResourceLimits.MemoryLimits.class,
          method.getReturnType(),
          "Should return MemoryLimits");
    }

    @Test
    @DisplayName("should have getExecutionLimits method")
    void shouldHaveGetExecutionLimitsMethod() throws NoSuchMethodException {
      final Method method = PanamaComponentResourceLimits.class.getMethod("getExecutionLimits");
      assertNotNull(method, "getExecutionLimits method should exist");
      assertEquals(
          ComponentResourceLimits.ExecutionLimits.class,
          method.getReturnType(),
          "Should return ExecutionLimits");
    }

    @Test
    @DisplayName("should have getIoLimits method")
    void shouldHaveGetIoLimitsMethod() throws NoSuchMethodException {
      final Method method = PanamaComponentResourceLimits.class.getMethod("getIoLimits");
      assertNotNull(method, "getIoLimits method should exist");
      assertEquals(
          ComponentResourceLimits.IoLimits.class, method.getReturnType(), "Should return IoLimits");
    }

    @Test
    @DisplayName("should have getNetworkLimits method")
    void shouldHaveGetNetworkLimitsMethod() throws NoSuchMethodException {
      final Method method = PanamaComponentResourceLimits.class.getMethod("getNetworkLimits");
      assertNotNull(method, "getNetworkLimits method should exist");
      assertEquals(
          ComponentResourceLimits.NetworkLimits.class,
          method.getReturnType(),
          "Should return NetworkLimits");
    }

    @Test
    @DisplayName("should have getFileSystemLimits method")
    void shouldHaveGetFileSystemLimitsMethod() throws NoSuchMethodException {
      final Method method = PanamaComponentResourceLimits.class.getMethod("getFileSystemLimits");
      assertNotNull(method, "getFileSystemLimits method should exist");
      assertEquals(
          ComponentResourceLimits.FileSystemLimits.class,
          method.getReturnType(),
          "Should return FileSystemLimits");
    }
  }

  @Nested
  @DisplayName("Validation Method Tests")
  class ValidationMethodTests {

    @Test
    @DisplayName("should have validate method")
    void shouldHaveValidateMethod() throws NoSuchMethodException {
      final Method method =
          PanamaComponentResourceLimits.class.getMethod(
              "validate", ComponentResourceLimits.ResourceUsage.class);
      assertNotNull(method, "validate method should exist");
      assertEquals(
          ComponentResourceLimits.ValidationResult.class,
          method.getReturnType(),
          "Should return ValidationResult");
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should have package-private default constructor")
    void shouldHavePackagePrivateDefaultConstructor() {
      boolean hasExpectedConstructor = false;
      for (var constructor : PanamaComponentResourceLimits.class.getDeclaredConstructors()) {
        if (constructor.getParameterCount() == 0) {
          hasExpectedConstructor = true;
          assertTrue(
              !Modifier.isPublic(constructor.getModifiers()),
              "Default constructor should be package-private");
          break;
        }
      }
      assertTrue(hasExpectedConstructor, "Should have default constructor");
    }
  }

  @Nested
  @DisplayName("Inner Class Tests")
  class InnerClassTests {

    @Test
    @DisplayName("should have inner classes for limit implementations")
    void shouldHaveInnerClasses() {
      Class<?>[] declaredClasses = PanamaComponentResourceLimits.class.getDeclaredClasses();
      assertTrue(declaredClasses.length > 0, "Should have inner classes");
    }

    @Test
    @DisplayName("should have DefaultMemoryLimits inner class")
    void shouldHaveDefaultMemoryLimitsInnerClass() {
      boolean found = false;
      for (var innerClass : PanamaComponentResourceLimits.class.getDeclaredClasses()) {
        if (innerClass.getSimpleName().equals("DefaultMemoryLimits")) {
          found = true;
          break;
        }
      }
      assertTrue(found, "Should have DefaultMemoryLimits inner class");
    }

    @Test
    @DisplayName("should have DefaultExecutionLimits inner class")
    void shouldHaveDefaultExecutionLimitsInnerClass() {
      boolean found = false;
      for (var innerClass : PanamaComponentResourceLimits.class.getDeclaredClasses()) {
        if (innerClass.getSimpleName().equals("DefaultExecutionLimits")) {
          found = true;
          break;
        }
      }
      assertTrue(found, "Should have DefaultExecutionLimits inner class");
    }

    @Test
    @DisplayName("should have DefaultValidationResult inner class")
    void shouldHaveDefaultValidationResultInnerClass() {
      boolean found = false;
      for (var innerClass : PanamaComponentResourceLimits.class.getDeclaredClasses()) {
        if (innerClass.getSimpleName().equals("DefaultValidationResult")) {
          found = true;
          break;
        }
      }
      assertTrue(found, "Should have DefaultValidationResult inner class");
    }

    @Test
    @DisplayName("should have DefaultLimitViolation inner class")
    void shouldHaveDefaultLimitViolationInnerClass() {
      boolean found = false;
      for (var innerClass : PanamaComponentResourceLimits.class.getDeclaredClasses()) {
        if (innerClass.getSimpleName().equals("DefaultLimitViolation")) {
          found = true;
          break;
        }
      }
      assertTrue(found, "Should have DefaultLimitViolation inner class");
    }
  }
}
