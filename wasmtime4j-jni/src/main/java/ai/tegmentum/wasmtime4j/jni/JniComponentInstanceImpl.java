package ai.tegmentum.wasmtime4j.jni;

import ai.tegmentum.wasmtime4j.Component;
import ai.tegmentum.wasmtime4j.ComponentInstance;
import ai.tegmentum.wasmtime4j.ComponentInstanceConfig;
import ai.tegmentum.wasmtime4j.ComponentInstanceState;
import ai.tegmentum.wasmtime4j.ComponentResourceUsage;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.exception.WitValueException;
import ai.tegmentum.wasmtime4j.jni.util.JniValidation;
import ai.tegmentum.wasmtime4j.wit.WitValue;
import ai.tegmentum.wasmtime4j.wit.WitValueMarshaller;
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
    JniValidation.requireNonNull(nativeInstance, "nativeInstance");
    JniValidation.requireNonNull(component, "component");
    this.nativeInstance = nativeInstance;
    this.component = component;
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
  public void close() {
    if (nativeInstance != null && !nativeInstance.isClosed()) {
      try {
        nativeInstance.close();
      } catch (final Exception e) {
        LOGGER.warning("Error closing component instance: " + e.getMessage());
      }
      LOGGER.fine("Closed component instance: " + instanceId);
    }
  }

  @Override
  public void stop() {
    // TODO: Implement stop functionality
    LOGGER.fine("Stopped component instance: " + instanceId);
  }

  @Override
  public void pause() throws WasmException {
    // TODO: Implement pause functionality
    LOGGER.fine("Pause not yet implemented for instance: " + instanceId);
  }

  public void resume() throws WasmException {
    // TODO: Implement resume functionality
    LOGGER.fine("Resume not yet implemented for instance: " + instanceId);
  }

  @Override
  public void bindInterface(final String interfaceName, final Object implementation)
      throws WasmException {
    // TODO: Implement interface binding
    LOGGER.fine(
        "Interface binding not yet implemented: interface="
            + interfaceName
            + ", instance="
            + instanceId);
  }

  @Override
  public java.util.Map<String, ai.tegmentum.wasmtime4j.WitInterfaceDefinition>
      getExportedInterfaces() {
    // TODO: Implement exported interfaces retrieval
    return java.util.Collections.emptyMap();
  }

  @Override
  public java.util.Set<String> getExportedFunctions() {
    // TODO: Implement exported functions retrieval
    return java.util.Collections.emptySet();
  }

  @Override
  public boolean hasFunction(final String functionName) {
    // TODO: Implement function checking
    return false;
  }

  @Override
  public Object invoke(final String functionName, final Object... args)
      throws ai.tegmentum.wasmtime4j.exception.WasmException {
    JniValidation.requireNonEmpty(functionName, "functionName");
    if (!isValid()) {
      throw new WasmException("Component instance is not valid");
    }

    try {
      // Convert arguments to WitValues - users must pass WitValue instances
      final java.util.List<WitValue> witValues = new java.util.ArrayList<>(args.length);
      for (int i = 0; i < args.length; i++) {
        if (!(args[i] instanceof WitValue)) {
          throw new WasmException(
              "Argument "
                  + i
                  + " must be a WitValue instance, got "
                  + (args[i] == null ? "null" : args[i].getClass().getName()));
        }
        witValues.add((WitValue) args[i]);
      }

      // Marshal all parameters
      final java.util.List<WitValueMarshaller.MarshalledValue> marshalled =
          WitValueMarshaller.marshalAll(witValues);

      // Prepare arrays for JNI call
      final int[] typeDiscriminators = new int[marshalled.size()];
      final byte[][] data = new byte[marshalled.size()][];
      for (int i = 0; i < marshalled.size(); i++) {
        typeDiscriminators[i] = marshalled.get(i).getTypeDiscriminator();
        data[i] = marshalled.get(i).getData();
      }

      // Call native function with engine handle and instance ID
      final Object[] result =
          JniComponent.nativeComponentInvokeFunction(
              component.getEngine().getNativeHandle(),
              nativeInstance.getNativeHandle(),
              functionName,
              typeDiscriminators,
              data);

      // Handle void return (null result)
      if (result == null || result.length == 0) {
        return null;
      }

      // Unmarshal result
      final int resultType = (Integer) result[0];
      final byte[] resultData = (byte[]) result[1];
      final WitValue resultValue = WitValueMarshaller.unmarshal(resultType, resultData);

      // Convert back to Java type
      return resultValue.toJava();

    } catch (final WitValueException e) {
      throw new WasmException("WIT value marshalling failed: " + e.getMessage(), e);
    } catch (final Exception e) {
      throw new WasmException("Function invocation failed: " + e.getMessage(), e);
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
