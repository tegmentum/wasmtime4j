package ai.tegmentum.wasmtime4j.jni;

import ai.tegmentum.wasmtime4j.Component;
import ai.tegmentum.wasmtime4j.ComponentInstance;
import ai.tegmentum.wasmtime4j.ComponentInstanceConfig;
import ai.tegmentum.wasmtime4j.ComponentInstanceState;
import ai.tegmentum.wasmtime4j.ComponentResourceUsage;
import ai.tegmentum.wasmtime4j.jni.util.JniValidation;
import java.util.logging.Logger;

/**
 * JNI implementation of the ComponentInstance interface.
 *
 * <p>This class wraps a native WebAssembly component instance handle and provides Component Model
 * functionality through JNI calls to the native Wasmtime library.
 *
 * @since 1.0.0
 */
public final class JniComponentInstanceImpl implements ComponentInstance {

  private static final Logger LOGGER = Logger.getLogger(JniComponentInstanceImpl.class.getName());

  private final JniComponent.JniComponentInstanceHandle nativeInstance;
  private final JniComponentImpl component;
  private final ComponentInstanceConfig config;
  private final String instanceId;

  /**
   * Creates a new JNI component instance implementation.
   *
   * @param nativeInstance the native component instance handle
   * @param component the component that created this instance
   * @param config the instance configuration
   */
  public JniComponentInstanceImpl(
      final JniComponent.JniComponentInstanceHandle nativeInstance,
      final JniComponentImpl component,
      final ComponentInstanceConfig config) {
    this.nativeInstance = JniValidation.requireNonNull(nativeInstance, "nativeInstance");
    this.component = JniValidation.requireNonNull(component, "component");
    this.config = config != null ? config : new ComponentInstanceConfig();
    this.instanceId = "jni-instance-" + System.nanoTime();
  }

  @Override
  public String getId() {
    return instanceId;
  }

  @Override
  public Component getComponent() {
    return component;
  }

  @Override
  public ComponentInstanceState getState() {
    if (!isValid()) {
      return ComponentInstanceState.TERMINATED;
    }
    return ComponentInstanceState.ACTIVE;
  }

  @Override
  public ComponentResourceUsage getResourceUsage() {
    return new ComponentResourceUsage(instanceId);
  }

  @Override
  public boolean isValid() {
    return !nativeInstance.isClosed() && nativeInstance.isValid();
  }

  @Override
  public void close() throws Exception {
    if (nativeInstance != null && !nativeInstance.isClosed()) {
      nativeInstance.close();
      LOGGER.fine("Closed component instance: " + instanceId);
    }
  }

  /**
   * Gets the native instance handle.
   *
   * @return the native instance handle
   */
  public JniComponent.JniComponentInstanceHandle getNativeInstance() {
    return nativeInstance;
  }

  /**
   * Gets the instance configuration.
   *
   * @return the instance configuration
   */
  public ComponentInstanceConfig getConfig() {
    return config;
  }
}
