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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.jni.exception.JniException;
import ai.tegmentum.wasmtime4j.jni.wasi.exception.WasiException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Comprehensive tests for {@link WasiEnvironmentOperationsPreview2}. */
@DisplayName("WasiEnvironmentOperationsPreview2 Tests")
class WasiEnvironmentOperationsPreview2Test {

  private WasiContext testContext;
  private ExecutorService executorService;
  private WasiEnvironmentOperationsPreview2 envOperations;

  @BeforeEach
  void setUp() {
    testContext = TestWasiContextFactory.createTestContext();
    executorService = Executors.newSingleThreadExecutor();
    envOperations = new WasiEnvironmentOperationsPreview2(testContext, executorService);
  }

  @AfterEach
  void tearDown() {
    if (executorService != null) {
      executorService.shutdown();
    }
  }

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("WasiEnvironmentOperationsPreview2 should be final class")
    void shouldBeFinalClass() {
      assertTrue(
          java.lang.reflect.Modifier.isFinal(
              WasiEnvironmentOperationsPreview2.class.getModifiers()),
          "WasiEnvironmentOperationsPreview2 should be final");
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("Constructor should throw on null context")
    void constructorShouldThrowOnNullContext() {
      assertThrows(
          JniException.class,
          () -> new WasiEnvironmentOperationsPreview2(null, executorService),
          "Should throw on null context");
    }

    @Test
    @DisplayName("Constructor should throw on null executor")
    void constructorShouldThrowOnNullExecutor() {
      assertThrows(
          JniException.class,
          () -> new WasiEnvironmentOperationsPreview2(testContext, null),
          "Should throw on null executor");
    }

    @Test
    @DisplayName("Constructor should create operations with valid parameters")
    void constructorShouldCreateOperationsWithValidParameters() {
      final WasiEnvironmentOperationsPreview2 ops =
          new WasiEnvironmentOperationsPreview2(testContext, executorService);
      assertNotNull(ops, "Operations should be created");
    }
  }

  @Nested
  @DisplayName("getEnvironment Tests")
  class GetEnvironmentTests {

    @Test
    @DisplayName("Should attempt to get environment")
    void shouldAttemptToGetEnvironment() {
      // Will throw due to native call failure (WasiException or UnsatisfiedLinkError)
      assertThrows(
          Throwable.class,
          () -> envOperations.getEnvironment(),
          "Should attempt to get environment");
    }
  }

  @Nested
  @DisplayName("getEnvironmentAsync Tests")
  class GetEnvironmentAsyncTests {

    @Test
    @DisplayName("Should return CompletableFuture")
    void shouldReturnCompletableFuture() {
      final CompletableFuture<?> future = envOperations.getEnvironmentAsync();

      assertNotNull(future, "Future should not be null");
      assertFalse(future.isDone(), "Future should not be immediately done");

      // Cancel to cleanup
      future.cancel(true);
    }
  }

  @Nested
  @DisplayName("getEnvironmentVariable Tests")
  class GetEnvironmentVariableTests {

    @Test
    @DisplayName("Should throw on null name")
    void shouldThrowOnNullName() {
      assertThrows(
          JniException.class,
          () -> envOperations.getEnvironmentVariable(null),
          "Should throw on null name");
    }

    @Test
    @DisplayName("Should throw on empty name")
    void shouldThrowOnEmptyName() {
      assertThrows(
          JniException.class,
          () -> envOperations.getEnvironmentVariable(""),
          "Should throw on empty name");
    }

    @Test
    @DisplayName("Should throw on name containing equals")
    void shouldThrowOnNameContainingEquals() {
      assertThrows(
          WasiException.class,
          () -> envOperations.getEnvironmentVariable("FOO=BAR"),
          "Should throw on name containing =");
    }

    @Test
    @DisplayName("Should throw on name containing null character")
    void shouldThrowOnNameContainingNullCharacter() {
      assertThrows(
          WasiException.class,
          () -> envOperations.getEnvironmentVariable("FOO\0BAR"),
          "Should throw on name containing null");
    }

    @Test
    @DisplayName("Should throw on name too long")
    void shouldThrowOnNameTooLong() {
      final StringBuilder longName = new StringBuilder();
      for (int i = 0; i < 32769; i++) {
        longName.append("A");
      }

      assertThrows(
          WasiException.class,
          () -> envOperations.getEnvironmentVariable(longName.toString()),
          "Should throw on name > 32KB");
    }
  }

  @Nested
  @DisplayName("getEnvironmentVariableAsync Tests")
  class GetEnvironmentVariableAsyncTests {

