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

package ai.tegmentum.wasmtime4j.debug;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.debug.DebugEvent.DebugEventType;
import ai.tegmentum.wasmtime4j.debug.ExecutionState.ExecutionStatus;
import ai.tegmentum.wasmtime4j.debug.GuestProfiler.ProfileFormat;
import ai.tegmentum.wasmtime4j.debug.ProfileData.FunctionProfile;
import ai.tegmentum.wasmtime4j.debug.WasmCoreDump.MemoryDump;
import ai.tegmentum.wasmtime4j.debug.WasmCoreDump.StackFrame;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.io.TempDir;

/**
 * Comprehensive integration tests for the debug package data classes.
 *
 * <p>Tests ProfilerConfig, ProfileData, WasmCoreDump, DebugEvent, and related enums.
 */
@DisplayName("Debug Package Integration Tests")
class DebugPackageIntegrationTest {

  private static final Logger LOGGER =
      Logger.getLogger(DebugPackageIntegrationTest.class.getName());

  @Nested
  @DisplayName("ProfilerConfig Tests")
  class ProfilerConfigTests {

    @Test
    @DisplayName("should create default config with correct values")
    void shouldCreateDefaultConfigWithCorrectValues(final TestInfo testInfo) {
      LOGGER.info("Running: " + testInfo.getDisplayName());

      final ProfilerConfig config = ProfilerConfig.defaults();

      assertTrue(config.isTrackFunctionCalls(), "Function calls should be tracked by default");
      assertFalse(
          config.isTrackMemoryOperations(), "Memory operations should not be tracked by default");
      assertFalse(
          config.isTrackInstructionCount(), "Instruction count should not be tracked by default");
      assertTrue(config.isTrackStackDepth(), "Stack depth should be tracked by default");
      assertEquals(
          1_000_000, config.getSamplingIntervalNanos(), "Default sampling interval should be 1ms");
      assertEquals(128, config.getMaxStackFrames(), "Default max stack frames should be 128");
    }

    @Test
    @DisplayName("should create builder with default values")
    void shouldCreateBuilderWithDefaultValues(final TestInfo testInfo) {
      LOGGER.info("Running: " + testInfo.getDisplayName());

      final ProfilerConfig config = ProfilerConfig.builder().build();

      assertEquals(ProfilerConfig.defaults().isTrackFunctionCalls(), config.isTrackFunctionCalls());
      assertEquals(
          ProfilerConfig.defaults().isTrackMemoryOperations(), config.isTrackMemoryOperations());
      assertEquals(
          ProfilerConfig.defaults().getSamplingIntervalNanos(), config.getSamplingIntervalNanos());
    }

    @Test
    @DisplayName("should configure all tracking options")
    void shouldConfigureAllTrackingOptions(final TestInfo testInfo) {
      LOGGER.info("Running: " + testInfo.getDisplayName());

      final ProfilerConfig config =
          ProfilerConfig.builder()
              .trackFunctionCalls(false)
              .trackMemoryOperations(true)
              .trackInstructionCount(true)
              .trackStackDepth(false)
              .build();

      assertFalse(config.isTrackFunctionCalls(), "Function calls tracking should be disabled");
      assertTrue(config.isTrackMemoryOperations(), "Memory operations tracking should be enabled");
      assertTrue(config.isTrackInstructionCount(), "Instruction count tracking should be enabled");
      assertFalse(config.isTrackStackDepth(), "Stack depth tracking should be disabled");
    }

    @Test
    @DisplayName("should configure sampling interval")
    void shouldConfigureSamplingInterval(final TestInfo testInfo) {
      LOGGER.info("Running: " + testInfo.getDisplayName());

      final ProfilerConfig config =
          ProfilerConfig.builder().samplingIntervalNanos(5_000_000).build();

      assertEquals(5_000_000, config.getSamplingIntervalNanos(), "Sampling interval should be 5ms");
    }

