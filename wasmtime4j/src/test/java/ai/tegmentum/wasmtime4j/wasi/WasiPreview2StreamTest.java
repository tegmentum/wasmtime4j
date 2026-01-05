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

package ai.tegmentum.wasmtime4j.wasi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive test suite for the WasiPreview2Stream interface.
 *
 * <p>WasiPreview2Stream provides async stream-based I/O operations for non-blocking reading and
 * writing of data in WASI Preview 2.
 */
@DisplayName("WasiPreview2Stream Interface Tests")
class WasiPreview2StreamTest {

  // ========================================================================
  // Type Definition Tests
  // ========================================================================

  @Nested
  @DisplayName("Type Definition Tests")
  class TypeDefinitionTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeAnInterface() {
      assertTrue(
          WasiPreview2Stream.class.isInterface(), "WasiPreview2Stream should be an interface");
    }

    @Test
    @DisplayName("should be public")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(WasiPreview2Stream.class.getModifiers()),
          "WasiPreview2Stream should be public");
    }
  }

  // ========================================================================
  // Inheritance Tests
  // ========================================================================

  @Nested
  @DisplayName("Inheritance Tests")
  class InheritanceTests {

    @Test
    @DisplayName("should extend AutoCloseable")
    void shouldExtendAutoCloseable() {
      assertTrue(
          AutoCloseable.class.isAssignableFrom(WasiPreview2Stream.class),
          "WasiPreview2Stream should extend AutoCloseable");
    }

    @Test
    @DisplayName("should have exactly 1 interface")
    void shouldHaveExactlyOneInterface() {
      assertEquals(
          1,
          WasiPreview2Stream.class.getInterfaces().length,
          "WasiPreview2Stream should extend 1 interface");
    }
  }

  // ========================================================================
  // Stream Identification Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Stream Identification Method Tests")
  class StreamIdentificationMethodTests {

    @Test
    @DisplayName("should have getStreamId method")
    void shouldHaveGetStreamIdMethod() throws NoSuchMethodException {
      Method method = WasiPreview2Stream.class.getMethod("getStreamId");
      assertNotNull(method, "getStreamId method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have getStreamType method")
    void shouldHaveGetStreamTypeMethod() throws NoSuchMethodException {
      Method method = WasiPreview2Stream.class.getMethod("getStreamType");
      assertNotNull(method, "getStreamType method should exist");
      assertEquals(
          WasiPreview2Stream.WasiStreamType.class,
          method.getReturnType(),
          "Should return WasiStreamType");
    }
  }

  // ========================================================================
  // Stream Status Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Stream Status Method Tests")
  class StreamStatusMethodTests {

    @Test
    @DisplayName("should have isReady method")
    void shouldHaveIsReadyMethod() throws NoSuchMethodException {
      Method method = WasiPreview2Stream.class.getMethod("isReady");
      assertNotNull(method, "isReady method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have isClosed method")
    void shouldHaveIsClosedMethod() throws NoSuchMethodException {
      Method method = WasiPreview2Stream.class.getMethod("isClosed");
      assertNotNull(method, "isClosed method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have getStatus method")
    void shouldHaveGetStatusMethod() throws NoSuchMethodException {
      Method method = WasiPreview2Stream.class.getMethod("getStatus");
      assertNotNull(method, "getStatus method should exist");
      assertEquals(
          WasiPreview2Stream.WasiStreamStatus.class,
          method.getReturnType(),
          "Should return WasiStreamStatus");
    }
  }

  // ========================================================================
  // Async I/O Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Async I/O Method Tests")
  class AsyncIoMethodTests {

    @Test
    @DisplayName("should have readAsync method")
    void shouldHaveReadAsyncMethod() throws NoSuchMethodException {
      Method method = WasiPreview2Stream.class.getMethod("readAsync", ByteBuffer.class);
      assertNotNull(method, "readAsync method should exist");
      assertEquals(
          CompletableFuture.class, method.getReturnType(), "Should return CompletableFuture");
    }

    @Test
    @DisplayName("should have writeAsync method")
    void shouldHaveWriteAsyncMethod() throws NoSuchMethodException {
      Method method = WasiPreview2Stream.class.getMethod("writeAsync", ByteBuffer.class);
      assertNotNull(method, "writeAsync method should exist");
      assertEquals(
          CompletableFuture.class, method.getReturnType(), "Should return CompletableFuture");
    }

    @Test
    @DisplayName("should have flushAsync method")
    void shouldHaveFlushAsyncMethod() throws NoSuchMethodException {
      Method method = WasiPreview2Stream.class.getMethod("flushAsync");
      assertNotNull(method, "flushAsync method should exist");
      assertEquals(
          CompletableFuture.class, method.getReturnType(), "Should return CompletableFuture");
    }
  }

  // ========================================================================
  // Pollable Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Pollable Method Tests")
  class PollableMethodTests {

    @Test
    @DisplayName("should have createPollable method")
    void shouldHaveCreatePollableMethod() throws NoSuchMethodException {
      Method method = WasiPreview2Stream.class.getMethod("createPollable");
      assertNotNull(method, "createPollable method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }
  }

  // ========================================================================
  // Lifecycle Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Lifecycle Method Tests")
  class LifecycleMethodTests {

    @Test
    @DisplayName("should have close method")
    void shouldHaveCloseMethod() throws NoSuchMethodException {
      Method method = WasiPreview2Stream.class.getMethod("close");
      assertNotNull(method, "close method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }
  }

  // ========================================================================
  // WasiStreamType Enum Tests
  // ========================================================================

  @Nested
  @DisplayName("WasiStreamType Enum Tests")
  class WasiStreamTypeEnumTests {

    @Test
    @DisplayName("should be a nested enum")
    void shouldBeNestedEnum() {
      assertTrue(
          WasiPreview2Stream.WasiStreamType.class.isEnum(), "WasiStreamType should be an enum");
    }

    @Test
    @DisplayName("should be public")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(WasiPreview2Stream.WasiStreamType.class.getModifiers()),
          "WasiStreamType should be public");
    }

    @Test
    @DisplayName("should have INPUT type")
    void shouldHaveInputType() {
      assertNotNull(WasiPreview2Stream.WasiStreamType.valueOf("INPUT"), "INPUT should exist");
    }

    @Test
    @DisplayName("should have OUTPUT type")
    void shouldHaveOutputType() {
      assertNotNull(WasiPreview2Stream.WasiStreamType.valueOf("OUTPUT"), "OUTPUT should exist");
    }

    @Test
    @DisplayName("should have BIDIRECTIONAL type")
    void shouldHaveBidirectionalType() {
      assertNotNull(
          WasiPreview2Stream.WasiStreamType.valueOf("BIDIRECTIONAL"), "BIDIRECTIONAL should exist");
    }

    @Test
    @DisplayName("should have exactly 3 stream types")
    void shouldHaveExactly3StreamTypes() {
      assertEquals(
          3,
          WasiPreview2Stream.WasiStreamType.values().length,
          "Should have exactly 3 stream types");
    }
  }

  // ========================================================================
  // WasiStreamStatus Enum Tests
  // ========================================================================

  @Nested
  @DisplayName("WasiStreamStatus Enum Tests")
  class WasiStreamStatusEnumTests {

    @Test
    @DisplayName("should be a nested enum")
    void shouldBeNestedEnum() {
      assertTrue(
          WasiPreview2Stream.WasiStreamStatus.class.isEnum(), "WasiStreamStatus should be an enum");
    }

    @Test
    @DisplayName("should be public")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(WasiPreview2Stream.WasiStreamStatus.class.getModifiers()),
          "WasiStreamStatus should be public");
    }

    @Test
    @DisplayName("should have READY status")
    void shouldHaveReadyStatus() {
      assertNotNull(WasiPreview2Stream.WasiStreamStatus.valueOf("READY"), "READY should exist");
    }

    @Test
    @DisplayName("should have BLOCKED status")
    void shouldHaveBlockedStatus() {
      assertNotNull(WasiPreview2Stream.WasiStreamStatus.valueOf("BLOCKED"), "BLOCKED should exist");
    }

    @Test
    @DisplayName("should have EOF status")
    void shouldHaveEofStatus() {
      assertNotNull(WasiPreview2Stream.WasiStreamStatus.valueOf("EOF"), "EOF should exist");
    }

    @Test
    @DisplayName("should have CLOSED status")
    void shouldHaveClosedStatus() {
      assertNotNull(WasiPreview2Stream.WasiStreamStatus.valueOf("CLOSED"), "CLOSED should exist");
    }

    @Test
    @DisplayName("should have ERROR status")
    void shouldHaveErrorStatus() {
      assertNotNull(WasiPreview2Stream.WasiStreamStatus.valueOf("ERROR"), "ERROR should exist");
    }

    @Test
    @DisplayName("should have exactly 5 status values")
    void shouldHaveExactly5StatusValues() {
      assertEquals(
          5,
          WasiPreview2Stream.WasiStreamStatus.values().length,
          "Should have exactly 5 status values");
    }
  }

  // ========================================================================
  // Nested Classes Tests
  // ========================================================================

  @Nested
  @DisplayName("Nested Classes Tests")
  class NestedClassesTests {

    @Test
    @DisplayName("should have expected nested enums")
    void shouldHaveExpectedNestedEnums() {
      Set<String> nestedClassNames =
          Arrays.stream(WasiPreview2Stream.class.getDeclaredClasses())
              .map(Class::getSimpleName)
              .collect(Collectors.toSet());

      assertTrue(nestedClassNames.contains("WasiStreamType"), "Should have WasiStreamType");
      assertTrue(nestedClassNames.contains("WasiStreamStatus"), "Should have WasiStreamStatus");
    }

    @Test
    @DisplayName("should have exactly 2 nested types")
    void shouldHaveExactly2NestedTypes() {
      assertEquals(
          2,
          WasiPreview2Stream.class.getDeclaredClasses().length,
          "Should have 2 nested types (WasiStreamType, WasiStreamStatus)");
    }
  }

  // ========================================================================
  // Method Count Tests
  // ========================================================================

  @Nested
  @DisplayName("Method Count Tests")
  class MethodCountTests {

    @Test
    @DisplayName("should have at least 9 abstract methods")
    void shouldHaveExpectedMethodCount() {
      long abstractMethodCount =
          Arrays.stream(WasiPreview2Stream.class.getDeclaredMethods())
              .filter(m -> !m.isSynthetic())
              .filter(m -> Modifier.isAbstract(m.getModifiers()))
              .count();
      assertTrue(abstractMethodCount >= 9, "Should have at least 9 abstract methods");
    }
  }

  // ========================================================================
  // Field Tests
  // ========================================================================

  @Nested
  @DisplayName("Field Tests")
  class FieldTests {

    @Test
    @DisplayName("should have no declared fields")
    void shouldHaveNoDeclaredFields() {
      assertEquals(
          0,
          WasiPreview2Stream.class.getDeclaredFields().length,
          "WasiPreview2Stream should have no declared fields");
    }
  }
}
