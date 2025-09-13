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

import static org.junit.jupiter.api.Assertions.*;

import ai.tegmentum.wasmtime4j.*;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.nativefunctions.NativeFunctionTestUtils;
import java.util.logging.Logger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

/**
 * Integration tests for Panama Table implementation with enhanced reference type handling.
 * 
 * <p>This test class validates the implementation of Issue #227 enhancements:
 * - Dynamic type detection replacing hard-coded funcref assumptions
 * - Proper element type validation matching table's declared type  
 * - Comprehensive null reference handling
 * - Enhanced error handling with meaningful error codes
 */
@EnabledIf("ai.tegmentum.wasmtime4j.panama.PanamaTableReferenceTypeIT#isPanamaAvailable")
public class PanamaTableReferenceTypeIT {

  private static final Logger logger = Logger.getLogger(PanamaTableReferenceTypeIT.class.getName());
  
  private final NativeFunctionTestUtils testUtils = new NativeFunctionTestUtils();

  static boolean isPanamaAvailable() {
    try {
      return System.getProperty("java.version", "").startsWith("23")
          && System.getProperty("wasmtime4j.runtime", "auto").equals("panama");
    } catch (Exception e) {
      return false;
    }
  }

  @Test
  void testTableElementTypeDetection() throws WasmException {
    logger.info("Testing dynamic table element type detection");
    
    final byte[] moduleBytes = testUtils.getTableModule();
    
    try (final Engine engine = Engine.create();
         final Store store = Store.create(engine);
         final Module module = Module.compile(engine, moduleBytes);
         final Instance instance = Instance.create(store, module)) {
      
      final var tableOpt = instance.getTable("table");
      assertTrue(tableOpt.isPresent(), "Table export should be present");
      
      final WasmTable table = tableOpt.get();
      
      // Test dynamic type detection - should detect funcref type
      final WasmValueType elementType = table.getElementType();
      assertNotNull(elementType, "Element type should not be null");
      assertEquals(WasmValueType.FUNCREF, elementType, "Table should have funcref element type");
      
      logger.info("Successfully detected table element type: " + elementType);
    }
  }

  @Test
  void testTableSizeAndMaxSize() throws WasmException {
    logger.info("Testing table size and maximum size operations");
    
    final byte[] moduleBytes = testUtils.getTableModule();
    
    try (final Engine engine = Engine.create();
         final Store store = Store.create(engine);
         final Module module = Module.compile(engine, moduleBytes);
         final Instance instance = Instance.create(store, module)) {
      
      final var tableOpt = instance.getTable("table");
      assertTrue(tableOpt.isPresent(), "Table export should be present");
      
      final WasmTable table = tableOpt.get();
      
      // Test size operation
      final int size = table.getSize();
      assertEquals(10, size, "Table should have initial size of 10");
      
      // Test max size operation (may be unlimited)
      final int maxSize = table.getMaxSize();
      assertTrue(maxSize == -1 || maxSize >= size, 
                 "Max size should be unlimited (-1) or >= current size");
      
      logger.info("Table size: " + size + ", max size: " + maxSize);
    }
  }

  @Test
  void testTableNullElementHandling() throws WasmException {
    logger.info("Testing table null element handling");
    
    final byte[] moduleBytes = testUtils.getTableModule();
    
    try (final Engine engine = Engine.create();
         final Store store = Store.create(engine);
         final Module module = Module.compile(engine, moduleBytes);
         final Instance instance = Instance.create(store, module)) {
      
      final var tableOpt = instance.getTable("table");
      assertTrue(tableOpt.isPresent(), "Table export should be present");
      
      final WasmTable table = tableOpt.get();
      
      // Test getting null element from uninitialized table
      final Object element = table.get(0);
      assertNull(element, "Uninitialized table element should be null");
      
      // Test setting null element (should succeed)
      assertDoesNotThrow(() -> table.set(0, null), 
                        "Setting null element should not throw");
      
      // Verify null element was set
      final Object retrievedElement = table.get(0);
      assertNull(retrievedElement, "Retrieved element should still be null");
      
      logger.info("Successfully handled null elements");
    }
  }