    @Test
    @DisplayName("should configure max stack frames")
    void shouldConfigureMaxStackFrames(final TestInfo testInfo) {
      LOGGER.info("Running: " + testInfo.getDisplayName());

      final ProfilerConfig config = ProfilerConfig.builder().maxStackFrames(256).build();

      assertEquals(256, config.getMaxStackFrames(), "Max stack frames should be 256");
    }

    @Test
    @DisplayName("should support builder method chaining")
    void shouldSupportBuilderMethodChaining(final TestInfo testInfo) {
      LOGGER.info("Running: " + testInfo.getDisplayName());

      final ProfilerConfig config =
          ProfilerConfig.builder()
              .trackFunctionCalls(true)
              .trackMemoryOperations(true)
              .trackInstructionCount(true)
              .trackStackDepth(true)
              .samplingIntervalNanos(100_000)
              .maxStackFrames(64)
              .build();

      assertTrue(config.isTrackFunctionCalls());
      assertTrue(config.isTrackMemoryOperations());
      assertTrue(config.isTrackInstructionCount());
      assertTrue(config.isTrackStackDepth());
      assertEquals(100_000, config.getSamplingIntervalNanos());
      assertEquals(64, config.getMaxStackFrames());
    }

    @Test
    @DisplayName("should accept zero sampling interval")
    void shouldAcceptZeroSamplingInterval(final TestInfo testInfo) {
      LOGGER.info("Running: " + testInfo.getDisplayName());

      final ProfilerConfig config = ProfilerConfig.builder().samplingIntervalNanos(0).build();

      assertEquals(
          0, config.getSamplingIntervalNanos(), "Zero sampling interval should be accepted");
    }

    @Test
    @DisplayName("should accept zero max stack frames")
    void shouldAcceptZeroMaxStackFrames(final TestInfo testInfo) {
      LOGGER.info("Running: " + testInfo.getDisplayName());

      final ProfilerConfig config = ProfilerConfig.builder().maxStackFrames(0).build();

      assertEquals(0, config.getMaxStackFrames(), "Zero max stack frames should be accepted");
    }
  }

  @Nested
  @DisplayName("ProfileData Tests")
  class ProfileDataTests {

    @Test
    @DisplayName("should create profile data with all fields")
    void shouldCreateProfileDataWithAllFields(final TestInfo testInfo) {
      LOGGER.info("Running: " + testInfo.getDisplayName());

      final Duration duration = Duration.ofMillis(1500);
      final List<FunctionProfile> profiles =
          Arrays.asList(
              new FunctionProfile("main", 0, 100, Duration.ofMillis(500), Duration.ofMillis(200)),
              new FunctionProfile("helper", 1, 50, Duration.ofMillis(200), Duration.ofMillis(150)));
      final Map<String, Long> metrics = new HashMap<>();
      metrics.put("custom_metric", 42L);

      final ProfileData data = new ProfileData(duration, 1000, 50000, 10, profiles, metrics);

      assertEquals(duration, data.getTotalDuration());
      assertEquals(1000, data.getTotalFunctionCalls());
      assertEquals(50000, data.getTotalInstructions());
      assertEquals(10, data.getMaxStackDepth());
      assertEquals(2, data.getFunctionProfiles().size());
      assertEquals(1, data.getCustomMetrics().size());
      assertEquals(42L, data.getCustomMetrics().get("custom_metric"));
    }

    @Test
    @DisplayName("should return unmodifiable function profiles list")
    void shouldReturnUnmodifiableFunctionProfilesList(final TestInfo testInfo) {
      LOGGER.info("Running: " + testInfo.getDisplayName());

      final List<FunctionProfile> profiles = new ArrayList<>();
      profiles.add(
          new FunctionProfile("main", 0, 100, Duration.ofMillis(500), Duration.ofMillis(200)));

      final ProfileData data =
          new ProfileData(Duration.ZERO, 0, 0, 0, profiles, Collections.emptyMap());

      assertThrows(
          UnsupportedOperationException.class,
          () ->
              data.getFunctionProfiles()
                  .add(new FunctionProfile("new", 1, 1, Duration.ZERO, Duration.ZERO)),
          "Function profiles list should be unmodifiable");
    }

