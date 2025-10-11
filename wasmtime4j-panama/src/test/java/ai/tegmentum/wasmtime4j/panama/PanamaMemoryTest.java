package ai.tegmentum.wasmtime4j.panama;

import static org.junit.jupiter.api.Assertions.*;

import ai.tegmentum.wasmtime4j.WasmMemory;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Test suite for PanamaMemory operations. */
public class PanamaMemoryTest {

  private static final int PAGE_SIZE = 65536; // 64KB

  private PanamaEngine engine;
  private PanamaStore store;
  private PanamaModule module;
  private PanamaInstance instance;

  /** Sets up test fixtures before each test. */
  @BeforeEach
  public void setUp() throws Exception {
    // Load the test WASM file with exported memory (1 initial page, 10 max pages)
    final Path wasmPath =
        Paths.get(getClass().getClassLoader().getResource("wasm/exports-test.wasm").toURI());
    final byte[] wasmBytes = Files.readAllBytes(wasmPath);

    engine = new PanamaEngine();
    store = new PanamaStore(engine);
    module = new PanamaModule(engine, wasmBytes);
    instance = new PanamaInstance(module, store);
  }

  /** Cleans up test fixtures after each test. */
  @AfterEach
  public void tearDown() {
    if (instance != null) {
      instance.close();
    }
    if (store != null) {
      store.close();
    }
    if (module != null) {
      module.close();
    }
    if (engine != null) {
      engine.close();
    }
  }

  @Test
  @DisplayName("Get initial memory size")
  public void testGetInitialSize() {
    final Optional<WasmMemory> memoryOpt = instance.getMemory("memory");
    assertTrue(memoryOpt.isPresent(), "Memory should be present");

    final WasmMemory memory = memoryOpt.get();
    assertEquals(1, memory.getSize(), "Initial memory size should be 1 page");
  }

  @Test
  @DisplayName("Grow memory by valid amount")
  public void testGrowMemory() {
    final Optional<WasmMemory> memoryOpt = instance.getMemory("memory");
    assertTrue(memoryOpt.isPresent());

    final WasmMemory memory = memoryOpt.get();
    final int previousSize = memory.grow(2);
    assertEquals(1, previousSize, "Previous size should be 1 page");
    assertEquals(3, memory.getSize(), "New size should be 3 pages");
  }

  @Test
  @DisplayName("Write and read bytes")
  public void testWriteAndReadBytes() {
    final Optional<WasmMemory> memoryOpt = instance.getMemory("memory");
    assertTrue(memoryOpt.isPresent());

    final WasmMemory memory = memoryOpt.get();

    // Write bytes
    final byte[] data = {0x01, 0x02, 0x03, 0x04, 0x05};
    memory.writeBytes(0, data, 0, data.length);

    // Read bytes back
    final byte[] readData = new byte[data.length];
    memory.readBytes(0, readData, 0, data.length);

    assertArrayEquals(data, readData, "Read data should match written data");
  }

  @Test
  @DisplayName("Write and read bytes with offsets")
  public void testWriteAndReadBytesWithOffsets() {
    final Optional<WasmMemory> memoryOpt = instance.getMemory("memory");
    assertTrue(memoryOpt.isPresent());

    final WasmMemory memory = memoryOpt.get();

    // Write bytes at offset 100
    final byte[] data = {0x0A, 0x0B, 0x0C, 0x0D};
    memory.writeBytes(100, data, 0, data.length);

    // Read bytes back from offset 100
    final byte[] readData = new byte[data.length];
    memory.readBytes(100, readData, 0, data.length);

    assertArrayEquals(data, readData, "Read data should match written data");
  }

  @Test
  @DisplayName("Write and read with array offsets")
  public void testWriteAndReadWithArrayOffsets() {
    final Optional<WasmMemory> memoryOpt = instance.getMemory("memory");
    assertTrue(memoryOpt.isPresent());

    final WasmMemory memory = memoryOpt.get();

    // Write partial array
    final byte[] sourceData = {0x00, 0x11, 0x22, 0x33, 0x44, 0x55};
    memory.writeBytes(0, sourceData, 2, 3); // Write 0x22, 0x33, 0x44

    // Read back
    final byte[] readData = new byte[5];
    memory.readBytes(0, readData, 1, 3); // Read into offset 1

    assertEquals(0x00, readData[0], "Byte at 0 should be untouched");
    assertEquals(0x22, readData[1], "Byte at 1 should be 0x22");
    assertEquals(0x33, readData[2], "Byte at 2 should be 0x33");
    assertEquals(0x44, readData[3], "Byte at 3 should be 0x44");
    assertEquals(0x00, readData[4], "Byte at 4 should be untouched");
  }

