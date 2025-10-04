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

package ai.tegmentum.wasmtime4j;

import java.util.List;
import java.util.Objects;

/**
 * Specification for component pipeline structure.
 *
 * <p>This class defines the stages and connections in a component processing pipeline.
 *
 * @since 1.0.0
 */
public final class ComponentPipelineSpec {

  private final String pipelineName;
  private final List<String> stageNames;
  private final ComponentPipelineConfig config;

  /**
   * Creates a new component pipeline specification.
   *
   * @param pipelineName the pipeline name
   * @param stageNames the ordered list of stage names
   * @param config the pipeline configuration
   */
  public ComponentPipelineSpec(
      final String pipelineName,
      final List<String> stageNames,
      final ComponentPipelineConfig config) {
    this.pipelineName = Objects.requireNonNull(pipelineName, "pipelineName cannot be null");
    this.stageNames = List.copyOf(Objects.requireNonNull(stageNames, "stageNames cannot be null"));
    this.config =
        config != null ? config : new ComponentPipelineConfig();
  }

  /**
   * Gets the pipeline name.
   *
   * @return pipeline name
   */
  public String getPipelineName() {
    return pipelineName;
  }

  /**
   * Gets the ordered list of stage names.
   *
   * @return immutable list of stage names
   */
  public List<String> getStageNames() {
    return stageNames;
  }

  /**
   * Gets the pipeline configuration.
   *
   * @return pipeline configuration
   */
  public ComponentPipelineConfig getConfig() {
    return config;
  }
}
