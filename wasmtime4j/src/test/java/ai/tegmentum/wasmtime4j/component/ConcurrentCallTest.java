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
package ai.tegmentum.wasmtime4j.component;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ConcurrentCall}.
 *
 * @since 1.1.0
 */
@DisplayName("ConcurrentCall")
class ConcurrentCallTest {

  @Test
  @DisplayName("creates call with varargs factory")
  void createsWithVarargs() {
    final ConcurrentCall call = ConcurrentCall.of("add", ComponentVal.s32(1), ComponentVal.s32(2));
    assertEquals("add", call.getFunctionName(), "Function name should be 'add'");
    assertEquals(2, call.getArgs().size(), "Should have 2 args");
    assertEquals(1, call.getArgs().get(0).asS32(), "First arg should be 1");
    assertEquals(2, call.getArgs().get(1).asS32(), "Second arg should be 2");
  }

  @Test
  @DisplayName("creates call with list factory")
  void createsWithList() {
    final List<ComponentVal> args = List.of(ComponentVal.string("hello"));
    final ConcurrentCall call = ConcurrentCall.of("greet", args);
    assertEquals("greet", call.getFunctionName(), "Function name should be 'greet'");
    assertEquals(1, call.getArgs().size(), "Should have 1 arg");
    assertEquals("hello", call.getArgs().get(0).asString(), "Arg should be 'hello'");
  }

  @Test
  @DisplayName("creates call with no arguments")
  void createsWithNoArgs() {
    final ConcurrentCall call = ConcurrentCall.of("noop");
    assertEquals("noop", call.getFunctionName(), "Function name should be 'noop'");
    assertTrue(call.getArgs().isEmpty(), "Should have no args");
  }

  @Test
  @DisplayName("args list is unmodifiable")
  void argsAreUnmodifiable() {
    final ConcurrentCall call = ConcurrentCall.of("test", ComponentVal.s32(1));
    assertThrows(
        UnsupportedOperationException.class,
        () -> call.getArgs().add(ComponentVal.s32(2)),
        "Args list should be unmodifiable");
  }

  @Test
  @DisplayName("rejects null function name")
  void rejectsNullName() {
    assertThrows(
        IllegalArgumentException.class,
        () -> ConcurrentCall.of(null, ComponentVal.s32(1)),
        "Should reject null function name");
  }

  @Test
  @DisplayName("rejects empty function name")
  void rejectsEmptyName() {
    assertThrows(
        IllegalArgumentException.class,
        () -> ConcurrentCall.of("", ComponentVal.s32(1)),
        "Should reject empty function name");
  }

  @Test
  @DisplayName("toString includes function name and args")
  void toStringIncludesDetails() {
    final ConcurrentCall call = ConcurrentCall.of("test", ComponentVal.s32(42));
    final String str = call.toString();
    assertNotNull(str, "toString should not return null");
    assertTrue(str.contains("test"), "toString should contain function name");
  }
}
