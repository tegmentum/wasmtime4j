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

package ai.tegmentum.wasmtime4j.panama;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.ComponentResourceLimits;
import ai.tegmentum.wasmtime4j.ComponentResourceLimits.ExecutionLimits;
import ai.tegmentum.wasmtime4j.ComponentResourceLimits.FileSystemLimits;
import ai.tegmentum.wasmtime4j.ComponentResourceLimits.FileSystemUsage;
import ai.tegmentum.wasmtime4j.ComponentResourceLimits.IoLimits;
import ai.tegmentum.wasmtime4j.ComponentResourceLimits.IoUsage;
import ai.tegmentum.wasmtime4j.ComponentResourceLimits.LimitViolation;
import ai.tegmentum.wasmtime4j.ComponentResourceLimits.MemoryLimits;
import ai.tegmentum.wasmtime4j.ComponentResourceLimits.NetworkLimits;
import ai.tegmentum.wasmtime4j.ComponentResourceLimits.NetworkUsage;
import ai.tegmentum.wasmtime4j.ComponentResourceLimits.ResourceUsage;
import ai.tegmentum.wasmtime4j.ComponentResourceLimits.ValidationResult;
import java.util.List;
import java.util.logging.Logger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for {@link PanamaComponentResourceLimits}.
 *
 * <p>These tests exercise actual method calls to improve JaCoCo coverage.
 */
@DisplayName("PanamaComponentResourceLimits Integration Tests")
class PanamaComponentResourceLimitsTest {

