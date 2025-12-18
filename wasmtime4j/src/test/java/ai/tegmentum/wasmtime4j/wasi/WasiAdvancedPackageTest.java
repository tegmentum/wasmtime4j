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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive tests for WASI Advanced Package features.
 *
 * <p>This test class covers the advanced WASI features including process management, Preview 2
 * context and streams, and supporting types not covered by WasiCorePackageTest.
 */
@DisplayName("WASI Advanced Package Tests")
class WasiAdvancedPackageTest {

  // ========================================================================
  // WasiProcess Interface Tests
  // ========================================================================

  @Nested
  @DisplayName("WasiProcess Interface Tests")
  class WasiProcessTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeAnInterface() {
      assertTrue(WasiProcess.class.isInterface(), "WasiProcess should be an interface");
    }

    @Test
    @DisplayName("should have spawn method")
    void shouldHaveSpawnMethod() throws NoSuchMethodException {
      Method method = WasiProcess.class.getMethod("spawn", WasiProcessConfig.class);

      assertNotNull(method, "spawn method should exist");
      assertEquals(WasiProcessId.class, method.getReturnType(), "Return type should be WasiProcessId");
      assertEquals(1, method.getParameterCount(), "spawn should take 1 parameter");
    }

    @Test
    @DisplayName("should have waitFor method")
    void shouldHaveWaitForMethod() throws NoSuchMethodException {
      Method method = WasiProcess.class.getMethod("waitFor", WasiProcessId.class);

      assertNotNull(method, "waitFor method should exist");
      assertEquals(int.class, method.getReturnType(), "Return type should be int");
      assertEquals(1, method.getParameterCount(), "waitFor should take 1 parameter");
    }

