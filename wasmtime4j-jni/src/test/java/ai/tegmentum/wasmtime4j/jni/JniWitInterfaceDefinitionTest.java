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

package ai.tegmentum.wasmtime4j.jni;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link JniWitInterfaceDefinition} class.
 *
 * <p>JniWitInterfaceDefinition provides JNI implementation of WIT interface definitions.
 */
@DisplayName("JniWitInterfaceDefinition Tests")
class JniWitInterfaceDefinitionTest {

  @Nested
  @DisplayName("Instance Creation Tests")
  class InstanceCreationTests {

    @Test
    @DisplayName("should create instance with all parameters")
    void shouldCreateInstanceWithAllParameters() {
      final Set<String> exports = new HashSet<>(Arrays.asList("export1", "export2"));
      final Set<String> imports = new HashSet<>(Arrays.asList("import1"));

      final JniWitInterfaceDefinition definition =
          new JniWitInterfaceDefinition(
              "test-interface", "1.0.0", "test-package", exports, imports);

      assertEquals("test-interface", definition.getName(), "Name should match");
      assertEquals("1.0.0", definition.getVersion(), "Version should match");
      assertEquals("test-package", definition.getPackageName(), "Package name should match");
    }

    @Test
    @DisplayName("should handle null parameters gracefully")
    void shouldHandleNullParametersGracefully() {
      final JniWitInterfaceDefinition definition =
          new JniWitInterfaceDefinition(null, null, null, null, null);

      assertNotNull(definition.getName(), "Name should not be null");
      assertNotNull(definition.getVersion(), "Version should not be null");
      assertNotNull(definition.getPackageName(), "Package name should not be null");
    }
  }
}
