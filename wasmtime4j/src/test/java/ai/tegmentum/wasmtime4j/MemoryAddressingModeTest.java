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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.memory.MemoryAddressingMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link MemoryAddressingMode}.
 *
 * <p>Verifies enum structure, constants, field accessors, is64Bit, supportsMemorySize,
 * supportsPageCount, pagesToBytes, bytesToPages, getOptimalMode, getOptimalModeForSize, and
 * toString.
 */
@DisplayName("MemoryAddressingMode Tests")
class MemoryAddressingModeTest {

  @Nested
  @DisplayName("Enum Structure Tests")
  class EnumStructureTests {

    @Test
    @DisplayName("should be an enum type")
    void shouldBeAnEnumType() {
      assertTrue(
          MemoryAddressingMode.class.isEnum(), "MemoryAddressingMode should be an enum type");
    }

    @Test
    @DisplayName("should have exactly 2 values")
    void shouldHaveExactValueCount() {
      assertEquals(
          2,
          MemoryAddressingMode.values().length,
          "MemoryAddressingMode should have exactly 2 values");
    }
  }

  @Nested
  @DisplayName("Enum Values Tests")
  class EnumValuesTests {

    @Test
    @DisplayName("should contain MEMORY32")
    void shouldContainMemory32() {
      assertNotNull(MemoryAddressingMode.MEMORY32, "MEMORY32 constant should exist");
    }

    @Test
    @DisplayName("should contain MEMORY64")
    void shouldContainMemory64() {
      assertNotNull(MemoryAddressingMode.MEMORY64, "MEMORY64 constant should exist");
    }
  }

  @Nested
  @DisplayName("GetDisplayName Tests")
  class GetDisplayNameTests {

    @Test
    @DisplayName("MEMORY32 should have display name '32-bit'")
    void memory32ShouldHaveDisplayName32Bit() {
      assertEquals(
          "32-bit",
          MemoryAddressingMode.MEMORY32.getDisplayName(),
          "MEMORY32 display name should be '32-bit'");
    }

    @Test
    @DisplayName("MEMORY64 should have display name '64-bit'")
    void memory64ShouldHaveDisplayName64Bit() {
      assertEquals(
          "64-bit",
          MemoryAddressingMode.MEMORY64.getDisplayName(),
          "MEMORY64 display name should be '64-bit'");
    }
  }

  @Nested
  @DisplayName("GetMaxMemorySize Tests")
  class GetMaxMemorySizeTests {

    @Test
    @DisplayName("MEMORY32 should have max memory size of 4GB")
    void memory32ShouldHaveMaxSize4Gb() {
      assertEquals(
          4_294_967_296L,
          MemoryAddressingMode.MEMORY32.getMaxMemorySize(),
          "MEMORY32 max memory size should be 4GB");
    }

    @Test
    @DisplayName("MEMORY64 should have max memory size of Long.MAX_VALUE")
    void memory64ShouldHaveMaxSizeLongMax() {
      assertEquals(
          Long.MAX_VALUE,
          MemoryAddressingMode.MEMORY64.getMaxMemorySize(),
          "MEMORY64 max memory size should be Long.MAX_VALUE");
    }
  }

  @Nested
  @DisplayName("GetMaxPageCount Tests")
  class GetMaxPageCountTests {

    @Test
    @DisplayName("MEMORY32 should have max page count of 65536")
    void memory32ShouldHaveMaxPageCount65536() {
      assertEquals(
          65536L,
          MemoryAddressingMode.MEMORY32.getMaxPageCount(),
          "MEMORY32 max page count should be 65536");
    }

    @Test
    @DisplayName("MEMORY64 should have large max page count")
    void memory64ShouldHaveLargeMaxPageCount() {
      assertEquals(
          Long.MAX_VALUE / 65536L,
          MemoryAddressingMode.MEMORY64.getMaxPageCount(),
          "MEMORY64 max page count should be Long.MAX_VALUE / 65536");
    }
  }

  @Nested
  @DisplayName("GetAddressType Tests")
  class GetAddressTypeTests {

    @Test
    @DisplayName("MEMORY32 should use Integer address type")
    void memory32ShouldUseIntegerAddressType() {
      assertEquals(
          Integer.class,
          MemoryAddressingMode.MEMORY32.getAddressType(),
          "MEMORY32 address type should be Integer.class");
    }

