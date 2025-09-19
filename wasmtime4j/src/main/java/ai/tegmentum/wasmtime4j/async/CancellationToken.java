package ai.tegmentum.wasmtime4j.async;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Provides cancellation support for asynchronous WebAssembly operations.
 *
 * <p>A CancellationToken enables cooperative cancellation of long-running async operations
 * by providing a mechanism for operations to check if cancellation has been requested
 * and respond appropriately. This is essential for responsive applications that need
 * to cancel operations due to user input, timeouts, or changing requirements.
 *
 * <p>Cancellation tokens support both immediate cancellation and scheduled cancellation
 * after a specified timeout period.
 *
 * @since 1.0.0
 */
public interface CancellationToken {

  /**
   * Checks if cancellation has been requested.
   *
   * <p>Operations should periodically check this flag and terminate gracefully
   * if cancellation has been requested.
   *
   * @return true if cancellation has been requested
   */
  boolean isCancelled();

  /**
   * Throws a CancellationException if cancellation has been requested.
   *
   * <p>This is a convenience method for operations that want to throw immediately
   * upon detecting cancellation.
   *
   * @throws CancellationException if cancellation has been requested
   */
  void throwIfCancelled() throws CancellationException;

  /**
   * Registers a callback to be executed when cancellation is requested.
   *
   * <p>The callback will be executed immediately if cancellation has already
   * been requested, or when cancellation is requested in the future.
   *
   * @param callback the callback to execute on cancellation
   * @return a registration handle that can be used to unregister the callback
   * @throws IllegalArgumentException if callback is null
   */
  CancellationRegistration register(final Runnable callback);

  /**
   * Registers a callback with state to be executed when cancellation is requested.
   *
   * <p>This overload allows passing state to the callback function.
   *
   * @param <T> the type of state
   * @param callback the callback to execute on cancellation
   * @param state the state to pass to the callback
   * @return a registration handle that can be used to unregister the callback
   * @throws IllegalArgumentException if callback is null
   */
  <T> CancellationRegistration register(final Consumer<T> callback, final T state);

  /**
   * Gets the reason for cancellation, if available.
   *
   * @return the cancellation reason, or null if not cancelled or no reason provided
   */
  String getCancellationReason();

  /**
   * Gets the timestamp when cancellation was requested.
   *
   * @return the cancellation timestamp, or null if not cancelled
   */
  Instant getCancellationTime();

  /**
   * Gets the source that requested cancellation.
   *
   * @return the cancellation source, or null if not available
   */
  String getCancellationSource();

  /**
   * Checks if this token can be cancelled.
   *
   * <p>Some tokens may be non-cancellable for certain operations that
   * cannot be safely interrupted.
   *
   * @return true if this token can be cancelled
   */
  boolean canBeCancelled();

  /**
   * Creates a CompletableFuture that completes when cancellation is requested.
   *
   * <p>This allows integration with async operations that can be cancelled
   * using CompletableFuture composition.
   *
   * @return a CompletableFuture that completes when cancelled
   */
  CompletableFuture<Void> asFuture();

  /**
   * Creates a new token that combines this token with another token.
   *
   * <p>The combined token will be cancelled if either this token or the
   * other token is cancelled.
   *
   * @param other the other token to combine with
   * @return a combined cancellation token
   * @throws IllegalArgumentException if other is null
   */
  CancellationToken combine(final CancellationToken other);

  /**
   * Creates a new token that will be cancelled after the specified timeout.
   *
   * <p>The returned token will be cancelled if either this token is cancelled
   * or the timeout expires.
   *
   * @param timeout the timeout duration
   * @return a token with timeout-based cancellation
   * @throws IllegalArgumentException if timeout is null or negative
   */
  CancellationToken withTimeout(final Duration timeout);

  /**
   * A registration handle for cancellation callbacks.
   */
  interface CancellationRegistration {
    /**
     * Unregisters the callback.
     *
     * <p>After calling this method, the callback will no longer be executed
     * when cancellation is requested.
     */
    void unregister();

