package ai.tegmentum.wasmtime4j.panama;

import ai.tegmentum.wasmtime4j.WasiContext;
import ai.tegmentum.wasmtime4j.WasiDirectoryPermissions;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.panama.ffi.WasmtimeBindings;
import ai.tegmentum.wasmtime4j.panama.util.PanamaResource;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

/**
 * Panama FFI implementation of the WasiContext interface with Preview 2 async I/O support.
 *
 * <p>This class provides WASI context functionality using Panama FFI for direct native calls
 * without JNI overhead. It supports both WASI Preview 1 and Preview 2 features including async I/O
 * operations, component model integration, and enhanced security.
 *
 * <p>Key async I/O features:
 *
 * <ul>
 *   <li>Non-blocking async operations with CompletableFuture support
 *   <li>Proper cancellation and timeout handling
 *   <li>Resource management through Arena-based cleanup
 *   <li>Integration with Java's async patterns
 * </ul>
 *
 * @since 1.0.0
 */
public final class PanamaWasiContextImpl extends PanamaResource implements WasiContext {

  private static final Logger LOGGER = Logger.getLogger(PanamaWasiContextImpl.class.getName());

  /**
   * Creates a new Panama WASI context with the specified native handle and arena.
   *
   * @param nativeHandle the native WASI context handle
   * @param arena the Arena for memory management
   * @throws IllegalArgumentException if nativeHandle is null or arena is null
   */
  public PanamaWasiContextImpl(MemorySegment nativeHandle, Arena arena) {
    super(nativeHandle, arena);
    if (nativeHandle.address() == 0) {
      throw new IllegalArgumentException("Native handle cannot be null pointer");
    }
  }

  @Override
  public WasiContext setArgv(String[] argv) {
    Objects.requireNonNull(argv, "Command line arguments cannot be null");
    ensureValid();

    try (Arena tempArena = Arena.ofConfined()) {
      var result = WasmtimeBindings.wasi_context_set_argv(tempArena, getNativeHandle(), argv);
      if (result != 0) {
        throw new WasmException("Failed to set command line arguments");
      }
      return this;
    } catch (Exception e) {
      if (e instanceof WasmException) {
        throw e;
      }
      throw new WasmException("Failed to set command line arguments", e);
    }
  }

  @Override
  public WasiContext setEnv(String key, String value) {
    Objects.requireNonNull(key, "Environment variable key cannot be null");
    Objects.requireNonNull(value, "Environment variable value cannot be null");
    ensureValid();

    try (Arena tempArena = Arena.ofConfined()) {
      var result = WasmtimeBindings.wasi_context_set_env(tempArena, getNativeHandle(), key, value);
      if (result != 0) {
        throw new WasmException("Failed to set environment variable");
      }
      return this;
    } catch (Exception e) {
      if (e instanceof WasmException) {
        throw e;
      }
      throw new WasmException("Failed to set environment variable", e);
    }
  }

  @Override
  public WasiContext setEnv(Map<String, String> env) {
    Objects.requireNonNull(env, "Environment variables map cannot be null");
    ensureValid();

    for (Map.Entry<String, String> entry : env.entrySet()) {
      setEnv(entry.getKey(), entry.getValue());
    }
    return this;
  }

  @Override
  public WasiContext inheritEnv() {
    ensureValid();

    try (Arena tempArena = Arena.ofConfined()) {
      var result = WasmtimeBindings.wasi_context_inherit_env(tempArena, getNativeHandle());
      if (result != 0) {
        throw new WasmException("Failed to inherit environment variables");
      }
      return this;
    } catch (Exception e) {
      if (e instanceof WasmException) {
        throw e;
      }
      throw new WasmException("Failed to inherit environment variables", e);
    }
  }

  @Override
  public WasiContext inheritStdio() {
    ensureValid();

    try (Arena tempArena = Arena.ofConfined()) {
      var result = WasmtimeBindings.wasi_context_inherit_stdio(tempArena, getNativeHandle());
      if (result != 0) {
        throw new WasmException("Failed to inherit stdio");
      }
      return this;
    } catch (Exception e) {
      if (e instanceof WasmException) {
        throw e;
      }
      throw new WasmException("Failed to inherit stdio", e);
    }
  }

  @Override
  public WasiContext setStdin(Path path) {
    Objects.requireNonNull(path, "Stdin path cannot be null");
    ensureValid();

    try (Arena tempArena = Arena.ofConfined()) {
      var result =
          WasmtimeBindings.wasi_context_set_stdin(tempArena, getNativeHandle(), path.toString());
      if (result != 0) {
        throw new WasmException("Failed to set stdin path");
      }
      return this;
    } catch (Exception e) {
      if (e instanceof WasmException) {
        throw e;
      }
      throw new WasmException("Failed to set stdin path", e);
    }
  }