  private static final Logger LOGGER =
      Logger.getLogger(PanamaComponentResourceLimitsTest.class.getName());

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("Should create instance with default constructor")
    void shouldCreateInstanceWithDefaultConstructor() {
      LOGGER.info("Testing default constructor");

      final PanamaComponentResourceLimits limits = new PanamaComponentResourceLimits();

      assertNotNull(limits, "Limits should be created");
      assertNotNull(limits.getMemoryLimits(), "Memory limits should be set");
      assertNotNull(limits.getExecutionLimits(), "Execution limits should be set");
      assertNotNull(limits.getIoLimits(), "IO limits should be set");
      assertNotNull(limits.getNetworkLimits(), "Network limits should be set");
      assertNotNull(limits.getFileSystemLimits(), "File system limits should be set");

      LOGGER.info("Default constructor creates all limit objects");
    }
  }

  @Nested
  @DisplayName("Memory Limits Tests")
  class MemoryLimitsTests {

    @Test
    @DisplayName("Should return default memory limits")
    void shouldReturnDefaultMemoryLimits() {
      LOGGER.info("Testing getMemoryLimits");

      final PanamaComponentResourceLimits limits = new PanamaComponentResourceLimits();
      final MemoryLimits memoryLimits = limits.getMemoryLimits();

      assertNotNull(memoryLimits, "Memory limits should not be null");
      assertEquals(
          Long.MAX_VALUE,
          memoryLimits.getMaxTotalMemory(),
          "Default max total memory should be MAX_VALUE");
      assertEquals(
          Long.MAX_VALUE,
          memoryLimits.getMaxHeapSize(),
          "Default max heap size should be MAX_VALUE");
      assertEquals(
          Long.MAX_VALUE,
          memoryLimits.getMaxStackSize(),
          "Default max stack size should be MAX_VALUE");

      LOGGER.info(
          "Memory limits: total="
              + memoryLimits.getMaxTotalMemory()
              + ", heap="
              + memoryLimits.getMaxHeapSize()
              + ", stack="
              + memoryLimits.getMaxStackSize());
    }
  }

  @Nested
  @DisplayName("Execution Limits Tests")
  class ExecutionLimitsTests {

    @Test
    @DisplayName("Should return default execution limits")
    void shouldReturnDefaultExecutionLimits() {
      LOGGER.info("Testing getExecutionLimits");

      final PanamaComponentResourceLimits limits = new PanamaComponentResourceLimits();
      final ExecutionLimits executionLimits = limits.getExecutionLimits();

      assertNotNull(executionLimits, "Execution limits should not be null");
      assertEquals(
          Long.MAX_VALUE,
          executionLimits.getMaxExecutionTime(),
          "Default max execution time should be MAX_VALUE");
      assertEquals(
          Long.MAX_VALUE, executionLimits.getMaxFuel(), "Default max fuel should be MAX_VALUE");
      assertEquals(
          Long.MAX_VALUE,
          executionLimits.getMaxInstructions(),
          "Default max instructions should be MAX_VALUE");

      LOGGER.info(
          "Execution limits: maxTime="
              + executionLimits.getMaxExecutionTime()
              + ", maxFuel="
              + executionLimits.getMaxFuel()
              + ", maxInstructions="
              + executionLimits.getMaxInstructions());
    }
  }

  @Nested
  @DisplayName("IO Limits Tests")
  class IoLimitsTests {

    @Test
    @DisplayName("Should return default IO limits")
    void shouldReturnDefaultIoLimits() {
      LOGGER.info("Testing getIoLimits");

      final PanamaComponentResourceLimits limits = new PanamaComponentResourceLimits();
      final IoLimits ioLimits = limits.getIoLimits();

      assertNotNull(ioLimits, "IO limits should not be null");
      assertEquals(
          Integer.MAX_VALUE,
          ioLimits.getMaxReadOpsPerSecond(),
          "Default max read ops should be MAX_VALUE");
      assertEquals(
          Integer.MAX_VALUE,
          ioLimits.getMaxWriteOpsPerSecond(),
          "Default max write ops should be MAX_VALUE");
      assertEquals(
          Long.MAX_VALUE,
          ioLimits.getMaxBytesReadPerSecond(),
          "Default max bytes read should be MAX_VALUE");
      assertEquals(
          Long.MAX_VALUE,
          ioLimits.getMaxBytesWrittenPerSecond(),
          "Default max bytes written should be MAX_VALUE");

      LOGGER.info(
          "IO limits: readOps="
              + ioLimits.getMaxReadOpsPerSecond()
              + ", writeOps="
              + ioLimits.getMaxWriteOpsPerSecond());
    }
  }

  @Nested
  @DisplayName("Network Limits Tests")
  class NetworkLimitsTests {

    @Test
    @DisplayName("Should return default network limits")
    void shouldReturnDefaultNetworkLimits() {
      LOGGER.info("Testing getNetworkLimits");

      final PanamaComponentResourceLimits limits = new PanamaComponentResourceLimits();
      final NetworkLimits networkLimits = limits.getNetworkLimits();

      assertNotNull(networkLimits, "Network limits should not be null");
      assertEquals(
          Integer.MAX_VALUE,
          networkLimits.getMaxConnections(),
          "Default max connections should be MAX_VALUE");
      assertEquals(
          Long.MAX_VALUE,
          networkLimits.getMaxBandwidth(),
          "Default max bandwidth should be MAX_VALUE");
      assertEquals(
          Integer.MAX_VALUE,
          networkLimits.getMaxRequestsPerSecond(),
          "Default max requests/sec should be MAX_VALUE");

      LOGGER.info(
          "Network limits: connections="
              + networkLimits.getMaxConnections()
              + ", bandwidth="
              + networkLimits.getMaxBandwidth());
    }
  }

  @Nested
  @DisplayName("File System Limits Tests")
  class FileSystemLimitsTests {

    @Test
    @DisplayName("Should return default file system limits")
    void shouldReturnDefaultFileSystemLimits() {
      LOGGER.info("Testing getFileSystemLimits");

      final PanamaComponentResourceLimits limits = new PanamaComponentResourceLimits();
      final FileSystemLimits fsLimits = limits.getFileSystemLimits();

      assertNotNull(fsLimits, "File system limits should not be null");
      assertEquals(
          Integer.MAX_VALUE,
          fsLimits.getMaxOpenFiles(),
          "Default max open files should be MAX_VALUE");
      assertEquals(
          Long.MAX_VALUE, fsLimits.getMaxDiskUsage(), "Default max disk usage should be MAX_VALUE");
      assertEquals(
          Long.MAX_VALUE, fsLimits.getMaxFileSize(), "Default max file size should be MAX_VALUE");

      LOGGER.info(
          "File system limits: openFiles="
              + fsLimits.getMaxOpenFiles()
              + ", maxDiskUsage="
              + fsLimits.getMaxDiskUsage());
    }
  }

  @Nested
  @DisplayName("Validation Tests")
  class ValidationTests {

    @Test
    @DisplayName("Should return valid result for null usage")
    void shouldReturnValidResultForNullUsage() {
      LOGGER.info("Testing validate with null usage");

      final PanamaComponentResourceLimits limits = new PanamaComponentResourceLimits();
      final ValidationResult result = limits.validate(null);

      assertNotNull(result, "Result should not be null");
      assertTrue(result.isValid(), "Should be valid for null usage");
      assertTrue(result.getViolations().isEmpty(), "Should have no violations");

      LOGGER.info("Null usage validation: valid=" + result.isValid());
    }

    @Test
    @DisplayName("Should return valid result when within limits")
    void shouldReturnValidResultWhenWithinLimits() {
      LOGGER.info("Testing validate with usage within limits");

      final PanamaComponentResourceLimits limits = new PanamaComponentResourceLimits();
      final ResourceUsage usage = new TestResourceUsage(1000L, 100L, 50L, 25L);
      final ValidationResult result = limits.validate(usage);

      assertNotNull(result, "Result should not be null");
      assertTrue(result.isValid(), "Should be valid when within limits");
      assertTrue(result.getViolations().isEmpty(), "Should have no violations");

      LOGGER.info("Within limits validation: valid=" + result.isValid());
    }

    @Test
    @DisplayName("Should return valid result for large values still within MAX_VALUE")
    void shouldReturnValidResultForLargeValues() {
      LOGGER.info("Testing validate with large values");

      final PanamaComponentResourceLimits limits = new PanamaComponentResourceLimits();
      // Since default limits are MAX_VALUE, even large usage won't trigger violations
      final ResourceUsage usage =
          new TestResourceUsage(
              Long.MAX_VALUE - 1, Long.MAX_VALUE - 1, Long.MAX_VALUE - 1, Long.MAX_VALUE - 1);

      final ValidationResult result = limits.validate(usage);
      assertNotNull(result, "Result should not be null");
      assertTrue(result.isValid(), "Should be valid when within (unlimited) limits");

      LOGGER.info("Large values validation: valid=" + result.isValid());
    }

    @Test
    @DisplayName("Should return violations list from result")
    void shouldReturnViolationsListFromResult() {
      LOGGER.info("Testing ValidationResult violations list");

      final PanamaComponentResourceLimits limits = new PanamaComponentResourceLimits();
      final ValidationResult result = limits.validate(null);

      final List<LimitViolation> violations = result.getViolations();
      assertNotNull(violations, "Violations list should not be null");
      assertTrue(violations.isEmpty(), "Should be empty for valid result");

      LOGGER.info("Violations list size: " + violations.size());
    }
  }

  @Nested
  @DisplayName("Interface Implementation Tests")
  class InterfaceImplementationTests {

    @Test
    @DisplayName("Should implement ComponentResourceLimits interface")
    void shouldImplementComponentResourceLimitsInterface() {
      LOGGER.info("Testing interface implementation");

      final PanamaComponentResourceLimits limits = new PanamaComponentResourceLimits();

      assertTrue(
          limits instanceof ComponentResourceLimits, "Should implement ComponentResourceLimits");

      LOGGER.info("Interface implementation verified");
    }
  }

  // Test helper class for ResourceUsage
  private static final class TestResourceUsage implements ResourceUsage {

    private final long currentMemory;
    private final long executionTime;
    private final long fuelConsumption;
    private final long instructionCount;

    TestResourceUsage(
        final long currentMemory,
        final long executionTime,
        final long fuelConsumption,
        final long instructionCount) {
      this.currentMemory = currentMemory;
      this.executionTime = executionTime;
      this.fuelConsumption = fuelConsumption;
      this.instructionCount = instructionCount;
    }

    @Override
    public long getCurrentMemoryUsage() {
      return currentMemory;
    }

    @Override
    public long getCurrentExecutionTime() {
      return executionTime;
    }

    @Override
    public long getCurrentFuelConsumption() {
      return fuelConsumption;
    }

    @Override
    public long getCurrentInstructionCount() {
      return instructionCount;
    }

    @Override
    public IoUsage getCurrentIoUsage() {
      return new TestIoUsage();
    }

    @Override
    public NetworkUsage getCurrentNetworkUsage() {
      return new TestNetworkUsage();
    }

    @Override
    public FileSystemUsage getCurrentFileSystemUsage() {
      return new TestFileSystemUsage();
    }
  }

  // Test helper class for IoUsage
  private static final class TestIoUsage implements IoUsage {

    @Override
    public long getReadOperations() {
      return 0;
    }

    @Override
    public long getWriteOperations() {
      return 0;
    }

    @Override
    public long getBytesRead() {
      return 0;
    }

    @Override
    public long getBytesWritten() {
      return 0;
    }
  }

  // Test helper class for NetworkUsage
  private static final class TestNetworkUsage implements NetworkUsage {

    @Override
    public int getActiveConnections() {
      return 0;
    }

    @Override
    public long getBandwidthUsage() {
      return 0;
    }

    @Override
    public long getRequestCount() {
      return 0;
    }
  }

  // Test helper class for FileSystemUsage
  private static final class TestFileSystemUsage implements FileSystemUsage {

    @Override
    public int getOpenFiles() {
      return 0;
    }

    @Override
    public long getDiskUsage() {
      return 0;
    }

    @Override
    public long getLargestFileSize() {
      return 0;
    }
  }
}
