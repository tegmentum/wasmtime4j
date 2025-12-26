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

package ai.tegmentum.wasmtime4j;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.ComponentRestoreOptions.ActionType;
import ai.tegmentum.wasmtime4j.ComponentRestoreOptions.ConflictResolution;
import ai.tegmentum.wasmtime4j.ComponentRestoreOptions.RestoreFilter;
import ai.tegmentum.wasmtime4j.ComponentRestoreOptions.RestoreMode;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ComponentRestoreOptions} interface.
 *
 * <p>ComponentRestoreOptions provides options for restoring WebAssembly components.
 */
@DisplayName("ComponentRestoreOptions Tests")
class ComponentRestoreOptionsTest {

  @Nested
  @DisplayName("Interface Structure Tests")
  class InterfaceStructureTests {

    @Test
    @DisplayName("should be public interface")
    void shouldBePublicInterface() {
      assertTrue(
          Modifier.isPublic(ComponentRestoreOptions.class.getModifiers()),
          "ComponentRestoreOptions should be public");
      assertTrue(
          ComponentRestoreOptions.class.isInterface(),
          "ComponentRestoreOptions should be an interface");
    }
  }

  @Nested
  @DisplayName("Method Signature Tests")
  class MethodSignatureTests {

    @Test
    @DisplayName("should have getMode method")
    void shouldHaveGetModeMethod() throws NoSuchMethodException {
      final Method method = ComponentRestoreOptions.class.getMethod("getMode");
      assertNotNull(method, "getMode method should exist");
      assertEquals(RestoreMode.class, method.getReturnType(), "Should return RestoreMode");
    }

