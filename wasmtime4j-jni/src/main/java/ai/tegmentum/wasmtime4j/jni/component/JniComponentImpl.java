package ai.tegmentum.wasmtime4j.jni.component;

import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.component.Component;
import ai.tegmentum.wasmtime4j.component.ComponentInstance;
import ai.tegmentum.wasmtime4j.component.ComponentLinker;
import ai.tegmentum.wasmtime4j.component.ComponentMetadata;
import ai.tegmentum.wasmtime4j.component.ComponentType;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.jni.JniComponent;
import ai.tegmentum.wasmtime4j.jni.exception.JniException;
import ai.tegmentum.wasmtime4j.jni.exception.JniResourceException;
import ai.tegmentum.wasmtime4j.jni.util.JniResource;
import ai.tegmentum.wasmtime4j.jni.util.JniValidation;
import java.util.logging.Logger;

/**
 * JNI implementation of the Component interface.
 *
 * <p>This class provides a bridge between the high-level Component interface and the low-level JNI
 * component operations. It implements defensive programming practices to prevent JVM crashes and
 * provides comprehensive error handling.
 *
 * <p>The implementation maintains a handle to the native component and ensures proper resource
 * cleanup through the JniResource base class.
 *
 * @since 1.0.0
 */
public final class JniComponentImpl extends JniResource implements Component {

  private static final Logger LOGGER = Logger.getLogger(JniComponentImpl.class.getName());

  private final JniComponent.JniComponentHandle componentHandle;
  private final JniComponentMetadataImpl metadata;
  private final JniComponentTypeImpl componentType;

  /**
   * Creates a new JNI component implementation.
   *
   * @param componentHandle the underlying JNI component handle
   * @throws IllegalArgumentException if componentHandle is null
   * @throws JniResourceException if componentHandle is invalid or closed
   */
  public JniComponentImpl(final JniComponent.JniComponentHandle componentHandle) {
    super(componentHandle != null ? componentHandle.getNativeHandle() : 0);

    JniValidation.requireNonNull(componentHandle, "componentHandle");
    if (componentHandle.isClosed()) {
      throw new JniResourceException("Component handle is closed");
    }

    this.componentHandle = componentHandle;
    this.metadata = new JniComponentMetadataImpl(componentHandle);
    this.componentType = new JniComponentTypeImpl(componentHandle);

    LOGGER.fine("Created JNI component implementation with handle: 0x"
        + Long.toHexString(componentHandle.getNativeHandle()));
  }

  @Override
  public ComponentInstance instantiate(final Store store, final ComponentLinker linker)
      throws WasmException {
    JniValidation.requireNonNull(store, "store");
    JniValidation.requireNonNull(linker, "linker");
    ensureNotClosed();

    try {
      // Cast to JNI implementations to access native handles
      if (!(store instanceof ai.tegmentum.wasmtime4j.jni.JniStore)) {
        throw new WasmException("Store must be a JNI implementation");
      }

      if (!(linker instanceof JniComponentLinkerImpl)) {
        throw new WasmException("Linker must be a JNI implementation");
      }

      final ai.tegmentum.wasmtime4j.jni.JniStore jniStore =
          (ai.tegmentum.wasmtime4j.jni.JniStore) store;
      final JniComponentLinkerImpl jniLinker = (JniComponentLinkerImpl) linker;

      if (jniStore.isClosed()) {
        throw new WasmException("Store is closed");
      }

      if (jniLinker.isClosed()) {
        throw new WasmException("Linker is closed");
      }

      // Call native instantiation method
      final long instanceHandle = nativeInstantiateWithLinker(
          getNativeHandle(),
          jniStore.getNativeHandle(),
          jniLinker.getNativeHandle()
      );

      JniValidation.requireValidHandle(instanceHandle, "instanceHandle");

      final JniComponent.JniComponentInstanceHandle jniInstanceHandle =
          new JniComponent.JniComponentInstanceHandle(instanceHandle);

      return new JniComponentInstanceImpl(jniInstanceHandle, this, store);

    } catch (final Exception e) {
      if (e instanceof WasmException) {
        throw e;
      }
      throw new WasmException("Failed to instantiate component", e);
    }
  }

