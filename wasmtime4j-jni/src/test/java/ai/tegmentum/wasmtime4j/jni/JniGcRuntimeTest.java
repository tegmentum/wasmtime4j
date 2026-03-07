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
package ai.tegmentum.wasmtime4j.jni;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.jni.exception.JniException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link JniGcRuntime}.
 *
 * <p>These tests verify behavioral aspects of JNI GC runtime including constructor validation.
 */
@DisplayName("JniGcRuntime Tests")
class JniGcRuntimeTest {

  @Test
  @DisplayName("Constructor should reject zero engine handle")
  void constructorShouldRejectZeroEngineHandle() {
    JniException e = assertThrows(JniException.class, () -> new JniGcRuntime(0));
    assertTrue(
        e.getMessage().contains("Invalid engine handle"),
        "Expected message to contain: Invalid engine handle");
  }
}
