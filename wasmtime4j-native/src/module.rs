//! WebAssembly module compilation, validation, and introspection
//!
//! This module provides defensive compilation and comprehensive module management
//! with validation, caching, and detailed introspection capabilities.

use crate::data_segment::DataSegment;
use crate::element_segment::ElementSegment;
use crate::element_segment_parser::{parse_data_segments, parse_element_segments};
use crate::engine::Engine;
use crate::error::{WasmtimeError, WasmtimeResult};
use std::collections::HashMap;
use std::sync::Arc;
use wasmtime::{Engine as WasmtimeEngine, FuncType, Module as WasmtimeModule, Mutability, ValType};
// Note: validation functions removed from crate root

/// Thread-safe wrapper around Wasmtime module with introspection
#[derive(Debug, Clone)]
pub struct Module {
    inner: Arc<WasmtimeModule>,
    /// Reference to the Engine to ensure same Engine Arc is used for validation
    /// CRITICAL: Wasmtime validates Module/Store compatibility using Arc::ptr_eq(),
    /// so we must maintain a reference to the same Engine (which contains Arc<WasmtimeEngine>)
    engine: Engine,
    pub metadata: ModuleMetadata,
    /// Element segments for table.init() operations (hybrid design - only passive segments cached)
    pub element_segments: Vec<Option<ElementSegment>>,
    /// Data segments for memory.init() operations (hybrid design - only passive segments cached)
    pub data_segments: Vec<Option<DataSegment>>,
}

/// Comprehensive module metadata for introspection and validation
#[derive(Debug, Clone)]
pub struct ModuleMetadata {
    /// Optional module name from the name custom section
    pub name: Option<String>,
    /// Size of the original WebAssembly binary in bytes
    pub size_bytes: usize,
    /// List of imports required by this module
    pub imports: Vec<ImportInfo>,
    /// List of exports provided by this module
    pub exports: Vec<ExportInfo>,
    /// Information about all functions in the module
    pub functions: Vec<FunctionInfo>,
    /// Information about global variables in the module
    pub globals: Vec<GlobalInfo>,
    /// Information about memory sections in the module
    pub memories: Vec<MemoryInfo>,
    /// Information about tables in the module
    pub tables: Vec<TableInfo>,
    /// Custom sections from the WebAssembly module
    pub custom_sections: HashMap<String, Vec<u8>>,
}

/// Import information for validation and resolution
#[derive(Debug, Clone, serde::Serialize, serde::Deserialize)]
pub struct ImportInfo {
    /// Module name that provides this import
    pub module: String,
    /// Name of the imported item within the module
    pub name: String,
    /// Type and signature of the imported item
    pub import_type: ImportKind,
}

/// Export information for binding and invocation
#[derive(Debug, Clone, serde::Serialize, serde::Deserialize)]
pub struct ExportInfo {
    /// Name of the exported item
    pub name: String,
    /// Type and signature of the exported item
    pub export_type: ExportKind,
}

/// Function signature information
#[derive(Debug, Clone)]
pub struct FunctionInfo {
    /// Zero-based index of this function in the module
    pub index: usize,
    /// Optional function name from debug info or custom sections
    pub name: Option<String>,
    /// Function signature with parameter and return types
    pub signature: FunctionSignature,
}

/// Function signature with parameter and return types
#[derive(Debug, Clone, serde::Serialize, serde::Deserialize)]
pub struct FunctionSignature {
    /// Parameter types for this function
    pub params: Vec<ModuleValueType>,
    /// Return types for this function
    pub returns: Vec<ModuleValueType>,
}

/// Global variable information
#[derive(Debug, Clone)]
pub struct GlobalInfo {
    /// Zero-based index of this global in the module
    pub index: usize,
    /// Optional global name from debug info or custom sections
    pub name: Option<String>,
    /// WebAssembly value type of this global
    pub value_type: ModuleValueType,
    /// Whether this global can be modified after initialization
    pub mutable: bool,
}

/// Memory information
#[derive(Debug, Clone)]
pub struct MemoryInfo {
    /// Zero-based index of this memory in the module
    pub index: usize,
    /// Optional memory name from debug info or custom sections
    pub name: Option<String>,
    /// Initial size in WebAssembly pages (64KB each)
    pub initial_pages: u64,
    /// Optional maximum size in WebAssembly pages
    pub maximum_pages: Option<u64>,
    /// Whether this memory can be shared between threads
    pub shared: bool,
    /// Whether this is a 64-bit memory (vs 32-bit)
    pub is_64: bool,
}

/// Table information
#[derive(Debug, Clone)]
pub struct TableInfo {
    /// Zero-based index of this table in the module
    pub index: usize,
    /// Optional table name from debug info or custom sections
    pub name: Option<String>,
    /// WebAssembly type of elements stored in this table
    pub element_type: ModuleValueType,
    /// Initial number of elements in the table
    pub initial_elements: u32,
    /// Optional maximum number of elements the table can grow to
    pub maximum_elements: Option<u32>,
}

/// WebAssembly value types with defensive validation
#[derive(Debug, Clone, Copy, PartialEq, Eq, serde::Serialize, serde::Deserialize)]
pub enum ModuleValueType {
    /// 32-bit integer
    I32,
    /// 64-bit integer
    I64,
    /// 32-bit floating point
    F32,
    /// 64-bit floating point
    F64,
    /// 128-bit SIMD vector
    V128,
    /// Reference to external object
    ExternRef,
    /// Reference to WebAssembly function
    FuncRef,
    /// Reference to any GC object (anyref)
    AnyRef,
    /// Reference to equality-comparable GC object (eqref)
    EqRef,
    /// Reference to i31 value (i31ref)
    I31Ref,
    /// Reference to struct type (structref)
    StructRef,
    /// Reference to array type (arrayref)
    ArrayRef,
    /// Null reference (ref null none) - bottom type
    NullRef,
    /// Null function reference (ref null nofunc)
    NullFuncRef,
    /// Null extern reference (ref null noextern)
    NullExternRef,
}

