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
import ai.tegmentum.wasmtime4j.jni.wasi.exception.WasiException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Comprehensive tests for {@link WasiExperimentalProcess}. */
@DisplayName("WasiExperimentalProcess Tests")
class WasiExperimentalProcessTest {

  private WasiContext testContext;
  private ExecutorService executorService;
  private WasiExperimentalProcess experimentalProcess;

  @BeforeEach
  void setUp() {
    testContext = TestWasiContextFactory.createTestContext();
    executorService = Executors.newSingleThreadExecutor();
    experimentalProcess = new WasiExperimentalProcess(testContext, executorService);
  }

  @AfterEach
  void tearDown() {
    if (experimentalProcess != null) {
      experimentalProcess.close();
    }
    if (executorService != null) {
      executorService.shutdown();
    }
  }

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("WasiExperimentalProcess should be final class")
    void shouldBeFinalClass() {
      assertTrue(
          java.lang.reflect.Modifier.isFinal(WasiExperimentalProcess.class.getModifiers()),
          "WasiExperimentalProcess should be final");
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("Constructor should throw on null context")
    void constructorShouldThrowOnNullContext() {
      assertThrows(
          JniException.class,
          () -> new WasiExperimentalProcess(null, executorService),
          "Should throw on null context");
    }

    @Test
    @DisplayName("Constructor should throw on null executor")
    void constructorShouldThrowOnNullExecutor() {
      assertThrows(
          JniException.class,
          () -> new WasiExperimentalProcess(testContext, null),
          "Should throw on null executor");
    }

    @Test
    @DisplayName("Constructor should create handler with valid parameters")
    void constructorShouldCreateHandlerWithValidParameters() {
      final WasiExperimentalProcess process =
          new WasiExperimentalProcess(testContext, executorService);
      assertNotNull(process, "Handler should be created");
      process.close();
    }
  }

  @Nested
  @DisplayName("createSandboxedProcessAsync Tests")
  class CreateSandboxedProcessAsyncTests {

    @Test
    @DisplayName("Should throw on null executable")
    void shouldThrowOnNullExecutable() {
      final List<String> args = Collections.emptyList();
      final Map<String, String> env = Collections.emptyMap();
      final WasiExperimentalProcess.SandboxConfig config =
          WasiExperimentalProcess.SandboxConfig.minimal();
      final WasiExperimentalProcess.ProcessResourceLimits limits =
          WasiExperimentalProcess.ProcessResourceLimits.defaultLimits();

      assertThrows(
          JniException.class,
          () -> experimentalProcess.createSandboxedProcessAsync(null, args, env, config, limits),
          "Should throw on null executable");
    }

    @Test
    @DisplayName("Should throw on empty executable")
    void shouldThrowOnEmptyExecutable() {
      final List<String> args = Collections.emptyList();
      final Map<String, String> env = Collections.emptyMap();
      final WasiExperimentalProcess.SandboxConfig config =
          WasiExperimentalProcess.SandboxConfig.minimal();
      final WasiExperimentalProcess.ProcessResourceLimits limits =
          WasiExperimentalProcess.ProcessResourceLimits.defaultLimits();

      assertThrows(
          JniException.class,
          () -> experimentalProcess.createSandboxedProcessAsync("", args, env, config, limits),
          "Should throw on empty executable");
    }

    @Test
    @DisplayName("Should throw on null arguments")
    void shouldThrowOnNullArguments() {
      final Map<String, String> env = Collections.emptyMap();
      final WasiExperimentalProcess.SandboxConfig config =
          WasiExperimentalProcess.SandboxConfig.minimal();
      final WasiExperimentalProcess.ProcessResourceLimits limits =
          WasiExperimentalProcess.ProcessResourceLimits.defaultLimits();

      assertThrows(
          JniException.class,
          () ->
              experimentalProcess.createSandboxedProcessAsync(
                  "/bin/test", null, env, config, limits),
          "Should throw on null arguments");
    }

    @Test
    @DisplayName("Should throw on null environment")
    void shouldThrowOnNullEnvironment() {
      final List<String> args = Collections.emptyList();
      final WasiExperimentalProcess.SandboxConfig config =
          WasiExperimentalProcess.SandboxConfig.minimal();
      final WasiExperimentalProcess.ProcessResourceLimits limits =
          WasiExperimentalProcess.ProcessResourceLimits.defaultLimits();

      assertThrows(
          JniException.class,
          () ->
              experimentalProcess.createSandboxedProcessAsync(
                  "/bin/test", args, null, config, limits),
          "Should throw on null environment");
    }

