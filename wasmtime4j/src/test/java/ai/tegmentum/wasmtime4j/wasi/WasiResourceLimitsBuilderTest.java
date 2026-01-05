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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.time.Duration;
import java.util.Arrays;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive test suite for the WasiResourceLimitsBuilder interface.
 *
 * <p>WasiResourceLimitsBuilder provides a fluent API for configuring resource limits including
 * memory, CPU time, file handles, network connections, and other system resources.
 */
@DisplayName("WasiResourceLimitsBuilder Interface Tests")
class WasiResourceLimitsBuilderTest {

  // ========================================================================
  // Type Definition Tests
  // ========================================================================

  @Nested
  @DisplayName("Type Definition Tests")
  class TypeDefinitionTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeAnInterface() {
      assertTrue(
          WasiResourceLimitsBuilder.class.isInterface(),
          "WasiResourceLimitsBuilder should be an interface");
    }

    @Test
    @DisplayName("should be public")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(WasiResourceLimitsBuilder.class.getModifiers()),
          "WasiResourceLimitsBuilder should be public");
    }
  }

  // ========================================================================
  // Inheritance Tests
  // ========================================================================

  @Nested
  @DisplayName("Inheritance Tests")
  class InheritanceTests {

    @Test
    @DisplayName("should not extend any interfaces")
    void shouldNotExtendAnyInterfaces() {
      assertEquals(
          0,
          WasiResourceLimitsBuilder.class.getInterfaces().length,
          "WasiResourceLimitsBuilder should not extend any interfaces");
    }
  }

  // ========================================================================
  // Memory Limit Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Memory Limit Method Tests")
  class MemoryLimitMethodTests {

    @Test
    @DisplayName("should have withMemoryLimit method")
    void shouldHaveWithMemoryLimitMethod() throws NoSuchMethodException {
      Method method = WasiResourceLimitsBuilder.class.getMethod("withMemoryLimit", long.class);
      assertNotNull(method, "withMemoryLimit method should exist");
      assertEquals(
          WasiResourceLimitsBuilder.class,
          method.getReturnType(),
          "Should return WasiResourceLimitsBuilder for chaining");
    }

    @Test
    @DisplayName("should have withoutMemoryLimit method")
    void shouldHaveWithoutMemoryLimitMethod() throws NoSuchMethodException {
      Method method = WasiResourceLimitsBuilder.class.getMethod("withoutMemoryLimit");
      assertNotNull(method, "withoutMemoryLimit method should exist");
      assertEquals(
          WasiResourceLimitsBuilder.class,
          method.getReturnType(),
          "Should return WasiResourceLimitsBuilder for chaining");
    }
  }

  // ========================================================================
  // Execution Timeout Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Execution Timeout Method Tests")
  class ExecutionTimeoutMethodTests {

    @Test
    @DisplayName("should have withExecutionTimeout method")
    void shouldHaveWithExecutionTimeoutMethod() throws NoSuchMethodException {
      Method method =
          WasiResourceLimitsBuilder.class.getMethod("withExecutionTimeout", Duration.class);
      assertNotNull(method, "withExecutionTimeout method should exist");
      assertEquals(
          WasiResourceLimitsBuilder.class,
          method.getReturnType(),
          "Should return WasiResourceLimitsBuilder for chaining");
    }

    @Test
    @DisplayName("should have withoutExecutionTimeout method")
    void shouldHaveWithoutExecutionTimeoutMethod() throws NoSuchMethodException {
      Method method = WasiResourceLimitsBuilder.class.getMethod("withoutExecutionTimeout");
      assertNotNull(method, "withoutExecutionTimeout method should exist");
      assertEquals(
          WasiResourceLimitsBuilder.class,
          method.getReturnType(),
          "Should return WasiResourceLimitsBuilder for chaining");
    }

    @Test
    @DisplayName("should have withTotalExecutionTimeout method")
    void shouldHaveWithTotalExecutionTimeoutMethod() throws NoSuchMethodException {
      Method method =
          WasiResourceLimitsBuilder.class.getMethod("withTotalExecutionTimeout", Duration.class);
      assertNotNull(method, "withTotalExecutionTimeout method should exist");
      assertEquals(
          WasiResourceLimitsBuilder.class,
          method.getReturnType(),
          "Should return WasiResourceLimitsBuilder for chaining");
    }

    @Test
    @DisplayName("should have withoutTotalExecutionTimeout method")
    void shouldHaveWithoutTotalExecutionTimeoutMethod() throws NoSuchMethodException {
      Method method = WasiResourceLimitsBuilder.class.getMethod("withoutTotalExecutionTimeout");
      assertNotNull(method, "withoutTotalExecutionTimeout method should exist");
      assertEquals(
          WasiResourceLimitsBuilder.class,
          method.getReturnType(),
          "Should return WasiResourceLimitsBuilder for chaining");
    }
  }

  // ========================================================================
  // File Handle Limit Method Tests
  // ========================================================================

  @Nested
  @DisplayName("File Handle Limit Method Tests")
  class FileHandleLimitMethodTests {

    @Test
    @DisplayName("should have withFileHandleLimit method")
    void shouldHaveWithFileHandleLimitMethod() throws NoSuchMethodException {
      Method method = WasiResourceLimitsBuilder.class.getMethod("withFileHandleLimit", int.class);
      assertNotNull(method, "withFileHandleLimit method should exist");
      assertEquals(
          WasiResourceLimitsBuilder.class,
          method.getReturnType(),
          "Should return WasiResourceLimitsBuilder for chaining");
    }

    @Test
    @DisplayName("should have withoutFileHandleLimit method")
    void shouldHaveWithoutFileHandleLimitMethod() throws NoSuchMethodException {
      Method method = WasiResourceLimitsBuilder.class.getMethod("withoutFileHandleLimit");
      assertNotNull(method, "withoutFileHandleLimit method should exist");
      assertEquals(
          WasiResourceLimitsBuilder.class,
          method.getReturnType(),
          "Should return WasiResourceLimitsBuilder for chaining");
    }
  }

  // ========================================================================
  // Network Limit Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Network Limit Method Tests")
  class NetworkLimitMethodTests {

    @Test
    @DisplayName("should have withNetworkConnectionLimit method")
    void shouldHaveWithNetworkConnectionLimitMethod() throws NoSuchMethodException {
      Method method =
          WasiResourceLimitsBuilder.class.getMethod("withNetworkConnectionLimit", int.class);
      assertNotNull(method, "withNetworkConnectionLimit method should exist");
      assertEquals(
          WasiResourceLimitsBuilder.class,
          method.getReturnType(),
          "Should return WasiResourceLimitsBuilder for chaining");
    }

    @Test
    @DisplayName("should have withoutNetworkConnectionLimit method")
    void shouldHaveWithoutNetworkConnectionLimitMethod() throws NoSuchMethodException {
      Method method = WasiResourceLimitsBuilder.class.getMethod("withoutNetworkConnectionLimit");
      assertNotNull(method, "withoutNetworkConnectionLimit method should exist");
      assertEquals(
          WasiResourceLimitsBuilder.class,
          method.getReturnType(),
          "Should return WasiResourceLimitsBuilder for chaining");
    }

    @Test
    @DisplayName("should have withNetworkSendLimit method")
    void shouldHaveWithNetworkSendLimitMethod() throws NoSuchMethodException {
      Method method = WasiResourceLimitsBuilder.class.getMethod("withNetworkSendLimit", long.class);
      assertNotNull(method, "withNetworkSendLimit method should exist");
      assertEquals(
          WasiResourceLimitsBuilder.class,
          method.getReturnType(),
          "Should return WasiResourceLimitsBuilder for chaining");
    }

    @Test
    @DisplayName("should have withoutNetworkSendLimit method")
    void shouldHaveWithoutNetworkSendLimitMethod() throws NoSuchMethodException {
      Method method = WasiResourceLimitsBuilder.class.getMethod("withoutNetworkSendLimit");
      assertNotNull(method, "withoutNetworkSendLimit method should exist");
      assertEquals(
          WasiResourceLimitsBuilder.class,
          method.getReturnType(),
          "Should return WasiResourceLimitsBuilder for chaining");
    }

    @Test
    @DisplayName("should have withNetworkReceiveLimit method")
    void shouldHaveWithNetworkReceiveLimitMethod() throws NoSuchMethodException {
      Method method =
          WasiResourceLimitsBuilder.class.getMethod("withNetworkReceiveLimit", long.class);
      assertNotNull(method, "withNetworkReceiveLimit method should exist");
      assertEquals(
          WasiResourceLimitsBuilder.class,
          method.getReturnType(),
          "Should return WasiResourceLimitsBuilder for chaining");
    }

    @Test
    @DisplayName("should have withoutNetworkReceiveLimit method")
    void shouldHaveWithoutNetworkReceiveLimitMethod() throws NoSuchMethodException {
      Method method = WasiResourceLimitsBuilder.class.getMethod("withoutNetworkReceiveLimit");
      assertNotNull(method, "withoutNetworkReceiveLimit method should exist");
      assertEquals(
          WasiResourceLimitsBuilder.class,
          method.getReturnType(),
          "Should return WasiResourceLimitsBuilder for chaining");
    }
  }

  // ========================================================================
  // Thread and Stack Limit Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Thread and Stack Limit Method Tests")
  class ThreadAndStackLimitMethodTests {

    @Test
    @DisplayName("should have withThreadLimit method")
    void shouldHaveWithThreadLimitMethod() throws NoSuchMethodException {
      Method method = WasiResourceLimitsBuilder.class.getMethod("withThreadLimit", int.class);
      assertNotNull(method, "withThreadLimit method should exist");
      assertEquals(
          WasiResourceLimitsBuilder.class,
          method.getReturnType(),
          "Should return WasiResourceLimitsBuilder for chaining");
    }

    @Test
    @DisplayName("should have withoutThreadLimit method")
    void shouldHaveWithoutThreadLimitMethod() throws NoSuchMethodException {
      Method method = WasiResourceLimitsBuilder.class.getMethod("withoutThreadLimit");
      assertNotNull(method, "withoutThreadLimit method should exist");
      assertEquals(
          WasiResourceLimitsBuilder.class,
          method.getReturnType(),
          "Should return WasiResourceLimitsBuilder for chaining");
    }

    @Test
    @DisplayName("should have withStackDepthLimit method")
    void shouldHaveWithStackDepthLimitMethod() throws NoSuchMethodException {
      Method method = WasiResourceLimitsBuilder.class.getMethod("withStackDepthLimit", int.class);
      assertNotNull(method, "withStackDepthLimit method should exist");
      assertEquals(
          WasiResourceLimitsBuilder.class,
          method.getReturnType(),
          "Should return WasiResourceLimitsBuilder for chaining");
    }

    @Test
    @DisplayName("should have withoutStackDepthLimit method")
    void shouldHaveWithoutStackDepthLimitMethod() throws NoSuchMethodException {
      Method method = WasiResourceLimitsBuilder.class.getMethod("withoutStackDepthLimit");
      assertNotNull(method, "withoutStackDepthLimit method should exist");
      assertEquals(
          WasiResourceLimitsBuilder.class,
          method.getReturnType(),
          "Should return WasiResourceLimitsBuilder for chaining");
    }
  }

  // ========================================================================
  // Resource Count Limit Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Resource Count Limit Method Tests")
  class ResourceCountLimitMethodTests {

    @Test
    @DisplayName("should have withResourceCountLimit method")
    void shouldHaveWithResourceCountLimitMethod() throws NoSuchMethodException {
      Method method =
          WasiResourceLimitsBuilder.class.getMethod("withResourceCountLimit", int.class);
      assertNotNull(method, "withResourceCountLimit method should exist");
      assertEquals(
          WasiResourceLimitsBuilder.class,
          method.getReturnType(),
          "Should return WasiResourceLimitsBuilder for chaining");
    }

    @Test
    @DisplayName("should have withoutResourceCountLimit method")
    void shouldHaveWithoutResourceCountLimitMethod() throws NoSuchMethodException {
      Method method = WasiResourceLimitsBuilder.class.getMethod("withoutResourceCountLimit");
      assertNotNull(method, "withoutResourceCountLimit method should exist");
      assertEquals(
          WasiResourceLimitsBuilder.class,
          method.getReturnType(),
          "Should return WasiResourceLimitsBuilder for chaining");
    }
  }

  // ========================================================================
  // File I/O Limit Method Tests
  // ========================================================================

  @Nested
  @DisplayName("File I/O Limit Method Tests")
  class FileIoLimitMethodTests {

    @Test
    @DisplayName("should have withFileWriteLimit method")
    void shouldHaveWithFileWriteLimitMethod() throws NoSuchMethodException {
      Method method = WasiResourceLimitsBuilder.class.getMethod("withFileWriteLimit", long.class);
      assertNotNull(method, "withFileWriteLimit method should exist");
      assertEquals(
          WasiResourceLimitsBuilder.class,
          method.getReturnType(),
          "Should return WasiResourceLimitsBuilder for chaining");
    }

    @Test
    @DisplayName("should have withoutFileWriteLimit method")
    void shouldHaveWithoutFileWriteLimitMethod() throws NoSuchMethodException {
      Method method = WasiResourceLimitsBuilder.class.getMethod("withoutFileWriteLimit");
      assertNotNull(method, "withoutFileWriteLimit method should exist");
      assertEquals(
          WasiResourceLimitsBuilder.class,
          method.getReturnType(),
          "Should return WasiResourceLimitsBuilder for chaining");
    }

    @Test
    @DisplayName("should have withFileReadLimit method")
    void shouldHaveWithFileReadLimitMethod() throws NoSuchMethodException {
      Method method = WasiResourceLimitsBuilder.class.getMethod("withFileReadLimit", long.class);
      assertNotNull(method, "withFileReadLimit method should exist");
      assertEquals(
          WasiResourceLimitsBuilder.class,
          method.getReturnType(),
          "Should return WasiResourceLimitsBuilder for chaining");
    }

    @Test
    @DisplayName("should have withoutFileReadLimit method")
    void shouldHaveWithoutFileReadLimitMethod() throws NoSuchMethodException {
      Method method = WasiResourceLimitsBuilder.class.getMethod("withoutFileReadLimit");
      assertNotNull(method, "withoutFileReadLimit method should exist");
      assertEquals(
          WasiResourceLimitsBuilder.class,
          method.getReturnType(),
          "Should return WasiResourceLimitsBuilder for chaining");
    }
  }

  // ========================================================================
  // CPU Time Limit Method Tests
  // ========================================================================

  @Nested
  @DisplayName("CPU Time Limit Method Tests")
  class CpuTimeLimitMethodTests {

    @Test
    @DisplayName("should have withCpuTimeLimit method")
    void shouldHaveWithCpuTimeLimitMethod() throws NoSuchMethodException {
      Method method = WasiResourceLimitsBuilder.class.getMethod("withCpuTimeLimit", Duration.class);
      assertNotNull(method, "withCpuTimeLimit method should exist");
      assertEquals(
          WasiResourceLimitsBuilder.class,
          method.getReturnType(),
          "Should return WasiResourceLimitsBuilder for chaining");
    }

    @Test
    @DisplayName("should have withoutCpuTimeLimit method")
    void shouldHaveWithoutCpuTimeLimitMethod() throws NoSuchMethodException {
      Method method = WasiResourceLimitsBuilder.class.getMethod("withoutCpuTimeLimit");
      assertNotNull(method, "withoutCpuTimeLimit method should exist");
      assertEquals(
          WasiResourceLimitsBuilder.class,
          method.getReturnType(),
          "Should return WasiResourceLimitsBuilder for chaining");
    }
  }

  // ========================================================================
  // Unlimited Mode Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Unlimited Mode Method Tests")
  class UnlimitedModeMethodTests {

    @Test
    @DisplayName("should have withUnlimited method")
    void shouldHaveWithUnlimitedMethod() throws NoSuchMethodException {
      Method method = WasiResourceLimitsBuilder.class.getMethod("withUnlimited", boolean.class);
      assertNotNull(method, "withUnlimited method should exist");
      assertEquals(
          WasiResourceLimitsBuilder.class,
          method.getReturnType(),
          "Should return WasiResourceLimitsBuilder for chaining");
    }

    @Test
    @DisplayName("should have clearAllLimits method")
    void shouldHaveClearAllLimitsMethod() throws NoSuchMethodException {
      Method method = WasiResourceLimitsBuilder.class.getMethod("clearAllLimits");
      assertNotNull(method, "clearAllLimits method should exist");
      assertEquals(
          WasiResourceLimitsBuilder.class,
          method.getReturnType(),
          "Should return WasiResourceLimitsBuilder for chaining");
    }
  }

  // ========================================================================
  // Build and Validate Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Build and Validate Method Tests")
  class BuildAndValidateMethodTests {

    @Test
    @DisplayName("should have build method")
    void shouldHaveBuildMethod() throws NoSuchMethodException {
      Method method = WasiResourceLimitsBuilder.class.getMethod("build");
      assertNotNull(method, "build method should exist");
      assertEquals(
          WasiResourceLimits.class, method.getReturnType(), "Should return WasiResourceLimits");
    }

    @Test
    @DisplayName("should have validate method")
    void shouldHaveValidateMethod() throws NoSuchMethodException {
      Method method = WasiResourceLimitsBuilder.class.getMethod("validate");
      assertNotNull(method, "validate method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }
  }

  // ========================================================================
  // Method Chaining Tests
  // ========================================================================

  @Nested
  @DisplayName("Method Chaining Tests")
  class MethodChainingTests {

    @Test
    @DisplayName("all configuration methods should return the builder")
    void allConfigurationMethodsShouldReturnBuilder() {
      long builderReturningMethods =
          Arrays.stream(WasiResourceLimitsBuilder.class.getDeclaredMethods())
              .filter(m -> !m.isSynthetic())
              .filter(m -> m.getReturnType().equals(WasiResourceLimitsBuilder.class))
              .count();
      assertTrue(
          builderReturningMethods >= 26, "Should have at least 26 methods returning builder");
    }
  }

  // ========================================================================
  // Nested Classes Tests
  // ========================================================================

  @Nested
  @DisplayName("Nested Classes Tests")
  class NestedClassesTests {

    @Test
    @DisplayName("should have no nested classes")
    void shouldHaveNoNestedClasses() {
      assertEquals(
          0,
          WasiResourceLimitsBuilder.class.getDeclaredClasses().length,
          "WasiResourceLimitsBuilder should have no nested classes");
    }
  }

  // ========================================================================
  // Method Count Tests
  // ========================================================================

  @Nested
  @DisplayName("Method Count Tests")
  class MethodCountTests {

    @Test
    @DisplayName("should have at least 28 abstract methods")
    void shouldHaveExpectedMethodCount() {
      long abstractMethodCount =
          Arrays.stream(WasiResourceLimitsBuilder.class.getDeclaredMethods())
              .filter(m -> !m.isSynthetic())
              .filter(m -> Modifier.isAbstract(m.getModifiers()))
              .count();
      assertTrue(abstractMethodCount >= 28, "Should have at least 28 abstract methods");
    }
  }

  // ========================================================================
  // Field Tests
  // ========================================================================

  @Nested
  @DisplayName("Field Tests")
  class FieldTests {

    @Test
    @DisplayName("should have no declared fields")
    void shouldHaveNoDeclaredFields() {
      assertEquals(
          0,
          WasiResourceLimitsBuilder.class.getDeclaredFields().length,
          "WasiResourceLimitsBuilder should have no declared fields");
    }
  }
}
