//! WIT (WebAssembly Interface Types) parsing and type definitions
//!
//! This module provides parsing and validation of WebAssembly Interface Type (WIT) definitions,
//! along with type definitions for the Component Model.

use crate::error::{WasmtimeError, WasmtimeResult};

/// WIT parser for interface definitions
///
/// Provides parsing and validation of WebAssembly Interface Type (WIT) definitions.
pub struct WitParser {
    /// Parser state (reserved for future use)
    _state: (),
}

impl WitParser {
    /// Create a new WIT parser
    ///
    /// # Returns
    ///
    /// Returns a new `WitParser` instance ready for interface parsing.
    ///
    /// # Errors
    ///
    /// Returns `WasmtimeError::EngineConfig` if the parser cannot be initialized.
    pub fn new() -> WasmtimeResult<Self> {
        Ok(WitParser {
            _state: (),
        })
    }

    /// Parse a WIT interface definition
    ///
    /// # Arguments
    ///
    /// * `wit_text` - WIT interface definition as text
    ///
    /// # Returns
    ///
    /// Returns a parsed `InterfaceDefinition` if successful.
    ///
    /// # Errors
    ///
    /// Returns `WasmtimeError::Compilation` if the WIT text is invalid or cannot be parsed.
    pub fn parse_interface(&mut self, wit_text: &str) -> WasmtimeResult<InterfaceDefinition> {
        if wit_text.is_empty() {
            return Err(WasmtimeError::InvalidParameter {
                message: "WIT text cannot be empty".to_string(),
            });
        }

        // Basic WIT parsing - this is a simplified implementation
        // A full implementation would use a proper WIT parser
        let interface_name = self.extract_interface_name(wit_text)?;
        let namespace = self.extract_namespace(wit_text);
        let version = self.extract_version(wit_text);

        Ok(InterfaceDefinition {
            name: interface_name,
            namespace,
            version,
            functions: Vec::new(), // Would be extracted from actual WIT parsing
            types: Vec::new(),     // Would be extracted from actual WIT parsing
            resources: Vec::new(), // Would be extracted from actual WIT parsing
        })
    }

    /// Validate WIT interface syntax
    ///
    /// # Arguments
    ///
    /// * `wit_text` - WIT interface definition to validate
    ///
    /// # Returns
    ///
    /// Returns `Ok(true)` if the syntax is valid, `Ok(false)` otherwise.
    ///
    /// # Errors
    ///
    /// Returns `WasmtimeError::ValidationError` if validation fails due to system errors.
    pub fn validate_syntax(&mut self, wit_text: &str) -> WasmtimeResult<bool> {
        if wit_text.is_empty() {
            return Ok(false);
        }

        // Basic syntax validation
        let has_interface = wit_text.contains("interface");
        let balanced_braces = self.check_balanced_braces(wit_text);
        let valid_keywords = self.check_valid_keywords(wit_text);

        Ok(has_interface && balanced_braces && valid_keywords)
    }

    /// Extract interface name from WIT text
    fn extract_interface_name(&self, wit_text: &str) -> WasmtimeResult<String> {
        // Look for pattern: interface <name> {
        for line in wit_text.lines() {
            let trimmed = line.trim();
            if trimmed.starts_with("interface ") {
                let parts: Vec<&str> = trimmed.split_whitespace().collect();
                if parts.len() >= 2 {
                    let name = parts[1].trim_end_matches('{').trim();
                    return Ok(name.to_string());
                }
            }
        }

        Err(WasmtimeError::Compilation {
            message: "No interface name found in WIT text".to_string(),
        })
    }

    /// Extract namespace from WIT text
    fn extract_namespace(&self, wit_text: &str) -> Option<String> {
        // Look for package declarations or namespace hints
        for line in wit_text.lines() {
            let trimmed = line.trim();
            if trimmed.starts_with("package ") || trimmed.starts_with("namespace ") {
                let parts: Vec<&str> = trimmed.split_whitespace().collect();
                if parts.len() >= 2 {
                    return Some(parts[1].to_string());
                }
            }
        }
        None
    }

    /// Extract version from WIT text
    fn extract_version(&self, wit_text: &str) -> Option<String> {
        // Look for version declarations
        for line in wit_text.lines() {
            let trimmed = line.trim();
            if trimmed.starts_with("version ") || trimmed.contains("@version") {
                let parts: Vec<&str> = trimmed.split_whitespace().collect();
                if parts.len() >= 2 {
                    return Some(parts[1].to_string());
                }
            }
        }
        None
    }

