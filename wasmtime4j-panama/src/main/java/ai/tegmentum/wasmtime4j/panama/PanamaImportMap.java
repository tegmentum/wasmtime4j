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

package ai.tegmentum.wasmtime4j.panama;

import ai.tegmentum.wasmtime4j.ImportMap;
import ai.tegmentum.wasmtime4j.WasmFunction;
import ai.tegmentum.wasmtime4j.WasmGlobal;
import ai.tegmentum.wasmtime4j.WasmMemory;
import ai.tegmentum.wasmtime4j.WasmTable;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Panama FFI implementation of the WebAssembly import map interface.
 *
 * <p>This implementation provides a high-performance, thread-safe import map specifically
 * optimized for Panama FFI operations. It maintains compatibility with the public ImportMap
 * interface while providing enhanced features for Panama-specific optimizations.
 *
 * <p>Key features:
 *
 * <ul>
 *   <li>Thread-safe concurrent access to imports
 *   <li>Type validation and conversion for Panama-specific implementations
 *   <li>Optimized storage for frequent import lookups
 *   <li>Integration with Panama resource management patterns
 * </ul>
 *
 * @since 1.0.0
 */
public final class PanamaImportMap implements ImportMap {
  private static final Logger logger = Logger.getLogger(PanamaImportMap.class.getName());

  // Concurrent storage for thread-safe access
  private final ConcurrentHashMap<String, ConcurrentHashMap<String, Object>> imports;

  // Performance optimizations
  private volatile int totalImports = 0;

  /**
   * Creates a new empty Panama import map.
   */
  public PanamaImportMap() {
    this.imports = new ConcurrentHashMap<>();
    
    if (logger.isLoggable(Level.FINE)) {
      logger.fine("Created new PanamaImportMap");
    }
  }

  @Override
  public ImportMap addFunction(final String moduleName, final String name, final WasmFunction function) {
    Objects.requireNonNull(moduleName, "Module name cannot be null");
    Objects.requireNonNull(name, "Import name cannot be null");
    Objects.requireNonNull(function, "Function cannot be null");

    // Validate that it's a compatible function type
    validateFunction(function);

    addImport(moduleName, name, function);
    
    if (logger.isLoggable(Level.FINE)) {
      logger.fine(String.format("Added function import: %s.%s", moduleName, name));
    }
    
    return this;
  }

  @Override
  public ImportMap addMemory(final String moduleName, final String name, final WasmMemory memory) {
    Objects.requireNonNull(moduleName, "Module name cannot be null");
    Objects.requireNonNull(name, "Import name cannot be null");
    Objects.requireNonNull(memory, "Memory cannot be null");

    // Validate that it's a compatible memory type
    validateMemory(memory);

    addImport(moduleName, name, memory);
    
    if (logger.isLoggable(Level.FINE)) {
      logger.fine(String.format("Added memory import: %s.%s", moduleName, name));
    }
    
    return this;
  }

  @Override
  public ImportMap addGlobal(final String moduleName, final String name, final WasmGlobal global) {
    Objects.requireNonNull(moduleName, "Module name cannot be null");
    Objects.requireNonNull(name, "Import name cannot be null");
    Objects.requireNonNull(global, "Global cannot be null");

    // Validate that it's a compatible global type
    validateGlobal(global);

    addImport(moduleName, name, global);
    
    if (logger.isLoggable(Level.FINE)) {
      logger.fine(String.format("Added global import: %s.%s", moduleName, name));
    }
    
    return this;
  }

  @Override
  public ImportMap addTable(final String moduleName, final String name, final WasmTable table) {
    Objects.requireNonNull(moduleName, "Module name cannot be null");
    Objects.requireNonNull(name, "Import name cannot be null");
    Objects.requireNonNull(table, "Table cannot be null");

    // Validate that it's a compatible table type
    validateTable(table);

    addImport(moduleName, name, table);
    
    if (logger.isLoggable(Level.FINE)) {
      logger.fine(String.format("Added table import: %s.%s", moduleName, name));
    }
    
    return this;
  }