    @Test
    @DisplayName("Should throw on null sandbox config")
    void shouldThrowOnNullSandboxConfig() {
      final List<String> args = Collections.emptyList();
      final Map<String, String> env = Collections.emptyMap();
      final WasiExperimentalProcess.ProcessResourceLimits limits =
          WasiExperimentalProcess.ProcessResourceLimits.defaultLimits();

      assertThrows(
          JniException.class,
          () ->
              experimentalProcess.createSandboxedProcessAsync("/bin/test", args, env, null, limits),
          "Should throw on null sandbox config");
    }

    @Test
    @DisplayName("Should throw on null resource limits")
    void shouldThrowOnNullResourceLimits() {
      final List<String> args = Collections.emptyList();
      final Map<String, String> env = Collections.emptyMap();
      final WasiExperimentalProcess.SandboxConfig config =
          WasiExperimentalProcess.SandboxConfig.minimal();

      assertThrows(
          JniException.class,
          () ->
              experimentalProcess.createSandboxedProcessAsync("/bin/test", args, env, config, null),
          "Should throw on null resource limits");
    }

    @Test
    @DisplayName("Should return CompletableFuture")
    void shouldReturnCompletableFuture() {
      final List<String> args = Collections.emptyList();
      final Map<String, String> env = Collections.emptyMap();
      final WasiExperimentalProcess.SandboxConfig config =
          WasiExperimentalProcess.SandboxConfig.minimal();
      final WasiExperimentalProcess.ProcessResourceLimits limits =
          WasiExperimentalProcess.ProcessResourceLimits.defaultLimits();

      final CompletableFuture<Long> future =
          experimentalProcess.createSandboxedProcessAsync("/bin/test", args, env, config, limits);

      assertNotNull(future, "Future should not be null");
      // Cancel to cleanup
      future.cancel(true);
    }
  }

  @Nested
  @DisplayName("createResourceMonitorAsync Tests")
  class CreateResourceMonitorAsyncTests {

    @Test
    @DisplayName("Should throw on null monitoring config")
    void shouldThrowOnNullMonitoringConfig() {
      final Consumer<WasiExperimentalProcess.ResourceUsageAlert> callback = alert -> {};

      assertThrows(
          JniException.class,
          () -> experimentalProcess.createResourceMonitorAsync(1L, null, callback),
          "Should throw on null monitoring config");
    }

    @Test
    @DisplayName("Should throw on null callback")
    void shouldThrowOnNullCallback() {
      final WasiExperimentalProcess.ResourceMonitoringConfig config =
          new WasiExperimentalProcess.ResourceMonitoringConfig(
              60, 100_000_000, 50, 1000, 1000, true);

      assertThrows(
          JniException.class,
          () -> experimentalProcess.createResourceMonitorAsync(1L, config, null),
          "Should throw on null callback");
    }

    @Test
    @DisplayName("Should throw on invalid process handle")
    void shouldThrowOnInvalidProcessHandle() {
      final WasiExperimentalProcess.ResourceMonitoringConfig config =
          new WasiExperimentalProcess.ResourceMonitoringConfig(
              60, 100_000_000, 50, 1000, 1000, true);
      final Consumer<WasiExperimentalProcess.ResourceUsageAlert> callback = alert -> {};

      assertThrows(
          WasiException.class,
          () -> experimentalProcess.createResourceMonitorAsync(999L, config, callback),
          "Should throw on invalid process handle");
    }
  }

  @Nested
  @DisplayName("createIpcChannelAsync Tests")
  class CreateIpcChannelAsyncTests {

    @Test
    @DisplayName("Should throw on null channel type")
    void shouldThrowOnNullChannelType() {
      final WasiExperimentalProcess.IpcChannelConfig config =
          WasiExperimentalProcess.IpcChannelConfig.defaultConfig();

      assertThrows(
          JniException.class,
          () -> experimentalProcess.createIpcChannelAsync(1L, 2L, null, config),
          "Should throw on null channel type");
    }

    @Test
    @DisplayName("Should throw on null channel config")
    void shouldThrowOnNullChannelConfig() {
      assertThrows(
          JniException.class,
          () ->
              experimentalProcess.createIpcChannelAsync(
                  1L, 2L, WasiExperimentalProcess.IpcChannelType.PIPE, null),
          "Should throw on null channel config");
    }

    @Test
    @DisplayName("Should throw on invalid source process")
    void shouldThrowOnInvalidSourceProcess() {
      final WasiExperimentalProcess.IpcChannelConfig config =
          WasiExperimentalProcess.IpcChannelConfig.defaultConfig();

      assertThrows(
          WasiException.class,
          () ->
              experimentalProcess.createIpcChannelAsync(
                  999L, 2L, WasiExperimentalProcess.IpcChannelType.PIPE, config),
          "Should throw on invalid source process");
    }
  }

  @Nested
  @DisplayName("registerSystemServiceAsync Tests")
  class RegisterSystemServiceAsyncTests {

