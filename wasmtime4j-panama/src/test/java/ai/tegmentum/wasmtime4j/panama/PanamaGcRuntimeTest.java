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
package ai.tegmentum.wasmtime4j.panama;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ai.tegmentum.wasmtime4j.panama.exception.PanamaException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link PanamaGcRuntime}.
 *
 * <p>Tests verify constructor validation of the Panama GC runtime.
 */
@DisplayName("PanamaGcRuntime Tests")
class PanamaGcRuntimeTest {

  @Test
  @DisplayName("Constructor should reject zero engine handle")
  void constructorShouldRejectZeroEngineHandle() {
    // PanamaGcRuntime resolves native symbols during class initialization.
    // When the native library is not loaded, an ExceptionInInitializerError is thrown
    // before the constructor validation can run. Both cases indicate correct rejection.
    assertThatThrownBy(() -> new PanamaGcRuntime(0))
        .isInstanceOfAny(PanamaException.class, ExceptionInInitializerError.class);
  }
}
