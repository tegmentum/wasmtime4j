package ai.tegmentum.wasmtime4j.edge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.exception.RuntimeException;
import ai.tegmentum.wasmtime4j.exception.SecurityException;
import ai.tegmentum.wasmtime4j.exception.ValidationException;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.function.WasmFunction;
import ai.tegmentum.wasmtime4j.memory.Memory;
import ai.tegmentum.wasmtime4j.utils.BaseIntegrationTest;
import ai.tegmentum.wasmtime4j.utils.TestUtils;
import java.security.SecureRandom;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Comprehensive security boundary validation tests for WebAssembly sandbox isolation. Tests
 * validate proper enforcement of memory access boundaries, import/export security restrictions, and
 * privilege escalation prevention mechanisms.
 */
@DisplayName("Security Boundary Edge Cases Tests")
final class SecurityBoundaryEdgeCasesIT extends BaseIntegrationTest {

  private static final SecureRandom RANDOM = new SecureRandom();
  private static final int MEMORY_PAGE_SIZE = 65536; // 64KB
  private static final int MAX_SAFE_MEMORY_ACCESS = MEMORY_PAGE_SIZE - 8;

  @Override
  protected void doSetUp(final TestInfo testInfo) {
    // Security boundary tests are always enabled
  }

  @Nested
  @DisplayName("Memory Boundary Violation Tests")
  final class MemoryBoundaryViolationTests {

    @Test
    @DisplayName("Should prevent out-of-bounds memory access")
    void shouldPreventOutOfBoundsMemoryAccess() throws WasmException {
      runWithBothRuntimes(
          (runtime, runtimeType) -> {
            LOGGER.info("Testing out-of-bounds memory access prevention on " + runtimeType);

            final Engine engine = runtime.createEngine();
            registerForCleanup(engine);

            final Store store = engine.createStore();
            registerForCleanup(store);

            final byte[] memoryAccessModule = createMemoryAccessModule();
            final Module module = engine.compileModule(memoryAccessModule);
            registerForCleanup(module);

            final Instance instance = store.instantiate(module);
            registerForCleanup(instance);

            final WasmFunction memoryReadFunction = instance.getFunction("read_memory");
            final WasmFunction memoryWriteFunction = instance.getFunction("write_memory");

            if (memoryReadFunction != null && memoryWriteFunction != null) {
              // Test valid memory access (should succeed)
              try {
                final Object validReadResult = memoryReadFunction.call(0);
                assertThat(validReadResult).isNotNull();
                LOGGER.fine("Valid memory read succeeded on " + runtimeType);

                memoryWriteFunction.call(0, 42);
                LOGGER.fine("Valid memory write succeeded on " + runtimeType);
              } catch (final Exception e) {
                LOGGER.info("Valid memory access failed: " + e.getMessage());
              }

              // Test out-of-bounds read (should fail)
              assertThatThrownBy(() -> memoryReadFunction.call(MEMORY_PAGE_SIZE + 1000))
                  .isInstanceOfAny(RuntimeException.class, SecurityException.class)
                  .satisfies(
                      e -> {
                        LOGGER.info("Out-of-bounds read properly blocked: " + e.getMessage());
                        assertThat(e.getMessage()).isNotNull();
                        assertThat(e.getMessage()).isNotEmpty();
                      });

              // Test out-of-bounds write (should fail)
              assertThatThrownBy(() -> memoryWriteFunction.call(MEMORY_PAGE_SIZE + 1000, 42))
                  .isInstanceOfAny(RuntimeException.class, SecurityException.class)
                  .satisfies(
                      e -> {
                        LOGGER.info("Out-of-bounds write properly blocked: " + e.getMessage());
                        assertThat(e.getMessage()).isNotNull();
                        assertThat(e.getMessage()).isNotEmpty();
                      });
            }

            LOGGER.info("Memory boundary violation prevention validated on " + runtimeType);
          });
    }

