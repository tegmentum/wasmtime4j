package ai.tegmentum.wasmtime4j.security;

import java.util.Map;

/**
 * Configuration for WebAssembly instance creation.
 *
 * @since 1.0.0
 */
public final class InstanceConfig {

  private final Map<String, Object> parameters;
  private final String moduleSource;
  private final SecurityContext securityContext;

  public InstanceConfig(
      final Map<String, Object> parameters,
      final String moduleSource,
      final SecurityContext securityContext) {
    this.parameters = Map.copyOf(parameters);
    this.moduleSource = moduleSource;
    this.securityContext = securityContext;
  }

  public static InstanceConfig defaultConfig() {
    return new InstanceConfig(Map.of(), "unknown", SecurityContext.defaultContext());
  }

  public Map<String, Object> getParameters() {
    return parameters;
  }

  public String getModuleSource() {
    return moduleSource;
  }

  public SecurityContext getSecurityContext() {
    return securityContext;
  }
}