    @Test
    @DisplayName("should return unmodifiable custom metrics map")
    void shouldReturnUnmodifiableCustomMetricsMap(final TestInfo testInfo) {
      LOGGER.info("Running: " + testInfo.getDisplayName());

      final Map<String, Long> metrics = new HashMap<>();
      metrics.put("metric1", 1L);

      final ProfileData data =
          new ProfileData(Duration.ZERO, 0, 0, 0, Collections.emptyList(), metrics);

      assertThrows(
          UnsupportedOperationException.class,
          () -> data.getCustomMetrics().put("new_metric", 100L),
          "Custom metrics map should be unmodifiable");
    }

    @Test
    @DisplayName("should create empty profile data")
    void shouldCreateEmptyProfileData(final TestInfo testInfo) {
      LOGGER.info("Running: " + testInfo.getDisplayName());

      final ProfileData data =
          new ProfileData(Duration.ZERO, 0, 0, 0, Collections.emptyList(), Collections.emptyMap());

      assertEquals(Duration.ZERO, data.getTotalDuration());
      assertEquals(0, data.getTotalFunctionCalls());
      assertEquals(0, data.getTotalInstructions());
      assertEquals(0, data.getMaxStackDepth());
      assertTrue(data.getFunctionProfiles().isEmpty());
      assertTrue(data.getCustomMetrics().isEmpty());
    }
  }

  @Nested
  @DisplayName("FunctionProfile Tests")
  class FunctionProfileTests {

    @Test
    @DisplayName("should create function profile with all fields")
    void shouldCreateFunctionProfileWithAllFields(final TestInfo testInfo) {
      LOGGER.info("Running: " + testInfo.getDisplayName());

      final FunctionProfile profile =
          new FunctionProfile(
              "testFunction", 42, 1000, Duration.ofMillis(500), Duration.ofMillis(250));

      assertEquals("testFunction", profile.getFunctionName());
      assertEquals(42, profile.getFunctionIndex());
      assertEquals(1000, profile.getCallCount());
      assertEquals(Duration.ofMillis(500), profile.getTotalTime());
      assertEquals(Duration.ofMillis(250), profile.getSelfTime());
    }

    @Test
    @DisplayName("should format toString correctly")
    void shouldFormatToStringCorrectly(final TestInfo testInfo) {
      LOGGER.info("Running: " + testInfo.getDisplayName());

      final FunctionProfile profile =
          new FunctionProfile("myFunc", 5, 100, Duration.ofMillis(200), Duration.ofMillis(100));

      final String str = profile.toString();
      assertTrue(str.contains("myFunc"), "toString should contain function name");
      assertTrue(str.contains("5"), "toString should contain function index");
      assertTrue(str.contains("100"), "toString should contain call count");
    }

    @Test
    @DisplayName("should handle null function name")
    void shouldHandleNullFunctionName(final TestInfo testInfo) {
      LOGGER.info("Running: " + testInfo.getDisplayName());

      final FunctionProfile profile = new FunctionProfile(null, 0, 0, Duration.ZERO, Duration.ZERO);

      assertNull(profile.getFunctionName(), "Null function name should be preserved");
    }
  }

  @Nested
  @DisplayName("WasmCoreDump Tests")
  class WasmCoreDumpTests {

    @TempDir Path tempDir;

    @Test
    @DisplayName("should create core dump with all fields")
    void shouldCreateCoreDumpWithAllFields(final TestInfo testInfo) {
      LOGGER.info("Running: " + testInfo.getDisplayName());

      final Instant timestamp = Instant.now();
      final List<MemoryDump> memoryDumps =
          Arrays.asList(new MemoryDump(0, new byte[] {1, 2, 3}, 0));
      final Map<String, Long> globals = new HashMap<>();
      globals.put("global_0", 100L);
      final List<StackFrame> stackFrames =
          Arrays.asList(new StackFrame("main", 0, 42, new long[] {1, 2, 3}));

      final WasmCoreDump dump =
          new WasmCoreDump("Test trap", null, timestamp, memoryDumps, globals, stackFrames);

      assertEquals("Test trap", dump.getTrapMessage());
      assertNull(dump.getBacktrace());
      assertEquals(timestamp, dump.getTimestamp());
      assertEquals(1, dump.getMemoryDumps().size());
      assertEquals(1, dump.getGlobals().size());
      assertEquals(100L, dump.getGlobals().get("global_0"));
      assertEquals(1, dump.getStackFrames().size());
    }