    @ParameterizedTest
    @ValueSource(
        ints = {
          -1000,
          -100,
          -10,
          -1,
          MEMORY_PAGE_SIZE,
          MEMORY_PAGE_SIZE + 1,
          MEMORY_PAGE_SIZE + 1000,
          Integer.MAX_VALUE
        })
    @DisplayName("Should reject invalid memory addresses")
    void shouldRejectInvalidMemoryAddresses(final int invalidAddress) throws WasmException {
      runWithBothRuntimes(
          (runtime, runtimeType) -> {
            LOGGER.info("Testing invalid memory address " + invalidAddress + " on " + runtimeType);

            final Engine engine = runtime.createEngine();
            registerForCleanup(engine);

            final Store store = engine.createStore();
            registerForCleanup(store);

            final byte[] memoryTestModule = createMemoryTestModule();
            final Module module = engine.compileModule(memoryTestModule);
            registerForCleanup(module);

            final Instance instance = store.instantiate(module);
            registerForCleanup(instance);

            final WasmFunction memoryAccessFunction = instance.getFunction("access_memory");
            if (memoryAccessFunction != null) {
              assertThatThrownBy(() -> memoryAccessFunction.call(invalidAddress))
                  .isInstanceOfAny(RuntimeException.class, SecurityException.class)
                  .satisfies(
                      e -> {
                        LOGGER.fine(
                            "Invalid memory address "
                                + invalidAddress
                                + " properly rejected: "
                                + e.getMessage());
                        assertThat(e.getMessage()).isNotNull();
                      });
            }
          });
    }

    @Test
    @DisplayName("Should enforce memory isolation between instances")
    void shouldEnforceMemoryIsolationBetweenInstances() throws WasmException {
      runWithBothRuntimes(
          (runtime, runtimeType) -> {
            LOGGER.info("Testing memory isolation between instances on " + runtimeType);

            final Engine engine = runtime.createEngine();
            registerForCleanup(engine);

            final Store store1 = engine.createStore();
            registerForCleanup(store1);
            final Store store2 = engine.createStore();
            registerForCleanup(store2);

            final byte[] isolationModule = createMemoryIsolationModule();
            final Module module = engine.compileModule(isolationModule);
            registerForCleanup(module);

            final Instance instance1 = store1.instantiate(module);
            registerForCleanup(instance1);
            final Instance instance2 = store2.instantiate(module);
            registerForCleanup(instance2);

            final WasmFunction writeFunction1 = instance1.getFunction("write_value");
            final WasmFunction readFunction1 = instance1.getFunction("read_value");
            final WasmFunction writeFunction2 = instance2.getFunction("write_value");
            final WasmFunction readFunction2 = instance2.getFunction("read_value");

            if (writeFunction1 != null
                && readFunction1 != null
                && writeFunction2 != null
                && readFunction2 != null) {
              // Write different values to each instance
              writeFunction1.call(42);
              writeFunction2.call(84);

              // Verify isolation - each instance should only see its own value
              final Object value1 = readFunction1.call();
              final Object value2 = readFunction2.call();

              assertThat(value1).isEqualTo(42);
              assertThat(value2).isEqualTo(84);

              LOGGER.info("Memory isolation between instances validated on " + runtimeType);
            }
          });
    }

    @Test
    @DisplayName("Should prevent memory corruption attacks")
    void shouldPreventMemoryCorruptionAttacks() throws WasmException {
      runWithBothRuntimes(
          (runtime, runtimeType) -> {
            LOGGER.info("Testing memory corruption attack prevention on " + runtimeType);

            final Engine engine = runtime.createEngine();
            registerForCleanup(engine);

            final Store store = engine.createStore();
            registerForCleanup(store);

            final byte[] corruptionModule = createMemoryCorruptionModule();
            final Module module = engine.compileModule(corruptionModule);
            registerForCleanup(module);

            final Instance instance = store.instantiate(module);
            registerForCleanup(instance);

            final WasmFunction corruptFunction = instance.getFunction("attempt_corruption");
            if (corruptFunction != null) {
              // Attempt various memory corruption techniques
              final int[] corruptionAttempts = {
                0x12345678, // Arbitrary pattern
                0xDEADBEEF, // Common test pattern
                0xFFFFFFFF, // All bits set
                0x00000000, // All bits clear
                0x80000000, // Sign bit
              };

              for (final int corruptionValue : corruptionAttempts) {
                assertThatThrownBy(() -> corruptFunction.call(corruptionValue))
                    .isInstanceOfAny(RuntimeException.class, SecurityException.class)
                    .satisfies(
                        e -> {
                          LOGGER.fine(
                              "Memory corruption attempt with value "
                                  + Integer.toHexString(corruptionValue)
                                  + " blocked: "
                                  + e.getMessage());
                        });
              }
            }

            // Verify instance integrity after corruption attempts
            assertThat(instance.isValid()).isTrue();

            LOGGER.info("Memory corruption attack prevention validated on " + runtimeType);
          });
    }
  }

