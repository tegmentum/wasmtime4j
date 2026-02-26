//! CodeBuilder FFI wrapper for stateful WebAssembly compilation.
//!
//! Wraps `wasmtime::CodeBuilder` with a handle-based API for use across FFI boundaries.
//! The builder accumulates configuration (wasm bytes, DWARF packages, hints) and then
//! compiles to a module or component.

use crate::engine::Engine;
use crate::error::{WasmtimeError, WasmtimeResult};
use crate::module::{Module, ModuleMetadata};

#[cfg(feature = "component-model")]
use crate::component::Component;

/// Opaque state held across FFI boundary for a CodeBuilder in progress.
///
/// Because `wasmtime::CodeBuilder` borrows the engine, we cannot store it directly.
/// Instead we store the inputs and replay them at compile time.
pub struct CodeBuilderState {
    engine: Engine,
    wasm_bytes: Option<Vec<u8>>,
    is_text: bool,
    dwarf_package: Option<Vec<u8>>,
    hint: Option<CodeHintKind>,
}

/// Mirrors Wasmtime's CodeHint to indicate expected code type.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum CodeHintKind {
    /// Expected to be a module.
    Module,
    /// Expected to be a component.
    Component,
}

/// Helper to configure a wasmtime CodeBuilder from stored state.
fn configure_builder<'a>(
    builder: &mut wasmtime::CodeBuilder<'a>,
    wasm_bytes: &'a [u8],
    is_text: bool,
    dwarf_package: Option<&'a [u8]>,
) -> WasmtimeResult<()> {
    if is_text {
        builder
            .wasm_binary_or_text(wasm_bytes, None)
            .map_err(|e| WasmtimeError::from_compilation_error(e))?;
    } else {
        builder
            .wasm_binary(wasm_bytes, None)
            .map_err(|e| WasmtimeError::from_compilation_error(e))?;
    }

    if let Some(dwarf) = dwarf_package {
        builder
            .dwarf_package(dwarf)
            .map_err(|e| WasmtimeError::from_compilation_error(e))?;
    }

    Ok(())
}

impl CodeBuilderState {
    /// Create a new CodeBuilder for the given engine.
    pub fn new(engine: &Engine) -> WasmtimeResult<Self> {
        engine.validate()?;
        Ok(CodeBuilderState {
            engine: engine.clone(),
            wasm_bytes: None,
            is_text: false,
            dwarf_package: None,
            hint: None,
        })
    }

    /// Set the WebAssembly binary bytes.
    pub fn wasm_binary(&mut self, bytes: Vec<u8>) -> WasmtimeResult<()> {
        if bytes.is_empty() {
            return Err(WasmtimeError::InvalidParameter {
                message: "WebAssembly bytes cannot be empty".to_string(),
            });
        }
        self.wasm_bytes = Some(bytes);
        self.is_text = false;
        Ok(())
    }

    /// Set WebAssembly binary or text (WAT) bytes.
    pub fn wasm_binary_or_text(&mut self, bytes: Vec<u8>) -> WasmtimeResult<()> {
        if bytes.is_empty() {
            return Err(WasmtimeError::InvalidParameter {
                message: "WebAssembly bytes cannot be empty".to_string(),
            });
        }
        self.wasm_bytes = Some(bytes);
        self.is_text = true;
        Ok(())
    }

    /// Set the DWARF debug package bytes.
    pub fn dwarf_package(&mut self, bytes: Vec<u8>) -> WasmtimeResult<()> {
        if bytes.is_empty() {
            return Err(WasmtimeError::InvalidParameter {
                message: "DWARF package bytes cannot be empty".to_string(),
            });
        }
        self.dwarf_package = Some(bytes);
        Ok(())
    }

    /// Set the compilation hint (informational — wasmtime auto-detects from bytes).
    pub fn hint(&mut self, hint: CodeHintKind) {
        self.hint = Some(hint);
    }

    /// Build the wasmtime CodeBuilder, serialize, then deserialize to get a Module.
    ///
    /// Wasmtime's `CodeBuilder` only exposes `compile_module_serialized()`, so we
    /// serialize first and then deserialize to obtain a usable `wasmtime::Module`.
    pub fn compile_module(&self) -> WasmtimeResult<Box<Module>> {
        let wasm_bytes = self.wasm_bytes.as_ref().ok_or_else(|| {
            WasmtimeError::InvalidParameter {
                message: "No WebAssembly bytes set on CodeBuilder".to_string(),
            }
        })?;

        // First serialize via CodeBuilder
        let serialized = self.compile_module_serialized()?;

        // Then deserialize to get a wasmtime::Module
        let module = unsafe {
            wasmtime::Module::deserialize(self.engine.inner(), &serialized)
        }
        .map_err(|e| WasmtimeError::from_compilation_error(e))?;

        // For deserialized modules, metadata extraction from the module itself is limited.
        // We use empty metadata since full extraction requires the original wasm bytes
        // to be parsed by the module (which we have), but the deserialized module
        // may not support all introspection.
        let metadata = match ModuleMetadata::extract(&module, wasm_bytes.len(), wasm_bytes) {
            Ok(m) => m,
            Err(_) => ModuleMetadata::empty(),
        };

        Ok(Box::new(Module::from_wasmtime_module(
            module,
            self.engine.clone(),
            metadata,
        )))
    }

