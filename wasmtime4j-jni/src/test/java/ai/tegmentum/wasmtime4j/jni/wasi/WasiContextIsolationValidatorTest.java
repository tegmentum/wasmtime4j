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
import ai.tegmentum.wasmtime4j.jni.wasi.WasiContextIsolationValidator.IsolationLevel;
import ai.tegmentum.wasmtime4j.jni.wasi.WasiContextIsolationValidator.IsolationStatistics;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Comprehensive tests for {@link WasiContextIsolationValidator}. */
@DisplayName("WasiContextIsolationValidator Tests")
class WasiContextIsolationValidatorTest {

  private WasiContextIsolationValidator validator;
  private WasiContext testContext1;
  private WasiContext testContext2;

  @BeforeEach
  void setUp() {
    validator = new WasiContextIsolationValidator(true);

    testContext1 =
        TestWasiContextFactory.createTestContext(
            1L, Collections.singletonMap("preopened", Paths.get("/tmp/context1")));

    testContext2 =
        TestWasiContextFactory.createTestContext(
            2L, Collections.singletonMap("preopened", Paths.get("/tmp/context2")));
  }

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("WasiContextIsolationValidator should be final class")
    void shouldBeFinalClass() {
      assertTrue(
          java.lang.reflect.Modifier.isFinal(WasiContextIsolationValidator.class.getModifiers()),
          "WasiContextIsolationValidator should be final");
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("Default constructor should enable strict isolation")
    void defaultConstructorShouldEnableStrictIsolation() {
      final WasiContextIsolationValidator defaultValidator = new WasiContextIsolationValidator();

      assertTrue(defaultValidator.isStrictIsolationMode(), "Default should be strict mode");
    }

    @Test
    @DisplayName("Constructor should accept strict mode parameter")
    void constructorShouldAcceptStrictModeParameter() {
      final WasiContextIsolationValidator strictValidator = new WasiContextIsolationValidator(true);
      final WasiContextIsolationValidator permissiveValidator =
          new WasiContextIsolationValidator(false);

      assertTrue(strictValidator.isStrictIsolationMode(), "Should be strict mode");
      assertFalse(permissiveValidator.isStrictIsolationMode(), "Should not be strict mode");
    }
  }

  @Nested
  @DisplayName("registerContext Tests")
  class RegisterContextTests {

    @Test
    @DisplayName("Should throw on null context ID")
    void shouldThrowOnNullContextId() {
      assertThrows(
          JniException.class,
          () -> validator.registerContext(null, testContext1, IsolationLevel.STANDARD),
          "Should throw on null context ID");
    }

    @Test
    @DisplayName("Should throw on empty context ID")
    void shouldThrowOnEmptyContextId() {
      assertThrows(
          JniException.class,
          () -> validator.registerContext("", testContext1, IsolationLevel.STANDARD),
          "Should throw on empty context ID");
    }

    @Test
    @DisplayName("Should throw on null context")
    void shouldThrowOnNullContext() {
      assertThrows(
          JniException.class,
          () -> validator.registerContext("ctx1", null, IsolationLevel.STANDARD),
          "Should throw on null context");
    }

    @Test
    @DisplayName("Should throw on null isolation level")
    void shouldThrowOnNullIsolationLevel() {
      assertThrows(
          JniException.class,
          () -> validator.registerContext("ctx1", testContext1, null),
          "Should throw on null isolation level");
    }

    @Test
    @DisplayName("Should register context successfully")
    void shouldRegisterContextSuccessfully() {
      validator.registerContext("ctx1", testContext1, IsolationLevel.STANDARD);

      assertEquals(1, validator.getActiveContextCount(), "Should have 1 active context");
    }

    @Test
    @DisplayName("Should throw on duplicate context ID")
    void shouldThrowOnDuplicateContextId() {
      validator.registerContext("ctx1", testContext1, IsolationLevel.STANDARD);

      assertThrows(
          JniException.class,
          () -> validator.registerContext("ctx1", testContext2, IsolationLevel.STANDARD),
          "Should throw on duplicate context ID");
    }