    @Test
    @DisplayName("Should throw on null name")
    void shouldThrowOnNullName() {
      assertThrows(
          JniException.class,
          () -> envOperations.getEnvironmentVariableAsync(null),
          "Should throw on null name");
    }

    @Test
    @DisplayName("Should throw on empty name")
    void shouldThrowOnEmptyName() {
      assertThrows(
          JniException.class,
          () -> envOperations.getEnvironmentVariableAsync(""),
          "Should throw on empty name");
    }

    @Test
    @DisplayName("Should return CompletableFuture")
    void shouldReturnCompletableFuture() {
      final CompletableFuture<?> future = envOperations.getEnvironmentVariableAsync("PATH");

      assertNotNull(future, "Future should not be null");

      // Cancel to cleanup
      future.cancel(true);
    }
  }

  @Nested
  @DisplayName("getArguments Tests")
  class GetArgumentsTests {

    @Test
    @DisplayName("Should attempt to get arguments")
    void shouldAttemptToGetArguments() {
      // Will throw due to native call failure (WasiException or UnsatisfiedLinkError)
      assertThrows(
          Throwable.class, () -> envOperations.getArguments(), "Should attempt to get arguments");
    }
  }

  @Nested
  @DisplayName("getArgumentsAsync Tests")
  class GetArgumentsAsyncTests {

    @Test
    @DisplayName("Should return CompletableFuture")
    void shouldReturnCompletableFuture() {
      final CompletableFuture<?> future = envOperations.getArgumentsAsync();

      assertNotNull(future, "Future should not be null");
      assertFalse(future.isDone(), "Future should not be immediately done");

      // Cancel to cleanup
      future.cancel(true);
    }
  }

  @Nested
  @DisplayName("getProgramName Tests")
  class GetProgramNameTests {

    @Test
    @DisplayName("Should attempt to get program name")
    void shouldAttemptToGetProgramName() {
      // Will throw due to native call failure (WasiException or UnsatisfiedLinkError)
      assertThrows(
          Throwable.class,
          () -> envOperations.getProgramName(),
          "Should attempt to get program name");
    }
  }

  @Nested
  @DisplayName("getProgramArguments Tests")
  class GetProgramArgumentsTests {

    @Test
    @DisplayName("Should attempt to get program arguments")
    void shouldAttemptToGetProgramArguments() {
      // Will throw due to native call failure (WasiException or UnsatisfiedLinkError)
      assertThrows(
          Throwable.class,
          () -> envOperations.getProgramArguments(),
          "Should attempt to get program arguments");
    }
  }

  @Nested
  @DisplayName("hasEnvironmentVariable Tests")
  class HasEnvironmentVariableTests {

    @Test
    @DisplayName("Should throw on null name")
    void shouldThrowOnNullName() {
      assertThrows(
          JniException.class,
          () -> envOperations.hasEnvironmentVariable(null),
          "Should throw on null name");
    }

    @Test
    @DisplayName("Should throw on empty name")
    void shouldThrowOnEmptyName() {
      assertThrows(
          JniException.class,
          () -> envOperations.hasEnvironmentVariable(""),
          "Should throw on empty name");
    }
  }

  @Nested
  @DisplayName("getEnvironmentVariableCount Tests")
  class GetEnvironmentVariableCountTests {

    @Test
    @DisplayName("Should attempt to get count")
    void shouldAttemptToGetCount() {
      // Will throw due to native call failure in getEnvironment() (WasiException or
      // UnsatisfiedLinkError)
      assertThrows(
          Throwable.class,
          () -> envOperations.getEnvironmentVariableCount(),
          "Should attempt to get count");
    }
  }

  @Nested
  @DisplayName("getArgumentCount Tests")
  class GetArgumentCountTests {

    @Test
    @DisplayName("Should attempt to get count")
    void shouldAttemptToGetCount() {
      // Will throw due to native call failure in getArguments() (WasiException or
      // UnsatisfiedLinkError)
      assertThrows(
          Throwable.class, () -> envOperations.getArgumentCount(), "Should attempt to get count");
    }
  }

  @Nested
  @DisplayName("exitAsync Tests")
  class ExitAsyncTests {

    @Test
    @DisplayName("Should return CompletableFuture")
    void shouldReturnCompletableFuture() {
      final CompletableFuture<Void> future = envOperations.exitAsync(0);

      assertNotNull(future, "Future should not be null");

      // Cancel to prevent actual exit
      future.cancel(true);
    }
  }
}
