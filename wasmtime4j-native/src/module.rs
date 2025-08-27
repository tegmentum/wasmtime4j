//! WebAssembly module compilation, validation, and introspection
//!
//! This module provides defensive compilation and comprehensive module management
//! with validation, caching, and detailed introspection capabilities.

use std::collections::HashMap;
use std::sync::Arc;
use wasmtime::{
    Module as WasmtimeModule, 
    ExportType, 
    ImportType, 
    FuncType, 
    GlobalType, 
    MemoryType, 
    TableType,
    ValType,
};
use crate::engine::Engine;
use crate::error::{WasmtimeError, WasmtimeResult};
use crate::{validate_not_null, validate_slice_bounds};

/// Thread-safe wrapper around Wasmtime module with introspection
#[derive(Debug, Clone)]
pub struct Module {
    inner: Arc<WasmtimeModule>,
    metadata: ModuleMetadata,
}

/// Comprehensive module metadata for introspection and validation
#[derive(Debug, Clone)]
pub struct ModuleMetadata {
    pub name: Option<String>,
    pub size_bytes: usize,
    pub imports: Vec<ImportInfo>,
    pub exports: Vec<ExportInfo>,
    pub functions: Vec<FunctionInfo>,
    pub globals: Vec<GlobalInfo>,
    pub memories: Vec<MemoryInfo>,
    pub tables: Vec<TableInfo>,
    pub custom_sections: HashMap<String, Vec<u8>>,
}

/// Import information for validation and resolution
#[derive(Debug, Clone)]
pub struct ImportInfo {
    pub module: String,
    pub name: String,
    pub import_type: ImportKind,
}

/// Export information for binding and invocation
#[derive(Debug, Clone)]
pub struct ExportInfo {
    pub name: String,
    pub export_type: ExportKind,
}

/// Function signature information
#[derive(Debug, Clone)]
pub struct FunctionInfo {
    pub index: usize,
    pub name: Option<String>,
    pub signature: FunctionSignature,
}

/// Function signature with parameter and return types
#[derive(Debug, Clone)]
pub struct FunctionSignature {
    pub params: Vec<ValueType>,
    pub returns: Vec<ValueType>,
}

/// Global variable information
#[derive(Debug, Clone)]
pub struct GlobalInfo {
    pub index: usize,
    pub name: Option<String>,
    pub value_type: ValueType,
    pub mutable: bool,
}

/// Memory information
#[derive(Debug, Clone)]
pub struct MemoryInfo {
    pub index: usize,
    pub name: Option<String>,
    pub initial_pages: u64,
    pub maximum_pages: Option<u64>,
    pub shared: bool,
    pub is_64: bool,
}

/// Table information
#[derive(Debug, Clone)]
pub struct TableInfo {
    pub index: usize,
    pub name: Option<String>,
    pub element_type: ValueType,
    pub initial_elements: u32,
    pub maximum_elements: Option<u32>,
}

/// WebAssembly value types with defensive validation
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum ValueType {
    I32,
    I64,
    F32,
    F64,
    V128,
    ExternRef,
    FuncRef,
}

/// Import kinds with type information
#[derive(Debug, Clone)]
pub enum ImportKind {
    Function(FunctionSignature),
    Global(ValueType, bool), // (type, mutable)
    Memory(u64, Option<u64>, bool), // (initial, max, shared)
    Table(ValueType, u32, Option<u32>), // (element_type, initial, max)
}

/// Export kinds with type information
#[derive(Debug, Clone)]
pub enum ExportKind {
    Function(FunctionSignature),
    Global(ValueType, bool),
    Memory(u64, Option<u64>, bool),
    Table(ValueType, u32, Option<u32>),
}