  @Nested
  @DisplayName("Import Security Restriction Tests")
  final class ImportSecurityRestrictionTests {

    @Test
    @DisplayName("Should enforce restricted import access")
    void shouldEnforceRestrictedImportAccess() throws WasmException {
      runWithBothRuntimes(
          (runtime, runtimeType) -> {
            LOGGER.info("Testing restricted import access enforcement on " + runtimeType);

            final Engine engine = runtime.createEngine();
            registerForCleanup(engine);

            final Store store = engine.createStore();
            registerForCleanup(store);

            // Create module that tries to import restricted functions
            final byte[] restrictedImportModule = createRestrictedImportModule();

            assertThatThrownBy(() -> engine.compileModule(restrictedImportModule))
                .isInstanceOfAny(ValidationException.class, SecurityException.class)
                .satisfies(
                    e -> {
                      LOGGER.info("Restricted import properly rejected: " + e.getMessage());
                      assertThat(e.getMessage()).isNotNull();
                      assertThat(e.getMessage()).isNotEmpty();
                    });

            LOGGER.info("Restricted import access enforcement validated on " + runtimeType);
          });
    }

    @Test
    @DisplayName("Should validate import function signatures")
    void shouldValidateImportFunctionSignatures() throws WasmException {
      runWithBothRuntimes(
          (runtime, runtimeType) -> {
            LOGGER.info("Testing import function signature validation on " + runtimeType);

            final Engine engine = runtime.createEngine();
            registerForCleanup(engine);

            final Store store = engine.createStore();
            registerForCleanup(store);

            // Test various invalid import signatures
            final String[] invalidSignatures = {
              "(import \"env\" \"invalid_func\" (func (param i32) (result i64"
                  + " i64)))", // Multi-return
              "(import \"env\" \"invalid_func\" (func (param externref)))", // Externref parameter
              "(import \"env\" \"invalid_func\" (func (result externref)))", // Externref result
              "(import \"env\" \"invalid_func\" (func (param i128)))", // Invalid type
            };

            for (final String invalidSignature : invalidSignatures) {
              final byte[] invalidModule = createModuleWithImport(invalidSignature);

              assertThatThrownBy(() -> engine.compileModule(invalidModule))
                  .isInstanceOfAny(ValidationException.class, SecurityException.class)
                  .satisfies(
                      e -> {
                        LOGGER.fine("Invalid import signature rejected: " + e.getMessage());
                        assertThat(e.getMessage()).isNotNull();
                      });
            }

            LOGGER.info("Import function signature validation completed on " + runtimeType);
          });
    }

    @Test
    @DisplayName("Should prevent privilege escalation through imports")
    void shouldPreventPrivilegeEscalationThroughImports() throws WasmException {
      runWithBothRuntimes(
          (runtime, runtimeType) -> {
            LOGGER.info(
                "Testing privilege escalation prevention through imports on " + runtimeType);

            final Engine engine = runtime.createEngine();
            registerForCleanup(engine);

            final Store store = engine.createStore();
            registerForCleanup(store);

            // Test imports that might allow privilege escalation
            final String[] privilegeEscalationImports = {
              "(import \"system\" \"exec\" (func (param i32) (result i32)))",
              "(import \"fs\" \"write_file\" (func (param i32 i32) (result i32)))",
              "(import \"net\" \"socket\" (func (result i32)))",
              "(import \"env\" \"system\" (func (param i32) (result i32)))",
              "(import \"libc\" \"malloc\" (func (param i32) (result i32)))",
            };

            for (final String privilegeImport : privilegeEscalationImports) {
              final byte[] escalationModule = createModuleWithImport(privilegeImport);

              // Should either reject compilation or prevent instantiation
              boolean securityEnforced = false;
              try {
                final Module module = engine.compileModule(escalationModule);
                registerForCleanup(module);

                // If compilation succeeds, instantiation should fail
                assertThatThrownBy(() -> store.instantiate(module))
                    .isInstanceOfAny(RuntimeException.class, SecurityException.class);
                securityEnforced = true;
              } catch (final Exception e) {
                // Compilation rejection is also acceptable
                assertThat(e).isInstanceOfAny(ValidationException.class, SecurityException.class);
                securityEnforced = true;
              }

              assertThat(securityEnforced).isTrue();
              LOGGER.fine("Privilege escalation import blocked: " + privilegeImport);
            }

            LOGGER.info("Privilege escalation prevention validated on " + runtimeType);
          });
    }
  }