    @Test
    @DisplayName("Should throw on null service name")
    void shouldThrowOnNullServiceName() {
      final WasiExperimentalProcess.SystemServiceMetadata metadata =
          new WasiExperimentalProcess.SystemServiceMetadata(
              "1.0", "Test service", Collections.emptyList(), Collections.emptyList(), 8080, false);
      final Consumer<WasiExperimentalProcess.ServiceRequest> handler = req -> {};

      assertThrows(
          JniException.class,
          () -> experimentalProcess.registerSystemServiceAsync(null, metadata, handler),
          "Should throw on null service name");
    }

    @Test
    @DisplayName("Should throw on empty service name")
    void shouldThrowOnEmptyServiceName() {
      final WasiExperimentalProcess.SystemServiceMetadata metadata =
          new WasiExperimentalProcess.SystemServiceMetadata(
              "1.0", "Test service", Collections.emptyList(), Collections.emptyList(), 8080, false);
      final Consumer<WasiExperimentalProcess.ServiceRequest> handler = req -> {};

      assertThrows(
          JniException.class,
          () -> experimentalProcess.registerSystemServiceAsync("", metadata, handler),
          "Should throw on empty service name");
    }

    @Test
    @DisplayName("Should throw on null metadata")
    void shouldThrowOnNullMetadata() {
      final Consumer<WasiExperimentalProcess.ServiceRequest> handler = req -> {};

      assertThrows(
          JniException.class,
          () -> experimentalProcess.registerSystemServiceAsync("test-service", null, handler),
          "Should throw on null metadata");
    }

    @Test
    @DisplayName("Should throw on null handler")
    void shouldThrowOnNullHandler() {
      final WasiExperimentalProcess.SystemServiceMetadata metadata =
          new WasiExperimentalProcess.SystemServiceMetadata(
              "1.0", "Test service", Collections.emptyList(), Collections.emptyList(), 8080, false);

      assertThrows(
          JniException.class,
          () -> experimentalProcess.registerSystemServiceAsync("test-service", metadata, null),
          "Should throw on null handler");
    }

    @Test
    @DisplayName("Should return CompletableFuture")
    void shouldReturnCompletableFuture() {
      final WasiExperimentalProcess.SystemServiceMetadata metadata =
          new WasiExperimentalProcess.SystemServiceMetadata(
              "1.0", "Test service", Collections.emptyList(), Collections.emptyList(), 8080, false);
      final Consumer<WasiExperimentalProcess.ServiceRequest> handler = req -> {};

      final CompletableFuture<String> future =
          experimentalProcess.registerSystemServiceAsync("test-service", metadata, handler);

      assertNotNull(future, "Future should not be null");
      // Cancel to cleanup
      future.cancel(true);
    }
  }

  @Nested
  @DisplayName("discoverSystemServicesAsync Tests")
  class DiscoverSystemServicesAsyncTests {

    @Test
    @DisplayName("Should throw on null service pattern")
    void shouldThrowOnNullServicePattern() {
      assertThrows(
          JniException.class,
          () -> experimentalProcess.discoverSystemServicesAsync(null, Collections.emptyList()),
          "Should throw on null service pattern");
    }

    @Test
    @DisplayName("Should throw on empty service pattern")
    void shouldThrowOnEmptyServicePattern() {
      assertThrows(
          JniException.class,
          () -> experimentalProcess.discoverSystemServicesAsync("", Collections.emptyList()),
          "Should throw on empty service pattern");
    }

    @Test
    @DisplayName("Should throw on null required capabilities")
    void shouldThrowOnNullRequiredCapabilities() {
      assertThrows(
          JniException.class,
          () -> experimentalProcess.discoverSystemServicesAsync("test-*", null),
          "Should throw on null required capabilities");
    }

    @Test
    @DisplayName("Should return CompletableFuture")
    void shouldReturnCompletableFuture() {
      final CompletableFuture<List<WasiExperimentalProcess.SystemServiceInfo>> future =
          experimentalProcess.discoverSystemServicesAsync("test-*", Collections.emptyList());

      assertNotNull(future, "Future should not be null");
      // Cancel to cleanup
      future.cancel(true);
    }
  }

  @Nested
  @DisplayName("getProcessResourceUsageAsync Tests")
  class GetProcessResourceUsageAsyncTests {

    @Test
    @DisplayName("Should throw on invalid process handle")
    void shouldThrowOnInvalidProcessHandle() {
      assertThrows(
          WasiException.class,
          () -> experimentalProcess.getProcessResourceUsageAsync(999L),
          "Should throw on invalid process handle");
    }
  }

  @Nested
  @DisplayName("terminateProcessAsync Tests")
  class TerminateProcessAsyncTests {

