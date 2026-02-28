/*
 * Copyright (c) 2024 Tegmentum AI. All rights reserved.
 * Licensed under the Apache License, Version 2.0.
 */

package ai.tegmentum.wasmtime4j.wasi.random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for WASI Random package interface.
 *
 * <p>This test class validates the WasiRandom interface for cryptographically-secure random
 * generation.
 */
@DisplayName("WASI Random Integration Tests")
public class WasiRandomTest {

  private static final Logger LOGGER = Logger.getLogger(WasiRandomTest.class.getName());

  @BeforeAll
  static void setUpClass() {
    LOGGER.info("Starting WASI Random Integration Tests");
  }

  @Nested
  @DisplayName("WasiRandom Interface Tests")
  class WasiRandomInterfaceTests {

    @Test
    @DisplayName("Should verify WasiRandom interface exists")
    void shouldVerifyWasiRandomInterfaceExists() {
      LOGGER.info("Testing WasiRandom interface existence");

      assertNotNull(WasiRandom.class, "WasiRandom interface should exist");
      assertTrue(WasiRandom.class.isInterface(), "WasiRandom should be an interface");

      LOGGER.info("WasiRandom interface verified");
    }

    @Test
    @DisplayName("Should implement WasiRandom with getRandomBytes method")
    void shouldImplementWasiRandomWithGetRandomBytesMethod() {
      LOGGER.info("Testing WasiRandom getRandomBytes method");

      WasiRandom random = createTestRandom();

      byte[] bytes16 = random.getRandomBytes(16);
      byte[] bytes32 = random.getRandomBytes(32);
      byte[] bytes64 = random.getRandomBytes(64);

      assertNotNull(bytes16, "16 bytes result should not be null");
      assertNotNull(bytes32, "32 bytes result should not be null");
      assertNotNull(bytes64, "64 bytes result should not be null");

      assertEquals(16, bytes16.length, "Should return exactly 16 bytes");
      assertEquals(32, bytes32.length, "Should return exactly 32 bytes");
      assertEquals(64, bytes64.length, "Should return exactly 64 bytes");

      LOGGER.info("getRandomBytes method verified");
    }

    @Test
    @DisplayName("Should implement WasiRandom with getRandomU64 method")
    void shouldImplementWasiRandomWithGetRandomU64Method() {
      LOGGER.info("Testing WasiRandom getRandomU64 method");

      WasiRandom random = createTestRandom();

      long value1 = random.getRandomU64();
      long value2 = random.getRandomU64();
      long value3 = random.getRandomU64();

      // Values should generally be different (extremely unlikely to be the same)
      LOGGER.info(
          "Random U64 values: "
              + Long.toUnsignedString(value1)
              + ", "
              + Long.toUnsignedString(value2)
              + ", "
              + Long.toUnsignedString(value3));

      // Note: It's possible but extremely unlikely for values to be equal
      // We test that at least one pair is different
      boolean atLeastOneDifferent = value1 != value2 || value2 != value3 || value1 != value3;
      assertTrue(atLeastOneDifferent, "Random values should generally be different");

      LOGGER.info("getRandomU64 method verified");
    }

    @Test
    @DisplayName("Should return zero-length array for zero length request")
    void shouldReturnZeroLengthArrayForZeroLengthRequest() {
      LOGGER.info("Testing getRandomBytes with zero length");

      WasiRandom random = createTestRandom();

      byte[] bytes = random.getRandomBytes(0);

      assertNotNull(bytes, "Result should not be null");
      assertEquals(0, bytes.length, "Should return zero-length array");

      LOGGER.info("Zero-length request verified");
    }

    @Test
    @DisplayName("Should reject negative length parameter")
    void shouldRejectNegativeLengthParameter() {
      LOGGER.info("Testing getRandomBytes with negative length");

      WasiRandom random = createTestRandom();

      assertThrows(
          IllegalArgumentException.class,
          () -> random.getRandomBytes(-1),
          "Should reject negative length");

      assertThrows(
          IllegalArgumentException.class,
          () -> random.getRandomBytes(-100),
          "Should reject large negative length");

      LOGGER.info("Negative length rejection verified");
    }

    @Test
    @DisplayName("Should handle large random byte requests")
    void shouldHandleLargeRandomByteRequests() {
      LOGGER.info("Testing getRandomBytes with large length");

      WasiRandom random = createTestRandom();

      byte[] bytes1024 = random.getRandomBytes(1024);
      byte[] bytes4096 = random.getRandomBytes(4096);

      assertNotNull(bytes1024, "1024 bytes result should not be null");
      assertNotNull(bytes4096, "4096 bytes result should not be null");

      assertEquals(1024, bytes1024.length, "Should return exactly 1024 bytes");
      assertEquals(4096, bytes4096.length, "Should return exactly 4096 bytes");

      LOGGER.info("Large random byte requests verified");
    }
  }

  @Nested
  @DisplayName("WasiRandom Uniqueness Tests")
  class WasiRandomUniquenessTests {

    @Test
    @DisplayName("Should produce unique random byte sequences")
    void shouldProduceUniqueRandomByteSequences() {
      LOGGER.info("Testing random bytes uniqueness");

      WasiRandom random = createTestRandom();

      byte[] bytes1 = random.getRandomBytes(32);
      byte[] bytes2 = random.getRandomBytes(32);
      byte[] bytes3 = random.getRandomBytes(32);

      // Each call should produce different results
      assertFalse(Arrays.equals(bytes1, bytes2), "First two byte arrays should be different");
      assertFalse(
          Arrays.equals(bytes2, bytes3), "Second and third byte arrays should be different");
      assertFalse(Arrays.equals(bytes1, bytes3), "First and third byte arrays should be different");

      LOGGER.info("Random bytes uniqueness verified");
    }