    @Test
    @DisplayName("Should update statistics on registration")
    void shouldUpdateStatisticsOnRegistration() {
      validator.registerContext("ctx1", testContext1, IsolationLevel.STANDARD);

      final IsolationStatistics stats = validator.getStatistics();
      assertEquals(1, stats.getContextsRegistered(), "Should count registered contexts");
    }
  }

  @Nested
  @DisplayName("unregisterContext Tests")
  class UnregisterContextTests {

    @Test
    @DisplayName("Should throw on null context ID")
    void shouldThrowOnNullContextId() {
      assertThrows(
          JniException.class,
          () -> validator.unregisterContext(null),
          "Should throw on null context ID");
    }

    @Test
    @DisplayName("Should throw on empty context ID")
    void shouldThrowOnEmptyContextId() {
      assertThrows(
          JniException.class,
          () -> validator.unregisterContext(""),
          "Should throw on empty context ID");
    }

    @Test
    @DisplayName("Should unregister context successfully")
    void shouldUnregisterContextSuccessfully() {
      validator.registerContext("ctx1", testContext1, IsolationLevel.STANDARD);
      assertEquals(1, validator.getActiveContextCount(), "Should have 1 active context");

      validator.unregisterContext("ctx1");
      assertEquals(0, validator.getActiveContextCount(), "Should have 0 active contexts");
    }

    @Test
    @DisplayName("Should handle unregistering non-existent context gracefully")
    void shouldHandleUnregisteringNonExistentContextGracefully() {
      assertDoesNotThrow(
          () -> validator.unregisterContext("nonexistent"),
          "Should handle non-existent context gracefully");
    }

    @Test
    @DisplayName("Should update statistics on unregistration")
    void shouldUpdateStatisticsOnUnregistration() {
      validator.registerContext("ctx1", testContext1, IsolationLevel.STANDARD);
      validator.unregisterContext("ctx1");

      final IsolationStatistics stats = validator.getStatistics();
      assertEquals(1, stats.getContextsUnregistered(), "Should count unregistered contexts");
    }
  }

  @Nested
  @DisplayName("validatePathAccess Tests")
  class ValidatePathAccessTests {

    @Test
    @DisplayName("Should throw on null context ID")
    void shouldThrowOnNullContextId() {
      assertThrows(
          JniException.class,
          () -> validator.validatePathAccess(null, Paths.get("/tmp"), WasiFileOperation.READ),
          "Should throw on null context ID");
    }

    @Test
    @DisplayName("Should throw on empty context ID")
    void shouldThrowOnEmptyContextId() {
      assertThrows(
          JniException.class,
          () -> validator.validatePathAccess("", Paths.get("/tmp"), WasiFileOperation.READ),
          "Should throw on empty context ID");
    }

    @Test
    @DisplayName("Should throw on null path")
    void shouldThrowOnNullPath() {
      validator.registerContext("ctx1", testContext1, IsolationLevel.STANDARD);

      assertThrows(
          JniException.class,
          () -> validator.validatePathAccess("ctx1", null, WasiFileOperation.READ),
          "Should throw on null path");
    }

    @Test
    @DisplayName("Should throw on null operation")
    void shouldThrowOnNullOperation() {
      validator.registerContext("ctx1", testContext1, IsolationLevel.STANDARD);

      assertThrows(
          JniException.class,
          () -> validator.validatePathAccess("ctx1", Paths.get("/tmp"), null),
          "Should throw on null operation");
    }

    @Test
    @DisplayName("Should throw for unknown context ID")
    void shouldThrowForUnknownContextId() {
      assertThrows(
          JniException.class,
          () -> validator.validatePathAccess("unknown", Paths.get("/tmp"), WasiFileOperation.READ),
          "Should throw for unknown context ID");
    }