    /// Build the wasmtime CodeBuilder and compile a module to serialized bytes.
    pub fn compile_module_serialized(&self) -> WasmtimeResult<Vec<u8>> {
        let wasm_bytes = self.wasm_bytes.as_ref().ok_or_else(|| {
            WasmtimeError::InvalidParameter {
                message: "No WebAssembly bytes set on CodeBuilder".to_string(),
            }
        })?;

        self.engine.validate()?;
        let _compile_guard = self.engine.acquire_compile_lock();

        let mut builder = wasmtime::CodeBuilder::new(self.engine.inner());
        configure_builder(
            &mut builder,
            wasm_bytes,
            self.is_text,
            self.dwarf_package.as_deref(),
        )?;

        builder
            .compile_module_serialized()
            .map_err(|e| WasmtimeError::from_compilation_error(e))
    }

    /// Build the wasmtime CodeBuilder, serialize, then deserialize to get a Component.
    #[cfg(feature = "component-model")]
    pub fn compile_component(&self) -> WasmtimeResult<Box<Component>> {
        let wasm_bytes = self.wasm_bytes.as_ref().ok_or_else(|| {
            WasmtimeError::InvalidParameter {
                message: "No WebAssembly bytes set on CodeBuilder".to_string(),
            }
        })?;

        // First serialize via CodeBuilder
        let serialized = self.compile_component_serialized()?;

        // Then deserialize to get a wasmtime::component::Component
        let component = unsafe {
            wasmtime::component::Component::deserialize(self.engine.inner(), &serialized)
        }
        .map_err(|e| WasmtimeError::from_compilation_error(e))?;

        let metadata = crate::component::ComponentMetadata {
            imports: Vec::new(),
            exports: Vec::new(),
            size_bytes: wasm_bytes.len(),
        };

        Ok(Box::new(Component::new(
            component,
            metadata,
            wasm_bytes.clone(),
        )))
    }

    /// Build the wasmtime CodeBuilder and compile a component to serialized bytes.
    #[cfg(feature = "component-model")]
    pub fn compile_component_serialized(&self) -> WasmtimeResult<Vec<u8>> {
        let wasm_bytes = self.wasm_bytes.as_ref().ok_or_else(|| {
            WasmtimeError::InvalidParameter {
                message: "No WebAssembly bytes set on CodeBuilder".to_string(),
            }
        })?;

        self.engine.validate()?;
        let _compile_guard = self.engine.acquire_compile_lock();

        let mut builder = wasmtime::CodeBuilder::new(self.engine.inner());
        configure_builder(
            &mut builder,
            wasm_bytes,
            self.is_text,
            self.dwarf_package.as_deref(),
        )?;

        builder
            .compile_component_serialized()
            .map_err(|e| WasmtimeError::from_compilation_error(e))
    }
}

// ==================== Core FFI Functions ====================

/// Create a new CodeBuilder for the given engine.
pub fn code_builder_new(engine: &Engine) -> WasmtimeResult<Box<CodeBuilderState>> {
    CodeBuilderState::new(engine).map(Box::new)
}

/// Set wasm binary bytes on the builder.
pub fn code_builder_wasm_binary(
    builder: &mut CodeBuilderState,
    bytes: Vec<u8>,
) -> WasmtimeResult<()> {
    builder.wasm_binary(bytes)
}

/// Set wasm binary or text bytes on the builder.
pub fn code_builder_wasm_binary_or_text(
    builder: &mut CodeBuilderState,
    bytes: Vec<u8>,
) -> WasmtimeResult<()> {
    builder.wasm_binary_or_text(bytes)
}

/// Set DWARF package bytes on the builder.
pub fn code_builder_dwarf_package(
    builder: &mut CodeBuilderState,
    bytes: Vec<u8>,
) -> WasmtimeResult<()> {
    builder.dwarf_package(bytes)
}

/// Set hint on the builder.
pub fn code_builder_hint(builder: &mut CodeBuilderState, hint_ordinal: i32) {
    let hint = match hint_ordinal {
        0 => CodeHintKind::Module,
        1 => CodeHintKind::Component,
        _ => return, // Ignore unknown hints
    };
    builder.hint(hint);
}

/// Compile module from builder.
pub fn code_builder_compile_module(
    builder: &CodeBuilderState,
) -> WasmtimeResult<Box<Module>> {
    builder.compile_module()
}

/// Compile module serialized from builder.
pub fn code_builder_compile_module_serialized(
    builder: &CodeBuilderState,
) -> WasmtimeResult<Vec<u8>> {
    builder.compile_module_serialized()
}

/// Compile component from builder.
#[cfg(feature = "component-model")]
pub fn code_builder_compile_component(
    builder: &CodeBuilderState,
) -> WasmtimeResult<Box<Component>> {
    builder.compile_component()
}

/// Compile component serialized from builder.
#[cfg(feature = "component-model")]
pub fn code_builder_compile_component_serialized(
    builder: &CodeBuilderState,
) -> WasmtimeResult<Vec<u8>> {
    builder.compile_component_serialized()
}

/// Destroy the code builder, releasing resources.
pub fn code_builder_destroy(builder: Box<CodeBuilderState>) {
    drop(builder);
}
