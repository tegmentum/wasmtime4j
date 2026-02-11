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

package ai.tegmentum.wasmtime4j.parity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmMemory;
import ai.tegmentum.wasmtime4j.WasmRuntime;
import ai.tegmentum.wasmtime4j.factory.WasmRuntimeFactory;
import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Cross-runtime parity tests for Memory operations.
 *
 * <p>These tests verify that JNI and Panama runtime implementations produce identical results for
 * memory creation, read/write operations, and memory growth.
 *
 * <p>Test coverage:
 *
 * <ul>
 *   <li>Memory size and page operations
 *   <li>Memory read and write operations
 *   <li>Memory growth behavior
 *   <li>Byte-level data integrity
 * </ul>
 */
@DisplayName("Memory Parity Tests")
@Tag("integration")
class MemoryParityTest {

  private static final Logger LOGGER = Logger.getLogger(MemoryParityTest.class.getName());

  /** Simple WASM module with 1-page memory export. */
  private static final String MEMORY_MODULE_WAT =
      "(module "
          + "  (memory (export \"memory\") 1 16)"
          + "  (func (export \"get_size\") (result i32)"
          + "    memory.size)"
          + "  (func (export \"grow\") (param i32) (result i32)"
          + "    local.get 0"
          + "    memory.grow)"
          + ")";

  private static boolean jniAvailable;
  private static boolean panamaAvailable;

  private WasmRuntime jniRuntime;
  private WasmRuntime panamaRuntime;
  private Engine jniEngine;
  private Engine panamaEngine;
  private Store jniStore;
  private Store panamaStore;
  private Module jniModule;
  private Module panamaModule;
  private Instance jniInstance;
  private Instance panamaInstance;
  private boolean jniCreatedSuccessfully;
  private boolean panamaCreatedSuccessfully;

  @BeforeAll
  static void checkRuntimeAvailability() {
    LOGGER.info("Checking runtime availability for memory parity tests");

    jniAvailable = WasmRuntimeFactory.isRuntimeAvailable(RuntimeType.JNI);
    panamaAvailable = WasmRuntimeFactory.isRuntimeAvailable(RuntimeType.PANAMA);

    LOGGER.info("JNI runtime available: " + jniAvailable);
    LOGGER.info("Panama runtime available: " + panamaAvailable);
  }

  @BeforeEach
  void setUp() {
    LOGGER.info("Setting up memory parity test resources");

    jniCreatedSuccessfully = false;
    panamaCreatedSuccessfully = false;

    if (jniAvailable) {
      try {
        jniRuntime = WasmRuntimeFactory.create(RuntimeType.JNI);
        jniEngine = jniRuntime.createEngine();
        jniStore = jniRuntime.createStore(jniEngine);
        jniModule = jniRuntime.compileModuleWat(jniEngine, MEMORY_MODULE_WAT);
        jniInstance = jniStore.createInstance(jniModule);
        jniCreatedSuccessfully = true;
        LOGGER.info("JNI resources created successfully");
      } catch (final Exception e) {
        LOGGER.warning("Failed to create JNI resources: " + e.getMessage());
      }
    }

    if (panamaAvailable) {
      try {
        panamaRuntime = WasmRuntimeFactory.create(RuntimeType.PANAMA);
        panamaEngine = panamaRuntime.createEngine();
        panamaStore = panamaRuntime.createStore(panamaEngine);
        panamaModule = panamaRuntime.compileModuleWat(panamaEngine, MEMORY_MODULE_WAT);
        panamaInstance = panamaStore.createInstance(panamaModule);
        panamaCreatedSuccessfully = true;
        LOGGER.info("Panama resources created successfully");
      } catch (final Exception e) {
        LOGGER.warning("Failed to create Panama resources: " + e.getMessage());
      }
    }
  }

