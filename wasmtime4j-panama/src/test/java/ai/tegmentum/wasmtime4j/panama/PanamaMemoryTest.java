package ai.tegmentum.wasmtime4j.panama;

import static org.junit.jupiter.api.Assertions.*;

import ai.tegmentum.wasmtime4j.MemoryType;
import ai.tegmentum.wasmtime4j.WasmMemory;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Test suite for PanamaMemory operations. */
@DisplayName("Panama Memory Tests")
public class PanamaMemoryTest {

  private static final Logger LOGGER = Logger.getLogger(PanamaMemoryTest.class.getName());
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

  /** Helper to get the test memory export from the instance. */
  private WasmMemory getMemory() {
    final Optional<WasmMemory> memoryOpt = instance.getMemory("memory");
    assertTrue(memoryOpt.isPresent(), "Memory export 'memory' should be present");
    return memoryOpt.get();
  }

  // ==================== Existing basic tests ====================

  @Test
  @DisplayName("Get initial memory size")
  public void testGetInitialSize() {
    final WasmMemory memory = getMemory();
    assertEquals(1, memory.getSize(), "Initial memory size should be 1 page");
  }

  @Test
  @DisplayName("Grow memory by valid amount")
  public void testGrowMemory() {
    final WasmMemory memory = getMemory();
    final int previousSize = memory.grow(2);
    assertEquals(1, previousSize, "Previous size should be 1 page");
    assertEquals(3, memory.getSize(), "New size should be 3 pages");
  }

  @Test
  @DisplayName("Write and read bytes")
  public void testWriteAndReadBytes() {
    final WasmMemory memory = getMemory();

    final byte[] data = {0x01, 0x02, 0x03, 0x04, 0x05};
    memory.writeBytes(0, data, 0, data.length);

    final byte[] readData = new byte[data.length];
    memory.readBytes(0, readData, 0, data.length);

    assertArrayEquals(data, readData, "Read data should match written data");
  }

  @Test
  @DisplayName("Write and read bytes with offsets")
  public void testWriteAndReadBytesWithOffsets() {
    final WasmMemory memory = getMemory();

    final byte[] data = {0x0A, 0x0B, 0x0C, 0x0D};
    memory.writeBytes(100, data, 0, data.length);

    final byte[] readData = new byte[data.length];
    memory.readBytes(100, readData, 0, data.length);

    assertArrayEquals(data, readData, "Read data should match written data");
  }

  @Test
  @DisplayName("Write and read with array offsets")
  public void testWriteAndReadWithArrayOffsets() {
    final WasmMemory memory = getMemory();

    final byte[] sourceData = {0x00, 0x11, 0x22, 0x33, 0x44, 0x55};
    memory.writeBytes(0, sourceData, 2, 3); // Write 0x22, 0x33, 0x44

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
    final WasmMemory memory = getMemory();

    final byte[] data = {0x10, 0x20, 0x30, 0x40};
    memory.writeBytes(0, data, 0, data.length);

    final ByteBuffer buffer = memory.getBuffer();
    assertNotNull(buffer, "Buffer should not be null");
    assertEquals(PAGE_SIZE, buffer.remaining(), "Buffer size should be one page");

    assertEquals(0x10, buffer.get(0));
    assertEquals(0x20, buffer.get(1));
    assertEquals(0x30, buffer.get(2));
    assertEquals(0x40, buffer.get(3));
  }

  @Test
  @DisplayName("Write large data")
  public void testWriteLargeData() {
    final WasmMemory memory = getMemory();

    final byte[] data = new byte[1024];
    for (int i = 0; i < data.length; i++) {
      data[i] = (byte) (i % 256);
    }
    memory.writeBytes(0, data, 0, data.length);

    final byte[] readData = new byte[data.length];
    memory.readBytes(0, readData, 0, data.length);

    assertArrayEquals(data, readData, "Large data should match");
  }

  @Test
  @DisplayName("Write at end of page")
  public void testWriteAtEndOfPage() {
    final WasmMemory memory = getMemory();

    final byte[] data = {0x01, 0x02, 0x03, 0x04};
    final int offset = PAGE_SIZE - data.length;
    memory.writeBytes(offset, data, 0, data.length);

    final byte[] readData = new byte[data.length];
    memory.readBytes(offset, readData, 0, data.length);

    assertArrayEquals(data, readData, "Data at end of page should match");
  }

  @Test
  @DisplayName("Grow memory multiple times")
  public void testGrowMultipleTimes() {
    final WasmMemory memory = getMemory();

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
    final WasmMemory memory = getMemory();

    final byte[] data = new byte[0];
    assertDoesNotThrow(() -> memory.writeBytes(0, data, 0, 0));
    assertDoesNotThrow(() -> memory.readBytes(0, data, 0, 0));
  }

  @Test
  @DisplayName("Grow by zero pages")
  public void testGrowByZeroPages() {
    final WasmMemory memory = getMemory();

    final int previousSize = memory.grow(0);
    assertEquals(1, previousSize, "Growing by 0 should return current size");
    assertEquals(1, memory.getSize(), "Size should remain unchanged");
  }

  @Test
  @DisplayName("Negative offset throws exception")
  public void testNegativeOffsetThrows() {
    final WasmMemory memory = getMemory();
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
    final WasmMemory memory = getMemory();

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
    final WasmMemory memory = getMemory();
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
    final WasmMemory memory = getMemory();
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
    final WasmMemory memory = getMemory();
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
    final WasmMemory memory = getMemory();

    assertThrows(
        IllegalArgumentException.class,
        () -> memory.grow(-1),
        "Growing by negative pages should throw");
  }

  @Test
  @DisplayName("Write data persists across reads")
  public void testDataPersistence() {
    final WasmMemory memory = getMemory();

    final byte[] data1 = {0x11, 0x22, 0x33};
    memory.writeBytes(0, data1, 0, data1.length);

    final byte[] read1 = new byte[data1.length];
    memory.readBytes(0, read1, 0, data1.length);
    assertArrayEquals(data1, read1);

    final byte[] read2 = new byte[data1.length];
    memory.readBytes(0, read2, 0, data1.length);
    assertArrayEquals(data1, read2);
  }

