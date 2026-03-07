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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import ai.tegmentum.wasmtime4j.config.EngineConfig;
import ai.tegmentum.wasmtime4j.config.LinearMemory;
import ai.tegmentum.wasmtime4j.config.MemoryCreator;
import ai.tegmentum.wasmtime4j.config.StackCreator;
import ai.tegmentum.wasmtime4j.config.StackMemory;
import ai.tegmentum.wasmtime4j.tests.framework.DualRuntimeTest;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

/**
 * Integration tests for custom MemoryCreator and StackCreator engine configuration.
 *
 * <p>Validates that custom creator interfaces can be set on EngineConfig, and that the engine
 * accepts configurations with custom creators across both JNI and Panama implementations.
 */
class CustomCreatorConfigTest extends DualRuntimeTest {

  private static final Logger LOGGER = Logger.getLogger(CustomCreatorConfigTest.class.getName());

  @AfterEach
  void tearDown() {
    clearRuntimeSelection();
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  void testMemoryCreatorConfigRoundTrip(final RuntimeType runtime) {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing MemoryCreator config round-trip");

    final MemoryCreator creator =
        (type, minBytes, maxBytes, reservedBytes, guardBytes) -> new StubLinearMemory(minBytes);

    final EngineConfig config = Engine.builder().withHostMemory(creator);
    assertSame(creator, config.getMemoryCreator(), "getMemoryCreator should return the set creator");
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  void testMemoryCreatorNullThrows(final RuntimeType runtime) {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing MemoryCreator null throws");
    assertThrows(
        IllegalArgumentException.class,
        () -> Engine.builder().withHostMemory(null),
        "withHostMemory(null) should throw");
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  void testStackCreatorConfigRoundTrip(final RuntimeType runtime) {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing StackCreator config round-trip");

    final StackCreator creator = (size, zeroed) -> new StubStackMemory(size);

    final EngineConfig config = Engine.builder().withHostStack(creator);
    assertSame(creator, config.getStackCreator(), "getStackCreator should return the set creator");
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  void testStackCreatorNullThrows(final RuntimeType runtime) {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing StackCreator null throws");
    assertThrows(
        IllegalArgumentException.class,
        () -> Engine.builder().withHostStack(null),
        "withHostStack(null) should throw");
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  void testEngineCreationWithMemoryCreator(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing Engine creation with MemoryCreator set");

    final MemoryCreator creator =
        (type, minBytes, maxBytes, reservedBytes, guardBytes) -> new StubLinearMemory(minBytes);

    final EngineConfig config = Engine.builder().withHostMemory(creator);
    // Engine creation should succeed even with a custom creator configured
    try (Engine engine = Engine.create(config)) {
      assertNotNull(engine, "Engine should be created with MemoryCreator");
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  void testConfigCopyPreservesCreators(final RuntimeType runtime) {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing EngineConfig copy preserves creators");

    final MemoryCreator memCreator =
        (type, minBytes, maxBytes, reservedBytes, guardBytes) -> new StubLinearMemory(minBytes);
    final StackCreator stackCreator = (size, zeroed) -> new StubStackMemory(size);

    final EngineConfig original =
        Engine.builder().withHostMemory(memCreator).withHostStack(stackCreator);
    final EngineConfig copy = original.copy();

    assertSame(memCreator, copy.getMemoryCreator(), "Copy should preserve MemoryCreator");
    assertSame(stackCreator, copy.getStackCreator(), "Copy should preserve StackCreator");
  }

  /** Minimal stub LinearMemory for testing config wiring only. */
  private static final class StubLinearMemory implements LinearMemory {
    private final long size;

    StubLinearMemory(final long size) {
      this.size = size;
    }

    @Override
    public long byteSize() {
      return size;
    }

    @Override
    public long byteCapacity() {
      return size;
    }

    @Override
    public void growTo(final long newSize) {
      // stub
    }

    @Override
    public long basePointer() {
      return 0;
    }

    @Override
    public void close() {
      // stub
    }
  }

  /** Minimal stub StackMemory for testing config wiring only. */
  private static final class StubStackMemory implements StackMemory {
    private final long size;

    StubStackMemory(final long size) {
      this.size = size;
    }

    @Override
    public long top() {
      return size;
    }

    @Override
    public long rangeStart() {
      return 0;
    }

    @Override
    public long rangeEnd() {
      return size;
    }

    @Override
    public long guardRangeStart() {
      return 0;
    }

    @Override
    public long guardRangeEnd() {
      return 0;
    }

    @Override
    public void close() {
      // stub
    }
  }
}