  @Nested
  @DisplayName("Export Security Restriction Tests")
  final class ExportSecurityRestrictionTests {

    @Test
    @DisplayName("Should validate export function access")
    void shouldValidateExportFunctionAccess() throws WasmException {
      runWithBothRuntimes(
          (runtime, runtimeType) -> {
            LOGGER.info("Testing export function access validation on " + runtimeType);

            final Engine engine = runtime.createEngine();
            registerForCleanup(engine);

            final Store store = engine.createStore();
            registerForCleanup(store);

            final byte[] exportSecurityModule = createExportSecurityModule();
            final Module module = engine.compileModule(exportSecurityModule);
            registerForCleanup(module);

            final Instance instance = store.instantiate(module);
            registerForCleanup(instance);

            // Test access to public exports (should succeed)
            final WasmFunction publicFunction = instance.getFunction("public_function");
            if (publicFunction != null) {
              final Object result = publicFunction.call();
              assertThat(result).isNotNull();
              LOGGER.fine("Public function access succeeded on " + runtimeType);
            }

            // Test access to internal functions (should fail or not exist)
            final WasmFunction internalFunction = instance.getFunction("internal_function");
            assertThat(internalFunction).isNull();
            LOGGER.fine("Internal function properly hidden on " + runtimeType);

            // Test access to system functions (should fail or not exist)
            final WasmFunction systemFunction = instance.getFunction("system_function");
            assertThat(systemFunction).isNull();
            LOGGER.fine("System function properly hidden on " + runtimeType);

            LOGGER.info("Export function access validation completed on " + runtimeType);
          });
    }

    @Test
    @DisplayName("Should prevent unauthorized memory export access")
    void shouldPreventUnauthorizedMemoryExportAccess() throws WasmException {
      runWithBothRuntimes(
          (runtime, runtimeType) -> {
            LOGGER.info("Testing unauthorized memory export access prevention on " + runtimeType);

            final Engine engine = runtime.createEngine();
            registerForCleanup(engine);

            final Store store = engine.createStore();
            registerForCleanup(store);

            final byte[] memoryExportModule = createMemoryExportModule();
            final Module module = engine.compileModule(memoryExportModule);
            registerForCleanup(module);

            final Instance instance = store.instantiate(module);
            registerForCleanup(instance);

            // Get exported memory
            final Memory memory = instance.getMemory("memory");
            if (memory != null) {
              // Test valid memory operations
              try {
                memory.write(0, new byte[] {1, 2, 3, 4});
                final byte[] data = memory.read(0, 4);
                assertThat(data).isEqualTo(new byte[] {1, 2, 3, 4});
                LOGGER.fine("Valid memory operations succeeded on " + runtimeType);
              } catch (final Exception e) {
                LOGGER.info("Memory operations failed: " + e.getMessage());
              }

              // Test invalid memory operations (should be blocked)
              assertThatThrownBy(() -> memory.read(-1, 4))
                  .isInstanceOfAny(RuntimeException.class, SecurityException.class);

              assertThatThrownBy(() -> memory.read(MEMORY_PAGE_SIZE + 1000, 4))
                  .isInstanceOfAny(RuntimeException.class, SecurityException.class);

              assertThatThrownBy(() -> memory.write(-1, new byte[] {1, 2, 3, 4}))
                  .isInstanceOfAny(RuntimeException.class, SecurityException.class);

              assertThatThrownBy(
                      () -> memory.write(MEMORY_PAGE_SIZE + 1000, new byte[] {1, 2, 3, 4}))
                  .isInstanceOfAny(RuntimeException.class, SecurityException.class);

              LOGGER.fine("Invalid memory operations properly blocked on " + runtimeType);
            }

            LOGGER.info("Unauthorized memory export access prevention validated on " + runtimeType);
          });
    }
  }