    /**
     * Checks if the callback is still registered.
     *
     * @return true if the callback is still registered
     */
    boolean isRegistered();

    /**
     * Gets the registration timestamp.
     *
     * @return when the callback was registered
     */
    Instant getRegistrationTime();
  }

  /**
   * Exception thrown when an operation is cancelled.
   */
  class CancellationException extends RuntimeException {
    private final String source;
    private final Instant cancellationTime;

    /**
     * Creates a new CancellationException.
     */
    public CancellationException() {
      this("Operation was cancelled", null, Instant.now());
    }

    /**
     * Creates a new CancellationException with a message.
     *
     * @param message the exception message
     */
    public CancellationException(final String message) {
      this(message, null, Instant.now());
    }

    /**
     * Creates a new CancellationException with full details.
     *
     * @param message the exception message
     * @param source the cancellation source
     * @param cancellationTime when cancellation occurred
     */
    public CancellationException(final String message, final String source, final Instant cancellationTime) {
      super(message);
      this.source = source;
      this.cancellationTime = cancellationTime;
    }

    /**
     * Gets the source that requested cancellation.
     *
     * @return the cancellation source
     */
    public String getSource() {
      return source;
    }

    /**
     * Gets when cancellation was requested.
     *
     * @return the cancellation time
     */
    public Instant getCancellationTime() {
      return cancellationTime;
    }
  }

  /**
   * Creates a new cancellation token that is not cancelled.
   *
   * @return a new non-cancelled token
   */
  static CancellationToken none() {
    return new NonCancellableToken();
  }

  /**
   * Creates a new cancellation token source.
   *
   * @return a new cancellation token source
   */
  static CancellationTokenSource create() {
    return new CancellationTokenSourceImpl();
  }

  /**
   * Creates a new cancellation token that will be cancelled after the specified timeout.
   *
   * @param timeout the timeout duration
   * @return a new token with timeout-based cancellation
   * @throws IllegalArgumentException if timeout is null or negative
   */
  static CancellationToken withTimeout(final Duration timeout) {
    return create().withTimeout(timeout);
  }

  /**
   * Creates a cancellation token that is already cancelled.
   *
   * @return a pre-cancelled token
   */
  static CancellationToken cancelled() {
    return CancelledToken.INSTANCE;
  }

  /**
   * Creates a cancellation token that is already cancelled with a reason.
   *
   * @param reason the cancellation reason
   * @return a pre-cancelled token with reason
   */
  static CancellationToken cancelled(final String reason) {
    return new CancelledToken(reason);
  }
}

/**
 * A source for creating and controlling cancellation tokens.
 */
interface CancellationTokenSource {
  /**
   * Gets the token associated with this source.
   *
   * @return the cancellation token
   */
  CancellationToken getToken();

  /**
   * Requests cancellation of the associated token.
   */
  void cancel();

  /**
   * Requests cancellation with a specific reason.
   *
   * @param reason the reason for cancellation
   */
  void cancel(final String reason);

  /**
   * Requests cancellation with a reason and source.
   *
   * @param reason the reason for cancellation
   * @param source the source requesting cancellation
   */
  void cancel(final String reason, final String source);

  /**
   * Schedules cancellation after the specified delay.
   *
   * @param delay the delay before cancellation
   */
  void cancelAfter(final Duration delay);

  /**
   * Schedules cancellation after the specified delay with a reason.
   *
   * @param delay the delay before cancellation
   * @param reason the reason for cancellation
   */
  void cancelAfter(final Duration delay, final String reason);

  /**
   * Creates a new token that will be cancelled after the specified timeout.
   *
   * @param timeout the timeout duration
   * @return a token with timeout-based cancellation
   */
  CancellationToken withTimeout(final Duration timeout);

