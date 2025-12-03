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

/**
 * WASI-NN (WebAssembly System Interface for Neural Networks) support.
 *
 * <p>This package provides Java bindings for the WASI-NN machine learning extension. WASI-NN
 * enables WebAssembly modules to perform neural network inference using various ML backends.
 *
 * <h2>Overview</h2>
 *
 * <p>WASI-NN follows a "graph loader" design where pre-trained models are loaded as opaque "graphs"
 * that can be used for inference. The main components are:
 *
 * <ul>
 *   <li>{@link ai.tegmentum.wasmtime4j.wasi.nn.NnContext} - Main entry point for WASI-NN operations
 *   <li>{@link ai.tegmentum.wasmtime4j.wasi.nn.NnGraph} - A loaded ML model
 *   <li>{@link ai.tegmentum.wasmtime4j.wasi.nn.NnGraphExecutionContext} - Context for running
 *       inference
 *   <li>{@link ai.tegmentum.wasmtime4j.wasi.nn.NnTensor} - Input/output tensor data
 * </ul>
 *
 * <h2>Supported Backends</h2>
 *
 * <p>The available ML backends depend on the Wasmtime build configuration:
 *
 * <ul>
 *   <li><strong>OpenVINO</strong> - Intel's inference engine (default on x86)
 *   <li><strong>ONNX Runtime</strong> - Cross-platform inference engine
 *   <li><strong>TensorFlow Lite</strong> - Lightweight TensorFlow runtime
 *   <li><strong>PyTorch</strong> - Experimental PyTorch support
 *   <li><strong>GGML</strong> - Support for LLM models like LLaMA
 * </ul>
 *
 * <h2>Example Usage</h2>
 *
 * <pre>{@code
 * try (WasmRuntime runtime = WasmRuntimeFactory.create();
 *      NnContext nn = runtime.createNnContext()) {
 *
 *     // Check if ONNX backend is available
 *     if (nn.isEncodingSupported(NnGraphEncoding.ONNX)) {
 *
 *         // Load model from file
 *         try (NnGraph graph = nn.loadGraphFromFile(
 *                 Path.of("model.onnx"),
 *                 NnGraphEncoding.ONNX,
 *                 NnExecutionTarget.CPU)) {
 *
 *             // Create execution context
 *             try (NnGraphExecutionContext exec = graph.createExecutionContext()) {
 *
 *                 // Prepare input tensor
 *                 float[] inputData = prepareInput();
 *                 NnTensor input = NnTensor.fromFloatArray(
 *                     new int[]{1, 3, 224, 224}, inputData);
 *
 *                 // Run inference
 *                 List<NnTensor> outputs = exec.computeByIndex(input);
 *
 *                 // Process output
 *                 float[] predictions = outputs.get(0).toFloatArray();
 *             }
 *         }
 *     }
 * }
 * }</pre>
 *
 * <h2>Stability Note</h2>
 *
 * <p>WASI-NN is a Tier 3 (experimental) feature in Wasmtime. The API may change in future versions
 * and may not be available in all Wasmtime builds.
 *
 * @since 1.0.0
 */
package ai.tegmentum.wasmtime4j.wasi.nn;