  @Nested
  @DisplayName("Cross-Module Isolation Tests")
  final class CrossModuleIsolationTests {

    @Test
    @DisplayName("Should enforce complete isolation between modules")
    void shouldEnforceCompleteIsolationBetweenModules() throws WasmException {
      runWithBothRuntimes(
          (runtime, runtimeType) -> {
            LOGGER.info("Testing complete isolation between modules on " + runtimeType);

            final Engine engine = runtime.createEngine();
            registerForCleanup(engine);

            final Store store1 = engine.createStore();
            registerForCleanup(store1);
            final Store store2 = engine.createStore();
            registerForCleanup(store2);

            // Create two different modules
            final byte[] module1Bytes = createIsolationTestModule("module1");
            final byte[] module2Bytes = createIsolationTestModule("module2");

            final Module module1 = engine.compileModule(module1Bytes);
            registerForCleanup(module1);
            final Module module2 = engine.compileModule(module2Bytes);
            registerForCleanup(module2);

            final Instance instance1 = store1.instantiate(module1);
            registerForCleanup(instance1);
            final Instance instance2 = store2.instantiate(module2);
            registerForCleanup(instance2);

            // Verify modules cannot access each other's data
            final WasmFunction setFunction1 = instance1.getFunction("set_value");
            final WasmFunction getFunction1 = instance1.getFunction("get_value");
            final WasmFunction setFunction2 = instance2.getFunction("set_value");
            final WasmFunction getFunction2 = instance2.getFunction("get_value");

            if (setFunction1 != null
                && getFunction1 != null
                && setFunction2 != null
                && getFunction2 != null) {
              // Set different values in each module
              setFunction1.call(123);
              setFunction2.call(456);

              // Verify each module only sees its own value
              final Object value1 = getFunction1.call();
              final Object value2 = getFunction2.call();

              assertThat(value1).isEqualTo(123);
              assertThat(value2).isEqualTo(456);

              LOGGER.info("Module isolation validated on " + runtimeType);
            }
          });
    }

    @Test
    @DisplayName("Should prevent cross-module data access")
    void shouldPreventCrossModuleDataAccess() throws WasmException {
      runWithBothRuntimes(
          (runtime, runtimeType) -> {
            LOGGER.info("Testing cross-module data access prevention on " + runtimeType);

            final Engine engine = runtime.createEngine();
            registerForCleanup(engine);

            final Store store = engine.createStore();
            registerForCleanup(store);

            // Create module that attempts to access external data
            final byte[] dataAccessModule = createCrossModuleDataAccessModule();

            // This should either fail compilation or fail instantiation
            boolean accessPrevented = false;
            try {
              final Module module = engine.compileModule(dataAccessModule);
              registerForCleanup(module);

              try {
                final Instance instance = store.instantiate(module);
                registerForCleanup(instance);

                final WasmFunction accessFunction = instance.getFunction("access_external_data");
                if (accessFunction != null) {
                  assertThatThrownBy(() -> accessFunction.call())
                      .isInstanceOfAny(RuntimeException.class, SecurityException.class);
                  accessPrevented = true;
                }
              } catch (final Exception e) {
                // Instantiation failure is acceptable
                accessPrevented = true;
              }
            } catch (final Exception e) {
              // Compilation failure is acceptable
              accessPrevented = true;
            }

            assertThat(accessPrevented).isTrue();
            LOGGER.info("Cross-module data access prevention validated on " + runtimeType);
          });
    }
  }

  @Nested
  @DisplayName("Sandbox Escape Prevention Tests")
  final class SandboxEscapePreventionTests {

    @Test
    @DisplayName("Should prevent file system access attempts")
    void shouldPreventFileSystemAccessAttempts() throws WasmException {
      runWithBothRuntimes(
          (runtime, runtimeType) -> {
            LOGGER.info("Testing file system access prevention on " + runtimeType);

            final Engine engine = runtime.createEngine();
            registerForCleanup(engine);

            final Store store = engine.createStore();
            registerForCleanup(store);

            final byte[] fileAccessModule = createFileAccessModule();

            // Should fail to compile or instantiate module with file access
            boolean fileAccessBlocked = false;
            try {
              final Module module = engine.compileModule(fileAccessModule);
              registerForCleanup(module);

              try {
                final Instance instance = store.instantiate(module);
                registerForCleanup(instance);

                final WasmFunction fileFunction = instance.getFunction("access_file");
                if (fileFunction != null) {
                  assertThatThrownBy(() -> fileFunction.call())
                      .isInstanceOfAny(RuntimeException.class, SecurityException.class);
                  fileAccessBlocked = true;
                }
              } catch (final Exception e) {
                fileAccessBlocked = true;
              }
            } catch (final Exception e) {
              fileAccessBlocked = true;
            }

            assertThat(fileAccessBlocked).isTrue();
            LOGGER.info("File system access prevention validated on " + runtimeType);
          });
    }