  /**
   * Closes this source and releases any associated resources.
   */
  void close();
}

/**
 * Non-cancellable token implementation.
 */
final class NonCancellableToken implements CancellationToken {
  @Override
  public boolean isCancelled() {
    return false;
  }

  @Override
  public void throwIfCancelled() throws CancellationException {
    // Never throws
  }

  @Override
  public CancellationRegistration register(final Runnable callback) {
    return new NoOpRegistration();
  }

  @Override
  public <T> CancellationRegistration register(final Consumer<T> callback, final T state) {
    return new NoOpRegistration();
  }

  @Override
  public String getCancellationReason() {
    return null;
  }

  @Override
  public Instant getCancellationTime() {
    return null;
  }

  @Override
  public String getCancellationSource() {
    return null;
  }

  @Override
  public boolean canBeCancelled() {
    return false;
  }

  @Override
  public CompletableFuture<Void> asFuture() {
    return new CompletableFuture<>(); // Never completes
  }

  @Override
  public CancellationToken combine(final CancellationToken other) {
    return other;
  }

  @Override
  public CancellationToken withTimeout(final Duration timeout) {
    return CancellationToken.withTimeout(timeout);
  }

  private static class NoOpRegistration implements CancellationRegistration {
    @Override
    public void unregister() {
      // No-op
    }

    @Override
    public boolean isRegistered() {
      return false;
    }

    @Override
    public Instant getRegistrationTime() {
      return Instant.now();
    }
  }
}

/**
 * Pre-cancelled token implementation.
 */
final class CancelledToken implements CancellationToken {
  static final CancelledToken INSTANCE = new CancelledToken("Operation was cancelled");

  private final String reason;
  private final Instant cancellationTime;

  CancelledToken(final String reason) {
    this.reason = reason;
    this.cancellationTime = Instant.now();
  }

  @Override
  public boolean isCancelled() {
    return true;
  }

  @Override
  public void throwIfCancelled() throws CancellationException {
    throw new CancellationException(reason, null, cancellationTime);
  }

  @Override
  public CancellationRegistration register(final Runnable callback) {
    callback.run(); // Execute immediately
    return new NoOpRegistration();
  }

  @Override
  public <T> CancellationRegistration register(final Consumer<T> callback, final T state) {
    callback.accept(state); // Execute immediately
    return new NoOpRegistration();
  }

  @Override
  public String getCancellationReason() {
    return reason;
  }

  @Override
  public Instant getCancellationTime() {
    return cancellationTime;
  }

  @Override
  public String getCancellationSource() {
    return null;
  }

  @Override
  public boolean canBeCancelled() {
    return true;
  }

  @Override
  public CompletableFuture<Void> asFuture() {
    CompletableFuture<Void> future = new CompletableFuture<>();
    future.completeExceptionally(new CancellationException(reason, null, cancellationTime));
    return future;
  }

  @Override
  public CancellationToken combine(final CancellationToken other) {
    return this; // Already cancelled
  }

  @Override
  public CancellationToken withTimeout(final Duration timeout) {
    return this; // Already cancelled
  }

  private static class NoOpRegistration implements CancellationToken.CancellationRegistration {
    @Override
    public void unregister() {
      // No-op
    }

    @Override
    public boolean isRegistered() {
      return false;
    }

    @Override
    public Instant getRegistrationTime() {
      return Instant.now();
    }
  }
}

/**
 * Implementation of CancellationTokenSource.
 */
final class CancellationTokenSourceImpl implements CancellationTokenSource {
  private volatile boolean cancelled = false;
  private volatile String reason;
  private volatile String source;
  private volatile Instant cancellationTime;
  private final Object lock = new Object();
  private final java.util.List<CallbackRegistration> callbacks = new java.util.ArrayList<>();
  private final CompletableFuture<Void> cancellationFuture = new CompletableFuture<>();

  @Override
  public CancellationToken getToken() {
    return new CancellationTokenImpl();
  }