  @AfterEach
  void tearDown() {
    LOGGER.info("Cleaning up memory parity test resources");

    closeQuietly(jniInstance, "JNI instance");
    closeQuietly(jniModule, "JNI module");
    closeQuietly(jniStore, "JNI store");
    closeQuietly(jniEngine, "JNI engine");
    closeQuietly(jniRuntime, "JNI runtime");

    closeQuietly(panamaInstance, "Panama instance");
    closeQuietly(panamaModule, "Panama module");
    closeQuietly(panamaStore, "Panama store");
    closeQuietly(panamaEngine, "Panama engine");
    closeQuietly(panamaRuntime, "Panama runtime");
  }

  private void closeQuietly(final AutoCloseable resource, final String name) {
    if (resource != null) {
      try {
        resource.close();
        LOGGER.info(name + " closed");
      } catch (final Exception e) {
        LOGGER.warning("Error closing " + name + ": " + e.getMessage());
      }
    }
  }

  private void requireBothRuntimes() {
    assumeTrue(
        jniCreatedSuccessfully && panamaCreatedSuccessfully,
        "Both JNI and Panama runtimes required");
  }

  @Nested
  @DisplayName("Memory Size Parity Tests")
  class MemorySizeParityTests {

    @Test
    @DisplayName("should report same initial memory size on both runtimes")
    void shouldReportSameInitialSize() throws Exception {
      requireBothRuntimes();

      final Optional<WasmMemory> jniMemoryOpt = jniInstance.getMemory("memory");
      final Optional<WasmMemory> panamaMemoryOpt = panamaInstance.getMemory("memory");

      assertThat(jniMemoryOpt).isPresent();
      assertThat(panamaMemoryOpt).isPresent();

      final WasmMemory jniMemory = jniMemoryOpt.get();
      final WasmMemory panamaMemory = panamaMemoryOpt.get();

      final int jniSize = jniMemory.getSize();
      final int panamaSize = panamaMemory.getSize();

      LOGGER.info("JNI memory size: " + jniSize + " pages");
      LOGGER.info("Panama memory size: " + panamaSize + " pages");

      // Both should have 1 page initial
      assertThat(jniSize).isEqualTo(1);
      assertThat(panamaSize).isEqualTo(1);
      assertThat(jniSize).isEqualTo(panamaSize);
    }

    @Test
    @DisplayName("should report same page count via size() alias on both runtimes")
    void shouldReportSamePageCount() throws Exception {
      requireBothRuntimes();

      final WasmMemory jniMemory = jniInstance.getMemory("memory").orElseThrow();
      final WasmMemory panamaMemory = panamaInstance.getMemory("memory").orElseThrow();

      final int jniPages = jniMemory.size();
      final int panamaPages = panamaMemory.size();

      LOGGER.info("JNI memory pages: " + jniPages);
      LOGGER.info("Panama memory pages: " + panamaPages);

      assertThat(jniPages).isEqualTo(1);
      assertThat(panamaPages).isEqualTo(1);
      assertThat(jniPages).isEqualTo(panamaPages);
    }
  }

  @Nested
  @DisplayName("Memory Read/Write Parity Tests")
  class MemoryReadWriteParityTests {

    @Test
    @DisplayName("should write and read byte with same results on both runtimes")
    void shouldWriteAndReadByte() throws Exception {
      requireBothRuntimes();

      final WasmMemory jniMemory = jniInstance.getMemory("memory").orElseThrow();
      final WasmMemory panamaMemory = panamaInstance.getMemory("memory").orElseThrow();

      final byte testValue = (byte) 0xAB;
      final int offset = 100;

      // Write same value to both memories
      jniMemory.writeByte(offset, testValue);
      panamaMemory.writeByte(offset, testValue);

      // Read back and compare
      final byte jniValue = jniMemory.readByte(offset);
      final byte panamaValue = panamaMemory.readByte(offset);

      LOGGER.info("JNI read value: 0x" + Integer.toHexString(jniValue & 0xFF));
      LOGGER.info("Panama read value: 0x" + Integer.toHexString(panamaValue & 0xFF));

      assertThat(jniValue).isEqualTo(testValue);
      assertThat(panamaValue).isEqualTo(testValue);
    }