    @Test
    @DisplayName("Should allow access within preopened directory")
    void shouldAllowAccessWithinPreopenedDirectory() {
      validator.registerContext("ctx1", testContext1, IsolationLevel.STANDARD);

      final Path allowedPath = Paths.get("/tmp/context1/file.txt");

      assertDoesNotThrow(
          () -> validator.validatePathAccess("ctx1", allowedPath, WasiFileOperation.READ),
          "Should allow access within preopened directory");
    }

    @Test
    @DisplayName("Should deny access outside preopened directory")
    void shouldDenyAccessOutsidePreopenedDirectory() {
      validator.registerContext("ctx1", testContext1, IsolationLevel.STANDARD);

      final Path disallowedPath = Paths.get("/tmp/other/file.txt");

      assertThrows(
          JniException.class,
          () -> validator.validatePathAccess("ctx1", disallowedPath, WasiFileOperation.READ),
          "Should deny access outside preopened directory");
    }
  }

  @Nested
  @DisplayName("validateResourceAccess Tests")
  class ValidateResourceAccessTests {

    @Test
    @DisplayName("Should throw on null context ID")
    void shouldThrowOnNullContextId() {
      assertThrows(
          JniException.class,
          () -> validator.validateResourceAccess(null, "resource1", "file"),
          "Should throw on null context ID");
    }

    @Test
    @DisplayName("Should throw on empty context ID")
    void shouldThrowOnEmptyContextId() {
      assertThrows(
          JniException.class,
          () -> validator.validateResourceAccess("", "resource1", "file"),
          "Should throw on empty context ID");
    }

    @Test
    @DisplayName("Should throw on null resource ID")
    void shouldThrowOnNullResourceId() {
      validator.registerContext("ctx1", testContext1, IsolationLevel.STANDARD);

      assertThrows(
          JniException.class,
          () -> validator.validateResourceAccess("ctx1", null, "file"),
          "Should throw on null resource ID");
    }

    @Test
    @DisplayName("Should throw on empty resource ID")
    void shouldThrowOnEmptyResourceId() {
      validator.registerContext("ctx1", testContext1, IsolationLevel.STANDARD);

      assertThrows(
          JniException.class,
          () -> validator.validateResourceAccess("ctx1", "", "file"),
          "Should throw on empty resource ID");
    }

    @Test
    @DisplayName("Should throw on null resource type")
    void shouldThrowOnNullResourceType() {
      validator.registerContext("ctx1", testContext1, IsolationLevel.STANDARD);

      assertThrows(
          JniException.class,
          () -> validator.validateResourceAccess("ctx1", "resource1", null),
          "Should throw on null resource type");
    }

    @Test
    @DisplayName("Should throw on empty resource type")
    void shouldThrowOnEmptyResourceType() {
      validator.registerContext("ctx1", testContext1, IsolationLevel.STANDARD);

      assertThrows(
          JniException.class,
          () -> validator.validateResourceAccess("ctx1", "resource1", ""),
          "Should throw on empty resource type");
    }

    @Test
    @DisplayName("Should allow first access to resource in strict mode")
    void shouldAllowFirstAccessToResourceInStrictMode() {
      validator.registerContext("ctx1", testContext1, IsolationLevel.STRICT);

      assertDoesNotThrow(
          () -> validator.validateResourceAccess("ctx1", "resource1", "file"),
          "Should allow first access to resource");
    }

    @Test
    @DisplayName("Should deny second context access in strict mode")
    void shouldDenySecondContextAccessInStrictMode() {
      validator.registerContext("ctx1", testContext1, IsolationLevel.STRICT);
      validator.registerContext("ctx2", testContext2, IsolationLevel.STRICT);

      // First context accesses resource
      validator.validateResourceAccess("ctx1", "resource1", "file");

      // Second context should be denied
      assertThrows(
          JniException.class,
          () -> validator.validateResourceAccess("ctx2", "resource1", "file"),
          "Should deny second context access in strict mode");
    }
  }

  @Nested
  @DisplayName("validateMemoryAccess Tests")
  class ValidateMemoryAccessTests {

    @Test
    @DisplayName("Should throw on null context ID")
    void shouldThrowOnNullContextId() {
      assertThrows(
          JniException.class,
          () -> validator.validateMemoryAccess(null, 0, 100),
          "Should throw on null context ID");
    }

