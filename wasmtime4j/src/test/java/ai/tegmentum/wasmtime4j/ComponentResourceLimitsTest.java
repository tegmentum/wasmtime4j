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

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ComponentResourceLimits} interface.
 *
 * <p>ComponentResourceLimits defines resource limits for WebAssembly components.
 */
@DisplayName("ComponentResourceLimits Tests")
class ComponentResourceLimitsTest {

  @Nested
  @DisplayName("Interface Structure Tests")
  class InterfaceStructureTests {

    @Test
    @DisplayName("should be public interface")
    void shouldBePublicInterface() {
      assertTrue(
          Modifier.isPublic(ComponentResourceLimits.class.getModifiers()),
          "ComponentResourceLimits should be public");
      assertTrue(
          ComponentResourceLimits.class.isInterface(),
          "ComponentResourceLimits should be an interface");
    }
  }

  @Nested
  @DisplayName("Limit Method Tests")
  class LimitMethodTests {

    @Test
    @DisplayName("should have getMemoryLimits method")
    void shouldHaveGetMemoryLimitsMethod() throws NoSuchMethodException {
      final Method method = ComponentResourceLimits.class.getMethod("getMemoryLimits");
      assertNotNull(method, "getMemoryLimits method should exist");
      assertEquals(
          ComponentResourceLimits.MemoryLimits.class,
          method.getReturnType(),
          "Should return MemoryLimits");
    }

    @Test
    @DisplayName("should have getExecutionLimits method")
    void shouldHaveGetExecutionLimitsMethod() throws NoSuchMethodException {
      final Method method = ComponentResourceLimits.class.getMethod("getExecutionLimits");
      assertNotNull(method, "getExecutionLimits method should exist");
      assertEquals(
          ComponentResourceLimits.ExecutionLimits.class,
          method.getReturnType(),
          "Should return ExecutionLimits");
    }

    @Test
    @DisplayName("should have getIoLimits method")
    void shouldHaveGetIoLimitsMethod() throws NoSuchMethodException {
      final Method method = ComponentResourceLimits.class.getMethod("getIoLimits");
      assertNotNull(method, "getIoLimits method should exist");
      assertEquals(
          ComponentResourceLimits.IoLimits.class, method.getReturnType(), "Should return IoLimits");
    }

    @Test
    @DisplayName("should have getNetworkLimits method")
    void shouldHaveGetNetworkLimitsMethod() throws NoSuchMethodException {
      final Method method = ComponentResourceLimits.class.getMethod("getNetworkLimits");
      assertNotNull(method, "getNetworkLimits method should exist");
      assertEquals(
          ComponentResourceLimits.NetworkLimits.class,
          method.getReturnType(),
          "Should return NetworkLimits");
    }

    @Test
    @DisplayName("should have getFileSystemLimits method")
    void shouldHaveGetFileSystemLimitsMethod() throws NoSuchMethodException {
      final Method method = ComponentResourceLimits.class.getMethod("getFileSystemLimits");
      assertNotNull(method, "getFileSystemLimits method should exist");
      assertEquals(
          ComponentResourceLimits.FileSystemLimits.class,
          method.getReturnType(),
          "Should return FileSystemLimits");
    }

    @Test
    @DisplayName("should have validate method")
    void shouldHaveValidateMethod() throws NoSuchMethodException {
      final Method method =
          ComponentResourceLimits.class.getMethod(
              "validate", ComponentResourceLimits.ResourceUsage.class);
      assertNotNull(method, "validate method should exist");
      assertEquals(
          ComponentResourceLimits.ValidationResult.class,
          method.getReturnType(),
          "Should return ValidationResult");
    }
  }

  @Nested
  @DisplayName("ViolationType Enum Tests")
  class ViolationTypeEnumTests {

    @Test
    @DisplayName("ViolationType should be an enum")
    void violationTypeShouldBeAnEnum() {
      assertTrue(
          ComponentResourceLimits.ViolationType.class.isEnum(), "ViolationType should be an enum");
    }

    @Test
    @DisplayName("ViolationType should have correct values")
    void violationTypeShouldHaveCorrectValues() {
      final var values = ComponentResourceLimits.ViolationType.values();
      assertEquals(7, values.length, "Should have 7 violation types");

      assertEquals(
          ComponentResourceLimits.ViolationType.MEMORY_LIMIT,
          ComponentResourceLimits.ViolationType.valueOf("MEMORY_LIMIT"));
      assertEquals(
          ComponentResourceLimits.ViolationType.EXECUTION_TIME,
          ComponentResourceLimits.ViolationType.valueOf("EXECUTION_TIME"));
      assertEquals(
          ComponentResourceLimits.ViolationType.FUEL_LIMIT,
          ComponentResourceLimits.ViolationType.valueOf("FUEL_LIMIT"));
      assertEquals(
          ComponentResourceLimits.ViolationType.INSTRUCTION_LIMIT,
          ComponentResourceLimits.ViolationType.valueOf("INSTRUCTION_LIMIT"));
      assertEquals(
          ComponentResourceLimits.ViolationType.IO_LIMIT,
          ComponentResourceLimits.ViolationType.valueOf("IO_LIMIT"));
      assertEquals(
          ComponentResourceLimits.ViolationType.NETWORK_LIMIT,
          ComponentResourceLimits.ViolationType.valueOf("NETWORK_LIMIT"));
      assertEquals(
          ComponentResourceLimits.ViolationType.FILESYSTEM_LIMIT,
          ComponentResourceLimits.ViolationType.valueOf("FILESYSTEM_LIMIT"));
    }
  }