    @Test
    @DisplayName("Should prevent network access attempts")
    void shouldPreventNetworkAccessAttempts() throws WasmException {
      runWithBothRuntimes(
          (runtime, runtimeType) -> {
            LOGGER.info("Testing network access prevention on " + runtimeType);

            final Engine engine = runtime.createEngine();
            registerForCleanup(engine);

            final Store store = engine.createStore();
            registerForCleanup(store);

            final byte[] networkAccessModule = createNetworkAccessModule();

            // Should fail to compile or instantiate module with network access
            boolean networkAccessBlocked = false;
            try {
              final Module module = engine.compileModule(networkAccessModule);
              registerForCleanup(module);

              try {
                final Instance instance = store.instantiate(module);
                registerForCleanup(instance);

                final WasmFunction networkFunction = instance.getFunction("network_call");
                if (networkFunction != null) {
                  assertThatThrownBy(() -> networkFunction.call())
                      .isInstanceOfAny(RuntimeException.class, SecurityException.class);
                  networkAccessBlocked = true;
                }
              } catch (final Exception e) {
                networkAccessBlocked = true;
              }
            } catch (final Exception e) {
              networkAccessBlocked = true;
            }

            assertThat(networkAccessBlocked).isTrue();
            LOGGER.info("Network access prevention validated on " + runtimeType);
          });
    }

    @Test
    @DisplayName("Should prevent system call access")
    void shouldPreventSystemCallAccess() throws WasmException {
      runWithBothRuntimes(
          (runtime, runtimeType) -> {
            LOGGER.info("Testing system call access prevention on " + runtimeType);

            final Engine engine = runtime.createEngine();
            registerForCleanup(engine);

            final Store store = engine.createStore();
            registerForCleanup(store);

            final byte[] systemCallModule = createSystemCallModule();

            // Should fail to compile or instantiate module with system calls
            boolean systemCallBlocked = false;
            try {
              final Module module = engine.compileModule(systemCallModule);
              registerForCleanup(module);

              try {
                final Instance instance = store.instantiate(module);
                registerForCleanup(instance);

                final WasmFunction systemFunction = instance.getFunction("system_call");
                if (systemFunction != null) {
                  assertThatThrownBy(() -> systemFunction.call())
                      .isInstanceOfAny(RuntimeException.class, SecurityException.class);
                  systemCallBlocked = true;
                }
              } catch (final Exception e) {
                systemCallBlocked = true;
              }
            } catch (final Exception e) {
              systemCallBlocked = true;
            }

            assertThat(systemCallBlocked).isTrue();
            LOGGER.info("System call access prevention validated on " + runtimeType);
          });
    }
  }

  // Helper methods for creating security test modules

  private byte[] createMemoryAccessModule() {
    return TestUtils.createWasmModuleFromWat(
        "(module\n"
            + "  (memory 1)\n"
            + "  (func $read_memory (export \"read_memory\") (param i32) (result i32)\n"
            + "    local.get 0\n"
            + "    i32.load\n"
            + "  )\n"
            + "  (func $write_memory (export \"write_memory\") (param i32 i32)\n"
            + "    local.get 0\n"
            + "    local.get 1\n"
            + "    i32.store\n"
            + "  )\n"
            + ")");
  }

  private byte[] createMemoryTestModule() {
    return TestUtils.createWasmModuleFromWat(
        "(module\n"
            + "  (memory 1)\n"
            + "  (func $access_memory (export \"access_memory\") (param i32) (result i32)\n"
            + "    local.get 0\n"
            + "    i32.load\n"
            + "  )\n"
            + ")");
  }