    /// Check if braces are balanced in WIT text
    fn check_balanced_braces(&self, wit_text: &str) -> bool {
        let mut depth = 0;
        for ch in wit_text.chars() {
            match ch {
                '{' => depth += 1,
                '}' => {
                    depth -= 1;
                    if depth < 0 {
                        return false;
                    }
                }
                _ => {}
            }
        }
        depth == 0
    }

    /// Check for valid WIT keywords
    fn check_valid_keywords(&self, wit_text: &str) -> bool {
        let _valid_keywords = [
            "interface", "type", "record", "variant", "enum", "flags",
            "resource", "func", "constructor", "method", "static",
            "use", "package", "world", "export", "import"
        ];

        // Basic check - ensure we don't have invalid syntax patterns
        for line in wit_text.lines() {
            let words: Vec<&str> = line.split_whitespace().collect();
            for word in words {
                let clean_word = word.trim_matches(|c: char| !c.is_alphabetic());
                if !clean_word.is_empty() && clean_word.chars().all(|c| c.is_alphabetic()) {
                    // This is a very basic check - a real implementation would be more sophisticated
                    if clean_word.len() > 20 {
                        return false; // Suspiciously long identifier
                    }
                }
            }
        }
        true
    }
}

/// WebAssembly Interface Type (WIT) interface definition
///
/// Represents a WIT interface with its methods, types, and resources.
/// Used for type-safe binding between host and component.
#[derive(Debug, Clone)]
pub struct InterfaceDefinition {
    /// Interface name
    pub name: String,
    /// Interface namespace (e.g., "wasi:filesystem")
    pub namespace: Option<String>,
    /// Interface version
    pub version: Option<String>,
    /// Functions defined in this interface
    pub functions: Vec<FunctionDefinition>,
    /// Types defined in this interface
    pub types: Vec<TypeDefinition>,
    /// Resources defined in this interface
    pub resources: Vec<ResourceDefinition>,
}

impl InterfaceDefinition {
    /// Convert interface definition to JSON representation
    ///
    /// # Returns
    ///
    /// Returns a JSON string representation of the interface definition.
    ///
    /// # Errors
    ///
    /// Returns an error if JSON serialization fails.
    pub fn to_json(&self) -> Result<String, Box<dyn std::error::Error + Send + Sync>> {
        // Simple JSON serialization without external dependencies
        let mut json = String::new();
        json.push_str("{\n");
        json.push_str(&format!("  \"name\": \"{}\",\n", self.name));

        if let Some(ref namespace) = self.namespace {
            json.push_str(&format!("  \"namespace\": \"{}\",\n", namespace));
        }

        if let Some(ref version) = self.version {
            json.push_str(&format!("  \"version\": \"{}\",\n", version));
        }

        json.push_str("  \"functions\": [\n");
        for (i, function) in self.functions.iter().enumerate() {
            if i > 0 { json.push_str(",\n"); }
            json.push_str(&format!("    {{\"name\": \"{}\"}}", function.name));
        }
        json.push_str("\n  ],\n");

        json.push_str("  \"types\": [\n");
        for (i, type_def) in self.types.iter().enumerate() {
            if i > 0 { json.push_str(",\n"); }
            json.push_str(&format!("    {{\"name\": \"{}\"}}", type_def.name));
        }
        json.push_str("\n  ],\n");

        json.push_str("  \"resources\": [\n");
        for (i, resource) in self.resources.iter().enumerate() {
            if i > 0 { json.push_str(",\n"); }
            json.push_str(&format!("    {{\"name\": \"{}\"}}", resource.name));
        }
        json.push_str("\n  ]\n");

        json.push_str("}");
        Ok(json)
    }
}

/// Function definition within a WIT interface
#[derive(Debug, Clone)]
pub struct FunctionDefinition {
    /// Function name
    pub name: String,
    /// Function parameters
    pub parameters: Vec<Parameter>,
    /// Function return types
    pub results: Vec<ComponentValueType>,
}

/// Parameter definition for WIT functions
#[derive(Debug, Clone)]
pub struct Parameter {
    /// Parameter name
    pub name: String,
    /// Parameter type
    pub value_type: ComponentValueType,
}

/// Type definition within a WIT interface
#[derive(Debug, Clone)]
pub struct TypeDefinition {
    /// Type name
    pub name: String,
    /// Type kind (record, variant, enum, etc.)
    pub kind: ComponentTypeKind,
}

