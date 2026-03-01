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
package ai.tegmentum.wasmtime4j.wasi.cli;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * WASI Preview 2 environment interface.
 *
 * <p>Provides access to POSIX-style environment variables and command-line arguments for CLI
 * programs. This interface corresponds to the wasi:cli/environment from the WASI Preview 2
 * specification.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * WasiEnvironment env = getEnvironment();
 *
 * // Get all environment variables
 * Map<String, String> vars = env.getEnvironmentVariables();
 * String path = vars.get("PATH");
 *
 * // Get command-line arguments
 * List<String> args = env.getArguments();
 * System.out.println("Program: " + args.get(0));
 *
 * // Get single variable
 * Optional<String> home = env.getVariable("HOME");
 * }</pre>
 *
 * @since 1.0.0
 */
public interface WasiEnvironment {

  /**
   * Gets all environment variables as a map.
   *
   * <p>Returns POSIX-style environment variables where each entry is a pair of variable name and
   * value. The returned map is immutable.
   *
   * @return immutable map of environment variable names to values
   */
  Map<String, String> getEnvironmentVariables();

  /**
   * Gets the value of a specific environment variable.
   *
   * @param name the variable name
   * @return the variable value if present, empty otherwise
   * @throws IllegalArgumentException if name is null or empty
   */
  Optional<String> getVariable(final String name);

  /**
   * Gets command-line arguments.
   *
   * <p>Returns traditional list-of-strings style command-line arguments. The first element is
   * typically the program name. The returned list is immutable.
   *
   * @return immutable list of command-line arguments
   */
  List<String> getArguments();

  /**
   * Gets the initial working directory path.
   *
   * <p>Returns the working directory that was set when the component was instantiated. This may be
   * empty if no working directory was provided.
   *
   * @return the initial working directory path, or empty if not provided
   */
  Optional<String> getInitialCwd();
}