    @Test
    @DisplayName("Should throw on invalid process handle")
    void shouldThrowOnInvalidProcessHandle() {
      assertThrows(
          WasiException.class,
          () -> experimentalProcess.terminateProcessAsync(999L, 5),
          "Should throw on invalid process handle");
    }
  }

  @Nested
  @DisplayName("close Tests")
  class CloseTests {

    @Test
    @DisplayName("Should close without error")
    void shouldCloseWithoutError() {
      final WasiExperimentalProcess process =
          new WasiExperimentalProcess(testContext, executorService);
      assertDoesNotThrow(process::close, "Should close without error");
    }

    @Test
    @DisplayName("Should handle multiple close calls")
    void shouldHandleMultipleCloseCalls() {
      final WasiExperimentalProcess process =
          new WasiExperimentalProcess(testContext, executorService);
      assertDoesNotThrow(process::close, "First close should succeed");
      assertDoesNotThrow(process::close, "Second close should succeed");
    }
  }

  @Nested
  @DisplayName("ProcessState Enum Tests")
  class ProcessStateEnumTests {

    @Test
    @DisplayName("Should have expected values")
    void shouldHaveExpectedValues() {
      final WasiExperimentalProcess.ProcessState[] values =
          WasiExperimentalProcess.ProcessState.values();
      assertEquals(4, values.length, "Should have 4 values");
      assertNotNull(WasiExperimentalProcess.ProcessState.CREATED, "CREATED should exist");
      assertNotNull(WasiExperimentalProcess.ProcessState.RUNNING, "RUNNING should exist");
      assertNotNull(WasiExperimentalProcess.ProcessState.SUSPENDED, "SUSPENDED should exist");
      assertNotNull(WasiExperimentalProcess.ProcessState.TERMINATED, "TERMINATED should exist");
    }

    @Test
    @DisplayName("Should support valueOf")
    void shouldSupportValueOf() {
      assertEquals(
          WasiExperimentalProcess.ProcessState.RUNNING,
          WasiExperimentalProcess.ProcessState.valueOf("RUNNING"),
          "valueOf should work for RUNNING");
    }
  }

  @Nested
  @DisplayName("SandboxType Enum Tests")
  class SandboxTypeEnumTests {

    @Test
    @DisplayName("Should have expected values")
    void shouldHaveExpectedValues() {
      final WasiExperimentalProcess.SandboxType[] values =
          WasiExperimentalProcess.SandboxType.values();
      assertEquals(4, values.length, "Should have 4 values");
      assertNotNull(WasiExperimentalProcess.SandboxType.MINIMAL, "MINIMAL should exist");
      assertNotNull(WasiExperimentalProcess.SandboxType.STANDARD, "STANDARD should exist");
      assertNotNull(WasiExperimentalProcess.SandboxType.STRICT, "STRICT should exist");
      assertNotNull(WasiExperimentalProcess.SandboxType.CONTAINER, "CONTAINER should exist");
    }
  }

  @Nested
  @DisplayName("IpcChannelType Enum Tests")
  class IpcChannelTypeEnumTests {

    @Test
    @DisplayName("Should have expected values")
    void shouldHaveExpectedValues() {
      final WasiExperimentalProcess.IpcChannelType[] values =
          WasiExperimentalProcess.IpcChannelType.values();
      assertEquals(4, values.length, "Should have 4 values");
      assertNotNull(WasiExperimentalProcess.IpcChannelType.PIPE, "PIPE should exist");
      assertNotNull(
          WasiExperimentalProcess.IpcChannelType.SHARED_MEMORY, "SHARED_MEMORY should exist");
      assertNotNull(
          WasiExperimentalProcess.IpcChannelType.MESSAGE_QUEUE, "MESSAGE_QUEUE should exist");
      assertNotNull(WasiExperimentalProcess.IpcChannelType.SOCKET, "SOCKET should exist");
    }
  }

  @Nested
  @DisplayName("IpcState Enum Tests")
  class IpcStateEnumTests {

    @Test
    @DisplayName("Should have expected values")
    void shouldHaveExpectedValues() {
      final WasiExperimentalProcess.IpcState[] values = WasiExperimentalProcess.IpcState.values();
      assertEquals(4, values.length, "Should have 4 values");
      assertNotNull(WasiExperimentalProcess.IpcState.CONNECTING, "CONNECTING should exist");
      assertNotNull(WasiExperimentalProcess.IpcState.CONNECTED, "CONNECTED should exist");
      assertNotNull(WasiExperimentalProcess.IpcState.DISCONNECTED, "DISCONNECTED should exist");
      assertNotNull(WasiExperimentalProcess.IpcState.ERROR, "ERROR should exist");
    }
  }

