package ai.tegmentum.wasmtime4j.jni;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.WasmValueType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Consolidated input validation tests for JNI implementation classes.
 *
 * <p>These tests exercise real pre-native-call validation code paths: null checks, zero/negative
 * handle rejection, negative value rejection, and type mismatch detection. All validation happens
 * in Java before any native call is made.
 *
 * <p>These tests use fake handles (which never reach native code) because the validation logic
 * under test runs entirely in Java. This is intentional — the goal is to verify that invalid inputs
 * are rejected before crossing the JNI boundary.
 */
@DisplayName("JNI Input Validation Tests")
class JniInputValidationTest {

  private static final long VALID_HANDLE = 0x12345678L;

  private JniEngine testEngine;
  private JniStore testStore;

  @BeforeEach
  void setUp() {
    testEngine = new JniEngine(VALID_HANDLE);
    testStore = new JniStore(VALID_HANDLE, testEngine);
  }

  @AfterEach
  void tearDown() {
    // Mark fake-handle resources as closed to prevent GC-triggered native cleanup
    testStore.markClosedForTesting();
    testEngine.markClosedForTesting();
  }

  @Nested
  @DisplayName("Handle Validation")
  class HandleValidation {

    @Test
    @DisplayName("Zero handle should be rejected as null pointer")
    void zeroHandleShouldBeRejected() {
      assertThatThrownBy(() -> new JniEngine(0L))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("nativeHandle")
          .hasMessageContaining("invalid native handle")
          .hasMessageContaining("null pointer");
    }

    @Test
    @DisplayName("Negative handle should be rejected as invalid")
    void negativeHandleShouldBeRejected() {
      assertThatThrownBy(() -> new JniEngine(-1L))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("nativeHandle")
          .hasMessageContaining("invalid native handle")
          .hasMessageContaining("negative value");
    }
  }

  @Nested
  @DisplayName("Constructor Null Parameter Rejection")
  class ConstructorNullParameterRejection {

    @Test
    @DisplayName("JniInstance should reject null module")
    void instanceShouldRejectNullModule() {
      assertThatThrownBy(() -> new JniInstance(VALID_HANDLE, null, testStore))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("module")
          .hasMessageContaining("must not be null");
    }

    @Test
    @DisplayName("JniInstance should reject null store")
    void instanceShouldRejectNullStore() {
      assertThatThrownBy(
              () -> new JniInstance(VALID_HANDLE, new JniModule(VALID_HANDLE, testEngine), null))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("store")
          .hasMessageContaining("must not be null");
    }

    @Test
    @DisplayName("JniTable should reject null store")
    void tableShouldRejectNullStore() {
      assertThatThrownBy(() -> new JniTable(VALID_HANDLE, null))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("store")
          .hasMessageContaining("must not be null");
    }

    @Test
    @DisplayName("JniGlobal should reject null store")
    void globalShouldRejectNullStore() {
      assertThatThrownBy(() -> new JniGlobal(VALID_HANDLE, null))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("store")
          .hasMessageContaining("must not be null");
    }

    @Test
    @DisplayName("JniCallbackRegistry should reject null store")
    void callbackRegistryShouldRejectNullStore() {
      assertThatThrownBy(() -> new JniCallbackRegistry(null))
          .isInstanceOf(NullPointerException.class)
          .hasMessageContaining("Store cannot be null");
    }
  }

  @Nested
  @DisplayName("Method Null Parameter Rejection")
  class MethodNullParameterRejection {

    @Test
    @DisplayName("compileModule should reject null bytes")
    void compileModuleShouldRejectNullBytes() {
      final JniEngine engine = new JniEngine(VALID_HANDLE);
      assertThatThrownBy(() -> engine.compileModule(null))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("wasmBytes")
          .hasMessageContaining("null");
    }

    @Test
    @DisplayName("compileModule should reject empty bytes")
    void compileModuleShouldRejectEmptyBytes() {
      final JniEngine engine = new JniEngine(VALID_HANDLE);
      assertThatThrownBy(() -> engine.compileModule(new byte[0]))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("wasmBytes")
          .hasMessageContaining("empty");
    }