    @Test
    @DisplayName("should return unmodifiable memory dumps list")
    void shouldReturnUnmodifiableMemoryDumpsList(final TestInfo testInfo) {
      LOGGER.info("Running: " + testInfo.getDisplayName());

      final WasmCoreDump dump =
          new WasmCoreDump(
              "trap",
              null,
              Instant.now(),
              new ArrayList<>(),
              Collections.emptyMap(),
              Collections.emptyList());

      assertThrows(
          UnsupportedOperationException.class,
          () -> dump.getMemoryDumps().add(new MemoryDump(0, new byte[0], 0)),
          "Memory dumps list should be unmodifiable");
    }

    @Test
    @DisplayName("should return unmodifiable globals map")
    void shouldReturnUnmodifiableGlobalsMap(final TestInfo testInfo) {
      LOGGER.info("Running: " + testInfo.getDisplayName());

      final WasmCoreDump dump =
          new WasmCoreDump(
              "trap",
              null,
              Instant.now(),
              Collections.emptyList(),
              new HashMap<>(),
              Collections.emptyList());

      assertThrows(
          UnsupportedOperationException.class,
          () -> dump.getGlobals().put("new_global", 1L),
          "Globals map should be unmodifiable");
    }

    @Test
    @DisplayName("should serialize to bytes")
    void shouldSerializeToBytes(final TestInfo testInfo) {
      LOGGER.info("Running: " + testInfo.getDisplayName());

      final Map<String, Long> globals = new HashMap<>();
      globals.put("test_global", 42L);
      final WasmCoreDump dump =
          new WasmCoreDump(
              "test trap message",
              null,
              Instant.now(),
              Collections.emptyList(),
              globals,
              Collections.emptyList());

      final byte[] serialized = dump.serialize();
      final String content = new String(serialized, StandardCharsets.UTF_8);

      assertTrue(content.contains("WASMCOREDUMP"), "Serialized data should contain header");
      assertTrue(
          content.contains("test trap message"), "Serialized data should contain trap message");
      assertTrue(content.contains("test_global"), "Serialized data should contain global name");
      assertTrue(content.contains("42"), "Serialized data should contain global value");
    }

    @Test
    @DisplayName("should write to and read from file")
    void shouldWriteToAndReadFromFile(final TestInfo testInfo) throws Exception {
      LOGGER.info("Running: " + testInfo.getDisplayName());

      final Path file = tempDir.resolve("test.coredump");
      final WasmCoreDump original =
          new WasmCoreDump(
              "file test trap",
              null,
              Instant.now(),
              Collections.emptyList(),
              Collections.emptyMap(),
              Collections.emptyList());

      original.writeTo(file);

      assertTrue(Files.exists(file), "Core dump file should exist");
      assertTrue(Files.size(file) > 0, "Core dump file should not be empty");

      final WasmCoreDump loaded = WasmCoreDump.readFrom(file);
      assertNotNull(loaded, "Loaded core dump should not be null");
    }

    @Test
    @DisplayName("should throw on null path for writeTo")
    void shouldThrowOnNullPathForWriteTo(final TestInfo testInfo) {
      LOGGER.info("Running: " + testInfo.getDisplayName());

      final WasmCoreDump dump =
          new WasmCoreDump(
              "trap",
              null,
              Instant.now(),
              Collections.emptyList(),
              Collections.emptyMap(),
              Collections.emptyList());

      assertThrows(NullPointerException.class, () -> dump.writeTo(null));
    }

