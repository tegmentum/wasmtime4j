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

package ai.tegmentum.wasmtime4j.wasi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the {@link WasiPermissionsImpl} package-private class.
 *
 * <p>Verifies constructor validation, unix permission bits, builder construction, special bits,
 * equals/hashCode, and toString behavior. Tests access WasiPermissionsImpl through the
 * WasiPermissions.of() and WasiPermissions.builder() factories.
 */
@DisplayName("WasiPermissionsImpl Tests")
class WasiPermissionsImplTest {

  @Nested
  @DisplayName("Constructor Validation Tests")
  class ConstructorValidationTests {

    @Test
    @DisplayName("should accept valid mode 0")
    void shouldAcceptModeZero() {
      final WasiPermissions perms = WasiPermissions.of(0);
      assertEquals(0, perms.getMode(), "Mode should be 0");
    }

    @Test
    @DisplayName("should accept valid mode 0644")
    void shouldAcceptMode0644() {
      final WasiPermissions perms = WasiPermissions.of(0644);
      assertEquals(0644, perms.getMode(), "Mode should be 0644");
    }

    @Test
    @DisplayName("should accept valid mode 0755")
    void shouldAcceptMode0755() {
      final WasiPermissions perms = WasiPermissions.of(0755);
      assertEquals(0755, perms.getMode(), "Mode should be 0755");
    }

    @Test
    @DisplayName("should accept valid mode 07777 (maximum)")
    void shouldAcceptMaxMode() {
      final WasiPermissions perms = WasiPermissions.of(07777);
      assertEquals(07777, perms.getMode(), "Mode should be 07777");
    }

    @Test
    @DisplayName("should throw for negative mode")
    void shouldThrowForNegativeMode() {
      assertThrows(
          IllegalArgumentException.class,
          () -> WasiPermissions.of(-1),
          "Should throw for negative mode");
    }

    @Test
    @DisplayName("should throw for mode exceeding 07777")
    void shouldThrowForExcessiveMode() {
      assertThrows(
          IllegalArgumentException.class,
          () -> WasiPermissions.of(010000),
          "Should throw for mode exceeding 07777");
    }
  }

  @Nested
  @DisplayName("Owner Permission Bit Tests")
  class OwnerPermissionBitTests {

    @Test
    @DisplayName("should detect owner read (0400)")
    void shouldDetectOwnerRead() {
      final WasiPermissions perms = WasiPermissions.of(0400);
      assertTrue(perms.isOwnerRead(), "Owner read should be set for 0400");
      assertFalse(perms.isOwnerWrite(), "Owner write should not be set for 0400");
      assertFalse(perms.isOwnerExecute(), "Owner execute should not be set for 0400");
    }

    @Test
    @DisplayName("should detect owner write (0200)")
    void shouldDetectOwnerWrite() {
      final WasiPermissions perms = WasiPermissions.of(0200);
      assertFalse(perms.isOwnerRead(), "Owner read should not be set for 0200");
      assertTrue(perms.isOwnerWrite(), "Owner write should be set for 0200");
      assertFalse(perms.isOwnerExecute(), "Owner execute should not be set for 0200");
    }

    @Test
    @DisplayName("should detect owner execute (0100)")
    void shouldDetectOwnerExecute() {
      final WasiPermissions perms = WasiPermissions.of(0100);
      assertFalse(perms.isOwnerRead(), "Owner read should not be set for 0100");
      assertFalse(perms.isOwnerWrite(), "Owner write should not be set for 0100");
      assertTrue(perms.isOwnerExecute(), "Owner execute should be set for 0100");
    }

    @Test
    @DisplayName("should detect all owner permissions (0700)")
    void shouldDetectAllOwnerPermissions() {
      final WasiPermissions perms = WasiPermissions.of(0700);
      assertTrue(perms.isOwnerRead(), "Owner read should be set for 0700");
      assertTrue(perms.isOwnerWrite(), "Owner write should be set for 0700");
      assertTrue(perms.isOwnerExecute(), "Owner execute should be set for 0700");
    }
  }

  @Nested
  @DisplayName("Group Permission Bit Tests")
  class GroupPermissionBitTests {

    @Test
    @DisplayName("should detect group read (0040)")
    void shouldDetectGroupRead() {
      final WasiPermissions perms = WasiPermissions.of(0040);
      assertTrue(perms.isGroupRead(), "Group read should be set for 0040");
      assertFalse(perms.isGroupWrite(), "Group write should not be set for 0040");
      assertFalse(perms.isGroupExecute(), "Group execute should not be set for 0040");
    }

    @Test
    @DisplayName("should detect group write (0020)")
    void shouldDetectGroupWrite() {
      final WasiPermissions perms = WasiPermissions.of(0020);
      assertTrue(perms.isGroupWrite(), "Group write should be set for 0020");
    }

