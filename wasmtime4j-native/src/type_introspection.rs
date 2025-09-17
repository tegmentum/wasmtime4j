//! Type introspection system for WebAssembly modules and instances.
//!
//! This module provides comprehensive type inspection capabilities including:
//! - Memory type introspection (limits, 64-bit addressing, shared memory)
//! - Table type introspection (element type, limits)
//! - Global type introspection (value type, mutability)
//! - Function type introspection (parameters, results)
//! - Module and instance type metadata extraction

use wasmtime::{
    FuncType as WasmtimeFuncType,
    GlobalType as WasmtimeGlobalType,
    MemoryType as WasmtimeMemoryType,
    TableType as WasmtimeTableType,
    ValType,
    Mutability,
    Engine,
    Module,
    Instance,
    Store,
    ExternType,
    ImportType as WasmtimeImportType,
    ExportType as WasmtimeExportType,
};
use crate::error::{WasmtimeError, WasmtimeResult};
use std::collections::HashMap;

/// Represents WebAssembly value types with conversion capabilities
#[repr(C)]
#[derive(Debug, Clone, Copy, PartialEq)]
pub enum IntrospectionValueType {
    I32 = 0,
    I64 = 1,
    F32 = 2,
    F64 = 3,
    V128 = 4,
    FuncRef = 5,
    ExternRef = 6,
}

impl From<ValType> for IntrospectionValueType {
    fn from(val_type: ValType) -> Self {
        match val_type {
            ValType::I32 => IntrospectionValueType::I32,
            ValType::I64 => IntrospectionValueType::I64,
            ValType::F32 => IntrospectionValueType::F32,
            ValType::F64 => IntrospectionValueType::F64,
            ValType::V128 => IntrospectionValueType::V128,
            ValType::FuncRef => IntrospectionValueType::FuncRef,
            ValType::ExternRef => IntrospectionValueType::ExternRef,
        }
    }
}

impl From<IntrospectionValueType> for ValType {
    fn from(value_type: IntrospectionValueType) -> Self {
        match value_type {
            IntrospectionValueType::I32 => ValType::I32,
            IntrospectionValueType::I64 => ValType::I64,
            IntrospectionValueType::F32 => ValType::F32,
            IntrospectionValueType::F64 => ValType::F64,
            IntrospectionValueType::V128 => ValType::V128,
            IntrospectionValueType::FuncRef => ValType::FuncRef,
            IntrospectionValueType::ExternRef => ValType::ExternRef,
        }
    }
}

/// Memory type information
#[repr(C)]
#[derive(Debug, Clone)]
pub struct MemoryTypeInfo {
    pub minimum: u64,
    pub maximum: Option<u64>,
    pub is_64_bit: bool,
    pub is_shared: bool,
}

impl From<WasmtimeMemoryType> for MemoryTypeInfo {
    fn from(memory_type: WasmtimeMemoryType) -> Self {
        Self {
            minimum: memory_type.minimum(),
            maximum: memory_type.maximum(),
            is_64_bit: memory_type.is_64(),
            is_shared: memory_type.is_shared(),
        }
    }
}

/// Table type information
#[repr(C)]
#[derive(Debug, Clone)]
pub struct TableTypeInfo {
    pub element_type: IntrospectionValueType,
    pub minimum: u32,
    pub maximum: Option<u32>,
}

impl From<WasmtimeTableType> for TableTypeInfo {
    fn from(table_type: WasmtimeTableType) -> Self {
        Self {
            element_type: table_type.element().into(),
            minimum: table_type.minimum(),
            maximum: table_type.maximum(),
        }
    }
}

/// Global type information
#[repr(C)]
#[derive(Debug, Clone)]
pub struct GlobalTypeInfo {
    pub value_type: IntrospectionValueType,
    pub is_mutable: bool,
}

impl From<WasmtimeGlobalType> for GlobalTypeInfo {
    fn from(global_type: WasmtimeGlobalType) -> Self {
        Self {
            value_type: global_type.content().into(),
            is_mutable: global_type.mutability() == Mutability::Var,
        }
    }
}

/// Function type information
#[repr(C)]
#[derive(Debug, Clone)]
pub struct FuncTypeInfo {
    pub params: Vec<IntrospectionValueType>,
    pub results: Vec<IntrospectionValueType>,
}

impl From<WasmtimeFuncType> for FuncTypeInfo {
    fn from(func_type: WasmtimeFuncType) -> Self {
        Self {
            params: func_type.params().map(|p| p.into()).collect(),
            results: func_type.results().map(|r| r.into()).collect(),
        }
    }
}

/// Type kind enumeration
#[repr(C)]
#[derive(Debug, Clone, Copy, PartialEq)]
pub enum TypeKind {
    Function = 0,
    Table = 1,
    Memory = 2,
    Global = 3,
}