    @Test
    @DisplayName("should throw on null path for readFrom")
    void shouldThrowOnNullPathForReadFrom(final TestInfo testInfo) {
      LOGGER.info("Running: " + testInfo.getDisplayName());

      assertThrows(NullPointerException.class, () -> WasmCoreDump.readFrom(null));
    }

    @Test
    @DisplayName("should format toString correctly")
    void shouldFormatToStringCorrectly(final TestInfo testInfo) {
      LOGGER.info("Running: " + testInfo.getDisplayName());

      final WasmCoreDump dump =
          new WasmCoreDump(
              "test trap",
              null,
              Instant.now(),
              Arrays.asList(new MemoryDump(0, new byte[100], 0)),
              Collections.singletonMap("g", 1L),
              Arrays.asList(new StackFrame("f", 0, 0, null)));

      final String str = dump.toString();
      assertTrue(str.contains("test trap"), "toString should contain trap message");
      assertTrue(str.contains("memoryDumps=1"), "toString should contain memory dump count");
      assertTrue(str.contains("globals=1"), "toString should contain globals count");
      assertTrue(str.contains("stackFrames=1"), "toString should contain stack frame count");
    }
  }

  @Nested
  @DisplayName("MemoryDump Tests")
  class MemoryDumpTests {

    @Test
    @DisplayName("should create memory dump with all fields")
    void shouldCreateMemoryDumpWithAllFields(final TestInfo testInfo) {
      LOGGER.info("Running: " + testInfo.getDisplayName());

      final byte[] data = {1, 2, 3, 4, 5};
      final MemoryDump dump = new MemoryDump(2, data, 0x1000);

      assertEquals(2, dump.getMemoryIndex());
      assertEquals(5, dump.getSize());
      assertEquals(0x1000, dump.getBaseAddress());
      assertArrayEquals(data, dump.getData());
    }

    @Test
    @DisplayName("should clone data in constructor")
    void shouldCloneDataInConstructor(final TestInfo testInfo) {
      LOGGER.info("Running: " + testInfo.getDisplayName());

      final byte[] original = {1, 2, 3};
      final MemoryDump dump = new MemoryDump(0, original, 0);

      original[0] = 99;

      assertEquals(1, dump.getData()[0], "Modifying original array should not affect dump");
    }

    @Test
    @DisplayName("should clone data in getter")
    void shouldCloneDataInGetter(final TestInfo testInfo) {
      LOGGER.info("Running: " + testInfo.getDisplayName());

      final MemoryDump dump = new MemoryDump(0, new byte[] {1, 2, 3}, 0);

      final byte[] retrieved = dump.getData();
      retrieved[0] = 99;

      assertEquals(1, dump.getData()[0], "Modifying retrieved array should not affect dump");
    }

    @Test
    @DisplayName("should return correct size")
    void shouldReturnCorrectSize(final TestInfo testInfo) {
      LOGGER.info("Running: " + testInfo.getDisplayName());

      final MemoryDump dump = new MemoryDump(0, new byte[1024], 0);

      assertEquals(1024, dump.getSize());
    }
  }

  @Nested
  @DisplayName("StackFrame Tests")
  class StackFrameTests {

    @Test
    @DisplayName("should create stack frame with all fields")
    void shouldCreateStackFrameWithAllFields(final TestInfo testInfo) {
      LOGGER.info("Running: " + testInfo.getDisplayName());

      final long[] locals = {10, 20, 30};
      final StackFrame frame = new StackFrame("myFunction", 5, 0x100, locals);

      assertEquals("myFunction", frame.getFunctionName());
      assertEquals(5, frame.getFunctionIndex());
      assertEquals(0x100, frame.getInstructionOffset());
      assertArrayEquals(locals, frame.getLocals());
    }