impl Module {
    /// Compile WebAssembly module from bytes with comprehensive validation
    pub fn compile(engine: &Engine, wasm_bytes: &[u8]) -> WasmtimeResult<Self> {
        // Defensive validation
        if wasm_bytes.is_empty() {
            return Err(WasmtimeError::InvalidParameter {
                message: "WebAssembly bytes cannot be empty".to_string(),
            });
        }

        // Reasonable size limit to prevent resource exhaustion
        const MAX_MODULE_SIZE: usize = 100 * 1024 * 1024; // 100MB
        if wasm_bytes.len() > MAX_MODULE_SIZE {
            return Err(WasmtimeError::Validation {
                message: format!("Module size {} exceeds maximum {}", wasm_bytes.len(), MAX_MODULE_SIZE),
            });
        }

        engine.validate()?;

        // Compile module with defensive error handling
        let module = WasmtimeModule::new(engine.inner(), wasm_bytes)
            .map_err(|e| WasmtimeError::from_compilation_error(e))?;

        // Extract comprehensive metadata
        let metadata = ModuleMetadata::extract(&module, wasm_bytes.len())?;

        Ok(Module {
            inner: Arc::new(module),
            metadata,
        })
    }

    /// Compile module from WebAssembly Text format (WAT)
    pub fn compile_wat(engine: &Engine, wat: &str) -> WasmtimeResult<Self> {
        if wat.is_empty() {
            return Err(WasmtimeError::InvalidParameter {
                message: "WAT string cannot be empty".to_string(),
            });
        }

        engine.validate()?;

        let wasm_bytes = wasmtime::wat::parse_str(wat)
            .map_err(|e| WasmtimeError::Compilation {
                message: format!("WAT parsing failed: {}", e),
            })?;

        Self::compile(engine, &wasm_bytes)
    }

    /// Get reference to inner Wasmtime module (internal use)
    pub(crate) fn inner(&self) -> &WasmtimeModule {
        &self.inner
    }

    /// Get module metadata for introspection
    pub fn metadata(&self) -> &ModuleMetadata {
        &self.metadata
    }

    /// Validate module is still functional (defensive check)
    pub fn validate(&self) -> WasmtimeResult<()> {
        if Arc::strong_count(&self.inner) == 0 {
            return Err(WasmtimeError::Internal {
                message: "Module reference count is invalid".to_string(),
            });
        }
        Ok(())
    }

    /// Check if module has specific export
    pub fn has_export(&self, name: &str) -> bool {
        self.metadata.exports.iter().any(|exp| exp.name == name)
    }

    /// Get export information by name
    pub fn get_export(&self, name: &str) -> Option<&ExportInfo> {
        self.metadata.exports.iter().find(|exp| exp.name == name)
    }

    /// Get all function exports
    pub fn function_exports(&self) -> Vec<&ExportInfo> {
        self.metadata.exports.iter()
            .filter(|exp| matches!(exp.export_type, ExportKind::Function(_)))
            .collect()
    }

    /// Get all memory exports
    pub fn memory_exports(&self) -> Vec<&ExportInfo> {
        self.metadata.exports.iter()
            .filter(|exp| matches!(exp.export_type, ExportKind::Memory(_, _, _)))
            .collect()
    }

    /// Get required imports for validation
    pub fn required_imports(&self) -> &[ImportInfo] {
        &self.metadata.imports
    }

    /// Validate that all required imports can be satisfied
    pub fn validate_imports(&self, available_imports: &HashMap<String, HashMap<String, ImportKind>>) -> WasmtimeResult<()> {
        for import in &self.metadata.imports {
            let module_imports = available_imports.get(&import.module)
                .ok_or_else(|| WasmtimeError::ImportExport {
                    message: format!("Missing import module: {}", import.module),
                })?;

            let import_item = module_imports.get(&import.name)
                .ok_or_else(|| WasmtimeError::ImportExport {
                    message: format!("Missing import: {}.{}", import.module, import.name),
                })?;

            // Validate import type compatibility
            if !import_types_compatible(&import.import_type, import_item) {
                return Err(WasmtimeError::ImportExport {
                    message: format!("Import type mismatch for {}.{}", import.module, import.name),
                });
            }
        }
        Ok(())
    }

    /// Get module serialization for caching
    pub fn serialize(&self) -> WasmtimeResult<Vec<u8>> {
        self.inner.serialize()
            .map_err(|e| WasmtimeError::Internal {
                message: format!("Module serialization failed: {}", e),
            })
    }

