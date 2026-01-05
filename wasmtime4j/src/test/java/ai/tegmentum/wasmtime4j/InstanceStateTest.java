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

import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive test suite for the InstanceState enum.
 *
 * <p>InstanceState represents the lifecycle state of a WebAssembly instance. This test verifies the
 * enum structure and values.
 */
@DisplayName("InstanceState Enum Tests")
class InstanceStateTest {

  // ========================================================================
  // Type Definition Tests
  // ========================================================================

  @Nested
  @DisplayName("Type Definition Tests")
  class TypeDefinitionTests {

    @Test
    @DisplayName("should be an enum")
    void shouldBeAnEnum() {
      assertTrue(InstanceState.class.isEnum(), "InstanceState should be an enum");
    }

    @Test
    @DisplayName("should be public")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(InstanceState.class.getModifiers()), "InstanceState should be public");
    }
  }

  // ========================================================================
  // Enum Value Tests
  // ========================================================================

  @Nested
  @DisplayName("Enum Value Tests")
  class EnumValueTests {

    @Test
    @DisplayName("should have CREATING value")
    void shouldHaveCreatingValue() {
      assertNotNull(InstanceState.CREATING, "InstanceState should have CREATING value");
    }

    @Test
    @DisplayName("should have CREATED value")
    void shouldHaveCreatedValue() {
      assertNotNull(InstanceState.CREATED, "InstanceState should have CREATED value");
    }

    @Test
    @DisplayName("should have RUNNING value")
    void shouldHaveRunningValue() {
      assertNotNull(InstanceState.RUNNING, "InstanceState should have RUNNING value");
    }

    @Test
    @DisplayName("should have SUSPENDED value")
    void shouldHaveSuspendedValue() {
      assertNotNull(InstanceState.SUSPENDED, "InstanceState should have SUSPENDED value");
    }

    @Test
    @DisplayName("should have ERROR value")
    void shouldHaveErrorValue() {
      assertNotNull(InstanceState.ERROR, "InstanceState should have ERROR value");
    }

    @Test
    @DisplayName("should have DISPOSED value")
    void shouldHaveDisposedValue() {
      assertNotNull(InstanceState.DISPOSED, "InstanceState should have DISPOSED value");
    }

    @Test
    @DisplayName("should have DESTROYING value")
    void shouldHaveDestroyingValue() {
      assertNotNull(InstanceState.DESTROYING, "InstanceState should have DESTROYING value");
    }

    @Test
    @DisplayName("should have all expected values")
    void shouldHaveAllExpectedValues() {
      Set<String> expectedValues =
          Set.of("CREATING", "CREATED", "RUNNING", "SUSPENDED", "ERROR", "DISPOSED", "DESTROYING");

      Set<String> actualValues =
          Arrays.stream(InstanceState.values()).map(Enum::name).collect(Collectors.toSet());

      assertEquals(expectedValues, actualValues, "InstanceState should have all expected values");
    }
  }

  // ========================================================================
  // Enum Count Tests
  // ========================================================================

  @Nested
  @DisplayName("Enum Count Tests")
  class EnumCountTests {

    @Test
    @DisplayName("should have exactly 7 values")
    void shouldHaveSevenValues() {
      assertEquals(7, InstanceState.values().length, "InstanceState should have exactly 7 values");
    }
  }

  // ========================================================================
  // Enum Ordinal Tests
  // ========================================================================

  @Nested
  @DisplayName("Enum Ordinal Tests")
  class EnumOrdinalTests {

    @Test
    @DisplayName("CREATING should be first")
    void creatingShouldBeFirst() {
      assertEquals(0, InstanceState.CREATING.ordinal(), "CREATING should have ordinal 0");
    }

    @Test
    @DisplayName("CREATED should be second")
    void createdShouldBeSecond() {
      assertEquals(1, InstanceState.CREATED.ordinal(), "CREATED should have ordinal 1");
    }

    @Test
    @DisplayName("RUNNING should be third")
    void runningShouldBeThird() {
      assertEquals(2, InstanceState.RUNNING.ordinal(), "RUNNING should have ordinal 2");
    }

    @Test
    @DisplayName("SUSPENDED should be fourth")
    void suspendedShouldBeFourth() {
      assertEquals(3, InstanceState.SUSPENDED.ordinal(), "SUSPENDED should have ordinal 3");
    }

    @Test
    @DisplayName("ERROR should be fifth")
    void errorShouldBeFifth() {
      assertEquals(4, InstanceState.ERROR.ordinal(), "ERROR should have ordinal 4");
    }

    @Test
    @DisplayName("DISPOSED should be sixth")
    void disposedShouldBeSixth() {
      assertEquals(5, InstanceState.DISPOSED.ordinal(), "DISPOSED should have ordinal 5");
    }

    @Test
    @DisplayName("DESTROYING should be seventh")
    void destroyingShouldBeSeventh() {
      assertEquals(6, InstanceState.DESTROYING.ordinal(), "DESTROYING should have ordinal 6");
    }
  }

  // ========================================================================
  // Enum valueOf Tests
  // ========================================================================

  @Nested
  @DisplayName("Enum valueOf Tests")
  class EnumValueOfTests {

    @Test
    @DisplayName("valueOf should return CREATING for 'CREATING'")
    void valueOfShouldReturnCreating() {
      assertEquals(
          InstanceState.CREATING,
          InstanceState.valueOf("CREATING"),
          "valueOf('CREATING') should return CREATING");
    }

    @Test
    @DisplayName("valueOf should return CREATED for 'CREATED'")
    void valueOfShouldReturnCreated() {
      assertEquals(
          InstanceState.CREATED,
          InstanceState.valueOf("CREATED"),
          "valueOf('CREATED') should return CREATED");
    }

    @Test
    @DisplayName("valueOf should return RUNNING for 'RUNNING'")
    void valueOfShouldReturnRunning() {
      assertEquals(
          InstanceState.RUNNING,
          InstanceState.valueOf("RUNNING"),
          "valueOf('RUNNING') should return RUNNING");
    }

    @Test
    @DisplayName("valueOf should return SUSPENDED for 'SUSPENDED'")
    void valueOfShouldReturnSuspended() {
      assertEquals(
          InstanceState.SUSPENDED,
          InstanceState.valueOf("SUSPENDED"),
          "valueOf('SUSPENDED') should return SUSPENDED");
    }

    @Test
    @DisplayName("valueOf should return ERROR for 'ERROR'")
    void valueOfShouldReturnError() {
      assertEquals(
          InstanceState.ERROR,
          InstanceState.valueOf("ERROR"),
          "valueOf('ERROR') should return ERROR");
    }

    @Test
    @DisplayName("valueOf should return DISPOSED for 'DISPOSED'")
    void valueOfShouldReturnDisposed() {
      assertEquals(
          InstanceState.DISPOSED,
          InstanceState.valueOf("DISPOSED"),
          "valueOf('DISPOSED') should return DISPOSED");
    }

    @Test
    @DisplayName("valueOf should return DESTROYING for 'DESTROYING'")
    void valueOfShouldReturnDestroying() {
      assertEquals(
          InstanceState.DESTROYING,
          InstanceState.valueOf("DESTROYING"),
          "valueOf('DESTROYING') should return DESTROYING");
    }
  }

  // ========================================================================
  // Enum name Tests
  // ========================================================================

  @Nested
  @DisplayName("Enum name Tests")
  class EnumNameTests {

    @Test
    @DisplayName("CREATING name should be 'CREATING'")
    void creatingNameShouldMatch() {
      assertEquals("CREATING", InstanceState.CREATING.name(), "CREATING name should be 'CREATING'");
    }

    @Test
    @DisplayName("CREATED name should be 'CREATED'")
    void createdNameShouldMatch() {
      assertEquals("CREATED", InstanceState.CREATED.name(), "CREATED name should be 'CREATED'");
    }

    @Test
    @DisplayName("RUNNING name should be 'RUNNING'")
    void runningNameShouldMatch() {
      assertEquals("RUNNING", InstanceState.RUNNING.name(), "RUNNING name should be 'RUNNING'");
    }

    @Test
    @DisplayName("SUSPENDED name should be 'SUSPENDED'")
    void suspendedNameShouldMatch() {
      assertEquals(
          "SUSPENDED", InstanceState.SUSPENDED.name(), "SUSPENDED name should be 'SUSPENDED'");
    }

    @Test
    @DisplayName("ERROR name should be 'ERROR'")
    void errorNameShouldMatch() {
      assertEquals("ERROR", InstanceState.ERROR.name(), "ERROR name should be 'ERROR'");
    }

    @Test
    @DisplayName("DISPOSED name should be 'DISPOSED'")
    void disposedNameShouldMatch() {
      assertEquals("DISPOSED", InstanceState.DISPOSED.name(), "DISPOSED name should be 'DISPOSED'");
    }

    @Test
    @DisplayName("DESTROYING name should be 'DESTROYING'")
    void destroyingNameShouldMatch() {
      assertEquals(
          "DESTROYING", InstanceState.DESTROYING.name(), "DESTROYING name should be 'DESTROYING'");
    }
  }
}