  @Override
  public void cancel() {
    cancel("Cancellation requested", null);
  }

  @Override
  public void cancel(final String reason) {
    cancel(reason, null);
  }

  @Override
  public void cancel(final String reason, final String source) {
    synchronized (lock) {
      if (cancelled) {
        return;
      }
      this.cancelled = true;
      this.reason = reason;
      this.source = source;
      this.cancellationTime = Instant.now();

      // Execute all callbacks
      for (CallbackRegistration callback : callbacks) {
        try {
          callback.execute();
        } catch (Exception e) {
          // Log but don't propagate exceptions from callbacks
        }
      }

      cancellationFuture.complete(null);
    }
  }

  @Override
  public void cancelAfter(final Duration delay) {
    cancelAfter(delay, "Timeout expired");
  }

  @Override
  public void cancelAfter(final Duration delay, final String reason) {
    CompletableFuture.delayedExecutor(delay.toMillis(), TimeUnit.MILLISECONDS)
        .execute(() -> cancel(reason, "Timeout"));
  }

  @Override
  public CancellationToken withTimeout(final Duration timeout) {
    CancellationTokenSource timeoutSource = new CancellationTokenSourceImpl();
    timeoutSource.cancelAfter(timeout);
    return getToken().combine(timeoutSource.getToken());
  }

  @Override
  public void close() {
    cancel("Token source closed");
  }

  private class CancellationTokenImpl implements CancellationToken {
    @Override
    public boolean isCancelled() {
      return cancelled;
    }

    @Override
    public void throwIfCancelled() throws CancellationException {
      if (cancelled) {
        throw new CancellationException(reason, source, cancellationTime);
      }
    }

    @Override
    public CancellationRegistration register(final Runnable callback) {
      return registerCallback(new RunnableCallback(callback));
    }

    @Override
    public <T> CancellationRegistration register(final Consumer<T> callback, final T state) {
      return registerCallback(new ConsumerCallback<>(callback, state));
    }

    @Override
    public String getCancellationReason() {
      return reason;
    }

    @Override
    public Instant getCancellationTime() {
      return cancellationTime;
    }

    @Override
    public String getCancellationSource() {
      return source;
    }

    @Override
    public boolean canBeCancelled() {
      return true;
    }

    @Override
    public CompletableFuture<Void> asFuture() {
      return cancellationFuture;
    }

    @Override
    public CancellationToken combine(final CancellationToken other) {
      if (other == null) {
        throw new IllegalArgumentException("Other token cannot be null");
      }
      return new CombinedToken(this, other);
    }

    @Override
    public CancellationToken withTimeout(final Duration timeout) {
      CancellationTokenSource timeoutSource = new CancellationTokenSourceImpl();
      timeoutSource.cancelAfter(timeout);
      return combine(timeoutSource.getToken());
    }

    private CancellationRegistration registerCallback(final CallbackRegistration callback) {
      synchronized (lock) {
        if (cancelled) {
          // Execute immediately if already cancelled
          try {
            callback.execute();
          } catch (Exception e) {
            // Log but don't propagate
          }
          return new NoOpRegistration();
        } else {
          callbacks.add(callback);
          return callback;
        }
      }
    }
  }

  private abstract static class CallbackRegistration implements CancellationToken.CancellationRegistration {
    private final Instant registrationTime = Instant.now();
    private volatile boolean registered = true;

    abstract void execute();

    @Override
    public void unregister() {
      registered = false;
    }

    @Override
    public boolean isRegistered() {
      return registered;
    }

    @Override
    public Instant getRegistrationTime() {
      return registrationTime;
    }
  }

  private static class RunnableCallback extends CallbackRegistration {
    private final Runnable callback;

    RunnableCallback(final Runnable callback) {
      this.callback = callback;
    }

    @Override
    void execute() {
      if (isRegistered()) {
        callback.run();
      }
    }
  }