  @Override
  public ComponentType getType() throws WasmException {
    ensureNotClosed();
    return componentType;
  }

  @Override
  public byte[] serialize() throws WasmException {
    ensureNotClosed();

    try {
      final byte[] serializedBytes = nativeSerializeComponent(getNativeHandle());
      if (serializedBytes == null || serializedBytes.length == 0) {
        throw new WasmException("Failed to serialize component - empty result");
      }
      return JniValidation.defensiveCopy(serializedBytes);
    } catch (final Exception e) {
      if (e instanceof WasmException) {
        throw e;
      }
      throw new WasmException("Failed to serialize component", e);
    }
  }

  @Override
  public void validate() throws WasmException {
    ensureNotClosed();

    try {
      final boolean isValid = nativeValidateComponent(getNativeHandle());
      if (!isValid) {
        throw new WasmException("Component validation failed");
      }
    } catch (final Exception e) {
      if (e instanceof WasmException) {
        throw e;
      }
      throw new WasmException("Failed to validate component", e);
    }
  }

  @Override
  public ComponentMetadata getMetadata() {
    return metadata;
  }

  @Override
  public boolean isValid() {
    return !isClosed() && componentHandle != null && componentHandle.isValid();
  }

  @Override
  protected void doClose() throws Exception {
    if (componentHandle != null && !componentHandle.isClosed()) {
      componentHandle.close();
      LOGGER.fine("Closed JNI component implementation with handle: 0x"
          + Long.toHexString(getNativeHandle()));
    }
  }

  @Override
  protected String getResourceType() {
    return "ComponentImpl";
  }

  /**
   * Gets the underlying JNI component handle.
   *
   * @return the JNI component handle
   */
  public JniComponent.JniComponentHandle getComponentHandle() {
    return componentHandle;
  }

  // Native method declarations

  /**
   * Instantiates a component with a store and linker.
   *
   * @param componentHandle the native component handle
   * @param storeHandle the native store handle
   * @param linkerHandle the native linker handle
   * @return native component instance handle or 0 on failure
   */
  private static native long nativeInstantiateWithLinker(
      long componentHandle, long storeHandle, long linkerHandle);

  /**
   * Serializes a component to its binary representation.
   *
   * @param componentHandle the native component handle
   * @return serialized component bytes or null on failure
   */
  private static native byte[] nativeSerializeComponent(long componentHandle);

  /**
   * Validates a component for correctness.
   *
   * @param componentHandle the native component handle
   * @return true if valid, false otherwise
   */
  private static native boolean nativeValidateComponent(long componentHandle);

  /**
   * Gets component type information.
   *
   * @param componentHandle the native component handle
   * @return native component type handle or 0 on failure
   */
  static native long nativeGetComponentType(long componentHandle);

  /**
   * Gets the number of exports in the component.
   *
   * @param componentHandle the native component handle
   * @return number of exports
   */
  static native int nativeGetExportCount(long componentHandle);

  /**
   * Gets the number of imports in the component.
   *
   * @param componentHandle the native component handle
   * @return number of imports
   */
  static native int nativeGetImportCount(long componentHandle);

  /**
   * Gets export names from the component.
   *
   * @param componentHandle the native component handle
   * @return array of export names or null on failure
   */
  static native String[] nativeGetExportNames(long componentHandle);

  /**
   * Gets import names from the component.
   *
   * @param componentHandle the native component handle
   * @return array of import names or null on failure
   */
  static native String[] nativeGetImportNames(long componentHandle);

  /**
   * Gets component metadata information.
   *
   * @param componentHandle the native component handle
   * @return metadata structure or null on failure
   */
  static native JniComponentMetadataImpl.MetadataStruct nativeGetComponentMetadata(
      long componentHandle);
}