    @Test
    @DisplayName("should detect group execute (0010)")
    void shouldDetectGroupExecute() {
      final WasiPermissions perms = WasiPermissions.of(0010);
      assertTrue(perms.isGroupExecute(), "Group execute should be set for 0010");
    }
  }

  @Nested
  @DisplayName("Other Permission Bit Tests")
  class OtherPermissionBitTests {

    @Test
    @DisplayName("should detect other read (0004)")
    void shouldDetectOtherRead() {
      final WasiPermissions perms = WasiPermissions.of(0004);
      assertTrue(perms.isOtherRead(), "Other read should be set for 0004");
      assertFalse(perms.isOtherWrite(), "Other write should not be set for 0004");
      assertFalse(perms.isOtherExecute(), "Other execute should not be set for 0004");
    }

    @Test
    @DisplayName("should detect other write (0002)")
    void shouldDetectOtherWrite() {
      final WasiPermissions perms = WasiPermissions.of(0002);
      assertTrue(perms.isOtherWrite(), "Other write should be set for 0002");
    }

    @Test
    @DisplayName("should detect other execute (0001)")
    void shouldDetectOtherExecute() {
      final WasiPermissions perms = WasiPermissions.of(0001);
      assertTrue(perms.isOtherExecute(), "Other execute should be set for 0001");
    }
  }

  @Nested
  @DisplayName("Special Bit Tests")
  class SpecialBitTests {

    @Test
    @DisplayName("should detect setuid (04000)")
    void shouldDetectSetuid() {
      final WasiPermissions perms = WasiPermissions.of(04000);
      assertTrue(perms.isSetuid(), "Setuid should be set for 04000");
      assertFalse(perms.isSetgid(), "Setgid should not be set for 04000");
      assertFalse(perms.isSticky(), "Sticky should not be set for 04000");
    }

    @Test
    @DisplayName("should detect setgid (02000)")
    void shouldDetectSetgid() {
      final WasiPermissions perms = WasiPermissions.of(02000);
      assertFalse(perms.isSetuid(), "Setuid should not be set for 02000");
      assertTrue(perms.isSetgid(), "Setgid should be set for 02000");
      assertFalse(perms.isSticky(), "Sticky should not be set for 02000");
    }

    @Test
    @DisplayName("should detect sticky (01000)")
    void shouldDetectSticky() {
      final WasiPermissions perms = WasiPermissions.of(01000);
      assertFalse(perms.isSetuid(), "Setuid should not be set for 01000");
      assertFalse(perms.isSetgid(), "Setgid should not be set for 01000");
      assertTrue(perms.isSticky(), "Sticky should be set for 01000");
    }

    @Test
    @DisplayName("should detect all special bits (07000)")
    void shouldDetectAllSpecialBits() {
      final WasiPermissions perms = WasiPermissions.of(07000);
      assertTrue(perms.isSetuid(), "Setuid should be set for 07000");
      assertTrue(perms.isSetgid(), "Setgid should be set for 07000");
      assertTrue(perms.isSticky(), "Sticky should be set for 07000");
    }
  }

  @Nested
  @DisplayName("Builder Tests")
  class BuilderTests {

    @Test
    @DisplayName("builder should create permissions with individual bits")
    void builderShouldCreatePermissions() {
      final WasiPermissions perms =
          WasiPermissions.builder()
              .ownerRead(true)
              .ownerWrite(true)
              .groupRead(true)
              .otherRead(true)
              .build();
      assertEquals(0644, perms.getMode(), "Builder should create mode 0644");
    }

    @Test
    @DisplayName("builder should support mode() shortcut")
    void builderShouldSupportModeShortcut() {
      final WasiPermissions perms = WasiPermissions.builder().mode(0755).build();
      assertEquals(0755, perms.getMode(), "Builder mode() should set 0755");
      assertTrue(perms.isOwnerRead(), "Owner read should be set");
      assertTrue(perms.isOwnerWrite(), "Owner write should be set");
      assertTrue(perms.isOwnerExecute(), "Owner execute should be set");
      assertTrue(perms.isGroupRead(), "Group read should be set");
      assertTrue(perms.isGroupExecute(), "Group execute should be set");
      assertTrue(perms.isOtherRead(), "Other read should be set");
      assertTrue(perms.isOtherExecute(), "Other execute should be set");
    }

    @Test
    @DisplayName("builder should support special bits")
    void builderShouldSupportSpecialBits() {
      final WasiPermissions perms =
          WasiPermissions.builder().setuid(true).setgid(true).sticky(true).build();
      assertTrue(perms.isSetuid(), "Setuid should be set via builder");
      assertTrue(perms.isSetgid(), "Setgid should be set via builder");
      assertTrue(perms.isSticky(), "Sticky should be set via builder");
    }

    @Test
    @DisplayName("builder should toggle bits off")
    void builderShouldToggleBitsOff() {
      final WasiPermissions perms =
          WasiPermissions.builder().ownerRead(true).ownerRead(false).build();
      assertFalse(perms.isOwnerRead(), "Owner read should be toggled off");
    }