    @Test
    @DisplayName("MEMORY64 should use Long address type")
    void memory64ShouldUseLongAddressType() {
      assertEquals(
          Long.class,
          MemoryAddressingMode.MEMORY64.getAddressType(),
          "MEMORY64 address type should be Long.class");
    }
  }

  @Nested
  @DisplayName("GetPageCountType Tests")
  class GetPageCountTypeTests {

    @Test
    @DisplayName("MEMORY32 should use Integer page count type")
    void memory32ShouldUseIntegerPageCountType() {
      assertEquals(
          Integer.class,
          MemoryAddressingMode.MEMORY32.getPageCountType(),
          "MEMORY32 page count type should be Integer.class");
    }

    @Test
    @DisplayName("MEMORY64 should use Long page count type")
    void memory64ShouldUseLongPageCountType() {
      assertEquals(
          Long.class,
          MemoryAddressingMode.MEMORY64.getPageCountType(),
          "MEMORY64 page count type should be Long.class");
    }
  }

  @Nested
  @DisplayName("Is64Bit Tests")
  class Is64BitTests {

    @Test
    @DisplayName("MEMORY32 should not be 64-bit")
    void memory32ShouldNotBe64Bit() {
      assertFalse(MemoryAddressingMode.MEMORY32.is64Bit(), "MEMORY32 should not be 64-bit");
    }

    @Test
    @DisplayName("MEMORY64 should be 64-bit")
    void memory64ShouldBe64Bit() {
      assertTrue(MemoryAddressingMode.MEMORY64.is64Bit(), "MEMORY64 should be 64-bit");
    }
  }

  @Nested
  @DisplayName("SupportsMemorySize Tests")
  class SupportsMemorySizeTests {

    @Test
    @DisplayName("MEMORY32 should support size within 4GB")
    void memory32ShouldSupportSizeWithin4Gb() {
      assertTrue(
          MemoryAddressingMode.MEMORY32.supportsMemorySize(0), "MEMORY32 should support size 0");
      assertTrue(
          MemoryAddressingMode.MEMORY32.supportsMemorySize(65536),
          "MEMORY32 should support 1 page");
      assertTrue(
          MemoryAddressingMode.MEMORY32.supportsMemorySize(4_294_967_296L),
          "MEMORY32 should support max size");
    }

    @Test
    @DisplayName("MEMORY32 should not support size exceeding 4GB")
    void memory32ShouldNotSupportSizeExceeding4Gb() {
      assertFalse(
          MemoryAddressingMode.MEMORY32.supportsMemorySize(4_294_967_297L),
          "MEMORY32 should not support size > 4GB");
    }

    @Test
    @DisplayName("should not support negative size")
    void shouldNotSupportNegativeSize() {
      assertFalse(
          MemoryAddressingMode.MEMORY32.supportsMemorySize(-1), "Should not support negative size");
    }
  }

  @Nested
  @DisplayName("SupportsPageCount Tests")
  class SupportsPageCountTests {

    @Test
    @DisplayName("MEMORY32 should support page count within 65536")
    void memory32ShouldSupportPageCountWithin65536() {
      assertTrue(
          MemoryAddressingMode.MEMORY32.supportsPageCount(0), "MEMORY32 should support 0 pages");
      assertTrue(
          MemoryAddressingMode.MEMORY32.supportsPageCount(65536),
          "MEMORY32 should support max pages");
    }

    @Test
    @DisplayName("MEMORY32 should not support page count exceeding 65536")
    void memory32ShouldNotSupportPageCountExceeding65536() {
      assertFalse(
          MemoryAddressingMode.MEMORY32.supportsPageCount(65537),
          "MEMORY32 should not support > 65536 pages");
    }
  }

  @Nested
  @DisplayName("PagesToBytes Tests")
  class PagesToBytesTests {

    @Test
    @DisplayName("should convert pages to bytes correctly")
    void shouldConvertPagesToBytes() {
      assertEquals(0L, MemoryAddressingMode.MEMORY32.pagesToBytes(0), "0 pages should be 0 bytes");
      assertEquals(
          65536L, MemoryAddressingMode.MEMORY32.pagesToBytes(1), "1 page should be 65536 bytes");
      assertEquals(
          655360L,
          MemoryAddressingMode.MEMORY32.pagesToBytes(10),
          "10 pages should be 655360 bytes");
    }