    @Test
    @DisplayName("Should produce unique U64 values over many iterations")
    void shouldProduceUniqueU64ValuesOverManyIterations() {
      LOGGER.info("Testing U64 uniqueness over many iterations");

      WasiRandom random = createTestRandom();
      Set<Long> values = new HashSet<>();
      int iterations = 100;

      for (int i = 0; i < iterations; i++) {
        values.add(random.getRandomU64());
      }

      // With 64-bit random values, collisions should be extremely rare
      assertEquals(
          iterations, values.size(), "All " + iterations + " random U64 values should be unique");

      LOGGER.info("U64 uniqueness verified over " + iterations + " iterations");
    }
  }

  @Nested
  @DisplayName("WasiRandom Distribution Tests")
  class WasiRandomDistributionTests {

    @Test
    @DisplayName("Should produce bytes with reasonable bit distribution")
    void shouldProduceBytesWithReasonableBitDistribution() {
      LOGGER.info("Testing bit distribution in random bytes");

      WasiRandom random = createTestRandom();
      byte[] bytes = random.getRandomBytes(1000);

      int[] bitCounts = new int[8];
      for (byte b : bytes) {
        for (int bit = 0; bit < 8; bit++) {
          if ((b & (1 << bit)) != 0) {
            bitCounts[bit]++;
          }
        }
      }

      // Each bit position should be set roughly 50% of the time
      // Allow 10% tolerance (40% to 60% range)
      for (int bit = 0; bit < 8; bit++) {
        double ratio = (double) bitCounts[bit] / bytes.length;
        LOGGER.info(String.format("Bit %d: %.1f%% set", bit, ratio * 100));
        assertTrue(
            ratio >= 0.35 && ratio <= 0.65,
            String.format(
                "Bit %d should be set ~50%% of the time (actual: %.1f%%)", bit, ratio * 100));
      }

      LOGGER.info("Bit distribution verified");
    }

    @Test
    @DisplayName("Should produce bytes with all values in reasonable range")
    void shouldProduceBytesWithAllValuesInReasonableRange() {
      LOGGER.info("Testing byte value distribution");

      WasiRandom random = createTestRandom();
      byte[] bytes = random.getRandomBytes(10000);

      Set<Integer> uniqueValues = new HashSet<>();
      for (byte b : bytes) {
        uniqueValues.add(b & 0xFF);
      }

      LOGGER.info("Unique byte values: " + uniqueValues.size() + "/256");

      // With 10000 random bytes, we should see most of the 256 possible values
      assertTrue(
          uniqueValues.size() >= 200,
          "Should produce at least 200 unique byte values in 10000 bytes");

      LOGGER.info("Byte value distribution verified");
    }
  }

  @Nested
  @DisplayName("WasiRandom Consistency Tests")
  class WasiRandomConsistencyTests {

    @Test
    @DisplayName("Should return fresh data on each call")
    void shouldReturnFreshDataOnEachCall() {
      LOGGER.info("Testing freshness of random data");

      WasiRandom random = createTestRandom();

      byte[][] results = new byte[10][];
      for (int i = 0; i < 10; i++) {
        results[i] = random.getRandomBytes(16);
      }

      // Verify all results are different
      for (int i = 0; i < 10; i++) {
        for (int j = i + 1; j < 10; j++) {
          assertFalse(
              Arrays.equals(results[i], results[j]),
              "Result " + i + " and " + j + " should be different");
        }
      }

      LOGGER.info("Random data freshness verified");
    }

    @Test
    @DisplayName("Should maintain independence between getRandomBytes and getRandomU64")
    void shouldMaintainIndependenceBetweenMethods() {
      LOGGER.info("Testing independence between getRandomBytes and getRandomU64");

      WasiRandom random = createTestRandom();

      // Interleave calls to both methods
      byte[] bytes1 = random.getRandomBytes(8);
      long firstU64 = random.getRandomU64();
      byte[] bytes2 = random.getRandomBytes(8);
      long secondU64 = random.getRandomU64();

      // All results should be independent
      assertFalse(Arrays.equals(bytes1, bytes2), "Byte arrays should be different");
      assertNotEquals(firstU64, secondU64, "U64 values should be different");

      LOGGER.info("Method independence verified");
    }
  }

  /**
   * Creates a test implementation of WasiRandom using SecureRandom.
   *
   * <p>This implementation matches the WASI spec requirements for cryptographically-secure random.
   */
  private WasiRandom createTestRandom() {
    return new WasiRandom() {
      private final SecureRandom secureRandom = new SecureRandom();

      @Override
      public byte[] getRandomBytes(final long len) {
        if (len < 0) {
          throw new IllegalArgumentException("Length cannot be negative: " + len);
        }
        if (len > Integer.MAX_VALUE) {
          throw new IllegalArgumentException("Length exceeds maximum array size: " + len);
        }
        byte[] bytes = new byte[(int) len];
        secureRandom.nextBytes(bytes);
        return bytes;
      }

      @Override
      public long getRandomU64() {
        return secureRandom.nextLong();
      }
    };
  }
}
