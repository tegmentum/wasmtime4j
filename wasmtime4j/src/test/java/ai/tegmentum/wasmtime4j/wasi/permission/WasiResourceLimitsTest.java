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

package ai.tegmentum.wasmtime4j.wasi.permission;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link WasiResourceLimits} class.
 *
 * <p>WasiResourceLimits provides resource limiting and quota enforcement configuration for WASI
 * contexts including memory usage limits, file descriptor limits, disk I/O limits, network
 * connection limits, and execution time limits.
 */
@DisplayName("WasiResourceLimits Tests")
class WasiResourceLimitsTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be public final class")
    void shouldBePublicFinalClass() {
      assertTrue(
          Modifier.isPublic(WasiResourceLimits.class.getModifiers()),
          "WasiResourceLimits should be public");
      assertTrue(
          Modifier.isFinal(WasiResourceLimits.class.getModifiers()),
          "WasiResourceLimits should be final");
    }
  }

  @Nested
  @DisplayName("Constant Tests")
  class ConstantTests {

    @Test
    @DisplayName("should have UNLIMITED constant")
    void shouldHaveUnlimitedConstant() throws NoSuchFieldException {
      final Field field = WasiResourceLimits.class.getField("UNLIMITED");
      assertNotNull(field, "UNLIMITED constant should exist");
      assertTrue(Modifier.isPublic(field.getModifiers()), "Should be public");
      assertTrue(Modifier.isStatic(field.getModifiers()), "Should be static");
      assertTrue(Modifier.isFinal(field.getModifiers()), "Should be final");
      assertEquals(long.class, field.getType(), "Should be long");
    }
  }

  @Nested
  @DisplayName("Static Factory Method Tests")
  class StaticFactoryMethodTests {

    @Test
    @DisplayName("should have defaultLimits static method")
    void shouldHaveDefaultLimitsMethod() throws NoSuchMethodException {
      final Method method = WasiResourceLimits.class.getMethod("defaultLimits");
      assertNotNull(method, "defaultLimits method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "Should be static");
      assertEquals(
          WasiResourceLimits.class, method.getReturnType(), "Should return WasiResourceLimits");
    }

    @Test
    @DisplayName("should have restrictiveLimits static method")
    void shouldHaveRestrictiveLimitsMethod() throws NoSuchMethodException {
      final Method method = WasiResourceLimits.class.getMethod("restrictiveLimits");
      assertNotNull(method, "restrictiveLimits method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "Should be static");
      assertEquals(
          WasiResourceLimits.class, method.getReturnType(), "Should return WasiResourceLimits");
    }

    @Test
    @DisplayName("should have permissiveLimits static method")
    void shouldHavePermissiveLimitsMethod() throws NoSuchMethodException {
      final Method method = WasiResourceLimits.class.getMethod("permissiveLimits");
      assertNotNull(method, "permissiveLimits method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "Should be static");
      assertEquals(
          WasiResourceLimits.class, method.getReturnType(), "Should return WasiResourceLimits");
    }

    @Test
    @DisplayName("should have unlimitedLimits static method")
    void shouldHaveUnlimitedLimitsMethod() throws NoSuchMethodException {
      final Method method = WasiResourceLimits.class.getMethod("unlimitedLimits");
      assertNotNull(method, "unlimitedLimits method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "Should be static");
      assertEquals(
          WasiResourceLimits.class, method.getReturnType(), "Should return WasiResourceLimits");
    }

    @Test
    @DisplayName("should have builder static method")
    void shouldHaveBuilderMethod() throws NoSuchMethodException {
      final Method method = WasiResourceLimits.class.getMethod("builder");
      assertNotNull(method, "builder method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "Should be static");
      assertEquals(
          WasiResourceLimits.Builder.class, method.getReturnType(), "Should return Builder");
    }
  }

  @Nested
  @DisplayName("Getter Method Tests")
  class GetterMethodTests {

    @Test
    @DisplayName("should have getMaxMemoryBytes method")
    void shouldHaveGetMaxMemoryBytesMethod() throws NoSuchMethodException {
      final Method method = WasiResourceLimits.class.getMethod("getMaxMemoryBytes");
      assertNotNull(method, "getMaxMemoryBytes method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have getMaxFileDescriptors method")
    void shouldHaveGetMaxFileDescriptorsMethod() throws NoSuchMethodException {
      final Method method = WasiResourceLimits.class.getMethod("getMaxFileDescriptors");
      assertNotNull(method, "getMaxFileDescriptors method should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }

    @Test
    @DisplayName("should have getMaxDiskReadsPerSecond method")
    void shouldHaveGetMaxDiskReadsPerSecondMethod() throws NoSuchMethodException {
      final Method method = WasiResourceLimits.class.getMethod("getMaxDiskReadsPerSecond");
      assertNotNull(method, "getMaxDiskReadsPerSecond method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have getMaxDiskWritesPerSecond method")
    void shouldHaveGetMaxDiskWritesPerSecondMethod() throws NoSuchMethodException {
      final Method method = WasiResourceLimits.class.getMethod("getMaxDiskWritesPerSecond");
      assertNotNull(method, "getMaxDiskWritesPerSecond method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have getMaxDiskReadBytesPerSecond method")
    void shouldHaveGetMaxDiskReadBytesPerSecondMethod() throws NoSuchMethodException {
      final Method method = WasiResourceLimits.class.getMethod("getMaxDiskReadBytesPerSecond");
      assertNotNull(method, "getMaxDiskReadBytesPerSecond method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have getMaxDiskWriteBytesPerSecond method")
    void shouldHaveGetMaxDiskWriteBytesPerSecondMethod() throws NoSuchMethodException {
      final Method method = WasiResourceLimits.class.getMethod("getMaxDiskWriteBytesPerSecond");
      assertNotNull(method, "getMaxDiskWriteBytesPerSecond method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have getMaxNetworkConnections method")
    void shouldHaveGetMaxNetworkConnectionsMethod() throws NoSuchMethodException {
      final Method method = WasiResourceLimits.class.getMethod("getMaxNetworkConnections");
      assertNotNull(method, "getMaxNetworkConnections method should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }

    @Test
    @DisplayName("should have getMaxExecutionTime method")
    void shouldHaveGetMaxExecutionTimeMethod() throws NoSuchMethodException {
      final Method method = WasiResourceLimits.class.getMethod("getMaxExecutionTime");
      assertNotNull(method, "getMaxExecutionTime method should exist");
      assertEquals(Duration.class, method.getReturnType(), "Should return Duration");
    }

    @Test
    @DisplayName("should have getMaxCpuTime method")
    void shouldHaveGetMaxCpuTimeMethod() throws NoSuchMethodException {
      final Method method = WasiResourceLimits.class.getMethod("getMaxCpuTime");
      assertNotNull(method, "getMaxCpuTime method should exist");
      assertEquals(Duration.class, method.getReturnType(), "Should return Duration");
    }

    @Test
    @DisplayName("should have getMaxWallClockTime method")
    void shouldHaveGetMaxWallClockTimeMethod() throws NoSuchMethodException {
      final Method method = WasiResourceLimits.class.getMethod("getMaxWallClockTime");
      assertNotNull(method, "getMaxWallClockTime method should exist");
      assertEquals(Duration.class, method.getReturnType(), "Should return Duration");
    }

    @Test
    @DisplayName("should have getMaxFileSize method")
    void shouldHaveGetMaxFileSizeMethod() throws NoSuchMethodException {
      final Method method = WasiResourceLimits.class.getMethod("getMaxFileSize");
      assertNotNull(method, "getMaxFileSize method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have getMaxDiskSpaceUsage method")
    void shouldHaveGetMaxDiskSpaceUsageMethod() throws NoSuchMethodException {
      final Method method = WasiResourceLimits.class.getMethod("getMaxDiskSpaceUsage");
      assertNotNull(method, "getMaxDiskSpaceUsage method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }
  }

  @Nested
  @DisplayName("Limit Check Method Tests")
  class LimitCheckMethodTests {

    @Test
    @DisplayName("should have isMemoryLimited method")
    void shouldHaveIsMemoryLimitedMethod() throws NoSuchMethodException {
      final Method method = WasiResourceLimits.class.getMethod("isMemoryLimited");
      assertNotNull(method, "isMemoryLimited method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have areFileDescriptorsLimited method")
    void shouldHaveAreFileDescriptorsLimitedMethod() throws NoSuchMethodException {
      final Method method = WasiResourceLimits.class.getMethod("areFileDescriptorsLimited");
      assertNotNull(method, "areFileDescriptorsLimited method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have isDiskIoLimited method")
    void shouldHaveIsDiskIoLimitedMethod() throws NoSuchMethodException {
      final Method method = WasiResourceLimits.class.getMethod("isDiskIoLimited");
      assertNotNull(method, "isDiskIoLimited method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have areNetworkConnectionsLimited method")
    void shouldHaveAreNetworkConnectionsLimitedMethod() throws NoSuchMethodException {
      final Method method = WasiResourceLimits.class.getMethod("areNetworkConnectionsLimited");
      assertNotNull(method, "areNetworkConnectionsLimited method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have isExecutionTimeLimited method")
    void shouldHaveIsExecutionTimeLimitedMethod() throws NoSuchMethodException {
      final Method method = WasiResourceLimits.class.getMethod("isExecutionTimeLimited");
      assertNotNull(method, "isExecutionTimeLimited method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have areFileOperationsLimited method")
    void shouldHaveAreFileOperationsLimitedMethod() throws NoSuchMethodException {
      final Method method = WasiResourceLimits.class.getMethod("areFileOperationsLimited");
      assertNotNull(method, "areFileOperationsLimited method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }
  }

  @Nested
  @DisplayName("ToString Method Tests")
  class ToStringMethodTests {

    @Test
    @DisplayName("should have toString method")
    void shouldHaveToStringMethod() throws NoSuchMethodException {
      final Method method = WasiResourceLimits.class.getMethod("toString");
      assertNotNull(method, "toString method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }
  }

  @Nested
  @DisplayName("Builder Inner Class Tests")
  class BuilderTests {

    @Test
    @DisplayName("Builder should be public static final class")
    void builderShouldBePublicStaticFinal() {
      Class<?> builderClass = WasiResourceLimits.Builder.class;
      assertTrue(Modifier.isPublic(builderClass.getModifiers()), "Should be public");
      assertTrue(Modifier.isStatic(builderClass.getModifiers()), "Should be static");
      assertTrue(Modifier.isFinal(builderClass.getModifiers()), "Should be final");
    }

    @Test
    @DisplayName("Builder should have withMaxMemoryBytes method")
    void builderShouldHaveWithMaxMemoryBytesMethod() throws NoSuchMethodException {
      final Method method =
          WasiResourceLimits.Builder.class.getMethod("withMaxMemoryBytes", long.class);
      assertNotNull(method, "withMaxMemoryBytes method should exist");
      assertEquals(
          WasiResourceLimits.Builder.class, method.getReturnType(), "Should return Builder");
    }

    @Test
    @DisplayName("Builder should have withMaxFileDescriptors method")
    void builderShouldHaveWithMaxFileDescriptorsMethod() throws NoSuchMethodException {
      final Method method =
          WasiResourceLimits.Builder.class.getMethod("withMaxFileDescriptors", int.class);
      assertNotNull(method, "withMaxFileDescriptors method should exist");
      assertEquals(
          WasiResourceLimits.Builder.class, method.getReturnType(), "Should return Builder");
    }

    @Test
    @DisplayName("Builder should have withMaxNetworkConnections method")
    void builderShouldHaveWithMaxNetworkConnectionsMethod() throws NoSuchMethodException {
      final Method method =
          WasiResourceLimits.Builder.class.getMethod("withMaxNetworkConnections", int.class);
      assertNotNull(method, "withMaxNetworkConnections method should exist");
      assertEquals(
          WasiResourceLimits.Builder.class, method.getReturnType(), "Should return Builder");
    }

    @Test
    @DisplayName("Builder should have withMaxExecutionTime method")
    void builderShouldHaveWithMaxExecutionTimeMethod() throws NoSuchMethodException {
      final Method method =
          WasiResourceLimits.Builder.class.getMethod("withMaxExecutionTime", Duration.class);
      assertNotNull(method, "withMaxExecutionTime method should exist");
      assertEquals(
          WasiResourceLimits.Builder.class, method.getReturnType(), "Should return Builder");
    }

    @Test
    @DisplayName("Builder should have build method")
    void builderShouldHaveBuildMethod() throws NoSuchMethodException {
      final Method method = WasiResourceLimits.Builder.class.getMethod("build");
      assertNotNull(method, "build method should exist");
      assertEquals(
          WasiResourceLimits.class, method.getReturnType(), "Should return WasiResourceLimits");
    }
  }

  @Nested
  @DisplayName("Functional Tests")
  class FunctionalTests {

    @Test
    @DisplayName("defaultLimits should return non-null instance")
    void defaultLimitsShouldReturnNonNull() {
      assertNotNull(WasiResourceLimits.defaultLimits(), "Should return non-null limits");
    }

    @Test
    @DisplayName("restrictiveLimits should return non-null instance")
    void restrictiveLimitsShouldReturnNonNull() {
      assertNotNull(WasiResourceLimits.restrictiveLimits(), "Should return non-null limits");
    }

    @Test
    @DisplayName("permissiveLimits should return non-null instance")
    void permissiveLimitsShouldReturnNonNull() {
      assertNotNull(WasiResourceLimits.permissiveLimits(), "Should return non-null limits");
    }

    @Test
    @DisplayName("unlimitedLimits should return non-null instance")
    void unlimitedLimitsShouldReturnNonNull() {
      assertNotNull(WasiResourceLimits.unlimitedLimits(), "Should return non-null limits");
    }

    @Test
    @DisplayName("builder should return non-null builder instance")
    void builderShouldReturnNonNull() {
      assertNotNull(WasiResourceLimits.builder(), "Should return non-null builder");
    }

    @Test
    @DisplayName("builder chain should work correctly")
    void builderChainShouldWork() {
      assertDoesNotThrow(
          () ->
              WasiResourceLimits.builder()
                  .withMaxMemoryBytes(1024 * 1024)
                  .withMaxFileDescriptors(100)
                  .withMaxNetworkConnections(50)
                  .withMaxExecutionTime(Duration.ofMinutes(10))
                  .build(),
          "Builder chain should not throw");
    }
  }
}
