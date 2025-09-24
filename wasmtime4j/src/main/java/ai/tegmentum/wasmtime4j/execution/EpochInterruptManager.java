package ai.tegmentum.wasmtime4j.execution;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Advanced epoch-based interruption manager for WebAssembly execution control.
 *
 * <p>The EpochInterruptManager provides sophisticated epoch-based interruption
 * capabilities including multi-level interrupt handling, cooperative and preemptive
 * interruption modes, interrupt recovery, and comprehensive interrupt analytics.
 *
 * @since 1.0.0
 */
public interface EpochInterruptManager {

  /**
   * Sets the epoch deadline for a specific execution context.
   *
   * @param contextId execution context identifier
   * @param deadline number of epoch ticks before interruption
   * @param mode interruption mode (cooperative or preemptive)
   * @throws WasmException if setting deadline fails
   * @throws IllegalArgumentException if parameters are invalid
   */
  void setEpochDeadline(String contextId, long deadline, InterruptMode mode) throws WasmException;

  /**
   * Sets a hierarchical epoch deadline with cascading interruption levels.
   *
   * @param contextId execution context identifier
   * @param levels multi-level deadline configuration
   * @throws WasmException if setting deadlines fails
   * @throws IllegalArgumentException if parameters are invalid
   */
  void setHierarchicalDeadline(String contextId, EpochDeadlineLevels levels) throws WasmException;

  /**
   * Increments the global epoch counter for all managed contexts.
   *
   * @return current epoch count after increment
   * @throws WasmException if epoch increment fails
   */
  long incrementGlobalEpoch() throws WasmException;

  /**
   * Increments the epoch counter for specific execution contexts.
   *
   * @param contextIds set of context identifiers to increment
   * @return map of context IDs to their new epoch counts
   * @throws WasmException if epoch increment fails
   * @throws IllegalArgumentException if contextIds is null or empty
   */
  java.util.Map<String, Long> incrementContextEpochs(Set<String> contextIds) throws WasmException;

  /**
   * Registers an interrupt handler for epoch-based interruptions.
   *
   * @param contextId execution context identifier
   * @param handler interrupt handler to be called on interruption
   * @param priority handler priority level
   * @throws WasmException if handler registration fails
   * @throws IllegalArgumentException if parameters are invalid
   */
  void registerInterruptHandler(String contextId, EpochInterruptHandler handler, InterruptPriority priority) throws WasmException;

  /**
   * Unregisters an interrupt handler for a specific context.
   *
   * @param contextId execution context identifier
   * @param handlerId unique handler identifier returned during registration
   * @throws WasmException if handler unregistration fails
   * @throws IllegalArgumentException if parameters are invalid
   */
  void unregisterInterruptHandler(String contextId, String handlerId) throws WasmException;

  /**
   * Configures interrupt recovery behavior for a context.
   *
   * @param contextId execution context identifier
   * @param recovery recovery configuration
   * @throws WasmException if configuration fails
   * @throws IllegalArgumentException if parameters are invalid
   */
  void configureInterruptRecovery(String contextId, InterruptRecoveryConfig recovery) throws WasmException;

  /**
   * Sets up epoch-based time slicing for cooperative multitasking.
   *
   * @param contextId execution context identifier
   * @param sliceConfig time slicing configuration
   * @throws WasmException if time slicing setup fails
   * @throws IllegalArgumentException if parameters are invalid
   */
  void setupTimeSlicing(String contextId, TimeSlicingConfig sliceConfig) throws WasmException;

  /**
   * Enables or disables cooperative interruption mode for a context.
   *
   * @param contextId execution context identifier
   * @param enabled true to enable cooperative mode, false for preemptive
   * @throws WasmException if mode change fails
   * @throws IllegalArgumentException if contextId is invalid
   */
  void setCooperativeMode(String contextId, boolean enabled) throws WasmException;

  /**
   * Creates an interrupt point that can be safely interrupted.
   *
   * @param contextId execution context identifier
   * @param interruptPoint interrupt point configuration
   * @return unique interrupt point identifier
   * @throws WasmException if interrupt point creation fails
   * @throws IllegalArgumentException if parameters are invalid
   */
  String createInterruptPoint(String contextId, InterruptPointConfig interruptPoint) throws WasmException;

  /**
   * Protects a code section from interruption during atomic operations.
   *
   * @param contextId execution context identifier
   * @param protectionConfig protection scope configuration
   * @return protection scope identifier for later removal
   * @throws WasmException if protection setup fails
   * @throws IllegalArgumentException if parameters are invalid
   */
  String protectFromInterruption(String contextId, InterruptProtectionConfig protectionConfig) throws WasmException;

  /**
   * Removes interrupt protection from a previously protected scope.
   *
   * @param contextId execution context identifier
   * @param protectionId protection scope identifier
   * @throws WasmException if protection removal fails
   * @throws IllegalArgumentException if parameters are invalid
   */
  void removeInterruptProtection(String contextId, String protectionId) throws WasmException;

  /**
   * Coordinates interruption across multiple threads for a context.
   *
   * @param contextId execution context identifier
   * @param coordination multi-threaded coordination configuration
   * @throws WasmException if coordination setup fails
   * @throws IllegalArgumentException if parameters are invalid
   */
  void setupMultiThreadedCoordination(String contextId, InterruptCoordinationConfig coordination) throws WasmException;

