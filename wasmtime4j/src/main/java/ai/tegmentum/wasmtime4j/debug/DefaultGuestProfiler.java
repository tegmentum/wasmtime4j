/*
 * Copyright 2025 Tegmentum AI
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

package ai.tegmentum.wasmtime4j.debug;

import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Default implementation of GuestProfiler.
 *
 * <p>This implementation provides basic profiling capabilities using Java-side instrumentation.
 *
 * @since 1.0.0
 */
final class DefaultGuestProfiler implements GuestProfiler {

  private final Store store;
  private final ProfilerConfig config;
  private volatile boolean profiling;
  private Instant startTime;
  private Instant stopTime;
  private long functionCallCount;
  private long instructionCount;
  private int maxStackDepth;
  private final List<ProfileData.FunctionProfile> functionProfiles;
  private final Map<String, Long> customMetrics;

  DefaultGuestProfiler(final Store store) {
    this(store, ProfilerConfig.defaults());
  }

  DefaultGuestProfiler(final Store store, final ProfilerConfig config) {
    this.store = Objects.requireNonNull(store, "store cannot be null");
    this.config = Objects.requireNonNull(config, "config cannot be null");
    this.profiling = false;
    this.functionCallCount = 0;
    this.instructionCount = 0;
    this.maxStackDepth = 0;
    this.functionProfiles = new ArrayList<>();
    this.customMetrics = new HashMap<>();
  }

  @Override
  public void start() throws WasmException {
    if (profiling) {
      throw new IllegalStateException("Profiling is already active");
    }
    profiling = true;
    startTime = Instant.now();
    stopTime = null;
    functionCallCount = 0;
    instructionCount = 0;
    maxStackDepth = 0;
    functionProfiles.clear();
    customMetrics.clear();
  }

  @Override
  public void stop() throws WasmException {
    if (!profiling) {
      throw new IllegalStateException("Profiling is not active");
    }
    profiling = false;
    stopTime = Instant.now();
  }

  @Override
  public boolean isProfiling() {
    return profiling;
  }

  @Override
  public ProfileData getProfileData() {
    if (profiling) {
      throw new IllegalStateException("Profiling must be stopped before getting data");
    }
    Duration duration = startTime != null && stopTime != null
        ? Duration.between(startTime, stopTime)
        : Duration.ZERO;

    return new ProfileData(
        duration,
        functionCallCount,
        instructionCount,
        maxStackDepth,
        new ArrayList<>(functionProfiles),
        new HashMap<>(customMetrics));
  }

  @Override
  public void exportTo(final Path path, final ProfileFormat format) throws WasmException {
    Objects.requireNonNull(path, "path cannot be null");
    Objects.requireNonNull(format, "format cannot be null");

    try (OutputStream os = Files.newOutputStream(path)) {
      exportTo(os, format);
    } catch (IOException e) {
      throw new WasmException("Failed to export profile data to " + path, e);
    }
  }

  @Override
  public void exportTo(final OutputStream outputStream, final ProfileFormat format)
      throws WasmException {
    Objects.requireNonNull(outputStream, "outputStream cannot be null");
    Objects.requireNonNull(format, "format cannot be null");

    ProfileData data = getProfileData();
    String output;

    switch (format) {
      case JSON:
        output = exportAsJson(data);
        break;
      case FLAMEGRAPH:
        output = exportAsFlamegraph(data);
        break;
      case CHROME_TRACE:
        output = exportAsChromeTrace(data);
        break;
      case PPROF:
        output = exportAsPprof(data);
        break;
      default:
        throw new WasmException("Unsupported profile format: " + format);
    }

    try {
      outputStream.write(output.getBytes(StandardCharsets.UTF_8));
    } catch (IOException e) {
      throw new WasmException("Failed to write profile data", e);
    }
  }

  @Override
  public void reset() {
    profiling = false;
    startTime = null;
    stopTime = null;
    functionCallCount = 0;
    instructionCount = 0;
    maxStackDepth = 0;
    functionProfiles.clear();
    customMetrics.clear();
  }