/// Import kinds with type information
#[derive(Debug, Clone, serde::Serialize, serde::Deserialize)]
pub enum ImportKind {
    /// Function import with signature
    Function(FunctionSignature),
    /// Global variable import with type and mutability
    Global(ModuleValueType, bool), // (type, mutable)
    /// Memory import with initial size, optional max size, is_64, and sharing
    Memory(u64, Option<u64>, bool, bool), // (initial, max, is_64, shared)
    /// Table import with element type, initial size, and optional max size
    Table(ModuleValueType, u32, Option<u32>), // (element_type, initial, max)
}

/// Export kinds with type information
#[derive(Debug, Clone, serde::Serialize, serde::Deserialize)]
pub enum ExportKind {
    /// Function export with signature
    Function(FunctionSignature),
    /// Global variable export with type and mutability
    Global(ModuleValueType, bool),
    /// Memory export with initial size, optional max size, is_64, and sharing
    Memory(u64, Option<u64>, bool, bool),
    /// Table export with element type, initial size, and optional max size
    Table(ModuleValueType, u32, Option<u32>),
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
                message: format!(
                    "Module size {} exceeds maximum {}",
                    wasm_bytes.len(),
                    MAX_MODULE_SIZE
                ),
            });
        }

        engine.validate()?;

        // Acquire compile lock to prevent race conditions during concurrent compilation.
        // This is critical when multiple threads compile modules to the same engine,
        // especially when using shared memory features.
        let _compile_guard = engine.acquire_compile_lock();

        // Compile module with defensive error handling
        let module = WasmtimeModule::new(engine.inner(), wasm_bytes)
            .map_err(|e| WasmtimeError::from_compilation_error(e))?;

        // Extract comprehensive metadata
        let metadata = ModuleMetadata::extract(&module, wasm_bytes.len(), wasm_bytes)?;

        // Parse element segments for table.init() support (hybrid design)
        let element_segments = parse_element_segments(wasm_bytes).unwrap_or_else(|e| {
            log::warn!("Failed to parse element segments: {:?}", e);
            Vec::new()
        });

        // Parse data segments for memory.init() support (hybrid design)
        let data_segments = parse_data_segments(wasm_bytes).unwrap_or_else(|e| {
            log::warn!("Failed to parse data segments: {:?}", e);
            Vec::new()
        });

        Ok(Module {
            inner: Arc::new(module),
            engine: engine.clone(),
            metadata,
            element_segments,
            data_segments,
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

        // Convert WAT to WASM bytes using wat crate
        let wasm_bytes = wat::parse_str(wat).map_err(|e| WasmtimeError::Compilation {
            message: format!("WAT parsing failed: {}", e),
        })?;

        Self::compile(engine, &wasm_bytes)
    }

    /// Validate WebAssembly bytecode without compiling (static validation)
    pub fn validate_bytes(wasm_bytes: &[u8]) -> WasmtimeResult<()> {
        // Defensive validation
        if wasm_bytes.is_empty() {
            return Err(WasmtimeError::InvalidParameter {
                message: "WebAssembly bytes cannot be empty".to_string(),
            });
        }

        // Check WASM magic number
        if wasm_bytes.len() < 8 {
            return Err(WasmtimeError::Validation {
                message: "WebAssembly binary too short".to_string(),
            });
        }

        if &wasm_bytes[0..4] != b"\0asm" {
            return Err(WasmtimeError::Validation {
                message: "Invalid WebAssembly magic number".to_string(),
            });
        }

        // Check version
        let version =
            u32::from_le_bytes([wasm_bytes[4], wasm_bytes[5], wasm_bytes[6], wasm_bytes[7]]);

        if version != 1 {
            return Err(WasmtimeError::Validation {
                message: format!("Unsupported WebAssembly version: {}", version),
            });
        }

        // Use the shared wasmtime engine for validation to avoid GLOBAL_CODE accumulation
        let validation_engine = crate::engine::get_shared_wasmtime_engine();
        match wasmtime::Module::validate(&validation_engine, wasm_bytes) {
            Ok(_) => Ok(()),
            Err(e) => Err(WasmtimeError::Validation {
                message: format!("WebAssembly validation failed: {}", e),
            }),
        }
    }

    /// Get reference to inner Wasmtime module (internal use)
    pub(crate) fn inner(&self) -> &WasmtimeModule {
        &*self.inner
    }

    /// Get reference to the engine this module was compiled with
    /// CRITICAL: This ensures Store uses the same Engine Arc for validation
    pub(crate) fn engine(&self) -> &Engine {
        &self.engine
    }

    /// Get the wasmtime Engine from the internal wasmtime::Module
    ///
    /// CRITICAL: This returns the exact Engine Arc that wasmtime uses internally.
    /// When creating a Store that will be used with this Module, the Store MUST
    /// be created using this Engine to ensure Arc pointer equality.
    ///
    /// This is necessary because wasmtime's Instance::new() uses Arc::ptr_eq()
    /// to verify that the Module and Store were created with the same Engine.
    pub(crate) fn wasmtime_engine(&self) -> &WasmtimeEngine {
        self.inner.engine()
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
        self.metadata
            .exports
            .iter()
            .filter(|exp| matches!(exp.export_type, ExportKind::Function(_)))
            .collect()
    }

    /// Get all memory exports
    pub fn memory_exports(&self) -> Vec<&ExportInfo> {
        self.metadata
            .exports
            .iter()
            .filter(|exp| matches!(exp.export_type, ExportKind::Memory(_, _, _, _)))
            .collect()
    }

    /// Get required imports for validation
    pub fn required_imports(&self) -> &[ImportInfo] {
        &self.metadata.imports
    }

    /// Validate that all required imports can be satisfied
    pub fn validate_imports(
        &self,
        available_imports: &HashMap<String, HashMap<String, ImportKind>>,
    ) -> WasmtimeResult<()> {
        for import in &self.metadata.imports {
            let module_imports = available_imports.get(&import.module).ok_or_else(|| {
                WasmtimeError::ImportExport {
                    message: format!("Missing import module: {}", import.module),
                }
            })?;

            let import_item =
                module_imports
                    .get(&import.name)
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
        self.inner.serialize().map_err(|e| WasmtimeError::Internal {
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
        let module =
            unsafe { WasmtimeModule::deserialize(engine.inner(), bytes) }.map_err(|e| {
                WasmtimeError::Compilation {
                    message: format!("Module deserialization failed: {}", e),
                }
            })?;

        // Note: We can't extract metadata from deserialized modules easily
        // This is a limitation we'll document
        let metadata = ModuleMetadata::empty();

        // Element segments cannot be recovered from deserialized modules
        // This is a known limitation - table.init() won't work with deserialized modules
        let element_segments = Vec::new();

        // Data segments cannot be recovered from deserialized modules
        // This is a known limitation - memory.init() won't work with deserialized modules
        let data_segments = Vec::new();

        Ok(Module {
            inner: Arc::new(module),
            engine: engine.clone(),
            metadata,
            element_segments,
            data_segments,
        })
    }

    /// Create a Module wrapper from a raw wasmtime::Module
    ///
    /// This is used when deserializing modules through the serializer which handles
    /// decompression and returns a raw wasmtime::Module.
    ///
    /// Note: Element and data segments cannot be recovered from deserialized modules,
    /// so table.init() and memory.init() operations won't work.
    pub fn from_wasmtime_module(
        module: WasmtimeModule,
        engine: Engine,
        metadata: ModuleMetadata,
    ) -> Self {
        Module {
            inner: Arc::new(module),
            engine,
            metadata,
            element_segments: Vec::new(),
            data_segments: Vec::new(),
        }
    }

    /// Deserialize module directly from a file using memory-mapped I/O
    ///
    /// This is more efficient than reading the file and then calling `deserialize()`
    /// because it uses memory-mapped I/O to avoid copying the file contents into memory.
    ///
    /// # Safety
    /// This function is unsafe because the file's contents must have been previously
    /// created by `Module::serialize()` from a compatible Wasmtime engine.
    ///
    /// # Arguments
    /// * `engine` - The Engine to use for deserialization
    /// * `path` - Path to the serialized module file
    ///
    /// # Returns
    /// The deserialized Module on success, or an error if deserialization fails
    pub fn deserialize_file(
        engine: &Engine,
        path: impl AsRef<std::path::Path>,
    ) -> WasmtimeResult<Self> {
        let path = path.as_ref();

        if !path.exists() {
            return Err(WasmtimeError::InvalidParameter {
                message: format!("File does not exist: {}", path.display()),
            });
        }

        engine.validate()?;

        // Safety: We trust that the serialized file came from Wasmtime
        let module =
            unsafe { WasmtimeModule::deserialize_file(engine.inner(), path) }.map_err(|e| {
                WasmtimeError::Compilation {
                    message: format!("Module deserialization from file failed: {}", e),
                }
            })?;

        // Note: We can't extract metadata from deserialized modules easily
        // This is a limitation we'll document
        let metadata = ModuleMetadata::empty();

        // Element segments cannot be recovered from deserialized modules
        // This is a known limitation - table.init() won't work with deserialized modules
        let element_segments = Vec::new();

        // Data segments cannot be recovered from deserialized modules
        // This is a known limitation - memory.init() won't work with deserialized modules
        let data_segments = Vec::new();

        Ok(Module {
            inner: Arc::new(module),
            engine: engine.clone(),
            metadata,
            element_segments,
            data_segments,
        })
    }
}

impl ModuleMetadata {
    fn extract(
        module: &WasmtimeModule,
        size_bytes: usize,
        wasm_bytes: &[u8],
    ) -> WasmtimeResult<Self> {
        let mut imports = Vec::new();
        let mut exports = Vec::new();
        let mut functions = Vec::new();
        let globals = Vec::new();
        let memories = Vec::new();
        let tables = Vec::new();

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

        // Extract function information from imports and exports
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

        // Also include exported functions (module-defined)
        for export in module.exports() {
            if let wasmtime::ExternType::Func(func_type) = export.ty() {
                functions.push(FunctionInfo {
                    index: func_index,
                    name: Some(export.name().to_string()),
                    signature: convert_func_type(&func_type)?,
                });
                func_index += 1;
            }
        }

        // Extract custom sections from raw WASM bytes
        let custom_sections = extract_custom_sections(wasm_bytes);

        Ok(ModuleMetadata {
            name: None, // Not easily extractable from Wasmtime
            size_bytes,
            imports,
            exports,
            functions,
            globals,
            memories,
            tables,
            custom_sections,
        })
    }

    /// Create empty metadata for deserialized modules
    pub fn empty() -> Self {
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

/// Extract custom sections from WebAssembly bytecode
fn extract_custom_sections(wasm_bytes: &[u8]) -> HashMap<String, Vec<u8>> {
    use wasmparser::{Parser, Payload};

    let mut custom_sections = HashMap::new();

    let parser = Parser::new(0);
    for payload in parser.parse_all(wasm_bytes) {
        match payload {
            Ok(Payload::CustomSection(reader)) => {
                let name = reader.name().to_string();
                let data = reader.data().to_vec();
                custom_sections.insert(name, data);
            }
            _ => {
                // Continue parsing other sections
            }
        }
    }

    custom_sections
}

// Helper functions for type conversion
fn convert_import_type(ty: wasmtime::ExternType) -> WasmtimeResult<ImportKind> {
    match ty {
        wasmtime::ExternType::Func(func_type) => {
            Ok(ImportKind::Function(convert_func_type(&func_type)?))
        }
        wasmtime::ExternType::Global(global_type) => Ok(ImportKind::Global(
            convert_val_type(global_type.content().clone())?,
            matches!(global_type.mutability(), Mutability::Var),
        )),
        wasmtime::ExternType::Memory(memory_type) => Ok(ImportKind::Memory(
            memory_type.minimum(),
            memory_type.maximum(),
            memory_type.is_64(),
            memory_type.is_shared(),
        )),
        wasmtime::ExternType::Table(table_type) => Ok(ImportKind::Table(
            convert_ref_type(table_type.element().clone())?,
            table_type.minimum().try_into().unwrap_or(u32::MAX),
            table_type
                .maximum()
                .map(|max| max.try_into().unwrap_or(u32::MAX)),
        )),
        wasmtime::ExternType::Tag(_tag_type) => {
            // Tag types are not supported in our interface
            Err(WasmtimeError::Module {
                message: "Tag types are not supported".to_string(),
            })
        }
    }
}

fn convert_export_type(ty: wasmtime::ExternType) -> WasmtimeResult<ExportKind> {
    match ty {
        wasmtime::ExternType::Func(func_type) => {
            Ok(ExportKind::Function(convert_func_type(&func_type)?))
        }
        wasmtime::ExternType::Global(global_type) => Ok(ExportKind::Global(
            convert_val_type(global_type.content().clone())?,
            matches!(global_type.mutability(), Mutability::Var),
        )),
        wasmtime::ExternType::Memory(memory_type) => Ok(ExportKind::Memory(
            memory_type.minimum(),
            memory_type.maximum(),
            memory_type.is_64(),
            memory_type.is_shared(),
        )),
        wasmtime::ExternType::Table(table_type) => Ok(ExportKind::Table(
            convert_ref_type(table_type.element().clone())?,
            table_type.minimum().try_into().unwrap_or(u32::MAX),
            table_type
                .maximum()
                .map(|max| max.try_into().unwrap_or(u32::MAX)),
        )),
        wasmtime::ExternType::Tag(_tag_type) => {
            // Tag types are not supported in our interface
            Err(WasmtimeError::Module {
                message: "Tag types are not supported".to_string(),
            })
        }
    }
}

fn convert_func_type(func_type: &FuncType) -> WasmtimeResult<FunctionSignature> {
    let params = func_type
        .params()
        .map(|vt| convert_val_type(vt))
        .collect::<Result<Vec<_>, _>>()?;

    let returns = func_type
        .results()
        .map(|vt| convert_val_type(vt))
        .collect::<Result<Vec<_>, _>>()?;

    Ok(FunctionSignature { params, returns })
}

fn convert_val_type(val_type: ValType) -> WasmtimeResult<ModuleValueType> {
    match val_type {
        ValType::I32 => Ok(ModuleValueType::I32),
        ValType::I64 => Ok(ModuleValueType::I64),
        ValType::F32 => Ok(ModuleValueType::F32),
        ValType::F64 => Ok(ModuleValueType::F64),
        ValType::V128 => Ok(ModuleValueType::V128),
        ValType::Ref(ref_type) => convert_ref_type(ref_type),
    }
}

fn convert_ref_type(ref_type: wasmtime::RefType) -> WasmtimeResult<ModuleValueType> {
    // Match on the heap type to determine the reference type
    // In wasmtime 40.x with GC support, concrete function references use ConcreteFunc
    match ref_type.heap_type() {
        wasmtime::HeapType::Extern => Ok(ModuleValueType::ExternRef),
        wasmtime::HeapType::Func | wasmtime::HeapType::ConcreteFunc(_) => {
            Ok(ModuleValueType::FuncRef)
        }
        // GC proposal heap types
        wasmtime::HeapType::Any | wasmtime::HeapType::Exn => Ok(ModuleValueType::AnyRef),
        wasmtime::HeapType::Eq => Ok(ModuleValueType::EqRef),
        wasmtime::HeapType::I31 => Ok(ModuleValueType::I31Ref),
        wasmtime::HeapType::Struct | wasmtime::HeapType::ConcreteStruct(_) => {
            Ok(ModuleValueType::StructRef)
        }
        wasmtime::HeapType::Array | wasmtime::HeapType::ConcreteArray(_) => {
            Ok(ModuleValueType::ArrayRef)
        }
        wasmtime::HeapType::None => Ok(ModuleValueType::NullRef),
        wasmtime::HeapType::NoFunc => Ok(ModuleValueType::NullFuncRef),
        wasmtime::HeapType::NoExtern => Ok(ModuleValueType::NullExternRef),
        _ => Err(WasmtimeError::Module {
            message: format!(
                "Unsupported reference type: {:?} (heap_type: {:?})",
                ref_type,
                ref_type.heap_type()
            ),
        }),
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
        (
            ImportKind::Memory(req_min, req_max, req_64, req_shared),
            ImportKind::Memory(avail_min, avail_max, avail_64, avail_shared),
        ) => {
            req_64 == avail_64
                && avail_min >= req_min
                && match (req_max, avail_max) {
                    (Some(req_max), Some(avail_max)) => avail_max >= req_max,
                    (Some(_), None) => true,
                    (None, _) => true,
                }
                && req_shared == avail_shared
        }
        (
            ImportKind::Table(req_elem, req_min, req_max),
            ImportKind::Table(avail_elem, avail_min, avail_max),
        ) => {
            req_elem == avail_elem
                && avail_min >= &req_min
                && match (req_max, avail_max) {
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

/// Shared core functions for module operations used by both JNI and Panama interfaces
///
/// These functions eliminate code duplication and provide consistent behavior
/// across interface implementations while maintaining defensive programming practices.
pub mod core {
    use super::*;
    use crate::engine::Engine;
    use crate::error::ffi_utils;
    use crate::{validate_not_empty, validate_ptr_not_null};
    use std::os::raw::c_void;

    /// Core function to compile a WebAssembly module from bytes
    pub fn compile_module(engine: &Engine, wasm_bytes: &[u8]) -> WasmtimeResult<Box<Module>> {
        validate_not_empty!(wasm_bytes, "WebAssembly bytes");
        Module::compile(engine, wasm_bytes).map(Box::new)
    }

    /// Core function to compile a WebAssembly module from WAT (WebAssembly Text format)
    pub fn compile_module_wat(engine: &Engine, wat: &str) -> WasmtimeResult<Box<Module>> {
        validate_not_empty!(wat.as_bytes(), "WAT string");
        Module::compile_wat(engine, wat).map(Box::new)
    }

    /// Core function to validate WebAssembly bytes without compilation
    pub fn validate_module_bytes(wasm_bytes: &[u8]) -> WasmtimeResult<()> {
        validate_not_empty!(wasm_bytes, "WebAssembly bytes");
        Module::validate_bytes(wasm_bytes)
    }

    /// Core function to validate module pointer and get reference
    pub unsafe fn get_module_ref(module_ptr: *const c_void) -> WasmtimeResult<&'static Module> {
        validate_ptr_not_null!(module_ptr, "module");
        Ok(&*(module_ptr as *const Module))
    }

    /// Core function to validate module pointer and get mutable reference
    pub unsafe fn get_module_mut(module_ptr: *mut c_void) -> WasmtimeResult<&'static mut Module> {
        validate_ptr_not_null!(module_ptr, "module");
        Ok(&mut *(module_ptr as *mut Module))
    }

    /// Core function to get module metadata
    pub fn get_metadata(module: &Module) -> &ModuleMetadata {
        module.metadata()
    }

    /// Core function to check if module has a specific export
    pub fn has_export(module: &Module, name: &str) -> bool {
        module.has_export(name)
    }

    /// Core function to get export information by name
    pub fn get_export_info<'a>(module: &'a Module, name: &str) -> Option<&'a ExportInfo> {
        module.get_export(name)
    }

    /// Core function to get all function exports
    pub fn get_function_exports(module: &Module) -> Vec<&ExportInfo> {
        module.function_exports()
    }

    /// Core function to get all memory exports
    pub fn get_memory_exports(module: &Module) -> Vec<&ExportInfo> {
        module.memory_exports()
    }

    /// Core function to get required imports
    pub fn get_required_imports(module: &Module) -> &[ImportInfo] {
        module.required_imports()
    }

    /// Core function to validate that all required imports can be satisfied
    pub fn validate_imports(
        module: &Module,
        available_imports: &HashMap<String, HashMap<String, ImportKind>>,
    ) -> WasmtimeResult<()> {
        module.validate_imports(available_imports)
    }

    /// Core function to serialize module for caching
    pub fn serialize_module(module: &Module) -> WasmtimeResult<Vec<u8>> {
        module.serialize()
    }

    /// Core function to deserialize module from cache
    pub fn deserialize_module(engine: &Engine, bytes: &[u8]) -> WasmtimeResult<Box<Module>> {
        validate_not_empty!(bytes, "serialized module bytes");
        Module::deserialize(engine, bytes).map(Box::new)
    }

    /// Core function to destroy a module (safe cleanup)
    pub unsafe fn destroy_module(module_ptr: *mut c_void) {
        ffi_utils::destroy_resource::<Module>(module_ptr, "Module");
    }

    /// Core function to validate module functionality
    pub fn validate_module(module: &Module) -> WasmtimeResult<()> {
        module.validate()
    }

    /// Core function to get module size in bytes
    pub fn get_module_size(module: &Module) -> usize {
        module.metadata().size_bytes
    }

    /// Core function to get module name (if available)
    pub fn get_module_name(module: &Module) -> Option<&str> {
        module.metadata().name.as_deref()
    }

    /// Core function to get number of exports
    pub fn get_export_count(module: &Module) -> usize {
        module.metadata().exports.len()
    }

    /// Core function to get number of imports
    pub fn get_import_count(module: &Module) -> usize {
        module.metadata().imports.len()
    }

    /// Core function to get number of functions
    pub fn get_function_count(module: &Module) -> usize {
        module.metadata().functions.len()
    }

    /// Core function to check if module has specific import
    pub fn has_import(module: &Module, module_name: &str, name: &str) -> bool {
        module
            .metadata
            .imports
            .iter()
            .any(|import| import.module == module_name && import.name == name)
    }

    /// Core function to get table exports (returns cloned data for FFI use)
    pub fn get_table_exports(module: &Module) -> Vec<ExportInfo> {
        module
            .metadata
            .exports
            .iter()
            .filter(|export| matches!(export.export_type, ExportKind::Table(_, _, _)))
            .cloned()
            .collect()
    }

    /// Core function to get global exports (returns cloned data for FFI use)
    pub fn get_global_exports(module: &Module) -> Vec<ExportInfo> {
        module
            .metadata
            .exports
            .iter()
            .filter(|export| matches!(export.export_type, ExportKind::Global(_, _)))
            .cloned()
            .collect()
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    // Use the global shared engine to reduce wasmtime GLOBAL_CODE registry accumulation
    fn shared_engine() -> Engine {
        crate::engine::get_shared_engine()
    }

    #[test]
    fn test_module_compilation() {
        let engine = shared_engine();

        // Simple WAT module for testing
        let wat = "(module (func (export \"test\") (result i32) i32.const 42))";
        let module = Module::compile_wat(&engine, wat).expect("Failed to compile module");

        assert!(module.validate().is_ok());
        assert!(module.has_export("test"));
        assert_eq!(module.metadata().exports.len(), 1);
    }

    #[test]
    fn test_empty_bytes() {
        let engine = shared_engine();
        let result = Module::compile(&engine, &[]);
        assert!(result.is_err());
    }

    #[test]
    fn test_module_metadata() {
        let engine = shared_engine();
        let wat = "(module 
                     (import \"env\" \"print\" (func $print (param i32)))
                     (func (export \"main\") (result i32) i32.const 42))";

        let module = Module::compile_wat(&engine, wat).expect("Failed to compile module");

        let metadata = module.metadata();
        assert_eq!(metadata.imports.len(), 1);
        assert_eq!(metadata.exports.len(), 1);

        let import = &metadata.imports[0];
        assert_eq!(import.module, "env");
        assert_eq!(import.name, "print");
    }

    #[test]
    fn test_function_exports() {
        let engine = shared_engine();
        let wat = "(module
                     (func (export \"add\") (param i32 i32) (result i32)
                       local.get 0 local.get 1 i32.add)
                     (memory (export \"mem\") 1))";

        let module = Module::compile_wat(&engine, wat).expect("Failed to compile module");

        let func_exports = module.function_exports();
        assert_eq!(func_exports.len(), 1);
        assert_eq!(func_exports[0].name, "add");

        let mem_exports = module.memory_exports();
        assert_eq!(mem_exports.len(), 1);
        assert_eq!(mem_exports[0].name, "mem");
    }

    // ==================== NEW TESTS ====================

    #[test]
    fn test_module_value_type_variants() {
        // Test that all variants can be created and compared
        let types = [
            ModuleValueType::I32,
            ModuleValueType::I64,
            ModuleValueType::F32,
            ModuleValueType::F64,
            ModuleValueType::V128,
            ModuleValueType::ExternRef,
            ModuleValueType::FuncRef,
            ModuleValueType::AnyRef,
            ModuleValueType::EqRef,
            ModuleValueType::I31Ref,
            ModuleValueType::StructRef,
            ModuleValueType::ArrayRef,
            ModuleValueType::NullRef,
            ModuleValueType::NullFuncRef,
            ModuleValueType::NullExternRef,
        ];

        // Verify all are distinct
        for (i, t1) in types.iter().enumerate() {
            for (j, t2) in types.iter().enumerate() {
                if i == j {
                    assert_eq!(t1, t2);
                } else {
                    assert_ne!(t1, t2);
                }
            }
        }
    }

    #[test]
    fn test_module_value_type_clone_copy() {
        let original = ModuleValueType::I32;
        let cloned = original.clone();
        let copied = original;

        assert_eq!(original, cloned);
        assert_eq!(original, copied);
    }

    #[test]
    fn test_import_kind_variants() {
        let func_import = ImportKind::Function(FunctionSignature {
            params: vec![ModuleValueType::I32],
            returns: vec![ModuleValueType::I32],
        });
        let global_import = ImportKind::Global(ModuleValueType::I64, true);
        let memory_import = ImportKind::Memory(1, Some(10), false, false);
        let table_import = ImportKind::Table(ModuleValueType::FuncRef, 1, Some(100));

        // Verify Debug works
        assert!(!format!("{:?}", func_import).is_empty());
        assert!(!format!("{:?}", global_import).is_empty());
        assert!(!format!("{:?}", memory_import).is_empty());
        assert!(!format!("{:?}", table_import).is_empty());
    }

    #[test]
    fn test_export_kind_variants() {
        let func_export = ExportKind::Function(FunctionSignature {
            params: vec![ModuleValueType::I32, ModuleValueType::I32],
            returns: vec![ModuleValueType::I64],
        });
        let global_export = ExportKind::Global(ModuleValueType::F32, false);
        let memory_export = ExportKind::Memory(1, None, false, false);
        let table_export = ExportKind::Table(ModuleValueType::FuncRef, 10, None);

        // Verify Clone works
        let cloned = func_export.clone();
        assert!(!format!("{:?}", cloned).is_empty());
        assert!(!format!("{:?}", global_export).is_empty());
        assert!(!format!("{:?}", memory_export).is_empty());
        assert!(!format!("{:?}", table_export).is_empty());
    }

    #[test]
    fn test_function_signature_creation() {
        let sig = FunctionSignature {
            params: vec![ModuleValueType::I32, ModuleValueType::I64],
            returns: vec![ModuleValueType::F64],
        };

        assert_eq!(sig.params.len(), 2);
        assert_eq!(sig.returns.len(), 1);
        assert_eq!(sig.params[0], ModuleValueType::I32);
        assert_eq!(sig.returns[0], ModuleValueType::F64);
    }

    #[test]
    fn test_module_metadata_empty() {
        let metadata = ModuleMetadata::empty();

        assert!(metadata.name.is_none());
        assert_eq!(metadata.size_bytes, 0);
        assert!(metadata.imports.is_empty());
        assert!(metadata.exports.is_empty());
        assert!(metadata.functions.is_empty());
        assert!(metadata.globals.is_empty());
        assert!(metadata.memories.is_empty());
        assert!(metadata.tables.is_empty());
        assert!(metadata.custom_sections.is_empty());
    }

    #[test]
    fn test_validate_bytes_empty() {
        let result = Module::validate_bytes(&[]);
        assert!(result.is_err());
        assert!(result.unwrap_err().to_string().contains("empty"));
    }

    #[test]
    fn test_validate_bytes_too_short() {
        let result = Module::validate_bytes(&[0x00, 0x61, 0x73, 0x6D]);
        assert!(result.is_err());
        assert!(result.unwrap_err().to_string().contains("too short"));
    }

    #[test]
    fn test_compile_wat_empty() {
        let engine = shared_engine();
        let result = Module::compile_wat(&engine, "");
        assert!(result.is_err());
        assert!(result.unwrap_err().to_string().contains("empty"));
    }

    #[test]
    fn test_module_with_global() {
        let engine = shared_engine();
        let wat = "(module
                     (global (export \"counter\") (mut i32) (i32.const 0))
                     (global (export \"constant\") i64 (i64.const 42)))";

        let module = Module::compile_wat(&engine, wat).expect("Failed to compile module");

        let metadata = module.metadata();
        assert_eq!(metadata.exports.len(), 2);

        // Check for global exports
        let global_exports: Vec<_> = metadata
            .exports
            .iter()
            .filter(|e| matches!(e.export_type, ExportKind::Global(_, _)))
            .collect();
        assert_eq!(global_exports.len(), 2);
    }

    #[test]
    fn test_module_with_table() {
        let engine = shared_engine();
        let wat = "(module
                     (table (export \"table\") 10 funcref))";

        let module = Module::compile_wat(&engine, wat).expect("Failed to compile module");

        let metadata = module.metadata();
        assert_eq!(metadata.exports.len(), 1);

        // Verify it's a table export
        match &metadata.exports[0].export_type {
            ExportKind::Table(elem_type, initial, max) => {
                assert_eq!(*elem_type, ModuleValueType::FuncRef);
                assert_eq!(*initial, 10);
                assert!(max.is_none());
            }
            _ => panic!("Expected table export"),
        }
    }

    #[test]
    fn test_module_get_export() {
        let engine = shared_engine();
        let wat = "(module
                     (func (export \"test\") (result i32) i32.const 42)
                     (memory (export \"mem\") 1))";

        let module = Module::compile_wat(&engine, wat).expect("Failed to compile module");

        assert!(module.get_export("test").is_some());
        assert!(module.get_export("mem").is_some());
        assert!(module.get_export("nonexistent").is_none());
    }

    #[test]
    fn test_import_info_creation() {
        let import = ImportInfo {
            module: "env".to_string(),
            name: "print".to_string(),
            import_type: ImportKind::Function(FunctionSignature {
                params: vec![ModuleValueType::I32],
                returns: vec![],
            }),
        };

        assert_eq!(import.module, "env");
        assert_eq!(import.name, "print");
        assert!(matches!(import.import_type, ImportKind::Function(_)));
    }

    #[test]
    fn test_export_info_creation() {
        let export = ExportInfo {
            name: "main".to_string(),
            export_type: ExportKind::Function(FunctionSignature {
                params: vec![],
                returns: vec![ModuleValueType::I32],
            }),
        };

        assert_eq!(export.name, "main");
        assert!(matches!(export.export_type, ExportKind::Function(_)));
    }
}

//
// Native C exports for JNI and Panama FFI consumption
//

use crate::shared_ffi::FFI_ERROR;
use std::ffi::CStr;
use std::os::raw::{c_char, c_int, c_void};

/// Compile WebAssembly module from bytes
///
/// # Safety
///
/// engine_ptr must be valid, wasm_bytes and size must be valid
/// Returns pointer to module that must be freed with wasmtime4j_module_destroy
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_module_compile(
    engine_ptr: *const c_void,
    wasm_bytes: *const u8,
    size: usize,
) -> *mut c_void {
    if engine_ptr.is_null() || wasm_bytes.is_null() || size == 0 {
        return std::ptr::null_mut();
    }

    match crate::engine::core::get_engine_ref(engine_ptr) {
        Ok(engine) => {
            let bytes = std::slice::from_raw_parts(wasm_bytes, size);
            match core::compile_module(engine, bytes) {
                Ok(module) => Box::into_raw(module) as *mut c_void,
                Err(_) => std::ptr::null_mut(),
            }
        }
        Err(_) => std::ptr::null_mut(),
    }
}

/// Compile WebAssembly module from WAT text
///
/// # Safety
///
/// engine_ptr must be valid, wat_text must be a valid null-terminated C string
/// Returns pointer to module that must be freed with wasmtime4j_module_destroy
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_module_compile_wat(
    engine_ptr: *const c_void,
    wat_text: *const c_char,
) -> *mut c_void {
    if engine_ptr.is_null() || wat_text.is_null() {
        return std::ptr::null_mut();
    }

    match crate::engine::core::get_engine_ref(engine_ptr) {
        Ok(engine) => match CStr::from_ptr(wat_text).to_str() {
            Ok(wat_str) => match core::compile_module_wat(engine, wat_str) {
                Ok(module) => Box::into_raw(module) as *mut c_void,
                Err(_) => std::ptr::null_mut(),
            },
            Err(_) => std::ptr::null_mut(),
        },
        Err(_) => std::ptr::null_mut(),
    }
}

/// Destroy module and free resources
///
/// # Safety
///
/// module_ptr must be a valid pointer from wasmtime4j_module_compile
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_module_destroy(module_ptr: *mut c_void) {
    if !module_ptr.is_null() {
        core::destroy_module(module_ptr);
    }
}

/// Alias for wasmtime4j_module_compile (Panama FFI compatibility)
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_module_create(
    engine_ptr: *const c_void,
    wasm_bytes: *const u8,
    size: usize,
) -> *mut c_void {
    wasmtime4j_module_compile(engine_ptr, wasm_bytes, size)
}

/// Alias for wasmtime4j_module_compile_wat (Panama FFI compatibility)
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_module_create_wat(
    engine_ptr: *const c_void,
    wat_text: *const c_char,
) -> *mut c_void {
    wasmtime4j_module_compile_wat(engine_ptr, wat_text)
}

/// Check if module has specific export
///
/// # Safety
///
/// module_ptr must be valid, name must be a valid null-terminated C string
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_module_has_export(
    module_ptr: *const c_void,
    name: *const c_char,
) -> c_int {
    if module_ptr.is_null() || name.is_null() {
        return FFI_ERROR;
    }

    match core::get_module_ref(module_ptr) {
        Ok(module) => match CStr::from_ptr(name).to_str() {
            Ok(name_str) => {
                if core::has_export(module, name_str) {
                    1
                } else {
                    0
                }
            }
            Err(_) => FFI_ERROR,
        },
        Err(_) => FFI_ERROR,
    }
}

/// Check if module has specific import
///
/// # Safety
///
/// module_ptr must be valid, module_name and name must be valid null-terminated C strings
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_module_has_import(
    module_ptr: *const c_void,
    module_name: *const c_char,
    name: *const c_char,
) -> c_int {
    if module_ptr.is_null() || module_name.is_null() || name.is_null() {
        return FFI_ERROR;
    }

    match core::get_module_ref(module_ptr) {
        Ok(module) => {
            match (
                CStr::from_ptr(module_name).to_str(),
                CStr::from_ptr(name).to_str(),
            ) {
                (Ok(mod_str), Ok(name_str)) => {
                    if core::has_import(module, mod_str, name_str) {
                        1
                    } else {
                        0
                    }
                }
                _ => FFI_ERROR,
            }
        }
        Err(_) => FFI_ERROR,
    }
}

/// Get number of imports in module
///
/// # Safety
///
/// module_ptr must be a valid pointer from wasmtime4j_module_compile
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_module_import_count(module_ptr: *const c_void) -> usize {
    match core::get_module_ref(module_ptr) {
        Ok(module) => core::get_metadata(module).imports.len(),
        Err(_) => 0,
    }
}

/// Get number of exports in module
///
/// # Safety
///
/// module_ptr must be a valid pointer from wasmtime4j_module_compile
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_module_export_count(module_ptr: *const c_void) -> usize {
    match core::get_module_ref(module_ptr) {
        Ok(module) => core::get_metadata(module).exports.len(),
        Err(_) => 0,
    }
}

/// Get number of function exports in module
///
/// # Safety
///
/// module_ptr must be a valid pointer from wasmtime4j_module_compile
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_module_function_export_count(
    module_ptr: *const c_void,
) -> usize {
    match core::get_module_ref(module_ptr) {
        Ok(module) => core::get_function_exports(module).len(),
        Err(_) => 0,
    }
}

/// Get number of memory exports in module
///
/// # Safety
///
/// module_ptr must be a valid pointer from wasmtime4j_module_compile
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_module_memory_export_count(module_ptr: *const c_void) -> usize {
    match core::get_module_ref(module_ptr) {
        Ok(module) => core::get_memory_exports(module).len(),
        Err(_) => 0,
    }
}

/// Get number of table exports in module
///
/// # Safety
///
/// module_ptr must be a valid pointer from wasmtime4j_module_compile
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_module_table_export_count(module_ptr: *const c_void) -> usize {
    match core::get_module_ref(module_ptr) {
        Ok(module) => core::get_table_exports(module).len(),
        Err(_) => 0,
    }
}

/// Get number of global exports in module
///
/// # Safety
///
/// module_ptr must be a valid pointer from wasmtime4j_module_compile
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_module_global_export_count(module_ptr: *const c_void) -> usize {
    match core::get_module_ref(module_ptr) {
        Ok(module) => core::get_global_exports(module).len(),
        Err(_) => 0,
    }
}

/// Get module size in bytes
///
/// # Safety
///
/// module_ptr must be a valid pointer from wasmtime4j_module_compile
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_module_size_bytes(module_ptr: *const c_void) -> usize {
    match core::get_module_ref(module_ptr) {
        Ok(module) => core::get_metadata(module).size_bytes,
        Err(_) => 0,
    }
}
/// Get export names from a module
///
/// # Safety
///
/// module_ptr must be a valid pointer from wasmtime4j_module_compile
/// names_out must point to a buffer large enough to hold all export names
/// Returns the number of exports, or 0 on error
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_module_get_export_names(
    module_ptr: *const c_void,
    names_out: *mut *mut c_char,
    max_count: usize,
) -> usize {
    if module_ptr.is_null() || names_out.is_null() {
        return 0;
    }

    match core::get_module_ref(module_ptr) {
        Ok(module) => {
            let exports = &core::get_metadata(module).exports;
            let count = exports.len().min(max_count);

            for (i, export) in exports.iter().take(count).enumerate() {
                // Allocate C string for export name
                let c_str = match std::ffi::CString::new(export.name.clone()) {
                    Ok(s) => s,
                    Err(_) => continue,
                };
                // Write pointer to output array
                let ptr = c_str.into_raw();
                std::ptr::write(names_out.add(i), ptr);
            }

            count
        }
        Err(_) => 0,
    }
}

/// Get export information for a specific export by name
///
/// # Safety
///
/// module_ptr must be a valid pointer
/// name must be a valid C string
/// Returns 0=not found, 1=function, 2=global, 3=memory, 4=table
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_module_get_export_kind(
    module_ptr: *const c_void,
    name: *const c_char,
) -> i32 {
    if module_ptr.is_null() || name.is_null() {
        return 0;
    }

    match (
        core::get_module_ref(module_ptr),
        std::ffi::CStr::from_ptr(name).to_str(),
    ) {
        (Ok(module), Ok(name_str)) => {
            // Query the wasmtime module directly for exports
            // This works correctly for both compiled and deserialized modules
            let wasmtime_module = module.inner();
            // Collect exports to avoid borrowing issues
            let exports: Vec<_> = wasmtime_module.exports().collect();
            for export in exports {
                if export.name() == name_str {
                    return match export.ty() {
                        wasmtime::ExternType::Func(_) => 1,
                        wasmtime::ExternType::Global(_) => 2,
                        wasmtime::ExternType::Memory(_) => 3,
                        wasmtime::ExternType::Table(_) => 4,
                        wasmtime::ExternType::Tag(_) => 5,
                    };
                }
            }
            0 // Not found
        }
        _ => 0,
    }
}

/// Free a C string allocated by Rust
///
/// # Safety
///
/// ptr must be a valid pointer to a C string allocated by CString::into_raw
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_free_string(ptr: *mut c_char) {
    if !ptr.is_null() {
        // Reconstruct CString from raw pointer and drop it
        let _ = std::ffi::CString::from_raw(ptr);
    }
}
