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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.wasi.WasiDirectoryPermissions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Tests for {@link WasiDirectoryPermissions} directory permission model. */
@DisplayName("WasiDirectoryPermissions")
final class WasiDirectoryPermissionsTest {

  @Nested
  @DisplayName("none factory method")
  final class NonePermissionsTests {

    @Test
    @DisplayName("should deny all operations when none() is used")
    void shouldDenyAllOperations() {
      final WasiDirectoryPermissions perms = WasiDirectoryPermissions.none();
      assertFalse(perms.canRead(), "none() should deny read");
      assertFalse(perms.canWrite(), "none() should deny write");
      assertFalse(perms.canCreate(), "none() should deny create");
      assertFalse(perms.canDelete(), "none() should deny delete");
      assertFalse(perms.canList(), "none() should deny list");
      assertFalse(perms.canTraverse(), "none() should deny traverse");
      assertFalse(perms.canAccessMetadata(), "none() should deny metadata access");
    }
  }

  @Nested
  @DisplayName("readOnly factory method")
  final class ReadOnlyPermissionsTests {

    @Test
    @DisplayName("should allow read, list, traverse, and metadata")
    void shouldAllowReadOperations() {
      final WasiDirectoryPermissions perms = WasiDirectoryPermissions.readOnly();
      assertTrue(perms.canRead(), "readOnly() should allow read");
      assertTrue(perms.canList(), "readOnly() should allow list");
      assertTrue(perms.canTraverse(), "readOnly() should allow traverse");
      assertTrue(perms.canAccessMetadata(), "readOnly() should allow metadata");
    }

    @Test
    @DisplayName("should deny write, create, and delete")
    void shouldDenyWriteOperations() {
      final WasiDirectoryPermissions perms = WasiDirectoryPermissions.readOnly();
      assertFalse(perms.canWrite(), "readOnly() should deny write");
      assertFalse(perms.canCreate(), "readOnly() should deny create");
      assertFalse(perms.canDelete(), "readOnly() should deny delete");
    }
  }

  @Nested
  @DisplayName("readWrite factory method")
  final class ReadWritePermissionsTests {

    @Test
    @DisplayName("should allow all operations")
    void shouldAllowAllOperations() {
      final WasiDirectoryPermissions perms = WasiDirectoryPermissions.readWrite();
      assertTrue(perms.canRead(), "readWrite() should allow read");
      assertTrue(perms.canWrite(), "readWrite() should allow write");
      assertTrue(perms.canCreate(), "readWrite() should allow create");
      assertTrue(perms.canDelete(), "readWrite() should allow delete");
      assertTrue(perms.canList(), "readWrite() should allow list");
      assertTrue(perms.canTraverse(), "readWrite() should allow traverse");
      assertTrue(perms.canAccessMetadata(), "readWrite() should allow metadata");
    }
  }

  @Nested
  @DisplayName("full factory method")
  final class FullPermissionsTests {

    @Test
    @DisplayName("should be equal to readWrite permissions")
    void shouldEqualReadWrite() {
      final WasiDirectoryPermissions full = WasiDirectoryPermissions.full();
      final WasiDirectoryPermissions readWrite = WasiDirectoryPermissions.readWrite();
      assertEquals(full, readWrite, "full() should equal readWrite()");
    }
  }

  @Nested
  @DisplayName("builder individual permissions")
  final class BuilderIndividualPermissionsTests {

    @Test
    @DisplayName("should build with only read permission")
    void shouldBuildWithOnlyRead() {
      final WasiDirectoryPermissions perms =
          WasiDirectoryPermissions.builder().allowRead().build();
      assertTrue(perms.canRead(), "Should allow read");
      assertFalse(perms.canWrite(), "Should deny write when only read is set");
      assertFalse(perms.canCreate(), "Should deny create when only read is set");
    }

    @Test
    @DisplayName("should build with only write permission")
    void shouldBuildWithOnlyWrite() {
      final WasiDirectoryPermissions perms =
          WasiDirectoryPermissions.builder().allowWrite().build();
      assertTrue(perms.canWrite(), "Should allow write");
      assertFalse(perms.canRead(), "Should deny read when only write is set");
    }

    @Test
    @DisplayName("should build with selective permissions")
    void shouldBuildWithSelectivePermissions() {
      final WasiDirectoryPermissions perms =
          WasiDirectoryPermissions.builder()
              .allowRead()
              .allowCreate()
              .allowTraverse()
              .build();
      assertTrue(perms.canRead(), "Should allow read");
      assertTrue(perms.canCreate(), "Should allow create");
      assertTrue(perms.canTraverse(), "Should allow traverse");
      assertFalse(perms.canWrite(), "Should deny write");
      assertFalse(perms.canDelete(), "Should deny delete");
      assertFalse(perms.canList(), "Should deny list");
      assertFalse(perms.canAccessMetadata(), "Should deny metadata");
    }
  }

  @Nested
  @DisplayName("equals and hashCode")
  final class EqualsAndHashCodeTests {

    @Test
    @DisplayName("should be equal for same permissions")
    void shouldBeEqualForSamePermissions() {
      final WasiDirectoryPermissions perms1 =
          WasiDirectoryPermissions.builder().allowRead().allowWrite().build();
      final WasiDirectoryPermissions perms2 =
          WasiDirectoryPermissions.builder().allowRead().allowWrite().build();
      assertEquals(perms1, perms2, "Permissions with same flags should be equal");
      assertEquals(perms1.hashCode(), perms2.hashCode(), "Hash codes should match for equal perms");
    }

    @Test
    @DisplayName("should not be equal for different permissions")
    void shouldNotBeEqualForDifferentPermissions() {
      final WasiDirectoryPermissions perms1 =
          WasiDirectoryPermissions.builder().allowRead().build();
      final WasiDirectoryPermissions perms2 =
          WasiDirectoryPermissions.builder().allowWrite().build();
      assertNotEquals(perms1, perms2, "Permissions with different flags should not be equal");
    }

    @Test
    @DisplayName("should not be equal to null")
    void shouldNotBeEqualToNull() {
      final WasiDirectoryPermissions perms = WasiDirectoryPermissions.readOnly();
      assertNotEquals(null, perms, "Permissions should not be equal to null");
    }

    @Test
    @DisplayName("should be reflexively equal")
    void shouldBeReflexivelyEqual() {
      final WasiDirectoryPermissions perms = WasiDirectoryPermissions.readWrite();
      assertEquals(perms, perms, "Permissions should be equal to itself");
    }
  }

  @Nested
  @DisplayName("toString")
  final class ToStringTests {

    @Test
    @DisplayName("should include all permission flags in string representation")
    void shouldIncludeAllFlags() {
      final WasiDirectoryPermissions perms = WasiDirectoryPermissions.readOnly();
      final String str = perms.toString();
      assertNotNull(str, "toString should not return null");
      assertTrue(str.contains("read=true"), "toString should include read flag");
      assertTrue(str.contains("write=false"), "toString should include write flag");
      assertTrue(str.contains("list=true"), "toString should include list flag");
    }
  }
}
