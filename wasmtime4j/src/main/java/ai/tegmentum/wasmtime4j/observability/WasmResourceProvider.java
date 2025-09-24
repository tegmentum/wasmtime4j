/*
 * Copyright 2024 Tegmentum AI
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ai.tegmentum.wasmtime4j.observability;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.ResourceProvider;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.semconv.ResourceAttributes;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.logging.Logger;

/**
 * Custom OpenTelemetry resource provider for WebAssembly runtime context.
 *
 * <p>This resource provider enriches telemetry data with WebAssembly-specific attributes and
 * runtime information, enabling better observability and correlation in distributed systems.
 *
 * @since 1.0.0
 */
public final class WasmResourceProvider implements ResourceProvider {

  private static final Logger LOGGER = Logger.getLogger(WasmResourceProvider.class.getName());

  /** Service name for wasmtime4j. */
  public static final String SERVICE_NAME = "wasmtime4j";

  /** Service namespace for wasmtime4j. */
  public static final String SERVICE_NAMESPACE = "ai.tegmentum";

  @Override
  public Resource createResource(final ConfigProperties config) {
    try {
      final Attributes.Builder attributesBuilder = Attributes.builder();

      // Basic service information
      attributesBuilder
          .put(ResourceAttributes.SERVICE_NAME,
               config.getString("service.name", SERVICE_NAME))
          .put(ResourceAttributes.SERVICE_NAMESPACE,
               config.getString("service.namespace", SERVICE_NAMESPACE))
          .put(ResourceAttributes.SERVICE_VERSION,
               config.getString("service.version", getServiceVersion()));

      // WebAssembly runtime information
      addWasmRuntimeAttributes(attributesBuilder, config);

      // Environment information
      addEnvironmentAttributes(attributesBuilder, config);

      // Java runtime information
      addJavaRuntimeAttributes(attributesBuilder);

      // Deployment information
      addDeploymentAttributes(attributesBuilder, config);

      final Resource resource = Resource.create(attributesBuilder.build());

      LOGGER.info("Created WebAssembly resource with attributes: " +
                  resource.getAttributes().asMap().keySet());

      return resource;

    } catch (final Exception e) {
      LOGGER.warning("Failed to create WebAssembly resource: " + e.getMessage());
      return Resource.empty();
    }
  }

  /** Adds WebAssembly runtime specific attributes. */
  private void addWasmRuntimeAttributes(final Attributes.Builder builder, final ConfigProperties config) {
    // WebAssembly runtime information
    builder.put("wasm.runtime.name", "wasmtime")
           .put("wasm.runtime.version", getWasmtimeVersion())
           .put("wasm.binding.type", detectBindingType())
           .put("wasm.binding.version", getServiceVersion());

    // Supported WebAssembly features
    builder.put("wasm.features.supported", getSupportedWasmFeatures())
           .put("wasm.wasi.version", "preview1,preview2")
           .put("wasm.component.model", "true");

    // Performance characteristics
    builder.put("wasm.compiler.backend", getCompilerBackend())
           .put("wasm.optimization.default", "speed")
           .put("wasm.memory.64bit", String.valueOf(is64BitSupported()));
  }

  /** Adds environment and deployment attributes. */
  private void addEnvironmentAttributes(final Attributes.Builder builder, final ConfigProperties config) {
    final String environment = config.getString("deployment.environment",
                                                System.getProperty("wasmtime4j.environment", "unknown"));
    builder.put(ResourceAttributes.DEPLOYMENT_ENVIRONMENT, environment);

    // Cloud provider information
    final String cloudProvider = detectCloudProvider();
    if (cloudProvider != null) {
      builder.put(ResourceAttributes.CLOUD_PROVIDER, cloudProvider);
    }

    // Container information
    final String containerId = getContainerId();
    if (containerId != null) {
      builder.put(ResourceAttributes.CONTAINER_ID, containerId);
    }

    // Kubernetes information
    final String k8sNamespace = System.getenv("KUBERNETES_NAMESPACE");
    final String k8sPodName = System.getenv("KUBERNETES_POD_NAME");
    if (k8sNamespace != null) {
      builder.put(ResourceAttributes.K8S_NAMESPACE_NAME, k8sNamespace);
    }
    if (k8sPodName != null) {
      builder.put(ResourceAttributes.K8S_POD_NAME, k8sPodName);
    }
  }

  /** Adds Java runtime attributes. */
  private void addJavaRuntimeAttributes(final Attributes.Builder builder) {
    final RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();

    builder.put(ResourceAttributes.PROCESS_RUNTIME_NAME, runtimeMXBean.getVmName())
           .put(ResourceAttributes.PROCESS_RUNTIME_VERSION, runtimeMXBean.getVmVersion())
           .put(ResourceAttributes.PROCESS_RUNTIME_DESCRIPTION, runtimeMXBean.getVmVendor());

    // Java version compatibility information
    final String javaVersion = System.getProperty("java.version");
    builder.put("process.runtime.java.version", javaVersion);

    // Panama FFI availability
    final boolean panamaAvailable = isPanamaAvailable();
    builder.put("process.runtime.panama.available", String.valueOf(panamaAvailable));

    // JNI availability (always true in Java)
    builder.put("process.runtime.jni.available", "true");
  }

