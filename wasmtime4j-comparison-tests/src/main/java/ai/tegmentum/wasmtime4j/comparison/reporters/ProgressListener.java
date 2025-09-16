package ai.tegmentum.wasmtime4j.comparison.reporters;

/**
 * Interface for receiving progress updates during long-running comparison operations. Implements
 * the Observer pattern to provide real-time feedback to users.
 *
 * @since 1.0.0
 */
public interface ProgressListener {
  /**
   * Called when an operation starts.
   *
   * @param operationName the name of the operation starting
   * @param totalSteps the total number of steps expected (0 if indeterminate)
   */
  void onOperationStarted(String operationName, int totalSteps);

  /**
   * Called when progress is made on the current operation.
   *
   * @param currentStep the current step number
   * @param stepDescription description of the current step
   */
  void onProgress(int currentStep, String stepDescription);

  /**
   * Called when an operation completes successfully.
   *
   * @param operationName the name of the completed operation
   * @param message optional completion message
   */
  void onOperationCompleted(String operationName, String message);

  /**
   * Called when an operation fails.
   *
   * @param operationName the name of the failed operation
   * @param error the error that occurred
   */
  void onOperationFailed(String operationName, Throwable error);

  /**
   * Called to report intermediate results or status updates.
   *
   * @param message the status message
   * @param details optional additional details
   */
  void onStatusUpdate(String message, String details);
}