  @Override
  public WasiContext setStdout(Path path) {
    Objects.requireNonNull(path, "Stdout path cannot be null");
    ensureValid();

    try (Arena tempArena = Arena.ofConfined()) {
      var result =
          WasmtimeBindings.wasi_context_set_stdout(tempArena, getNativeHandle(), path.toString());
      if (result != 0) {
        throw new WasmException("Failed to set stdout path");
      }
      return this;
    } catch (Exception e) {
      if (e instanceof WasmException) {
        throw e;
      }
      throw new WasmException("Failed to set stdout path", e);
    }
  }

  @Override
  public WasiContext setStderr(Path path) {
    Objects.requireNonNull(path, "Stderr path cannot be null");
    ensureValid();

    try (Arena tempArena = Arena.ofConfined()) {
      var result =
          WasmtimeBindings.wasi_context_set_stderr(tempArena, getNativeHandle(), path.toString());
      if (result != 0) {
        throw new WasmException("Failed to set stderr path");
      }
      return this;
    } catch (Exception e) {
      if (e instanceof WasmException) {
        throw e;
      }
      throw new WasmException("Failed to set stderr path", e);
    }
  }

  @Override
  public WasiContext preopenedDir(Path hostPath, String guestPath) throws WasmException {
    Objects.requireNonNull(hostPath, "Host path cannot be null");
    Objects.requireNonNull(guestPath, "Guest path cannot be null");
    ensureValid();

    try (Arena tempArena = Arena.ofConfined()) {
      var result =
          WasmtimeBindings.wasi_context_preopened_dir(
              tempArena, getNativeHandle(), hostPath.toString(), guestPath);
      if (result != 0) {
        throw new WasmException("Failed to add preopened directory");
      }
      return this;
    } catch (Exception e) {
      if (e instanceof WasmException) {
        throw e;
      }
      throw new WasmException("Failed to add preopened directory", e);
    }
  }

  @Override
  public WasiContext preopenedDirReadOnly(Path hostPath, String guestPath) throws WasmException {
    Objects.requireNonNull(hostPath, "Host path cannot be null");
    Objects.requireNonNull(guestPath, "Guest path cannot be null");
    ensureValid();

    try (Arena tempArena = Arena.ofConfined()) {
      var result =
          WasmtimeBindings.wasi_context_preopened_dir_readonly(
              tempArena, getNativeHandle(), hostPath.toString(), guestPath);
      if (result != 0) {
        throw new WasmException("Failed to add read-only preopened directory");
      }
      return this;
    } catch (Exception e) {
      if (e instanceof WasmException) {
        throw e;
      }
      throw new WasmException("Failed to add read-only preopened directory", e);
    }
  }

  @Override
  public WasiContext setWorkingDirectory(String workingDir) {
    Objects.requireNonNull(workingDir, "Working directory cannot be null");
    ensureValid();

    try (Arena tempArena = Arena.ofConfined()) {
      var result =
          WasmtimeBindings.wasi_context_set_working_directory(
              tempArena, getNativeHandle(), workingDir);
      if (result != 0) {
        throw new WasmException("Failed to set working directory");
      }
      return this;
    } catch (Exception e) {
      if (e instanceof WasmException) {
        throw e;
      }
      throw new WasmException("Failed to set working directory", e);
    }
  }

  @Override
  public WasiContext setNetworkEnabled(boolean enabled) {
    ensureValid();

    try (Arena tempArena = Arena.ofConfined()) {
      var result =
          WasmtimeBindings.wasi_context_set_network_enabled(tempArena, getNativeHandle(), enabled);
      if (result != 0) {
        throw new WasmException("Failed to set network enabled state");
      }
      return this;
    } catch (Exception e) {
      if (e instanceof WasmException) {
        throw e;
      }
      throw new WasmException("Failed to set network enabled state", e);
    }
  }

  @Override
  public WasiContext setMaxOpenFiles(int maxFds) {
    if (maxFds < -1) {
      throw new IllegalArgumentException("Maximum file descriptors must be >= -1");
    }
    ensureValid();

    try (Arena tempArena = Arena.ofConfined()) {
      var result =
          WasmtimeBindings.wasi_context_set_max_open_files(tempArena, getNativeHandle(), maxFds);
      if (result != 0) {
        throw new WasmException("Failed to set maximum open files");
      }
      return this;
    } catch (Exception e) {
      if (e instanceof WasmException) {
        throw e;
      }
      throw new WasmException("Failed to set maximum open files", e);
    }
  }

