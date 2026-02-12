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

package ai.tegmentum.wasmtime4j.panama.wasi.permission;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.panama.wasi.WasiFileOperation;
import ai.tegmentum.wasmtime4j.wasi.permission.WasiResourceLimits;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link WasiPermissionManager} class.
 *
 * <p>WasiPermissionManager provides configurable permission system for controlling WASI
 * capabilities with fine-grained controls in Panama FFI implementation including file system access
 * controls, environment variable access restrictions, and resource limiting.
 */
@DisplayName("WasiPermissionManager Tests")
class WasiPermissionManagerTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be public final class")
    void shouldBePublicFinalClass() {
      assertTrue(
          Modifier.isPublic(WasiPermissionManager.class.getModifiers()),
          "WasiPermissionManager should be public");
      assertTrue(
          Modifier.isFinal(WasiPermissionManager.class.getModifiers()),
          "WasiPermissionManager should be final");
    }
  }

  @Nested
  @DisplayName("Static Factory Method Tests")
  class StaticFactoryMethodTests {

    @Test
    @DisplayName("should have defaultManager static method")
    void shouldHaveDefaultManagerMethod() throws NoSuchMethodException {
      final Method method = WasiPermissionManager.class.getMethod("defaultManager");
      assertNotNull(method, "defaultManager method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "Should be static");
      assertEquals(
          WasiPermissionManager.class,
          method.getReturnType(),
          "Should return WasiPermissionManager");
    }

    @Test
    @DisplayName("should have restrictiveManager static method")
    void shouldHaveRestrictiveManagerMethod() throws NoSuchMethodException {
      final Method method = WasiPermissionManager.class.getMethod("restrictiveManager");
      assertNotNull(method, "restrictiveManager method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "Should be static");
      assertEquals(
          WasiPermissionManager.class,
          method.getReturnType(),
          "Should return WasiPermissionManager");
    }

    @Test
    @DisplayName("should have permissiveManager static method")
    void shouldHavePermissiveManagerMethod() throws NoSuchMethodException {
      final Method method = WasiPermissionManager.class.getMethod("permissiveManager");
      assertNotNull(method, "permissiveManager method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "Should be static");
      assertEquals(
          WasiPermissionManager.class,
          method.getReturnType(),
          "Should return WasiPermissionManager");
    }

    @Test
    @DisplayName("should have builder static method")
    void shouldHaveBuilderMethod() throws NoSuchMethodException {
      final Method method = WasiPermissionManager.class.getMethod("builder");
      assertNotNull(method, "builder method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "Should be static");
      assertEquals(
          WasiPermissionManager.Builder.class, method.getReturnType(), "Should return Builder");
    }
  }

  @Nested
  @DisplayName("Validation Method Tests")
  class ValidationMethodTests {

    @Test
    @DisplayName("should have validateFileSystemAccess method with Path only")
    void shouldHaveValidateFileSystemAccessMethodPathOnly() throws NoSuchMethodException {
      final Method method =
          WasiPermissionManager.class.getMethod("validateFileSystemAccess", Path.class);
      assertNotNull(method, "validateFileSystemAccess method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have validateFileSystemAccess method with Path and Operation")
    void shouldHaveValidateFileSystemAccessMethodWithOperation() throws NoSuchMethodException {
      final Method method =
          WasiPermissionManager.class.getMethod(
              "validateFileSystemAccess", Path.class, WasiFileOperation.class);
      assertNotNull(method, "validateFileSystemAccess method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have validateEnvironmentAccess method")
    void shouldHaveValidateEnvironmentAccessMethod() throws NoSuchMethodException {
      final Method method =
          WasiPermissionManager.class.getMethod("validateEnvironmentAccess", String.class);
      assertNotNull(method, "validateEnvironmentAccess method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }
  }

  @Nested
  @DisplayName("Getter Method Tests")
  class GetterMethodTests {

    @Test
    @DisplayName("should have getResourceLimits method")
    void shouldHaveGetResourceLimitsMethod() throws NoSuchMethodException {
      final Method method = WasiPermissionManager.class.getMethod("getResourceLimits");
      assertNotNull(method, "getResourceLimits method should exist");
      assertEquals(
          WasiResourceLimits.class, method.getReturnType(), "Should return WasiResourceLimits");
    }

    @Test
    @DisplayName("should have areDangerousOperationsAllowed method")
    void shouldHaveAreDangerousOperationsAllowedMethod() throws NoSuchMethodException {
      final Method method = WasiPermissionManager.class.getMethod("areDangerousOperationsAllowed");
      assertNotNull(method, "areDangerousOperationsAllowed method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have isStrictPathValidationEnabled method")
    void shouldHaveIsStrictPathValidationEnabledMethod() throws NoSuchMethodException {
      final Method method = WasiPermissionManager.class.getMethod("isStrictPathValidationEnabled");
      assertNotNull(method, "isStrictPathValidationEnabled method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }
  }

  @Nested
  @DisplayName("Builder Inner Class Tests")
  class BuilderTests {

    @Test
    @DisplayName("Builder should be public static final class")
    void builderShouldBePublicStaticFinal() {
      Class<?> builderClass = WasiPermissionManager.Builder.class;
      assertTrue(Modifier.isPublic(builderClass.getModifiers()), "Should be public");
      assertTrue(Modifier.isStatic(builderClass.getModifiers()), "Should be static");
      assertTrue(Modifier.isFinal(builderClass.getModifiers()), "Should be final");
    }

    @Test
    @DisplayName("Builder should have withPathPermission method")
    void builderShouldHaveWithPathPermissionMethod() throws NoSuchMethodException {
      final Method method =
          WasiPermissionManager.Builder.class.getMethod(
              "withPathPermission", Path.class, WasiFileOperation.class);
      assertNotNull(method, "withPathPermission method should exist");
      assertEquals(
          WasiPermissionManager.Builder.class, method.getReturnType(), "Should return Builder");
    }

    @Test
    @DisplayName("Builder should have withPathPermissions method")
    void builderShouldHaveWithPathPermissionsMethod() throws NoSuchMethodException {
      final Method method =
          WasiPermissionManager.Builder.class.getMethod(
              "withPathPermissions", Path.class, WasiFileOperation[].class);
      assertNotNull(method, "withPathPermissions method should exist");
      assertEquals(
          WasiPermissionManager.Builder.class, method.getReturnType(), "Should return Builder");
    }

    @Test
    @DisplayName("Builder should have withGlobalPermission method")
    void builderShouldHaveWithGlobalPermissionMethod() throws NoSuchMethodException {
      final Method method =
          WasiPermissionManager.Builder.class.getMethod(
              "withGlobalPermission", WasiFileOperation.class);
      assertNotNull(method, "withGlobalPermission method should exist");
      assertEquals(
          WasiPermissionManager.Builder.class, method.getReturnType(), "Should return Builder");
    }

    @Test
    @DisplayName("Builder should have withGlobalPermissions method")
    void builderShouldHaveWithGlobalPermissionsMethod() throws NoSuchMethodException {
      final Method method =
          WasiPermissionManager.Builder.class.getMethod(
              "withGlobalPermissions", WasiFileOperation[].class);
      assertNotNull(method, "withGlobalPermissions method should exist");
      assertEquals(
          WasiPermissionManager.Builder.class, method.getReturnType(), "Should return Builder");
    }

    @Test
    @DisplayName("Builder should have withAllFileOperations method")
    void builderShouldHaveWithAllFileOperationsMethod() throws NoSuchMethodException {
      final Method method = WasiPermissionManager.Builder.class.getMethod("withAllFileOperations");
      assertNotNull(method, "withAllFileOperations method should exist");
      assertEquals(
          WasiPermissionManager.Builder.class, method.getReturnType(), "Should return Builder");
    }

    @Test
    @DisplayName("Builder should have withEnvironmentVariable method")
    void builderShouldHaveWithEnvironmentVariableMethod() throws NoSuchMethodException {
      final Method method =
          WasiPermissionManager.Builder.class.getMethod("withEnvironmentVariable", String.class);
      assertNotNull(method, "withEnvironmentVariable method should exist");
      assertEquals(
          WasiPermissionManager.Builder.class, method.getReturnType(), "Should return Builder");
    }

    @Test
    @DisplayName("Builder should have withAllEnvironmentVariables method")
    void builderShouldHaveWithAllEnvironmentVariablesMethod() throws NoSuchMethodException {
      final Method method =
          WasiPermissionManager.Builder.class.getMethod("withAllEnvironmentVariables");
      assertNotNull(method, "withAllEnvironmentVariables method should exist");
      assertEquals(
          WasiPermissionManager.Builder.class, method.getReturnType(), "Should return Builder");
    }

    @Test
    @DisplayName("Builder should have withDeniedEnvironmentVariable method")
    void builderShouldHaveWithDeniedEnvironmentVariableMethod() throws NoSuchMethodException {
      final Method method =
          WasiPermissionManager.Builder.class.getMethod(
              "withDeniedEnvironmentVariable", String.class);
      assertNotNull(method, "withDeniedEnvironmentVariable method should exist");
      assertEquals(
          WasiPermissionManager.Builder.class, method.getReturnType(), "Should return Builder");
    }

    @Test
    @DisplayName("Builder should have withResourceLimits method")
    void builderShouldHaveWithResourceLimitsMethod() throws NoSuchMethodException {
      final Method method =
          WasiPermissionManager.Builder.class.getMethod(
              "withResourceLimits", WasiResourceLimits.class);
      assertNotNull(method, "withResourceLimits method should exist");
      assertEquals(
          WasiPermissionManager.Builder.class, method.getReturnType(), "Should return Builder");
    }

    @Test
    @DisplayName("Builder should have withDangerousOperations method")
    void builderShouldHaveWithDangerousOperationsMethod() throws NoSuchMethodException {
      final Method method =
          WasiPermissionManager.Builder.class.getMethod("withDangerousOperations", boolean.class);
      assertNotNull(method, "withDangerousOperations method should exist");
      assertEquals(
          WasiPermissionManager.Builder.class, method.getReturnType(), "Should return Builder");
    }

    @Test
    @DisplayName("Builder should have withStrictPathValidation method")
    void builderShouldHaveWithStrictPathValidationMethod() throws NoSuchMethodException {
      final Method method =
          WasiPermissionManager.Builder.class.getMethod("withStrictPathValidation", boolean.class);
      assertNotNull(method, "withStrictPathValidation method should exist");
      assertEquals(
          WasiPermissionManager.Builder.class, method.getReturnType(), "Should return Builder");
    }

    @Test
    @DisplayName("Builder should have build method")
    void builderShouldHaveBuildMethod() throws NoSuchMethodException {
      final Method method = WasiPermissionManager.Builder.class.getMethod("build");
      assertNotNull(method, "build method should exist");
      assertEquals(
          WasiPermissionManager.class,
          method.getReturnType(),
          "Should return WasiPermissionManager");
    }
  }

  @Nested
  @DisplayName("Functional Tests")
  class FunctionalTests {

    @Test
    @DisplayName("defaultManager should return non-null instance")
    void defaultManagerShouldReturnNonNull() {
      assertNotNull(WasiPermissionManager.defaultManager(), "Should return non-null manager");
    }

    @Test
    @DisplayName("restrictiveManager should return non-null instance")
    void restrictiveManagerShouldReturnNonNull() {
      assertNotNull(WasiPermissionManager.restrictiveManager(), "Should return non-null manager");
    }

    @Test
    @DisplayName("permissiveManager should return non-null instance")
    void permissiveManagerShouldReturnNonNull() {
      assertNotNull(WasiPermissionManager.permissiveManager(), "Should return non-null manager");
    }

    @Test
    @DisplayName("builder should return non-null builder instance")
    void builderShouldReturnNonNull() {
      assertNotNull(WasiPermissionManager.builder(), "Should return non-null builder");
    }

    @Test
    @DisplayName("builder chain should work correctly")
    void builderChainShouldWork() {
      assertDoesNotThrow(
          () ->
              WasiPermissionManager.builder()
                  .withGlobalPermission(WasiFileOperation.READ)
                  .withDangerousOperations(false)
                  .withStrictPathValidation(true)
                  .withResourceLimits(WasiResourceLimits.defaultLimits())
                  .build(),
          "Builder chain should not throw");
    }
  }
}
