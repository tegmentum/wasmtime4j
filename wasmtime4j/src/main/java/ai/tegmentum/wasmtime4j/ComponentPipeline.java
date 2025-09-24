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

import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Component pipeline for data flow composition and processing.
 *
 * <p>A ComponentPipeline represents a chain of components where the output of one component
 * becomes the input of the next, enabling complex data processing workflows with type-safe
 * interface matching between stages.
 *
 * @since 1.0.0
 */
public interface ComponentPipeline extends AutoCloseable {

  /**
   * Gets the pipeline identifier.
   *
   * @return the pipeline ID
   */
  String getId();

  /**
   * Gets the pipeline name.
   *
   * @return the pipeline name
   */
  String getName();

  /**
   * Gets the components in this pipeline in execution order.
   *
   * @return ordered list of pipeline components
   */
  List<ComponentSimple> getComponents();

  /**
   * Gets the pipeline configuration.
   *
   * @return the pipeline configuration
   */
  ComponentPipelineConfig getConfiguration();

  /**
   * Executes the pipeline with the given input data.
   *
   * @param input the input data for the pipeline
   * @return the pipeline output data
   * @throws WasmException if pipeline execution fails
   */
  WasmValue execute(WasmValue input) throws WasmException;

  /**
   * Executes the pipeline asynchronously with the given input data.
   *
   * @param input the input data for the pipeline
   * @return future containing the pipeline output data
   */
  CompletableFuture<WasmValue> executeAsync(WasmValue input);

  /**
   * Executes the pipeline with streaming input data.
   *
   * @param inputStream the streaming input data
   * @return streaming pipeline output
   * @throws WasmException if streaming execution fails
   */
  ComponentPipelineStream executeStream(ComponentPipelineStream inputStream) throws WasmException;

  /**
   * Executes a single stage of the pipeline.
   *
   * @param stageIndex the index of the stage to execute
   * @param input the input data for the stage
   * @return the stage output data
   * @throws WasmException if stage execution fails
   */
  WasmValue executeStage(int stageIndex, WasmValue input) throws WasmException;

  /**
   * Validates the pipeline configuration and component compatibility.
   *
   * @return pipeline validation result
   */
  PipelineValidationResult validate();

  /**
   * Gets the current state of the pipeline.
   *
   * @return the pipeline state
   */
  PipelineState getState();

  /**
   * Starts the pipeline for continuous processing.
   *
   * @throws WasmException if pipeline startup fails
   */
  void start() throws WasmException;

  /**
   * Stops the pipeline and completes any in-flight processing.
   *
   * @throws WasmException if pipeline shutdown fails
   */
  void stop() throws WasmException;

  /**
   * Pauses the pipeline execution.
   *
   * @throws WasmException if pipeline pause fails
   */
  void pause() throws WasmException;

  /**
   * Resumes a paused pipeline.
   *
   * @throws WasmException if pipeline resume fails
   */
  void resume() throws WasmException;

  /**
   * Adds a new stage to the pipeline.
   *
   * @param component the component to add as a new stage
   * @param position the position to insert the stage
   * @throws WasmException if stage addition fails
   */
  void addStage(ComponentSimple component, int position) throws WasmException;

  /**
   * Removes a stage from the pipeline.
   *
   * @param position the position of the stage to remove
   * @throws WasmException if stage removal fails
   */
  void removeStage(int position) throws WasmException;

  /**
   * Replaces a stage in the pipeline.
   *
   * @param position the position of the stage to replace
   * @param newComponent the new component for the stage
   * @throws WasmException if stage replacement fails
   */
  void replaceStage(int position, ComponentSimple newComponent) throws WasmException;

  /**
   * Gets the number of stages in the pipeline.
   *
   * @return the number of stages
   */
  int getStageCount();

  /**
   * Gets performance metrics for the pipeline.
   *
   * @return pipeline performance metrics
   */
  PipelineMetrics getMetrics();

  /**
   * Gets the pipeline execution history.
   *
   * @return list of recent pipeline executions
   */
  List<PipelineExecution> getExecutionHistory();

  /**
   * Sets a pipeline event listener.
   *
   * @param listener the event listener
   */
  void setEventListener(PipelineEventListener listener);

  /**
   * Removes the pipeline event listener.
   */
  void removeEventListener();

  /**
   * Creates a checkpoint of the current pipeline state.
   *
   * @return the checkpoint identifier
   * @throws WasmException if checkpoint creation fails
   */
  String createCheckpoint() throws WasmException;

  /**
   * Restores the pipeline from a checkpoint.
   *
   * @param checkpointId the checkpoint identifier
   * @throws WasmException if checkpoint restoration fails
   */
  void restoreCheckpoint(String checkpointId) throws WasmException;