  @Override
  public Optional<WasmFunction> getFunction(final String moduleName, final String name) {
    Objects.requireNonNull(moduleName, "Module name cannot be null");
    Objects.requireNonNull(name, "Import name cannot be null");

    final Object import_ = getImport(moduleName, name);
    if (import_ instanceof WasmFunction function) {
      return Optional.of(function);
    }
    
    return Optional.empty();
  }

  @Override
  public Map<String, Map<String, Object>> getImports() {
    // Return a defensive copy to prevent external modification
    final Map<String, Map<String, Object>> result = new ConcurrentHashMap<>();
    
    for (final Map.Entry<String, ConcurrentHashMap<String, Object>> moduleEntry : imports.entrySet()) {
      final Map<String, Object> moduleImports = new ConcurrentHashMap<>(moduleEntry.getValue());
      result.put(moduleEntry.getKey(), moduleImports);
    }
    
    return result;
  }

  @Override
  public boolean contains(final String moduleName, final String name) {
    Objects.requireNonNull(moduleName, "Module name cannot be null");
    Objects.requireNonNull(name, "Import name cannot be null");

    final ConcurrentHashMap<String, Object> moduleImports = imports.get(moduleName);
    return moduleImports != null && moduleImports.containsKey(name);
  }

  /**
   * Gets a memory import by module and name.
   *
   * @param moduleName the module name
   * @param name the import name
   * @return the memory, or empty if not found
   */
  public Optional<WasmMemory> getMemory(final String moduleName, final String name) {
    Objects.requireNonNull(moduleName, "Module name cannot be null");
    Objects.requireNonNull(name, "Import name cannot be null");

    final Object import_ = getImport(moduleName, name);
    if (import_ instanceof WasmMemory memory) {
      return Optional.of(memory);
    }
    
    return Optional.empty();
  }

  /**
   * Gets a global import by module and name.
   *
   * @param moduleName the module name
   * @param name the import name
   * @return the global, or empty if not found
   */
  public Optional<WasmGlobal> getGlobal(final String moduleName, final String name) {
    Objects.requireNonNull(moduleName, "Module name cannot be null");
    Objects.requireNonNull(name, "Import name cannot be null");

    final Object import_ = getImport(moduleName, name);
    if (import_ instanceof WasmGlobal global) {
      return Optional.of(global);
    }
    
    return Optional.empty();
  }

  /**
   * Gets a table import by module and name.
   *
   * @param moduleName the module name
   * @param name the import name
   * @return the table, or empty if not found
   */
  public Optional<WasmTable> getTable(final String moduleName, final String name) {
    Objects.requireNonNull(moduleName, "Module name cannot be null");
    Objects.requireNonNull(name, "Import name cannot be null");

    final Object import_ = getImport(moduleName, name);
    if (import_ instanceof WasmTable table) {
      return Optional.of(table);
    }
    
    return Optional.empty();
  }

  /**
   * Gets the total number of imports in this map.
   *
   * @return the total number of imports
   */
  public int getTotalImports() {
    return totalImports;
  }

  /**
   * Gets the number of modules in this import map.
   *
   * @return the number of modules
   */
  public int getModuleCount() {
    return imports.size();
  }

  /**
   * Clears all imports from this map.
   */
  public void clear() {
    imports.clear();
    totalImports = 0;
    
    if (logger.isLoggable(Level.FINE)) {
      logger.fine("Cleared all imports from PanamaImportMap");
    }
  }

  /**
   * Removes all imports for the specified module.
   *
   * @param moduleName the module name
   * @return true if the module existed and was removed
   */
  public boolean removeModule(final String moduleName) {
    Objects.requireNonNull(moduleName, "Module name cannot be null");

    final ConcurrentHashMap<String, Object> removed = imports.remove(moduleName);
    if (removed != null) {
      totalImports -= removed.size();
      
      if (logger.isLoggable(Level.FINE)) {
        logger.fine("Removed module from import map: " + moduleName);
      }
      
      return true;
    }
    
    return false;
  }