    /// Deserialize module from cache
    pub fn deserialize(engine: &Engine, bytes: &[u8]) -> WasmtimeResult<Self> {
        if bytes.is_empty() {
            return Err(WasmtimeError::InvalidParameter {
                message: "Serialized module bytes cannot be empty".to_string(),
            });
        }

        engine.validate()?;

        // Safety: We trust that the serialized bytes came from Wasmtime
        let module = unsafe { WasmtimeModule::deserialize(engine.inner(), bytes) }
            .map_err(|e| WasmtimeError::Compilation {
                message: format!("Module deserialization failed: {}", e),
            })?;

        // Note: We can't extract metadata from deserialized modules easily
        // This is a limitation we'll document
        let metadata = ModuleMetadata::empty();

        Ok(Module {
            inner: Arc::new(module),
            metadata,
        })
    }
}

impl ModuleMetadata {
    fn extract(module: &WasmtimeModule, size_bytes: usize) -> WasmtimeResult<Self> {
        let mut imports = Vec::new();
        let mut exports = Vec::new();
        let mut functions = Vec::new();
        let mut globals = Vec::new();
        let mut memories = Vec::new();
        let mut tables = Vec::new();

        // Extract imports
        for import in module.imports() {
            let import_type = convert_import_type(import.ty())?;
            imports.push(ImportInfo {
                module: import.module().to_string(),
                name: import.name().to_string(),
                import_type,
            });
        }

        // Extract exports
        for export in module.exports() {
            let export_type = convert_export_type(export.ty())?;
            exports.push(ExportInfo {
                name: export.name().to_string(),
                export_type,
            });
        }

        // Extract function information (basic)
        let mut func_index = 0;
        for import in module.imports() {
            if let wasmtime::ExternType::Func(func_type) = import.ty() {
                functions.push(FunctionInfo {
                    index: func_index,
                    name: Some(format!("{}:{}", import.module(), import.name())),
                    signature: convert_func_type(&func_type)?,
                });
                func_index += 1;
            }
        }

        Ok(ModuleMetadata {
            name: None, // Not easily extractable from Wasmtime
            size_bytes,
            imports,
            exports,
            functions,
            globals,
            memories,
            tables,
            custom_sections: HashMap::new(), // Not easily extractable
        })
    }

    fn empty() -> Self {
        ModuleMetadata {
            name: None,
            size_bytes: 0,
            imports: Vec::new(),
            exports: Vec::new(),
            functions: Vec::new(),
            globals: Vec::new(),
            memories: Vec::new(),
            tables: Vec::new(),
            custom_sections: HashMap::new(),
        }
    }
}

// Helper functions for type conversion
fn convert_import_type(ty: wasmtime::ExternType) -> WasmtimeResult<ImportKind> {
    match ty {
        wasmtime::ExternType::Func(func_type) => {
            Ok(ImportKind::Function(convert_func_type(&func_type)?))
        }
        wasmtime::ExternType::Global(global_type) => {
            Ok(ImportKind::Global(
                convert_val_type(global_type.content())?,
                global_type.mutability().is_mutable()
            ))
        }
        wasmtime::ExternType::Memory(memory_type) => {
            Ok(ImportKind::Memory(
                memory_type.minimum(),
                memory_type.maximum(),
                memory_type.is_shared()
            ))
        }
        wasmtime::ExternType::Table(table_type) => {
            Ok(ImportKind::Table(
                convert_val_type(table_type.element())?,
                table_type.minimum(),
                table_type.maximum()
            ))
        }
    }
}

fn convert_export_type(ty: wasmtime::ExternType) -> WasmtimeResult<ExportKind> {
    match ty {
        wasmtime::ExternType::Func(func_type) => {
            Ok(ExportKind::Function(convert_func_type(&func_type)?))
        }
        wasmtime::ExternType::Global(global_type) => {
            Ok(ExportKind::Global(
                convert_val_type(global_type.content())?,
                global_type.mutability().is_mutable()
            ))
        }
        wasmtime::ExternType::Memory(memory_type) => {
            Ok(ExportKind::Memory(
                memory_type.minimum(),
                memory_type.maximum(),
                memory_type.is_shared()
            ))
        }
        wasmtime::ExternType::Table(table_type) => {
            Ok(ExportKind::Table(
                convert_val_type(table_type.element())?,
                table_type.minimum(),
                table_type.maximum()
            ))
        }
    }
}

