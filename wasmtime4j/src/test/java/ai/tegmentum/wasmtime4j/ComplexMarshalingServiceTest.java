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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Tests for {@link ComplexMarshalingService} marshaling operations. */
@DisplayName("ComplexMarshalingService")
final class ComplexMarshalingServiceTest {

  @Nested
  @DisplayName("constructor")
  final class ConstructorTests {

    @Test
    @DisplayName("should create with default configuration")
    void shouldCreateWithDefaultConfig() {
      final ComplexMarshalingService service = new ComplexMarshalingService();
      assertNotNull(service, "Service with default config should not be null");
    }

    @Test
    @DisplayName("should create with custom configuration")
    void shouldCreateWithCustomConfig() {
      final MarshalingConfiguration config = MarshalingConfiguration.defaultConfiguration();
      final ComplexMarshalingService service = new ComplexMarshalingService(config);
      assertNotNull(service, "Service with custom config should not be null");
    }

    @Test
    @DisplayName("should reject null configuration")
    void shouldRejectNullConfig() {
      assertThrows(
          NullPointerException.class,
          () -> new ComplexMarshalingService(null),
          "Expected NullPointerException for null configuration");
    }
  }

  @Nested
  @DisplayName("estimateSerializedSize")
  final class EstimateSerializedSizeTests {

    @Test
    @DisplayName("should estimate size for Integer")
    void shouldEstimateSizeForInteger() {
      final ComplexMarshalingService service = new ComplexMarshalingService();
      final long size = service.estimateSerializedSize(42);
      assertEquals(4, size, "Integer size estimate should be 4 bytes");
    }

    @Test
    @DisplayName("should estimate size for Long")
    void shouldEstimateSizeForLong() {
      final ComplexMarshalingService service = new ComplexMarshalingService();
      final long size = service.estimateSerializedSize(42L);
      assertEquals(8, size, "Long size estimate should be 8 bytes");
    }

    @Test
    @DisplayName("should estimate size for Float")
    void shouldEstimateSizeForFloat() {
      final ComplexMarshalingService service = new ComplexMarshalingService();
      final long size = service.estimateSerializedSize(3.14f);
      assertEquals(4, size, "Float size estimate should be 4 bytes");
    }

    @Test
    @DisplayName("should estimate size for Double")
    void shouldEstimateSizeForDouble() {
      final ComplexMarshalingService service = new ComplexMarshalingService();
      final long size = service.estimateSerializedSize(3.14d);
      assertEquals(8, size, "Double size estimate should be 8 bytes");
    }

    @Test
    @DisplayName("should estimate size for String")
    void shouldEstimateSizeForString() {
      final ComplexMarshalingService service = new ComplexMarshalingService();
      final long size = service.estimateSerializedSize("hello");
      assertEquals(10, size, "String 'hello' size should be 10 (5 chars * 2 bytes approx UTF-16)");
    }

    @Test
    @DisplayName("should estimate size for byte array")
    void shouldEstimateSizeForByteArray() {
      final ComplexMarshalingService service = new ComplexMarshalingService();
      final byte[] data = new byte[100];
      final long size = service.estimateSerializedSize(data);
      assertEquals(100, size, "byte[100] size estimate should be 100 bytes");
    }

    @Test
    @DisplayName("should reject null object")
    void shouldRejectNullObject() {
      final ComplexMarshalingService service = new ComplexMarshalingService();
      assertThrows(
          NullPointerException.class,
          () -> service.estimateSerializedSize(null),
          "Expected NullPointerException for null object in estimateSerializedSize");
    }
  }

  @Nested
  @DisplayName("marshal null validation")
  final class MarshalNullValidationTests {

    @Test
    @DisplayName("should reject null object in marshal")
    void shouldRejectNullObjectInMarshal() {
      final ComplexMarshalingService service = new ComplexMarshalingService();
      assertThrows(
          NullPointerException.class,
          () -> service.marshal(null),
          "Expected NullPointerException for null object in marshal");
    }
  }

  @Nested
  @DisplayName("unmarshal null validation")
  final class UnmarshalNullValidationTests {

    @Test
    @DisplayName("should reject null marshaled data in unmarshal")
    void shouldRejectNullMarshaledData() {
      final ComplexMarshalingService service = new ComplexMarshalingService();
      assertThrows(
          NullPointerException.class,
          () -> service.unmarshal(null, String.class),
          "Expected NullPointerException for null marshaled data in unmarshal");
    }

