package ai.tegmentum.wasmtime4j.debug;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Callback interface for handling debug events during WebAssembly execution.
 *
 * <p>Implement this interface and register it with {@link
 * ai.tegmentum.wasmtime4j.Store#setDebugHandler(DebugHandler)} to receive notifications about debug
 * events such as breakpoints, traps, and exceptions.
 *
 * <p>The debug handler is invoked asynchronously and must return a {@link CompletableFuture} that
 * completes when the handler is done processing the event. The store requires async support to be
 * enabled (via {@code Config.asyncSupport(true)}).
 *
 * <p><strong>Important:</strong> The {@link FrameHandle} instances provided in the frames list are
 * only valid during the handler invocation. They become invalid after the returned future
 * completes.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * store.setDebugHandler((event, frames) -> {
 *     System.out.println("Debug event: " + event);
 *     for (FrameHandle frame : frames) {
 *         System.out.println("  Frame: func=" + frame.getFunctionIndex()
 *             + " pc=" + frame.getPc());
 *     }
 *     return CompletableFuture.completedFuture(null);
 * });
 * }</pre>
 *
 * @since 1.1.0
 */
@FunctionalInterface
public interface DebugHandler {

  /**
   * Handles a debug event.
   *
   * @param event the type of debug event that occurred
   * @param frames the current stack frames (innermost to outermost), valid only during this call
   * @return a future that completes when the handler is done
   */
  CompletableFuture<Void> handle(DebugEvent event, List<FrameHandle> frames);
}
