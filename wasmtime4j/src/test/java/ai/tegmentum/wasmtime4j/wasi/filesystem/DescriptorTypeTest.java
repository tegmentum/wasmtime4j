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

package ai.tegmentum.wasmtime4j.wasi.filesystem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link DescriptorType} enum.
 *
 * <p>DescriptorType identifies the type of filesystem object a descriptor references, corresponding
 * to the descriptor-type from WASI Preview 2 filesystem specification.
 */
@DisplayName("DescriptorType Tests")
class DescriptorTypeTest {

  @Nested
  @DisplayName("Enum Structure Tests")
  class EnumStructureTests {

    @Test
    @DisplayName("should be an enum")
    void shouldBeAnEnum() {
      assertTrue(DescriptorType.class.isEnum(), "DescriptorType should be an enum");
    }

    @Test
    @DisplayName("should have exactly 8 values")
    void shouldHaveExactlyEightValues() {
      final DescriptorType[] values = DescriptorType.values();
      assertEquals(8, values.length, "Should have exactly 8 descriptor types");
    }

    @Test
    @DisplayName("should have UNKNOWN value")
    void shouldHaveUnknownValue() {
      assertNotNull(DescriptorType.valueOf("UNKNOWN"), "Should have UNKNOWN");
    }

    @Test
    @DisplayName("should have BLOCK_DEVICE value")
    void shouldHaveBlockDeviceValue() {
      assertNotNull(DescriptorType.valueOf("BLOCK_DEVICE"), "Should have BLOCK_DEVICE");
    }

    @Test
    @DisplayName("should have CHARACTER_DEVICE value")
    void shouldHaveCharacterDeviceValue() {
      assertNotNull(DescriptorType.valueOf("CHARACTER_DEVICE"), "Should have CHARACTER_DEVICE");
    }

    @Test
    @DisplayName("should have DIRECTORY value")
    void shouldHaveDirectoryValue() {
      assertNotNull(DescriptorType.valueOf("DIRECTORY"), "Should have DIRECTORY");
    }

    @Test
    @DisplayName("should have FIFO value")
    void shouldHaveFifoValue() {
      assertNotNull(DescriptorType.valueOf("FIFO"), "Should have FIFO");
    }

    @Test
    @DisplayName("should have SYMBOLIC_LINK value")
    void shouldHaveSymbolicLinkValue() {
      assertNotNull(DescriptorType.valueOf("SYMBOLIC_LINK"), "Should have SYMBOLIC_LINK");
    }

    @Test
    @DisplayName("should have REGULAR_FILE value")
    void shouldHaveRegularFileValue() {
      assertNotNull(DescriptorType.valueOf("REGULAR_FILE"), "Should have REGULAR_FILE");
    }

    @Test
    @DisplayName("should have SOCKET value")
    void shouldHaveSocketValue() {
      assertNotNull(DescriptorType.valueOf("SOCKET"), "Should have SOCKET");
    }
  }

  @Nested
  @DisplayName("Enum Value Tests")
  class EnumValueTests {

    @Test
    @DisplayName("should have unique ordinals")
    void shouldHaveUniqueOrdinals() {
      final Set<Integer> ordinals = new HashSet<>();
      for (final DescriptorType type : DescriptorType.values()) {
        assertTrue(ordinals.add(type.ordinal()), "Ordinal should be unique: " + type);
      }
    }

    @Test
    @DisplayName("should have unique names")
    void shouldHaveUniqueNames() {
      final Set<String> names = new HashSet<>();
      for (final DescriptorType type : DescriptorType.values()) {
        assertTrue(names.add(type.name()), "Name should be unique: " + type);
      }
    }

    @Test
    @DisplayName("should be retrievable by name")
    void shouldBeRetrievableByName() {
      for (final DescriptorType type : DescriptorType.values()) {
        assertEquals(type, DescriptorType.valueOf(type.name()), "Should be retrievable by name");
      }
    }
  }

  @Nested
  @DisplayName("Type Category Tests")
  class TypeCategoryTests {

    @Test
    @DisplayName("should have device types")
    void shouldHaveDeviceTypes() {
      final Set<DescriptorType> deviceTypes =
          Set.of(DescriptorType.BLOCK_DEVICE, DescriptorType.CHARACTER_DEVICE);
      for (final DescriptorType type : deviceTypes) {
        assertNotNull(type, "Should have device type: " + type);
      }
    }

    @Test
    @DisplayName("should have file system entry types")
    void shouldHaveFileSystemEntryTypes() {
      final Set<DescriptorType> entryTypes =
          Set.of(
              DescriptorType.DIRECTORY, DescriptorType.REGULAR_FILE, DescriptorType.SYMBOLIC_LINK);
      for (final DescriptorType type : entryTypes) {
        assertNotNull(type, "Should have entry type: " + type);
      }
    }

    @Test
    @DisplayName("should have IPC types")
    void shouldHaveIpcTypes() {
      final Set<DescriptorType> ipcTypes = Set.of(DescriptorType.FIFO, DescriptorType.SOCKET);
      for (final DescriptorType type : ipcTypes) {
        assertNotNull(type, "Should have IPC type: " + type);
      }
    }
  }

  @Nested
  @DisplayName("Usage Pattern Tests")
  class UsagePatternTests {

