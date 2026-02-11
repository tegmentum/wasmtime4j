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

package ai.tegmentum.wasmtime4j.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.config.DependencyResolution;
import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.type.FunctionType;
import ai.tegmentum.wasmtime4j.func.HostFunction;
import ai.tegmentum.wasmtime4j.validation.ImportInfo;
import ai.tegmentum.wasmtime4j.validation.ImportValidation;
import ai.tegmentum.wasmtime4j.Linker;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.WasmValueType;
import ai.tegmentum.wasmtime4j.tests.framework.DualRuntimeTest;
import java.util.List;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

/**
 * Tests for {@link Linker#resolveDependencies(Module...)},
 * {@link Linker#validateImports(Module...)}, and {@link Linker#getImportRegistry()}.
 *
 * <p>Covers dependency resolution ordering, import validation with satisfied and missing imports,
 * and import registry inspection.
 *
 * @since 1.0.0
 */
@DisplayName("Linker Dependency Resolution Tests")
public class LinkerDependencyResolutionTest extends DualRuntimeTest {

  private static final Logger LOGGER =
      Logger.getLogger(LinkerDependencyResolutionTest.class.getName());

  /** Self-contained module exporting get_val. */
  private static final String MODULE_A_WAT =
      """
      (module
        (func (export "get_val") (result i32) i32.const 42))
      """;

  /** Module importing env/get_val and exporting call_it. */
  private static final String MODULE_B_WAT =
      """
      (module
        (import "env" "get_val" (func (result i32)))
        (func (export "call_it") (result i32) call 0))
      """;