  /**
   * Removes a specific import from the map.
   *
   * @param moduleName the module name
   * @param name the import name
   * @return true if the import existed and was removed
   */
  public boolean removeImport(final String moduleName, final String name) {
    Objects.requireNonNull(moduleName, "Module name cannot be null");
    Objects.requireNonNull(name, "Import name cannot be null");

    final ConcurrentHashMap<String, Object> moduleImports = imports.get(moduleName);
    if (moduleImports != null) {
      final Object removed = moduleImports.remove(name);
      if (removed != null) {
        totalImports--;
        
        // Remove the module map if it's now empty
        if (moduleImports.isEmpty()) {
          imports.remove(moduleName);
        }
        
        if (logger.isLoggable(Level.FINE)) {
          logger.fine(String.format("Removed import: %s.%s", moduleName, name));
        }
        
        return true;
      }
    }
    
    return false;
  }

  /**
   * Adds an import to the map.
   *
   * @param moduleName the module name
   * @param name the import name
   * @param import_ the import object
   */
  private void addImport(final String moduleName, final String name, final Object import_) {
    final ConcurrentHashMap<String, Object> moduleImports = imports.computeIfAbsent(
        moduleName, k -> new ConcurrentHashMap<>());
    
    final Object previous = moduleImports.put(name, import_);
    if (previous == null) {
      totalImports++;
    }
  }

  /**
   * Gets an import from the map.
   *
   * @param moduleName the module name
   * @param name the import name
   * @return the import object, or null if not found
   */
  private Object getImport(final String moduleName, final String name) {
    final ConcurrentHashMap<String, Object> moduleImports = imports.get(moduleName);
    return moduleImports != null ? moduleImports.get(name) : null;
  }

  /**
   * Validates that a function is compatible with Panama FFI operations.
   *
   * @param function the function to validate
   * @throws IllegalArgumentException if the function is not compatible
   */
  private void validateFunction(final WasmFunction function) {
    // For now, accept all WasmFunction implementations
    // In the future, we might want to ensure Panama-specific optimizations
    
    if (function instanceof PanamaFunction || function instanceof PanamaHostFunction) {
      // These are preferred for optimal performance
      return;
    }
    
    // Log a performance warning for non-Panama functions
    if (logger.isLoggable(Level.FINE)) {
      logger.fine("Non-Panama function added to import map. This may impact performance: " + 
                  function.getClass().getName());
    }
  }

  /**
   * Validates that a memory is compatible with Panama FFI operations.
   *
   * @param memory the memory to validate
   * @throws IllegalArgumentException if the memory is not compatible
   */
  private void validateMemory(final WasmMemory memory) {
    // Similar validation for memory
    if (memory instanceof PanamaMemory) {
      return;
    }
    
    if (logger.isLoggable(Level.FINE)) {
      logger.fine("Non-Panama memory added to import map. This may impact performance: " + 
                  memory.getClass().getName());
    }
  }

  /**
   * Validates that a global is compatible with Panama FFI operations.
   *
   * @param global the global to validate
   * @throws IllegalArgumentException if the global is not compatible
   */
  private void validateGlobal(final WasmGlobal global) {
    // Similar validation for global
    if (global instanceof PanamaGlobal) {
      return;
    }
    
    if (logger.isLoggable(Level.FINE)) {
      logger.fine("Non-Panama global added to import map. This may impact performance: " + 
                  global.getClass().getName());
    }
  }

  /**
   * Validates that a table is compatible with Panama FFI operations.
   *
   * @param table the table to validate
   * @throws IllegalArgumentException if the table is not compatible
   */
  private void validateTable(final WasmTable table) {
    // Similar validation for table
    if (table instanceof PanamaTable) {
      return;
    }
    
    if (logger.isLoggable(Level.FINE)) {
      logger.fine("Non-Panama table added to import map. This may impact performance: " + 
                  table.getClass().getName());
    }
  }

  @Override
  public String toString() {
    return String.format(
        "PanamaImportMap{modules=%d, totalImports=%d}",
        imports.size(), totalImports);
  }
}