  @Test
  void testTableBoundsChecking() throws WasmException {
    logger.info("Testing table bounds checking");
    
    final byte[] moduleBytes = testUtils.getTableModule();
    
    try (final Engine engine = Engine.create();
         final Store store = Store.create(engine);
         final Module module = Module.compile(engine, moduleBytes);
         final Instance instance = Instance.create(store, module)) {
      
      final var tableOpt = instance.getTable("table");
      assertTrue(tableOpt.isPresent(), "Table export should be present");
      
      final WasmTable table = tableOpt.get();
      final int size = table.getSize();
      
      // Test negative index
      assertThrows(IllegalArgumentException.class, 
                   () -> table.get(-1), 
                   "Negative index should throw IllegalArgumentException");
      
      assertThrows(IllegalArgumentException.class, 
                   () -> table.set(-1, null), 
                   "Negative index should throw IllegalArgumentException");
      
      // Test out of bounds index
      assertThrows(RuntimeException.class, 
                   () -> table.get(size), 
                   "Out of bounds index should throw RuntimeException");
      
      assertThrows(RuntimeException.class, 
                   () -> table.set(size, null), 
                   "Out of bounds index should throw RuntimeException");
      
      logger.info("Successfully validated bounds checking");
    }
  }

  @Test
  void testTableGrowOperation() throws WasmException {
    logger.info("Testing table grow operation");
    
    final byte[] moduleBytes = testUtils.getTableModule();
    
    try (final Engine engine = Engine.create();
         final Store store = Store.create(engine);
         final Module module = Module.compile(engine, moduleBytes);
         final Instance instance = Instance.create(store, module)) {
      
      final var tableOpt = instance.getTable("table");
      assertTrue(tableOpt.isPresent(), "Table export should be present");
      
      final WasmTable table = tableOpt.get();
      final int initialSize = table.getSize();
      
      // Test growing with null initial value
      final int previousSize = table.grow(5, null);
      assertEquals(initialSize, previousSize, 
                   "Grow should return previous size");
      
      final int newSize = table.getSize();
      assertEquals(initialSize + 5, newSize, 
                   "New size should be previous size + growth");
      
      // Test negative growth
      assertThrows(IllegalArgumentException.class, 
                   () -> table.grow(-1, null), 
                   "Negative growth should throw IllegalArgumentException");
      
      logger.info("Successfully tested table growth from " + initialSize + " to " + newSize);
    }
  }

  @Test
  void testTableErrorHandling() throws WasmException {
    logger.info("Testing table error handling");
    
    final byte[] moduleBytes = testUtils.getTableModule();
    
    try (final Engine engine = Engine.create();
         final Store store = Store.create(engine);
         final Module module = Module.compile(engine, moduleBytes);
         final Instance instance = Instance.create(store, module)) {
      
      final var tableOpt = instance.getTable("table");
      assertTrue(tableOpt.isPresent(), "Table export should be present");
      
      final WasmTable table = tableOpt.get();
      
      // Test setting incompatible value type
      final Object incompatibleValue = "not a function";
      assertThrows(IllegalArgumentException.class, 
                   () -> table.set(0, incompatibleValue), 
                   "Setting incompatible value should throw IllegalArgumentException");
      
      // Test growing with incompatible initial value
      assertThrows(IllegalArgumentException.class, 
                   () -> table.grow(1, incompatibleValue), 
                   "Growing with incompatible value should throw IllegalArgumentException");
      
      logger.info("Successfully validated error handling");
    }
  }

  @Test
  void testTableToStringRepresentation() throws WasmException {
    logger.info("Testing table string representation");
    
    final byte[] moduleBytes = testUtils.getTableModule();
    
    try (final Engine engine = Engine.create();
         final Store store = Store.create(engine);
         final Module module = Module.compile(engine, moduleBytes);
         final Instance instance = Instance.create(store, module)) {
      
      final var tableOpt = instance.getTable("table");
      assertTrue(tableOpt.isPresent(), "Table export should be present");
      
      final WasmTable table = tableOpt.get();
      
      final String tableString = table.toString();
      assertNotNull(tableString, "toString should not return null");
      assertTrue(tableString.contains("Table"), "toString should mention Table");
      assertTrue(tableString.contains("size="), "toString should include size");
      assertTrue(tableString.contains("elementType="), "toString should include elementType");
      
      logger.info("Table string representation: " + tableString);
    }
  }
}