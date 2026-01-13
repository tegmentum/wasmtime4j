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

package ai.tegmentum.wasmtime4j.bindgen.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/** Tests for JavaNaming utility. */
class JavaNamingTest {

  @Test
  void shouldConvertKebabCaseToClassName() {
    assertThat(JavaNaming.toClassName("http-request")).isEqualTo("HttpRequest");
    assertThat(JavaNaming.toClassName("my-custom-type")).isEqualTo("MyCustomType");
    assertThat(JavaNaming.toClassName("simple")).isEqualTo("Simple");
  }

  @Test
  void shouldConvertKebabCaseToFieldName() {
    assertThat(JavaNaming.toFieldName("get-value")).isEqualTo("getValue");
    assertThat(JavaNaming.toFieldName("my-field")).isEqualTo("myField");
    assertThat(JavaNaming.toFieldName("simple")).isEqualTo("simple");
  }

  @Test
  void shouldConvertToEnumConstant() {
    assertThat(JavaNaming.toEnumConstant("my-value")).isEqualTo("MY_VALUE");
    assertThat(JavaNaming.toEnumConstant("simple")).isEqualTo("SIMPLE");
    assertThat(JavaNaming.toEnumConstant("http-request")).isEqualTo("HTTP_REQUEST");
  }

  @Test
  void shouldConvertWitPackageToJavaPackage() {
    assertThat(JavaNaming.toPackageName("wasi:http")).isEqualTo("wasi.http");
    assertThat(JavaNaming.toPackageName("my-package:types")).isEqualTo("my_package.types");
    assertThat(JavaNaming.toPackageName("")).isEqualTo("");
  }

  @Test
  void shouldEscapeJavaKeywords() {
    assertThat(JavaNaming.toFieldName("class")).isEqualTo("class_");
    assertThat(JavaNaming.toFieldName("import")).isEqualTo("import_");
    assertThat(JavaNaming.toFieldName("default")).isEqualTo("default_");
    assertThat(JavaNaming.toFieldName("record")).isEqualTo("record_");
  }

  @Test
  void shouldIdentifyKeywords() {
    assertThat(JavaNaming.isKeyword("class")).isTrue();
    assertThat(JavaNaming.isKeyword("void")).isTrue();
    assertThat(JavaNaming.isKeyword("record")).isTrue();
    assertThat(JavaNaming.isKeyword("myField")).isFalse();
  }

  @Test
  void shouldValidateIdentifiers() {
    assertThat(JavaNaming.isValidIdentifier("myField")).isTrue();
    assertThat(JavaNaming.isValidIdentifier("MyClass")).isTrue();
    assertThat(JavaNaming.isValidIdentifier("_private")).isTrue();
    assertThat(JavaNaming.isValidIdentifier("123invalid")).isFalse();
    assertThat(JavaNaming.isValidIdentifier("class")).isFalse(); // keyword
    assertThat(JavaNaming.isValidIdentifier("")).isFalse();
    assertThat(JavaNaming.isValidIdentifier(null)).isFalse();
  }

  @Test
  void shouldHandleSnakeCaseInput() {
    assertThat(JavaNaming.toClassName("my_custom_type")).isEqualTo("MyCustomType");
    assertThat(JavaNaming.toFieldName("get_value")).isEqualTo("getValue");
  }

  @Test
  void shouldHandleMixedCase() {
    assertThat(JavaNaming.toClassName("http-request_type")).isEqualTo("HttpRequestType");
  }

  @Test
  void shouldThrowOnEmptyClassName() {
    assertThatThrownBy(() -> JavaNaming.toClassName(""))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("cannot be empty");
  }

  @Test
  void shouldThrowOnNullClassName() {
    assertThatThrownBy(() -> JavaNaming.toClassName(null)).isInstanceOf(NullPointerException.class);
  }

  @Test
  void shouldHandleLeadingDigits() {
    // Leading digits should be prefixed with underscore
    assertThat(JavaNaming.toClassName("123-type")).isEqualTo("_123Type");
    assertThat(JavaNaming.toFieldName("2nd-value")).isEqualTo("_2ndValue");
  }
}