  @Nested
  @DisplayName("ServiceState Enum Tests")
  class ServiceStateEnumTests {

    @Test
    @DisplayName("Should have expected values")
    void shouldHaveExpectedValues() {
      final WasiExperimentalProcess.ServiceState[] values =
          WasiExperimentalProcess.ServiceState.values();
      assertEquals(4, values.length, "Should have 4 values");
      assertNotNull(WasiExperimentalProcess.ServiceState.REGISTERING, "REGISTERING should exist");
      assertNotNull(WasiExperimentalProcess.ServiceState.ACTIVE, "ACTIVE should exist");
      assertNotNull(WasiExperimentalProcess.ServiceState.INACTIVE, "INACTIVE should exist");
      assertNotNull(WasiExperimentalProcess.ServiceState.DISCOVERED, "DISCOVERED should exist");
    }
  }

  @Nested
  @DisplayName("SandboxCapabilities Tests")
  class SandboxCapabilitiesTests {

    @Test
    @DisplayName("Should have expected static values")
    void shouldHaveExpectedStaticValues() {
      assertNotNull(WasiExperimentalProcess.SandboxCapabilities.NONE, "NONE should exist");
      assertNotNull(
          WasiExperimentalProcess.SandboxCapabilities.FILE_READ, "FILE_READ should exist");
      assertNotNull(
          WasiExperimentalProcess.SandboxCapabilities.FILE_WRITE, "FILE_WRITE should exist");
      assertNotNull(
          WasiExperimentalProcess.SandboxCapabilities.NETWORK_CLIENT,
          "NETWORK_CLIENT should exist");
      assertNotNull(
          WasiExperimentalProcess.SandboxCapabilities.NETWORK_SERVER,
          "NETWORK_SERVER should exist");
      assertNotNull(
          WasiExperimentalProcess.SandboxCapabilities.PROCESS_SPAWN, "PROCESS_SPAWN should exist");
      assertNotNull(
          WasiExperimentalProcess.SandboxCapabilities.SYSTEM_INFO, "SYSTEM_INFO should exist");
    }

    @Test
    @DisplayName("Should have expected values")
    void shouldHaveExpectedValues() {
      assertEquals(0x00, WasiExperimentalProcess.SandboxCapabilities.NONE.value, "NONE value");
      assertEquals(
          0x01, WasiExperimentalProcess.SandboxCapabilities.FILE_READ.value, "FILE_READ value");
      assertEquals(
          0x02, WasiExperimentalProcess.SandboxCapabilities.FILE_WRITE.value, "FILE_WRITE value");
      assertEquals(
          0x04,
          WasiExperimentalProcess.SandboxCapabilities.NETWORK_CLIENT.value,
          "NETWORK_CLIENT value");
      assertEquals(
          0x08,
          WasiExperimentalProcess.SandboxCapabilities.NETWORK_SERVER.value,
          "NETWORK_SERVER value");
      assertEquals(
          0x10,
          WasiExperimentalProcess.SandboxCapabilities.PROCESS_SPAWN.value,
          "PROCESS_SPAWN value");
      assertEquals(
          0x20, WasiExperimentalProcess.SandboxCapabilities.SYSTEM_INFO.value, "SYSTEM_INFO value");
    }

    @Test
    @DisplayName("Should combine capabilities")
    void shouldCombineCapabilities() {
      final WasiExperimentalProcess.SandboxCapabilities combined =
          WasiExperimentalProcess.SandboxCapabilities.FILE_READ.combine(
              WasiExperimentalProcess.SandboxCapabilities.FILE_WRITE);
      assertEquals(0x03, combined.value, "Combined value should be 0x03");
    }
  }

  @Nested
  @DisplayName("SandboxConfig Tests")
  class SandboxConfigTests {

    @Test
    @DisplayName("Should create minimal config")
    void shouldCreateMinimalConfig() {
      final WasiExperimentalProcess.SandboxConfig config =
          WasiExperimentalProcess.SandboxConfig.minimal();
      assertNotNull(config, "Config should not be null");
      assertEquals(
          WasiExperimentalProcess.SandboxType.MINIMAL,
          config.sandboxType,
          "Should be MINIMAL type");
      assertFalse(config.allowNetworking, "Should not allow networking");
      assertFalse(config.allowFileSystemAccess, "Should not allow file system access");
      assertFalse(config.allowProcessControl, "Should not allow process control");
    }

    @Test
    @DisplayName("Should create standard config")
    void shouldCreateStandardConfig() {
      final WasiExperimentalProcess.SandboxConfig config =
          WasiExperimentalProcess.SandboxConfig.standard();
      assertNotNull(config, "Config should not be null");
      assertEquals(
          WasiExperimentalProcess.SandboxType.STANDARD,
          config.sandboxType,
          "Should be STANDARD type");
      assertFalse(config.allowNetworking, "Should not allow networking");
      assertTrue(config.allowFileSystemAccess, "Should allow file system access");
      assertFalse(config.allowProcessControl, "Should not allow process control");
    }