  @Nested
  @DisplayName("MemoryLimits Nested Interface Tests")
  class MemoryLimitsInterfaceTests {

    @Test
    @DisplayName("MemoryLimits should be an interface")
    void memoryLimitsShouldBeAnInterface() {
      assertTrue(
          ComponentResourceLimits.MemoryLimits.class.isInterface(),
          "MemoryLimits should be an interface");
    }

    @Test
    @DisplayName("MemoryLimits should have getMaxHeapSize method")
    void memoryLimitsShouldHaveGetMaxHeapSizeMethod() throws NoSuchMethodException {
      final Method method = ComponentResourceLimits.MemoryLimits.class.getMethod("getMaxHeapSize");
      assertNotNull(method, "getMaxHeapSize method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("MemoryLimits should have getMaxStackSize method")
    void memoryLimitsShouldHaveGetMaxStackSizeMethod() throws NoSuchMethodException {
      final Method method = ComponentResourceLimits.MemoryLimits.class.getMethod("getMaxStackSize");
      assertNotNull(method, "getMaxStackSize method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("MemoryLimits should have getMaxTotalMemory method")
    void memoryLimitsShouldHaveGetMaxTotalMemoryMethod() throws NoSuchMethodException {
      final Method method =
          ComponentResourceLimits.MemoryLimits.class.getMethod("getMaxTotalMemory");
      assertNotNull(method, "getMaxTotalMemory method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }
  }

  @Nested
  @DisplayName("ExecutionLimits Nested Interface Tests")
  class ExecutionLimitsInterfaceTests {

    @Test
    @DisplayName("ExecutionLimits should be an interface")
    void executionLimitsShouldBeAnInterface() {
      assertTrue(
          ComponentResourceLimits.ExecutionLimits.class.isInterface(),
          "ExecutionLimits should be an interface");
    }

    @Test
    @DisplayName("ExecutionLimits should have getMaxExecutionTime method")
    void executionLimitsShouldHaveGetMaxExecutionTimeMethod() throws NoSuchMethodException {
      final Method method =
          ComponentResourceLimits.ExecutionLimits.class.getMethod("getMaxExecutionTime");
      assertNotNull(method, "getMaxExecutionTime method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("ExecutionLimits should have getMaxFuel method")
    void executionLimitsShouldHaveGetMaxFuelMethod() throws NoSuchMethodException {
      final Method method = ComponentResourceLimits.ExecutionLimits.class.getMethod("getMaxFuel");
      assertNotNull(method, "getMaxFuel method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("ExecutionLimits should have getMaxInstructions method")
    void executionLimitsShouldHaveGetMaxInstructionsMethod() throws NoSuchMethodException {
      final Method method =
          ComponentResourceLimits.ExecutionLimits.class.getMethod("getMaxInstructions");
      assertNotNull(method, "getMaxInstructions method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }
  }

  @Nested
  @DisplayName("IoLimits Nested Interface Tests")
  class IoLimitsInterfaceTests {

    @Test
    @DisplayName("IoLimits should be an interface")
    void ioLimitsShouldBeAnInterface() {
      assertTrue(
          ComponentResourceLimits.IoLimits.class.isInterface(), "IoLimits should be an interface");
    }

    @Test
    @DisplayName("IoLimits should have getMaxReadOpsPerSecond method")
    void ioLimitsShouldHaveGetMaxReadOpsPerSecondMethod() throws NoSuchMethodException {
      final Method method =
          ComponentResourceLimits.IoLimits.class.getMethod("getMaxReadOpsPerSecond");
      assertNotNull(method, "getMaxReadOpsPerSecond method should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }

    @Test
    @DisplayName("IoLimits should have getMaxWriteOpsPerSecond method")
    void ioLimitsShouldHaveGetMaxWriteOpsPerSecondMethod() throws NoSuchMethodException {
      final Method method =
          ComponentResourceLimits.IoLimits.class.getMethod("getMaxWriteOpsPerSecond");
      assertNotNull(method, "getMaxWriteOpsPerSecond method should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }
  }

  @Nested
  @DisplayName("NetworkLimits Nested Interface Tests")
  class NetworkLimitsInterfaceTests {

    @Test
    @DisplayName("NetworkLimits should be an interface")
    void networkLimitsShouldBeAnInterface() {
      assertTrue(
          ComponentResourceLimits.NetworkLimits.class.isInterface(),
          "NetworkLimits should be an interface");
    }

    @Test
    @DisplayName("NetworkLimits should have getMaxConnections method")
    void networkLimitsShouldHaveGetMaxConnectionsMethod() throws NoSuchMethodException {
      final Method method =
          ComponentResourceLimits.NetworkLimits.class.getMethod("getMaxConnections");
      assertNotNull(method, "getMaxConnections method should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }

    @Test
    @DisplayName("NetworkLimits should have getMaxBandwidth method")
    void networkLimitsShouldHaveGetMaxBandwidthMethod() throws NoSuchMethodException {
      final Method method =
          ComponentResourceLimits.NetworkLimits.class.getMethod("getMaxBandwidth");
      assertNotNull(method, "getMaxBandwidth method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }
  }

  @Nested
  @DisplayName("FileSystemLimits Nested Interface Tests")
  class FileSystemLimitsInterfaceTests {

    @Test
    @DisplayName("FileSystemLimits should be an interface")
    void fileSystemLimitsShouldBeAnInterface() {
      assertTrue(
          ComponentResourceLimits.FileSystemLimits.class.isInterface(),
          "FileSystemLimits should be an interface");
    }

    @Test
    @DisplayName("FileSystemLimits should have getMaxOpenFiles method")
    void fileSystemLimitsShouldHaveGetMaxOpenFilesMethod() throws NoSuchMethodException {
      final Method method =
          ComponentResourceLimits.FileSystemLimits.class.getMethod("getMaxOpenFiles");
      assertNotNull(method, "getMaxOpenFiles method should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }

    @Test
    @DisplayName("FileSystemLimits should have getMaxDiskUsage method")
    void fileSystemLimitsShouldHaveGetMaxDiskUsageMethod() throws NoSuchMethodException {
      final Method method =
          ComponentResourceLimits.FileSystemLimits.class.getMethod("getMaxDiskUsage");
      assertNotNull(method, "getMaxDiskUsage method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("FileSystemLimits should have getMaxFileSize method")
    void fileSystemLimitsShouldHaveGetMaxFileSizeMethod() throws NoSuchMethodException {
      final Method method =
          ComponentResourceLimits.FileSystemLimits.class.getMethod("getMaxFileSize");
      assertNotNull(method, "getMaxFileSize method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }
  }

  @Nested
  @DisplayName("ValidationResult Nested Interface Tests")
  class ValidationResultInterfaceTests {

    @Test
    @DisplayName("ValidationResult should be an interface")
    void validationResultShouldBeAnInterface() {
      assertTrue(
          ComponentResourceLimits.ValidationResult.class.isInterface(),
          "ValidationResult should be an interface");
    }

    @Test
    @DisplayName("ValidationResult should have isValid method")
    void validationResultShouldHaveIsValidMethod() throws NoSuchMethodException {
      final Method method = ComponentResourceLimits.ValidationResult.class.getMethod("isValid");
      assertNotNull(method, "isValid method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("ValidationResult should have getViolations method")
    void validationResultShouldHaveGetViolationsMethod() throws NoSuchMethodException {
      final Method method =
          ComponentResourceLimits.ValidationResult.class.getMethod("getViolations");
      assertNotNull(method, "getViolations method should exist");
      assertEquals(java.util.List.class, method.getReturnType(), "Should return List");
    }
  }

  @Nested
  @DisplayName("LimitViolation Nested Interface Tests")
  class LimitViolationInterfaceTests {

    @Test
    @DisplayName("LimitViolation should be an interface")
    void limitViolationShouldBeAnInterface() {
      assertTrue(
          ComponentResourceLimits.LimitViolation.class.isInterface(),
          "LimitViolation should be an interface");
    }

    @Test
    @DisplayName("LimitViolation should have getType method")
    void limitViolationShouldHaveGetTypeMethod() throws NoSuchMethodException {
      final Method method = ComponentResourceLimits.LimitViolation.class.getMethod("getType");
      assertNotNull(method, "getType method should exist");
      assertEquals(
          ComponentResourceLimits.ViolationType.class,
          method.getReturnType(),
          "Should return ViolationType");
    }

    @Test
    @DisplayName("LimitViolation should have getLimitName method")
    void limitViolationShouldHaveGetLimitNameMethod() throws NoSuchMethodException {
      final Method method = ComponentResourceLimits.LimitViolation.class.getMethod("getLimitName");
      assertNotNull(method, "getLimitName method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }

    @Test
    @DisplayName("LimitViolation should have getCurrentValue method")
    void limitViolationShouldHaveGetCurrentValueMethod() throws NoSuchMethodException {
      final Method method =
          ComponentResourceLimits.LimitViolation.class.getMethod("getCurrentValue");
      assertNotNull(method, "getCurrentValue method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("LimitViolation should have getLimitValue method")
    void limitViolationShouldHaveGetLimitValueMethod() throws NoSuchMethodException {
      final Method method = ComponentResourceLimits.LimitViolation.class.getMethod("getLimitValue");
      assertNotNull(method, "getLimitValue method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("LimitViolation should have getMessage method")
    void limitViolationShouldHaveGetMessageMethod() throws NoSuchMethodException {
      final Method method = ComponentResourceLimits.LimitViolation.class.getMethod("getMessage");
      assertNotNull(method, "getMessage method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }
  }
}