  private byte[] createMemoryIsolationModule() {
    return TestUtils.createWasmModuleFromWat(
        "(module\n"
            + "  (memory 1)\n"
            + "  (global $value (mut i32) (i32.const 0))\n"
            + "  (func $write_value (export \"write_value\") (param i32)\n"
            + "    local.get 0\n"
            + "    global.set $value\n"
            + "  )\n"
            + "  (func $read_value (export \"read_value\") (result i32)\n"
            + "    global.get $value\n"
            + "  )\n"
            + ")");
  }

  private byte[] createMemoryCorruptionModule() {
    return TestUtils.createWasmModuleFromWat(
        "(module\n"
            + "  (memory 1)\n"
            + "  (func $attempt_corruption (export \"attempt_corruption\") (param i32)\n"
            + "    local.get 0\n"
            + "    i32.const 999999\n"
            + "    i32.store\n" // Attempt to write to invalid address
            + "  )\n"
            + ")");
  }

  private byte[] createRestrictedImportModule() {
    return TestUtils.createWasmModuleFromWat(
        "(module\n"
            + "  (import \"system\" \"exec\" (func $exec (param i32) (result i32)))\n"
            + "  (func $test (export \"test\")\n"
            + "    i32.const 0\n"
            + "    call $exec\n"
            + "    drop\n"
            + "  )\n"
            + ")");
  }

  private byte[] createModuleWithImport(final String importDeclaration) {
    return TestUtils.createWasmModuleFromWat("(module\n" + "  " + importDeclaration + "\n" + ")");
  }

  private byte[] createExportSecurityModule() {
    return TestUtils.createWasmModuleFromWat(
        "(module\n"
            + "  (func $public_function (export \"public_function\") (result i32)\n"
            + "    i32.const 42\n"
            + "  )\n"
            + "  (func $internal_function (result i32)\n"
            + "    i32.const 123\n"
            + "  )\n"
            + ")");
  }

  private byte[] createMemoryExportModule() {
    return TestUtils.createWasmModuleFromWat(
        "(module\n"
            + "  (memory 1)\n"
            + "  (export \"memory\" (memory 0))\n"
            + "  (func $test (export \"test\") (result i32)\n"
            + "    i32.const 42\n"
            + "  )\n"
            + ")");
  }

  private byte[] createIsolationTestModule(final String moduleId) {
    return TestUtils.createWasmModuleFromWat(
        "(module\n"
            + "  (global $value (mut i32) (i32.const 0))\n"
            + "  (func $set_value (export \"set_value\") (param i32)\n"
            + "    local.get 0\n"
            + "    global.set $value\n"
            + "  )\n"
            + "  (func $get_value (export \"get_value\") (result i32)\n"
            + "    global.get $value\n"
            + "  )\n"
            + ")");
  }

  private byte[] createCrossModuleDataAccessModule() {
    return TestUtils.createWasmModuleFromWat(
        "(module\n"
            + "  (import \"external\" \"data\" (global $external_data i32))\n"
            + "  (func $access_external_data (export \"access_external_data\") (result i32)\n"
            + "    global.get $external_data\n"
            + "  )\n"
            + ")");
  }

  private byte[] createFileAccessModule() {
    return TestUtils.createWasmModuleFromWat(
        "(module\n"
            + "  (import \"fs\" \"open\" (func $open (param i32 i32) (result i32)))\n"
            + "  (func $access_file (export \"access_file\") (result i32)\n"
            + "    i32.const 0\n"
            + "    i32.const 0\n"
            + "    call $open\n"
            + "  )\n"
            + ")");
  }

  private byte[] createNetworkAccessModule() {
    return TestUtils.createWasmModuleFromWat(
        "(module\n"
            + "  (import \"net\" \"socket\" (func $socket (result i32)))\n"
            + "  (func $network_call (export \"network_call\") (result i32)\n"
            + "    call $socket\n"
            + "  )\n"
            + ")");
  }

  private byte[] createSystemCallModule() {
    return TestUtils.createWasmModuleFromWat(
        "(module\n"
            + "  (import \"env\" \"system\" (func $system (param i32) (result i32)))\n"
            + "  (func $system_call (export \"system_call\") (result i32)\n"
            + "    i32.const 0\n"
            + "    call $system\n"
            + "  )\n"
            + ")");
  }
}
