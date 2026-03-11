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
package ai.tegmentum.wasmtime4j.wasi.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Tests for the {@link WasiExit} interface constants and basic implementability. */
@DisplayName("WasiExit Tests")
class WasiExitTest {

  @Nested
  @DisplayName("Exit Status Constants")
  class ExitStatusConstants {

    @Test
    @DisplayName("EXIT_SUCCESS should be 0")
    void exitSuccessShouldBeZero() {
      assertEquals(0, WasiExit.EXIT_SUCCESS);
    }

    @Test
    @DisplayName("EXIT_FAILURE should be 1")
    void exitFailureShouldBeOne() {
      assertEquals(1, WasiExit.EXIT_FAILURE);
    }

    @Test
    @DisplayName("EXIT_SUCCESS and EXIT_FAILURE should be different")
    void exitStatusesShouldBeDifferent() {
      assertNotEquals(WasiExit.EXIT_SUCCESS, WasiExit.EXIT_FAILURE);
    }
  }

  @Nested
  @DisplayName("Interface Implementation")
  class InterfaceImplementation {

    @Test
    @DisplayName("should be implementable as lambda")
    void shouldBeImplementableAsLambda() {
      final int[] capturedCode = new int[] {-1};
      WasiExit exit = (statusCode) -> capturedCode[0] = statusCode;

      exit.exit(WasiExit.EXIT_SUCCESS);
      assertEquals(0, capturedCode[0]);

      exit.exit(WasiExit.EXIT_FAILURE);
      assertEquals(1, capturedCode[0]);

      exit.exit(42);
      assertEquals(42, capturedCode[0]);
    }
  }
}