  @Test
  @DisplayName("Get buffer returns snapshot")
  public void testGetBuffer() {
    final Optional<WasmMemory> memoryOpt = instance.getMemory("memory");
    assertTrue(memoryOpt.isPresent());

    final WasmMemory memory = memoryOpt.get();

    // Write some data
    final byte[] data = {0x10, 0x20, 0x30, 0x40};
    memory.writeBytes(0, data, 0, data.length);

    // Get buffer
    final ByteBuffer buffer = memory.getBuffer();
    assertNotNull(buffer, "Buffer should not be null");
    assertEquals(PAGE_SIZE, buffer.remaining(), "Buffer size should be one page");

    // Verify data
    assertEquals(0x10, buffer.get(0));
    assertEquals(0x20, buffer.get(1));
    assertEquals(0x30, buffer.get(2));
    assertEquals(0x40, buffer.get(3));
  }

  @Test
  @DisplayName("Write large data")
  public void testWriteLargeData() {
    final Optional<WasmMemory> memoryOpt = instance.getMemory("memory");
    assertTrue(memoryOpt.isPresent());

    final WasmMemory memory = memoryOpt.get();

    // Write 1KB of data
    final byte[] data = new byte[1024];
    for (int i = 0; i < data.length; i++) {
      data[i] = (byte) (i % 256);
    }
    memory.writeBytes(0, data, 0, data.length);

    // Read back and verify
    final byte[] readData = new byte[data.length];
    memory.readBytes(0, readData, 0, data.length);

    assertArrayEquals(data, readData, "Large data should match");
  }

  @Test
  @DisplayName("Write at end of page")
  public void testWriteAtEndOfPage() {
    final Optional<WasmMemory> memoryOpt = instance.getMemory("memory");
    assertTrue(memoryOpt.isPresent());

    final WasmMemory memory = memoryOpt.get();

    // Write at the last bytes of the page
    final byte[] data = {0x01, 0x02, 0x03, 0x04};
    final int offset = PAGE_SIZE - data.length;
    memory.writeBytes(offset, data, 0, data.length);

    // Read back
    final byte[] readData = new byte[data.length];
    memory.readBytes(offset, readData, 0, data.length);

    assertArrayEquals(data, readData, "Data at end of page should match");
  }

  @Test
  @DisplayName("Grow memory multiple times")
  public void testGrowMultipleTimes() {
    final Optional<WasmMemory> memoryOpt = instance.getMemory("memory");
    assertTrue(memoryOpt.isPresent());

    final WasmMemory memory = memoryOpt.get();

    assertEquals(1, memory.getSize(), "Initial size should be 1");

    int previousSize = memory.grow(1);
    assertEquals(1, previousSize, "Previous size should be 1");
    assertEquals(2, memory.getSize(), "Size after first grow should be 2");

    previousSize = memory.grow(2);
    assertEquals(2, previousSize, "Previous size should be 2");
    assertEquals(4, memory.getSize(), "Size after second grow should be 4");
  }

  @Test
  @DisplayName("Write and read zero bytes")
  public void testWriteAndReadZeroBytes() {
    final Optional<WasmMemory> memoryOpt = instance.getMemory("memory");
    assertTrue(memoryOpt.isPresent());

    final WasmMemory memory = memoryOpt.get();

    // Write zero bytes (should not throw)
    final byte[] data = new byte[0];
    assertDoesNotThrow(() -> memory.writeBytes(0, data, 0, 0));

    // Read zero bytes (should not throw)
    assertDoesNotThrow(() -> memory.readBytes(0, data, 0, 0));
  }

  @Test
  @DisplayName("Grow by zero pages")
  public void testGrowByZeroPages() {
    final Optional<WasmMemory> memoryOpt = instance.getMemory("memory");
    assertTrue(memoryOpt.isPresent());

    final WasmMemory memory = memoryOpt.get();

    final int previousSize = memory.grow(0);
    assertEquals(1, previousSize, "Growing by 0 should return current size");
    assertEquals(1, memory.getSize(), "Size should remain unchanged");
  }

  @Test
  @DisplayName("Negative offset throws exception")
  public void testNegativeOffsetThrows() {
    final Optional<WasmMemory> memoryOpt = instance.getMemory("memory");
    assertTrue(memoryOpt.isPresent());

    final WasmMemory memory = memoryOpt.get();
    final byte[] data = new byte[10];

    assertThrows(
        IndexOutOfBoundsException.class,
        () -> memory.writeBytes(-1, data, 0, data.length),
        "Negative offset should throw");

    assertThrows(
        IndexOutOfBoundsException.class,
        () -> memory.readBytes(-1, data, 0, data.length),
        "Negative offset should throw");
  }