    @Test
    @DisplayName("Should throw on empty context ID")
    void shouldThrowOnEmptyContextId() {
      assertThrows(
          JniException.class,
          () -> validator.validateMemoryAccess("", 0, 100),
          "Should throw on empty context ID");
    }

    @Test
    @DisplayName("Should throw on negative memory address")
    void shouldThrowOnNegativeMemoryAddress() {
      validator.registerContext("ctx1", testContext1, IsolationLevel.STANDARD);

      assertThrows(
          JniException.class,
          () -> validator.validateMemoryAccess("ctx1", -1, 100),
          "Should throw on negative memory address");
    }

    @Test
    @DisplayName("Should throw on non-positive size")
    void shouldThrowOnNonPositiveSize() {
      validator.registerContext("ctx1", testContext1, IsolationLevel.STANDARD);

      assertThrows(
          JniException.class,
          () -> validator.validateMemoryAccess("ctx1", 0, 0),
          "Should throw on zero size");

      assertThrows(
          JniException.class,
          () -> validator.validateMemoryAccess("ctx1", 0, -1),
          "Should throw on negative size");
    }

    @Test
    @DisplayName("Should allow memory access for registered context")
    void shouldAllowMemoryAccessForRegisteredContext() {
      validator.registerContext("ctx1", testContext1, IsolationLevel.STANDARD);

      assertDoesNotThrow(
          () -> validator.validateMemoryAccess("ctx1", 0, 100),
          "Should allow memory access for registered context");
    }
  }

  @Nested
  @DisplayName("validateCrossContextCommunication Tests")
  class ValidateCrossContextCommunicationTests {

    @Test
    @DisplayName("Should throw on null source context ID")
    void shouldThrowOnNullSourceContextId() {
      assertThrows(
          JniException.class,
          () -> validator.validateCrossContextCommunication(null, "ctx2", "message"),
          "Should throw on null source context ID");
    }

    @Test
    @DisplayName("Should throw on empty source context ID")
    void shouldThrowOnEmptySourceContextId() {
      assertThrows(
          JniException.class,
          () -> validator.validateCrossContextCommunication("", "ctx2", "message"),
          "Should throw on empty source context ID");
    }

    @Test
    @DisplayName("Should throw on null target context ID")
    void shouldThrowOnNullTargetContextId() {
      validator.registerContext("ctx1", testContext1, IsolationLevel.STANDARD);

      assertThrows(
          JniException.class,
          () -> validator.validateCrossContextCommunication("ctx1", null, "message"),
          "Should throw on null target context ID");
    }

    @Test
    @DisplayName("Should throw on empty target context ID")
    void shouldThrowOnEmptyTargetContextId() {
      validator.registerContext("ctx1", testContext1, IsolationLevel.STANDARD);

      assertThrows(
          JniException.class,
          () -> validator.validateCrossContextCommunication("ctx1", "", "message"),
          "Should throw on empty target context ID");
    }

    @Test
    @DisplayName("Should throw on null communication type")
    void shouldThrowOnNullCommunicationType() {
      validator.registerContext("ctx1", testContext1, IsolationLevel.STANDARD);
      validator.registerContext("ctx2", testContext2, IsolationLevel.STANDARD);

      assertThrows(
          JniException.class,
          () -> validator.validateCrossContextCommunication("ctx1", "ctx2", null),
          "Should throw on null communication type");
    }

    @Test
    @DisplayName("Should throw on empty communication type")
    void shouldThrowOnEmptyCommunicationType() {
      validator.registerContext("ctx1", testContext1, IsolationLevel.STANDARD);
      validator.registerContext("ctx2", testContext2, IsolationLevel.STANDARD);

      assertThrows(
          JniException.class,
          () -> validator.validateCrossContextCommunication("ctx1", "ctx2", ""),
          "Should throw on empty communication type");
    }

