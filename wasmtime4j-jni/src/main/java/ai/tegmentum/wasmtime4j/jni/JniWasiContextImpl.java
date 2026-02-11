package ai.tegmentum.wasmtime4j.jni;

import ai.tegmentum.wasmtime4j.wasi.WasiContext;
import ai.tegmentum.wasmtime4j.wasi.WasiDirectoryPermissions;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.jni.util.JniResource;
import java.nio.file.Path;
import java.util.Map;
import java.util.logging.Logger;

/**
 * JNI implementation of the WasiContext interface with Preview 2 support.
 *
 * <p>This class provides WASI context functionality using JNI bindings to the native Wasmtime WASI
 * implementation. It supports both WASI Preview 1 and Preview 2 features including async I/O,
 * component model integration, and enhanced security.
 *
 * @since 1.0.0
 */
public final class JniWasiContextImpl extends JniResource implements WasiContext {

  private static final Logger LOGGER = Logger.getLogger(JniWasiContextImpl.class.getName());

  /**
   * Creates a new JNI WASI context with the specified native handle.
   *
   * @param nativeHandle the native WASI context handle
   * @throws IllegalArgumentException if nativeHandle is 0
   */
  public JniWasiContextImpl(long nativeHandle) {
    super(nativeHandle);
    if (nativeHandle == 0) {
      throw new IllegalArgumentException("Native handle cannot be 0");
    }
  }

  @Override
  protected String getResourceType() {
    return "WasiContext";
  }

  @Override
  public WasiContext setArgv(String[] argv) {
    if (argv == null) {
      throw new IllegalArgumentException("Command line arguments cannot be null");
    }
    ensureNotClosed();

    try {
      final int result = nativeSetArgv(nativeHandle, argv);
      if (result != 0) {
        throw new RuntimeException("Failed to set command line arguments");
      }
      return this;
    } catch (Exception e) {
      if (e instanceof RuntimeException) {
        throw (RuntimeException) e;
      }
      throw new RuntimeException("Failed to set command line arguments", e);
    }
  }

  @Override
  public WasiContext setEnv(String key, String value) {
    if (key == null) {
      throw new IllegalArgumentException("Environment variable key cannot be null");
    }
    if (value == null) {
      throw new IllegalArgumentException("Environment variable value cannot be null");
    }
    ensureNotClosed();

    try {
      final int result = nativeSetEnv(nativeHandle, key, value);
      if (result != 0) {
        throw new RuntimeException("Failed to set environment variable");
      }
      return this;
    } catch (Exception e) {
      if (e instanceof RuntimeException) {
        throw (RuntimeException) e;
      }
      throw new RuntimeException("Failed to set environment variable", e);
    }
  }

  @Override
  public WasiContext setEnv(Map<String, String> env) {
    if (env == null) {
      throw new IllegalArgumentException("Environment variables map cannot be null");
    }
    ensureNotClosed();

    for (Map.Entry<String, String> entry : env.entrySet()) {
      setEnv(entry.getKey(), entry.getValue());
    }
    return this;
  }

  @Override
  public WasiContext inheritEnv() {
    ensureNotClosed();

    try {
      final int result = nativeInheritEnv(nativeHandle);
      if (result != 0) {
        throw new RuntimeException("Failed to inherit environment variables");
      }
      return this;
    } catch (Exception e) {
      if (e instanceof RuntimeException) {
        throw (RuntimeException) e;
      }
      throw new RuntimeException("Failed to inherit environment variables", e);
    }
  }

  @Override
  public WasiContext inheritStdio() {
    ensureNotClosed();

    try {
      final int result = nativeInheritStdio(nativeHandle);
      if (result != 0) {
        throw new RuntimeException("Failed to inherit stdio");
      }
      return this;
    } catch (Exception e) {
      if (e instanceof RuntimeException) {
        throw (RuntimeException) e;
      }
      throw new RuntimeException("Failed to inherit stdio", e);
    }
  }