  /** Adds deployment-specific attributes. */
  private void addDeploymentAttributes(final Attributes.Builder builder, final ConfigProperties config) {
    // Application instance information
    final String instanceId = config.getString("service.instance.id",
                                              System.getProperty("wasmtime4j.instance.id",
                                                               generateInstanceId()));
    builder.put(ResourceAttributes.SERVICE_INSTANCE_ID, instanceId);

    // Deployment metadata
    final String deploymentVersion = config.getString("deployment.version",
                                                     System.getProperty("wasmtime4j.deployment.version"));
    if (deploymentVersion != null) {
      builder.put("deployment.version", deploymentVersion);
    }

    final String buildNumber = config.getString("build.number",
                                               System.getProperty("wasmtime4j.build.number"));
    if (buildNumber != null) {
      builder.put("build.number", buildNumber);
    }

    final String gitCommit = config.getString("git.commit",
                                             System.getProperty("wasmtime4j.git.commit"));
    if (gitCommit != null) {
      builder.put("git.commit.sha", gitCommit);
    }
  }

  /** Gets the service version from system properties or manifest. */
  private String getServiceVersion() {
    return System.getProperty("wasmtime4j.version", "1.0.0-SNAPSHOT");
  }

  /** Gets the Wasmtime version. */
  private String getWasmtimeVersion() {
    return System.getProperty("wasmtime4j.wasmtime.version", "36.0.2");
  }

  /** Detects the binding type being used. */
  private String detectBindingType() {
    final String javaVersion = System.getProperty("java.version");
    final boolean isPanama23Plus = isPanamaAvailable() && isJava23OrLater(javaVersion);

    if (isPanama23Plus) {
      return "panama";
    } else {
      return "jni";
    }
  }

  /** Gets supported WebAssembly features. */
  private String getSupportedWasmFeatures() {
    return "bulk-memory,multi-value,reference-types,simd,threads,tail-call,component-model,wasi";
  }

  /** Gets the compiler backend information. */
  private String getCompilerBackend() {
    return "cranelift"; // Wasmtime uses Cranelift
  }

  /** Checks if 64-bit memory is supported. */
  private boolean is64BitSupported() {
    return true; // Modern Wasmtime supports 64-bit memory
  }

  /** Detects cloud provider from environment. */
  private String detectCloudProvider() {
    // AWS detection
    if (System.getenv("AWS_REGION") != null || System.getenv("AWS_EXECUTION_ENV") != null) {
      return "aws";
    }

    // Google Cloud detection
    if (System.getenv("GOOGLE_CLOUD_PROJECT") != null || System.getenv("GCLOUD_PROJECT") != null) {
      return "gcp";
    }

    // Azure detection
    if (System.getenv("AZURE_FUNCTIONS_ENVIRONMENT") != null || System.getenv("AZURE_CLIENT_ID") != null) {
      return "azure";
    }

    return null;
  }

  /** Gets container ID if running in a container. */
  private String getContainerId() {
    // Try to read from cgroup file (Linux containers)
    try {
      final java.nio.file.Path cgroupPath = java.nio.file.Paths.get("/proc/self/cgroup");
      if (java.nio.file.Files.exists(cgroupPath)) {
        final java.util.List<String> lines = java.nio.file.Files.readAllLines(cgroupPath);
        for (final String line : lines) {
          if (line.contains("docker") || line.contains("containerd")) {
            final String[] parts = line.split("/");
            if (parts.length > 0) {
              final String lastPart = parts[parts.length - 1];
              if (lastPart.length() >= 12) { // Docker container ID length
                return lastPart.substring(0, 12);
              }
            }
          }
        }
      }
    } catch (final Exception e) {
      // Ignore errors - not in container or can't detect
    }

    return null;
  }

  /** Generates a unique instance ID. */
  private String generateInstanceId() {
    return java.util.UUID.randomUUID().toString();
  }

  /** Checks if Panama Foreign Function API is available. */
  private boolean isPanamaAvailable() {
    try {
      // Try to access a Panama API class
      Class.forName("java.lang.foreign.MemorySegment");
      return true;
    } catch (final ClassNotFoundException e) {
      return false;
    }
  }

  /** Checks if running on Java 23 or later. */
  private boolean isJava23OrLater(final String javaVersion) {
    try {
      // Parse major version number
      final String[] parts = javaVersion.split("\\.");
      if (parts.length >= 1) {
        final String majorVersionStr = parts[0];
        final int majorVersion = Integer.parseInt(majorVersionStr);
        return majorVersion >= 23;
      }
    } catch (final NumberFormatException e) {
      // Fallback: assume older version if parsing fails
      return false;
    }
    return false;
  }

  @Override
  public int order() {
    return 100; // Higher priority to ensure our attributes are included
  }
}