    @Test
    @DisplayName("should clone locals in constructor")
    void shouldCloneLocalsInConstructor(final TestInfo testInfo) {
      LOGGER.info("Running: " + testInfo.getDisplayName());

      final long[] original = {1, 2, 3};
      final StackFrame frame = new StackFrame("func", 0, 0, original);

      original[0] = 99;

      assertEquals(1, frame.getLocals()[0], "Modifying original array should not affect frame");
    }

    @Test
    @DisplayName("should clone locals in getter")
    void shouldCloneLocalsInGetter(final TestInfo testInfo) {
      LOGGER.info("Running: " + testInfo.getDisplayName());

      final StackFrame frame = new StackFrame("func", 0, 0, new long[] {1, 2, 3});

      final long[] retrieved = frame.getLocals();
      retrieved[0] = 99;

      assertEquals(1, frame.getLocals()[0], "Modifying retrieved array should not affect frame");
    }

    @Test
    @DisplayName("should handle null locals")
    void shouldHandleNullLocals(final TestInfo testInfo) {
      LOGGER.info("Running: " + testInfo.getDisplayName());

      final StackFrame frame = new StackFrame("func", 0, 0, null);

      assertNotNull(frame.getLocals(), "Null locals should become empty array");
      assertEquals(0, frame.getLocals().length);
    }

    @Test
    @DisplayName("should format toString correctly")
    void shouldFormatToStringCorrectly(final TestInfo testInfo) {
      LOGGER.info("Running: " + testInfo.getDisplayName());

      final StackFrame frame = new StackFrame("testFunc", 10, 256, new long[] {1, 2});

      final String str = frame.toString();
      assertTrue(str.contains("testFunc"), "toString should contain function name");
      assertTrue(str.contains("10"), "toString should contain function index");
      assertTrue(str.contains("256"), "toString should contain instruction offset");
      assertTrue(str.contains("2"), "toString should contain locals count");
    }
  }

  @Nested
  @DisplayName("DebugEvent Tests")
  class DebugEventTests {

    @Test
    @DisplayName("should create event with type and state")
    void shouldCreateEventWithTypeAndState(final TestInfo testInfo) {
      LOGGER.info("Running: " + testInfo.getDisplayName());

      final ExecutionState state = createMockExecutionState();
      final DebugEvent event = new DebugEvent(DebugEventType.BREAKPOINT, state);

      assertEquals(DebugEventType.BREAKPOINT, event.getType());
      assertEquals(state, event.getExecutionState());
      assertTrue(event.getData().isEmpty());
      assertTrue(event.getTimestamp() > 0);
    }

    @Test
    @DisplayName("should create event with additional data")
    void shouldCreateEventWithAdditionalData(final TestInfo testInfo) {
      LOGGER.info("Running: " + testInfo.getDisplayName());

      final ExecutionState state = createMockExecutionState();
      final Map<String, Object> data = new HashMap<>();
      data.put("breakpoint_id", 42);
      data.put("message", "test");

      final DebugEvent event = new DebugEvent(DebugEventType.STEP, state, data);

      assertEquals(2, event.getData().size());
      assertEquals(42, event.getData("breakpoint_id"));
      assertEquals("test", event.getData("message"));
      assertTrue(event.hasData("breakpoint_id"));
      assertFalse(event.hasData("nonexistent"));
    }

    @Test
    @DisplayName("should return unmodifiable data map")
    void shouldReturnUnmodifiableDataMap(final TestInfo testInfo) {
      LOGGER.info("Running: " + testInfo.getDisplayName());

      final ExecutionState state = createMockExecutionState();
      final DebugEvent event = new DebugEvent(DebugEventType.PAUSE, state, new HashMap<>());

      assertThrows(
          UnsupportedOperationException.class,
          () -> event.getData().put("new_key", "value"),
          "Data map should be unmodifiable");
    }

    @Test
    @DisplayName("should throw on null event type")
    void shouldThrowOnNullEventType(final TestInfo testInfo) {
      LOGGER.info("Running: " + testInfo.getDisplayName());

      final ExecutionState state = createMockExecutionState();

      assertThrows(NullPointerException.class, () -> new DebugEvent(null, state));
    }