    @Test
    @DisplayName("Should create custom config")
    void shouldCreateCustomConfig() {
      final WasiExperimentalProcess.SandboxConfig config =
          new WasiExperimentalProcess.SandboxConfig(
              WasiExperimentalProcess.SandboxType.STRICT,
              WasiExperimentalProcess.SandboxCapabilities.FILE_READ,
              true,
              true,
              true);
      assertNotNull(config, "Config should not be null");
      assertEquals(
          WasiExperimentalProcess.SandboxType.STRICT, config.sandboxType, "Should be STRICT type");
      assertTrue(config.allowNetworking, "Should allow networking");
      assertTrue(config.allowFileSystemAccess, "Should allow file system access");
      assertTrue(config.allowProcessControl, "Should allow process control");
    }
  }

  @Nested
  @DisplayName("ProcessResourceLimits Tests")
  class ProcessResourceLimitsTests {

    @Test
    @DisplayName("Should create default limits")
    void shouldCreateDefaultLimits() {
      final WasiExperimentalProcess.ProcessResourceLimits limits =
          WasiExperimentalProcess.ProcessResourceLimits.defaultLimits();
      assertNotNull(limits, "Limits should not be null");
      assertEquals(512 * 1024 * 1024, limits.maxMemoryBytes, "Default max memory");
      assertEquals(50, limits.maxCpuPercent, "Default max CPU");
      assertEquals(256, limits.maxFileDescriptors, "Default max file descriptors");
      assertEquals(10, limits.maxProcesses, "Default max processes");
      assertEquals(300, limits.timeoutSeconds, "Default timeout");
    }

    @Test
    @DisplayName("Should create custom limits")
    void shouldCreateCustomLimits() {
      final WasiExperimentalProcess.ProcessResourceLimits limits =
          new WasiExperimentalProcess.ProcessResourceLimits(1024 * 1024, 75, 100, 5, 60);
      assertEquals(1024 * 1024, limits.maxMemoryBytes, "Custom max memory");
      assertEquals(75, limits.maxCpuPercent, "Custom max CPU");
      assertEquals(100, limits.maxFileDescriptors, "Custom max file descriptors");
      assertEquals(5, limits.maxProcesses, "Custom max processes");
      assertEquals(60, limits.timeoutSeconds, "Custom timeout");
    }

    @Test
    @DisplayName("Should clamp negative values to zero or minimum")
    void shouldClampNegativeValues() {
      final WasiExperimentalProcess.ProcessResourceLimits limits =
          new WasiExperimentalProcess.ProcessResourceLimits(-100, -50, -10, -5, -60);
      assertEquals(0, limits.maxMemoryBytes, "Negative memory should be clamped to 0");
      assertEquals(1, limits.maxCpuPercent, "Negative CPU should be clamped to 1");
      assertEquals(
          1, limits.maxFileDescriptors, "Negative file descriptors should be clamped to 1");
      assertEquals(1, limits.maxProcesses, "Negative processes should be clamped to 1");
      assertEquals(0, limits.timeoutSeconds, "Negative timeout should be clamped to 0");
    }

    @Test
    @DisplayName("Should clamp CPU to 100")
    void shouldClampCpuTo100() {
      final WasiExperimentalProcess.ProcessResourceLimits limits =
          new WasiExperimentalProcess.ProcessResourceLimits(1024 * 1024, 150, 100, 5, 60);
      assertEquals(100, limits.maxCpuPercent, "CPU should be clamped to 100");
    }
  }

  @Nested
  @DisplayName("ResourceMonitoringConfig Tests")
  class ResourceMonitoringConfigTests {

    @Test
    @DisplayName("Should create config with valid values")
    void shouldCreateConfigWithValidValues() {
      final WasiExperimentalProcess.ResourceMonitoringConfig config =
          new WasiExperimentalProcess.ResourceMonitoringConfig(
              60, 1_000_000_000, 80, 10_000_000, 5_000_000, true);

      assertEquals(60, config.intervalSeconds, "Interval should match");
      assertEquals(1_000_000_000, config.memoryThresholdBytes, "Memory threshold should match");
      assertEquals(80, config.cpuThresholdPercent, "CPU threshold should match");
      assertEquals(10_000_000, config.ioThresholdBytesPerSecond, "I/O threshold should match");
      assertEquals(
          5_000_000, config.networkThresholdBytesPerSecond, "Network threshold should match");
      assertTrue(config.enableDetailedStats, "Detailed stats should be enabled");
    }