  // ===== WASI Preview 2 async I/O methods =====

  @Override
  public WasiContext setAsyncIoEnabled(boolean enabled) {
    ensureValid();

    try (Arena tempArena = Arena.ofConfined()) {
      var result =
          WasmtimeBindings.wasi_preview2_set_async_io_enabled(
              tempArena, getNativeHandle(), enabled);
      if (result != 0) {
        throw new WasmException("Failed to set async I/O enabled state");
      }
      return this;
    } catch (Exception e) {
      if (e instanceof WasmException) {
        throw e;
      }
      throw new WasmException("Failed to set async I/O enabled state", e);
    }
  }

  @Override
  public WasiContext setMaxAsyncOperations(int maxOps) {
    if (maxOps < -1) {
      throw new IllegalArgumentException("Maximum async operations must be >= -1");
    }
    ensureValid();

    try (Arena tempArena = Arena.ofConfined()) {
      var result =
          WasmtimeBindings.wasi_preview2_set_max_async_operations(
              tempArena, getNativeHandle(), maxOps);
      if (result != 0) {
        throw new WasmException("Failed to set maximum async operations");
      }
      return this;
    } catch (Exception e) {
      if (e instanceof WasmException) {
        throw e;
      }
      throw new WasmException("Failed to set maximum async operations", e);
    }
  }

  @Override
  public WasiContext setAsyncTimeout(long timeoutMs) {
    if (timeoutMs < -1) {
      throw new IllegalArgumentException("Async timeout must be >= -1");
    }
    ensureValid();

    try (Arena tempArena = Arena.ofConfined()) {
      var result =
          WasmtimeBindings.wasi_preview2_set_async_timeout(tempArena, getNativeHandle(), timeoutMs);
      if (result != 0) {
        throw new WasmException("Failed to set async timeout");
      }
      return this;
    } catch (Exception e) {
      if (e instanceof WasmException) {
        throw e;
      }
      throw new WasmException("Failed to set async timeout", e);
    }
  }

  @Override
  public WasiContext setComponentModelEnabled(boolean enabled) {
    ensureValid();

    try (Arena tempArena = Arena.ofConfined()) {
      var result =
          WasmtimeBindings.wasi_preview2_set_component_model_enabled(
              tempArena, getNativeHandle(), enabled);
      if (result != 0) {
        throw new WasmException("Failed to set component model enabled state");
      }
      return this;
    } catch (Exception e) {
      if (e instanceof WasmException) {
        throw e;
      }
      throw new WasmException("Failed to set component model enabled state", e);
    }
  }

  @Override
  public WasiContext setProcessEnabled(boolean enabled) {
    ensureValid();

    try (Arena tempArena = Arena.ofConfined()) {
      var result =
          WasmtimeBindings.wasi_preview2_set_process_enabled(tempArena, getNativeHandle(), enabled);
      if (result != 0) {
        throw new WasmException("Failed to set process enabled state");
      }
      return this;
    } catch (Exception e) {
      if (e instanceof WasmException) {
        throw e;
      }
      throw new WasmException("Failed to set process enabled state", e);
    }
  }

  @Override
  public WasiContext setFilesystemWorkingDir(Path workingDir) {
    Objects.requireNonNull(workingDir, "Filesystem working directory cannot be null");
    ensureValid();

    try (Arena tempArena = Arena.ofConfined()) {
      var result =
          WasmtimeBindings.wasi_preview2_set_filesystem_working_dir(
              tempArena, getNativeHandle(), workingDir.toString());
      if (result != 0) {
        throw new WasmException("Failed to set filesystem working directory");
      }
      return this;
    } catch (Exception e) {
      if (e instanceof WasmException) {
        throw e;
      }
      throw new WasmException("Failed to set filesystem working directory", e);
    }
  }

  @Override
  public WasiContext preopenedDirWithPermissions(
      Path hostPath, String guestPath, WasiDirectoryPermissions permissions) throws WasmException {
    Objects.requireNonNull(hostPath, "Host path cannot be null");
    Objects.requireNonNull(guestPath, "Guest path cannot be null");
    Objects.requireNonNull(permissions, "Permissions cannot be null");
    ensureValid();

    try (Arena tempArena = Arena.ofConfined()) {
      var result =
          WasmtimeBindings.wasi_preview2_preopened_dir_with_permissions(
              tempArena,
              getNativeHandle(),
              hostPath.toString(),
              guestPath,
              permissions.canRead(),
              permissions.canWrite(),
              permissions.canCreate(),
              permissions.canDelete(),
              permissions.canList(),
              permissions.canTraverse(),
              permissions.canAccessMetadata());
      if (result != 0) {
        throw new WasmException("Failed to add preopened directory with permissions");
      }
      return this;
    } catch (Exception e) {
      if (e instanceof WasmException) {
        throw e;
      }
      throw new WasmException("Failed to add preopened directory with permissions", e);
    }
  }