/// Import descriptor with detailed type information
#[repr(C)]
#[derive(Debug, Clone)]
pub struct ImportDescriptorInfo {
    pub module_name: String,
    pub field_name: String,
    pub type_kind: TypeKind,
    pub func_type: Option<FuncTypeInfo>,
    pub table_type: Option<TableTypeInfo>,
    pub memory_type: Option<MemoryTypeInfo>,
    pub global_type: Option<GlobalTypeInfo>,
}

impl From<WasmtimeImportType<'_>> for ImportDescriptorInfo {
    fn from(import_type: WasmtimeImportType<'_>) -> Self {
        let mut descriptor = Self {
            module_name: import_type.module().to_string(),
            field_name: import_type.name().to_string(),
            type_kind: TypeKind::Function, // Will be set correctly below
            func_type: None,
            table_type: None,
            memory_type: None,
            global_type: None,
        };

        match import_type.ty() {
            ExternType::Func(func_type) => {
                descriptor.type_kind = TypeKind::Function;
                descriptor.func_type = Some(func_type.into());
            }
            ExternType::Table(table_type) => {
                descriptor.type_kind = TypeKind::Table;
                descriptor.table_type = Some(table_type.into());
            }
            ExternType::Memory(memory_type) => {
                descriptor.type_kind = TypeKind::Memory;
                descriptor.memory_type = Some(memory_type.into());
            }
            ExternType::Global(global_type) => {
                descriptor.type_kind = TypeKind::Global;
                descriptor.global_type = Some(global_type.into());
            }
            ExternType::Tag(_) => {
                return Err(WasmtimeError::Validation {
                    message: "Tag imports not supported".to_string(),
                });
            }
        }

        descriptor
    }
}

/// Export descriptor with detailed type information
#[repr(C)]
#[derive(Debug, Clone)]
pub struct ExportDescriptorInfo {
    pub field_name: String,
    pub type_kind: TypeKind,
    pub func_type: Option<FuncTypeInfo>,
    pub table_type: Option<TableTypeInfo>,
    pub memory_type: Option<MemoryTypeInfo>,
    pub global_type: Option<GlobalTypeInfo>,
}

impl From<WasmtimeExportType<'_>> for ExportDescriptorInfo {
    fn from(export_type: WasmtimeExportType<'_>) -> Self {
        let mut descriptor = Self {
            field_name: export_type.name().to_string(),
            type_kind: TypeKind::Function, // Will be set correctly below
            func_type: None,
            table_type: None,
            memory_type: None,
            global_type: None,
        };

        match export_type.ty() {
            ExternType::Func(func_type) => {
                descriptor.type_kind = TypeKind::Function;
                descriptor.func_type = Some(func_type.into());
            }
            ExternType::Table(table_type) => {
                descriptor.type_kind = TypeKind::Table;
                descriptor.table_type = Some(table_type.into());
            }
            ExternType::Memory(memory_type) => {
                descriptor.type_kind = TypeKind::Memory;
                descriptor.memory_type = Some(memory_type.into());
            }
            ExternType::Global(global_type) => {
                descriptor.type_kind = TypeKind::Global;
                descriptor.global_type = Some(global_type.into());
            }
            ExternType::Tag(_) => {
                return Err(WasmtimeError::Validation {
                    message: "Tag exports not supported".to_string(),
                });
            }
        }

        descriptor
    }
}

/// Type introspection capabilities for modules
pub struct ModuleTypeIntrospector;

impl ModuleTypeIntrospector {
    /// Extract all import descriptors from a module
    pub fn get_import_descriptors(module: &Module) -> WasmtimeResult<Vec<ImportDescriptorInfo>> {
        let descriptors = module
            .imports()
            .map(|import| ImportDescriptorInfo::from(import))
            .collect();
        Ok(descriptors)
    }

    /// Extract all export descriptors from a module
    pub fn get_export_descriptors(module: &Module) -> WasmtimeResult<Vec<ExportDescriptorInfo>> {
        let descriptors = module
            .exports()
            .map(|export| ExportDescriptorInfo::from(export))
            .collect();
        Ok(descriptors)
    }

    /// Get function type by index
    pub fn get_function_type(module: &Module, index: u32) -> WasmtimeResult<Option<FuncTypeInfo>> {
        // Note: Wasmtime doesn't expose function types by index directly in the public API
        // We'll need to iterate through exports to find function types
        for export in module.exports() {
            if let ExternType::Func(func_type) = export.ty() {
                // This is a simplified implementation - in practice, we'd need
                // a more sophisticated mapping system
                return Ok(Some(func_type.into()));
            }
        }
        Ok(None)
    }

    /// Check if module has specific import
    pub fn has_import(module: &Module, module_name: &str, field_name: &str) -> bool {
        module.imports().any(|import| {
            import.module() == module_name && import.name() == field_name
        })
    }