  /**
   * Gets available checkpoints for this pipeline.
   *
   * @return list of available checkpoint identifiers
   */
  List<String> getAvailableCheckpoints();

  @Override
  void close();

  /**
   * Pipeline execution states.
   */
  enum PipelineState {
    /** Pipeline is not yet initialized */
    UNINITIALIZED,
    /** Pipeline is ready to execute */
    READY,
    /** Pipeline is currently running */
    RUNNING,
    /** Pipeline is paused */
    PAUSED,
    /** Pipeline is stopping */
    STOPPING,
    /** Pipeline is stopped */
    STOPPED,
    /** Pipeline has encountered an error */
    ERROR,
    /** Pipeline is being reconfigured */
    RECONFIGURING
  }

  /**
   * Pipeline event listener interface.
   */
  interface PipelineEventListener {
    /**
     * Called when pipeline state changes.
     *
     * @param oldState the previous state
     * @param newState the new state
     */
    void onStateChange(PipelineState oldState, PipelineState newState);

    /**
     * Called when a pipeline stage completes.
     *
     * @param stageIndex the completed stage index
     * @param executionTime the stage execution time in milliseconds
     */
    void onStageComplete(int stageIndex, long executionTime);

    /**
     * Called when a pipeline execution completes.
     *
     * @param execution the completed execution
     */
    void onExecutionComplete(PipelineExecution execution);

    /**
     * Called when a pipeline error occurs.
     *
     * @param error the error that occurred
     */
    void onError(Exception error);
  }

  /**
   * Pipeline execution record.
   */
  interface PipelineExecution {
    /**
     * Gets the execution identifier.
     *
     * @return the execution ID
     */
    String getId();

    /**
     * Gets the execution start time.
     *
     * @return the start time in milliseconds
     */
    long getStartTime();

    /**
     * Gets the execution end time.
     *
     * @return the end time in milliseconds
     */
    long getEndTime();

    /**
     * Gets the total execution time.
     *
     * @return the execution time in milliseconds
     */
    long getExecutionTime();

    /**
     * Checks if the execution was successful.
     *
     * @return true if successful
     */
    boolean isSuccessful();

    /**
     * Gets the execution error, if any.
     *
     * @return the error, or null if successful
     */
    Exception getError();

    /**
     * Gets stage execution times.
     *
     * @return map of stage index to execution time
     */
    Map<Integer, Long> getStageExecutionTimes();

    /**
     * Gets the input data size.
     *
     * @return the input data size in bytes
     */
    long getInputSize();

    /**
     * Gets the output data size.
     *
     * @return the output data size in bytes
     */
    long getOutputSize();
  }

  /**
   * Pipeline performance metrics.
   */
  interface PipelineMetrics {
    /**
     * Gets the total number of executions.
     *
     * @return the total executions
     */
    long getTotalExecutions();

    /**
     * Gets the number of successful executions.
     *
     * @return the successful executions
     */
    long getSuccessfulExecutions();

    /**
     * Gets the number of failed executions.
     *
     * @return the failed executions
     */
    long getFailedExecutions();

    /**
     * Gets the average execution time.
     *
     * @return the average execution time in milliseconds
     */
    double getAverageExecutionTime();

    /**
     * Gets the minimum execution time.
     *
     * @return the minimum execution time in milliseconds
     */
    long getMinExecutionTime();

    /**
     * Gets the maximum execution time.
     *
     * @return the maximum execution time in milliseconds
     */
    long getMaxExecutionTime();

    /**
     * Gets the average throughput.
     *
     * @return the throughput in executions per second
     */
    double getThroughput();

    /**
     * Gets performance metrics for individual stages.
     *
     * @return map of stage index to stage metrics
     */
    Map<Integer, StageMetrics> getStageMetrics();

    /**
     * Stage-specific performance metrics.
     */
    interface StageMetrics {
      int getStageIndex();
      long getTotalExecutions();
      double getAverageExecutionTime();
      long getMinExecutionTime();
      long getMaxExecutionTime();
      double getErrorRate();
    }
  }

  /**
   * Pipeline validation result.
   */
  interface PipelineValidationResult {
    /**
     * Checks if the pipeline is valid.
     *
     * @return true if valid
     */
    boolean isValid();

    /**
     * Gets validation errors.
     *
     * @return list of validation errors
     */
    List<String> getErrors();

    /**
     * Gets validation warnings.
     *
     * @return list of validation warnings
     */
    List<String> getWarnings();

    /**
     * Gets interface compatibility information.
     *
     * @return interface compatibility details
     */
    Map<Integer, ComponentCompatibilityResult> getInterfaceCompatibility();
  }
}