    @Test
    @DisplayName("Should clamp interval to valid range")
    void shouldClampIntervalToValidRange() {
      final WasiExperimentalProcess.ResourceMonitoringConfig configLow =
          new WasiExperimentalProcess.ResourceMonitoringConfig(0, 0, 0, 0, 0, false);
      assertEquals(1, configLow.intervalSeconds, "Interval should be clamped to minimum 1");

      final WasiExperimentalProcess.ResourceMonitoringConfig configHigh =
          new WasiExperimentalProcess.ResourceMonitoringConfig(4000, 0, 0, 0, 0, false);
      assertEquals(3600, configHigh.intervalSeconds, "Interval should be clamped to max 3600");
    }
  }

  @Nested
  @DisplayName("IpcChannelConfig Tests")
  class IpcChannelConfigTests {

    @Test
    @DisplayName("Should create default config")
    void shouldCreateDefaultConfig() {
      final WasiExperimentalProcess.IpcChannelConfig config =
          WasiExperimentalProcess.IpcChannelConfig.defaultConfig();
      assertNotNull(config, "Config should not be null");
      assertEquals(64 * 1024, config.bufferSize, "Default buffer size");
      assertEquals(1000, config.maxMessages, "Default max messages");
      assertTrue(config.bidirectional, "Default bidirectional");
      assertFalse(config.persistent, "Default not persistent");
    }

    @Test
    @DisplayName("Should create custom config")
    void shouldCreateCustomConfig() {
      final WasiExperimentalProcess.IpcChannelConfig config =
          new WasiExperimentalProcess.IpcChannelConfig(128 * 1024, 500, false, true);
      assertEquals(128 * 1024, config.bufferSize, "Custom buffer size");
      assertEquals(500, config.maxMessages, "Custom max messages");
      assertFalse(config.bidirectional, "Custom bidirectional");
      assertTrue(config.persistent, "Custom persistent");
    }

    @Test
    @DisplayName("Should clamp small values")
    void shouldClampSmallValues() {
      final WasiExperimentalProcess.IpcChannelConfig config =
          new WasiExperimentalProcess.IpcChannelConfig(100, 0, true, false);
      assertEquals(1024, config.bufferSize, "Buffer size should be clamped to minimum 1024");
      assertEquals(1, config.maxMessages, "Max messages should be clamped to minimum 1");
    }
  }

  @Nested
  @DisplayName("SystemServiceMetadata Tests")
  class SystemServiceMetadataTests {

    @Test
    @DisplayName("Should create metadata with values")
    void shouldCreateMetadataWithValues() {
      final List<String> capabilities = Arrays.asList("read", "write");
      final List<String> endpoints = Arrays.asList("/api/v1", "/api/v2");

      final WasiExperimentalProcess.SystemServiceMetadata metadata =
          new WasiExperimentalProcess.SystemServiceMetadata(
              "1.0.0", "Test service", capabilities, endpoints, 8080, true);

      assertEquals("1.0.0", metadata.version, "Version should match");
      assertEquals("Test service", metadata.description, "Description should match");
      assertEquals(2, metadata.capabilities.size(), "Should have 2 capabilities");
      assertEquals(2, metadata.endpoints.size(), "Should have 2 endpoints");
      assertEquals(8080, metadata.port, "Port should match");
      assertTrue(metadata.secure, "Should be secure");
    }

    @Test
    @DisplayName("Capabilities list should be immutable")
    void capabilitiesListShouldBeImmutable() {
      final List<String> capabilities = new ArrayList<>();
      capabilities.add("read");

      final WasiExperimentalProcess.SystemServiceMetadata metadata =
          new WasiExperimentalProcess.SystemServiceMetadata(
              "1.0.0", "Test", capabilities, Collections.emptyList(), 8080, false);

      assertThrows(
          UnsupportedOperationException.class,
          () -> metadata.capabilities.add("write"),
          "Capabilities list should be immutable");
    }
  }

  @Nested
  @DisplayName("ProcessResourceUsage Tests")
  class ProcessResourceUsageTests {

    @Test
    @DisplayName("Should create resource usage with values")
    void shouldCreateResourceUsageWithValues() {
      final Duration executionTime = Duration.ofSeconds(120);
      final Instant lastUpdated = Instant.now();

      final WasiExperimentalProcess.ProcessResourceUsage usage =
          new WasiExperimentalProcess.ProcessResourceUsage(
              50.5,
              1024 * 1024 * 100,
              1000000,
              500000,
              250000,
              125000,
              32,
              4,
              executionTime,
              lastUpdated);

      assertEquals(50.5, usage.cpuUsagePercent, 0.01, "CPU usage should match");
      assertEquals(1024 * 1024 * 100, usage.memoryUsageBytes, "Memory should match");
      assertEquals(1000000, usage.ioReadBytes, "I/O read should match");
      assertEquals(500000, usage.ioWriteBytes, "I/O write should match");
      assertEquals(250000, usage.networkRxBytes, "Network RX should match");
      assertEquals(125000, usage.networkTxBytes, "Network TX should match");
      assertEquals(32, usage.fileDescriptorCount, "File descriptors should match");
      assertEquals(4, usage.threadCount, "Thread count should match");
      assertEquals(executionTime, usage.executionTime, "Execution time should match");
      assertEquals(lastUpdated, usage.lastUpdated, "Last updated should match");
    }
  }