  @Test
  @DisplayName("Multiple writes to different offsets")
  public void testMultipleWritesDifferentOffsets() {
    final WasmMemory memory = getMemory();

    final byte[] data1 = {0x01, 0x02, 0x03};
    memory.writeBytes(0, data1, 0, data1.length);

    final byte[] data2 = {0x04, 0x05, 0x06};
    memory.writeBytes(100, data2, 0, data2.length);

    final byte[] data3 = {0x07, 0x08, 0x09};
    memory.writeBytes(200, data3, 0, data3.length);

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

  // ==================== New test groups ====================

  @Nested
  @DisplayName("Max Size Tests")
  class MaxSizeTests {

    @Test
    @DisplayName("Should return positive max size for bounded memory")
    public void shouldReturnPositiveMaxSize() throws Exception {
      final WasmMemory memory = getMemory();
      final int maxSize = memory.getMaxSize();
      LOGGER.info("Memory max size: " + maxSize + " pages");
      assertTrue(maxSize > 0, "Max size should be positive for bounded memory");
      assertTrue(
          maxSize >= memory.getSize(),
          "Max size (" + maxSize + ") should be >= current size (" + memory.getSize() + ")");
    }

    @Test
    @DisplayName("Should return max size of 10 pages for exports-test module")
    public void shouldReturnCorrectMaxSize() throws Exception {
      final WasmMemory memory = getMemory();
      final int maxSize = memory.getMaxSize();
      // exports-test.wasm has (memory 1 10) - 10 max pages
      assertEquals(10, maxSize, "Max size should be 10 pages for exports-test module");
    }

    @Test
    @DisplayName("Should maintain max size after grow")
    public void shouldMaintainMaxSizeAfterGrow() throws Exception {
      final WasmMemory memory = getMemory();
      final int maxBefore = memory.getMaxSize();
      memory.grow(2);
      final int maxAfter = memory.getMaxSize();
      assertEquals(maxBefore, maxAfter, "Max size should not change after grow");
    }
  }

  @Nested
  @DisplayName("Memory Type Tests")
  class MemoryTypeTests {

    @Test
    @DisplayName("Should return non-null memory type")
    public void shouldReturnNonNullMemoryType() throws Exception {
      final WasmMemory memory = getMemory();
      final MemoryType memType = memory.getMemoryType();
      assertNotNull(memType, "Memory type should not be null");
      LOGGER.info("Memory type: " + memType);
    }

    @Test
    @DisplayName("Should have minimum of 1 page")
    public void shouldHaveCorrectMinimum() throws Exception {
      final WasmMemory memory = getMemory();
      final MemoryType memType = memory.getMemoryType();
      assertEquals(1, memType.getMinimum(), "Minimum should be 1 page");
    }

    @Test
    @DisplayName("Should have maximum of 10 pages")
    public void shouldHaveMaximum() throws Exception {
      final WasmMemory memory = getMemory();
      final MemoryType memType = memory.getMemoryType();
      final Optional<Long> max = memType.getMaximum();
      assertTrue(max.isPresent(), "Maximum should be present for bounded memory");
      assertEquals(10L, max.get(), "Maximum should be 10 pages");
    }

    @Test
    @DisplayName("Should not be 64-bit")
    public void shouldNotBe64Bit() throws Exception {
      final WasmMemory memory = getMemory();
      final MemoryType memType = memory.getMemoryType();
      assertFalse(memType.is64Bit(), "Regular memory should not be 64-bit");
    }

    @Test
    @DisplayName("Should not be shared")
    public void shouldNotBeSharedFromType() throws Exception {
      final WasmMemory memory = getMemory();
      final MemoryType memType = memory.getMemoryType();
      assertFalse(memType.isShared(), "Regular memory should not be shared");
    }
  }

  @Nested
  @DisplayName("Read Byte Tests")
  class ReadByteTests {

    @Test
    @DisplayName("Should read byte at offset after writing bytes via writeBytes")
    public void shouldReadByteAfterWrite() throws Exception {
      final WasmMemory memory = getMemory();

      // Write known data using writeBytes (which writes to native memory)
      final byte[] data = {0x41, 0x42, 0x43};
      memory.writeBytes(0, data, 0, data.length);

      // Read individual bytes using readByte
      final byte result = memory.readByte(0);
      assertEquals(0x41, result, "readByte(0) should return 0x41");
      assertEquals(0x42, memory.readByte(1), "readByte(1) should return 0x42");
      assertEquals(0x43, memory.readByte(2), "readByte(2) should return 0x43");
    }

    @Test
    @DisplayName("Should read zero from uninitialized memory")
    public void shouldReadZeroFromUninitialized() throws Exception {
      final WasmMemory memory = getMemory();

      // Offset 5000 should be uninitialized (zero)
      final byte result = memory.readByte(5000);
      assertEquals(0, result, "Uninitialized memory should read as zero");
    }

    @Test
    @DisplayName("Should throw for negative offset")
    public void shouldThrowForNegativeOffset() throws Exception {
      final WasmMemory memory = getMemory();
      assertThrows(
          IndexOutOfBoundsException.class,
          () -> memory.readByte(-1),
          "Negative offset should throw IndexOutOfBoundsException");
    }

    @Test
    @DisplayName("Should throw for out-of-bounds offset")
    public void shouldThrowForOutOfBoundsOffset() throws Exception {
      final WasmMemory memory = getMemory();
      // Memory is 1 page = 65536 bytes, offset >= 65536 is out of bounds
      assertThrows(
          IndexOutOfBoundsException.class,
          () -> memory.readByte(PAGE_SIZE),
          "Offset at page boundary should throw IndexOutOfBoundsException");
    }
  }

  @Nested
  @DisplayName("Write Byte Tests")
  class WriteByteTests {

    @Test
    @DisplayName("Should write byte without error")
    public void shouldWriteByteWithoutError() throws Exception {
      final WasmMemory memory = getMemory();
      // writeByte operates on a buffer copy, so it won't persist to native memory,
      // but it should execute without throwing
      assertDoesNotThrow(
          () -> memory.writeByte(0, (byte) 0xFF), "writeByte should not throw for valid offset");
    }

    @Test
    @DisplayName("Should throw for negative offset")
    public void shouldThrowForNegativeOffset() throws Exception {
      final WasmMemory memory = getMemory();
      assertThrows(
          IndexOutOfBoundsException.class,
          () -> memory.writeByte(-1, (byte) 0x42),
          "Negative offset should throw IndexOutOfBoundsException");
    }

    @Test
    @DisplayName("Should throw for out-of-bounds offset")
    public void shouldThrowForOutOfBoundsOffset() throws Exception {
      final WasmMemory memory = getMemory();
      assertThrows(
          IndexOutOfBoundsException.class,
          () -> memory.writeByte(PAGE_SIZE, (byte) 0x42),
          "Offset at page boundary should throw IndexOutOfBoundsException");
    }
  }

  @Nested
  @DisplayName("Copy Tests")
  class CopyTests {

    @Test
    @DisplayName("Should copy non-overlapping regions without error")
    public void shouldCopyNonOverlapping() throws Exception {
      final WasmMemory memory = getMemory();

      // Write source data at offset 0
      final byte[] srcData = {0x01, 0x02, 0x03, 0x04, 0x05};
      memory.writeBytes(0, srcData, 0, srcData.length);

      // Copy from [0..5) to [100..105) - operates on buffer copy
      assertDoesNotThrow(
          () -> memory.copy(100, 0, 5), "copy() should not throw for valid non-overlapping copy");
    }

    @Test
    @DisplayName("Should copy overlapping regions without error")
    public void shouldCopyOverlapping() throws Exception {
      final WasmMemory memory = getMemory();

      final byte[] data = {0x01, 0x02, 0x03, 0x04, 0x05};
      memory.writeBytes(0, data, 0, data.length);

      // Overlapping copy: src [0..5) -> dest [2..7)
      assertDoesNotThrow(() -> memory.copy(2, 0, 5), "copy() should handle overlapping regions");
    }

    @Test
    @DisplayName("Should handle zero-length copy")
    public void shouldCopyZeroLength() throws Exception {
      final WasmMemory memory = getMemory();
      assertDoesNotThrow(() -> memory.copy(0, 0, 0), "Zero-length copy should not throw");
    }

    @Test
    @DisplayName("Should throw for negative destination offset")
    public void shouldThrowForNegativeDestOffset() throws Exception {
      final WasmMemory memory = getMemory();
      assertThrows(
          IndexOutOfBoundsException.class,
          () -> memory.copy(-1, 0, 5),
          "Negative destination offset should throw");
    }

    @Test
    @DisplayName("Should throw for negative source offset")
    public void shouldThrowForNegativeSrcOffset() throws Exception {
      final WasmMemory memory = getMemory();
      assertThrows(
          IndexOutOfBoundsException.class,
          () -> memory.copy(0, -1, 5),
          "Negative source offset should throw");
    }

    @Test
    @DisplayName("Should throw for negative length")
    public void shouldThrowForNegativeLength() throws Exception {
      final WasmMemory memory = getMemory();
      assertThrows(
          IndexOutOfBoundsException.class,
          () -> memory.copy(0, 0, -1),
          "Negative length should throw");
    }

    @Test
    @DisplayName("Should throw for out-of-bounds source")
    public void shouldThrowForOutOfBoundsSrc() throws Exception {
      final WasmMemory memory = getMemory();
      assertThrows(
          IndexOutOfBoundsException.class,
          () -> memory.copy(0, PAGE_SIZE - 2, 5),
          "Source past memory bounds should throw");
    }

    @Test
    @DisplayName("Should throw for out-of-bounds destination")
    public void shouldThrowForOutOfBoundsDest() throws Exception {
      final WasmMemory memory = getMemory();
      assertThrows(
          IndexOutOfBoundsException.class,
          () -> memory.copy(PAGE_SIZE - 2, 0, 5),
          "Destination past memory bounds should throw");
    }
  }

  @Nested
  @DisplayName("Fill Tests")
  class FillTests {

    @Test
    @DisplayName("Should fill without error")
    public void shouldFillWithoutError() throws Exception {
      final WasmMemory memory = getMemory();
      // fill operates on a buffer copy, but should execute without error
      assertDoesNotThrow(
          () -> memory.fill(0, (byte) 0xAB, 100), "fill() should not throw for valid parameters");
    }

    @Test
    @DisplayName("Should handle zero-length fill")
    public void shouldFillZeroLength() throws Exception {
      final WasmMemory memory = getMemory();
      assertDoesNotThrow(() -> memory.fill(0, (byte) 0xFF, 0), "Zero-length fill should not throw");
    }

    @Test
    @DisplayName("Should throw for negative offset")
    public void shouldThrowForNegativeOffset() throws Exception {
      final WasmMemory memory = getMemory();
      assertThrows(
          IndexOutOfBoundsException.class,
          () -> memory.fill(-1, (byte) 0xFF, 10),
          "Negative offset should throw");
    }

    @Test
    @DisplayName("Should throw for negative length")
    public void shouldThrowForNegativeLength() throws Exception {
      final WasmMemory memory = getMemory();
      assertThrows(
          IndexOutOfBoundsException.class,
          () -> memory.fill(0, (byte) 0xFF, -1),
          "Negative length should throw");
    }

    @Test
    @DisplayName("Should throw for out-of-bounds range")
    public void shouldThrowForOutOfBounds() throws Exception {
      final WasmMemory memory = getMemory();
      assertThrows(
          IndexOutOfBoundsException.class,
          () -> memory.fill(PAGE_SIZE - 5, (byte) 0xFF, 10),
          "Fill past memory bounds should throw");
    }
  }

  @Nested
  @DisplayName("Shared Memory Tests")
  class SharedMemoryTests {

    @Test
    @DisplayName("Regular memory should not be shared")
    public void shouldNotBeSharedForRegularMemory() throws Exception {
      final WasmMemory memory = getMemory();
      final boolean shared = memory.isShared();
      LOGGER.info("isShared: " + shared);
      assertFalse(shared, "Regular (non-shared) memory should return false from isShared()");
    }
  }

  @Nested
  @DisplayName("64-Bit Addressing Tests")
  class Addressing64BitTests {

    @Test
    @DisplayName("Regular memory should not support 64-bit addressing")
    public void shouldNotSupport64BitForRegularMemory() throws Exception {
      final WasmMemory memory = getMemory();
      final boolean supports64 = memory.supports64BitAddressing();
      LOGGER.info("supports64BitAddressing: " + supports64);
      assertFalse(supports64, "Regular memory should not support 64-bit addressing");
    }
  }

  @Nested
  @DisplayName("Close Tests")
  class CloseTests {

    @Test
    @DisplayName("Should close without error")
    public void shouldCloseWithoutError() throws Exception {
      final Optional<WasmMemory> memOpt = instance.getMemory("memory");
      assertTrue(memOpt.isPresent());
      final PanamaMemory memory = (PanamaMemory) memOpt.get();

      assertDoesNotThrow(memory::close, "close() should not throw");
    }

    @Test
    @DisplayName("Should throw IllegalStateException on operations after close")
    public void shouldThrowAfterClose() throws Exception {
      final Optional<WasmMemory> memOpt = instance.getMemory("memory");
      assertTrue(memOpt.isPresent());
      final PanamaMemory memory = (PanamaMemory) memOpt.get();

      memory.close();

      assertThrows(
          IllegalStateException.class, memory::getSize, "getSize() after close should throw");
      assertThrows(
          IllegalStateException.class, () -> memory.grow(1), "grow() after close should throw");
      assertThrows(
          IllegalStateException.class, memory::getMaxSize, "getMaxSize() after close should throw");
      assertThrows(
          IllegalStateException.class,
          memory::getMemoryType,
          "getMemoryType() after close should throw");
      assertThrows(
          IllegalStateException.class, memory::getBuffer, "getBuffer() after close should throw");
      assertThrows(
          IllegalStateException.class,
          () -> memory.readByte(0),
          "readByte() after close should throw");
      assertThrows(
          IllegalStateException.class,
          () -> memory.writeByte(0, (byte) 0),
          "writeByte() after close should throw");
      assertThrows(
          IllegalStateException.class,
          () -> memory.readBytes(0, new byte[1], 0, 1),
          "readBytes() after close should throw");
      assertThrows(
          IllegalStateException.class,
          () -> memory.writeBytes(0, new byte[1], 0, 1),
          "writeBytes() after close should throw");
      assertThrows(
          IllegalStateException.class,
          () -> memory.copy(0, 0, 1),
          "copy() after close should throw");
      assertThrows(
          IllegalStateException.class,
          () -> memory.fill(0, (byte) 0, 1),
          "fill() after close should throw");
      assertThrows(
          IllegalStateException.class, memory::isShared, "isShared() after close should throw");
    }

    @Test
    @DisplayName("Should allow double close without error")
    public void shouldAllowDoubleClose() throws Exception {
      final Optional<WasmMemory> memOpt = instance.getMemory("memory");
      assertTrue(memOpt.isPresent());
      final PanamaMemory memory = (PanamaMemory) memOpt.get();

      memory.close();
      assertDoesNotThrow(memory::close, "Second close() should not throw");
    }
  }

  @Nested
  @DisplayName("Atomic Operations Validation Tests")
  class AtomicValidationTests {

    @Test
    @DisplayName("Should reject negative offset for atomicCompareAndSwapInt")
    public void shouldRejectNegativeOffsetForCasInt() throws Exception {
      final WasmMemory memory = getMemory();
      assertThrows(
          IllegalArgumentException.class,
          () -> memory.atomicCompareAndSwapInt(-1, 0, 0),
          "Negative offset should throw IllegalArgumentException");
    }

    @Test
    @DisplayName("Should reject misaligned offset for atomicCompareAndSwapInt")
    public void shouldRejectMisalignedOffsetForCasInt() throws Exception {
      final WasmMemory memory = getMemory();
      assertThrows(
          IllegalArgumentException.class,
          () -> memory.atomicCompareAndSwapInt(1, 0, 0),
          "Misaligned offset should throw IllegalArgumentException");
    }

    @Test
    @DisplayName("Should reject misaligned offset for atomicCompareAndSwapLong")
    public void shouldRejectMisalignedOffsetForCasLong() throws Exception {
      final WasmMemory memory = getMemory();
      assertThrows(
          IllegalArgumentException.class,
          () -> memory.atomicCompareAndSwapLong(3, 0L, 0L),
          "Misaligned offset should throw IllegalArgumentException");
    }

    @Test
    @DisplayName("Should reject misaligned offset for atomicLoadInt")
    public void shouldRejectMisalignedOffsetForLoadInt() throws Exception {
      final WasmMemory memory = getMemory();
      assertThrows(
          IllegalArgumentException.class,
          () -> memory.atomicLoadInt(2),
          "Misaligned offset should throw IllegalArgumentException");
    }

    @Test
    @DisplayName("Should reject misaligned offset for atomicLoadLong")
    public void shouldRejectMisalignedOffsetForLoadLong() throws Exception {
      final WasmMemory memory = getMemory();
      assertThrows(
          IllegalArgumentException.class,
          () -> memory.atomicLoadLong(4),
          "4-byte aligned but not 8-byte aligned should throw for Long");
    }

    @Test
    @DisplayName("Should reject misaligned offset for atomicStoreInt")
    public void shouldRejectMisalignedOffsetForStoreInt() throws Exception {
      final WasmMemory memory = getMemory();
      assertThrows(
          IllegalArgumentException.class,
          () -> memory.atomicStoreInt(3, 0),
          "Misaligned offset should throw IllegalArgumentException");
    }

    @Test
    @DisplayName("Should reject misaligned offset for atomicStoreLong")
    public void shouldRejectMisalignedOffsetForStoreLong() throws Exception {
      final WasmMemory memory = getMemory();
      assertThrows(
          IllegalArgumentException.class,
          () -> memory.atomicStoreLong(5, 0L),
          "Misaligned offset should throw IllegalArgumentException");
    }

    @Test
    @DisplayName("Should reject misaligned offset for atomicAddInt")
    public void shouldRejectMisalignedOffsetForAddInt() throws Exception {
      final WasmMemory memory = getMemory();
      assertThrows(
          IllegalArgumentException.class,
          () -> memory.atomicAddInt(1, 0),
          "Misaligned offset should throw IllegalArgumentException");
    }

    @Test
    @DisplayName("Should reject misaligned offset for atomicAddLong")
    public void shouldRejectMisalignedOffsetForAddLong() throws Exception {
      final WasmMemory memory = getMemory();
      assertThrows(
          IllegalArgumentException.class,
          () -> memory.atomicAddLong(3, 0L),
          "Misaligned offset should throw IllegalArgumentException");
    }

    @Test
    @DisplayName("Should reject misaligned offset for atomicAndInt")
    public void shouldRejectMisalignedOffsetForAndInt() throws Exception {
      final WasmMemory memory = getMemory();
      assertThrows(
          IllegalArgumentException.class,
          () -> memory.atomicAndInt(1, 0),
          "Misaligned offset should throw IllegalArgumentException");
    }

    @Test
    @DisplayName("Should reject misaligned offset for atomicOrInt")
    public void shouldRejectMisalignedOffsetForOrInt() throws Exception {
      final WasmMemory memory = getMemory();
      assertThrows(
          IllegalArgumentException.class,
          () -> memory.atomicOrInt(1, 0),
          "Misaligned offset should throw IllegalArgumentException");
    }

    @Test
    @DisplayName("Should reject misaligned offset for atomicXorInt")
    public void shouldRejectMisalignedOffsetForXorInt() throws Exception {
      final WasmMemory memory = getMemory();
      assertThrows(
          IllegalArgumentException.class,
          () -> memory.atomicXorInt(1, 0),
          "Misaligned offset should throw IllegalArgumentException");
    }

    @Test
    @DisplayName("Should reject misaligned offset for atomicNotify")
    public void shouldRejectMisalignedOffsetForNotify() throws Exception {
      final WasmMemory memory = getMemory();
      assertThrows(
          IllegalArgumentException.class,
          () -> memory.atomicNotify(1, 1),
          "Misaligned offset should throw IllegalArgumentException");
    }

    @Test
    @DisplayName("Should reject negative count for atomicNotify")
    public void shouldRejectNegativeNotifyCount() throws Exception {
      final WasmMemory memory = getMemory();
      assertThrows(
          IllegalArgumentException.class,
          () -> memory.atomicNotify(0, -1),
          "Negative count should throw IllegalArgumentException");
    }

    @Test
    @DisplayName("Should reject misaligned offset for atomicWait32")
    public void shouldRejectMisalignedOffsetForWait32() throws Exception {
      final WasmMemory memory = getMemory();
      assertThrows(
          IllegalArgumentException.class,
          () -> memory.atomicWait32(1, 0, 1000),
          "Misaligned offset should throw IllegalArgumentException");
    }

    @Test
    @DisplayName("Should reject invalid timeout for atomicWait32")
    public void shouldRejectInvalidWait32Timeout() throws Exception {
      final WasmMemory memory = getMemory();
      assertThrows(
          IllegalArgumentException.class,
          () -> memory.atomicWait32(0, 0, -2),
          "Timeout of -2 should throw (only -1 is valid for infinite)");
    }

    @Test
    @DisplayName("Should reject misaligned offset for atomicWait64")
    public void shouldRejectMisalignedOffsetForWait64() throws Exception {
      final WasmMemory memory = getMemory();
      assertThrows(
          IllegalArgumentException.class,
          () -> memory.atomicWait64(3, 0L, 1000),
          "Misaligned offset should throw IllegalArgumentException");
    }

    @Test
    @DisplayName("Should reject invalid timeout for atomicWait64")
    public void shouldRejectInvalidWait64Timeout() throws Exception {
      final WasmMemory memory = getMemory();
      assertThrows(
          IllegalArgumentException.class,
          () -> memory.atomicWait64(0, 0L, -2),
          "Timeout of -2 should throw (only -1 is valid for infinite)");
    }
  }

  @Nested
  @DisplayName("Atomic Operations on Non-Shared Memory Tests")
  class AtomicOperationsTests {

    @Test
    @DisplayName("atomicCompareAndSwapInt should throw UnsupportedOperationException")
    public void shouldThrowUnsupportedForCasInt() throws Exception {
      final WasmMemory memory = getMemory();
      assertThrows(
          UnsupportedOperationException.class,
          () -> memory.atomicCompareAndSwapInt(0, 0, 42),
          "Atomic CAS on non-shared memory should throw UnsupportedOperationException");
    }

    @Test
    @DisplayName("atomicCompareAndSwapLong should throw UnsupportedOperationException")
    public void shouldThrowUnsupportedForCasLong() throws Exception {
      final WasmMemory memory = getMemory();
      assertThrows(
          UnsupportedOperationException.class,
          () -> memory.atomicCompareAndSwapLong(0, 0L, 42L),
          "Atomic CAS Long on non-shared memory should throw");
    }

    @Test
    @DisplayName("atomicLoadInt should throw UnsupportedOperationException")
    public void shouldThrowUnsupportedForLoadInt() throws Exception {
      final WasmMemory memory = getMemory();
      assertThrows(
          UnsupportedOperationException.class,
          () -> memory.atomicLoadInt(0),
          "Atomic load int on non-shared memory should throw");
    }

    @Test
    @DisplayName("atomicLoadLong should throw UnsupportedOperationException")
    public void shouldThrowUnsupportedForLoadLong() throws Exception {
      final WasmMemory memory = getMemory();
      assertThrows(
          UnsupportedOperationException.class,
          () -> memory.atomicLoadLong(0),
          "Atomic load long on non-shared memory should throw");
    }

    @Test
    @DisplayName("atomicStoreInt should throw UnsupportedOperationException")
    public void shouldThrowUnsupportedForStoreInt() throws Exception {
      final WasmMemory memory = getMemory();
      assertThrows(
          UnsupportedOperationException.class,
          () -> memory.atomicStoreInt(0, 42),
          "Atomic store int on non-shared memory should throw");
    }

    @Test
    @DisplayName("atomicStoreLong should throw UnsupportedOperationException")
    public void shouldThrowUnsupportedForStoreLong() throws Exception {
      final WasmMemory memory = getMemory();
      assertThrows(
          UnsupportedOperationException.class,
          () -> memory.atomicStoreLong(0, 42L),
          "Atomic store long on non-shared memory should throw");
    }

    @Test
    @DisplayName("atomicAddInt should throw UnsupportedOperationException")
    public void shouldThrowUnsupportedForAddInt() throws Exception {
      final WasmMemory memory = getMemory();
      assertThrows(
          UnsupportedOperationException.class,
          () -> memory.atomicAddInt(0, 1),
          "Atomic add int on non-shared memory should throw");
    }

    @Test
    @DisplayName("atomicAddLong should throw UnsupportedOperationException")
    public void shouldThrowUnsupportedForAddLong() throws Exception {
      final WasmMemory memory = getMemory();
      assertThrows(
          UnsupportedOperationException.class,
          () -> memory.atomicAddLong(0, 1L),
          "Atomic add long on non-shared memory should throw");
    }

    @Test
    @DisplayName("atomicAndInt should throw UnsupportedOperationException")
    public void shouldThrowUnsupportedForAndInt() throws Exception {
      final WasmMemory memory = getMemory();
      assertThrows(
          UnsupportedOperationException.class,
          () -> memory.atomicAndInt(0, 0xFF),
          "Atomic AND int on non-shared memory should throw");
    }

    @Test
    @DisplayName("atomicOrInt should throw UnsupportedOperationException")
    public void shouldThrowUnsupportedForOrInt() throws Exception {
      final WasmMemory memory = getMemory();
      assertThrows(
          UnsupportedOperationException.class,
          () -> memory.atomicOrInt(0, 0xFF),
          "Atomic OR int on non-shared memory should throw");
    }

    @Test
    @DisplayName("atomicXorInt should throw UnsupportedOperationException")
    public void shouldThrowUnsupportedForXorInt() throws Exception {
      final WasmMemory memory = getMemory();
      assertThrows(
          UnsupportedOperationException.class,
          () -> memory.atomicXorInt(0, 0xFF),
          "Atomic XOR int on non-shared memory should throw");
    }

    @Test
    @DisplayName("atomicFence should throw UnsupportedOperationException")
    public void shouldThrowUnsupportedForFence() throws Exception {
      final WasmMemory memory = getMemory();
      assertThrows(
          UnsupportedOperationException.class,
          memory::atomicFence,
          "Atomic fence on non-shared memory should throw");
    }

    @Test
    @DisplayName("atomicNotify should throw UnsupportedOperationException")
    public void shouldThrowUnsupportedForNotify() throws Exception {
      final WasmMemory memory = getMemory();
      assertThrows(
          UnsupportedOperationException.class,
          () -> memory.atomicNotify(0, 1),
          "Atomic notify on non-shared memory should throw");
    }

    @Test
    @DisplayName("atomicWait32 should throw UnsupportedOperationException")
    public void shouldThrowUnsupportedForWait32() throws Exception {
      final WasmMemory memory = getMemory();
      assertThrows(
          UnsupportedOperationException.class,
          () -> memory.atomicWait32(0, 0, 1000),
          "Atomic wait32 on non-shared memory should throw");
    }

    @Test
    @DisplayName("atomicWait64 should throw UnsupportedOperationException")
    public void shouldThrowUnsupportedForWait64() throws Exception {
      final WasmMemory memory = getMemory();
      assertThrows(
          UnsupportedOperationException.class,
          () -> memory.atomicWait64(0, 0L, 1000),
          "Atomic wait64 on non-shared memory should throw");
    }
  }

  @Nested
  @DisplayName("64-Bit Operations Tests")
  class Operations64BitTests {

    @Test
    @DisplayName("getSize64 should return page count")
    public void shouldGetSize64() throws Exception {
      final WasmMemory memory = getMemory();
      final long size64 = memory.getSize64();
      LOGGER.info("getSize64: " + size64);
      assertEquals(1L, size64, "getSize64() should return 1 for single-page memory");
    }

    @Test
    @DisplayName("grow64 should return previous page count")
    public void shouldGrow64() throws Exception {
      final WasmMemory memory = getMemory();
      final long previousPages = memory.grow64(1L);
      LOGGER.info("grow64(1) returned previous pages: " + previousPages);
      assertEquals(1L, previousPages, "grow64(1) should return 1 (previous page count)");
      assertEquals(2L, memory.getSize64(), "Size should be 2 after growing by 1");
    }

    @Test
    @DisplayName("grow64 should reject negative pages")
    public void shouldGrow64RejectNegative() throws Exception {
      final WasmMemory memory = getMemory();
      assertThrows(
          IllegalArgumentException.class,
          () -> memory.grow64(-1L),
          "grow64(-1) should throw IllegalArgumentException");
    }

    @Test
    @DisplayName("Should write and read bytes using 64-bit operations")
    public void shouldReadWriteBytes64() throws Exception {
      final WasmMemory memory = getMemory();

      final byte[] data = {0x10, 0x20, 0x30, 0x40, 0x50};
      memory.writeBytes64(0L, data, 0, data.length);

      final byte[] readBack = new byte[data.length];
      memory.readBytes64(0L, readBack, 0, data.length);

      assertArrayEquals(data, readBack, "64-bit read should match 64-bit write");
    }

    @Test
    @DisplayName("Should write and read bytes at non-zero 64-bit offset")
    public void shouldReadWriteBytes64AtOffset() throws Exception {
      final WasmMemory memory = getMemory();

      final byte[] data = {0x0A, 0x0B, 0x0C};
      memory.writeBytes64(500L, data, 0, data.length);

      final byte[] readBack = new byte[data.length];
      memory.readBytes64(500L, readBack, 0, data.length);

      assertArrayEquals(data, readBack, "64-bit read at offset 500 should match write");
    }

    @Test
    @DisplayName("Should handle zero-length 64-bit read")
    public void shouldReadBytes64ZeroLength() throws Exception {
      final WasmMemory memory = getMemory();
      final byte[] empty = new byte[0];
      assertDoesNotThrow(
          () -> memory.readBytes64(0L, empty, 0, 0), "Zero-length 64-bit read should not throw");
    }

    @Test
    @DisplayName("Should handle zero-length 64-bit write")
    public void shouldWriteBytes64ZeroLength() throws Exception {
      final WasmMemory memory = getMemory();
      final byte[] empty = new byte[0];
      assertDoesNotThrow(
          () -> memory.writeBytes64(0L, empty, 0, 0), "Zero-length 64-bit write should not throw");
    }

    @Test
    @DisplayName("readBytes64 should reject null dest")
    public void shouldReadBytes64RejectNull() throws Exception {
      final WasmMemory memory = getMemory();
      assertThrows(
          IllegalArgumentException.class,
          () -> memory.readBytes64(0L, null, 0, 10),
          "Null dest should throw IllegalArgumentException");
    }

    @Test
    @DisplayName("writeBytes64 should reject null src")
    public void shouldWriteBytes64RejectNull() throws Exception {
      final WasmMemory memory = getMemory();
      assertThrows(
          IllegalArgumentException.class,
          () -> memory.writeBytes64(0L, null, 0, 10),
          "Null src should throw IllegalArgumentException");
    }

    @Test
    @DisplayName("readBytes64 should reject negative offset")
    public void shouldRejectNegativeOffset64Read() throws Exception {
      final WasmMemory memory = getMemory();
      assertThrows(
          IndexOutOfBoundsException.class,
          () -> memory.readBytes64(-1L, new byte[10], 0, 10),
          "Negative offset should throw IndexOutOfBoundsException");
    }

    @Test
    @DisplayName("writeBytes64 should reject negative offset")
    public void shouldRejectNegativeOffset64Write() throws Exception {
      final WasmMemory memory = getMemory();
      assertThrows(
          IndexOutOfBoundsException.class,
          () -> memory.writeBytes64(-1L, new byte[10], 0, 10),
          "Negative offset should throw IndexOutOfBoundsException");
    }

    @Test
    @DisplayName("readBytes64 should reject negative dest offset")
    public void shouldRejectNegativeDestOffset64Read() throws Exception {
      final WasmMemory memory = getMemory();
      assertThrows(
          IndexOutOfBoundsException.class,
          () -> memory.readBytes64(0L, new byte[10], -1, 5),
          "Negative dest offset should throw IndexOutOfBoundsException");
    }

    @Test
    @DisplayName("writeBytes64 should reject negative src offset")
    public void shouldRejectNegativeSrcOffset64Write() throws Exception {
      final WasmMemory memory = getMemory();
      assertThrows(
          IndexOutOfBoundsException.class,
          () -> memory.writeBytes64(0L, new byte[10], -1, 5),
          "Negative src offset should throw IndexOutOfBoundsException");
    }

    @Test
    @DisplayName("readBytes64 should reject negative length")
    public void shouldRejectNegativeLength64Read() throws Exception {
      final WasmMemory memory = getMemory();
      assertThrows(
          IndexOutOfBoundsException.class,
          () -> memory.readBytes64(0L, new byte[10], 0, -1),
          "Negative length should throw IndexOutOfBoundsException");
    }

    @Test
    @DisplayName("writeBytes64 should reject negative length")
    public void shouldRejectNegativeLength64Write() throws Exception {
      final WasmMemory memory = getMemory();
      assertThrows(
          IndexOutOfBoundsException.class,
          () -> memory.writeBytes64(0L, new byte[10], 0, -1),
          "Negative length should throw IndexOutOfBoundsException");
    }

    @Test
    @DisplayName("readBytes64 should reject array bounds exceeded")
    public void shouldRejectArrayBoundsExceeded64Read() throws Exception {
      final WasmMemory memory = getMemory();
      assertThrows(
          IndexOutOfBoundsException.class,
          () -> memory.readBytes64(0L, new byte[5], 3, 5),
          "Array bounds exceeded should throw IndexOutOfBoundsException");
    }

    @Test
    @DisplayName("writeBytes64 should reject array bounds exceeded")
    public void shouldRejectArrayBoundsExceeded64Write() throws Exception {
      final WasmMemory memory = getMemory();
      assertThrows(
          IndexOutOfBoundsException.class,
          () -> memory.writeBytes64(0L, new byte[5], 3, 5),
          "Array bounds exceeded should throw IndexOutOfBoundsException");
    }
  }

  @Nested
  @DisplayName("Init and Drop Data Segment Validation Tests")
  class InitDropDataSegmentTests {

    @Test
    @DisplayName("init should throw for negative destination offset")
    public void shouldThrowForNegativeDestOffset() throws Exception {
      final WasmMemory memory = getMemory();
      assertThrows(
          IndexOutOfBoundsException.class,
          () -> memory.init(-1, 0, 0, 10),
          "Negative destination offset should throw");
    }

    @Test
    @DisplayName("init should throw for negative data segment index")
    public void shouldThrowForNegativeDataSegmentIndex() throws Exception {
      final WasmMemory memory = getMemory();
      assertThrows(
          IllegalArgumentException.class,
          () -> memory.init(0, -1, 0, 10),
          "Negative data segment index should throw");
    }

    @Test
    @DisplayName("init should throw for negative source offset")
    public void shouldThrowForNegativeSrcOffset() throws Exception {
      final WasmMemory memory = getMemory();
      assertThrows(
          IndexOutOfBoundsException.class,
          () -> memory.init(0, 0, -1, 10),
          "Negative source offset should throw");
    }

    @Test
    @DisplayName("init should throw for negative length")
    public void shouldThrowForNegativeLength() throws Exception {
      final WasmMemory memory = getMemory();
      assertThrows(
          IndexOutOfBoundsException.class,
          () -> memory.init(0, 0, 0, -1),
          "Negative length should throw");
    }

    @Test
    @DisplayName("init should handle zero length without error")
    public void shouldHandleZeroLengthInit() throws Exception {
      final WasmMemory memory = getMemory();
      // Zero length should return immediately without native call
      assertDoesNotThrow(() -> memory.init(0, 0, 0, 0), "Zero-length init should not throw");
    }

    @Test
    @DisplayName("dropDataSegment should throw for negative index")
    public void shouldThrowForNegativeDropIndex() throws Exception {
      final WasmMemory memory = getMemory();
      assertThrows(
          IllegalArgumentException.class,
          () -> memory.dropDataSegment(-1),
          "Negative data segment index should throw");
    }
  }

  @Nested
  @DisplayName("Buffer Caching Tests")
  class BufferCachingTests {

    @Test
    @DisplayName("getBuffer should return consistent content across calls")
    public void shouldReturnConsistentBufferContent() throws Exception {
      final WasmMemory memory = getMemory();

      final byte[] data = {0x01, 0x02, 0x03};
      memory.writeBytes(0, data, 0, data.length);

      final ByteBuffer buf1 = memory.getBuffer();
      final ByteBuffer buf2 = memory.getBuffer();

      assertNotNull(buf1);
      assertNotNull(buf2);
      assertEquals(buf1.get(0), buf2.get(0), "Buffers should have same content at offset 0");
      assertEquals(buf1.get(1), buf2.get(1), "Buffers should have same content at offset 1");
      assertEquals(buf1.get(2), buf2.get(2), "Buffers should have same content at offset 2");
    }

    @Test
    @DisplayName("getBuffer should reflect new size after grow")
    public void shouldRefreshBufferAfterGrow() throws Exception {
      final WasmMemory memory = getMemory();

      final ByteBuffer buf1 = memory.getBuffer();
      assertEquals(PAGE_SIZE, buf1.remaining(), "Initial buffer should be 1 page");

      memory.grow(2);

      // After grow, buffer should reflect new size (3 pages)
      final ByteBuffer buf2 = memory.getBuffer();
      assertEquals(PAGE_SIZE * 3, buf2.remaining(), "Buffer after grow should be 3 pages");
    }

    @Test
    @DisplayName("Written data should be visible in buffer snapshot")
    public void shouldShowWrittenDataInBuffer() throws Exception {
      final WasmMemory memory = getMemory();

      // Write specific pattern
      final byte[] pattern = {(byte) 0xDE, (byte) 0xAD, (byte) 0xBE, (byte) 0xEF};
      memory.writeBytes(256, pattern, 0, pattern.length);

      // Verify via getBuffer
      final ByteBuffer buffer = memory.getBuffer();
      assertEquals((byte) 0xDE, buffer.get(256));
      assertEquals((byte) 0xAD, buffer.get(257));
      assertEquals((byte) 0xBE, buffer.get(258));
      assertEquals((byte) 0xEF, buffer.get(259));
    }
  }

  @Nested
  @DisplayName("Internal Accessor Tests")
  class InternalAccessorTests {

    @Test
    @DisplayName("Should return memory name for instance-exported memory")
    public void shouldReturnMemoryName() throws Exception {
      final Optional<WasmMemory> memOpt = instance.getMemory("memory");
      assertTrue(memOpt.isPresent());
      final PanamaMemory pMem = (PanamaMemory) memOpt.get();

      final String name = pMem.getMemoryName();
      assertEquals("memory", name, "Memory name should be 'memory'");
    }

    @Test
    @DisplayName("Should return source instance for instance-exported memory")
    public void shouldReturnSourceInstance() throws Exception {
      final Optional<WasmMemory> memOpt = instance.getMemory("memory");
      assertTrue(memOpt.isPresent());
      final PanamaMemory pMem = (PanamaMemory) memOpt.get();

      assertNotNull(pMem.getSourceInstance(), "Source instance should not be null");
      assertSame(instance, pMem.getSourceInstance(), "Source instance should be the test instance");
    }

    @Test
    @DisplayName("Should return non-null native memory")
    public void shouldReturnNativeMemory() throws Exception {
      final Optional<WasmMemory> memOpt = instance.getMemory("memory");
      assertTrue(memOpt.isPresent());
      final PanamaMemory pMem = (PanamaMemory) memOpt.get();

      assertNotNull(pMem.getNativeMemory(), "getNativeMemory() should not return null");
    }

    @Test
    @DisplayName("Should be instance-exported")
    public void shouldBeInstanceExported() throws Exception {
      final Optional<WasmMemory> memOpt = instance.getMemory("memory");
      assertTrue(memOpt.isPresent());
      final PanamaMemory pMem = (PanamaMemory) memOpt.get();

      assertTrue(pMem.isInstanceExported(), "Memory from instance should be instance-exported");
    }

    @Test
    @DisplayName("Should return owning instance")
    public void shouldReturnOwningInstance() throws Exception {
      final Optional<WasmMemory> memOpt = instance.getMemory("memory");
      assertTrue(memOpt.isPresent());
      final PanamaMemory pMem = (PanamaMemory) memOpt.get();

      assertNotNull(pMem.getOwningInstance(), "Owning instance should not be null");
      assertSame(instance, pMem.getOwningInstance(), "Owning instance should match test instance");
    }

    @Test
    @DisplayName("Should return export name")
    public void shouldReturnExportName() throws Exception {
      final Optional<WasmMemory> memOpt = instance.getMemory("memory");
      assertTrue(memOpt.isPresent());
      final PanamaMemory pMem = (PanamaMemory) memOpt.get();

      assertEquals("memory", pMem.getExportName(), "Export name should be 'memory'");
    }
  }

  @Nested
  @DisplayName("Constructor Validation Tests")
  class ConstructorValidationTests {

    @Test
    @DisplayName("Should throw for null memory name")
    public void shouldThrowForNullMemoryName() throws Exception {
      assertThrows(
          IllegalArgumentException.class,
          () -> new PanamaMemory(null, instance),
          "Null memory name should throw IllegalArgumentException");
    }

    @Test
    @DisplayName("Should throw for empty memory name")
    public void shouldThrowForEmptyMemoryName() throws Exception {
      assertThrows(
          IllegalArgumentException.class,
          () -> new PanamaMemory("", instance),
          "Empty memory name should throw IllegalArgumentException");
    }

    @Test
    @DisplayName("Should throw for null instance in name constructor")
    public void shouldThrowForNullInstance() throws Exception {
      assertThrows(
          IllegalArgumentException.class,
          () -> new PanamaMemory("test", null),
          "Null instance should throw IllegalArgumentException");
    }
  }

  @Nested
  @DisplayName("Memory Out-of-Bounds Access Tests")
  class OutOfBoundsTests {

    @Test
    @DisplayName("Write beyond memory should throw")
    public void shouldThrowForWriteBeyondMemory() throws Exception {
      final WasmMemory memory = getMemory();
      final byte[] data = new byte[10];

      // Attempting to write at offset just past the memory end
      assertThrows(
          IndexOutOfBoundsException.class,
          () -> memory.writeBytes(PAGE_SIZE, data, 0, data.length),
          "Writing beyond memory end should throw");
    }

    @Test
    @DisplayName("Read beyond memory should throw")
    public void shouldThrowForReadBeyondMemory() throws Exception {
      final WasmMemory memory = getMemory();
      final byte[] data = new byte[10];

      assertThrows(
          IndexOutOfBoundsException.class,
          () -> memory.readBytes(PAGE_SIZE, data, 0, data.length),
          "Reading beyond memory end should throw");
    }

    @Test
    @DisplayName("Write spanning memory boundary should throw")
    public void shouldThrowForWriteSpanningBoundary() throws Exception {
      final WasmMemory memory = getMemory();
      final byte[] data = new byte[10];

      // Start 5 bytes from end, try to write 10
      assertThrows(
          IndexOutOfBoundsException.class,
          () -> memory.writeBytes(PAGE_SIZE - 5, data, 0, data.length),
          "Write spanning memory boundary should throw");
    }

    @Test
    @DisplayName("Read spanning memory boundary should throw")
    public void shouldThrowForReadSpanningBoundary() throws Exception {
      final WasmMemory memory = getMemory();
      final byte[] data = new byte[10];

      assertThrows(
          IndexOutOfBoundsException.class,
          () -> memory.readBytes(PAGE_SIZE - 5, data, 0, data.length),
          "Read spanning memory boundary should throw");
    }
  }

  @Nested
  @DisplayName("Grow Boundary Tests")
  class GrowBoundaryTests {

    @Test
    @DisplayName("Grow beyond max should return -1")
    public void shouldReturnNegativeOneForGrowBeyondMax() throws Exception {
      final WasmMemory memory = getMemory();
      // Memory has max of 10 pages, currently 1. Trying to grow by 100 should fail.
      final int result = memory.grow(100);
      assertEquals(-1, result, "Growing beyond maximum should return -1");
      assertEquals(1, memory.getSize(), "Size should remain unchanged after failed grow");
    }

    @Test
    @DisplayName("Grow to exactly max should succeed")
    public void shouldGrowToExactMax() throws Exception {
      final WasmMemory memory = getMemory();
      // Memory has max of 10 pages, currently 1. Grow by 9 to reach exactly 10.
      final int result = memory.grow(9);
      assertEquals(1, result, "Previous size should be 1");
      assertEquals(10, memory.getSize(), "Size should be exactly 10 pages");
    }

    @Test
    @DisplayName("Data persists after grow")
    public void shouldPreserveDataAfterGrow() throws Exception {
      final WasmMemory memory = getMemory();

      // Write data before grow
      final byte[] data = {0x41, 0x42, 0x43, 0x44};
      memory.writeBytes(1000, data, 0, data.length);

      // Grow memory
      memory.grow(3);

      // Verify data persists
      final byte[] readBack = new byte[data.length];
      memory.readBytes(1000, readBack, 0, data.length);
      assertArrayEquals(data, readBack, "Data should persist after memory grow");
    }

    @Test
    @DisplayName("Can write to newly grown pages")
    public void shouldWriteToNewlyGrownPages() throws Exception {
      final WasmMemory memory = getMemory();
      memory.grow(2); // Now 3 pages

      // Write to page 2 (offset in [2*PAGE_SIZE, 3*PAGE_SIZE))
      final int offset = 2 * PAGE_SIZE + 100;
      final byte[] data = {0x0A, 0x0B, 0x0C};
      memory.writeBytes(offset, data, 0, data.length);

      final byte[] readBack = new byte[data.length];
      memory.readBytes(offset, readBack, 0, data.length);
      assertArrayEquals(data, readBack, "Data written to grown pages should be readable");
    }
  }
}