    @Test
    @DisplayName("should throw on null execution state")
    void shouldThrowOnNullExecutionState(final TestInfo testInfo) {
      LOGGER.info("Running: " + testInfo.getDisplayName());

      assertThrows(NullPointerException.class, () -> new DebugEvent(DebugEventType.COMPLETE, null));
    }

    @Test
    @DisplayName("should implement equals correctly")
    void shouldImplementEqualsCorrectly(final TestInfo testInfo) {
      LOGGER.info("Running: " + testInfo.getDisplayName());

      final ExecutionState state = createMockExecutionState();
      final DebugEvent event1 = new DebugEvent(DebugEventType.EXCEPTION, state);

      assertEquals(event1, event1, "Event should equal itself");
      assertNotEquals(event1, null, "Event should not equal null");
      assertNotEquals(event1, "string", "Event should not equal different type");
    }

    @Test
    @DisplayName("should implement hashCode correctly")
    void shouldImplementHashCodeCorrectly(final TestInfo testInfo) {
      LOGGER.info("Running: " + testInfo.getDisplayName());

      final ExecutionState state = createMockExecutionState();
      final DebugEvent event = new DebugEvent(DebugEventType.TERMINATE, state);

      final int hash1 = event.hashCode();
      final int hash2 = event.hashCode();

      assertEquals(hash1, hash2, "hashCode should be consistent");
    }

    @Test
    @DisplayName("should format toString correctly")
    void shouldFormatToStringCorrectly(final TestInfo testInfo) {
      LOGGER.info("Running: " + testInfo.getDisplayName());

      final ExecutionState state = createMockExecutionState();
      final DebugEvent event = new DebugEvent(DebugEventType.RESUME, state);

      final String str = event.toString();
      assertTrue(str.contains("RESUME"), "toString should contain event type");
      assertTrue(str.contains("timestamp"), "toString should contain timestamp field");
    }

    private ExecutionState createMockExecutionState() {
      return new ExecutionState() {
        @Override
        public ExecutionStatus getStatus() {
          return ExecutionStatus.RUNNING;
        }

        @Override
        public long getInstructionPointer() {
          return 0;
        }

        @Override
        public List<ai.tegmentum.wasmtime4j.debug.StackFrame> getStackFrames() {
          return Collections.emptyList();
        }

        @Override
        public String getCurrentModule() {
          return "test";
        }

        @Override
        public String getCurrentFunction() {
          return "main";
        }

        @Override
        public ExecutionStatistics getStatistics() {
          return null;
        }
      };
    }
  }

  @Nested
  @DisplayName("DebugEventType Enum Tests")
  class DebugEventTypeTests {

    @Test
    @DisplayName("should have all expected values")
    void shouldHaveAllExpectedValues(final TestInfo testInfo) {
      LOGGER.info("Running: " + testInfo.getDisplayName());

      final DebugEventType[] types = DebugEventType.values();

      assertEquals(7, types.length);
      assertNotNull(DebugEventType.valueOf("BREAKPOINT"));
      assertNotNull(DebugEventType.valueOf("STEP"));
      assertNotNull(DebugEventType.valueOf("PAUSE"));
      assertNotNull(DebugEventType.valueOf("RESUME"));
      assertNotNull(DebugEventType.valueOf("COMPLETE"));
      assertNotNull(DebugEventType.valueOf("EXCEPTION"));
      assertNotNull(DebugEventType.valueOf("TERMINATE"));
    }
  }

  @Nested
  @DisplayName("ExecutionStatus Enum Tests")
  class ExecutionStatusTests {

    @Test
    @DisplayName("should have all expected values")
    void shouldHaveAllExpectedValues(final TestInfo testInfo) {
      LOGGER.info("Running: " + testInfo.getDisplayName());

      final ExecutionStatus[] statuses = ExecutionStatus.values();

      assertEquals(5, statuses.length);
      assertNotNull(ExecutionStatus.valueOf("RUNNING"));
      assertNotNull(ExecutionStatus.valueOf("PAUSED"));
      assertNotNull(ExecutionStatus.valueOf("STOPPED"));
      assertNotNull(ExecutionStatus.valueOf("CRASHED"));
      assertNotNull(ExecutionStatus.valueOf("COMPLETED"));
    }
  }