    @Test
    @DisplayName("should write and read int32 with same results on both runtimes")
    void shouldWriteAndReadInt32() throws Exception {
      requireBothRuntimes();

      final WasmMemory jniMemory = jniInstance.getMemory("memory").orElseThrow();
      final WasmMemory panamaMemory = panamaInstance.getMemory("memory").orElseThrow();

      final int testValue = 0xDEADBEEF;
      final long offset = 200L;

      // Write same value to both memories
      jniMemory.writeInt32(offset, testValue);
      panamaMemory.writeInt32(offset, testValue);

      // Read back and compare
      final int jniValue = jniMemory.readInt32(offset);
      final int panamaValue = panamaMemory.readInt32(offset);

      LOGGER.info("JNI read value: 0x" + Integer.toHexString(jniValue));
      LOGGER.info("Panama read value: 0x" + Integer.toHexString(panamaValue));

      assertThat(jniValue).isEqualTo(testValue);
      assertThat(panamaValue).isEqualTo(testValue);
    }

    @Test
    @DisplayName("should write and read int64 with same results on both runtimes")
    void shouldWriteAndReadInt64() throws Exception {
      requireBothRuntimes();

      final WasmMemory jniMemory = jniInstance.getMemory("memory").orElseThrow();
      final WasmMemory panamaMemory = panamaInstance.getMemory("memory").orElseThrow();

      final long testValue = 0xDEADBEEFCAFEBABEL;
      final long offset = 300L;

      // Write same value to both memories
      jniMemory.writeInt64(offset, testValue);
      panamaMemory.writeInt64(offset, testValue);

      // Read back and compare
      final long jniValue = jniMemory.readInt64(offset);
      final long panamaValue = panamaMemory.readInt64(offset);

      LOGGER.info("JNI read value: 0x" + Long.toHexString(jniValue));
      LOGGER.info("Panama read value: 0x" + Long.toHexString(panamaValue));

      assertThat(jniValue).isEqualTo(testValue);
      assertThat(panamaValue).isEqualTo(testValue);
    }

    @Test
    @DisplayName("should write and read byte array with same results on both runtimes")
    void shouldWriteAndReadByteArray() throws Exception {
      requireBothRuntimes();

      final WasmMemory jniMemory = jniInstance.getMemory("memory").orElseThrow();
      final WasmMemory panamaMemory = panamaInstance.getMemory("memory").orElseThrow();

      final byte[] testData = new byte[] {0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08};
      final int offset = 400;

      // Write same data to both memories
      jniMemory.writeBytes(offset, testData, 0, testData.length);
      panamaMemory.writeBytes(offset, testData, 0, testData.length);

      // Read back and compare
      final byte[] jniResult = new byte[testData.length];
      final byte[] panamaResult = new byte[testData.length];

      jniMemory.readBytes(offset, jniResult, 0, jniResult.length);
      panamaMemory.readBytes(offset, panamaResult, 0, panamaResult.length);

      LOGGER.info("JNI read " + jniResult.length + " bytes");
      LOGGER.info("Panama read " + panamaResult.length + " bytes");

      assertThat(jniResult).isEqualTo(testData);
      assertThat(panamaResult).isEqualTo(testData);
    }
  }

  @Nested
  @DisplayName("Memory Growth Parity Tests")
  class MemoryGrowthParityTests {