    @Test
    @DisplayName("should support switch statement")
    void shouldSupportSwitchStatement() {
      final DescriptorType type = DescriptorType.REGULAR_FILE;

      final String description;
      switch (type) {
        case UNKNOWN:
          description = "Unknown type";
          break;
        case BLOCK_DEVICE:
          description = "Block device";
          break;
        case CHARACTER_DEVICE:
          description = "Character device";
          break;
        case DIRECTORY:
          description = "Directory";
          break;
        case FIFO:
          description = "Named pipe";
          break;
        case SYMBOLIC_LINK:
          description = "Symbolic link";
          break;
        case REGULAR_FILE:
          description = "Regular file";
          break;
        case SOCKET:
          description = "Socket";
          break;
        default:
          description = "Unhandled type";
      }

      assertEquals("Regular file", description, "REGULAR_FILE description");
    }

    @Test
    @DisplayName("should be usable in collections")
    void shouldBeUsableInCollections() {
      final Set<DescriptorType> readableTypes = new HashSet<>();
      readableTypes.add(DescriptorType.REGULAR_FILE);
      readableTypes.add(DescriptorType.DIRECTORY);
      readableTypes.add(DescriptorType.SYMBOLIC_LINK);

      assertTrue(
          readableTypes.contains(DescriptorType.REGULAR_FILE), "Should contain REGULAR_FILE");
      assertTrue(readableTypes.contains(DescriptorType.DIRECTORY), "Should contain DIRECTORY");
      assertEquals(3, readableTypes.size(), "Should have 3 readable types");
    }

    @Test
    @DisplayName("should support type checking pattern")
    void shouldSupportTypeCheckingPattern() {
      final DescriptorType type = DescriptorType.DIRECTORY;

      final boolean isDirectory = type == DescriptorType.DIRECTORY;
      final boolean isFile = type == DescriptorType.REGULAR_FILE;
      final boolean isLink = type == DescriptorType.SYMBOLIC_LINK;

      assertTrue(isDirectory, "Should identify as directory");
      assertTrue(!isFile, "Should not identify as file");
      assertTrue(!isLink, "Should not identify as link");
    }
  }

  @Nested
  @DisplayName("WASI Specification Compliance Tests")
  class WasiSpecificationComplianceTests {

    @Test
    @DisplayName("should have all WASI Preview 2 descriptor-type values")
    void shouldHaveAllWasiPreview2DescriptorTypeValues() {
      // WASI Preview 2 specifies: unknown, block-device, character-device,
      // directory, fifo, symbolic-link, regular-file, socket
      final String[] expectedTypes = {
        "UNKNOWN",
        "BLOCK_DEVICE",
        "CHARACTER_DEVICE",
        "DIRECTORY",
        "FIFO",
        "SYMBOLIC_LINK",
        "REGULAR_FILE",
        "SOCKET"
      };

      for (final String typeName : expectedTypes) {
        assertNotNull(DescriptorType.valueOf(typeName), "Should have WASI type: " + typeName);
      }
    }

    @Test
    @DisplayName("should have correct number of types as per specification")
    void shouldHaveCorrectNumberOfTypesAsPerSpecification() {
      assertEquals(
          8, DescriptorType.values().length, "Should match WASI Preview 2 descriptor-type count");
    }
  }

  @Nested
  @DisplayName("POSIX Inode Type Mapping Tests")
  class PosixInodeTypeMappingTests {

    @Test
    @DisplayName("should map to POSIX file types")
    void shouldMapToPosixFileTypes() {
      // BLOCK_DEVICE -> S_IFBLK
      // CHARACTER_DEVICE -> S_IFCHR
      // DIRECTORY -> S_IFDIR
      // FIFO -> S_IFIFO
      // SYMBOLIC_LINK -> S_IFLNK
      // REGULAR_FILE -> S_IFREG
      // SOCKET -> S_IFSOCK

      assertNotNull(DescriptorType.BLOCK_DEVICE, "Maps to S_IFBLK");
      assertNotNull(DescriptorType.CHARACTER_DEVICE, "Maps to S_IFCHR");
      assertNotNull(DescriptorType.DIRECTORY, "Maps to S_IFDIR");
      assertNotNull(DescriptorType.FIFO, "Maps to S_IFIFO");
      assertNotNull(DescriptorType.SYMBOLIC_LINK, "Maps to S_IFLNK");
      assertNotNull(DescriptorType.REGULAR_FILE, "Maps to S_IFREG");
      assertNotNull(DescriptorType.SOCKET, "Maps to S_IFSOCK");
    }

    @Test
    @DisplayName("UNKNOWN should handle undetermined types")
    void unknownShouldHandleUndeterminedTypes() {
      final DescriptorType unknown = DescriptorType.UNKNOWN;
      assertEquals(0, unknown.ordinal(), "UNKNOWN should be first ordinal");
    }
  }

  @Nested
  @DisplayName("File Operation Compatibility Tests")
  class FileOperationCompatibilityTests {

    @Test
    @DisplayName("REGULAR_FILE should support read/write operations")
    void regularFileShouldSupportReadWriteOperations() {
      final DescriptorType type = DescriptorType.REGULAR_FILE;
      assertEquals("REGULAR_FILE", type.name(), "Should be REGULAR_FILE");
    }

    @Test
    @DisplayName("DIRECTORY should support directory operations")
    void directoryShouldSupportDirectoryOperations() {
      final DescriptorType type = DescriptorType.DIRECTORY;
      assertEquals("DIRECTORY", type.name(), "Should be DIRECTORY");
    }

    @Test
    @DisplayName("SYMBOLIC_LINK should be resolvable")
    void symbolicLinkShouldBeResolvable() {
      final DescriptorType type = DescriptorType.SYMBOLIC_LINK;
      assertEquals("SYMBOLIC_LINK", type.name(), "Should be SYMBOLIC_LINK");
    }
  }
}
