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

import java.util.logging.Logger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/** Tests for {@link CodeStyle}. */
@DisplayName("CodeStyle Tests")
class CodeStyleTest {

  private static final Logger LOGGER = Logger.getLogger(CodeStyleTest.class.getName());

  @Nested
  @DisplayName("Enum Value Tests")
  class EnumValueTests {

    @Test
    @DisplayName("should have exactly two code styles")
    void shouldHaveExactlyTwoCodeStyles() {
      LOGGER.info("Verifying CodeStyle enum values");

      CodeStyle[] styles = CodeStyle.values();

      assertThat(styles).hasSize(2);
      assertThat(styles).containsExactlyInAnyOrder(CodeStyle.MODERN, CodeStyle.LEGACY);
    }

    @Test
    @DisplayName("should get style by name")
    void shouldGetStyleByName() {
      assertThat(CodeStyle.valueOf("MODERN")).isEqualTo(CodeStyle.MODERN);
      assertThat(CodeStyle.valueOf("LEGACY")).isEqualTo(CodeStyle.LEGACY);
    }
  }

  @Nested
  @DisplayName("Minimum Java Version Tests")
  class MinimumJavaVersionTests {

    @Test
    @DisplayName("MODERN should require Java 17")
    void modernShouldRequireJava17() {
      LOGGER.info("Testing MODERN minimum Java version");

      assertThat(CodeStyle.MODERN.getMinimumJavaVersion()).isEqualTo("17");
    }

    @Test
    @DisplayName("LEGACY should require Java 8")
    void legacyShouldRequireJava8() {
      LOGGER.info("Testing LEGACY minimum Java version");

      assertThat(CodeStyle.LEGACY.getMinimumJavaVersion()).isEqualTo("8");
    }

    @ParameterizedTest
    @EnumSource(CodeStyle.class)
    @DisplayName("all styles should have non-null minimum Java version")
    void allStylesShouldHaveNonNullMinimumJavaVersion(final CodeStyle style) {
      assertThat(style.getMinimumJavaVersion()).isNotNull();
      assertThat(style.getMinimumJavaVersion()).isNotEmpty();
    }
  }

  @Nested
  @DisplayName("Feature Support Tests")
  class FeatureSupportTests {

    @Test
    @DisplayName("MODERN should support records")
    void modernShouldSupportRecords() {
      LOGGER.info("Testing MODERN records support");

      assertThat(CodeStyle.MODERN.supportsRecords()).isTrue();
    }

    @Test
    @DisplayName("LEGACY should not support records")
    void legacyShouldNotSupportRecords() {
      LOGGER.info("Testing LEGACY records support");

      assertThat(CodeStyle.LEGACY.supportsRecords()).isFalse();
    }

    @Test
    @DisplayName("MODERN should support sealed interfaces")
    void modernShouldSupportSealedInterfaces() {
      LOGGER.info("Testing MODERN sealed interfaces support");

      assertThat(CodeStyle.MODERN.supportsSealedInterfaces()).isTrue();
    }

    @Test
    @DisplayName("LEGACY should not support sealed interfaces")
    void legacyShouldNotSupportSealedInterfaces() {
      LOGGER.info("Testing LEGACY sealed interfaces support");

      assertThat(CodeStyle.LEGACY.supportsSealedInterfaces()).isFalse();
    }

    @Test
    @DisplayName("MODERN should not generate builders")
    void modernShouldNotGenerateBuilders() {
      LOGGER.info("Testing MODERN builder generation");

      assertThat(CodeStyle.MODERN.generatesBuilders()).isFalse();
    }

    @Test
    @DisplayName("LEGACY should generate builders")
    void legacyShouldGenerateBuilders() {
      LOGGER.info("Testing LEGACY builder generation");

      assertThat(CodeStyle.LEGACY.generatesBuilders()).isTrue();
    }
  }

  @Nested
  @DisplayName("Feature Consistency Tests")
  class FeatureConsistencyTests {

    @Test
    @DisplayName("MODERN features should be consistent")
    void modernFeaturesShouldBeConsistent() {
      LOGGER.info("Testing MODERN feature consistency");

      CodeStyle modern = CodeStyle.MODERN;

      // Modern style should have all modern features
      assertThat(modern.supportsRecords()).isTrue();
      assertThat(modern.supportsSealedInterfaces()).isTrue();
      // Modern style should not need builders (records are enough)
      assertThat(modern.generatesBuilders()).isFalse();
    }

    @Test
    @DisplayName("LEGACY features should be consistent")
    void legacyFeaturesShouldBeConsistent() {
      LOGGER.info("Testing LEGACY feature consistency");

      CodeStyle legacy = CodeStyle.LEGACY;

      // Legacy style should not have any modern features
      assertThat(legacy.supportsRecords()).isFalse();
      assertThat(legacy.supportsSealedInterfaces()).isFalse();
      // Legacy style needs builders to create immutable objects
      assertThat(legacy.generatesBuilders()).isTrue();
    }
  }

  @Nested
  @DisplayName("ToString Tests")
  class ToStringTests {

    @Test
    @DisplayName("MODERN toString should be 'MODERN'")
    void modernToStringShouldBeModern() {
      assertThat(CodeStyle.MODERN.toString()).isEqualTo("MODERN");
    }

    @Test
    @DisplayName("LEGACY toString should be 'LEGACY'")
    void legacyToStringShouldBeLegacy() {
      assertThat(CodeStyle.LEGACY.toString()).isEqualTo("LEGACY");
    }
  }

  @Nested
  @DisplayName("Name and Ordinal Tests")
  class NameAndOrdinalTests {

    @Test
    @DisplayName("MODERN should have correct name")
    void modernShouldHaveCorrectName() {
      assertThat(CodeStyle.MODERN.name()).isEqualTo("MODERN");
    }

    @Test
    @DisplayName("LEGACY should have correct name")
    void legacyShouldHaveCorrectName() {
      assertThat(CodeStyle.LEGACY.name()).isEqualTo("LEGACY");
    }

    @Test
    @DisplayName("ordinals should be sequential starting from 0")
    void ordinalsShouldBeSequential() {
      CodeStyle[] styles = CodeStyle.values();

      for (int i = 0; i < styles.length; i++) {
        assertThat(styles[i].ordinal()).isEqualTo(i);
      }
    }
  }
}