    @Test
    @DisplayName("should grow memory with same results on both runtimes")
    void shouldGrowMemory() throws Exception {
      requireBothRuntimes();

      final WasmMemory jniMemory = jniInstance.getMemory("memory").orElseThrow();
      final WasmMemory panamaMemory = panamaInstance.getMemory("memory").orElseThrow();

      final int jniInitialPages = jniMemory.getSize();
      final int panamaInitialPages = panamaMemory.getSize();

      LOGGER.info("JNI initial pages: " + jniInitialPages);
      LOGGER.info("Panama initial pages: " + panamaInitialPages);

      // Grow by 2 pages
      final int jniPreviousSize = jniMemory.grow(2);
      final int panamaPreviousSize = panamaMemory.grow(2);

      LOGGER.info("JNI previous size (pages): " + jniPreviousSize);
      LOGGER.info("Panama previous size (pages): " + panamaPreviousSize);

      // Both should return the previous page count
      assertThat(jniPreviousSize).isEqualTo(jniInitialPages);
      assertThat(panamaPreviousSize).isEqualTo(panamaInitialPages);

      // Check new size
      final int jniFinalPages = jniMemory.getSize();
      final int panamaFinalPages = panamaMemory.getSize();

      LOGGER.info("JNI final pages: " + jniFinalPages);
      LOGGER.info("Panama final pages: " + panamaFinalPages);

      assertThat(jniFinalPages).isEqualTo(jniInitialPages + 2);
      assertThat(panamaFinalPages).isEqualTo(panamaInitialPages + 2);
    }

    @Test
    @DisplayName("should have writable grown memory on both runtimes")
    void shouldWriteToGrownMemory() throws Exception {
      requireBothRuntimes();

      final WasmMemory jniMemory = jniInstance.getMemory("memory").orElseThrow();
      final WasmMemory panamaMemory = panamaInstance.getMemory("memory").orElseThrow();

      // Grow memory
      jniMemory.grow(1);
      panamaMemory.grow(1);

      // Write to the newly grown memory (second page)
      final long offset = 65536L; // Start of second page
      final int testValue = 0x12345678;

      jniMemory.writeInt32(offset, testValue);
      panamaMemory.writeInt32(offset, testValue);

      // Read back
      final int jniValue = jniMemory.readInt32(offset);
      final int panamaValue = panamaMemory.readInt32(offset);

      LOGGER.info("JNI value at offset " + offset + ": 0x" + Integer.toHexString(jniValue));
      LOGGER.info("Panama value at offset " + offset + ": 0x" + Integer.toHexString(panamaValue));

      assertThat(jniValue).isEqualTo(testValue);
      assertThat(panamaValue).isEqualTo(testValue);
    }
  }

  @Nested
  @DisplayName("Memory Buffer Parity Tests")
  class MemoryBufferParityTests {

    @Test
    @DisplayName("should provide equivalent byte buffers on both runtimes")
    void shouldProvideEquivalentByteBuffers() throws Exception {
      requireBothRuntimes();

      final WasmMemory jniMemory = jniInstance.getMemory("memory").orElseThrow();
      final WasmMemory panamaMemory = panamaInstance.getMemory("memory").orElseThrow();

      // Get buffers
      final ByteBuffer jniBuffer = jniMemory.getBuffer();
      final ByteBuffer panamaBuffer = panamaMemory.getBuffer();

      LOGGER.info("JNI buffer capacity: " + jniBuffer.capacity());
      LOGGER.info("Panama buffer capacity: " + panamaBuffer.capacity());

      // Both buffers should have same capacity (1 page = 64KB)
      assertThat(jniBuffer.capacity()).isEqualTo(panamaBuffer.capacity());
      assertThat(jniBuffer.capacity()).isEqualTo(64 * 1024);

      // Write through buffer
      jniBuffer.putInt(500, 0xABCDEF12);
      panamaBuffer.putInt(500, 0xABCDEF12);

      // Read through memory API
      final int jniValue = jniMemory.readInt32(500L);
      final int panamaValue = panamaMemory.readInt32(500L);

      assertThat(jniValue).isEqualTo(0xABCDEF12);
      assertThat(panamaValue).isEqualTo(0xABCDEF12);
    }
  }
}