  @Override
  public void close() throws WasmException {
    if (profiling) {
      stop();
    }
    reset();
  }

  private String exportAsJson(final ProfileData data) {
    StringBuilder sb = new StringBuilder();
    sb.append("{\n");
    sb.append("  \"totalDuration\": ").append(data.getTotalDuration().toNanos()).append(",\n");
    sb.append("  \"totalFunctionCalls\": ").append(data.getTotalFunctionCalls()).append(",\n");
    sb.append("  \"totalInstructions\": ").append(data.getTotalInstructions()).append(",\n");
    sb.append("  \"maxStackDepth\": ").append(data.getMaxStackDepth()).append(",\n");
    sb.append("  \"functions\": [\n");

    List<ProfileData.FunctionProfile> profiles = data.getFunctionProfiles();
    for (int i = 0; i < profiles.size(); i++) {
      ProfileData.FunctionProfile fp = profiles.get(i);
      sb.append("    {\n");
      sb.append("      \"name\": \"").append(escapeJson(fp.getFunctionName())).append("\",\n");
      sb.append("      \"index\": ").append(fp.getFunctionIndex()).append(",\n");
      sb.append("      \"callCount\": ").append(fp.getCallCount()).append(",\n");
      sb.append("      \"totalTimeNanos\": ").append(fp.getTotalTime().toNanos()).append(",\n");
      sb.append("      \"selfTimeNanos\": ").append(fp.getSelfTime().toNanos()).append("\n");
      sb.append("    }");
      if (i < profiles.size() - 1) {
        sb.append(",");
      }
      sb.append("\n");
    }

    sb.append("  ]\n");
    sb.append("}\n");
    return sb.toString();
  }

  private String exportAsFlamegraph(final ProfileData data) {
    StringBuilder sb = new StringBuilder();
    for (ProfileData.FunctionProfile fp : data.getFunctionProfiles()) {
      sb.append(fp.getFunctionName()).append(" ")
          .append(fp.getSelfTime().toNanos() / 1000).append("\n");
    }
    return sb.toString();
  }

  private String exportAsChromeTrace(final ProfileData data) {
    StringBuilder sb = new StringBuilder();
    sb.append("{\"traceEvents\":[\n");
    long ts = 0;
    List<ProfileData.FunctionProfile> profiles = data.getFunctionProfiles();
    for (int i = 0; i < profiles.size(); i++) {
      ProfileData.FunctionProfile fp = profiles.get(i);
      sb.append("{\"name\":\"").append(escapeJson(fp.getFunctionName())).append("\",");
      sb.append("\"cat\":\"wasm\",\"ph\":\"X\",\"ts\":").append(ts).append(",");
      sb.append("\"dur\":").append(fp.getTotalTime().toNanos() / 1000).append(",");
      sb.append("\"pid\":1,\"tid\":1}");
      if (i < profiles.size() - 1) {
        sb.append(",");
      }
      sb.append("\n");
      ts += fp.getTotalTime().toNanos() / 1000;
    }
    sb.append("]}\n");
    return sb.toString();
  }

  private String exportAsPprof(final ProfileData data) {
    // Simplified pprof-like text format
    StringBuilder sb = new StringBuilder();
    sb.append("--- profile ---\n");
    sb.append("duration: ").append(data.getTotalDuration().toMillis()).append("ms\n");
    sb.append("samples: ").append(data.getTotalFunctionCalls()).append("\n\n");

    for (ProfileData.FunctionProfile fp : data.getFunctionProfiles()) {
      sb.append(String.format("%8d %8d %s%n",
          fp.getSelfTime().toNanos() / 1000,
          fp.getTotalTime().toNanos() / 1000,
          fp.getFunctionName()));
    }
    return sb.toString();
  }

  private String escapeJson(final String s) {
    if (s == null) {
      return "";
    }
    return s.replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
        .replace("\t", "\\t");
  }
}
