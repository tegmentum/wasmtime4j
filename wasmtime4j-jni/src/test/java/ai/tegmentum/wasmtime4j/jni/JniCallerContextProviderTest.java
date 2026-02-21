package ai.tegmentum.wasmtime4j.jni;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ai.tegmentum.wasmtime4j.spi.CallerContextProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link JniCallerContextProvider}.
 *
 * <p>Verifies that the provider correctly implements the CallerContextProvider SPI and delegates to
 * JniHostFunction's ThreadLocal-based caller context mechanism.
 */
@DisplayName("JniCallerContextProvider Tests")
class JniCallerContextProviderTest {

  @Test
  @DisplayName("Should implement CallerContextProvider interface")
  void shouldImplementCallerContextProvider() {
    final JniCallerContextProvider provider = new JniCallerContextProvider();
    assertThat(provider)
        .as("JniCallerContextProvider should implement CallerContextProvider")
        .isInstanceOf(CallerContextProvider.class);
  }

  @Test
  @DisplayName("getCurrentCaller() outside callback context should throw UnsupportedOperationException")
  void getCurrentCallerOutsideContextShouldThrow() {
    final JniCallerContextProvider provider = new JniCallerContextProvider();

    assertThatThrownBy(provider::getCurrentCaller)
        .as("getCurrentCaller() should throw when no callback context is active")
        .isInstanceOf(UnsupportedOperationException.class)
        .hasMessageContaining("Caller context not available");
  }

  @Test
  @DisplayName("Constructor should create valid instance")
  void constructorShouldCreateValidInstance() {
    final JniCallerContextProvider provider = new JniCallerContextProvider();
    assertThat(provider).isNotNull();
  }
}
