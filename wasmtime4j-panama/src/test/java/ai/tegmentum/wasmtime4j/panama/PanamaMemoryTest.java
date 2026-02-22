package ai.tegmentum.wasmtime4j.panama;

import static org.junit.jupiter.api.Assertions.*;

import ai.tegmentum.wasmtime4j.WasmMemory;
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

/**
 * Test suite for Panama-specific PanamaMemory operations.
 *
 * <p>Generic memory API tests have been migrated to {@code MemoryApiDualRuntimeTest} in the
 * integration test module. This file retains only Panama-specific tests that require direct access
 * to {@link PanamaMemory} internals (close behavior, internal accessors, constructor validation).
 */
@DisplayName("Panama Memory Tests")
public class PanamaMemoryTest {

  private static final Logger LOGGER = Logger.getLogger(PanamaMemoryTest.class.getName());

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
}