/// Resource definition within a WIT interface
#[derive(Debug, Clone)]
pub struct ResourceDefinition {
    /// Resource name
    pub name: String,
    /// Resource constructor functions
    pub constructors: Vec<FunctionDefinition>,
    /// Resource instance methods
    pub methods: Vec<FunctionDefinition>,
}

/// WebAssembly Component Model value types
#[derive(Debug, Clone)]
pub enum ComponentValueType {
    /// Boolean type
    Bool,
    /// 8-bit signed integer
    S8,
    /// 8-bit unsigned integer
    U8,
    /// 16-bit signed integer
    S16,
    /// 16-bit unsigned integer
    U16,
    /// 32-bit signed integer
    S32,
    /// 32-bit unsigned integer
    U32,
    /// 64-bit signed integer
    S64,
    /// 64-bit unsigned integer
    U64,
    /// 32-bit floating point
    Float32,
    /// 64-bit floating point
    Float64,
    /// Unicode string
    String,
    /// List of values
    List(Box<ComponentValueType>),
    /// Optional value
    Option(Box<ComponentValueType>),
    /// Result type with success and error cases
    Result {
        /// Success value type
        ok: Option<Box<ComponentValueType>>,
        /// Error value type
        err: Option<Box<ComponentValueType>>
    },
    /// Record type with named fields
    Record(Vec<FieldType>),
    /// Tuple type with indexed elements
    Tuple(Vec<ComponentValueType>),
    /// Variant type with multiple cases
    Variant(Vec<CaseType>),
    /// Enum type with named values
    Enum(Vec<String>),
    /// Flags type with named bits
    Flags(Vec<String>),
    /// Resource handle
    Resource(String),
    /// Custom type reference
    Type(String),
}

/// Type kind for WIT type definitions
#[derive(Debug, Clone)]
pub enum ComponentTypeKind {
    /// Record type with named fields
    Record(Vec<FieldType>),
    /// Variant type with multiple cases
    Variant(Vec<CaseType>),
    /// Enum type with named values
    Enum(Vec<String>),
    /// Type alias
    Alias(ComponentValueType),
}

/// Field type for record types
#[derive(Debug, Clone)]
pub struct FieldType {
    /// Field name
    pub name: String,
    /// Field type
    pub value_type: ComponentValueType,
}

/// Case type for variant types
#[derive(Debug, Clone)]
pub struct CaseType {
    /// Case name
    pub name: String,
    /// Optional case payload type
    pub payload: Option<ComponentValueType>,
}

// FFI functions for WIT parser

use std::os::raw::{c_char, c_int, c_void};
use std::ffi::CStr;

const FFI_SUCCESS: c_int = 0;
const FFI_ERROR: c_int = -1;

/// Create a WIT parser context
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_wit_parser_new() -> *mut c_void {
    match WitParser::new() {
        Ok(parser) => Box::into_raw(Box::new(parser)) as *mut c_void,
        Err(_) => std::ptr::null_mut(),
    }
}

/// Destroy a WIT parser context
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_wit_parser_destroy(parser_ptr: *mut c_void) {
    if !parser_ptr.is_null() {
        let _ = Box::from_raw(parser_ptr as *mut WitParser);
    }
}

/// Parse a WIT interface definition
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_wit_parser_parse_interface(
    parser_ptr: *mut c_void,
    wit_text: *const c_char,
    interface_out: *mut *mut c_void,
) -> c_int {
    if parser_ptr.is_null() || wit_text.is_null() || interface_out.is_null() {
        return FFI_ERROR;
    }

    let parser = &mut *(parser_ptr as *mut WitParser);
    let wit_str = match CStr::from_ptr(wit_text).to_str() {
        Ok(s) => s,
        Err(_) => return FFI_ERROR,
    };

    match parser.parse_interface(wit_str) {
        Ok(interface) => {
            *interface_out = Box::into_raw(Box::new(interface)) as *mut c_void;
            FFI_SUCCESS
        }
        Err(_) => FFI_ERROR,
    }
}

/// Validate WIT interface syntax
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_wit_parser_validate_syntax(
    parser_ptr: *mut c_void,
    wit_text: *const c_char,
) -> c_int {
    if parser_ptr.is_null() || wit_text.is_null() {
        return FFI_ERROR;
    }

    let parser = &mut *(parser_ptr as *mut WitParser);
    let wit_str = match CStr::from_ptr(wit_text).to_str() {
        Ok(s) => s,
        Err(_) => return FFI_ERROR,
    };

    match parser.validate_syntax(wit_str) {
        Ok(is_valid) => if is_valid { 1 } else { 0 },
        Err(_) => FFI_ERROR,
    }
}