    @Test
    @DisplayName("Should deny communication in strict mode")
    void shouldDenyCommunicationInStrictMode() {
      validator.registerContext("ctx1", testContext1, IsolationLevel.STRICT);
      validator.registerContext("ctx2", testContext2, IsolationLevel.STANDARD);

      assertThrows(
          JniException.class,
          () -> validator.validateCrossContextCommunication("ctx1", "ctx2", "message"),
          "Should deny communication when source is strict");
    }

    @Test
    @DisplayName("Should allow communication between permissive contexts")
    void shouldAllowCommunicationBetweenPermissiveContexts() {
      validator.registerContext("ctx1", testContext1, IsolationLevel.PERMISSIVE);
      validator.registerContext("ctx2", testContext2, IsolationLevel.PERMISSIVE);

      assertDoesNotThrow(
          () -> validator.validateCrossContextCommunication("ctx1", "ctx2", "message"),
          "Should allow communication between permissive contexts");
    }
  }

  @Nested
  @DisplayName("IsolationStatistics Tests")
  class IsolationStatisticsTests {

    @Test
    @DisplayName("Should return correct statistics")
    void shouldReturnCorrectStatistics() {
      validator.registerContext("ctx1", testContext1, IsolationLevel.STANDARD);
      validator.unregisterContext("ctx1");

      final IsolationStatistics stats = validator.getStatistics();

      assertEquals(1, stats.getContextsRegistered(), "Should count registered contexts");
      assertEquals(1, stats.getContextsUnregistered(), "Should count unregistered contexts");
      assertEquals(0, stats.getActiveContexts(), "Should calculate active contexts correctly");
    }

    @Test
    @DisplayName("Statistics toString should contain all fields")
    void statisticsToStringShouldContainAllFields() {
      validator.registerContext("ctx1", testContext1, IsolationLevel.STANDARD);

      final IsolationStatistics stats = validator.getStatistics();
      final String str = stats.toString();

      assertTrue(str.contains("active="), "Should contain active");
      assertTrue(str.contains("violations="), "Should contain violations");
    }

    @Test
    @DisplayName("Should track total violations")
    void shouldTrackTotalViolations() {
      validator.registerContext("ctx1", testContext1, IsolationLevel.STANDARD);

      // Try to access resource outside boundaries to generate violation
      try {
        validator.validatePathAccess("ctx1", Paths.get("/unauthorized"), WasiFileOperation.READ);
      } catch (JniException ignored) {
        // Expected
      }

      final IsolationStatistics stats = validator.getStatistics();
      assertTrue(stats.getTotalViolations() >= 1, "Should track violations");
    }
  }

  @Nested
  @DisplayName("IsolationLevel Tests")
  class IsolationLevelTests {

    @Test
    @DisplayName("Should have all isolation levels")
    void shouldHaveAllIsolationLevels() {
      assertEquals(3, IsolationLevel.values().length, "Should have 3 isolation levels");
      assertNotNull(IsolationLevel.PERMISSIVE, "PERMISSIVE should exist");
      assertNotNull(IsolationLevel.STANDARD, "STANDARD should exist");
      assertNotNull(IsolationLevel.STRICT, "STRICT should exist");
    }
  }

  @Nested
  @DisplayName("getActiveContextCount Tests")
  class GetActiveContextCountTests {

    @Test
    @DisplayName("Should return 0 initially")
    void shouldReturnZeroInitially() {
      assertEquals(0, validator.getActiveContextCount(), "Should have 0 active contexts initially");
    }

    @Test
    @DisplayName("Should count active contexts correctly")
    void shouldCountActiveContextsCorrectly() {
      validator.registerContext("ctx1", testContext1, IsolationLevel.STANDARD);
      assertEquals(1, validator.getActiveContextCount(), "Should have 1 active context");

      validator.registerContext("ctx2", testContext2, IsolationLevel.STANDARD);
      assertEquals(2, validator.getActiveContextCount(), "Should have 2 active contexts");

      validator.unregisterContext("ctx1");
      assertEquals(
          1, validator.getActiveContextCount(), "Should have 1 active context after unregister");
    }
  }
}