    @Test
    @DisplayName("should have kill method")
    void shouldHaveKillMethod() throws NoSuchMethodException {
      Method method = WasiProcess.class.getMethod("kill", WasiProcessId.class, WasiSignal.class);

      assertNotNull(method, "kill method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
      assertEquals(2, method.getParameterCount(), "kill should take 2 parameters");
    }

    @Test
    @DisplayName("should have getEnvironment method")
    void shouldHaveGetEnvironmentMethod() throws NoSuchMethodException {
      Method method = WasiProcess.class.getMethod("getEnvironment");

      assertNotNull(method, "getEnvironment method should exist");
      assertEquals(Map.class, method.getReturnType(), "Return type should be Map");
      assertEquals(0, method.getParameterCount(), "getEnvironment should take no parameters");
    }

    @Test
    @DisplayName("should have getEnvironmentVariable method")
    void shouldHaveGetEnvironmentVariableMethod() throws NoSuchMethodException {
      Method method = WasiProcess.class.getMethod("getEnvironmentVariable", String.class);

      assertNotNull(method, "getEnvironmentVariable method should exist");
      assertEquals(String.class, method.getReturnType(), "Return type should be String");
    }

    @Test
    @DisplayName("should have setEnvironmentVariable method")
    void shouldHaveSetEnvironmentVariableMethod() throws NoSuchMethodException {
      Method method =
          WasiProcess.class.getMethod("setEnvironmentVariable", String.class, String.class);

      assertNotNull(method, "setEnvironmentVariable method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
    }

    @Test
    @DisplayName("should have getCurrentProcessId method")
    void shouldHaveGetCurrentProcessIdMethod() throws NoSuchMethodException {
      Method method = WasiProcess.class.getMethod("getCurrentProcessId");

      assertNotNull(method, "getCurrentProcessId method should exist");
      assertEquals(WasiProcessId.class, method.getReturnType(), "Return type should be WasiProcessId");
    }

    @Test
    @DisplayName("should have getCurrentWorkingDirectory method")
    void shouldHaveGetCurrentWorkingDirectoryMethod() throws NoSuchMethodException {
      Method method = WasiProcess.class.getMethod("getCurrentWorkingDirectory");

      assertNotNull(method, "getCurrentWorkingDirectory method should exist");
      assertEquals(String.class, method.getReturnType(), "Return type should be String");
    }

    @Test
    @DisplayName("should have setCurrentWorkingDirectory method")
    void shouldHaveSetCurrentWorkingDirectoryMethod() throws NoSuchMethodException {
      Method method = WasiProcess.class.getMethod("setCurrentWorkingDirectory", String.class);

      assertNotNull(method, "setCurrentWorkingDirectory method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
    }

    @Test
    @DisplayName("should have getCommandLineArguments method")
    void shouldHaveGetCommandLineArgumentsMethod() throws NoSuchMethodException {
      Method method = WasiProcess.class.getMethod("getCommandLineArguments");

      assertNotNull(method, "getCommandLineArguments method should exist");
      assertEquals(List.class, method.getReturnType(), "Return type should be List");
    }

    @Test
    @DisplayName("should have exit method")
    void shouldHaveExitMethod() throws NoSuchMethodException {
      Method method = WasiProcess.class.getMethod("exit", int.class);

      assertNotNull(method, "exit method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
    }

    @Test
    @DisplayName("should have abort method")
    void shouldHaveAbortMethod() throws NoSuchMethodException {
      Method method = WasiProcess.class.getMethod("abort");

      assertNotNull(method, "abort method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
    }

    @Test
    @DisplayName("should have isProcessRunning method")
    void shouldHaveIsProcessRunningMethod() throws NoSuchMethodException {
      Method method = WasiProcess.class.getMethod("isProcessRunning", WasiProcessId.class);

      assertNotNull(method, "isProcessRunning method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Return type should be boolean");
    }

    @Test
    @DisplayName("should have getProcessExitCode method")
    void shouldHaveGetProcessExitCodeMethod() throws NoSuchMethodException {
      Method method = WasiProcess.class.getMethod("getProcessExitCode", WasiProcessId.class);

      assertNotNull(method, "getProcessExitCode method should exist");
      assertEquals(int.class, method.getReturnType(), "Return type should be int");
    }

    @Test
    @DisplayName("should have all expected methods")
    void shouldHaveAllExpectedMethods() {
      Set<String> expectedMethods =
          new HashSet<>(
              Arrays.asList(
                  "spawn",
                  "waitFor",
                  "kill",
                  "getEnvironment",
                  "getEnvironmentVariable",
                  "setEnvironmentVariable",
                  "getCurrentProcessId",
                  "getCurrentWorkingDirectory",
                  "setCurrentWorkingDirectory",
                  "getCommandLineArguments",
                  "exit",
                  "abort",
                  "isProcessRunning",
                  "getProcessExitCode"));

      Method[] methods = WasiProcess.class.getDeclaredMethods();
      Set<String> actualMethods = new HashSet<>();
      for (Method m : methods) {
        if (Modifier.isPublic(m.getModifiers())) {
          actualMethods.add(m.getName());
        }
      }

      for (String expected : expectedMethods) {
        assertTrue(actualMethods.contains(expected), "Should have method: " + expected);
      }
    }
  }

  // ========================================================================
  // WasiProcessConfig Tests
  // ========================================================================

  @Nested
  @DisplayName("WasiProcessConfig Tests")
  class WasiProcessConfigTests {

    @Test
    @DisplayName("should be a final class")
    void shouldBeFinalClass() {
      assertTrue(
          Modifier.isFinal(WasiProcessConfig.class.getModifiers()),
          "WasiProcessConfig should be a final class");
    }

    @Test
    @DisplayName("should have builder static method")
    void shouldHaveBuilderMethod() throws NoSuchMethodException {
      Method method = WasiProcessConfig.class.getMethod("builder");

      assertNotNull(method, "builder method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "builder should be static");
      assertEquals(
          WasiProcessConfig.Builder.class,
          method.getReturnType(),
          "Return type should be Builder");
    }

    @Test
    @DisplayName("should have getProgram method")
    void shouldHaveGetProgramMethod() throws NoSuchMethodException {
      Method method = WasiProcessConfig.class.getMethod("getProgram");

      assertNotNull(method, "getProgram method should exist");
      assertEquals(String.class, method.getReturnType(), "Return type should be String");
    }

    @Test
    @DisplayName("should have getArguments method")
    void shouldHaveGetArgumentsMethod() throws NoSuchMethodException {
      Method method = WasiProcessConfig.class.getMethod("getArguments");

      assertNotNull(method, "getArguments method should exist");
      assertEquals(List.class, method.getReturnType(), "Return type should be List");
    }

    @Test
    @DisplayName("should have getEnvironment method")
    void shouldHaveGetEnvironmentMethod() throws NoSuchMethodException {
      Method method = WasiProcessConfig.class.getMethod("getEnvironment");

      assertNotNull(method, "getEnvironment method should exist");
      assertEquals(Map.class, method.getReturnType(), "Return type should be Map");
    }

    @Test
    @DisplayName("should have getWorkingDirectory method")
    void shouldHaveGetWorkingDirectoryMethod() throws NoSuchMethodException {
      Method method = WasiProcessConfig.class.getMethod("getWorkingDirectory");

      assertNotNull(method, "getWorkingDirectory method should exist");
      assertEquals(String.class, method.getReturnType(), "Return type should be String");
    }

    @Test
    @DisplayName("should have getResourceLimits method")
    void shouldHaveGetResourceLimitsMethod() throws NoSuchMethodException {
      Method method = WasiProcessConfig.class.getMethod("getResourceLimits");

      assertNotNull(method, "getResourceLimits method should exist");
      assertEquals(WasiResourceLimits.class, method.getReturnType(), "Return type should be WasiResourceLimits");
    }

    @Test
    @DisplayName("should have toBuilder method")
    void shouldHaveToBuilderMethod() throws NoSuchMethodException {
      Method method = WasiProcessConfig.class.getMethod("toBuilder");

      assertNotNull(method, "toBuilder method should exist");
      assertEquals(
          WasiProcessConfig.Builder.class,
          method.getReturnType(),
          "Return type should be Builder");
    }

    @Test
    @DisplayName("should build config with program")
    void shouldBuildConfigWithProgram() {
      WasiProcessConfig config =
          WasiProcessConfig.builder().setProgram("/bin/echo").build();

      assertEquals("/bin/echo", config.getProgram(), "Program should match");
    }

    @Test
    @DisplayName("should throw exception when building without program")
    void shouldThrowExceptionWhenBuildingWithoutProgram() {
      WasiProcessConfig.Builder builder = WasiProcessConfig.builder();

      assertThrows(
          IllegalStateException.class, builder::build, "Should throw when program not set");
    }

    @Test
    @DisplayName("should build config with arguments")
    void shouldBuildConfigWithArguments() {
      WasiProcessConfig config =
          WasiProcessConfig.builder()
              .setProgram("/bin/echo")
              .addArgument("Hello")
              .addArgument("World")
              .build();

      assertEquals(2, config.getArguments().size(), "Should have 2 arguments");
      assertEquals("Hello", config.getArguments().get(0), "First argument should match");
      assertEquals("World", config.getArguments().get(1), "Second argument should match");
    }

    @Test
    @DisplayName("should build config with environment variables")
    void shouldBuildConfigWithEnvironmentVariables() {
      WasiProcessConfig config =
          WasiProcessConfig.builder()
              .setProgram("/bin/echo")
              .setEnvironmentVariable("PATH", "/usr/bin")
              .setEnvironmentVariable("HOME", "/home/user")
              .build();

      assertEquals(2, config.getEnvironment().size(), "Should have 2 env vars");
      assertEquals("/usr/bin", config.getEnvironment().get("PATH"), "PATH should match");
      assertEquals("/home/user", config.getEnvironment().get("HOME"), "HOME should match");
    }

    @Test
    @DisplayName("should throw exception for null program")
    void shouldThrowExceptionForNullProgram() {
      WasiProcessConfig.Builder builder = WasiProcessConfig.builder();

      assertThrows(
          IllegalArgumentException.class,
          () -> builder.setProgram(null),
          "Should throw for null program");
    }

    @Test
    @DisplayName("should throw exception for empty program")
    void shouldThrowExceptionForEmptyProgram() {
      WasiProcessConfig.Builder builder = WasiProcessConfig.builder();

      assertThrows(
          IllegalArgumentException.class,
          () -> builder.setProgram(""),
          "Should throw for empty program");
    }

    @Test
    @DisplayName("should throw exception for null argument")
    void shouldThrowExceptionForNullArgument() {
      WasiProcessConfig.Builder builder = WasiProcessConfig.builder().setProgram("/bin/echo");

      assertThrows(
          IllegalArgumentException.class,
          () -> builder.addArgument(null),
          "Should throw for null argument");
    }

    @Test
    @DisplayName("should throw exception for null environment key")
    void shouldThrowExceptionForNullEnvironmentKey() {
      WasiProcessConfig.Builder builder = WasiProcessConfig.builder().setProgram("/bin/echo");

      assertThrows(
          IllegalArgumentException.class,
          () -> builder.setEnvironmentVariable(null, "value"),
          "Should throw for null env key");
    }

    @Test
    @DisplayName("should throw exception for null environment value")
    void shouldThrowExceptionForNullEnvironmentValue() {
      WasiProcessConfig.Builder builder = WasiProcessConfig.builder().setProgram("/bin/echo");

      assertThrows(
          IllegalArgumentException.class,
          () -> builder.setEnvironmentVariable("key", null),
          "Should throw for null env value");
    }

    @Test
    @DisplayName("should return immutable arguments list")
    void shouldReturnImmutableArgumentsList() {
      WasiProcessConfig config =
          WasiProcessConfig.builder()
              .setProgram("/bin/echo")
              .addArgument("test")
              .build();

      List<String> args = config.getArguments();
      assertThrows(
          UnsupportedOperationException.class,
          () -> args.add("new"),
          "Arguments should be immutable");
    }

    @Test
    @DisplayName("should return immutable environment map")
    void shouldReturnImmutableEnvironmentMap() {
      WasiProcessConfig config =
          WasiProcessConfig.builder()
              .setProgram("/bin/echo")
              .setEnvironmentVariable("KEY", "VALUE")
              .build();

      Map<String, String> env = config.getEnvironment();
      assertThrows(
          UnsupportedOperationException.class,
          () -> env.put("NEW", "VALUE"),
          "Environment should be immutable");
    }

    @Test
    @DisplayName("should create builder from existing config")
    void shouldCreateBuilderFromExistingConfig() {
      WasiProcessConfig original =
          WasiProcessConfig.builder()
              .setProgram("/bin/echo")
              .addArgument("original")
              .setWorkingDirectory("/tmp")
              .build();

      WasiProcessConfig modified =
          original.toBuilder()
              .addArgument("modified")
              .build();

      assertEquals(2, modified.getArguments().size(), "Should have 2 arguments");
      assertEquals("/bin/echo", modified.getProgram(), "Program should be preserved");
      assertEquals("/tmp", modified.getWorkingDirectory(), "Working dir should be preserved");
    }
  }

  // ========================================================================
  // WasiProcessConfig.Builder Tests
  // ========================================================================

  @Nested
  @DisplayName("WasiProcessConfig.Builder Tests")
  class WasiProcessConfigBuilderTests {

    @Test
    @DisplayName("should be a static final inner class")
    void shouldBeStaticFinalInnerClass() {
      Class<?> builderClass = WasiProcessConfig.Builder.class;

      assertTrue(Modifier.isStatic(builderClass.getModifiers()), "Builder should be static");
      assertTrue(Modifier.isFinal(builderClass.getModifiers()), "Builder should be final");
      assertEquals(
          WasiProcessConfig.class,
          builderClass.getEnclosingClass(),
          "Builder should be inside WasiProcessConfig");
    }

    @Test
    @DisplayName("should have setProgram method returning Builder")
    void shouldHaveSetProgramMethod() throws NoSuchMethodException {
      Method method = WasiProcessConfig.Builder.class.getMethod("setProgram", String.class);

      assertNotNull(method, "setProgram method should exist");
      assertEquals(
          WasiProcessConfig.Builder.class,
          method.getReturnType(),
          "Return type should be Builder");
    }

    @Test
    @DisplayName("should have addArgument method returning Builder")
    void shouldHaveAddArgumentMethod() throws NoSuchMethodException {
      Method method = WasiProcessConfig.Builder.class.getMethod("addArgument", String.class);

      assertNotNull(method, "addArgument method should exist");
      assertEquals(
          WasiProcessConfig.Builder.class,
          method.getReturnType(),
          "Return type should be Builder");
    }

    @Test
    @DisplayName("should have setArguments method returning Builder")
    void shouldHaveSetArgumentsMethod() throws NoSuchMethodException {
      Method method = WasiProcessConfig.Builder.class.getMethod("setArguments", List.class);

      assertNotNull(method, "setArguments method should exist");
      assertEquals(
          WasiProcessConfig.Builder.class,
          method.getReturnType(),
          "Return type should be Builder");
    }

    @Test
    @DisplayName("should have setEnvironmentVariable method returning Builder")
    void shouldHaveSetEnvironmentVariableMethod() throws NoSuchMethodException {
      Method method =
          WasiProcessConfig.Builder.class.getMethod(
              "setEnvironmentVariable", String.class, String.class);

      assertNotNull(method, "setEnvironmentVariable method should exist");
      assertEquals(
          WasiProcessConfig.Builder.class,
          method.getReturnType(),
          "Return type should be Builder");
    }

    @Test
    @DisplayName("should have setEnvironment method returning Builder")
    void shouldHaveSetEnvironmentMethod() throws NoSuchMethodException {
      Method method = WasiProcessConfig.Builder.class.getMethod("setEnvironment", Map.class);

      assertNotNull(method, "setEnvironment method should exist");
      assertEquals(
          WasiProcessConfig.Builder.class,
          method.getReturnType(),
          "Return type should be Builder");
    }

    @Test
    @DisplayName("should have setWorkingDirectory method returning Builder")
    void shouldHaveSetWorkingDirectoryMethod() throws NoSuchMethodException {
      Method method =
          WasiProcessConfig.Builder.class.getMethod("setWorkingDirectory", String.class);

      assertNotNull(method, "setWorkingDirectory method should exist");
      assertEquals(
          WasiProcessConfig.Builder.class,
          method.getReturnType(),
          "Return type should be Builder");
    }

    @Test
    @DisplayName("should have setResourceLimits method returning Builder")
    void shouldHaveSetResourceLimitsMethod() throws NoSuchMethodException {
      Method method =
          WasiProcessConfig.Builder.class.getMethod("setResourceLimits", WasiResourceLimits.class);

      assertNotNull(method, "setResourceLimits method should exist");
      assertEquals(
          WasiProcessConfig.Builder.class,
          method.getReturnType(),
          "Return type should be Builder");
    }

    @Test
    @DisplayName("should have build method returning WasiProcessConfig")
    void shouldHaveBuildMethod() throws NoSuchMethodException {
      Method method = WasiProcessConfig.Builder.class.getMethod("build");

      assertNotNull(method, "build method should exist");
      assertEquals(
          WasiProcessConfig.class,
          method.getReturnType(),
          "Return type should be WasiProcessConfig");
    }

    @Test
    @DisplayName("should support fluent API")
    void shouldSupportFluentApi() {
      WasiProcessConfig config =
          WasiProcessConfig.builder()
              .setProgram("/bin/test")
              .addArgument("arg1")
              .addArgument("arg2")
              .setEnvironmentVariable("KEY1", "VALUE1")
              .setEnvironmentVariable("KEY2", "VALUE2")
              .setWorkingDirectory("/tmp")
              .build();

      assertNotNull(config, "Config should be built");
      assertEquals("/bin/test", config.getProgram());
      assertEquals(2, config.getArguments().size());
      assertEquals(2, config.getEnvironment().size());
      assertEquals("/tmp", config.getWorkingDirectory());
    }
  }

  // ========================================================================
  // WasiSignal Enum Tests
  // ========================================================================

  @Nested
  @DisplayName("WasiSignal Enum Tests")
  class WasiSignalEnumTests {

    @Test
    @DisplayName("should be an enum")
    void shouldBeAnEnum() {
      assertTrue(WasiSignal.class.isEnum(), "WasiSignal should be an enum");
    }

    @Test
    @DisplayName("should have at least 20 signal values")
    void shouldHaveAtLeast20SignalValues() {
      WasiSignal[] values = WasiSignal.values();

      assertTrue(values.length >= 20, "Should have at least 20 signal values");
    }

    @Test
    @DisplayName("should have common POSIX signals")
    void shouldHaveCommonPosixSignals() {
      assertNotNull(WasiSignal.valueOf("SIGHUP"), "SIGHUP should exist");
      assertNotNull(WasiSignal.valueOf("SIGINT"), "SIGINT should exist");
      assertNotNull(WasiSignal.valueOf("SIGQUIT"), "SIGQUIT should exist");
      assertNotNull(WasiSignal.valueOf("SIGKILL"), "SIGKILL should exist");
      assertNotNull(WasiSignal.valueOf("SIGTERM"), "SIGTERM should exist");
      assertNotNull(WasiSignal.valueOf("SIGSTOP"), "SIGSTOP should exist");
    }

    @Test
    @DisplayName("should have getCode method")
    void shouldHaveGetCodeMethod() throws NoSuchMethodException {
      Method method = WasiSignal.class.getMethod("getCode");

      assertNotNull(method, "getCode method should exist");
      assertEquals(int.class, method.getReturnType(), "Return type should be int");
    }

    @Test
    @DisplayName("should have fromCode static method")
    void shouldHaveFromCodeMethod() throws NoSuchMethodException {
      Method method = WasiSignal.class.getMethod("fromCode", int.class);

      assertNotNull(method, "fromCode method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "fromCode should be static");
      assertEquals(WasiSignal.class, method.getReturnType(), "Return type should be WasiSignal");
    }

    @Test
    @DisplayName("should return correct codes for signals")
    void shouldReturnCorrectCodesForSignals() {
      assertEquals(1, WasiSignal.SIGHUP.getCode(), "SIGHUP should be 1");
      assertEquals(2, WasiSignal.SIGINT.getCode(), "SIGINT should be 2");
      assertEquals(9, WasiSignal.SIGKILL.getCode(), "SIGKILL should be 9");
      assertEquals(15, WasiSignal.SIGTERM.getCode(), "SIGTERM should be 15");
    }

    @Test
    @DisplayName("should convert from code correctly")
    void shouldConvertFromCodeCorrectly() {
      assertEquals(WasiSignal.SIGHUP, WasiSignal.fromCode(1), "fromCode(1) should return SIGHUP");
      assertEquals(WasiSignal.SIGINT, WasiSignal.fromCode(2), "fromCode(2) should return SIGINT");
      assertEquals(WasiSignal.SIGKILL, WasiSignal.fromCode(9), "fromCode(9) should return SIGKILL");
      assertEquals(WasiSignal.SIGTERM, WasiSignal.fromCode(15), "fromCode(15) should return SIGTERM");
    }

    @Test
    @DisplayName("should throw exception for invalid code")
    void shouldThrowExceptionForInvalidCode() {
      assertThrows(
          IllegalArgumentException.class,
          () -> WasiSignal.fromCode(999),
          "Should throw for invalid code");
    }

    @Test
    @DisplayName("should have unique codes for all signals")
    void shouldHaveUniqueCodesForAllSignals() {
      Set<Integer> codes = new HashSet<>();
      for (WasiSignal signal : WasiSignal.values()) {
        assertTrue(codes.add(signal.getCode()), "Signal code " + signal.getCode() + " should be unique");
      }
    }
  }

  // ========================================================================
  // WasiPreview2Context Interface Tests
  // ========================================================================

  @Nested
  @DisplayName("WasiPreview2Context Interface Tests")
  class WasiPreview2ContextTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeAnInterface() {
      assertTrue(
          WasiPreview2Context.class.isInterface(), "WasiPreview2Context should be an interface");
    }

    @Test
    @DisplayName("should extend AutoCloseable")
    void shouldExtendAutoCloseable() {
      assertTrue(
          AutoCloseable.class.isAssignableFrom(WasiPreview2Context.class),
          "WasiPreview2Context should extend AutoCloseable");
    }

    @Test
    @DisplayName("should have create static method")
    void shouldHaveCreateMethod() throws NoSuchMethodException {
      Method method = WasiPreview2Context.class.getMethod("create", WasiConfig.class);

      assertNotNull(method, "create method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "create should be static");
      assertEquals(
          WasiPreview2Context.class,
          method.getReturnType(),
          "Return type should be WasiPreview2Context");
    }

    @Test
    @DisplayName("should have createResource method")
    void shouldHaveCreateResourceMethod() throws NoSuchMethodException {
      Method method =
          WasiPreview2Context.class.getMethod("createResource", String.class, ByteBuffer.class);

      assertNotNull(method, "createResource method should exist");
      assertEquals(long.class, method.getReturnType(), "Return type should be long");
    }

    @Test
    @DisplayName("should have destroyResource method")
    void shouldHaveDestroyResourceMethod() throws NoSuchMethodException {
      Method method = WasiPreview2Context.class.getMethod("destroyResource", long.class);

      assertNotNull(method, "destroyResource method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
    }

    @Test
    @DisplayName("should have openInputStream method")
    void shouldHaveOpenInputStreamMethod() throws NoSuchMethodException {
      Method method = WasiPreview2Context.class.getMethod("openInputStream", long.class);

      assertNotNull(method, "openInputStream method should exist");
      assertEquals(
          WasiPreview2Stream.class,
          method.getReturnType(),
          "Return type should be WasiPreview2Stream");
    }

    @Test
    @DisplayName("should have openOutputStream method")
    void shouldHaveOpenOutputStreamMethod() throws NoSuchMethodException {
      Method method = WasiPreview2Context.class.getMethod("openOutputStream", long.class);

      assertNotNull(method, "openOutputStream method should exist");
      assertEquals(
          WasiPreview2Stream.class,
          method.getReturnType(),
          "Return type should be WasiPreview2Stream");
    }

    @Test
    @DisplayName("should have openBidirectionalStream method")
    void shouldHaveOpenBidirectionalStreamMethod() throws NoSuchMethodException {
      Method method = WasiPreview2Context.class.getMethod("openBidirectionalStream", long.class);

      assertNotNull(method, "openBidirectionalStream method should exist");
      assertEquals(
          WasiPreview2Stream.class,
          method.getReturnType(),
          "Return type should be WasiPreview2Stream");
    }

    @Test
    @DisplayName("should have createTcpSocket method")
    void shouldHaveCreateTcpSocketMethod() throws NoSuchMethodException {
      Method method = WasiPreview2Context.class.getMethod("createTcpSocket", int.class);

      assertNotNull(method, "createTcpSocket method should exist");
      assertEquals(long.class, method.getReturnType(), "Return type should be long");
    }

    @Test
    @DisplayName("should have connectTcpAsync method")
    void shouldHaveConnectTcpAsyncMethod() throws NoSuchMethodException {
      Method method =
          WasiPreview2Context.class.getMethod(
              "connectTcpAsync", long.class, String.class, int.class);

      assertNotNull(method, "connectTcpAsync method should exist");
      assertEquals(
          CompletableFuture.class,
          method.getReturnType(),
          "Return type should be CompletableFuture");
    }

    @Test
    @DisplayName("should have createUdpSocket method")
    void shouldHaveCreateUdpSocketMethod() throws NoSuchMethodException {
      Method method = WasiPreview2Context.class.getMethod("createUdpSocket", int.class);

      assertNotNull(method, "createUdpSocket method should exist");
      assertEquals(long.class, method.getReturnType(), "Return type should be long");
    }

    @Test
    @DisplayName("should have sendUdpAsync method")
    void shouldHaveSendUdpAsyncMethod() throws NoSuchMethodException {
      Method method =
          WasiPreview2Context.class.getMethod(
              "sendUdpAsync", long.class, ByteBuffer.class, String.class, int.class);

      assertNotNull(method, "sendUdpAsync method should exist");
      assertEquals(
          CompletableFuture.class,
          method.getReturnType(),
          "Return type should be CompletableFuture");
    }

    @Test
    @DisplayName("should have httpRequestAsync method")
    void shouldHaveHttpRequestAsyncMethod() throws NoSuchMethodException {
      Method method =
          WasiPreview2Context.class.getMethod(
              "httpRequestAsync", String.class, String.class, Map.class, ByteBuffer.class);

      assertNotNull(method, "httpRequestAsync method should exist");
      assertEquals(
          CompletableFuture.class,
          method.getReturnType(),
          "Return type should be CompletableFuture");
    }

    @Test
    @DisplayName("should have openFileAsync method")
    void shouldHaveOpenFileAsyncMethod() throws NoSuchMethodException {
      Method method =
          WasiPreview2Context.class.getMethod(
              "openFileAsync", String.class, int.class, long.class);

      assertNotNull(method, "openFileAsync method should exist");
      assertEquals(
          CompletableFuture.class,
          method.getReturnType(),
          "Return type should be CompletableFuture");
    }

    @Test
    @DisplayName("should have readFileAsync method")
    void shouldHaveReadFileAsyncMethod() throws NoSuchMethodException {
      Method method =
          WasiPreview2Context.class.getMethod(
              "readFileAsync", int.class, ByteBuffer.class, long.class);

      assertNotNull(method, "readFileAsync method should exist");
      assertEquals(
          CompletableFuture.class,
          method.getReturnType(),
          "Return type should be CompletableFuture");
    }

    @Test
    @DisplayName("should have writeFileAsync method")
    void shouldHaveWriteFileAsyncMethod() throws NoSuchMethodException {
      Method method =
          WasiPreview2Context.class.getMethod(
              "writeFileAsync", int.class, ByteBuffer.class, long.class);

      assertNotNull(method, "writeFileAsync method should exist");
      assertEquals(
          CompletableFuture.class,
          method.getReturnType(),
          "Return type should be CompletableFuture");
    }

    @Test
    @DisplayName("should have getTimeAsync method")
    void shouldHaveGetTimeAsyncMethod() throws NoSuchMethodException {
      Method method =
          WasiPreview2Context.class.getMethod("getTimeAsync", int.class, long.class);

      assertNotNull(method, "getTimeAsync method should exist");
      assertEquals(
          CompletableFuture.class,
          method.getReturnType(),
          "Return type should be CompletableFuture");
    }

    @Test
    @DisplayName("should have getRandomBytesAsync method")
    void shouldHaveGetRandomBytesAsyncMethod() throws NoSuchMethodException {
      Method method =
          WasiPreview2Context.class.getMethod("getRandomBytesAsync", ByteBuffer.class);

      assertNotNull(method, "getRandomBytesAsync method should exist");
      assertEquals(
          CompletableFuture.class,
          method.getReturnType(),
          "Return type should be CompletableFuture");
    }

    @Test
    @DisplayName("should have poll method")
    void shouldHavePollMethod() throws NoSuchMethodException {
      Method method =
          WasiPreview2Context.class.getMethod("poll", List.class, long.class);

      assertNotNull(method, "poll method should exist");
      assertEquals(List.class, method.getReturnType(), "Return type should be List");
    }

    @Test
    @DisplayName("should have createPollable method")
    void shouldHaveCreatePollableMethod() throws NoSuchMethodException {
      Method method = WasiPreview2Context.class.getMethod("createPollable", long.class);

      assertNotNull(method, "createPollable method should exist");
      assertEquals(long.class, method.getReturnType(), "Return type should be long");
    }

    @Test
    @DisplayName("should have getConfig method")
    void shouldHaveGetConfigMethod() throws NoSuchMethodException {
      Method method = WasiPreview2Context.class.getMethod("getConfig");

      assertNotNull(method, "getConfig method should exist");
      assertEquals(WasiConfig.class, method.getReturnType(), "Return type should be WasiConfig");
    }

    @Test
    @DisplayName("should have getPerformanceMetrics method")
    void shouldHaveGetPerformanceMetricsMethod() throws NoSuchMethodException {
      Method method = WasiPreview2Context.class.getMethod("getPerformanceMetrics");

      assertNotNull(method, "getPerformanceMetrics method should exist");
      assertEquals(
          WasiPerformanceMetrics.class,
          method.getReturnType(),
          "Return type should be WasiPerformanceMetrics");
    }

    @Test
    @DisplayName("should have close method")
    void shouldHaveCloseMethod() throws NoSuchMethodException {
      Method method = WasiPreview2Context.class.getMethod("close");

      assertNotNull(method, "close method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
    }
  }

  // ========================================================================
  // WasiPreview2Context.WasiHttpResponse Inner Interface Tests
  // ========================================================================

  @Nested
  @DisplayName("WasiHttpResponse Inner Interface Tests")
  class WasiHttpResponseTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeAnInterface() {
      assertTrue(
          WasiPreview2Context.WasiHttpResponse.class.isInterface(),
          "WasiHttpResponse should be an interface");
    }

    @Test
    @DisplayName("should be nested in WasiPreview2Context")
    void shouldBeNestedInWasiPreview2Context() {
      assertEquals(
          WasiPreview2Context.class,
          WasiPreview2Context.WasiHttpResponse.class.getEnclosingClass(),
          "WasiHttpResponse should be nested in WasiPreview2Context");
    }

    @Test
    @DisplayName("should have getStatusCode method")
    void shouldHaveGetStatusCodeMethod() throws NoSuchMethodException {
      Method method = WasiPreview2Context.WasiHttpResponse.class.getMethod("getStatusCode");

      assertNotNull(method, "getStatusCode method should exist");
      assertEquals(int.class, method.getReturnType(), "Return type should be int");
    }

    @Test
    @DisplayName("should have getHeaders method")
    void shouldHaveGetHeadersMethod() throws NoSuchMethodException {
      Method method = WasiPreview2Context.WasiHttpResponse.class.getMethod("getHeaders");

      assertNotNull(method, "getHeaders method should exist");
      assertEquals(Map.class, method.getReturnType(), "Return type should be Map");
    }

    @Test
    @DisplayName("should have getBody method")
    void shouldHaveGetBodyMethod() throws NoSuchMethodException {
      Method method = WasiPreview2Context.WasiHttpResponse.class.getMethod("getBody");

      assertNotNull(method, "getBody method should exist");
      assertEquals(ByteBuffer.class, method.getReturnType(), "Return type should be ByteBuffer");
    }
  }

  // ========================================================================
  // WasiPreview2Stream Interface Tests
  // ========================================================================

  @Nested
  @DisplayName("WasiPreview2Stream Interface Tests")
  class WasiPreview2StreamTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeAnInterface() {
      assertTrue(
          WasiPreview2Stream.class.isInterface(), "WasiPreview2Stream should be an interface");
    }

    @Test
    @DisplayName("should extend AutoCloseable")
    void shouldExtendAutoCloseable() {
      assertTrue(
          AutoCloseable.class.isAssignableFrom(WasiPreview2Stream.class),
          "WasiPreview2Stream should extend AutoCloseable");
    }

    @Test
    @DisplayName("should have getStreamId method")
    void shouldHaveGetStreamIdMethod() throws NoSuchMethodException {
      Method method = WasiPreview2Stream.class.getMethod("getStreamId");

      assertNotNull(method, "getStreamId method should exist");
      assertEquals(long.class, method.getReturnType(), "Return type should be long");
    }

    @Test
    @DisplayName("should have getStreamType method")
    void shouldHaveGetStreamTypeMethod() throws NoSuchMethodException {
      Method method = WasiPreview2Stream.class.getMethod("getStreamType");

      assertNotNull(method, "getStreamType method should exist");
      assertEquals(
          WasiPreview2Stream.WasiStreamType.class,
          method.getReturnType(),
          "Return type should be WasiStreamType");
    }

    @Test
    @DisplayName("should have isReady method")
    void shouldHaveIsReadyMethod() throws NoSuchMethodException {
      Method method = WasiPreview2Stream.class.getMethod("isReady");

      assertNotNull(method, "isReady method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Return type should be boolean");
    }

    @Test
    @DisplayName("should have isClosed method")
    void shouldHaveIsClosedMethod() throws NoSuchMethodException {
      Method method = WasiPreview2Stream.class.getMethod("isClosed");

      assertNotNull(method, "isClosed method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Return type should be boolean");
    }

    @Test
    @DisplayName("should have getStatus method")
    void shouldHaveGetStatusMethod() throws NoSuchMethodException {
      Method method = WasiPreview2Stream.class.getMethod("getStatus");

      assertNotNull(method, "getStatus method should exist");
      assertEquals(
          WasiPreview2Stream.WasiStreamStatus.class,
          method.getReturnType(),
          "Return type should be WasiStreamStatus");
    }

    @Test
    @DisplayName("should have readAsync method")
    void shouldHaveReadAsyncMethod() throws NoSuchMethodException {
      Method method = WasiPreview2Stream.class.getMethod("readAsync", ByteBuffer.class);

      assertNotNull(method, "readAsync method should exist");
      assertEquals(
          CompletableFuture.class,
          method.getReturnType(),
          "Return type should be CompletableFuture");
    }

    @Test
    @DisplayName("should have writeAsync method")
    void shouldHaveWriteAsyncMethod() throws NoSuchMethodException {
      Method method = WasiPreview2Stream.class.getMethod("writeAsync", ByteBuffer.class);

      assertNotNull(method, "writeAsync method should exist");
      assertEquals(
          CompletableFuture.class,
          method.getReturnType(),
          "Return type should be CompletableFuture");
    }

    @Test
    @DisplayName("should have flushAsync method")
    void shouldHaveFlushAsyncMethod() throws NoSuchMethodException {
      Method method = WasiPreview2Stream.class.getMethod("flushAsync");

      assertNotNull(method, "flushAsync method should exist");
      assertEquals(
          CompletableFuture.class,
          method.getReturnType(),
          "Return type should be CompletableFuture");
    }

    @Test
    @DisplayName("should have createPollable method")
    void shouldHaveCreatePollableMethod() throws NoSuchMethodException {
      Method method = WasiPreview2Stream.class.getMethod("createPollable");

      assertNotNull(method, "createPollable method should exist");
      assertEquals(long.class, method.getReturnType(), "Return type should be long");
    }

    @Test
    @DisplayName("should have close method")
    void shouldHaveCloseMethod() throws NoSuchMethodException {
      Method method = WasiPreview2Stream.class.getMethod("close");

      assertNotNull(method, "close method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
    }
  }

  // ========================================================================
  // WasiPreview2Stream.WasiStreamType Enum Tests
  // ========================================================================

  @Nested
  @DisplayName("WasiStreamType Enum Tests")
  class WasiStreamTypeEnumTests {

    @Test
    @DisplayName("should be an enum")
    void shouldBeAnEnum() {
      assertTrue(
          WasiPreview2Stream.WasiStreamType.class.isEnum(),
          "WasiStreamType should be an enum");
    }

    @Test
    @DisplayName("should have exactly 3 values")
    void shouldHaveExactly3Values() {
      WasiPreview2Stream.WasiStreamType[] values = WasiPreview2Stream.WasiStreamType.values();

      assertEquals(3, values.length, "Should have exactly 3 stream type values");
    }

    @Test
    @DisplayName("should have INPUT value")
    void shouldHaveInputValue() {
      WasiPreview2Stream.WasiStreamType type = WasiPreview2Stream.WasiStreamType.INPUT;

      assertNotNull(type, "INPUT should exist");
      assertEquals("INPUT", type.name(), "Name should be INPUT");
    }

    @Test
    @DisplayName("should have OUTPUT value")
    void shouldHaveOutputValue() {
      WasiPreview2Stream.WasiStreamType type = WasiPreview2Stream.WasiStreamType.OUTPUT;

      assertNotNull(type, "OUTPUT should exist");
      assertEquals("OUTPUT", type.name(), "Name should be OUTPUT");
    }

    @Test
    @DisplayName("should have BIDIRECTIONAL value")
    void shouldHaveBidirectionalValue() {
      WasiPreview2Stream.WasiStreamType type = WasiPreview2Stream.WasiStreamType.BIDIRECTIONAL;

      assertNotNull(type, "BIDIRECTIONAL should exist");
      assertEquals("BIDIRECTIONAL", type.name(), "Name should be BIDIRECTIONAL");
    }

    @Test
    @DisplayName("should be accessible via valueOf")
    void shouldBeAccessibleViaValueOf() {
      assertEquals(
          WasiPreview2Stream.WasiStreamType.INPUT,
          WasiPreview2Stream.WasiStreamType.valueOf("INPUT"));
      assertEquals(
          WasiPreview2Stream.WasiStreamType.OUTPUT,
          WasiPreview2Stream.WasiStreamType.valueOf("OUTPUT"));
      assertEquals(
          WasiPreview2Stream.WasiStreamType.BIDIRECTIONAL,
          WasiPreview2Stream.WasiStreamType.valueOf("BIDIRECTIONAL"));
    }

    @Test
    @DisplayName("should have correct ordinals")
    void shouldHaveCorrectOrdinals() {
      assertEquals(0, WasiPreview2Stream.WasiStreamType.INPUT.ordinal(), "INPUT should be 0");
      assertEquals(1, WasiPreview2Stream.WasiStreamType.OUTPUT.ordinal(), "OUTPUT should be 1");
      assertEquals(
          2, WasiPreview2Stream.WasiStreamType.BIDIRECTIONAL.ordinal(), "BIDIRECTIONAL should be 2");
    }
  }

  // ========================================================================
  // WasiPreview2Stream.WasiStreamStatus Enum Tests
  // ========================================================================

  @Nested
  @DisplayName("WasiStreamStatus Enum Tests")
  class WasiStreamStatusEnumTests {

    @Test
    @DisplayName("should be an enum")
    void shouldBeAnEnum() {
      assertTrue(
          WasiPreview2Stream.WasiStreamStatus.class.isEnum(),
          "WasiStreamStatus should be an enum");
    }

    @Test
    @DisplayName("should have exactly 5 values")
    void shouldHaveExactly5Values() {
      WasiPreview2Stream.WasiStreamStatus[] values = WasiPreview2Stream.WasiStreamStatus.values();

      assertEquals(5, values.length, "Should have exactly 5 stream status values");
    }

    @Test
    @DisplayName("should have READY value")
    void shouldHaveReadyValue() {
      WasiPreview2Stream.WasiStreamStatus status = WasiPreview2Stream.WasiStreamStatus.READY;

      assertNotNull(status, "READY should exist");
      assertEquals("READY", status.name(), "Name should be READY");
    }

    @Test
    @DisplayName("should have BLOCKED value")
    void shouldHaveBlockedValue() {
      WasiPreview2Stream.WasiStreamStatus status = WasiPreview2Stream.WasiStreamStatus.BLOCKED;

      assertNotNull(status, "BLOCKED should exist");
      assertEquals("BLOCKED", status.name(), "Name should be BLOCKED");
    }

    @Test
    @DisplayName("should have EOF value")
    void shouldHaveEofValue() {
      WasiPreview2Stream.WasiStreamStatus status = WasiPreview2Stream.WasiStreamStatus.EOF;

      assertNotNull(status, "EOF should exist");
      assertEquals("EOF", status.name(), "Name should be EOF");
    }

    @Test
    @DisplayName("should have CLOSED value")
    void shouldHaveClosedValue() {
      WasiPreview2Stream.WasiStreamStatus status = WasiPreview2Stream.WasiStreamStatus.CLOSED;

      assertNotNull(status, "CLOSED should exist");
      assertEquals("CLOSED", status.name(), "Name should be CLOSED");
    }

    @Test
    @DisplayName("should have ERROR value")
    void shouldHaveErrorValue() {
      WasiPreview2Stream.WasiStreamStatus status = WasiPreview2Stream.WasiStreamStatus.ERROR;

      assertNotNull(status, "ERROR should exist");
      assertEquals("ERROR", status.name(), "Name should be ERROR");
    }

    @Test
    @DisplayName("should be accessible via valueOf")
    void shouldBeAccessibleViaValueOf() {
      assertEquals(
          WasiPreview2Stream.WasiStreamStatus.READY,
          WasiPreview2Stream.WasiStreamStatus.valueOf("READY"));
      assertEquals(
          WasiPreview2Stream.WasiStreamStatus.BLOCKED,
          WasiPreview2Stream.WasiStreamStatus.valueOf("BLOCKED"));
      assertEquals(
          WasiPreview2Stream.WasiStreamStatus.EOF,
          WasiPreview2Stream.WasiStreamStatus.valueOf("EOF"));
      assertEquals(
          WasiPreview2Stream.WasiStreamStatus.CLOSED,
          WasiPreview2Stream.WasiStreamStatus.valueOf("CLOSED"));
      assertEquals(
          WasiPreview2Stream.WasiStreamStatus.ERROR,
          WasiPreview2Stream.WasiStreamStatus.valueOf("ERROR"));
    }

    @Test
    @DisplayName("should throw IllegalArgumentException for invalid valueOf")
    void shouldThrowIllegalArgumentExceptionForInvalidValueOf() {
      assertThrows(
          IllegalArgumentException.class,
          () -> WasiPreview2Stream.WasiStreamStatus.valueOf("INVALID"),
          "Should throw for invalid enum name");
    }

    @Test
    @DisplayName("should have correct ordinals")
    void shouldHaveCorrectOrdinals() {
      assertEquals(0, WasiPreview2Stream.WasiStreamStatus.READY.ordinal(), "READY should be 0");
      assertEquals(1, WasiPreview2Stream.WasiStreamStatus.BLOCKED.ordinal(), "BLOCKED should be 1");
      assertEquals(2, WasiPreview2Stream.WasiStreamStatus.EOF.ordinal(), "EOF should be 2");
      assertEquals(3, WasiPreview2Stream.WasiStreamStatus.CLOSED.ordinal(), "CLOSED should be 3");
      assertEquals(4, WasiPreview2Stream.WasiStreamStatus.ERROR.ordinal(), "ERROR should be 4");
    }

    @Test
    @DisplayName("should be usable in switch statement")
    void shouldBeUsableInSwitchStatement() {
      for (WasiPreview2Stream.WasiStreamStatus status :
          WasiPreview2Stream.WasiStreamStatus.values()) {
        String result;
        switch (status) {
          case READY:
            result = "ready";
            break;
          case BLOCKED:
            result = "blocked";
            break;
          case EOF:
            result = "eof";
            break;
          case CLOSED:
            result = "closed";
            break;
          case ERROR:
            result = "error";
            break;
          default:
            result = "unknown";
        }
        assertTrue(
            Arrays.asList("ready", "blocked", "eof", "closed", "error").contains(result),
            "Switch should handle all statuses");
      }
    }

    @Test
    @DisplayName("should represent terminal states correctly")
    void shouldRepresentTerminalStatesCorrectly() {
      Set<WasiPreview2Stream.WasiStreamStatus> terminalStates = new HashSet<>();
      terminalStates.add(WasiPreview2Stream.WasiStreamStatus.EOF);
      terminalStates.add(WasiPreview2Stream.WasiStreamStatus.CLOSED);
      terminalStates.add(WasiPreview2Stream.WasiStreamStatus.ERROR);

      assertEquals(3, terminalStates.size(), "Should have 3 terminal states");
    }

    @Test
    @DisplayName("should represent active states correctly")
    void shouldRepresentActiveStatesCorrectly() {
      Set<WasiPreview2Stream.WasiStreamStatus> activeStates = new HashSet<>();
      activeStates.add(WasiPreview2Stream.WasiStreamStatus.READY);
      activeStates.add(WasiPreview2Stream.WasiStreamStatus.BLOCKED);

      assertEquals(2, activeStates.size(), "Should have 2 active states");
    }
  }

  // ========================================================================
  // Cross-Interface Relationship Tests
  // ========================================================================

  @Nested
  @DisplayName("Cross-Interface Relationship Tests")
  class CrossInterfaceRelationshipTests {

    @Test
    @DisplayName("WasiProcess should use WasiProcessId")
    void wasiProcessShouldUseWasiProcessId() throws NoSuchMethodException {
      Method spawnMethod = WasiProcess.class.getMethod("spawn", WasiProcessConfig.class);
      Method waitForMethod = WasiProcess.class.getMethod("waitFor", WasiProcessId.class);
      Method killMethod = WasiProcess.class.getMethod("kill", WasiProcessId.class, WasiSignal.class);

      assertEquals(
          WasiProcessId.class,
          spawnMethod.getReturnType(),
          "spawn should return WasiProcessId");
      assertEquals(
          WasiProcessId.class,
          waitForMethod.getParameterTypes()[0],
          "waitFor should take WasiProcessId");
      assertEquals(
          WasiProcessId.class,
          killMethod.getParameterTypes()[0],
          "kill should take WasiProcessId");
    }

    @Test
    @DisplayName("WasiProcess should use WasiProcessConfig")
    void wasiProcessShouldUseWasiProcessConfig() throws NoSuchMethodException {
      Method spawnMethod = WasiProcess.class.getMethod("spawn", WasiProcessConfig.class);

      assertEquals(
          WasiProcessConfig.class,
          spawnMethod.getParameterTypes()[0],
          "spawn should take WasiProcessConfig");
    }

    @Test
    @DisplayName("WasiProcess should use WasiSignal")
    void wasiProcessShouldUseWasiSignal() throws NoSuchMethodException {
      Method killMethod = WasiProcess.class.getMethod("kill", WasiProcessId.class, WasiSignal.class);

      assertEquals(
          WasiSignal.class, killMethod.getParameterTypes()[1], "kill should take WasiSignal");
    }

    @Test
    @DisplayName("WasiProcessConfig should use WasiResourceLimits")
    void wasiProcessConfigShouldUseWasiResourceLimits() throws NoSuchMethodException {
      Method getLimitsMethod = WasiProcessConfig.class.getMethod("getResourceLimits");
      Method setLimitsMethod =
          WasiProcessConfig.Builder.class.getMethod("setResourceLimits", WasiResourceLimits.class);

      assertEquals(
          WasiResourceLimits.class,
          getLimitsMethod.getReturnType(),
          "getResourceLimits should return WasiResourceLimits");
      assertEquals(
          WasiResourceLimits.class,
          setLimitsMethod.getParameterTypes()[0],
          "setResourceLimits should take WasiResourceLimits");
    }

    @Test
    @DisplayName("WasiPreview2Context should use WasiConfig")
    void wasiPreview2ContextShouldUseWasiConfig() throws NoSuchMethodException {
      Method createMethod = WasiPreview2Context.class.getMethod("create", WasiConfig.class);
      Method getConfigMethod = WasiPreview2Context.class.getMethod("getConfig");

      assertEquals(
          WasiConfig.class, createMethod.getParameterTypes()[0], "create should take WasiConfig");
      assertEquals(
          WasiConfig.class, getConfigMethod.getReturnType(), "getConfig should return WasiConfig");
    }

    @Test
    @DisplayName("WasiPreview2Context should use WasiPreview2Stream")
    void wasiPreview2ContextShouldUseWasiPreview2Stream() throws NoSuchMethodException {
      Method inputMethod = WasiPreview2Context.class.getMethod("openInputStream", long.class);
      Method outputMethod = WasiPreview2Context.class.getMethod("openOutputStream", long.class);
      Method bidiMethod = WasiPreview2Context.class.getMethod("openBidirectionalStream", long.class);

      assertEquals(
          WasiPreview2Stream.class,
          inputMethod.getReturnType(),
          "openInputStream should return WasiPreview2Stream");
      assertEquals(
          WasiPreview2Stream.class,
          outputMethod.getReturnType(),
          "openOutputStream should return WasiPreview2Stream");
      assertEquals(
          WasiPreview2Stream.class,
          bidiMethod.getReturnType(),
          "openBidirectionalStream should return WasiPreview2Stream");
    }

    @Test
    @DisplayName("WasiPreview2Context should use WasiPerformanceMetrics")
    void wasiPreview2ContextShouldUseWasiPerformanceMetrics() throws NoSuchMethodException {
      Method metricsMethod = WasiPreview2Context.class.getMethod("getPerformanceMetrics");

      assertEquals(
          WasiPerformanceMetrics.class,
          metricsMethod.getReturnType(),
          "getPerformanceMetrics should return WasiPerformanceMetrics");
    }
  }

  // ========================================================================
  // Interface Completeness Tests
  // ========================================================================

  @Nested
  @DisplayName("Interface Completeness Tests")
  class InterfaceCompletenessTests {

    @Test
    @DisplayName("WasiProcess should have all lifecycle methods")
    void wasiProcessShouldHaveAllLifecycleMethods() throws NoSuchMethodException {
      assertNotNull(WasiProcess.class.getMethod("spawn", WasiProcessConfig.class));
      assertNotNull(WasiProcess.class.getMethod("waitFor", WasiProcessId.class));
      assertNotNull(WasiProcess.class.getMethod("kill", WasiProcessId.class, WasiSignal.class));
      assertNotNull(WasiProcess.class.getMethod("isProcessRunning", WasiProcessId.class));
      assertNotNull(WasiProcess.class.getMethod("getProcessExitCode", WasiProcessId.class));
      assertNotNull(WasiProcess.class.getMethod("exit", int.class));
      assertNotNull(WasiProcess.class.getMethod("abort"));
    }

    @Test
    @DisplayName("WasiProcess should have all environment methods")
    void wasiProcessShouldHaveAllEnvironmentMethods() throws NoSuchMethodException {
      assertNotNull(WasiProcess.class.getMethod("getEnvironment"));
      assertNotNull(WasiProcess.class.getMethod("getEnvironmentVariable", String.class));
      assertNotNull(
          WasiProcess.class.getMethod("setEnvironmentVariable", String.class, String.class));
      assertNotNull(WasiProcess.class.getMethod("getCommandLineArguments"));
    }

    @Test
    @DisplayName("WasiProcess should have all directory methods")
    void wasiProcessShouldHaveAllDirectoryMethods() throws NoSuchMethodException {
      assertNotNull(WasiProcess.class.getMethod("getCurrentWorkingDirectory"));
      assertNotNull(WasiProcess.class.getMethod("setCurrentWorkingDirectory", String.class));
    }

    @Test
    @DisplayName("WasiPreview2Context should have all stream methods")
    void wasiPreview2ContextShouldHaveAllStreamMethods() throws NoSuchMethodException {
      assertNotNull(WasiPreview2Context.class.getMethod("openInputStream", long.class));
      assertNotNull(WasiPreview2Context.class.getMethod("openOutputStream", long.class));
      assertNotNull(WasiPreview2Context.class.getMethod("openBidirectionalStream", long.class));
    }

    @Test
    @DisplayName("WasiPreview2Context should have all network methods")
    void wasiPreview2ContextShouldHaveAllNetworkMethods() throws NoSuchMethodException {
      assertNotNull(WasiPreview2Context.class.getMethod("createTcpSocket", int.class));
      assertNotNull(
          WasiPreview2Context.class.getMethod(
              "connectTcpAsync", long.class, String.class, int.class));
      assertNotNull(WasiPreview2Context.class.getMethod("createUdpSocket", int.class));
      assertNotNull(
          WasiPreview2Context.class.getMethod(
              "sendUdpAsync", long.class, ByteBuffer.class, String.class, int.class));
      assertNotNull(
          WasiPreview2Context.class.getMethod(
              "httpRequestAsync", String.class, String.class, Map.class, ByteBuffer.class));
    }

    @Test
    @DisplayName("WasiPreview2Context should have all file methods")
    void wasiPreview2ContextShouldHaveAllFileMethods() throws NoSuchMethodException {
      assertNotNull(
          WasiPreview2Context.class.getMethod(
              "openFileAsync", String.class, int.class, long.class));
      assertNotNull(
          WasiPreview2Context.class.getMethod(
              "readFileAsync", int.class, ByteBuffer.class, long.class));
      assertNotNull(
          WasiPreview2Context.class.getMethod(
              "writeFileAsync", int.class, ByteBuffer.class, long.class));
    }

    @Test
    @DisplayName("WasiPreview2Context should have all clock and random methods")
    void wasiPreview2ContextShouldHaveAllClockAndRandomMethods() throws NoSuchMethodException {
      assertNotNull(WasiPreview2Context.class.getMethod("getTimeAsync", int.class, long.class));
      assertNotNull(WasiPreview2Context.class.getMethod("getRandomBytesAsync", ByteBuffer.class));
    }

    @Test
    @DisplayName("WasiPreview2Context should have all polling methods")
    void wasiPreview2ContextShouldHaveAllPollingMethods() throws NoSuchMethodException {
      assertNotNull(WasiPreview2Context.class.getMethod("poll", List.class, long.class));
      assertNotNull(WasiPreview2Context.class.getMethod("createPollable", long.class));
    }

    @Test
    @DisplayName("WasiPreview2Stream should have all async methods")
    void wasiPreview2StreamShouldHaveAllAsyncMethods() throws NoSuchMethodException {
      assertNotNull(WasiPreview2Stream.class.getMethod("readAsync", ByteBuffer.class));
      assertNotNull(WasiPreview2Stream.class.getMethod("writeAsync", ByteBuffer.class));
      assertNotNull(WasiPreview2Stream.class.getMethod("flushAsync"));
    }

    @Test
    @DisplayName("WasiPreview2Stream should have all status methods")
    void wasiPreview2StreamShouldHaveAllStatusMethods() throws NoSuchMethodException {
      assertNotNull(WasiPreview2Stream.class.getMethod("getStreamId"));
      assertNotNull(WasiPreview2Stream.class.getMethod("getStreamType"));
      assertNotNull(WasiPreview2Stream.class.getMethod("isReady"));
      assertNotNull(WasiPreview2Stream.class.getMethod("isClosed"));
      assertNotNull(WasiPreview2Stream.class.getMethod("getStatus"));
    }
  }
}
