/*
 * Copyright 2024 Tegmentum AI. All rights reserved.
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

package ai.tegmentum.wasmtime4j.bindgen;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Tests for BindgenConfig.
 */
class BindgenConfigTest {

  @Test
  void shouldCreateConfigWithDefaultValues() {
    BindgenConfig config = BindgenConfig.builder()
        .packageName("com.example")
        .outputDirectory(Path.of("target/generated"))
        .addWitSource(Path.of("src/main/wit"))
        .build();

    assertThat(config.getCodeStyle()).isEqualTo(CodeStyle.MODERN);
    assertThat(config.getPackageName()).isEqualTo("com.example");
    assertThat(config.getOutputDirectory()).isEqualTo(Path.of("target/generated"));
    assertThat(config.isGenerateJavadoc()).isTrue();
    assertThat(config.isGenerateBuilders()).isTrue();
    assertThat(config.hasWitSources()).isTrue();
    assertThat(config.hasWasmSources()).isFalse();
  }

  @Test
  void shouldCreateConfigWithLegacyStyle() {
    BindgenConfig config = BindgenConfig.builder()
        .packageName("com.example")
        .outputDirectory(Path.of("target/generated"))
        .codeStyle(CodeStyle.LEGACY)
        .addWasmSource(Path.of("module.wasm"))
        .build();

    assertThat(config.getCodeStyle()).isEqualTo(CodeStyle.LEGACY);
    assertThat(config.hasWasmSources()).isTrue();
  }

  @Test
  void shouldValidateRequiredPackageName() {
    BindgenConfig config = BindgenConfig.builder()
        .outputDirectory(Path.of("target/generated"))
        .addWitSource(Path.of("src/main/wit"))
        .build();

    assertThatThrownBy(config::validate)
        .isInstanceOf(BindgenException.class)
        .hasMessageContaining("packageName is required");
  }

  @Test
  void shouldValidateRequiredOutputDirectory() {
    BindgenConfig config = BindgenConfig.builder()
        .packageName("com.example")
        .addWitSource(Path.of("src/main/wit"))
        .build();

    assertThatThrownBy(config::validate)
        .isInstanceOf(BindgenException.class)
        .hasMessageContaining("outputDirectory is required");
  }

  @Test
  void shouldValidateAtLeastOneSource() {
    BindgenConfig config = BindgenConfig.builder()
        .packageName("com.example")
        .outputDirectory(Path.of("target/generated"))
        .build();

    assertThatThrownBy(config::validate)
        .isInstanceOf(BindgenException.class)
        .hasMessageContaining("At least one WIT or WASM source must be specified");
  }

  @Test
  void shouldPassValidationWithValidConfig() throws BindgenException {
    BindgenConfig config = BindgenConfig.builder()
        .packageName("com.example")
        .outputDirectory(Path.of("target/generated"))
        .addWitSource(Path.of("api.wit"))
        .build();

    // Should not throw
    config.validate();
  }

  @Test
  void shouldSupportMultipleSources() {
    BindgenConfig config = BindgenConfig.builder()
        .packageName("com.example")
        .outputDirectory(Path.of("target/generated"))
        .witSources(List.of(Path.of("api.wit"), Path.of("types.wit")))
        .wasmSources(List.of(Path.of("module1.wasm"), Path.of("module2.wasm")))
        .build();

    assertThat(config.getWitSources()).hasSize(2);
    assertThat(config.getWasmSources()).hasSize(2);
  }

  @Test
  void shouldSupportInterfacePrefixAndSuffix() {
    BindgenConfig config = BindgenConfig.builder()
        .packageName("com.example")
        .outputDirectory(Path.of("target/generated"))
        .addWitSource(Path.of("api.wit"))
        .interfacePrefix("I")
        .interfaceSuffix("Api")
        .build();

    assertThat(config.getInterfacePrefix()).hasValue("I");
    assertThat(config.getInterfaceSuffix()).hasValue("Api");
  }

  @Test
  void shouldReturnEmptyOptionalForMissingPrefixAndSuffix() {
    BindgenConfig config = BindgenConfig.builder()
        .packageName("com.example")
        .outputDirectory(Path.of("target/generated"))
        .addWitSource(Path.of("api.wit"))
        .build();

    assertThat(config.getInterfacePrefix()).isEmpty();
    assertThat(config.getInterfaceSuffix()).isEmpty();
  }

  @Test
  void shouldImplementEqualsAndHashCode() {
    BindgenConfig config1 = BindgenConfig.builder()
        .packageName("com.example")
        .outputDirectory(Path.of("target/generated"))
        .addWitSource(Path.of("api.wit"))
        .build();

    BindgenConfig config2 = BindgenConfig.builder()
        .packageName("com.example")
        .outputDirectory(Path.of("target/generated"))
        .addWitSource(Path.of("api.wit"))
        .build();

    assertThat(config1).isEqualTo(config2);
    assertThat(config1.hashCode()).isEqualTo(config2.hashCode());
  }

  @Test
  void shouldImplementToString() {
    BindgenConfig config = BindgenConfig.builder()
        .packageName("com.example")
        .outputDirectory(Path.of("target/generated"))
        .addWitSource(Path.of("api.wit"))
        .build();

    String toString = config.toString();
    assertThat(toString).contains("com.example");
    assertThat(toString).contains("MODERN");
  }
}