    @Test
    @DisplayName("should have setMode method")
    void shouldHaveSetModeMethod() throws NoSuchMethodException {
      final Method method = ComponentRestoreOptions.class.getMethod("setMode", RestoreMode.class);
      assertNotNull(method, "setMode method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have isOverwriteEnabled method")
    void shouldHaveIsOverwriteEnabledMethod() throws NoSuchMethodException {
      final Method method = ComponentRestoreOptions.class.getMethod("isOverwriteEnabled");
      assertNotNull(method, "isOverwriteEnabled method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have isVerificationEnabled method")
    void shouldHaveIsVerificationEnabledMethod() throws NoSuchMethodException {
      final Method method = ComponentRestoreOptions.class.getMethod("isVerificationEnabled");
      assertNotNull(method, "isVerificationEnabled method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have getTargetComponentId method")
    void shouldHaveGetTargetComponentIdMethod() throws NoSuchMethodException {
      final Method method = ComponentRestoreOptions.class.getMethod("getTargetComponentId");
      assertNotNull(method, "getTargetComponentId method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }

    @Test
    @DisplayName("should have getConflictResolution method")
    void shouldHaveGetConflictResolutionMethod() throws NoSuchMethodException {
      final Method method = ComponentRestoreOptions.class.getMethod("getConflictResolution");
      assertNotNull(method, "getConflictResolution method should exist");
      assertEquals(
          ConflictResolution.class, method.getReturnType(), "Should return ConflictResolution");
    }

    @Test
    @DisplayName("should have getFilters method")
    void shouldHaveGetFiltersMethod() throws NoSuchMethodException {
      final Method method = ComponentRestoreOptions.class.getMethod("getFilters");
      assertNotNull(method, "getFilters method should exist");
      assertEquals(java.util.Set.class, method.getReturnType(), "Should return Set");
    }

    @Test
    @DisplayName("should have getTimeout method")
    void shouldHaveGetTimeoutMethod() throws NoSuchMethodException {
      final Method method = ComponentRestoreOptions.class.getMethod("getTimeout");
      assertNotNull(method, "getTimeout method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have getParameters method")
    void shouldHaveGetParametersMethod() throws NoSuchMethodException {
      final Method method = ComponentRestoreOptions.class.getMethod("getParameters");
      assertNotNull(method, "getParameters method should exist");
      assertEquals(java.util.Map.class, method.getReturnType(), "Should return Map");
    }
  }

  @Nested
  @DisplayName("RestoreMode Enum Tests")
  class RestoreModeEnumTests {

    @Test
    @DisplayName("should have all restore modes")
    void shouldHaveAllRestoreModes() {
      final var modes = RestoreMode.values();
      assertEquals(5, modes.length, "Should have 5 restore modes");
    }

    @Test
    @DisplayName("should have COMPLETE mode")
    void shouldHaveCompleteMode() {
      assertEquals(RestoreMode.COMPLETE, RestoreMode.valueOf("COMPLETE"));
    }

    @Test
    @DisplayName("should have PARTIAL mode")
    void shouldHavePartialMode() {
      assertEquals(RestoreMode.PARTIAL, RestoreMode.valueOf("PARTIAL"));
    }

    @Test
    @DisplayName("should have STATE_ONLY mode")
    void shouldHaveStateOnlyMode() {
      assertEquals(RestoreMode.STATE_ONLY, RestoreMode.valueOf("STATE_ONLY"));
    }

    @Test
    @DisplayName("should have CONFIG_ONLY mode")
    void shouldHaveConfigOnlyMode() {
      assertEquals(RestoreMode.CONFIG_ONLY, RestoreMode.valueOf("CONFIG_ONLY"));
    }

    @Test
    @DisplayName("should have CUSTOM mode")
    void shouldHaveCustomMode() {
      assertEquals(RestoreMode.CUSTOM, RestoreMode.valueOf("CUSTOM"));
    }
  }

  @Nested
  @DisplayName("ConflictResolution Enum Tests")
  class ConflictResolutionEnumTests {

    @Test
    @DisplayName("should have all conflict resolutions")
    void shouldHaveAllConflictResolutions() {
      final var resolutions = ConflictResolution.values();
      assertEquals(5, resolutions.length, "Should have 5 conflict resolutions");
    }

    @Test
    @DisplayName("should have FAIL resolution")
    void shouldHaveFailResolution() {
      assertEquals(ConflictResolution.FAIL, ConflictResolution.valueOf("FAIL"));
    }

    @Test
    @DisplayName("should have SKIP resolution")
    void shouldHaveSkipResolution() {
      assertEquals(ConflictResolution.SKIP, ConflictResolution.valueOf("SKIP"));
    }

    @Test
    @DisplayName("should have OVERWRITE resolution")
    void shouldHaveOverwriteResolution() {
      assertEquals(ConflictResolution.OVERWRITE, ConflictResolution.valueOf("OVERWRITE"));
    }

    @Test
    @DisplayName("should have MERGE resolution")
    void shouldHaveMergeResolution() {
      assertEquals(ConflictResolution.MERGE, ConflictResolution.valueOf("MERGE"));
    }

    @Test
    @DisplayName("should have INTERACTIVE resolution")
    void shouldHaveInteractiveResolution() {
      assertEquals(ConflictResolution.INTERACTIVE, ConflictResolution.valueOf("INTERACTIVE"));
    }
  }

  @Nested
  @DisplayName("RestoreFilter Enum Tests")
  class RestoreFilterEnumTests {

    @Test
    @DisplayName("should have all restore filters")
    void shouldHaveAllRestoreFilters() {
      final var filters = RestoreFilter.values();
      assertEquals(8, filters.length, "Should have 8 restore filters");
    }

    @Test
    @DisplayName("should have INCLUDE_CODE filter")
    void shouldHaveIncludeCodeFilter() {
      assertEquals(RestoreFilter.INCLUDE_CODE, RestoreFilter.valueOf("INCLUDE_CODE"));
    }

    @Test
    @DisplayName("should have INCLUDE_STATE filter")
    void shouldHaveIncludeStateFilter() {
      assertEquals(RestoreFilter.INCLUDE_STATE, RestoreFilter.valueOf("INCLUDE_STATE"));
    }

    @Test
    @DisplayName("should have INCLUDE_CONFIG filter")
    void shouldHaveIncludeConfigFilter() {
      assertEquals(RestoreFilter.INCLUDE_CONFIG, RestoreFilter.valueOf("INCLUDE_CONFIG"));
    }

    @Test
    @DisplayName("should have INCLUDE_METADATA filter")
    void shouldHaveIncludeMetadataFilter() {
      assertEquals(RestoreFilter.INCLUDE_METADATA, RestoreFilter.valueOf("INCLUDE_METADATA"));
    }

    @Test
    @DisplayName("should have INCLUDE_SECURITY filter")
    void shouldHaveIncludeSecurityFilter() {
      assertEquals(RestoreFilter.INCLUDE_SECURITY, RestoreFilter.valueOf("INCLUDE_SECURITY"));
    }

    @Test
    @DisplayName("should have INCLUDE_DEPENDENCIES filter")
    void shouldHaveIncludeDependenciesFilter() {
      assertEquals(
          RestoreFilter.INCLUDE_DEPENDENCIES, RestoreFilter.valueOf("INCLUDE_DEPENDENCIES"));
    }

    @Test
    @DisplayName("should have EXCLUDE_TEMPORARY filter")
    void shouldHaveExcludeTemporaryFilter() {
      assertEquals(RestoreFilter.EXCLUDE_TEMPORARY, RestoreFilter.valueOf("EXCLUDE_TEMPORARY"));
    }

    @Test
    @DisplayName("should have EXCLUDE_DEBUG filter")
    void shouldHaveExcludeDebugFilter() {
      assertEquals(RestoreFilter.EXCLUDE_DEBUG, RestoreFilter.valueOf("EXCLUDE_DEBUG"));
    }
  }

  @Nested
  @DisplayName("ActionType Enum Tests")
  class ActionTypeEnumTests {

    @Test
    @DisplayName("should have all action types")
    void shouldHaveAllActionTypes() {
      final var types = ActionType.values();
      assertEquals(7, types.length, "Should have 7 action types");
    }

    @Test
    @DisplayName("should have VALIDATION type")
    void shouldHaveValidationType() {
      assertEquals(ActionType.VALIDATION, ActionType.valueOf("VALIDATION"));
    }

    @Test
    @DisplayName("should have INITIALIZATION type")
    void shouldHaveInitializationType() {
      assertEquals(ActionType.INITIALIZATION, ActionType.valueOf("INITIALIZATION"));
    }

    @Test
    @DisplayName("should have CONFIGURATION type")
    void shouldHaveConfigurationType() {
      assertEquals(ActionType.CONFIGURATION, ActionType.valueOf("CONFIGURATION"));
    }

    @Test
    @DisplayName("should have VERIFICATION type")
    void shouldHaveVerificationType() {
      assertEquals(ActionType.VERIFICATION, ActionType.valueOf("VERIFICATION"));
    }

    @Test
    @DisplayName("should have CLEANUP type")
    void shouldHaveCleanupType() {
      assertEquals(ActionType.CLEANUP, ActionType.valueOf("CLEANUP"));
    }

    @Test
    @DisplayName("should have NOTIFICATION type")
    void shouldHaveNotificationType() {
      assertEquals(ActionType.NOTIFICATION, ActionType.valueOf("NOTIFICATION"));
    }

    @Test
    @DisplayName("should have CUSTOM type")
    void shouldHaveCustomType() {
      assertEquals(ActionType.CUSTOM, ActionType.valueOf("CUSTOM"));
    }
  }

  @Nested
  @DisplayName("Nested Interface Structure Tests")
  class NestedInterfaceStructureTests {

    @Test
    @DisplayName("should have all expected nested interfaces")
    void shouldHaveAllExpectedNestedInterfaces() {
      final var nestedClasses = ComponentRestoreOptions.class.getDeclaredClasses();
      final var classNames =
          java.util.Arrays.stream(nestedClasses)
              .map(Class::getSimpleName)
              .collect(java.util.stream.Collectors.toSet());

      assertTrue(classNames.contains("PostRestoreAction"), "Should have PostRestoreAction");
      assertTrue(classNames.contains("RestoreContext"), "Should have RestoreContext");
      assertTrue(classNames.contains("ActionResult"), "Should have ActionResult");
    }

    @Test
    @DisplayName("should have all expected enums")
    void shouldHaveAllExpectedEnums() {
      final var nestedClasses = ComponentRestoreOptions.class.getDeclaredClasses();
      final var enumNames =
          java.util.Arrays.stream(nestedClasses)
              .filter(Class::isEnum)
              .map(Class::getSimpleName)
              .collect(java.util.stream.Collectors.toSet());

      assertTrue(enumNames.contains("RestoreMode"), "Should have RestoreMode enum");
      assertTrue(enumNames.contains("ConflictResolution"), "Should have ConflictResolution enum");
      assertTrue(enumNames.contains("RestoreFilter"), "Should have RestoreFilter enum");
      assertTrue(enumNames.contains("ActionType"), "Should have ActionType enum");
    }
  }
}
