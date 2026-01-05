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
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link WasiResourceLimits} interface.
 *
 * <p>WasiResourceLimits provides resource limits configuration for WASI.
 */
@DisplayName("WasiResourceLimits Tests")
class WasiResourceLimitsTest {

  @Nested
  @DisplayName("Interface Structure Tests")
  class InterfaceStructureTests {

    @Test
    @DisplayName("should be public interface")
    void shouldBePublicInterface() {
      assertTrue(
          Modifier.isPublic(WasiResourceLimits.class.getModifiers()),
          "WasiResourceLimits should be public");
      assertTrue(
          WasiResourceLimits.class.isInterface(), "WasiResourceLimits should be an interface");
    }
  }

  @Nested
  @DisplayName("Static Factory Method Tests")
  class StaticFactoryMethodTests {

    @Test
    @DisplayName("should have builder static method")
    void shouldHaveBuilderStaticMethod() throws NoSuchMethodException {
      final Method method = WasiResourceLimits.class.getMethod("builder");
      assertNotNull(method, "builder method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "builder should be static");
      assertEquals(
          WasiResourceLimitsBuilder.class, method.getReturnType(), "Should return Builder");
    }

    @Test
    @DisplayName("should have defaultLimits static method")
    void shouldHaveDefaultLimitsStaticMethod() throws NoSuchMethodException {
      final Method method = WasiResourceLimits.class.getMethod("defaultLimits");
      assertNotNull(method, "defaultLimits method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "defaultLimits should be static");
      assertEquals(
          WasiResourceLimits.class, method.getReturnType(), "Should return WasiResourceLimits");
    }

    @Test
    @DisplayName("should have unlimited static method")
    void shouldHaveUnlimitedStaticMethod() throws NoSuchMethodException {
      final Method method = WasiResourceLimits.class.getMethod("unlimited");
      assertNotNull(method, "unlimited method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "unlimited should be static");
      assertEquals(
          WasiResourceLimits.class, method.getReturnType(), "Should return WasiResourceLimits");
    }
  }

  @Nested
  @DisplayName("Memory Limit Method Tests")
  class MemoryLimitMethodTests {

    @Test
    @DisplayName("should have getMemoryLimit method")
    void shouldHaveGetMemoryLimitMethod() throws NoSuchMethodException {
      final Method method = WasiResourceLimits.class.getMethod("getMemoryLimit");
      assertNotNull(method, "getMemoryLimit method should exist");
      assertEquals(Optional.class, method.getReturnType(), "Should return Optional");
    }
  }

  @Nested
  @DisplayName("Timeout Method Tests")
  class TimeoutMethodTests {

    @Test
    @DisplayName("should have getExecutionTimeout method")
    void shouldHaveGetExecutionTimeoutMethod() throws NoSuchMethodException {
      final Method method = WasiResourceLimits.class.getMethod("getExecutionTimeout");
      assertNotNull(method, "getExecutionTimeout method should exist");
      assertEquals(Optional.class, method.getReturnType(), "Should return Optional");
    }

    @Test
    @DisplayName("should have getTotalExecutionTimeout method")
    void shouldHaveGetTotalExecutionTimeoutMethod() throws NoSuchMethodException {
      final Method method = WasiResourceLimits.class.getMethod("getTotalExecutionTimeout");
      assertNotNull(method, "getTotalExecutionTimeout method should exist");
      assertEquals(Optional.class, method.getReturnType(), "Should return Optional");
    }

    @Test
    @DisplayName("should have getCpuTimeLimit method")
    void shouldHaveGetCpuTimeLimitMethod() throws NoSuchMethodException {
      final Method method = WasiResourceLimits.class.getMethod("getCpuTimeLimit");
      assertNotNull(method, "getCpuTimeLimit method should exist");
      assertEquals(Optional.class, method.getReturnType(), "Should return Optional");
    }
  }

  @Nested
  @DisplayName("Handle Limit Method Tests")
  class HandleLimitMethodTests {

    @Test
    @DisplayName("should have getFileHandleLimit method")
    void shouldHaveGetFileHandleLimitMethod() throws NoSuchMethodException {
      final Method method = WasiResourceLimits.class.getMethod("getFileHandleLimit");
      assertNotNull(method, "getFileHandleLimit method should exist");
      assertEquals(Optional.class, method.getReturnType(), "Should return Optional");
    }

    @Test
    @DisplayName("should have getNetworkConnectionLimit method")
    void shouldHaveGetNetworkConnectionLimitMethod() throws NoSuchMethodException {
      final Method method = WasiResourceLimits.class.getMethod("getNetworkConnectionLimit");
      assertNotNull(method, "getNetworkConnectionLimit method should exist");
      assertEquals(Optional.class, method.getReturnType(), "Should return Optional");
    }

    @Test
    @DisplayName("should have getThreadLimit method")
    void shouldHaveGetThreadLimitMethod() throws NoSuchMethodException {
      final Method method = WasiResourceLimits.class.getMethod("getThreadLimit");
      assertNotNull(method, "getThreadLimit method should exist");
      assertEquals(Optional.class, method.getReturnType(), "Should return Optional");
    }

    @Test
    @DisplayName("should have getResourceCountLimit method")
    void shouldHaveGetResourceCountLimitMethod() throws NoSuchMethodException {
      final Method method = WasiResourceLimits.class.getMethod("getResourceCountLimit");
      assertNotNull(method, "getResourceCountLimit method should exist");
      assertEquals(Optional.class, method.getReturnType(), "Should return Optional");
    }
  }

  @Nested
  @DisplayName("Stack Limit Method Tests")
  class StackLimitMethodTests {

    @Test
    @DisplayName("should have getStackDepthLimit method")
    void shouldHaveGetStackDepthLimitMethod() throws NoSuchMethodException {
      final Method method = WasiResourceLimits.class.getMethod("getStackDepthLimit");
      assertNotNull(method, "getStackDepthLimit method should exist");
      assertEquals(Optional.class, method.getReturnType(), "Should return Optional");
    }
  }

  @Nested
  @DisplayName("IO Limit Method Tests")
  class IoLimitMethodTests {

    @Test
    @DisplayName("should have getFileWriteLimit method")
    void shouldHaveGetFileWriteLimitMethod() throws NoSuchMethodException {
      final Method method = WasiResourceLimits.class.getMethod("getFileWriteLimit");
      assertNotNull(method, "getFileWriteLimit method should exist");
      assertEquals(Optional.class, method.getReturnType(), "Should return Optional");
    }

    @Test
    @DisplayName("should have getFileReadLimit method")
    void shouldHaveGetFileReadLimitMethod() throws NoSuchMethodException {
      final Method method = WasiResourceLimits.class.getMethod("getFileReadLimit");
      assertNotNull(method, "getFileReadLimit method should exist");
      assertEquals(Optional.class, method.getReturnType(), "Should return Optional");
    }

    @Test
    @DisplayName("should have getNetworkSendLimit method")
    void shouldHaveGetNetworkSendLimitMethod() throws NoSuchMethodException {
      final Method method = WasiResourceLimits.class.getMethod("getNetworkSendLimit");
      assertNotNull(method, "getNetworkSendLimit method should exist");
      assertEquals(Optional.class, method.getReturnType(), "Should return Optional");
    }

    @Test
    @DisplayName("should have getNetworkReceiveLimit method")
    void shouldHaveGetNetworkReceiveLimitMethod() throws NoSuchMethodException {
      final Method method = WasiResourceLimits.class.getMethod("getNetworkReceiveLimit");
      assertNotNull(method, "getNetworkReceiveLimit method should exist");
      assertEquals(Optional.class, method.getReturnType(), "Should return Optional");
    }
  }

  @Nested
  @DisplayName("State Method Tests")
  class StateMethodTests {

    @Test
    @DisplayName("should have isUnlimited method")
    void shouldHaveIsUnlimitedMethod() throws NoSuchMethodException {
      final Method method = WasiResourceLimits.class.getMethod("isUnlimited");
      assertNotNull(method, "isUnlimited method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have hasLimits method")
    void shouldHaveHasLimitsMethod() throws NoSuchMethodException {
      final Method method = WasiResourceLimits.class.getMethod("hasLimits");
      assertNotNull(method, "hasLimits method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }
  }

  @Nested
  @DisplayName("Utility Method Tests")
  class UtilityMethodTests {

    @Test
    @DisplayName("should have toBuilder method")
    void shouldHaveToBuilderMethod() throws NoSuchMethodException {
      final Method method = WasiResourceLimits.class.getMethod("toBuilder");
      assertNotNull(method, "toBuilder method should exist");
      assertEquals(
          WasiResourceLimitsBuilder.class, method.getReturnType(), "Should return Builder");
    }

    @Test
    @DisplayName("should have validate method")
    void shouldHaveValidateMethod() throws NoSuchMethodException {
      final Method method = WasiResourceLimits.class.getMethod("validate");
      assertNotNull(method, "validate method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have getSummary method")
    void shouldHaveGetSummaryMethod() throws NoSuchMethodException {
      final Method method = WasiResourceLimits.class.getMethod("getSummary");
      assertNotNull(method, "getSummary method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }
  }
}