  // ===== Async I/O support methods =====

  /**
   * Performs an async read operation with proper timeout and cancellation.
   *
   * @param streamId the stream ID to read from
   * @param buffer the buffer to read into
   * @param timeoutMs timeout in milliseconds, or -1 for no timeout
   * @return CompletableFuture that completes with the number of bytes read
   */
  public CompletableFuture<Integer> asyncRead(int streamId, byte[] buffer, long timeoutMs) {
    Objects.requireNonNull(buffer, "Buffer cannot be null");
    ensureValid();

    return CompletableFuture.supplyAsync(
        () -> {
          try (Arena tempArena = Arena.ofConfined()) {
            var operationId =
                WasmtimeBindings.wasi_preview2_stream_read_async(
                    tempArena, getNativeHandle(), streamId, buffer, buffer.length);

            if (operationId < 0) {
              throw new WasmException("Failed to start async read operation");
            }

            // Wait for completion with timeout
            return waitForAsyncOperation(operationId, timeoutMs);
          } catch (Exception e) {
            if (e instanceof RuntimeException) {
              throw e;
            }
            throw new WasmException("Async read operation failed", e);
          }
        });
  }

  /**
   * Performs an async write operation with proper timeout and cancellation.
   *
   * @param streamId the stream ID to write to
   * @param buffer the buffer to write from
   * @param timeoutMs timeout in milliseconds, or -1 for no timeout
   * @return CompletableFuture that completes with the number of bytes written
   */
  public CompletableFuture<Integer> asyncWrite(int streamId, byte[] buffer, long timeoutMs) {
    Objects.requireNonNull(buffer, "Buffer cannot be null");
    ensureValid();

    return CompletableFuture.supplyAsync(
        () -> {
          try (Arena tempArena = Arena.ofConfined()) {
            var operationId =
                WasmtimeBindings.wasi_preview2_stream_write_async(
                    tempArena, getNativeHandle(), streamId, buffer, buffer.length);

            if (operationId < 0) {
              throw new WasmException("Failed to start async write operation");
            }

            // Wait for completion with timeout
            return waitForAsyncOperation(operationId, timeoutMs);
          } catch (Exception e) {
            if (e instanceof RuntimeException) {
              throw e;
            }
            throw new WasmException("Async write operation failed", e);
          }
        });
  }

  /**
   * Waits for an async operation to complete with proper timeout handling.
   *
   * @param operationId the operation ID to wait for
   * @param timeoutMs timeout in milliseconds, or -1 for no timeout
   * @return the operation result (typically bytes transferred)
   * @throws WasmException if the operation fails or times out
   */
  private int waitForAsyncOperation(long operationId, long timeoutMs) throws WasmException {
    try (Arena tempArena = Arena.ofConfined()) {
      long startTime = System.currentTimeMillis();

      while (true) {
        var status =
            WasmtimeBindings.wasi_preview2_get_operation_status(
                tempArena, getNativeHandle(), operationId);

        if (status > 0) {
          // Operation completed successfully
          return (int) status;
        } else if (status < 0) {
          // Operation failed
          throw new WasmException("Async operation failed with status: " + status);
        }

        // Check timeout
        if (timeoutMs > 0) {
          long elapsed = System.currentTimeMillis() - startTime;
          if (elapsed >= timeoutMs) {
            // Cancel the operation
            WasmtimeBindings.wasi_preview2_cancel_operation(
                tempArena, getNativeHandle(), operationId);
            throw new WasmException("Async operation timed out after " + timeoutMs + "ms");
          }
        }

        // Brief sleep to avoid busy waiting
        try {
          Thread.sleep(1);
        } catch (InterruptedException e) {
          // Cancel the operation
          WasmtimeBindings.wasi_preview2_cancel_operation(
              tempArena, getNativeHandle(), operationId);
          Thread.currentThread().interrupt();
          throw new WasmException("Async operation interrupted", e);
        }
      }
    }
  }

  @Override
  protected void doCleanup() {
    try (Arena tempArena = Arena.ofConfined()) {
      WasmtimeBindings.wasi_preview2_context_destroy(tempArena, getNativeHandle());
    } catch (Exception e) {
      LOGGER.warning("Failed to cleanup WASI context: " + e.getMessage());
    }
  }
}
