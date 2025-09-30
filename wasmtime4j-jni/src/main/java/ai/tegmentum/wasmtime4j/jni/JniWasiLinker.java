package ai.tegmentum.wasmtime4j.jni;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.jni.util.JniResource;
import ai.tegmentum.wasmtime4j.jni.util.JniValidation;
import ai.tegmentum.wasmtime4j.wasi.WasiLinker;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

/**
 * JNI implementation of the WasiLinker interface.
 *
 * <p>This class provides a WASI-specific linker implementation using JNI calls to the native
 * Wasmtime library. It extends the standard linker functionality with WASI-specific capabilities.
 *
 * @since 1.0.0
 */
public final class JniWasiLinker extends JniResource implements WasiLinker {

  private static final Logger LOGGER = Logger.getLogger(JniWasiLinker.class.getName());

  private final Engine engine;
  private final AtomicBoolean closed = new AtomicBoolean(false);

  /**
   * Creates a new JNI WASI linker.
   *
   * @param nativeHandle the native linker handle
   * @param engine the engine this linker belongs to
   */
  public JniWasiLinker(final long nativeHandle, final Engine engine) {
    super(nativeHandle);
    JniValidation.requireNonNull(engine, "engine");
    this.engine = engine;
    LOGGER.fine("Created JNI WASI linker with handle: 0x" + Long.toHexString(nativeHandle));
  }

  @Override
  public Instance instantiate(final Store store, final Module module) throws WasmException {
    JniValidation.requireNonNull(store, "store");
    JniValidation.requireNonNull(module, "module");
    ensureNotClosed();

    try {
      // Get native handles
      final long storeHandle = ((JniStore) store).getNativeHandle();
      final long moduleHandle = ((JniModule) module).getNativeHandle();

      final long instanceHandle = nativeInstantiate(getNativeHandle(), storeHandle, moduleHandle);
      if (instanceHandle == 0) {
        throw new WasmException("Failed to instantiate module with WASI linker");
      }

      return new JniInstance(instanceHandle, module, store);
    } catch (final Exception e) {
      throw new WasmException("Failed to instantiate module with WASI linker", e);
    }
  }

  @Override
  public WasiLinker addDirectoryMapping(final String guestPath, final Path hostPath)
      throws WasmException {
    JniValidation.requireNonEmpty(guestPath, "guestPath");
    JniValidation.requireNonNull(hostPath, "hostPath");
    ensureNotClosed();

    try {
      final int result =
          nativeAddDirectoryMapping(getNativeHandle(), guestPath, hostPath.toString());
      if (result != 0) {
        throw new WasmException(
            "Failed to add directory mapping: " + guestPath + " -> " + hostPath);
      }
      return this;
    } catch (final Exception e) {
      throw new WasmException("Failed to add directory mapping", e);
    }
  }

  @Override
  public WasiLinker setEnvironmentVariables(final Map<String, String> envVars)
      throws WasmException {
    JniValidation.requireNonNull(envVars, "envVars");
    ensureNotClosed();

    try {
      for (final Map.Entry<String, String> entry : envVars.entrySet()) {
        final int result =
            nativeSetEnvironmentVariable(getNativeHandle(), entry.getKey(), entry.getValue());
        if (result != 0) {
          throw new WasmException("Failed to set environment variable: " + entry.getKey());
        }
      }
      return this;
    } catch (final Exception e) {
      throw new WasmException("Failed to set environment variables", e);
    }
  }

  @Override
  public WasiLinker setCommandLineArguments(final List<String> args) throws WasmException {
    JniValidation.requireNonNull(args, "args");
    ensureNotClosed();

    try {
      final String[] argsArray = args.toArray(new String[0]);
      final int result = nativeSetCommandLineArguments(getNativeHandle(), argsArray);
      if (result != 0) {
        throw new WasmException("Failed to set command line arguments");
      }
      return this;
    } catch (final Exception e) {
      throw new WasmException("Failed to set command line arguments", e);
    }
  }

  @Override
  protected void doClose() throws Exception {
    if (closed.compareAndSet(false, true)) {
      nativeDestroyWasiLinker(getNativeHandle());
      LOGGER.fine(
          "Destroyed JNI WASI linker with handle: 0x" + Long.toHexString(getNativeHandle()));
    }
  }

  @Override
  protected String getResourceType() {
    return "WasiLinker";
  }

  private void ensureNotClosed() {
    if (closed.get()) {
      throw new IllegalStateException("WasiLinker has been closed");
    }
  }

  // Native method declarations

  private static native long nativeInstantiate(
      long linkerHandle, long storeHandle, long moduleHandle);

  private static native int nativeAddDirectoryMapping(
      long linkerHandle, String guestPath, String hostPath);

  private static native int nativeSetEnvironmentVariable(
      long linkerHandle, String key, String value);

  private static native int nativeSetCommandLineArguments(long linkerHandle, String[] args);

  private static native void nativeDestroyWasiLinker(long linkerHandle);
}