  private static class ConsumerCallback<T> extends CallbackRegistration {
    private final Consumer<T> callback;
    private final T state;

    ConsumerCallback(final Consumer<T> callback, final T state) {
      this.callback = callback;
      this.state = state;
    }

    @Override
    void execute() {
      if (isRegistered()) {
        callback.accept(state);
      }
    }
  }

  private static class NoOpRegistration implements CancellationToken.CancellationRegistration {
    @Override
    public void unregister() {
      // No-op
    }

    @Override
    public boolean isRegistered() {
      return false;
    }

    @Override
    public Instant getRegistrationTime() {
      return Instant.now();
    }
  }
}

/**
 * Combined cancellation token implementation.
 */
final class CombinedToken implements CancellationToken {
  private final CancellationToken token1;
  private final CancellationToken token2;

  CombinedToken(final CancellationToken token1, final CancellationToken token2) {
    this.token1 = token1;
    this.token2 = token2;
  }

  @Override
  public boolean isCancelled() {
    return token1.isCancelled() || token2.isCancelled();
  }

  @Override
  public void throwIfCancelled() throws CancellationException {
    if (token1.isCancelled()) {
      token1.throwIfCancelled();
    }
    if (token2.isCancelled()) {
      token2.throwIfCancelled();
    }
  }

  @Override
  public CancellationRegistration register(final Runnable callback) {
    CancellationRegistration reg1 = token1.register(callback);
    CancellationRegistration reg2 = token2.register(callback);
    return new CombinedRegistration(reg1, reg2);
  }

  @Override
  public <T> CancellationRegistration register(final Consumer<T> callback, final T state) {
    CancellationRegistration reg1 = token1.register(callback, state);
    CancellationRegistration reg2 = token2.register(callback, state);
    return new CombinedRegistration(reg1, reg2);
  }

  @Override
  public String getCancellationReason() {
    if (token1.isCancelled()) {
      return token1.getCancellationReason();
    }
    if (token2.isCancelled()) {
      return token2.getCancellationReason();
    }
    return null;
  }

  @Override
  public Instant getCancellationTime() {
    if (token1.isCancelled()) {
      return token1.getCancellationTime();
    }
    if (token2.isCancelled()) {
      return token2.getCancellationTime();
    }
    return null;
  }

  @Override
  public String getCancellationSource() {
    if (token1.isCancelled()) {
      return token1.getCancellationSource();
    }
    if (token2.isCancelled()) {
      return token2.getCancellationSource();
    }
    return null;
  }

  @Override
  public boolean canBeCancelled() {
    return token1.canBeCancelled() || token2.canBeCancelled();
  }

  @Override
  public CompletableFuture<Void> asFuture() {
    CompletableFuture<Void> future1 = token1.asFuture();
    CompletableFuture<Void> future2 = token2.asFuture();
    return CompletableFuture.anyOf(future1, future2).thenApply(v -> null);
  }

  @Override
  public CancellationToken combine(final CancellationToken other) {
    return new CombinedToken(this, other);
  }

  @Override
  public CancellationToken withTimeout(final Duration timeout) {
    return combine(CancellationToken.withTimeout(timeout));
  }

  private static class CombinedRegistration implements CancellationRegistration {
    private final CancellationRegistration reg1;
    private final CancellationRegistration reg2;

    CombinedRegistration(final CancellationRegistration reg1, final CancellationRegistration reg2) {
      this.reg1 = reg1;
      this.reg2 = reg2;
    }

    @Override
    public void unregister() {
      reg1.unregister();
      reg2.unregister();
    }

    @Override
    public boolean isRegistered() {
      return reg1.isRegistered() || reg2.isRegistered();
    }

    @Override
    public Instant getRegistrationTime() {
      Instant time1 = reg1.getRegistrationTime();
      Instant time2 = reg2.getRegistrationTime();
      return time1.isBefore(time2) ? time1 : time2;
    }
  }
}