  @Nested
  @DisplayName("ProfileFormat Enum Tests")
  class ProfileFormatTests {

    @Test
    @DisplayName("should have all expected values")
    void shouldHaveAllExpectedValues(final TestInfo testInfo) {
      LOGGER.info("Running: " + testInfo.getDisplayName());

      final ProfileFormat[] formats = ProfileFormat.values();

      assertEquals(4, formats.length);
      assertNotNull(ProfileFormat.valueOf("JSON"));
      assertNotNull(ProfileFormat.valueOf("FLAMEGRAPH"));
      assertNotNull(ProfileFormat.valueOf("CHROME_TRACE"));
      assertNotNull(ProfileFormat.valueOf("PPROF"));
    }
  }

  @Nested
  @DisplayName("Defensive Copying Tests")
  class DefensiveCopyingTests {

    @Test
    @DisplayName("should protect ProfileData from external list modification")
    void shouldProtectProfileDataFromExternalListModification(final TestInfo testInfo) {
      LOGGER.info("Running: " + testInfo.getDisplayName());

      final List<FunctionProfile> profiles = new ArrayList<>();
      profiles.add(new FunctionProfile("original", 0, 1, Duration.ZERO, Duration.ZERO));

      final ProfileData data =
          new ProfileData(Duration.ZERO, 0, 0, 0, profiles, Collections.emptyMap());

      // Try to modify the original list
      profiles.add(new FunctionProfile("added", 1, 1, Duration.ZERO, Duration.ZERO));

      // ProfileData should still have only 1 profile
      assertEquals(
          1,
          data.getFunctionProfiles().size(),
          "ProfileData should not be affected by external modification");
    }

    @Test
    @DisplayName("should protect ProfileData from external map modification")
    void shouldProtectProfileDataFromExternalMapModification(final TestInfo testInfo) {
      LOGGER.info("Running: " + testInfo.getDisplayName());

      final Map<String, Long> metrics = new HashMap<>();
      metrics.put("original", 1L);

      final ProfileData data =
          new ProfileData(Duration.ZERO, 0, 0, 0, Collections.emptyList(), metrics);

      // Try to modify the original map
      metrics.put("added", 2L);

      // ProfileData should still have only 1 metric
      assertEquals(
          1,
          data.getCustomMetrics().size(),
          "ProfileData should not be affected by external modification");
    }

    @Test
    @DisplayName("should protect WasmCoreDump from external list modification")
    void shouldProtectWasmCoreDumpFromExternalListModification(final TestInfo testInfo) {
      LOGGER.info("Running: " + testInfo.getDisplayName());

      final List<MemoryDump> dumps = new ArrayList<>();
      dumps.add(new MemoryDump(0, new byte[10], 0));

      final WasmCoreDump dump =
          new WasmCoreDump(
              "trap", null, Instant.now(), dumps, Collections.emptyMap(), Collections.emptyList());

      // Try to modify the original list
      dumps.add(new MemoryDump(1, new byte[20], 0));

      assertEquals(
          1,
          dump.getMemoryDumps().size(),
          "WasmCoreDump should not be affected by external modification");
    }

    @Test
    @DisplayName("should ensure MemoryDump data is cloned")
    void shouldEnsureMemoryDumpDataIsCloned(final TestInfo testInfo) {
      LOGGER.info("Running: " + testInfo.getDisplayName());

      final byte[] data = {1, 2, 3, 4, 5};
      final MemoryDump dump = new MemoryDump(0, data, 0);

      // Get two copies of data
      final byte[] copy1 = dump.getData();
      final byte[] copy2 = dump.getData();

      assertNotSame(copy1, copy2, "getData should return different array instances");
      assertArrayEquals(copy1, copy2, "getData should return equal data");
    }
  }
}
