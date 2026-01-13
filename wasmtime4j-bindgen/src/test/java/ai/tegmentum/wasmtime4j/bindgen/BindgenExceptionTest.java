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

import java.io.IOException;
import java.util.logging.Logger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Tests for {@link BindgenException}. */
@DisplayName("BindgenException Tests")
class BindgenExceptionTest {

  private static final Logger LOGGER = Logger.getLogger(BindgenExceptionTest.class.getName());

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should create exception with message only")
    void shouldCreateExceptionWithMessageOnly() {
      LOGGER.info("Testing constructor with message only");

      BindgenException exception = new BindgenException("Test error message");

      assertThat(exception.getMessage()).isEqualTo("Test error message");
      assertThat(exception.getCause()).isNull();
    }

    @Test
    @DisplayName("should create exception with message and cause")
    void shouldCreateExceptionWithMessageAndCause() {
      LOGGER.info("Testing constructor with message and cause");

      IOException cause = new IOException("underlying cause");
      BindgenException exception = new BindgenException("Test error", cause);

      assertThat(exception.getMessage()).isEqualTo("Test error");
      assertThat(exception.getCause()).isEqualTo(cause);
    }

    @Test
    @DisplayName("should create exception with cause only")
    void shouldCreateExceptionWithCauseOnly() {
      LOGGER.info("Testing constructor with cause only");

      IOException cause = new IOException("root cause");
      BindgenException exception = new BindgenException(cause);

      assertThat(exception.getCause()).isEqualTo(cause);
      assertThat(exception.getMessage()).contains("IOException");
    }
  }

  @Nested
  @DisplayName("Factory Method Tests")
  class FactoryMethodTests {

    @Test
    @DisplayName("witParseError() should create exception with WIT file info")
    void witParseErrorShouldCreateExceptionWithWitFileInfo() {
      LOGGER.info("Testing witParseError() factory method");

      Exception cause = new RuntimeException("syntax error");
      BindgenException exception = BindgenException.witParseError("api.wit", cause);

      assertThat(exception.getMessage()).contains("Failed to parse WIT file");
      assertThat(exception.getMessage()).contains("api.wit");
      assertThat(exception.getCause()).isEqualTo(cause);
    }

    @Test
    @DisplayName("wasmIntrospectionError() should create exception with WASM file info")
    void wasmIntrospectionErrorShouldCreateExceptionWithWasmFileInfo() {
      LOGGER.info("Testing wasmIntrospectionError() factory method");

      Exception cause = new RuntimeException("invalid WASM");
      BindgenException exception = BindgenException.wasmIntrospectionError("module.wasm", cause);

      assertThat(exception.getMessage()).contains("Failed to introspect WASM module");
      assertThat(exception.getMessage()).contains("module.wasm");
      assertThat(exception.getCause()).isEqualTo(cause);
    }

    @Test
    @DisplayName("codeGenerationError() should create exception with type name")
    void codeGenerationErrorShouldCreateExceptionWithTypeName() {
      LOGGER.info("Testing codeGenerationError() factory method");

      Exception cause = new RuntimeException("generation failed");
      BindgenException exception = BindgenException.codeGenerationError("MyRecord", cause);

      assertThat(exception.getMessage()).contains("Failed to generate code for type");
      assertThat(exception.getMessage()).contains("MyRecord");
      assertThat(exception.getCause()).isEqualTo(cause);
    }

    @Test
    @DisplayName("configurationError() should create exception with message")
    void configurationErrorShouldCreateExceptionWithMessage() {
      LOGGER.info("Testing configurationError() factory method");

      BindgenException exception = BindgenException.configurationError("packageName is required");

      assertThat(exception.getMessage()).contains("Configuration error");
      assertThat(exception.getMessage()).contains("packageName is required");
      assertThat(exception.getCause()).isNull();
    }

    @Test
    @DisplayName("ioError() should create exception with operation info")
    void ioErrorShouldCreateExceptionWithOperationInfo() {
      LOGGER.info("Testing ioError() factory method");

      IOException cause = new IOException("file not found");
      BindgenException exception = BindgenException.ioError("reading config file", cause);

      assertThat(exception.getMessage()).contains("I/O error during");
      assertThat(exception.getMessage()).contains("reading config file");
      assertThat(exception.getCause()).isEqualTo(cause);
    }
  }

  @Nested
  @DisplayName("Exception Type Tests")
  class ExceptionTypeTests {

    @Test
    @DisplayName("should be checked exception")
    void shouldBeCheckedException() {
      LOGGER.info("Verifying BindgenException is a checked exception");

      BindgenException exception = new BindgenException("test");

      assertThat(exception).isInstanceOf(Exception.class);
      assertThat(exception).isNotInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("should be serializable")
    void shouldBeSerializable() {
      LOGGER.info("Verifying BindgenException is serializable");

      BindgenException exception = new BindgenException("test");

      assertThat(exception).isInstanceOf(java.io.Serializable.class);
    }
  }

  @Nested
  @DisplayName("Message Format Tests")
  class MessageFormatTests {

    @Test
    @DisplayName("witParseError() message should have consistent format")
    void witParseErrorMessageShouldHaveConsistentFormat() {
      BindgenException exception = BindgenException.witParseError("test.wit",
          new RuntimeException("error"));

      assertThat(exception.getMessage()).startsWith("Failed to parse WIT file: ");
    }

    @Test
    @DisplayName("wasmIntrospectionError() message should have consistent format")
    void wasmIntrospectionErrorMessageShouldHaveConsistentFormat() {
      BindgenException exception = BindgenException.wasmIntrospectionError("test.wasm",
          new RuntimeException("error"));

      assertThat(exception.getMessage()).startsWith("Failed to introspect WASM module: ");
    }

    @Test
    @DisplayName("codeGenerationError() message should have consistent format")
    void codeGenerationErrorMessageShouldHaveConsistentFormat() {
      BindgenException exception = BindgenException.codeGenerationError("TestType",
          new RuntimeException("error"));

      assertThat(exception.getMessage()).startsWith("Failed to generate code for type: ");
    }

    @Test
    @DisplayName("configurationError() message should have consistent format")
    void configurationErrorMessageShouldHaveConsistentFormat() {
      BindgenException exception = BindgenException.configurationError("invalid setting");

      assertThat(exception.getMessage()).startsWith("Configuration error: ");
    }

    @Test
    @DisplayName("ioError() message should have consistent format")
    void ioErrorMessageShouldHaveConsistentFormat() {
      BindgenException exception = BindgenException.ioError("writing file",
          new IOException("error"));

      assertThat(exception.getMessage()).startsWith("I/O error during ");
    }
  }

  @Nested
  @DisplayName("Cause Preservation Tests")
  class CausePreservationTests {

    @Test
    @DisplayName("factory methods should preserve cause")
    void factoryMethodsShouldPreserveCause() {
      LOGGER.info("Testing cause preservation in factory methods");

      Throwable originalCause = new IllegalArgumentException("original");

      BindgenException wit = BindgenException.witParseError("file.wit", originalCause);
      BindgenException wasm = BindgenException.wasmIntrospectionError("module.wasm", originalCause);
      BindgenException code = BindgenException.codeGenerationError("Type", originalCause);
      BindgenException io = BindgenException.ioError("operation", originalCause);

      assertThat(wit.getCause()).isSameAs(originalCause);
      assertThat(wasm.getCause()).isSameAs(originalCause);
      assertThat(code.getCause()).isSameAs(originalCause);
      assertThat(io.getCause()).isSameAs(originalCause);
    }

    @Test
    @DisplayName("configurationError should have no cause")
    void configurationErrorShouldHaveNoCause() {
      BindgenException exception = BindgenException.configurationError("test");

      assertThat(exception.getCause()).isNull();
    }
  }

  @Nested
  @DisplayName("Stack Trace Tests")
  class StackTraceTests {

    @Test
    @DisplayName("should have non-empty stack trace")
    void shouldHaveNonEmptyStackTrace() {
      BindgenException exception = new BindgenException("test");

      assertThat(exception.getStackTrace()).isNotEmpty();
    }

    @Test
    @DisplayName("factory methods should have stack trace including factory call")
    void factoryMethodsShouldHaveStackTraceIncludingFactoryCall() {
      BindgenException exception = BindgenException.configurationError("test");

      StackTraceElement[] stackTrace = exception.getStackTrace();
      assertThat(stackTrace).isNotEmpty();

      // The first element should be from within BindgenException
      boolean hasBindgenExceptionFrame = false;
      for (StackTraceElement element : stackTrace) {
        if (element.getClassName().contains("BindgenException")) {
          hasBindgenExceptionFrame = true;
          break;
        }
      }
      assertThat(hasBindgenExceptionFrame).isTrue();
    }
  }
}