  /**
   * Chains interrupt handlers for complex interrupt processing workflows.
   *
   * @param contextId execution context identifier
   * @param handlerChain chain of handlers to execute in sequence
   * @return chain identifier for management and modification
   * @throws WasmException if handler chaining fails
   * @throws IllegalArgumentException if parameters are invalid
   */
  String chainInterruptHandlers(String contextId, InterruptHandlerChain handlerChain) throws WasmException;

  /**
   * Manually triggers an epoch interrupt for testing or emergency situations.
   *
   * @param contextId execution context identifier
   * @param reason reason for manual interruption
   * @param immediate true for immediate interruption, false for next epoch
   * @throws WasmException if manual interrupt fails
   * @throws IllegalArgumentException if parameters are invalid
   */
  void triggerManualInterrupt(String contextId, String reason, boolean immediate) throws WasmException;

  /**
   * Pauses epoch interrupt processing for a context.
   *
   * @param contextId execution context identifier
   * @return pause token for resuming interrupts later
   * @throws WasmException if pause fails
   * @throws IllegalArgumentException if contextId is invalid
   */
  String pauseInterrupts(String contextId) throws WasmException;

  /**
   * Resumes epoch interrupt processing using a pause token.
   *
   * @param contextId execution context identifier
   * @param pauseToken token returned from pauseInterrupts
   * @throws WasmException if resume fails
   * @throws IllegalArgumentException if parameters are invalid
   */
  void resumeInterrupts(String contextId, String pauseToken) throws WasmException;

  /**
   * Gets comprehensive interrupt statistics for a context.
   *
   * @param contextId execution context identifier
   * @return detailed interrupt statistics and analytics
   * @throws WasmException if statistics retrieval fails
   * @throws IllegalArgumentException if contextId is invalid
   */
  InterruptStatistics getInterruptStatistics(String contextId) throws WasmException;

  /**
   * Gets current epoch count for a specific context.
   *
   * @param contextId execution context identifier
   * @return current epoch count
   * @throws WasmException if epoch query fails
   * @throws IllegalArgumentException if contextId is invalid
   */
  long getCurrentEpoch(String contextId) throws WasmException;

  /**
   * Gets remaining epochs until next interrupt deadline.
   *
   * @param contextId execution context identifier
   * @return epochs remaining until interrupt, or -1 if no deadline set
   * @throws WasmException if query fails
   * @throws IllegalArgumentException if contextId is invalid
   */
  long getRemainingEpochs(String contextId) throws WasmException;

  /**
   * Checks if a context is currently in an interrupted state.
   *
   * @param contextId execution context identifier
   * @return true if context is interrupted, false otherwise
   * @throws WasmException if state query fails
   * @throws IllegalArgumentException if contextId is invalid
   */
  boolean isInterrupted(String contextId) throws WasmException;

  /**
   * Estimates time until next interrupt based on current epoch progression.
   *
   * @param contextId execution context identifier
   * @return estimated time until interrupt, or null if unpredictable
   * @throws WasmException if estimation fails
   * @throws IllegalArgumentException if contextId is invalid
   */
  Duration estimateTimeToInterrupt(String contextId) throws WasmException;

  /**
   * Asynchronously waits for the next interrupt in a context.
   *
   * @param contextId execution context identifier
   * @param timeout maximum time to wait for interrupt
   * @return future that completes when interrupt occurs or timeout expires
   * @throws WasmException if async wait setup fails
   * @throws IllegalArgumentException if parameters are invalid
   */
  CompletableFuture<InterruptEvent> waitForNextInterrupt(String contextId, Duration timeout) throws WasmException;

  /**
   * Subscribes to interrupt events for real-time monitoring.
   *
   * @param contextId execution context identifier
   * @param subscriber callback to receive interrupt events
   * @return subscription identifier for later unsubscription
   * @throws WasmException if subscription fails
   * @throws IllegalArgumentException if parameters are invalid
   */
  String subscribeToInterrupts(String contextId, Consumer<InterruptEvent> subscriber) throws WasmException;

  /**
   * Unsubscribes from interrupt event notifications.
   *
   * @param contextId execution context identifier
   * @param subscriptionId subscription identifier from subscribeToInterrupts
   * @throws WasmException if unsubscription fails
   * @throws IllegalArgumentException if parameters are invalid
   */
  void unsubscribeFromInterrupts(String contextId, String subscriptionId) throws WasmException;

  /**
   * Gets all active execution contexts managed by this interrupt manager.
   *
   * @return set of active context identifiers
   */
  Set<String> getActiveContexts();

  /**
   * Gets global interrupt manager statistics across all contexts.
   *
   * @return comprehensive global interrupt statistics
   * @throws WasmException if statistics retrieval fails
   */
  GlobalInterruptStatistics getGlobalStatistics() throws WasmException;

  /**
   * Validates interrupt manager state and configuration consistency.
   *
   * @return validation result with any detected issues
   * @throws WasmException if validation fails
   */
  InterruptValidationResult validate() throws WasmException;

  /**
   * Cleans up interrupt management resources for terminated contexts.
   *
   * @param contextId execution context identifier to clean up
   * @throws WasmException if cleanup fails
   */
  void cleanupContext(String contextId) throws WasmException;

  /**
   * Resets interrupt configuration for a context to defaults.
   *
   * @param contextId execution context identifier
   * @throws WasmException if reset fails
   * @throws IllegalArgumentException if contextId is invalid
   */
  void resetInterruptConfig(String contextId) throws WasmException;
}