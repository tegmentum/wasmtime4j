package ai.tegmentum.wasmtime4j.testsuite;

/** Exception thrown when test suite operations fail. */
public final class TestSuiteException extends Exception {

  public TestSuiteException(final String message) {
    super(message);
  }

  public TestSuiteException(final String message, final Throwable cause) {
    super(message, cause);
  }

  public TestSuiteException(final Throwable cause) {
    super(cause);
  }
}