  @Override
  public WasiContext setStdin(Path path) {
    if (path == null) {
      throw new IllegalArgumentException("Stdin path cannot be null");
    }
    ensureNotClosed();

    try {
      final int result = nativeSetStdin(nativeHandle, path.toString());
      if (result != 0) {
        throw new RuntimeException("Failed to set stdin path");
      }
      return this;
    } catch (Exception e) {
      if (e instanceof RuntimeException) {
        throw (RuntimeException) e;
      }
      throw new RuntimeException("Failed to set stdin path", e);
    }
  }

  @Override
  public WasiContext setStdinBytes(byte[] data) {
    if (data == null) {
      throw new IllegalArgumentException("Stdin data cannot be null");
    }
    ensureNotClosed();

    try {
      final int result = nativeSetStdinBytes(nativeHandle, data);
      if (result != 0) {
        throw new RuntimeException("Failed to set stdin bytes");
      }
      return this;
    } catch (Exception e) {
      if (e instanceof RuntimeException) {
        throw (RuntimeException) e;
      }
      throw new RuntimeException("Failed to set stdin bytes", e);
    }
  }

  @Override
  public WasiContext setStdout(Path path) {
    if (path == null) {
      throw new IllegalArgumentException("Stdout path cannot be null");
    }
    ensureNotClosed();

    try {
      final int result = nativeSetStdout(nativeHandle, path.toString());
      if (result != 0) {
        throw new RuntimeException("Failed to set stdout path");
      }
      return this;
    } catch (Exception e) {
      if (e instanceof RuntimeException) {
        throw (RuntimeException) e;
      }
      throw new RuntimeException("Failed to set stdout path", e);
    }
  }

  @Override
  public WasiContext setStderr(Path path) {
    if (path == null) {
      throw new IllegalArgumentException("Stderr path cannot be null");
    }
    ensureNotClosed();

    try {
      final int result = nativeSetStderr(nativeHandle, path.toString());
      if (result != 0) {
        throw new RuntimeException("Failed to set stderr path");
      }
      return this;
    } catch (Exception e) {
      if (e instanceof RuntimeException) {
        throw (RuntimeException) e;
      }
      throw new RuntimeException("Failed to set stderr path", e);
    }
  }