  @Test
  @DisplayName("Null array throws exception")
  public void testNullArrayThrows() {
    final Optional<WasmMemory> memoryOpt = instance.getMemory("memory");
    assertTrue(memoryOpt.isPresent());

    final WasmMemory memory = memoryOpt.get();

    assertThrows(
        IllegalArgumentException.class,
        () -> memory.writeBytes(0, null, 0, 10),
        "Null source array should throw");

    assertThrows(
        IllegalArgumentException.class,
        () -> memory.readBytes(0, null, 0, 10),
        "Null destination array should throw");
  }

  @Test
  @DisplayName("Negative array offset throws exception")
  public void testNegativeArrayOffsetThrows() {
    final Optional<WasmMemory> memoryOpt = instance.getMemory("memory");
    assertTrue(memoryOpt.isPresent());

    final WasmMemory memory = memoryOpt.get();
    final byte[] data = new byte[10];

    assertThrows(
        IndexOutOfBoundsException.class,
        () -> memory.writeBytes(0, data, -1, 5),
        "Negative array offset should throw");

    assertThrows(
        IndexOutOfBoundsException.class,
        () -> memory.readBytes(0, data, -1, 5),
        "Negative array offset should throw");
  }

  @Test
  @DisplayName("Negative length throws exception")
  public void testNegativeLengthThrows() {
    final Optional<WasmMemory> memoryOpt = instance.getMemory("memory");
    assertTrue(memoryOpt.isPresent());

    final WasmMemory memory = memoryOpt.get();
    final byte[] data = new byte[10];

    assertThrows(
        IndexOutOfBoundsException.class,
        () -> memory.writeBytes(0, data, 0, -1),
        "Negative length should throw");

    assertThrows(
        IndexOutOfBoundsException.class,
        () -> memory.readBytes(0, data, 0, -1),
        "Negative length should throw");
  }

  @Test
  @DisplayName("Array bounds exceeded throws exception")
  public void testArrayBoundsExceededThrows() {
    final Optional<WasmMemory> memoryOpt = instance.getMemory("memory");
    assertTrue(memoryOpt.isPresent());

    final WasmMemory memory = memoryOpt.get();
    final byte[] data = new byte[10];

    assertThrows(
        IndexOutOfBoundsException.class,
        () -> memory.writeBytes(0, data, 5, 10),
        "Array bounds exceeded should throw");

    assertThrows(
        IndexOutOfBoundsException.class,
        () -> memory.readBytes(0, data, 5, 10),
        "Array bounds exceeded should throw");
  }

  @Test
  @DisplayName("Grow by negative pages throws exception")
  public void testGrowByNegativePagesThrows() {
    final Optional<WasmMemory> memoryOpt = instance.getMemory("memory");
    assertTrue(memoryOpt.isPresent());

    final WasmMemory memory = memoryOpt.get();

    assertThrows(
        IllegalArgumentException.class,
        () -> memory.grow(-1),
        "Growing by negative pages should throw");
  }

  @Test
  @DisplayName("Write data persists across reads")
  public void testDataPersistence() {
    final Optional<WasmMemory> memoryOpt = instance.getMemory("memory");
    assertTrue(memoryOpt.isPresent());

    final WasmMemory memory = memoryOpt.get();

    // Write data
    final byte[] data1 = {0x11, 0x22, 0x33};
    memory.writeBytes(0, data1, 0, data1.length);

    // Read first time
    final byte[] read1 = new byte[data1.length];
    memory.readBytes(0, read1, 0, data1.length);
    assertArrayEquals(data1, read1);

    // Read second time
    final byte[] read2 = new byte[data1.length];
    memory.readBytes(0, read2, 0, data1.length);
    assertArrayEquals(data1, read2);
  }

  @Test
  @DisplayName("Multiple writes to different offsets")
  public void testMultipleWritesDifferentOffsets() {
    final Optional<WasmMemory> memoryOpt = instance.getMemory("memory");
    assertTrue(memoryOpt.isPresent());

    final WasmMemory memory = memoryOpt.get();

    // Write at offset 0
    final byte[] data1 = {0x01, 0x02, 0x03};
    memory.writeBytes(0, data1, 0, data1.length);

    // Write at offset 100
    final byte[] data2 = {0x04, 0x05, 0x06};
    memory.writeBytes(100, data2, 0, data2.length);

    // Write at offset 200
    final byte[] data3 = {0x07, 0x08, 0x09};
    memory.writeBytes(200, data3, 0, data3.length);

    // Read back all three
    final byte[] read1 = new byte[3];
    memory.readBytes(0, read1, 0, 3);
    assertArrayEquals(data1, read1);

    final byte[] read2 = new byte[3];
    memory.readBytes(100, read2, 0, 3);
    assertArrayEquals(data2, read2);

    final byte[] read3 = new byte[3];
    memory.readBytes(200, read3, 0, 3);
    assertArrayEquals(data3, read3);
  }
}