    @Test
    @DisplayName("getFunction should reject null name")
    void getFunctionShouldRejectNullName() {
      final JniModule testModule = new JniModule(VALID_HANDLE, testEngine);
      final JniInstance instance = new JniInstance(VALID_HANDLE, testModule, testStore);
      assertThatThrownBy(() -> instance.getFunction(null))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("name")
          .hasMessageContaining("must not be null");
      instance.markClosedForTesting();
      testModule.markClosedForTesting();
    }

    @Test
    @DisplayName("getFunction should reject blank name")
    void getFunctionShouldRejectBlankName() {
      final JniModule testModule = new JniModule(VALID_HANDLE, testEngine);
      final JniInstance instance = new JniInstance(VALID_HANDLE, testModule, testStore);
      assertThatThrownBy(() -> instance.getFunction("   "))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("name")
          .hasMessageContaining("must not be empty or whitespace-only");
      instance.markClosedForTesting();
      testModule.markClosedForTesting();
    }

    @Test
    @DisplayName("readBytes should reject null buffer")
    void readBytesShouldRejectNullBuffer() {
      final JniMemory memory = new JniMemory(VALID_HANDLE, testStore);
      assertThatThrownBy(() -> memory.readBytes(0L, null))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("buffer")
          .hasMessageContaining("must not be null");
      memory.markClosedForTesting();
    }
  }

  @Nested
  @DisplayName("Negative Value Rejection")
  class NegativeValueRejection {

    @Test
    @DisplayName("addFuel should reject negative value")
    void addFuelShouldRejectNegativeValue() {
      assertThatThrownBy(() -> testStore.addFuel(-1))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("additionalFuel")
          .hasMessageContaining("non-negative");
    }

    @Test
    @DisplayName("memory grow should reject negative pages")
    void memoryGrowShouldRejectNegativePages() {
      final JniMemory memory = new JniMemory(VALID_HANDLE, testStore);
      assertThatThrownBy(() -> memory.grow(-1L))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("pages")
          .hasMessageContaining("non-negative");
      memory.markClosedForTesting();
    }

    @Test
    @DisplayName("table get should reject negative index")
    void tableGetShouldRejectNegativeIndex() {
      final JniTable table = new JniTable(VALID_HANDLE, testStore);
      assertThatThrownBy(() -> table.get(-1))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("index")
          .hasMessageContaining("non-negative");
      table.markClosedForTesting();
    }

    @Test
    @DisplayName("table fill should reject negative start")
    void tableFillShouldRejectNegativeStart() {
      final JniTable table = new JniTable(VALID_HANDLE, testStore);
      assertThatThrownBy(() -> table.fill(-1, 1, null))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("start")
          .hasMessageContaining("non-negative");
      table.markClosedForTesting();
    }

    @Test
    @DisplayName("table fill should reject negative count")
    void tableFillShouldRejectNegativeCount() {
      final JniTable table = new JniTable(VALID_HANDLE, testStore);
      assertThatThrownBy(() -> table.fill(0, -1, null))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("count")
          .hasMessageContaining("non-negative");
      table.markClosedForTesting();
    }

    @Test
    @DisplayName("createMemory should reject negative initial pages")
    void createMemoryShouldRejectNegativeInitialPages() {
      assertThatThrownBy(() -> testStore.createMemory(-1, 10))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Initial pages")
          .hasMessageContaining("negative");
    }
  }

  @Nested
  @DisplayName("Invalid Range and Type Rejection")
  class InvalidRangeAndTypeRejection {

    @Test
    @DisplayName("createMemory should reject max pages less than initial")
    void createMemoryShouldRejectMaxLessThanInitial() {
      assertThatThrownBy(() -> testStore.createMemory(10, 5))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Max pages")
          .hasMessageContaining("cannot be less than initial pages");
    }

    @Test
    @DisplayName("createTable should reject non-reference element type")
    void createTableShouldRejectInvalidElementType() {
      assertThatThrownBy(() -> testStore.createTable(WasmValueType.I32, 10, 20))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Element type")
          .hasMessageContaining("must be FUNCREF or EXTERNREF");
    }

    @Test
    @DisplayName("createGlobal should reject mismatched value type")
    void createGlobalShouldRejectMismatchedType() {
      assertThatThrownBy(() -> testStore.createGlobal(WasmValueType.I64, false, WasmValue.i32(42)))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Initial value type")
          .hasMessageContaining("does not match global type");
    }
  }
}
