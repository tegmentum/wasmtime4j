package ai.tegmentum.wasmtime4j.jni.component;

import ai.tegmentum.wasmtime4j.component.ComponentType;
import ai.tegmentum.wasmtime4j.jni.JniComponent;
import ai.tegmentum.wasmtime4j.jni.util.JniValidation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

/**
 * JNI implementation of ComponentType interface.
 *
 * <p>This class provides component type information by making JNI calls to retrieve type data
 * from the native Wasmtime component representation.
 *
 * @since 1.0.0
 */
public final class JniComponentTypeImpl implements ComponentType {

  private static final Logger LOGGER = Logger.getLogger(JniComponentTypeImpl.class.getName());

  private final JniComponent.JniComponentHandle componentHandle;
  private volatile List<String> exportNames;
  private volatile List<String> importNames;

  /**
   * Creates a new JNI component type implementation.
   *
   * @param componentHandle the component handle to get type information for
   * @throws IllegalArgumentException if componentHandle is null
   */
  public JniComponentTypeImpl(final JniComponent.JniComponentHandle componentHandle) {
    JniValidation.requireNonNull(componentHandle, "componentHandle");
    this.componentHandle = componentHandle;
  }

  @Override
  public List<String> getExportNames() {
    if (exportNames == null) {
      synchronized (this) {
        if (exportNames == null) {
          exportNames = loadExportNames();
        }
      }
    }
    return exportNames;
  }

  @Override
  public List<String> getImportNames() {
    if (importNames == null) {
      synchronized (this) {
        if (importNames == null) {
          importNames = loadImportNames();
        }
      }
    }
    return importNames;
  }

  @Override
  public String getName() {
    try {
      if (componentHandle.isClosed()) {
        return "unknown";
      }

      final String name = nativeGetComponentName(componentHandle.getNativeHandle());
      return name != null ? name : "anonymous";

    } catch (final Exception e) {
      LOGGER.warning("Failed to get component name: " + e.getMessage());
      return "unknown";
    }
  }

  @Override
  public String getVersion() {
    try {
      if (componentHandle.isClosed()) {
        return "0.0.0";
      }

      final String version = nativeGetComponentVersion(componentHandle.getNativeHandle());
      return version != null ? version : "0.0.0";

    } catch (final Exception e) {
      LOGGER.warning("Failed to get component version: " + e.getMessage());
      return "0.0.0";
    }
  }

  @Override
  public boolean hasExport(final String name) {
    JniValidation.requireNonEmpty(name, "name");
    return getExportNames().contains(name);
  }

  @Override
  public boolean hasImport(final String name) {
    JniValidation.requireNonEmpty(name, "name");
    return getImportNames().contains(name);
  }

  /**
   * Loads export names from the native component.
   *
   * @return list of export names
   */
  private List<String> loadExportNames() {
    try {
      if (componentHandle.isClosed()) {
        return Collections.emptyList();
      }

      final String[] names = JniComponentImpl.nativeGetExportNames(
          componentHandle.getNativeHandle());

      if (names == null || names.length == 0) {
        return Collections.emptyList();
      }

      final List<String> nameList = new ArrayList<>();
      for (final String name : names) {
        if (name != null && !name.trim().isEmpty()) {
          nameList.add(name);
        }
      }

      return Collections.unmodifiableList(nameList);

    } catch (final Exception e) {
      LOGGER.warning("Failed to load export names: " + e.getMessage());
      return Collections.emptyList();
    }
  }

  /**
   * Loads import names from the native component.
   *
   * @return list of import names
   */
  private List<String> loadImportNames() {
    try {
      if (componentHandle.isClosed()) {
        return Collections.emptyList();
      }

      final String[] names = JniComponentImpl.nativeGetImportNames(
          componentHandle.getNativeHandle());

      if (names == null || names.length == 0) {
        return Collections.emptyList();
      }

      final List<String> nameList = new ArrayList<>();
      for (final String name : names) {
        if (name != null && !name.trim().isEmpty()) {
          nameList.add(name);
        }
      }

      return Collections.unmodifiableList(nameList);

    } catch (final Exception e) {
      LOGGER.warning("Failed to load import names: " + e.getMessage());
      return Collections.emptyList();
    }
  }

  // Native method declarations

  /**
   * Gets the component name.
   *
   * @param componentHandle the native component handle
   * @return component name or null if not available
   */
  private static native String nativeGetComponentName(long componentHandle);

  /**
   * Gets the component version.
   *
   * @param componentHandle the native component handle
   * @return component version or null if not available
   */
  private static native String nativeGetComponentVersion(long componentHandle);
}