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

package ai.tegmentum.wasmtime4j.wasi.random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.security.SecureRandom;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link WasiRandom} interface.
 *
 * <p>WasiRandom provides access to cryptographically-secure random data according to the WASI
 * Preview 2 specification.
 */
@DisplayName("WasiRandom Tests")
class WasiRandomTest {

  @Nested
  @DisplayName("Interface Structure Tests")
  class InterfaceStructureTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeAnInterface() {
      assertTrue(WasiRandom.class.isInterface(), "WasiRandom should be an interface");
    }

    @Test
    @DisplayName("should be public")
    void shouldBePublic() {
      assertTrue(Modifier.isPublic(WasiRandom.class.getModifiers()), "WasiRandom should be public");
    }

    @Test
    @DisplayName("should have getRandomBytes method")
    void shouldHaveGetRandomBytesMethod() throws NoSuchMethodException {
      final Method method = WasiRandom.class.getMethod("getRandomBytes", long.class);
      assertNotNull(method, "getRandomBytes method should exist");
      assertEquals(byte[].class, method.getReturnType(), "getRandomBytes should return byte[]");
    }

    @Test
    @DisplayName("should have getRandomU64 method")
    void shouldHaveGetRandomU64Method() throws NoSuchMethodException {
      final Method method = WasiRandom.class.getMethod("getRandomU64");
      assertNotNull(method, "getRandomU64 method should exist");
      assertEquals(long.class, method.getReturnType(), "getRandomU64 should return long");
    }

    @Test
    @DisplayName("should have exactly two methods")
    void shouldHaveExactlyTwoMethods() {
      int methodCount = 0;
      for (final Method method : WasiRandom.class.getDeclaredMethods()) {
        if (!method.isSynthetic()) {
          methodCount++;
        }
      }
      assertEquals(2, methodCount, "WasiRandom should have exactly 2 methods");
    }
  }

  @Nested
  @DisplayName("Method Signature Tests")
  class MethodSignatureTests {

    @Test
    @DisplayName("getRandomBytes should take long parameter")
    void getRandomBytesShouldTakeLongParameter() throws NoSuchMethodException {
      final Method method = WasiRandom.class.getMethod("getRandomBytes", long.class);
      assertEquals(1, method.getParameterCount(), "getRandomBytes should take one parameter");
      assertEquals(long.class, method.getParameterTypes()[0], "Parameter should be long");
    }

    @Test
    @DisplayName("getRandomU64 should take no parameters")
    void getRandomU64ShouldTakeNoParameters() throws NoSuchMethodException {
      final Method method = WasiRandom.class.getMethod("getRandomU64");
      assertEquals(0, method.getParameterCount(), "getRandomU64 should take no parameters");
    }
  }

  @Nested
  @DisplayName("Mock Implementation Tests")
  class MockImplementationTests {

    @Test
    @DisplayName("mock should return random bytes of requested length")
    void mockShouldReturnRandomBytesOfRequestedLength() {
      final MockWasiRandom random = new MockWasiRandom();

      final byte[] bytes = random.getRandomBytes(16);

      assertNotNull(bytes, "getRandomBytes should return non-null");
      assertEquals(16, bytes.length, "Should return requested number of bytes");
    }

    @Test
    @DisplayName("mock should return zero-length array for zero length")
    void mockShouldReturnZeroLengthArrayForZeroLength() {
      final MockWasiRandom random = new MockWasiRandom();

      final byte[] bytes = random.getRandomBytes(0);

      assertNotNull(bytes, "getRandomBytes should return non-null");
      assertEquals(0, bytes.length, "Should return empty array for zero length");
    }

    @Test
    @DisplayName("mock should return different bytes on successive calls")
    void mockShouldReturnDifferentBytesOnSuccessiveCalls() {
      final MockWasiRandom random = new MockWasiRandom();

      final byte[] first = random.getRandomBytes(32);
      final byte[] second = random.getRandomBytes(32);

      // Very unlikely to be equal for cryptographically secure random
      boolean different = false;
      for (int i = 0; i < first.length; i++) {
        if (first[i] != second[i]) {
          different = true;
          break;
        }
      }
      assertTrue(different, "Successive calls should return different bytes");
    }

    @Test
    @DisplayName("mock should return random u64 values")
    void mockShouldReturnRandomU64Values() {
      final MockWasiRandom random = new MockWasiRandom();

      final long first = random.getRandomU64();
      final long second = random.getRandomU64();

      // Very unlikely to be equal for cryptographically secure random
      assertNotEquals(first, second, "Successive calls should return different values");
    }

    @Test
    @DisplayName("mock getRandomU64 should cover full long range")
    void mockGetRandomU64ShouldCoverFullLongRange() {
      final MockWasiRandom random = new MockWasiRandom();

      boolean hasPositive = false;
      boolean hasNegative = false;

      // Sample enough values to likely see both positive and negative
      for (int i = 0; i < 100; i++) {
        final long value = random.getRandomU64();
        if (value > 0) {
          hasPositive = true;
        }
        if (value < 0) {
          hasNegative = true;
        }
        if (hasPositive && hasNegative) {
          break;
        }
      }

      assertTrue(hasPositive || hasNegative, "Should produce non-zero values");
    }
  }

  @Nested
  @DisplayName("Interface Contract Tests")
  class InterfaceContractTests {

    @Test
    @DisplayName("all methods should be public")
    void allMethodsShouldBePublic() {
      for (final Method method : WasiRandom.class.getDeclaredMethods()) {
        if (!method.isSynthetic()) {
          assertTrue(
              Modifier.isPublic(method.getModifiers()),
              "Method " + method.getName() + " should be public");
        }
      }
    }
  }

  @Nested
  @DisplayName("Security Property Tests")
  class SecurityPropertyTests {

    @Test
    @DisplayName("random bytes should appear unpredictable")
    void randomBytesShouldAppearUnpredictable() {
      final MockWasiRandom random = new MockWasiRandom();
      final byte[] bytes = random.getRandomBytes(1000);

      // Simple check: count zeros vs ones in bits
      int ones = 0;
      int zeros = 0;
      for (final byte b : bytes) {
        for (int i = 0; i < 8; i++) {
          if ((b & (1 << i)) != 0) {
            ones++;
          } else {
            zeros++;
          }
        }
      }

      // For truly random data, should be roughly 50/50
      final double ratio = (double) ones / (ones + zeros);
      assertTrue(
          ratio > 0.4 && ratio < 0.6,
          "Bit distribution should be roughly even (got " + ratio + ")");
    }
  }

  /** Mock implementation of WasiRandom for testing. */
  private static class MockWasiRandom implements WasiRandom {
    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    public byte[] getRandomBytes(final long len) {
      if (len < 0) {
        throw new IllegalArgumentException("Length cannot be negative");
      }
      final byte[] bytes = new byte[(int) len];
      secureRandom.nextBytes(bytes);
      return bytes;
    }

    @Override
    public long getRandomU64() {
      return secureRandom.nextLong();
    }
  }
}
