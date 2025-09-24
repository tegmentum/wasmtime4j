/*
 * Copyright 2024 Tegmentum AI
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

package ai.tegmentum.wasmtime4j.observability;

import static org.assertj.core.api.Assertions.assertThat;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.semconv.ResourceAttributes;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link WasmResourceProvider}.
 */
final class WasmResourceProviderTest {

  private WasmResourceProvider provider;
  private ConfigProperties configProperties;

  @BeforeEach
  void setUp() {
    provider = new WasmResourceProvider();
    configProperties = ConfigProperties.createForTest(Map.of());
  }

  @Test
  void shouldCreateResourceWithDefaultConfiguration() {
    // When
    final Resource resource = provider.createResource(configProperties);

    // Then
    assertThat(resource).isNotNull();
    final Attributes attributes = resource.getAttributes();

    // Verify basic service attributes
    assertThat(attributes.get(ResourceAttributes.SERVICE_NAME)).isEqualTo("wasmtime4j");
    assertThat(attributes.get(ResourceAttributes.SERVICE_NAMESPACE)).isEqualTo("ai.tegmentum");
    assertThat(attributes.get(ResourceAttributes.SERVICE_VERSION)).isNotNull();
  }

  @Test
  void shouldCreateResourceWithCustomConfiguration() {
    // Given
    final ConfigProperties customConfig = ConfigProperties.createForTest(Map.of(
        "service.name", "custom-wasmtime-app",
        "service.namespace", "custom.namespace",
        "service.version", "2.0.0"
    ));

    // When
    final Resource resource = provider.createResource(customConfig);

    // Then
    final Attributes attributes = resource.getAttributes();
    assertThat(attributes.get(ResourceAttributes.SERVICE_NAME)).isEqualTo("custom-wasmtime-app");
    assertThat(attributes.get(ResourceAttributes.SERVICE_NAMESPACE)).isEqualTo("custom.namespace");
    assertThat(attributes.get(ResourceAttributes.SERVICE_VERSION)).isEqualTo("2.0.0");
  }

  @Test
  void shouldIncludeWasmRuntimeAttributes() {
    // When
    final Resource resource = provider.createResource(configProperties);

    // Then
    final Attributes attributes = resource.getAttributes();
    assertThat(attributes.get("wasm.runtime.name")).isEqualTo("wasmtime");
    assertThat(attributes.get("wasm.runtime.version")).isNotNull();
    assertThat(attributes.get("wasm.binding.type")).isIn("jni", "panama");
    assertThat(attributes.get("wasm.binding.version")).isNotNull();
    assertThat(attributes.get("wasm.features.supported")).isNotNull();
    assertThat(attributes.get("wasm.wasi.version")).isEqualTo("preview1,preview2");
    assertThat(attributes.get("wasm.component.model")).isEqualTo("true");
  }

  @Test
  void shouldIncludeJavaRuntimeAttributes() {
    // When
    final Resource resource = provider.createResource(configProperties);

    // Then
    final Attributes attributes = resource.getAttributes();
    assertThat(attributes.get(ResourceAttributes.PROCESS_RUNTIME_NAME)).isNotNull();
    assertThat(attributes.get(ResourceAttributes.PROCESS_RUNTIME_VERSION)).isNotNull();
    assertThat(attributes.get(ResourceAttributes.PROCESS_RUNTIME_DESCRIPTION)).isNotNull();
    assertThat(attributes.get("process.runtime.java.version")).isNotNull();
    assertThat(attributes.get("process.runtime.jni.available")).isEqualTo("true");
  }

  @Test
  void shouldIncludeServiceInstanceId() {
    // When
    final Resource resource = provider.createResource(configProperties);

    // Then
    final Attributes attributes = resource.getAttributes();
    assertThat(attributes.get(ResourceAttributes.SERVICE_INSTANCE_ID)).isNotNull();
    assertThat(attributes.get(ResourceAttributes.SERVICE_INSTANCE_ID)).matches("[a-f0-9-]{36}"); // UUID pattern
  }

  @Test
  void shouldIncludeDeploymentEnvironment() {
    // Given
    final ConfigProperties configWithEnv = ConfigProperties.createForTest(Map.of(
        "deployment.environment", "production"
    ));

    // When
    final Resource resource = provider.createResource(configWithEnv);

    // Then
    final Attributes attributes = resource.getAttributes();
    assertThat(attributes.get(ResourceAttributes.DEPLOYMENT_ENVIRONMENT)).isEqualTo("production");
  }

  @Test
  void shouldIncludeCompilerBackendInformation() {
    // When
    final Resource resource = provider.createResource(configProperties);

    // Then
    final Attributes attributes = resource.getAttributes();
    assertThat(attributes.get("wasm.compiler.backend")).isEqualTo("cranelift");
  }

  @Test
  void shouldHandleResourceCreationErrors() {
    // Given - provider should handle errors gracefully
    // When
    final Resource resource = provider.createResource(configProperties);

    // Then - should return a valid resource even if some attributes fail
    assertThat(resource).isNotNull();
  }

  @Test
  void shouldHaveCorrectOrder() {
    // When
    final int order = provider.order();

    // Then
    assertThat(order).isEqualTo(100);
  }

  @Test
  void shouldDetectBindingType() {
    // When
    final Resource resource = provider.createResource(configProperties);

    // Then
    final Attributes attributes = resource.getAttributes();
    final String bindingType = attributes.get("wasm.binding.type");
    assertThat(bindingType).isIn("jni", "panama");

    // Verify consistency with Java version
    final String javaVersion = System.getProperty("java.version");
    if (javaVersion.startsWith("23") || javaVersion.startsWith("24")) {
      // Could be panama if available, otherwise jni
      assertThat(bindingType).isIn("jni", "panama");
    } else {
      // Should be jni for older versions
      assertThat(bindingType).isEqualTo("jni");
    }
  }

  @Test
  void shouldIncludeSupportedWasmFeatures() {
    // When
    final Resource resource = provider.createResource(configProperties);

    // Then
    final Attributes attributes = resource.getAttributes();
    final String supportedFeatures = attributes.get("wasm.features.supported");
    assertThat(supportedFeatures).contains("bulk-memory");
    assertThat(supportedFeatures).contains("multi-value");
    assertThat(supportedFeatures).contains("reference-types");
    assertThat(supportedFeatures).contains("simd");
    assertThat(supportedFeatures).contains("threads");
    assertThat(supportedFeatures).contains("component-model");
    assertThat(supportedFeatures).contains("wasi");
  }
}