  @Nested
  @DisplayName("ResourceUsageAlert Tests")
  class ResourceUsageAlertTests {

    @Test
    @DisplayName("Should create alert with values")
    void shouldCreateAlertWithValues() {
      final Instant timestamp = Instant.now();

      final WasiExperimentalProcess.ResourceUsageAlert alert =
          new WasiExperimentalProcess.ResourceUsageAlert(
              1L, "CPU", "CPU usage exceeded threshold", 80.0, 95.5, timestamp);

      assertEquals(1L, alert.processHandle, "Process handle should match");
      assertEquals("CPU", alert.alertType, "Alert type should match");
      assertEquals("CPU usage exceeded threshold", alert.message, "Message should match");
      assertEquals(80.0, alert.thresholdValue, 0.01, "Threshold should match");
      assertEquals(95.5, alert.currentValue, 0.01, "Current value should match");
      assertEquals(timestamp, alert.timestamp, "Timestamp should match");
    }
  }

  @Nested
  @DisplayName("ServiceRequest Tests")
  class ServiceRequestTests {

    @Test
    @DisplayName("Should create request with values")
    void shouldCreateRequestWithValues() {
      final Map<String, String> parameters = new HashMap<>();
      parameters.put("key", "value");
      final byte[] data = "test data".getBytes();

      final WasiExperimentalProcess.ServiceRequest request =
          new WasiExperimentalProcess.ServiceRequest("read", parameters, data, "client-123");

      assertEquals("read", request.operation, "Operation should match");
      assertEquals("value", request.parameters.get("key"), "Parameters should match");
      assertEquals("client-123", request.clientId, "Client ID should match");
      assertNotNull(request.data, "Data should not be null");
    }

    @Test
    @DisplayName("Should handle null parameters")
    void shouldHandleNullParameters() {
      final WasiExperimentalProcess.ServiceRequest request =
          new WasiExperimentalProcess.ServiceRequest("read", null, null, "client-123");

      assertNotNull(request.parameters, "Parameters should not be null");
      assertTrue(request.parameters.isEmpty(), "Parameters should be empty");
      assertNotNull(request.data, "Data should not be null");
      assertEquals(0, request.data.length, "Data should be empty");
    }

    @Test
    @DisplayName("Parameters map should be immutable")
    void parametersMapShouldBeImmutable() {
      final Map<String, String> parameters = new HashMap<>();
      parameters.put("key", "value");

      final WasiExperimentalProcess.ServiceRequest request =
          new WasiExperimentalProcess.ServiceRequest("read", parameters, null, "client-123");

      assertThrows(
          UnsupportedOperationException.class,
          () -> request.parameters.put("newKey", "newValue"),
          "Parameters map should be immutable");
    }
  }

  @Nested
  @DisplayName("SystemServiceInfo Tests")
  class SystemServiceInfoTests {

    @Test
    @DisplayName("Should create service info with values")
    void shouldCreateServiceInfoWithValues() {
      final WasiExperimentalProcess.SystemServiceMetadata metadata =
          new WasiExperimentalProcess.SystemServiceMetadata(
              "1.0", "Test", Collections.emptyList(), Collections.emptyList(), 8080, false);
      final Consumer<WasiExperimentalProcess.ServiceRequest> handler = req -> {};
      final long registeredAt = System.currentTimeMillis();

      final WasiExperimentalProcess.SystemServiceInfo info =
          new WasiExperimentalProcess.SystemServiceInfo(
              "test-service",
              metadata,
              handler,
              WasiExperimentalProcess.ServiceState.ACTIVE,
              "service-id-123",
              registeredAt);

      assertEquals("test-service", info.serviceName, "Service name should match");
      assertNotNull(info.metadata, "Metadata should not be null");
      assertNotNull(info.handler, "Handler should not be null");
      assertEquals(
          WasiExperimentalProcess.ServiceState.ACTIVE, info.state, "State should be ACTIVE");
      assertEquals("service-id-123", info.serviceId, "Service ID should match");
      assertEquals(registeredAt, info.registeredAt, "Registered time should match");
    }
  }
}