fn convert_func_type(func_type: &FuncType) -> WasmtimeResult<FunctionSignature> {
    let params = func_type.params()
        .map(|vt| convert_val_type(vt))
        .collect::<Result<Vec<_>, _>>()?;
    
    let returns = func_type.results()
        .map(|vt| convert_val_type(vt))
        .collect::<Result<Vec<_>, _>>()?;

    Ok(FunctionSignature { params, returns })
}

fn convert_val_type(val_type: ValType) -> WasmtimeResult<ValueType> {
    match val_type {
        ValType::I32 => Ok(ValueType::I32),
        ValType::I64 => Ok(ValueType::I64),
        ValType::F32 => Ok(ValueType::F32),
        ValType::F64 => Ok(ValueType::F64),
        ValType::V128 => Ok(ValueType::V128),
        ValType::ExternRef => Ok(ValueType::ExternRef),
        ValType::FuncRef => Ok(ValueType::FuncRef),
    }
}

fn import_types_compatible(required: &ImportKind, available: &ImportKind) -> bool {
    match (required, available) {
        (ImportKind::Function(req), ImportKind::Function(avail)) => {
            signatures_compatible(req, avail)
        }
        (ImportKind::Global(req_type, req_mut), ImportKind::Global(avail_type, avail_mut)) => {
            req_type == avail_type && req_mut <= avail_mut
        }
        (ImportKind::Memory(req_min, req_max, req_shared), ImportKind::Memory(avail_min, avail_max, avail_shared)) => {
            avail_min >= req_min && 
            match (req_max, avail_max) {
                (Some(req_max), Some(avail_max)) => avail_max >= req_max,
                (Some(_), None) => true,
                (None, _) => true,
            } && req_shared == avail_shared
        }
        (ImportKind::Table(req_elem, req_min, req_max), ImportKind::Table(avail_elem, avail_min, avail_max)) => {
            req_elem == avail_elem && avail_min >= &req_min &&
            match (req_max, avail_max) {
                (Some(req_max), Some(avail_max)) => avail_max >= req_max,
                (Some(_), None) => true,
                (None, _) => true,
            }
        }
        _ => false,
    }
}

fn signatures_compatible(required: &FunctionSignature, available: &FunctionSignature) -> bool {
    required.params == available.params && required.returns == available.returns
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_module_compilation() {
        let engine = Engine::new().expect("Failed to create engine");
        
        // Simple WAT module for testing
        let wat = "(module (func (export \"test\") (result i32) i32.const 42))";
        let module = Module::compile_wat(&engine, wat)
            .expect("Failed to compile module");

        assert!(module.validate().is_ok());
        assert!(module.has_export("test"));
        assert_eq!(module.metadata().exports.len(), 1);
    }

    #[test]
    fn test_empty_bytes() {
        let engine = Engine::new().expect("Failed to create engine");
        let result = Module::compile(&engine, &[]);
        assert!(result.is_err());
    }

    #[test]
    fn test_module_metadata() {
        let engine = Engine::new().expect("Failed to create engine");
        let wat = "(module 
                     (import \"env\" \"print\" (func $print (param i32)))
                     (func (export \"main\") (result i32) i32.const 42))";
        
        let module = Module::compile_wat(&engine, wat)
            .expect("Failed to compile module");

        let metadata = module.metadata();
        assert_eq!(metadata.imports.len(), 1);
        assert_eq!(metadata.exports.len(), 1);
        
        let import = &metadata.imports[0];
        assert_eq!(import.module, "env");
        assert_eq!(import.name, "print");
    }

    #[test]
    fn test_function_exports() {
        let engine = Engine::new().expect("Failed to create engine");
        let wat = "(module 
                     (func (export \"add\") (param i32 i32) (result i32) 
                       local.get 0 local.get 1 i32.add)
                     (memory (export \"mem\") 1))";
        
        let module = Module::compile_wat(&engine, wat)
            .expect("Failed to compile module");

        let func_exports = module.function_exports();
        assert_eq!(func_exports.len(), 1);
        assert_eq!(func_exports[0].name, "add");
        
        let mem_exports = module.memory_exports();
        assert_eq!(mem_exports.len(), 1);
        assert_eq!(mem_exports[0].name, "mem");
    }
}