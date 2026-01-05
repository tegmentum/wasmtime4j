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

package ai.tegmentum.wasmtime4j.panama.wasi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link WasiDirectoryAccessControl} class.
 *
 * <p>WasiDirectoryAccessControl provides configurable directory access control system for WASI file
 * operations in Panama FFI context.
 */
@DisplayName("WasiDirectoryAccessControl Tests")
class WasiDirectoryAccessControlTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be public final class")
    void shouldBePublicFinalClass() {
      assertTrue(
          Modifier.isPublic(WasiDirectoryAccessControl.class.getModifiers()),
          "WasiDirectoryAccessControl should be public");
      assertTrue(
          Modifier.isFinal(WasiDirectoryAccessControl.class.getModifiers()),
          "WasiDirectoryAccessControl should be final");
    }
  }

  @Nested
  @DisplayName("Static Method Tests")
  class StaticMethodTests {

    @Test
    @DisplayName("should have builder static method")
    void shouldHaveBuilderMethod() throws NoSuchMethodException {
      final Method method = WasiDirectoryAccessControl.class.getMethod("builder");
      assertNotNull(method, "builder method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "Should be static");
      assertEquals(
          WasiDirectoryAccessControl.Builder.class,
          method.getReturnType(),
          "Should return Builder");
    }
  }

  @Nested
  @DisplayName("Validation Method Tests")
  class ValidationMethodTests {

    @Test
    @DisplayName("should have validateDirectoryAccess method")
    void shouldHaveValidateDirectoryAccessMethod() throws NoSuchMethodException {
      final Method method =
          WasiDirectoryAccessControl.class.getMethod(
              "validateDirectoryAccess", Path.class, WasiFileOperation.class);
      assertNotNull(method, "validateDirectoryAccess method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }
  }

  @Nested
  @DisplayName("Permission Grant Method Tests")
  class PermissionGrantMethodTests {

    @Test
    @DisplayName("should have grantDirectoryPermissions method")
    void shouldHaveGrantDirectoryPermissionsMethod() throws NoSuchMethodException {
      final Method method =
          WasiDirectoryAccessControl.class.getMethod(
              "grantDirectoryPermissions", String.class, Set.class, boolean.class);
      assertNotNull(method, "grantDirectoryPermissions method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have revokeDirectoryPermissions method")
    void shouldHaveRevokeDirectoryPermissionsMethod() throws NoSuchMethodException {
      final Method method =
          WasiDirectoryAccessControl.class.getMethod(
              "revokeDirectoryPermissions", String.class, Set.class);
      assertNotNull(method, "revokeDirectoryPermissions method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have setDirectoryRuleEnabled method")
    void shouldHaveSetDirectoryRuleEnabledMethod() throws NoSuchMethodException {
      final Method method =
          WasiDirectoryAccessControl.class.getMethod(
              "setDirectoryRuleEnabled", String.class, boolean.class);
      assertNotNull(method, "setDirectoryRuleEnabled method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }
  }

  @Nested
  @DisplayName("Query Method Tests")
  class QueryMethodTests {

    @Test
    @DisplayName("should have getDirectoryPermissions method")
    void shouldHaveGetDirectoryPermissionsMethod() throws NoSuchMethodException {
      final Method method =
          WasiDirectoryAccessControl.class.getMethod("getDirectoryPermissions", String.class);
      assertNotNull(method, "getDirectoryPermissions method should exist");
      assertEquals(Set.class, method.getReturnType(), "Should return Set");
    }

    @Test
    @DisplayName("should have listDirectoryRules method")
    void shouldHaveListDirectoryRulesMethod() throws NoSuchMethodException {
      final Method method = WasiDirectoryAccessControl.class.getMethod("listDirectoryRules");
      assertNotNull(method, "listDirectoryRules method should exist");
      assertEquals(Map.class, method.getReturnType(), "Should return Map");
    }

    @Test
    @DisplayName("should have getRuleCount method")
    void shouldHaveGetRuleCountMethod() throws NoSuchMethodException {
      final Method method = WasiDirectoryAccessControl.class.getMethod("getRuleCount");
      assertNotNull(method, "getRuleCount method should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }
  }

  @Nested
  @DisplayName("Cleanup Method Tests")
  class CleanupMethodTests {

    @Test
    @DisplayName("should have clearAllRules method")
    void shouldHaveClearAllRulesMethod() throws NoSuchMethodException {
      final Method method = WasiDirectoryAccessControl.class.getMethod("clearAllRules");
      assertNotNull(method, "clearAllRules method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }
  }

  @Nested
  @DisplayName("Builder Inner Class Tests")
  class BuilderTests {

    @Test
    @DisplayName("Builder should be public static final class")
    void builderShouldBePublicStaticFinal() {
      Class<?> innerClass = WasiDirectoryAccessControl.Builder.class;
      assertTrue(Modifier.isPublic(innerClass.getModifiers()), "Should be public");
      assertTrue(Modifier.isStatic(innerClass.getModifiers()), "Should be static");
      assertTrue(Modifier.isFinal(innerClass.getModifiers()), "Should be final");
    }

    @Test
    @DisplayName("Builder should have withInheritance method")
    void builderShouldHaveWithInheritanceMethod() throws NoSuchMethodException {
      final Method method =
          WasiDirectoryAccessControl.Builder.class.getMethod("withInheritance", boolean.class);
      assertNotNull(method, "withInheritance method should exist");
      assertEquals(
          WasiDirectoryAccessControl.Builder.class,
          method.getReturnType(),
          "Should return Builder");
    }

    @Test
    @DisplayName("Builder should have withStrictPathValidation method")
    void builderShouldHaveWithStrictPathValidationMethod() throws NoSuchMethodException {
      final Method method =
          WasiDirectoryAccessControl.Builder.class.getMethod(
              "withStrictPathValidation", boolean.class);
      assertNotNull(method, "withStrictPathValidation method should exist");
      assertEquals(
          WasiDirectoryAccessControl.Builder.class,
          method.getReturnType(),
          "Should return Builder");
    }

    @Test
    @DisplayName("Builder should have withAuditLogging method")
    void builderShouldHaveWithAuditLoggingMethod() throws NoSuchMethodException {
      final Method method =
          WasiDirectoryAccessControl.Builder.class.getMethod("withAuditLogging", boolean.class);
      assertNotNull(method, "withAuditLogging method should exist");
      assertEquals(
          WasiDirectoryAccessControl.Builder.class,
          method.getReturnType(),
          "Should return Builder");
    }

    @Test
    @DisplayName("Builder should have build method")
    void builderShouldHaveBuildMethod() throws NoSuchMethodException {
      final Method method = WasiDirectoryAccessControl.Builder.class.getMethod("build");
      assertNotNull(method, "build method should exist");
      assertEquals(
          WasiDirectoryAccessControl.class,
          method.getReturnType(),
          "Should return WasiDirectoryAccessControl");
    }
  }
}
