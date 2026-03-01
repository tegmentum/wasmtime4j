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
package ai.tegmentum.wasmtime4j.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link GcHeapOutOfMemoryException} class.
 *
 * <p>GcHeapOutOfMemoryException indicates GC heap exhaustion during struct/array allocation.
 */
@DisplayName("GcHeapOutOfMemoryException Tests")
class GcHeapOutOfMemoryExceptionTest {

  @Test
  @DisplayName("should create with message")
  void shouldCreateWithMessage() {
    final GcHeapOutOfMemoryException exception =
        new GcHeapOutOfMemoryException("GC heap exhausted after 1024 allocations");
    assertEquals("GC heap exhausted after 1024 allocations", exception.getMessage());
    assertNull(exception.getCause());
  }

  @Test
  @DisplayName("should create with message and cause")
  void shouldCreateWithMessageAndCause() {
    final OutOfMemoryError cause = new OutOfMemoryError("heap space");
    final GcHeapOutOfMemoryException exception =
        new GcHeapOutOfMemoryException("struct allocation failed", cause);
    assertEquals("struct allocation failed", exception.getMessage());
    assertEquals(cause, exception.getCause());
  }

  @Test
  @DisplayName("should extend WasmRuntimeException")
  void shouldExtendWasmRuntimeException() {
    final GcHeapOutOfMemoryException exception = new GcHeapOutOfMemoryException("test");
    assertTrue(exception instanceof WasmRuntimeException, "Should be a WasmRuntimeException");
    assertTrue(exception instanceof WasmException, "Should also be a WasmException");
  }

  @Test
  @DisplayName("should be serializable")
  void shouldBeSerializable() {
    final GcHeapOutOfMemoryException exception = new GcHeapOutOfMemoryException("test");
    assertNotNull(exception);
  }
}