    @Test
    @DisplayName("should reject null expected type in unmarshal")
    void shouldRejectNullExpectedType() {
      final ComplexMarshalingService service = new ComplexMarshalingService();
      final ComplexMarshalingService.MarshaledData data =
          new ComplexMarshalingService.MarshaledData(
              ComplexMarshalingService.MarshalingStrategy.VALUE_BASED, new byte[0], null);
      assertThrows(
          NullPointerException.class,
          () -> service.unmarshal(data, null),
          "Expected NullPointerException for null expected type in unmarshal");
    }
  }

  @Nested
  @DisplayName("createComplexValue null validation")
  final class CreateComplexValueNullValidationTests {

    @Test
    @DisplayName("should reject null object in createComplexValue")
    void shouldRejectNullObjectInCreateComplexValue() {
      final ComplexMarshalingService service = new ComplexMarshalingService();
      assertThrows(
          NullPointerException.class,
          () -> service.createComplexValue(null),
          "Expected NullPointerException for null object in createComplexValue");
    }
  }

  @Nested
  @DisplayName("MarshalingStrategy enum")
  final class MarshalingStrategyTests {

    @Test
    @DisplayName("should have three strategies")
    void shouldHaveThreeStrategies() {
      assertEquals(
          3,
          ComplexMarshalingService.MarshalingStrategy.values().length,
          "Should have 3 marshaling strategies: VALUE_BASED, MEMORY_BASED, HYBRID");
    }
  }

  @Nested
  @DisplayName("MarshaledData")
  final class MarshaledDataTests {

    @Test
    @DisplayName("should return correct strategy code for VALUE_BASED")
    void shouldReturnCorrectCodeForValueBased() {
      final ComplexMarshalingService.MarshaledData data =
          new ComplexMarshalingService.MarshaledData(
              ComplexMarshalingService.MarshalingStrategy.VALUE_BASED, new byte[0], null);
      assertEquals(0, data.getStrategyCode(), "VALUE_BASED strategy code should be 0");
    }

    @Test
    @DisplayName("should return correct strategy code for MEMORY_BASED")
    void shouldReturnCorrectCodeForMemoryBased() {
      final ComplexMarshalingService.MarshaledData data =
          new ComplexMarshalingService.MarshaledData(
              ComplexMarshalingService.MarshalingStrategy.MEMORY_BASED, null, null);
      assertEquals(1, data.getStrategyCode(), "MEMORY_BASED strategy code should be 1");
    }

    @Test
    @DisplayName("should return correct strategy code for HYBRID")
    void shouldReturnCorrectCodeForHybrid() {
      final ComplexMarshalingService.MarshaledData data =
          new ComplexMarshalingService.MarshaledData(
              ComplexMarshalingService.MarshalingStrategy.HYBRID, new byte[0], null);
      assertEquals(2, data.getStrategyCode(), "HYBRID strategy code should be 2");
    }

    @Test
    @DisplayName("should return strategy from MarshaledData")
    void shouldReturnStrategy() {
      final ComplexMarshalingService.MarshaledData data =
          new ComplexMarshalingService.MarshaledData(
              ComplexMarshalingService.MarshalingStrategy.VALUE_BASED, new byte[] {1, 2}, null);
      assertEquals(
          ComplexMarshalingService.MarshalingStrategy.VALUE_BASED,
          data.getStrategy(),
          "Strategy should be VALUE_BASED");
    }
  }

  @Nested
  @DisplayName("MemoryHandle")
  final class MemoryHandleTests {

    @Test
    @DisplayName("should store address and size")
    void shouldStoreAddressAndSize() {
      final ComplexMarshalingService.MemoryHandle handle =
          new ComplexMarshalingService.MemoryHandle(0x1000, 256);
      assertEquals(0x1000, handle.getAddress(), "Address should be 0x1000");
      assertEquals(256, handle.getSize(), "Size should be 256");
    }

    @Test
    @DisplayName("should create copy via copy constructor")
    void shouldCreateCopyViaCopyConstructor() {
      final ComplexMarshalingService.MemoryHandle original =
          new ComplexMarshalingService.MemoryHandle(100, 50);
      final ComplexMarshalingService.MemoryHandle copy =
          new ComplexMarshalingService.MemoryHandle(original);
      assertEquals(
          original.getAddress(), copy.getAddress(), "Copied address should match original");
      assertEquals(original.getSize(), copy.getSize(), "Copied size should match original");
    }
  }
}