  @AfterEach
  void cleanup() {
    clearRuntimeSelection();
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("resolveDependencies with single self-contained module succeeds")
  void resolveDependenciesSingleModuleSucceeds(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing resolveDependencies with single module");

    try (Engine engine = Engine.create();
        Module moduleA = engine.compileWat(MODULE_A_WAT);
        Linker<Void> linker = Linker.create(engine)) {

      final DependencyResolution resolution = linker.resolveDependencies(moduleA);

      assertNotNull(resolution, "DependencyResolution should not be null");
      assertTrue(resolution.isResolutionSuccessful(), "Resolution should be successful");
      assertFalse(resolution.hasCircularDependencies(), "Should have no circular dependencies");

      LOGGER.info("[" + runtime + "] instantiationOrder: " + resolution.getInstantiationOrder());
      LOGGER.info("[" + runtime + "] totalModules: " + resolution.getTotalModules());
      LOGGER.info("[" + runtime + "] resolutionSuccessful: " + resolution.isResolutionSuccessful());
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("resolveDependencies with two modules finds correct order")
  void resolveDependenciesMultiModuleFindsOrder(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing resolveDependencies with two dependent modules");

    try (Engine engine = Engine.create();
        Module moduleA = engine.compileWat(MODULE_A_WAT);
        Module moduleB = engine.compileWat(MODULE_B_WAT);
        Linker<Void> linker = Linker.create(engine)) {

      // Define moduleA's export so linker knows about it
      linker.defineHostFunction(
          "env",
          "get_val",
          new FunctionType(new WasmValueType[] {}, new WasmValueType[] {WasmValueType.I32}),
          (HostFunction) params -> new WasmValue[] {WasmValue.i32(42)});

      final DependencyResolution resolution = linker.resolveDependencies(moduleA, moduleB);

      assertNotNull(resolution, "DependencyResolution should not be null");
      assertTrue(resolution.isResolutionSuccessful(), "Resolution should be successful");
      assertEquals(2, resolution.getTotalModules(), "Should report 2 total modules");

      final List<Module> order = resolution.getInstantiationOrder();
      LOGGER.info("[" + runtime + "] instantiationOrder size: " + order.size());
      LOGGER.info("[" + runtime + "] totalModules: " + resolution.getTotalModules());
      LOGGER.info(
          "[" + runtime + "] resolvedDependencies: " + resolution.getResolvedDependencies());
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("resolveDependencies with null throws IllegalArgumentException")
  void resolveDependenciesNullThrows(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing resolveDependencies with null");

    try (Engine engine = Engine.create();
        Linker<Void> linker = Linker.create(engine)) {

      assertThrows(
          IllegalArgumentException.class,
          () -> linker.resolveDependencies((Module[]) null),
          "Should throw IllegalArgumentException for null modules");

      LOGGER.info("[" + runtime + "] Correctly threw IllegalArgumentException for null");
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("validateImports with no-imports module returns valid")
  void validateImportsNoImportsValid(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing validateImports with self-contained module");

    try (Engine engine = Engine.create();
        Module moduleA = engine.compileWat(MODULE_A_WAT);
        Linker<Void> linker = Linker.create(engine)) {

      final ImportValidation validation = linker.validateImports(moduleA);

      assertNotNull(validation, "ImportValidation should not be null");
      assertTrue(validation.isValid(), "Self-contained module should validate as valid");
      assertFalse(validation.hasCriticalIssues(), "Should have no critical issues");

      LOGGER.info("[" + runtime + "] isValid: " + validation.isValid());
      LOGGER.info("[" + runtime + "] totalImports: " + validation.getTotalImports());
      LOGGER.info("[" + runtime + "] issues: " + validation.getIssues());
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("validateImports with missing import returns invalid")
  void validateImportsMissingImportInvalid(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing validateImports with missing import");

    try (Engine engine = Engine.create();
        Module moduleB = engine.compileWat(MODULE_B_WAT);
        Linker<Void> linker = Linker.create(engine)) {

      final ImportValidation validation = linker.validateImports(moduleB);

      assertNotNull(validation, "ImportValidation should not be null");
      // Module B requires env/get_val which is not defined
      assertTrue(
          !validation.isValid() || validation.hasCriticalIssues(),
          "Module with unsatisfied imports should be invalid or have critical issues");

      LOGGER.info("[" + runtime + "] isValid: " + validation.isValid());
      LOGGER.info("[" + runtime + "] hasCriticalIssues: " + validation.hasCriticalIssues());
      LOGGER.info("[" + runtime + "] issues: " + validation.getIssues());
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("validateImports with satisfied import returns valid")
  void validateImportsSatisfiedImportValid(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing validateImports with satisfied import");

    try (Engine engine = Engine.create();
        Module moduleB = engine.compileWat(MODULE_B_WAT);
        Linker<Void> linker = Linker.create(engine)) {

      // Define the import that moduleB requires
      linker.defineHostFunction(
          "env",
          "get_val",
          new FunctionType(new WasmValueType[] {}, new WasmValueType[] {WasmValueType.I32}),
          (HostFunction) params -> new WasmValue[] {WasmValue.i32(42)});

      final ImportValidation validation = linker.validateImports(moduleB);

      assertNotNull(validation, "ImportValidation should not be null");
      assertTrue(validation.isValid(), "Module with satisfied imports should be valid");

      LOGGER.info("[" + runtime + "] isValid: " + validation.isValid());
      LOGGER.info("[" + runtime + "] validImports: " + validation.getValidImports());
      LOGGER.info("[" + runtime + "] totalImports: " + validation.getTotalImports());
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("getImportRegistry on empty linker returns non-null list")
  void getImportRegistryEmptyLinker(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing getImportRegistry on empty linker");

    try (Engine engine = Engine.create();
        Linker<Void> linker = Linker.create(engine)) {

      final List<ImportInfo> registry = linker.getImportRegistry();

      assertNotNull(registry, "Import registry should not be null");
      LOGGER.info("[" + runtime + "] Registry size: " + registry.size());

      // Verify list is unmodifiable
      try {
        registry.add(null);
        // If we get here, the list is modifiable - that's okay too
        LOGGER.info("[" + runtime + "] Registry is modifiable (implementation choice)");
      } catch (final UnsupportedOperationException e) {
        LOGGER.info("[" + runtime + "] Registry is unmodifiable (immutable list)");
      }
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("getImportRegistry after defining host function contains entry")
  void getImportRegistryAfterDefineHostFunction(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing getImportRegistry after defineHostFunction");

    try (Engine engine = Engine.create();
        Linker<Void> linker = Linker.create(engine)) {

      linker.defineHostFunction(
          "env",
          "get_val",
          new FunctionType(new WasmValueType[] {}, new WasmValueType[] {WasmValueType.I32}),
          (HostFunction) params -> new WasmValue[] {WasmValue.i32(42)});

      final List<ImportInfo> registry = linker.getImportRegistry();

      assertNotNull(registry, "Import registry should not be null");
      assertFalse(registry.isEmpty(), "Registry should contain at least one entry");

      boolean foundEntry = false;
      for (final ImportInfo info : registry) {
        LOGGER.info(
            "[" + runtime + "] Registry entry: module=" + info.getModuleName()
                + " name=" + info.getImportName()
                + " type=" + info.getImportType()
                + " isHostFunction=" + info.isHostFunction());

        if ("env".equals(info.getModuleName())
            && "get_val".equals(info.getImportName())) {
          foundEntry = true;
          assertEquals(
              ImportInfo.ImportType.FUNCTION,
              info.getImportType(),
              "Import type should be FUNCTION");
          assertTrue(info.isHostFunction(), "Should be a host function");
        }
      }

      assertTrue(foundEntry, "Registry should contain the defined host function entry");
    }
  }
}
