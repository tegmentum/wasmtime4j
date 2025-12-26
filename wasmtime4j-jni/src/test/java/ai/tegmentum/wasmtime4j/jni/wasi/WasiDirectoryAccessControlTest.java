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

package ai.tegmentum.wasmtime4j.jni.wasi;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.jni.exception.JniException;
import ai.tegmentum.wasmtime4j.jni.wasi.exception.WasiPermissionException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Comprehensive tests for {@link WasiDirectoryAccessControl}. */
@DisplayName("WasiDirectoryAccessControl Tests")
class WasiDirectoryAccessControlTest {

  @TempDir Path tempDir;

  private WasiDirectoryAccessControl accessControl;

  @BeforeEach
  void setUp() {
    accessControl =
        WasiDirectoryAccessControl.builder()
            .withInheritance(true)
            .withStrictPathValidation(false) // Avoid filesystem checks in tests
            .withAuditLogging(false)
            .build();
  }

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("WasiDirectoryAccessControl should be final class")
    void shouldBeFinalClass() {
      assertTrue(
          java.lang.reflect.Modifier.isFinal(WasiDirectoryAccessControl.class.getModifiers()),
          "WasiDirectoryAccessControl should be final");
    }
  }

  @Nested
  @DisplayName("Builder Tests")
  class BuilderTests {

    @Test
    @DisplayName("Builder should create access control with defaults")
    void builderShouldCreateWithDefaults() {
      final WasiDirectoryAccessControl control = WasiDirectoryAccessControl.builder().build();

      assertNotNull(control, "Access control should be created");
      assertEquals(0, control.getRuleCount(), "Should have no rules initially");
    }

    @Test
    @DisplayName("Builder should configure inheritance")
    void builderShouldConfigureInheritance() {
      final WasiDirectoryAccessControl control =
          WasiDirectoryAccessControl.builder().withInheritance(false).build();

      assertNotNull(control, "Access control should be created");
    }

    @Test
    @DisplayName("Builder should configure strict path validation")
    void builderShouldConfigureStrictPathValidation() {
      final WasiDirectoryAccessControl control =
          WasiDirectoryAccessControl.builder().withStrictPathValidation(true).build();

      assertNotNull(control, "Access control should be created");
    }

    @Test
    @DisplayName("Builder should configure audit logging")
    void builderShouldConfigureAuditLogging() {
      final WasiDirectoryAccessControl control =
          WasiDirectoryAccessControl.builder().withAuditLogging(true).build();

      assertNotNull(control, "Access control should be created");
    }

    @Test
    @DisplayName("Builder should add global default permission")
    void builderShouldAddGlobalDefaultPermission() {
      final WasiDirectoryAccessControl control =
          WasiDirectoryAccessControl.builder()
              .withGlobalDefaultPermission(WasiFileOperation.READ)
              .withStrictPathValidation(false)
              .build();

      assertNotNull(control, "Access control should be created");

      // Global default permission should allow READ on any directory
      assertDoesNotThrow(() -> control.validateDirectoryAccess(tempDir, WasiFileOperation.READ));
    }

    @Test
    @DisplayName("Builder should throw on null permission")
    void builderShouldThrowOnNullPermission() {
      assertThrows(
          JniException.class,
          () -> WasiDirectoryAccessControl.builder().withGlobalDefaultPermission(null),
          "Should throw on null permission");
    }

    @Test
    @DisplayName("Builder should add directory permissions")
    void builderShouldAddDirectoryPermissions() {
      final Set<WasiFileOperation> permissions =
          EnumSet.of(WasiFileOperation.READ, WasiFileOperation.WRITE);

      final WasiDirectoryAccessControl control =
          WasiDirectoryAccessControl.builder()
              .withDirectoryPermissions(tempDir.toString(), permissions, false)
              .withStrictPathValidation(false)
              .build();

      assertEquals(1, control.getRuleCount(), "Should have one rule");
    }

    @Test
    @DisplayName("Builder should throw on null directory path")
    void builderShouldThrowOnNullDirectoryPath() {
      final Set<WasiFileOperation> permissions = EnumSet.of(WasiFileOperation.READ);

      assertThrows(
          JniException.class,
          () ->
              WasiDirectoryAccessControl.builder()
                  .withDirectoryPermissions(null, permissions, false),
          "Should throw on null directory path");
    }

    @Test
    @DisplayName("Builder should throw on empty directory path")
    void builderShouldThrowOnEmptyDirectoryPath() {
      final Set<WasiFileOperation> permissions = EnumSet.of(WasiFileOperation.READ);

      assertThrows(
          JniException.class,
          () ->
              WasiDirectoryAccessControl.builder().withDirectoryPermissions("", permissions, false),
          "Should throw on empty directory path");
    }

    @Test
    @DisplayName("Builder should throw on null permissions set")
    void builderShouldThrowOnNullPermissionsSet() {
      assertThrows(
          JniException.class,
          () ->
              WasiDirectoryAccessControl.builder()
                  .withDirectoryPermissions(tempDir.toString(), null, false),
          "Should throw on null permissions set");
    }
  }

  @Nested
  @DisplayName("validateDirectoryAccess Tests")
  class ValidateDirectoryAccessTests {

    @Test
    @DisplayName("Should throw on null path")
    void shouldThrowOnNullPath() {
      assertThrows(
          JniException.class,
          () -> accessControl.validateDirectoryAccess(null, WasiFileOperation.READ),
          "Should throw on null path");
    }

    @Test
    @DisplayName("Should throw on null operation")
    void shouldThrowOnNullOperation() {
      assertThrows(
          JniException.class,
          () -> accessControl.validateDirectoryAccess(tempDir, null),
          "Should throw on null operation");
    }

    @Test
    @DisplayName("Should deny access when no permissions configured")
    void shouldDenyAccessWhenNoPermissionsConfigured() {
      assertThrows(
          WasiPermissionException.class,
          () -> accessControl.validateDirectoryAccess(tempDir, WasiFileOperation.READ),
          "Should deny access when no permissions configured");
    }

    @Test
    @DisplayName("Should allow access when permission granted")
    void shouldAllowAccessWhenPermissionGranted() {
      accessControl.grantDirectoryPermissions(
          tempDir.toString(), EnumSet.of(WasiFileOperation.READ), false);

      assertDoesNotThrow(
          () -> accessControl.validateDirectoryAccess(tempDir, WasiFileOperation.READ),
          "Should allow access when permission granted");
    }

    @Test
    @DisplayName("Should deny access for ungrated operation")
    void shouldDenyAccessForUngrantedOperation() {
      accessControl.grantDirectoryPermissions(
          tempDir.toString(), EnumSet.of(WasiFileOperation.READ), false);

      assertThrows(
          WasiPermissionException.class,
          () -> accessControl.validateDirectoryAccess(tempDir, WasiFileOperation.WRITE),
          "Should deny access for ungranted operation");
    }

    @Test
    @DisplayName("Should allow access through inheritance")
    void shouldAllowAccessThroughInheritance() throws IOException {
      // Create parent with recursive permission
      accessControl.grantDirectoryPermissions(
          tempDir.toString(), EnumSet.of(WasiFileOperation.READ), true); // Recursive

      // Create subdirectory
      final Path subDir = Files.createDirectories(tempDir.resolve("subdir"));

      // Build new control with inheritance enabled
      final WasiDirectoryAccessControl inheritingControl =
          WasiDirectoryAccessControl.builder()
              .withInheritance(true)
              .withStrictPathValidation(false)
              .withDirectoryPermissions(
                  tempDir.toString(), EnumSet.of(WasiFileOperation.READ), true)
              .build();

      assertDoesNotThrow(
          () -> inheritingControl.validateDirectoryAccess(subDir, WasiFileOperation.READ),
          "Should allow access through inheritance");
    }
  }

  @Nested
  @DisplayName("grantDirectoryPermissions Tests")
  class GrantDirectoryPermissionsTests {

    @Test
    @DisplayName("Should throw on null directory path")
    void shouldThrowOnNullDirectoryPath() {
      assertThrows(
          JniException.class,
          () ->
              accessControl.grantDirectoryPermissions(
                  null, EnumSet.of(WasiFileOperation.READ), false),
          "Should throw on null directory path");
    }

    @Test
    @DisplayName("Should throw on empty directory path")
    void shouldThrowOnEmptyDirectoryPath() {
      assertThrows(
          JniException.class,
          () ->
              accessControl.grantDirectoryPermissions(
                  "", EnumSet.of(WasiFileOperation.READ), false),
          "Should throw on empty directory path");
    }

    @Test
    @DisplayName("Should throw on null permissions")
    void shouldThrowOnNullPermissions() {
      assertThrows(
          JniException.class,
          () -> accessControl.grantDirectoryPermissions(tempDir.toString(), null, false),
          "Should throw on null permissions");
    }

    @Test
    @DisplayName("Should grant permissions successfully")
    void shouldGrantPermissionsSuccessfully() {
      accessControl.grantDirectoryPermissions(
          tempDir.toString(), EnumSet.of(WasiFileOperation.READ, WasiFileOperation.WRITE), false);

      assertEquals(1, accessControl.getRuleCount(), "Should have one rule");
    }

    @Test
    @DisplayName("Should grant recursive permissions")
    void shouldGrantRecursivePermissions() {
      accessControl.grantDirectoryPermissions(
          tempDir.toString(), EnumSet.of(WasiFileOperation.READ), true);

      assertEquals(1, accessControl.getRuleCount(), "Should have one rule");
    }

    @Test
    @DisplayName("Should overwrite existing permissions")
    void shouldOverwriteExistingPermissions() {
      accessControl.grantDirectoryPermissions(
          tempDir.toString(), EnumSet.of(WasiFileOperation.READ), false);

      accessControl.grantDirectoryPermissions(
          tempDir.toString(), EnumSet.of(WasiFileOperation.WRITE), false);

      assertEquals(1, accessControl.getRuleCount(), "Should still have one rule");

      final Set<WasiFileOperation> perms =
          accessControl.getDirectoryPermissions(tempDir.toString());
      assertTrue(perms.contains(WasiFileOperation.WRITE), "Should have WRITE permission");
      assertFalse(perms.contains(WasiFileOperation.READ), "Should not have READ permission");
    }
  }

  @Nested
  @DisplayName("revokeDirectoryPermissions Tests")
  class RevokeDirectoryPermissionsTests {

    @Test
    @DisplayName("Should throw on null directory path")
    void shouldThrowOnNullDirectoryPath() {
      assertThrows(
          JniException.class,
          () -> accessControl.revokeDirectoryPermissions(null, EnumSet.of(WasiFileOperation.READ)),
          "Should throw on null directory path");
    }

    @Test
    @DisplayName("Should throw on empty directory path")
    void shouldThrowOnEmptyDirectoryPath() {
      assertThrows(
          JniException.class,
          () -> accessControl.revokeDirectoryPermissions("", EnumSet.of(WasiFileOperation.READ)),
          "Should throw on empty directory path");
    }

    @Test
    @DisplayName("Should throw on null permissions")
    void shouldThrowOnNullPermissions() {
      assertThrows(
          JniException.class,
          () -> accessControl.revokeDirectoryPermissions(tempDir.toString(), null),
          "Should throw on null permissions");
    }

    @Test
    @DisplayName("Should revoke specific permissions")
    void shouldRevokeSpecificPermissions() {
      accessControl.grantDirectoryPermissions(
          tempDir.toString(), EnumSet.of(WasiFileOperation.READ, WasiFileOperation.WRITE), false);

      accessControl.revokeDirectoryPermissions(
          tempDir.toString(), EnumSet.of(WasiFileOperation.WRITE));

      final Set<WasiFileOperation> perms =
          accessControl.getDirectoryPermissions(tempDir.toString());
      assertTrue(perms.contains(WasiFileOperation.READ), "Should still have READ permission");
      assertFalse(perms.contains(WasiFileOperation.WRITE), "Should not have WRITE permission");
    }

    @Test
    @DisplayName("Should remove rule when all permissions revoked")
    void shouldRemoveRuleWhenAllPermissionsRevoked() {
      accessControl.grantDirectoryPermissions(
          tempDir.toString(), EnumSet.of(WasiFileOperation.READ), false);

      assertEquals(1, accessControl.getRuleCount(), "Should have one rule");

      accessControl.revokeDirectoryPermissions(
          tempDir.toString(), EnumSet.of(WasiFileOperation.READ));

      assertEquals(0, accessControl.getRuleCount(), "Rule should be removed");
    }

    @Test
    @DisplayName("Should handle revoke on non-existent rule gracefully")
    void shouldHandleRevokeOnNonExistentRuleGracefully() {
      assertDoesNotThrow(
          () ->
              accessControl.revokeDirectoryPermissions(
                  tempDir.toString(), EnumSet.of(WasiFileOperation.READ)),
          "Should handle non-existent rule gracefully");
    }
  }

  @Nested
  @DisplayName("setDirectoryRuleEnabled Tests")
  class SetDirectoryRuleEnabledTests {

    @Test
    @DisplayName("Should throw on null directory path")
    void shouldThrowOnNullDirectoryPath() {
      assertThrows(
          JniException.class,
          () -> accessControl.setDirectoryRuleEnabled(null, true),
          "Should throw on null directory path");
    }

    @Test
    @DisplayName("Should throw on empty directory path")
    void shouldThrowOnEmptyDirectoryPath() {
      assertThrows(
          JniException.class,
          () -> accessControl.setDirectoryRuleEnabled("", true),
          "Should throw on empty directory path");
    }

    @Test
    @DisplayName("Should disable rule")
    void shouldDisableRule() {
      accessControl.grantDirectoryPermissions(
          tempDir.toString(), EnumSet.of(WasiFileOperation.READ), false);

      accessControl.setDirectoryRuleEnabled(tempDir.toString(), false);

      // Should deny access now that rule is disabled
      assertThrows(
          WasiPermissionException.class,
          () -> accessControl.validateDirectoryAccess(tempDir, WasiFileOperation.READ),
          "Should deny access when rule disabled");
    }

    @Test
    @DisplayName("Should enable rule")
    void shouldEnableRule() {
      accessControl.grantDirectoryPermissions(
          tempDir.toString(), EnumSet.of(WasiFileOperation.READ), false);

      accessControl.setDirectoryRuleEnabled(tempDir.toString(), false);
      accessControl.setDirectoryRuleEnabled(tempDir.toString(), true);

      // Should allow access now that rule is re-enabled
      assertDoesNotThrow(
          () -> accessControl.validateDirectoryAccess(tempDir, WasiFileOperation.READ),
          "Should allow access when rule enabled");
    }

    @Test
    @DisplayName("Should handle non-existent rule gracefully")
    void shouldHandleNonExistentRuleGracefully() {
      assertDoesNotThrow(
          () -> accessControl.setDirectoryRuleEnabled(tempDir.toString(), true),
          "Should handle non-existent rule gracefully");
    }
  }

  @Nested
  @DisplayName("getDirectoryPermissions Tests")
  class GetDirectoryPermissionsTests {

    @Test
    @DisplayName("Should throw on null directory path")
    void shouldThrowOnNullDirectoryPath() {
      assertThrows(
          JniException.class,
          () -> accessControl.getDirectoryPermissions(null),
          "Should throw on null directory path");
    }

    @Test
    @DisplayName("Should throw on empty directory path")
    void shouldThrowOnEmptyDirectoryPath() {
      assertThrows(
          JniException.class,
          () -> accessControl.getDirectoryPermissions(""),
          "Should throw on empty directory path");
    }

    @Test
    @DisplayName("Should return empty set for unknown directory")
    void shouldReturnEmptySetForUnknownDirectory() {
      final Set<WasiFileOperation> perms =
          accessControl.getDirectoryPermissions(tempDir.toString());

      assertNotNull(perms, "Permissions should not be null");
      assertTrue(perms.isEmpty(), "Should be empty for unknown directory");
    }

    @Test
    @DisplayName("Should return configured permissions")
    void shouldReturnConfiguredPermissions() {
      accessControl.grantDirectoryPermissions(
          tempDir.toString(), EnumSet.of(WasiFileOperation.READ, WasiFileOperation.WRITE), false);

      final Set<WasiFileOperation> perms =
          accessControl.getDirectoryPermissions(tempDir.toString());

      assertEquals(2, perms.size(), "Should have 2 permissions");
      assertTrue(perms.contains(WasiFileOperation.READ), "Should have READ");
      assertTrue(perms.contains(WasiFileOperation.WRITE), "Should have WRITE");
    }

    @Test
    @DisplayName("Should return global defaults when no explicit rule")
    void shouldReturnGlobalDefaultsWhenNoExplicitRule() {
      final WasiDirectoryAccessControl controlWithDefaults =
          WasiDirectoryAccessControl.builder()
              .withGlobalDefaultPermission(WasiFileOperation.READ)
              .withStrictPathValidation(false)
              .build();

      final Set<WasiFileOperation> perms =
          controlWithDefaults.getDirectoryPermissions(tempDir.toString());

      assertTrue(perms.contains(WasiFileOperation.READ), "Should have global default READ");
    }
  }

  @Nested
  @DisplayName("listDirectoryRules Tests")
  class ListDirectoryRulesTests {

    @Test
    @DisplayName("Should return empty map initially")
    void shouldReturnEmptyMapInitially() {
      final Map<String, Set<WasiFileOperation>> rules = accessControl.listDirectoryRules();

      assertNotNull(rules, "Rules map should not be null");
      assertTrue(rules.isEmpty(), "Should be empty initially");
    }

    @Test
    @DisplayName("Should list all configured rules")
    void shouldListAllConfiguredRules() throws IOException {
      final Path dir1 = Files.createDirectories(tempDir.resolve("dir1"));
      final Path dir2 = Files.createDirectories(tempDir.resolve("dir2"));

      accessControl.grantDirectoryPermissions(
          dir1.toString(), EnumSet.of(WasiFileOperation.READ), false);

      accessControl.grantDirectoryPermissions(
          dir2.toString(), EnumSet.of(WasiFileOperation.WRITE), false);

      final Map<String, Set<WasiFileOperation>> rules = accessControl.listDirectoryRules();

      assertEquals(2, rules.size(), "Should have 2 rules");
    }

    @Test
    @DisplayName("Should not list disabled rules")
    void shouldNotListDisabledRules() {
      accessControl.grantDirectoryPermissions(
          tempDir.toString(), EnumSet.of(WasiFileOperation.READ), false);

      accessControl.setDirectoryRuleEnabled(tempDir.toString(), false);

      final Map<String, Set<WasiFileOperation>> rules = accessControl.listDirectoryRules();

      assertTrue(rules.isEmpty(), "Disabled rules should not be listed");
    }
  }

  @Nested
  @DisplayName("clearAllRules Tests")
  class ClearAllRulesTests {

    @Test
    @DisplayName("Should clear all rules")
    void shouldClearAllRules() throws IOException {
      final Path dir1 = Files.createDirectories(tempDir.resolve("dir1"));
      final Path dir2 = Files.createDirectories(tempDir.resolve("dir2"));

      accessControl.grantDirectoryPermissions(
          dir1.toString(), EnumSet.of(WasiFileOperation.READ), false);

      accessControl.grantDirectoryPermissions(
          dir2.toString(), EnumSet.of(WasiFileOperation.WRITE), false);

      assertEquals(2, accessControl.getRuleCount(), "Should have 2 rules");

      accessControl.clearAllRules();

      assertEquals(0, accessControl.getRuleCount(), "All rules should be cleared");
    }
  }

  @Nested
  @DisplayName("getRuleCount Tests")
  class GetRuleCountTests {

    @Test
    @DisplayName("Should return 0 initially")
    void shouldReturnZeroInitially() {
      assertEquals(0, accessControl.getRuleCount(), "Should have 0 rules initially");
    }

    @Test
    @DisplayName("Should count rules correctly")
    void shouldCountRulesCorrectly() throws IOException {
      final Path dir1 = Files.createDirectories(tempDir.resolve("dir1"));
      final Path dir2 = Files.createDirectories(tempDir.resolve("dir2"));

      accessControl.grantDirectoryPermissions(
          dir1.toString(), EnumSet.of(WasiFileOperation.READ), false);

      assertEquals(1, accessControl.getRuleCount(), "Should have 1 rule");

      accessControl.grantDirectoryPermissions(
          dir2.toString(), EnumSet.of(WasiFileOperation.WRITE), false);

      assertEquals(2, accessControl.getRuleCount(), "Should have 2 rules");
    }
  }

  @Nested
  @DisplayName("Path Validation Tests")
  class PathValidationTests {

    @Test
    @DisplayName("Should normalize paths for comparison")
    void shouldNormalizePathsForComparison() throws IOException {
      final Path subDir = Files.createDirectories(tempDir.resolve("subdir"));

      accessControl.grantDirectoryPermissions(
          subDir.toString(), EnumSet.of(WasiFileOperation.READ), false);

      // Access with different path representation
      final Path unnormalizedPath = tempDir.resolve("subdir").resolve("..").resolve("subdir");

      assertDoesNotThrow(
          () -> accessControl.validateDirectoryAccess(unnormalizedPath, WasiFileOperation.READ),
          "Should normalize paths for validation");
    }
  }
}
