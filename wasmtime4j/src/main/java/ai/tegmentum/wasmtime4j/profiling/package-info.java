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
 * Profiling infrastructure for WebAssembly runtime performance analysis.
 *
 * <p>This package provides comprehensive profiling capabilities for analyzing the performance of
 * WebAssembly code execution. Key components include:
 *
 * <ul>
 *   <li>{@link ai.tegmentum.wasmtime4j.profiling.AdvancedProfiler} - Main profiler for tracking
 *       function executions and memory allocations
 *   <li>{@link ai.tegmentum.wasmtime4j.profiling.FlameGraphGenerator} - Generates flame graph
 *       visualizations for performance analysis
 *   <li>{@link ai.tegmentum.wasmtime4j.profiling.PerformanceInsights} - Analyzes profiling data to
 *       identify hot spots and generate recommendations
 * </ul>
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * // Create profiler with configuration
 * AdvancedProfiler.ProfilerConfiguration config = AdvancedProfiler.ProfilerConfiguration.builder()
 *     .samplingInterval(Duration.ofMicroseconds(100))
 *     .enableMemoryProfiling(true)
 *     .enableFlameGraphs(true)
 *     .build();
 *
 * try (AdvancedProfiler profiler = new AdvancedProfiler(config)) {
 *     // Start profiling session
 *     AdvancedProfiler.ProfilingSession session = profiler.startProfiling(Duration.ofMinutes(5));
 *
 *     // Profile operations
 *     int result = profiler.profileOperation("compute", () -> expensiveOperation(), "JNI");
 *
 *     // Get statistics and flame graph
 *     AdvancedProfiler.ProfilingStatistics stats = session.getStatistics();
 *     FlameGraphGenerator.FlameFrame flameGraph = session.generateFlameGraph();
 *
 *     // Analyze performance
 *     PerformanceInsights insights = new PerformanceInsights();
 *     PerformanceInsights.PerformanceInsightsResult analysis =
 *         insights.analyzePerformance(flameGraph, stats);
 *
 *     // Review hot spots and recommendations
 *     for (PerformanceInsights.HotSpot hotSpot : analysis.getHotSpots()) {
 *         System.out.println(hotSpot.getFunctionName() + ": " + hotSpot.getTimePercentage() + "%");
 *     }
 * }
 * }</pre>
 *
 * @since 1.0.0
 */
package ai.tegmentum.wasmtime4j.profiling;
