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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Basic implementation of ComponentPipeline for foundational Component Model support.
 *
 * <p>This implementation provides core component pipeline functionality as part of
 * Task #304 to stabilize the Component Model foundation.
 *
 * @since 1.0.0
 */
public final class BasicComponentPipeline implements ComponentPipeline {

  private final String pipelineId;
  private final List<ComponentSimple> stages;
  private final ComponentPipelineSpec specification;

  /**
   * Creates a new basic component pipeline.
   *
   * @param specification the pipeline specification
   */
  public BasicComponentPipeline(final ComponentPipelineSpec specification) {
    this.specification = specification != null ? specification : new ComponentPipelineSpec();
    this.pipelineId = "pipeline-" + System.nanoTime();
    this.stages = new ArrayList<>();
  }

  @Override
  public String getId() {
    return pipelineId;
  }

  @Override
  public List<ComponentSimple> getStages() {
    return Collections.unmodifiableList(stages);
  }

  @Override
  public ComponentPipelineSpec getSpecification() {
    return specification;
  }

  @Override
  public void addStage(final ComponentSimple component) {
    if (component != null) {
      stages.add(component);
    }
  }

  @Override
  public void removeStage(final ComponentSimple component) {
    stages.remove(component);
  }

  @Override
  public boolean isValid() {
    return stages.stream().allMatch(ComponentSimple::isValid);
  }

  @Override
  public void close() {
    stages.forEach(stage -> {
      try {
        stage.close();
      } catch (Exception e) {
        // Log and continue
      }
    });
    stages.clear();
  }

  @Override
  public String toString() {
    return "BasicComponentPipeline{" +
        "pipelineId='" + pipelineId + '\'' +
        ", stageCount=" + stages.size() +
        '}';
  }
}