    @Test
    @DisplayName("should throw for negative page count")
    void shouldThrowForNegativePageCount() {
      assertThrows(
          IllegalArgumentException.class,
          () -> MemoryAddressingMode.MEMORY32.pagesToBytes(-1),
          "pagesToBytes with negative count should throw");
    }
  }

  @Nested
  @DisplayName("BytesToPages Tests")
  class BytesToPagesTests {

    @Test
    @DisplayName("should convert bytes to pages correctly")
    void shouldConvertBytesToPages() {
      assertEquals(0L, MemoryAddressingMode.MEMORY32.bytesToPages(0), "0 bytes should be 0 pages");
      assertEquals(
          1L, MemoryAddressingMode.MEMORY32.bytesToPages(65536), "65536 bytes should be 1 page");
      assertEquals(
          10L,
          MemoryAddressingMode.MEMORY32.bytesToPages(655360),
          "655360 bytes should be 10 pages");
    }

    @Test
    @DisplayName("should throw for unaligned byte count")
    void shouldThrowForUnalignedByteCount() {
      assertThrows(
          IllegalArgumentException.class,
          () -> MemoryAddressingMode.MEMORY32.bytesToPages(100),
          "bytesToPages with unaligned size should throw");
    }
  }

  @Nested
  @DisplayName("GetOptimalMode Tests")
  class GetOptimalModeTests {

    @Test
    @DisplayName("should return MEMORY32 for small page requirements")
    void shouldReturnMemory32ForSmallPageRequirements() {
      assertEquals(
          MemoryAddressingMode.MEMORY32,
          MemoryAddressingMode.getOptimalMode(100, null),
          "100 pages should use MEMORY32");
    }

    @Test
    @DisplayName("should return MEMORY32 for max 32-bit pages")
    void shouldReturnMemory32ForMax32BitPages() {
      assertEquals(
          MemoryAddressingMode.MEMORY32,
          MemoryAddressingMode.getOptimalMode(65536, null),
          "65536 pages should use MEMORY32");
    }

    @Test
    @DisplayName("should return MEMORY64 for pages exceeding 32-bit limit")
    void shouldReturnMemory64ForLargePageRequirements() {
      assertEquals(
          MemoryAddressingMode.MEMORY64,
          MemoryAddressingMode.getOptimalMode(65537, null),
          "65537 pages should use MEMORY64");
    }
  }

  @Nested
  @DisplayName("GetOptimalModeForSize Tests")
  class GetOptimalModeForSizeTests {

    @Test
    @DisplayName("should return MEMORY32 for size within 4GB")
    void shouldReturnMemory32ForSizeWithin4Gb() {
      assertEquals(
          MemoryAddressingMode.MEMORY32,
          MemoryAddressingMode.getOptimalModeForSize(1_000_000, null),
          "1MB should use MEMORY32");
    }

    @Test
    @DisplayName("should return MEMORY64 for size exceeding 4GB")
    void shouldReturnMemory64ForSizeExceeding4Gb() {
      assertEquals(
          MemoryAddressingMode.MEMORY64,
          MemoryAddressingMode.getOptimalModeForSize(5_000_000_000L, null),
          "5GB should use MEMORY64");
    }
  }

  @Nested
  @DisplayName("ToString Tests")
  class ToStringTests {

    @Test
    @DisplayName("toString should contain display name")
    void toStringShouldContainDisplayName() {
      assertTrue(
          MemoryAddressingMode.MEMORY32.toString().contains("32-bit"),
          "MEMORY32 toString should contain '32-bit'");
      assertTrue(
          MemoryAddressingMode.MEMORY64.toString().contains("64-bit"),
          "MEMORY64 toString should contain '64-bit'");
    }
  }

  @Nested
  @DisplayName("ValueOf Tests")
  class ValueOfTests {

    @Test
    @DisplayName("should resolve all constants via valueOf")
    void shouldResolveAllConstantsViaValueOf() {
      for (final MemoryAddressingMode value : MemoryAddressingMode.values()) {
        assertEquals(
            value,
            MemoryAddressingMode.valueOf(value.name()),
            "valueOf should return " + value.name());
      }
    }

    @Test
    @DisplayName("should throw IllegalArgumentException for invalid name")
    void shouldThrowForInvalidName() {
      assertThrows(
          IllegalArgumentException.class,
          () -> MemoryAddressingMode.valueOf("INVALID_CONSTANT"),
          "valueOf with invalid name should throw IllegalArgumentException");
    }
  }
}