  @Override
  public WasiContext preopenedDir(Path hostPath, String guestPath) throws WasmException {
    if (hostPath == null) {
      throw new IllegalArgumentException("Host path cannot be null");
    }
    if (guestPath == null) {
      throw new IllegalArgumentException("Guest path cannot be null");
    }
    ensureNotClosed();

    try {
      final int result = nativePreopenedDir(nativeHandle, hostPath.toString(), guestPath);
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
    if (hostPath == null) {
      throw new IllegalArgumentException("Host path cannot be null");
    }
    if (guestPath == null) {
      throw new IllegalArgumentException("Guest path cannot be null");
    }
    ensureNotClosed();

    try {
      final int result = nativePreopenedDirReadOnly(nativeHandle, hostPath.toString(), guestPath);
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
    if (workingDir == null) {
      throw new IllegalArgumentException("Working directory cannot be null");
    }
    ensureNotClosed();

    try {
      final int result = nativeSetWorkingDirectory(nativeHandle, workingDir);
      if (result != 0) {
        throw new RuntimeException("Failed to set working directory");
      }
      return this;
    } catch (Exception e) {
      if (e instanceof RuntimeException) {
        throw (RuntimeException) e;
      }
      throw new RuntimeException("Failed to set working directory", e);
    }
  }

  @Override
  public WasiContext setNetworkEnabled(boolean enabled) {
    ensureNotClosed();

    try {
      final int result = nativeSetNetworkEnabled(nativeHandle, enabled);
      if (result != 0) {
        throw new RuntimeException("Failed to set network enabled state");
      }
      return this;
    } catch (Exception e) {
      if (e instanceof RuntimeException) {
        throw (RuntimeException) e;
      }
      throw new RuntimeException("Failed to set network enabled state", e);
    }
  }

  @Override
  public WasiContext setMaxOpenFiles(int maxFds) {
    if (maxFds < -1) {
      throw new IllegalArgumentException("Maximum file descriptors must be >= -1");
    }
    ensureNotClosed();

    try {
      final int result = nativeSetMaxOpenFiles(nativeHandle, maxFds);
      if (result != 0) {
        throw new RuntimeException("Failed to set maximum open files");
      }
      return this;
    } catch (Exception e) {
      if (e instanceof RuntimeException) {
        throw (RuntimeException) e;
      }
      throw new RuntimeException("Failed to set maximum open files", e);
    }
  }

  // ===== WASI Preview 2 methods =====

  @Override
  public WasiContext setAsyncIoEnabled(boolean enabled) {
    ensureNotClosed();

    try {
      final int result = nativeSetAsyncIoEnabled(nativeHandle, enabled);
      if (result != 0) {
        throw new RuntimeException("Failed to set async I/O enabled state");
      }
      return this;
    } catch (Exception e) {
      if (e instanceof RuntimeException) {
        throw (RuntimeException) e;
      }
      throw new RuntimeException("Failed to set async I/O enabled state", e);
    }
  }

  @Override
  public WasiContext setMaxAsyncOperations(int maxOps) {
    if (maxOps < -1) {
      throw new IllegalArgumentException("Maximum async operations must be >= -1");
    }
    ensureNotClosed();

    try {
      final int result = nativeSetMaxAsyncOperations(nativeHandle, maxOps);
      if (result != 0) {
        throw new RuntimeException("Failed to set maximum async operations");
      }
      return this;
    } catch (Exception e) {
      if (e instanceof RuntimeException) {
        throw (RuntimeException) e;
      }
      throw new RuntimeException("Failed to set maximum async operations", e);
    }
  }

  @Override
  public WasiContext setAsyncTimeout(long timeoutMs) {
    if (timeoutMs < -1) {
      throw new IllegalArgumentException("Async timeout must be >= -1");
    }
    ensureNotClosed();

    try {
      final int result = nativeSetAsyncTimeout(nativeHandle, timeoutMs);
      if (result != 0) {
        throw new RuntimeException("Failed to set async timeout");
      }
      return this;
    } catch (Exception e) {
      if (e instanceof RuntimeException) {
        throw (RuntimeException) e;
      }
      throw new RuntimeException("Failed to set async timeout", e);
    }
  }

  @Override
  public WasiContext setComponentModelEnabled(boolean enabled) {
    ensureNotClosed();

    try {
      final int result = nativeSetComponentModelEnabled(nativeHandle, enabled);
      if (result != 0) {
        throw new RuntimeException("Failed to set component model enabled state");
      }
      return this;
    } catch (Exception e) {
      if (e instanceof RuntimeException) {
        throw (RuntimeException) e;
      }
      throw new RuntimeException("Failed to set component model enabled state", e);
    }
  }

  @Override
  public WasiContext setProcessEnabled(boolean enabled) {
    ensureNotClosed();

    try {
      final int result = nativeSetProcessEnabled(nativeHandle, enabled);
      if (result != 0) {
        throw new RuntimeException("Failed to set process enabled state");
      }
      return this;
    } catch (Exception e) {
      if (e instanceof RuntimeException) {
        throw (RuntimeException) e;
      }
      throw new RuntimeException("Failed to set process enabled state", e);
    }
  }

  @Override
  public WasiContext setFilesystemWorkingDir(Path workingDir) {
    if (workingDir == null) {
      throw new IllegalArgumentException("Filesystem working directory cannot be null");
    }
    ensureNotClosed();

    try {
      final int result = nativeSetFilesystemWorkingDir(nativeHandle, workingDir.toString());
      if (result != 0) {
        throw new RuntimeException("Failed to set filesystem working directory");
      }
      return this;
    } catch (Exception e) {
      if (e instanceof RuntimeException) {
        throw (RuntimeException) e;
      }
      throw new RuntimeException("Failed to set filesystem working directory", e);
    }
  }

  @Override
  public WasiContext preopenedDirWithPermissions(
      Path hostPath, String guestPath, WasiDirectoryPermissions permissions) throws WasmException {
    if (hostPath == null) {
      throw new IllegalArgumentException("Host path cannot be null");
    }
    if (guestPath == null) {
      throw new IllegalArgumentException("Guest path cannot be null");
    }
    if (permissions == null) {
      throw new IllegalArgumentException("Permissions cannot be null");
    }
    ensureNotClosed();

    try {
      final int result =
          nativePreopenedDirWithPermissions(
              nativeHandle,
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

  // ===== Output Capture methods =====

  @Override
  public WasiContext enableOutputCapture() throws WasmException {
    ensureNotClosed();

    try {
      final int result = nativeEnableOutputCapture(nativeHandle);
      if (result != 0) {
        throw new WasmException("Failed to enable output capture");
      }
      return this;
    } catch (Exception e) {
      if (e instanceof WasmException) {
        throw e;
      }
      throw new WasmException("Failed to enable output capture", e);
    }
  }

  @Override
  public byte[] getStdoutCapture() {
    ensureNotClosed();
    return nativeGetStdoutCapture(nativeHandle);
  }

  @Override
  public byte[] getStderrCapture() {
    ensureNotClosed();
    return nativeGetStderrCapture(nativeHandle);
  }

  @Override
  public boolean hasStdoutCapture() {
    ensureNotClosed();
    final int result = nativeHasStdoutCapture(nativeHandle);
    return result == 1;
  }

  @Override
  public boolean hasStderrCapture() {
    ensureNotClosed();
    final int result = nativeHasStderrCapture(nativeHandle);
    return result == 1;
  }

  @Override
  protected void doClose() throws Exception {
    if (nativeHandle != 0) {
      nativeCleanup(nativeHandle);
    }
  }

  // ===== Native method declarations =====

  private static native int nativeSetArgv(long contextHandle, String[] argv);

  private static native int nativeSetEnv(long contextHandle, String key, String value);

  private static native int nativeInheritEnv(long contextHandle);

  private static native int nativeInheritStdio(long contextHandle);

  private static native int nativeSetStdin(long contextHandle, String path);

  private static native int nativeSetStdinBytes(long contextHandle, byte[] data);

  private static native int nativeSetStdout(long contextHandle, String path);

  private static native int nativeSetStderr(long contextHandle, String path);

  private static native int nativePreopenedDir(
      long contextHandle, String hostPath, String guestPath);

  private static native int nativePreopenedDirReadOnly(
      long contextHandle, String hostPath, String guestPath);

  private static native int nativeSetWorkingDirectory(long contextHandle, String workingDir);

  private static native int nativeSetNetworkEnabled(long contextHandle, boolean enabled);

  private static native int nativeSetMaxOpenFiles(long contextHandle, int maxFds);

  // WASI Preview 2 native methods

  private static native int nativeSetAsyncIoEnabled(long contextHandle, boolean enabled);

  private static native int nativeSetMaxAsyncOperations(long contextHandle, int maxOps);

  private static native int nativeSetAsyncTimeout(long contextHandle, long timeoutMs);

  private static native int nativeSetComponentModelEnabled(long contextHandle, boolean enabled);

  private static native int nativeSetProcessEnabled(long contextHandle, boolean enabled);

  private static native int nativeSetFilesystemWorkingDir(long contextHandle, String workingDir);

  private static native int nativePreopenedDirWithPermissions(
      long contextHandle,
      String hostPath,
      String guestPath,
      boolean canRead,
      boolean canWrite,
      boolean canCreate,
      boolean canDelete,
      boolean canList,
      boolean canTraverse,
      boolean canAccessMetadata);

  private static native void nativeCleanup(long contextHandle);

  // Output capture native methods

  private static native int nativeEnableOutputCapture(long contextHandle);

  private static native byte[] nativeGetStdoutCapture(long contextHandle);

  private static native byte[] nativeGetStderrCapture(long contextHandle);

  private static native int nativeHasStdoutCapture(long contextHandle);

  private static native int nativeHasStderrCapture(long contextHandle);
}
