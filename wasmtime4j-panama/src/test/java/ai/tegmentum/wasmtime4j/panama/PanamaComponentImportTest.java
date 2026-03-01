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
package ai.tegmentum.wasmtime4j.panama;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.component.Component;
import ai.tegmentum.wasmtime4j.component.ComponentEngineConfig;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests for Component Model import introspection.
 *
 * <p>Verifies that components with imports can be loaded and their import interfaces can be
 * enumerated correctly.
 *
 * @since 1.0.0
 */
final class PanamaComponentImportTest {

  private PanamaEngine panamaEngine;
  private PanamaComponentEngine componentEngine;

  @BeforeEach
  void setUp() throws WasmException {
    panamaEngine = new PanamaEngine();
    componentEngine = new PanamaComponentEngine(new ComponentEngineConfig());
  }

  @AfterEach
  void tearDown() {
    if (componentEngine != null) {
      componentEngine.close();
    }
    if (panamaEngine != null) {
      panamaEngine.close();
    }
  }

  @Test
  @DisplayName("Component with imports should enumerate imported interfaces")
  void testComponentWithImports() throws IOException, WasmException {
    final Path componentPath = Path.of("src/test/resources/components/with-imports.wasm");
    final byte[] componentBytes = Files.readAllBytes(componentPath);

    final Component component = componentEngine.compileComponent(componentBytes);
    assertNotNull(component, "Component should be loaded");

    final Set<String> imports = component.getImportedInterfaces();
    assertNotNull(imports, "Imports should not be null");
    assertFalse(imports.isEmpty(), "Component should have imports");

    // Verify our custom logger interface is imported
    boolean hasLogger = imports.stream().anyMatch(name -> name.contains("logger"));
    assertTrue(hasLogger, "Should import logger interface");

    // Verify WASI interfaces are imported
    boolean hasWasi = imports.stream().anyMatch(name -> name.contains("wasi:"));
    assertTrue(hasWasi, "Should import WASI interfaces");

    component.close();
  }
}