    @Test
    @DisplayName("builder mode() should throw for invalid mode")
    void builderModeShouldThrowForInvalidMode() {
      assertThrows(
          IllegalArgumentException.class,
          () -> WasiPermissions.builder().mode(-1),
          "Builder mode() should throw for negative mode");
      assertThrows(
          IllegalArgumentException.class,
          () -> WasiPermissions.builder().mode(010000),
          "Builder mode() should throw for mode exceeding 07777");
    }
  }

  @Nested
  @DisplayName("Common Permission Patterns Tests")
  class CommonPermissionPatternsTests {

    @Test
    @DisplayName("0644 should have correct bit pattern (rw-r--r--)")
    void mode0644ShouldHaveCorrectBitPattern() {
      final WasiPermissions perms = WasiPermissions.of(0644);
      assertTrue(perms.isOwnerRead(), "Owner read should be set");
      assertTrue(perms.isOwnerWrite(), "Owner write should be set");
      assertFalse(perms.isOwnerExecute(), "Owner execute should not be set");
      assertTrue(perms.isGroupRead(), "Group read should be set");
      assertFalse(perms.isGroupWrite(), "Group write should not be set");
      assertFalse(perms.isGroupExecute(), "Group execute should not be set");
      assertTrue(perms.isOtherRead(), "Other read should be set");
      assertFalse(perms.isOtherWrite(), "Other write should not be set");
      assertFalse(perms.isOtherExecute(), "Other execute should not be set");
    }

    @Test
    @DisplayName("0 should have no permissions")
    void modeZeroShouldHaveNoPermissions() {
      final WasiPermissions perms = WasiPermissions.of(0);
      assertFalse(perms.isOwnerRead(), "Owner read should not be set");
      assertFalse(perms.isOwnerWrite(), "Owner write should not be set");
      assertFalse(perms.isOwnerExecute(), "Owner execute should not be set");
      assertFalse(perms.isGroupRead(), "Group read should not be set");
      assertFalse(perms.isGroupWrite(), "Group write should not be set");
      assertFalse(perms.isGroupExecute(), "Group execute should not be set");
      assertFalse(perms.isOtherRead(), "Other read should not be set");
      assertFalse(perms.isOtherWrite(), "Other write should not be set");
      assertFalse(perms.isOtherExecute(), "Other execute should not be set");
    }
  }

  @Nested
  @DisplayName("Equals and HashCode Tests")
  class EqualsAndHashCodeTests {

    @Test
    @DisplayName("permissions with same mode should be equal")
    void sameModeShouldBeEqual() {
      final WasiPermissions perms1 = WasiPermissions.of(0755);
      final WasiPermissions perms2 = WasiPermissions.of(0755);
      assertEquals(perms1, perms2, "Permissions with same mode should be equal");
      assertEquals(
          perms1.hashCode(),
          perms2.hashCode(),
          "Permissions with same mode should have same hashCode");
    }

    @Test
    @DisplayName("permissions with different mode should not be equal")
    void differentModeShouldNotBeEqual() {
      final WasiPermissions perms1 = WasiPermissions.of(0644);
      final WasiPermissions perms2 = WasiPermissions.of(0755);
      assertNotEquals(perms1, perms2, "Permissions with different modes should not be equal");
    }

    @Test
    @DisplayName("should not equal null")
    void shouldNotEqualNull() {
      final WasiPermissions perms = WasiPermissions.of(0644);
      assertNotEquals(null, perms, "Permissions should not equal null");
    }
  }

  @Nested
  @DisplayName("ToString Tests")
  class ToStringTests {

    @Test
    @DisplayName("toString should contain octal mode")
    void toStringShouldContainOctalMode() {
      final WasiPermissions perms = WasiPermissions.of(0755);
      final String result = perms.toString();
      assertTrue(
          result.contains("755"), "toString should contain octal mode representation: " + result);
    }

    @Test
    @DisplayName("toString should contain WasiPermissions")
    void toStringShouldContainClassName() {
      final WasiPermissions perms = WasiPermissions.of(0644);
      final String result = perms.toString();
      assertTrue(
          result.contains("WasiPermissions"),
          "toString should contain class identifier: " + result);
    }
  }

  @Nested
  @DisplayName("Default Permission Factory Tests")
  class DefaultPermissionFactoryTests {

    @Test
    @DisplayName("defaultFilePermissions should return 0644")
    void defaultFilePermissionsShouldReturn0644() {
      final WasiPermissions perms = WasiPermissions.defaultFilePermissions();
      assertEquals(0644, perms.getMode(), "Default file permissions should be 0644");
    }

    @Test
    @DisplayName("defaultDirectoryPermissions should return 0755")
    void defaultDirectoryPermissionsShouldReturn0755() {
      final WasiPermissions perms = WasiPermissions.defaultDirectoryPermissions();
      assertEquals(0755, perms.getMode(), "Default directory permissions should be 0755");
    }
  }
}
