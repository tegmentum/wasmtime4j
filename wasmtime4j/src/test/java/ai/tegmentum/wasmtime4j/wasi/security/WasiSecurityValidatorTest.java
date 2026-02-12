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

package ai.tegmentum.wasmtime4j.wasi.security;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Path;
import java.util.regex.Pattern;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link WasiSecurityValidator} class.
 *
 * <p>WasiSecurityValidator provides WASI security validation with comprehensive protection against
 * common attacks including path traversal prevention, environment variable access control, and
 * resource access validation.
 */
@DisplayName("WasiSecurityValidator Tests")
class WasiSecurityValidatorTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be public final class")
    void shouldBePublicFinalClass() {
      assertTrue(
          Modifier.isPublic(WasiSecurityValidator.class.getModifiers()),
          "WasiSecurityValidator should be public");
      assertTrue(
          Modifier.isFinal(WasiSecurityValidator.class.getModifiers()),
          "WasiSecurityValidator should be final");
    }
  }

  @Nested
  @DisplayName("Static Factory Method Tests")
  class StaticFactoryMethodTests {

    @Test
    @DisplayName("should have defaultValidator static method")
    void shouldHaveDefaultValidatorMethod() throws NoSuchMethodException {
      final Method method = WasiSecurityValidator.class.getMethod("defaultValidator");
      assertNotNull(method, "defaultValidator method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "Should be static");
      assertEquals(
          WasiSecurityValidator.class,
          method.getReturnType(),
          "Should return WasiSecurityValidator");
    }

    @Test
    @DisplayName("should have builder static method")
    void shouldHaveBuilderMethod() throws NoSuchMethodException {
      final Method method = WasiSecurityValidator.class.getMethod("builder");
      assertNotNull(method, "builder method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "Should be static");
      assertEquals(
          WasiSecurityValidator.Builder.class, method.getReturnType(), "Should return Builder");
    }
  }

  @Nested
  @DisplayName("Validation Method Tests")
  class ValidationMethodTests {

    @Test
    @DisplayName("should have validatePath method")
    void shouldHaveValidatePathMethod() throws NoSuchMethodException {
      final Method method = WasiSecurityValidator.class.getMethod("validatePath", Path.class);
      assertNotNull(method, "validatePath method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have validateEnvironmentAccess method")
    void shouldHaveValidateEnvironmentAccessMethod() throws NoSuchMethodException {
      final Method method =
          WasiSecurityValidator.class.getMethod("validateEnvironmentAccess", String.class);
      assertNotNull(method, "validateEnvironmentAccess method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have validateResourceAccess method")
    void shouldHaveValidateResourceAccessMethod() throws NoSuchMethodException {
      final Method method =
          WasiSecurityValidator.class.getMethod("validateResourceAccess", String.class);
      assertNotNull(method, "validateResourceAccess method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }
  }

  @Nested
  @DisplayName("Builder Inner Class Tests")
  class BuilderTests {

    @Test
    @DisplayName("Builder should be public static final class")
    void builderShouldBePublicStaticFinal() {
      Class<?> builderClass = WasiSecurityValidator.Builder.class;
      assertTrue(Modifier.isPublic(builderClass.getModifiers()), "Should be public");
      assertTrue(Modifier.isStatic(builderClass.getModifiers()), "Should be static");
      assertTrue(Modifier.isFinal(builderClass.getModifiers()), "Should be final");
    }

    @Test
    @DisplayName("Builder should have withMaxPathLength method")
    void builderShouldHaveWithMaxPathLengthMethod() throws NoSuchMethodException {
      final Method method =
          WasiSecurityValidator.Builder.class.getMethod("withMaxPathLength", int.class);
      assertNotNull(method, "withMaxPathLength method should exist");
      assertEquals(
          WasiSecurityValidator.Builder.class, method.getReturnType(), "Should return Builder");
    }

    @Test
    @DisplayName("Builder should have withAllowAbsolutePaths method")
    void builderShouldHaveWithAllowAbsolutePathsMethod() throws NoSuchMethodException {
      final Method method =
          WasiSecurityValidator.Builder.class.getMethod("withAllowAbsolutePaths", boolean.class);
      assertNotNull(method, "withAllowAbsolutePaths method should exist");
      assertEquals(
          WasiSecurityValidator.Builder.class, method.getReturnType(), "Should return Builder");
    }

    @Test
    @DisplayName("Builder should have withAllowSymbolicLinks method")
    void builderShouldHaveWithAllowSymbolicLinksMethod() throws NoSuchMethodException {
      final Method method =
          WasiSecurityValidator.Builder.class.getMethod("withAllowSymbolicLinks", boolean.class);
      assertNotNull(method, "withAllowSymbolicLinks method should exist");
      assertEquals(
          WasiSecurityValidator.Builder.class, method.getReturnType(), "Should return Builder");
    }

    @Test
    @DisplayName("Builder should have withForbiddenPathComponent method")
    void builderShouldHaveWithForbiddenPathComponentMethod() throws NoSuchMethodException {
      final Method method =
          WasiSecurityValidator.Builder.class.getMethod("withForbiddenPathComponent", String.class);
      assertNotNull(method, "withForbiddenPathComponent method should exist");
      assertEquals(
          WasiSecurityValidator.Builder.class, method.getReturnType(), "Should return Builder");
    }

    @Test
    @DisplayName("Builder should have withAllowedEnvironmentPattern method with Pattern")
    void builderShouldHaveWithAllowedEnvironmentPatternMethodWithPattern()
        throws NoSuchMethodException {
      final Method method =
          WasiSecurityValidator.Builder.class.getMethod(
              "withAllowedEnvironmentPattern", Pattern.class);
      assertNotNull(method, "withAllowedEnvironmentPattern method should exist");
      assertEquals(
          WasiSecurityValidator.Builder.class, method.getReturnType(), "Should return Builder");
    }

    @Test
    @DisplayName("Builder should have withAllowedEnvironmentPattern method with String")
    void builderShouldHaveWithAllowedEnvironmentPatternMethodWithString()
        throws NoSuchMethodException {
      final Method method =
          WasiSecurityValidator.Builder.class.getMethod(
              "withAllowedEnvironmentPattern", String.class);
      assertNotNull(method, "withAllowedEnvironmentPattern method should exist");
      assertEquals(
          WasiSecurityValidator.Builder.class, method.getReturnType(), "Should return Builder");
    }

    @Test
    @DisplayName("Builder should have withForbiddenEnvironmentName method")
    void builderShouldHaveWithForbiddenEnvironmentNameMethod() throws NoSuchMethodException {
      final Method method =
          WasiSecurityValidator.Builder.class.getMethod(
              "withForbiddenEnvironmentName", String.class);
      assertNotNull(method, "withForbiddenEnvironmentName method should exist");
      assertEquals(
          WasiSecurityValidator.Builder.class, method.getReturnType(), "Should return Builder");
    }

    @Test
    @DisplayName("Builder should have build method")
    void builderShouldHaveBuildMethod() throws NoSuchMethodException {
      final Method method = WasiSecurityValidator.Builder.class.getMethod("build");
      assertNotNull(method, "build method should exist");
      assertEquals(
          WasiSecurityValidator.class,
          method.getReturnType(),
          "Should return WasiSecurityValidator");
    }
  }

  @Nested
  @DisplayName("Functional Tests")
  class FunctionalTests {

    @Test
    @DisplayName("defaultValidator should return non-null instance")
    void defaultValidatorShouldReturnNonNull() {
      assertNotNull(WasiSecurityValidator.defaultValidator(), "Should return non-null validator");
    }

    @Test
    @DisplayName("builder should return non-null builder instance")
    void builderShouldReturnNonNull() {
      assertNotNull(WasiSecurityValidator.builder(), "Should return non-null builder");
    }

    @Test
    @DisplayName("builder chain should work correctly")
    void builderChainShouldWork() {
      assertDoesNotThrow(
          () ->
              WasiSecurityValidator.builder()
                  .withMaxPathLength(1024)
                  .withAllowAbsolutePaths(true)
                  .withAllowSymbolicLinks(false)
                  .withForbiddenPathComponent("secret")
                  .withForbiddenEnvironmentName("API_KEY")
                  .build(),
          "Builder chain should not throw");
    }
  }
}