    /// Check if module has specific export
    pub fn has_export(module: &Module, field_name: &str) -> bool {
        module.exports().any(|export| export.name() == field_name)
    }
}

/// Type introspection capabilities for instances
pub struct InstanceTypeIntrospector;

impl InstanceTypeIntrospector {
    /// Get runtime type information for an export
    pub fn get_export_type(
        instance: &Instance,
        store: &mut Store<()>,
        name: &str,
    ) -> WasmtimeResult<Option<ExportDescriptorInfo>> {
        if let Some(export) = instance.get_export(&mut *store, name) {
            let descriptor = match export {
                wasmtime::Extern::Func(func) => {
                    let func_type = func.ty(&*store);
                    ExportDescriptorInfo {
                        field_name: name.to_string(),
                        type_kind: TypeKind::Function,
                        func_type: Some(func_type.into()),
                        table_type: None,
                        memory_type: None,
                        global_type: None,
                    }
                }
                wasmtime::Extern::Table(table) => {
                    let table_type = table.ty(&*store);
                    ExportDescriptorInfo {
                        field_name: name.to_string(),
                        type_kind: TypeKind::Table,
                        func_type: None,
                        table_type: Some(table_type.into()),
                        memory_type: None,
                        global_type: None,
                    }
                }
                wasmtime::Extern::Memory(memory) => {
                    let memory_type = memory.ty(&*store);
                    ExportDescriptorInfo {
                        field_name: name.to_string(),
                        type_kind: TypeKind::Memory,
                        func_type: None,
                        table_type: None,
                        memory_type: Some(memory_type.into()),
                        global_type: None,
                    }
                }
                wasmtime::Extern::Global(global) => {
                    let global_type = global.ty(&*store);
                    ExportDescriptorInfo {
                        field_name: name.to_string(),
                        type_kind: TypeKind::Global,
                        func_type: None,
                        table_type: None,
                        memory_type: None,
                        global_type: Some(global_type.into()),
                    }
                }
                wasmtime::Extern::SharedMemory(_) => {
                    return Err(WasmtimeError::Validation {
                        message: "SharedMemory exports not supported".to_string(),
                    });
                }
                wasmtime::Extern::Tag(_) => {
                    return Err(WasmtimeError::Validation {
                        message: "Tag exports not supported".to_string(),
                    });
                }
            };
            Ok(Some(descriptor))
        } else {
            Ok(None)
        }
    }

    /// Get all runtime export types
    pub fn get_all_export_types(
        instance: &Instance,
        store: &mut Store<()>,
    ) -> WasmtimeResult<HashMap<String, ExportDescriptorInfo>> {
        let mut exports = HashMap::new();

        for export in instance.exports(store) {
            let name = export.0.to_string();
            if let Some(descriptor) = Self::get_export_type(instance, store, &name)? {
                exports.insert(name, descriptor);
            }
        }

        Ok(exports)
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use wasmtime::{Engine, Module, Store};

    #[test]
    fn test_value_type_conversion() {
        assert_eq!(IntrospectionValueType::from(ValType::I32), IntrospectionValueType::I32);
        assert_eq!(IntrospectionValueType::from(ValType::F64), IntrospectionValueType::F64);
        assert_eq!(IntrospectionValueType::from(ValType::FuncRef), IntrospectionValueType::FuncRef);

        assert_eq!(ValType::from(IntrospectionValueType::I64), ValType::I64);
        assert_eq!(ValType::from(IntrospectionValueType::F32), ValType::F32);
        assert_eq!(ValType::from(IntrospectionValueType::ExternRef), ValType::ExternRef);
    }

    #[test]
    fn test_memory_type_info_conversion() {
        let engine = Engine::default();
        let memory_type = WasmtimeMemoryType::new(1, Some(10));
        let memory_info = MemoryTypeInfo::from(memory_type);

        assert_eq!(memory_info.minimum, 1);
        assert_eq!(memory_info.maximum, Some(10));
        assert!(!memory_info.is_64_bit);
        assert!(!memory_info.is_shared);
    }

    #[test]
    fn test_module_introspection() {
        let engine = Engine::default();

        // Simple WAT module with imports and exports
        let wat = r#"
            (module
                (import "env" "memory" (memory 1))
                (import "env" "table" (table 1 funcref))
                (func $add (param i32 i32) (result i32)
                    local.get 0
                    local.get 1
                    i32.add)
                (export "add" (func $add))
            )
        "#;

        let module = Module::new(&engine, wat).unwrap();

        let imports = ModuleTypeIntrospector::get_import_descriptors(&module).unwrap();
        assert_eq!(imports.len(), 2);

        let exports = ModuleTypeIntrospector::get_export_descriptors(&module).unwrap();
        assert_eq!(exports.len(), 1);
        assert_eq!(exports[0].field_name, "add");
        assert_eq!(exports[0].type_kind, TypeKind::Function);
    }
}