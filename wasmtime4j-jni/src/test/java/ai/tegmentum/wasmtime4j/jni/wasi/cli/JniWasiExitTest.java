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

package ai.tegmentum.wasmtime4j.jni.wasi.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;

import ai.tegmentum.wasmtime4j.wasi.cli.WasiExit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link JniWasiExit} class.
 *
 * <p>JniWasiExit provides JNI-based access to WASI Preview 2 program termination operations.
 */
@DisplayName("JniWasiExit Tests")
class JniWasiExitTest {

  @Nested
  @DisplayName("Interface Constants Tests")
  class InterfaceConstantsTests {

    @Test
    @DisplayName("WasiExit should have EXIT_SUCCESS constant")
    void wasiExitShouldHaveExitSuccessConstant() {
      assertEquals(0, WasiExit.EXIT_SUCCESS, "EXIT_SUCCESS should be 0");
    }

    @Test
    @DisplayName("WasiExit should have EXIT_FAILURE constant")
    void wasiExitShouldHaveExitFailureConstant() {
      assertEquals(1, WasiExit.EXIT_FAILURE, "EXIT_FAILURE should be 1");
    }
  }
}
