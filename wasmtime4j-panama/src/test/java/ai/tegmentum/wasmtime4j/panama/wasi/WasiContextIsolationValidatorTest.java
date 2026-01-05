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

package ai.tegmentum.wasmtime4j.panama.wasi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.foreign.MemorySegment;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link WasiContextIsolationValidator} class.
 *
 * <p>WasiContextIsolationValidator provides context isolation validation to ensure WASI contexts
 * cannot interfere with each other using Panama FFI.
 */
@DisplayName("WasiContextIsolationValidator Tests")
class WasiContextIsolationValidatorTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be public final class")
    void shouldBePublicFinalClass() {
      assertTrue(
          Modifier.isPublic(WasiContextIsolationValidator.class.getModifiers()),
          "WasiContextIsolationValidator should be public");
      assertTrue(
          Modifier.isFinal(WasiContextIsolationValidator.class.getModifiers()),
          "WasiContextIsolationValidator should be final");
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should have default constructor")
    void shouldHaveDefaultConstructor() throws NoSuchMethodException {
      final Constructor<?> constructor = WasiContextIsolationValidator.class.getConstructor();
      assertNotNull(constructor, "Default constructor should exist");
      assertTrue(Modifier.isPublic(constructor.getModifiers()), "Constructor should be public");
    }

    @Test
    @DisplayName("should have constructor with strict isolation mode parameter")
    void shouldHaveStrictModeConstructor() throws NoSuchMethodException {
      final Constructor<?> constructor =
          WasiContextIsolationValidator.class.getConstructor(boolean.class);
      assertNotNull(constructor, "Constructor with boolean should exist");
      assertTrue(Modifier.isPublic(constructor.getModifiers()), "Constructor should be public");
    }
  }

  @Nested
  @DisplayName("Context Registration Method Tests")
  class ContextRegistrationMethodTests {

    @Test
    @DisplayName("should have registerContext method")
    void shouldHaveRegisterContextMethod() throws NoSuchMethodException {
      final Method method =
          WasiContextIsolationValidator.class.getMethod(
              "registerContext",
              String.class,
              WasiContext.class,
              WasiContextIsolationValidator.IsolationLevel.class);
      assertNotNull(method, "registerContext method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have unregisterContext method")
    void shouldHaveUnregisterContextMethod() throws NoSuchMethodException {
      final Method method =
          WasiContextIsolationValidator.class.getMethod("unregisterContext", String.class);
      assertNotNull(method, "unregisterContext method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }
  }

  @Nested
  @DisplayName("Validation Method Tests")
  class ValidationMethodTests {

    @Test
    @DisplayName("should have validatePathAccess method")
    void shouldHaveValidatePathAccessMethod() throws NoSuchMethodException {
      final Method method =
          WasiContextIsolationValidator.class.getMethod(
              "validatePathAccess", String.class, Path.class, WasiFileOperation.class);
      assertNotNull(method, "validatePathAccess method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have validateResourceAccess method")
    void shouldHaveValidateResourceAccessMethod() throws NoSuchMethodException {
      final Method method =
          WasiContextIsolationValidator.class.getMethod(
              "validateResourceAccess", String.class, String.class, String.class);
      assertNotNull(method, "validateResourceAccess method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have validateMemoryAccess method")
    void shouldHaveValidateMemoryAccessMethod() throws NoSuchMethodException {
      final Method method =
          WasiContextIsolationValidator.class.getMethod(
              "validateMemoryAccess", String.class, MemorySegment.class);
      assertNotNull(method, "validateMemoryAccess method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have validateCrossContextCommunication method")
    void shouldHaveValidateCrossContextCommunicationMethod() throws NoSuchMethodException {
      final Method method =
          WasiContextIsolationValidator.class.getMethod(
              "validateCrossContextCommunication", String.class, String.class, String.class);
      assertNotNull(method, "validateCrossContextCommunication method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }
  }

  @Nested
  @DisplayName("Memory Allocation Method Tests")
  class MemoryAllocationMethodTests {

    @Test
    @DisplayName("should have allocateMemoryToContext method")
    void shouldHaveAllocateMemoryToContextMethod() throws NoSuchMethodException {
      final Method method =
          WasiContextIsolationValidator.class.getMethod(
              "allocateMemoryToContext", String.class, MemorySegment.class);
      assertNotNull(method, "allocateMemoryToContext method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have deallocateMemoryFromContext method")
    void shouldHaveDeallocateMemoryFromContextMethod() throws NoSuchMethodException {
      final Method method =
          WasiContextIsolationValidator.class.getMethod(
              "deallocateMemoryFromContext", String.class, MemorySegment.class);
      assertNotNull(method, "deallocateMemoryFromContext method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }
  }

  @Nested
  @DisplayName("Statistics Method Tests")
  class StatisticsMethodTests {

    @Test
    @DisplayName("should have getStatistics method")
    void shouldHaveGetStatisticsMethod() throws NoSuchMethodException {
      final Method method = WasiContextIsolationValidator.class.getMethod("getStatistics");
      assertNotNull(method, "getStatistics method should exist");
      assertEquals(
          WasiContextIsolationValidator.IsolationStatistics.class,
          method.getReturnType(),
          "Should return IsolationStatistics");
    }

    @Test
    @DisplayName("should have getActiveContextCount method")
    void shouldHaveGetActiveContextCountMethod() throws NoSuchMethodException {
      final Method method = WasiContextIsolationValidator.class.getMethod("getActiveContextCount");
      assertNotNull(method, "getActiveContextCount method should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }

    @Test
    @DisplayName("should have isStrictIsolationMode method")
    void shouldHaveIsStrictIsolationModeMethod() throws NoSuchMethodException {
      final Method method = WasiContextIsolationValidator.class.getMethod("isStrictIsolationMode");
      assertNotNull(method, "isStrictIsolationMode method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }
  }

  @Nested
  @DisplayName("IsolationLevel Enum Tests")
  class IsolationLevelEnumTests {

    @Test
    @DisplayName("IsolationLevel should be public enum")
    void isolationLevelShouldBePublicEnum() {
      Class<?> innerClass = WasiContextIsolationValidator.IsolationLevel.class;
      assertTrue(Modifier.isPublic(innerClass.getModifiers()), "Should be public");
      assertTrue(innerClass.isEnum(), "Should be an enum");
    }

    @Test
    @DisplayName("IsolationLevel should have PERMISSIVE constant")
    void isolationLevelShouldHavePermissiveConstant() {
      assertNotNull(
          WasiContextIsolationValidator.IsolationLevel.valueOf("PERMISSIVE"),
          "PERMISSIVE constant should exist");
    }

    @Test
    @DisplayName("IsolationLevel should have STANDARD constant")
    void isolationLevelShouldHaveStandardConstant() {
      assertNotNull(
          WasiContextIsolationValidator.IsolationLevel.valueOf("STANDARD"),
          "STANDARD constant should exist");
    }

    @Test
    @DisplayName("IsolationLevel should have STRICT constant")
    void isolationLevelShouldHaveStrictConstant() {
      assertNotNull(
          WasiContextIsolationValidator.IsolationLevel.valueOf("STRICT"),
          "STRICT constant should exist");
    }
  }

  @Nested
  @DisplayName("IsolationStatistics Inner Class Tests")
  class IsolationStatisticsTests {

    @Test
    @DisplayName("IsolationStatistics should be public static final class")
    void isolationStatisticsShouldBePublicStaticFinal() {
      Class<?> innerClass = WasiContextIsolationValidator.IsolationStatistics.class;
      assertTrue(Modifier.isPublic(innerClass.getModifiers()), "Should be public");
      assertTrue(Modifier.isStatic(innerClass.getModifiers()), "Should be static");
      assertTrue(Modifier.isFinal(innerClass.getModifiers()), "Should be final");
    }

    @Test
    @DisplayName("IsolationStatistics should have getTotalViolations method")
    void isolationStatisticsShouldHaveGetTotalViolationsMethod() throws NoSuchMethodException {
      final Method method =
          WasiContextIsolationValidator.IsolationStatistics.class.getMethod("getTotalViolations");
      assertNotNull(method, "getTotalViolations method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("IsolationStatistics should have getPathIsolationViolations method")
    void isolationStatisticsShouldHaveGetPathIsolationViolationsMethod()
        throws NoSuchMethodException {
      final Method method =
          WasiContextIsolationValidator.IsolationStatistics.class.getMethod(
              "getPathIsolationViolations");
      assertNotNull(method, "getPathIsolationViolations method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("IsolationStatistics should have getMemoryIsolationViolations method")
    void isolationStatisticsShouldHaveGetMemoryIsolationViolationsMethod()
        throws NoSuchMethodException {
      final Method method =
          WasiContextIsolationValidator.IsolationStatistics.class.getMethod(
              "getMemoryIsolationViolations");
      assertNotNull(method, "getMemoryIsolationViolations method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }
  }
}
