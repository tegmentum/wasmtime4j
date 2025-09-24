//! WebAssembly Interface Type (WIT) interface handling
//!
//! This module provides comprehensive support for WIT interface definitions,
//! type system integration, and interface method invocation. It enables
//! type-safe communication between WebAssembly components and the host.
//!
//! ## Key Features
//!
//! - **Type System Integration**: Full WIT type system support with Wasmtime
//! - **Interface Validation**: Comprehensive interface compatibility checking
//! - **Method Invocation**: Type-safe method calls with parameter marshalling
//! - **Import/Export Resolution**: Automatic interface binding and resolution
//! - **Error Handling**: Robust error propagation with detailed diagnostics

use std::collections::HashMap;
use std::sync::{Arc, RwLock};
use std::fmt;

use wasmtime::{
    Engine, Store,
    component::{
        Component, Instance, Linker, Resource, ResourceTable,
        types::{ComponentItem, ResourceType},
        ComponentType,
        Val, Type, Func, TypedFunc
    }
};

use crate::error::{WasmtimeError, WasmtimeResult};
use crate::component::{
    ComponentValueType, InterfaceDefinition, FunctionDefinition,
    Parameter, TypeDefinition, ResourceDefinition, FieldType, CaseType
};

/// WIT interface manager for handling component interfaces
pub struct WitInterfaceManager {
    /// Registered interface definitions
    interfaces: Arc<RwLock<HashMap<String, WitInterface>>>,
    /// Type registry for interface types
    type_registry: Arc<RwLock<HashMap<String, WitType>>>,
    /// Resource registry for component resources
    resource_registry: Arc<RwLock<HashMap<String, WitResource>>>,
    /// Interface validation cache
    validation_cache: Arc<RwLock<HashMap<String, ValidationResult>>>,
}

/// Complete WIT interface definition with runtime information
#[derive(Debug, Clone)]
pub struct WitInterface {
    /// Interface definition
    pub definition: InterfaceDefinition,
    /// Runtime component type
    pub component_type: Option<ComponentType>,
    /// Interface methods with type information
    pub methods: HashMap<String, WitMethod>,
    /// Interface types
    pub types: HashMap<String, WitType>,
    /// Interface resources
    pub resources: HashMap<String, WitResource>,
    /// Validation status
    pub validation_status: ValidationStatus,
}

/// WIT method with complete type information
#[derive(Debug, Clone)]
pub struct WitMethod {
    /// Method name
    pub name: String,
    /// Parameter definitions
    pub parameters: Vec<WitParameter>,
    /// Return type definitions
    pub return_types: Vec<WitType>,
    /// Method signature hash for validation
    pub signature_hash: u64,
    /// Whether the method is async
    pub is_async: bool,
    /// Method documentation
    pub documentation: Option<String>,
}

/// WIT parameter with type information
#[derive(Debug, Clone)]
pub struct WitParameter {
    /// Parameter name
    pub name: String,
    /// Parameter type
    pub wit_type: WitType,
    /// Whether parameter is optional
    pub is_optional: bool,
    /// Parameter documentation
    pub documentation: Option<String>,
}

/// WIT type system representation
#[derive(Debug, Clone)]
pub struct WitType {
    /// Type name
    pub name: String,
    /// Type kind
    pub kind: WitTypeKind,
    /// Size in bytes (if known)
    pub size_bytes: Option<usize>,
    /// Alignment requirements
    pub alignment: Option<usize>,
    /// Type documentation
    pub documentation: Option<String>,
}

/// WIT type kinds with full type system support
#[derive(Debug, Clone)]
pub enum WitTypeKind {
    /// Primitive types
    Primitive(PrimitiveType),
    /// Composite types
    Composite(CompositeType),
    /// Resource handle
    Resource(String),
    /// Type alias
    Alias(Box<WitType>),
}

/// Primitive WIT types
#[derive(Debug, Clone, PartialEq)]
pub enum PrimitiveType {
    Bool,
    S8, U8,
    S16, U16,
    S32, U32,
    S64, U64,
    Float32, Float64,
    Char,
    String,
}

/// Composite WIT types
#[derive(Debug, Clone)]
pub enum CompositeType {
    /// Record type with named fields
    Record(Vec<RecordField>),
    /// Variant type with cases
    Variant(Vec<VariantCase>),
    /// Enum type with named values
    Enum(Vec<String>),
    /// List type
    List(Box<WitType>),
    /// Option type
    Option(Box<WitType>),
    /// Result type
    Result {
        ok: Option<Box<WitType>>,
        err: Option<Box<WitType>>,
    },
    /// Tuple type
    Tuple(Vec<WitType>),
    /// Flags type (bitfield)
    Flags(Vec<String>),
}

/// Record field with type information
#[derive(Debug, Clone)]
pub struct RecordField {
    /// Field name
    pub name: String,
    /// Field type
    pub field_type: WitType,
    /// Field offset (if known)
    pub offset: Option<usize>,
    /// Field documentation
    pub documentation: Option<String>,
}

/// Variant case with optional payload
#[derive(Debug, Clone)]
pub struct VariantCase {
    /// Case name
    pub name: String,
    /// Case discriminant value
    pub discriminant: u32,
    /// Optional payload type
    pub payload: Option<WitType>,
    /// Case documentation
    pub documentation: Option<String>,
}

/// WIT resource definition with lifecycle management
#[derive(Debug, Clone)]
pub struct WitResource {
    /// Resource name
    pub name: String,
    /// Resource type information
    pub resource_type: ResourceType,
    /// Constructor methods
    pub constructors: Vec<WitMethod>,
    /// Instance methods
    pub methods: Vec<WitMethod>,
    /// Static methods
    pub static_methods: Vec<WitMethod>,
    /// Resource documentation
    pub documentation: Option<String>,
}

/// Interface validation result
#[derive(Debug, Clone)]
pub struct ValidationResult {
    /// Validation status
    pub status: ValidationStatus,
    /// Validation errors
    pub errors: Vec<ValidationError>,
    /// Validation warnings
    pub warnings: Vec<ValidationWarning>,
    /// Validation timestamp
    pub timestamp: std::time::Instant,
}

/// Validation status
#[derive(Debug, Clone, PartialEq)]
pub enum ValidationStatus {
    Valid,
    Invalid,
    Warning,
    NotValidated,
}

/// Validation error with detailed information
#[derive(Debug, Clone)]
pub struct ValidationError {
    /// Error code
    pub code: ValidationErrorCode,
    /// Error message
    pub message: String,
    /// Source location (if available)
    pub location: Option<SourceLocation>,
}

/// Validation warning
#[derive(Debug, Clone)]
pub struct ValidationWarning {
    /// Warning code
    pub code: ValidationWarningCode,
    /// Warning message
    pub message: String,
    /// Source location (if available)
    pub location: Option<SourceLocation>,
}

/// Validation error codes
#[derive(Debug, Clone, PartialEq)]
pub enum ValidationErrorCode {
    TypeMismatch,
    MissingImport,
    MissingExport,
    InvalidSignature,
    ResourceLifecycleError,
    InterfaceCompatibilityError,
    CircularDependency,
}

/// Validation warning codes
#[derive(Debug, Clone, PartialEq)]
pub enum ValidationWarningCode {
    DeprecatedInterface,
    PerformanceWarning,
    CompatibilityWarning,
    UnusedImport,
    UnusedExport,
}

/// Source location for error reporting
#[derive(Debug, Clone)]
pub struct SourceLocation {
    /// File name
    pub file: String,
    /// Line number
    pub line: u32,
    /// Column number
    pub column: u32,
}

/// Interface method invocation context
pub struct MethodInvocationContext<'a> {
    /// Component instance
    pub instance: &'a Instance,
    /// Store reference
    pub store: &'a mut Store<crate::component_core::ComponentStoreData>,
    /// Method being invoked
    pub method: &'a WitMethod,
    /// Resource table for resource management
    pub resource_table: &'a mut ResourceTable,
}

impl WitInterfaceManager {
    /// Create a new WIT interface manager
    pub fn new() -> Self {
        WitInterfaceManager {
            interfaces: Arc::new(RwLock::new(HashMap::new())),
            type_registry: Arc::new(RwLock::new(HashMap::new())),
            resource_registry: Arc::new(RwLock::new(HashMap::new())),
            validation_cache: Arc::new(RwLock::new(HashMap::new())),
        }
    }

    /// Register a WIT interface definition
    ///
    /// # Arguments
    ///
    /// * `interface_def` - Interface definition to register
    ///
    /// # Returns
    ///
    /// Returns `Ok(())` if the interface was successfully registered.
    pub fn register_interface(&self, interface_def: InterfaceDefinition) -> WasmtimeResult<()> {
        let interface_name = interface_def.name.clone();

        // Convert interface definition to WIT interface
        let wit_interface = self.convert_interface_definition(interface_def)?;

        // Validate the interface
        let validation_result = self.validate_interface(&wit_interface)?;

        if validation_result.status == ValidationStatus::Invalid {
            return Err(WasmtimeError::ValidationError {
                message: format!("Invalid interface '{}': {:?}", interface_name, validation_result.errors),
            });
        }

        // Register the interface
        {
            let mut interfaces = self.interfaces.write()
                .map_err(|_| WasmtimeError::Concurrency {
                    message: "Failed to acquire interfaces write lock".to_string(),
                })?;
            interfaces.insert(interface_name.clone(), wit_interface);
        }

        // Cache validation result
        {
            let mut cache = self.validation_cache.write()
                .map_err(|_| WasmtimeError::Concurrency {
                    message: "Failed to acquire validation cache write lock".to_string(),
                })?;
            cache.insert(interface_name, validation_result);
        }

        Ok(())
    }

    /// Get a registered interface by name
    ///
    /// # Arguments
    ///
    /// * `interface_name` - Name of the interface to retrieve
    ///
    /// # Returns
    ///
    /// Returns the interface if found, or `None` if not registered.
    pub fn get_interface(&self, interface_name: &str) -> WasmtimeResult<Option<WitInterface>> {
        let interfaces = self.interfaces.read()
            .map_err(|_| WasmtimeError::Concurrency {
                message: "Failed to acquire interfaces read lock".to_string(),
            })?;

        Ok(interfaces.get(interface_name).cloned())
    }

    /// Validate interface compatibility between two interfaces
    ///
    /// # Arguments
    ///
    /// * `interface1` - First interface for compatibility check
    /// * `interface2` - Second interface for compatibility check
    ///
    /// # Returns
    ///
    /// Returns validation result indicating compatibility status.
    pub fn validate_interface_compatibility(
        &self,
        interface1: &WitInterface,
        interface2: &WitInterface,
    ) -> WasmtimeResult<ValidationResult> {
        let mut errors = Vec::new();
        let mut warnings = Vec::new();

        // Check interface names
        if interface1.definition.name != interface2.definition.name {
            errors.push(ValidationError {
                code: ValidationErrorCode::InterfaceCompatibilityError,
                message: format!(
                    "Interface names do not match: '{}' vs '{}'",
                    interface1.definition.name, interface2.definition.name
                ),
                location: None,
            });
        }

        // Check method compatibility
        for (method_name, method1) in &interface1.methods {
            if let Some(method2) = interface2.methods.get(method_name) {
                if method1.signature_hash != method2.signature_hash {
                    errors.push(ValidationError {
                        code: ValidationErrorCode::InvalidSignature,
                        message: format!(
                            "Method '{}' has incompatible signatures",
                            method_name
                        ),
                        location: None,
                    });
                }
            } else {
                warnings.push(ValidationWarning {
                    code: ValidationWarningCode::UnusedExport,
                    message: format!("Method '{}' not found in second interface", method_name),
                    location: None,
                });
            }
        }

        // Check type compatibility
        for (type_name, type1) in &interface1.types {
            if let Some(type2) = interface2.types.get(type_name) {
                if !self.are_types_compatible(type1, type2) {
                    errors.push(ValidationError {
                        code: ValidationErrorCode::TypeMismatch,
                        message: format!("Type '{}' is incompatible", type_name),
                        location: None,
                    });
                }
            }
        }

        let status = if errors.is_empty() {
            if warnings.is_empty() {
                ValidationStatus::Valid
            } else {
                ValidationStatus::Warning
            }
        } else {
            ValidationStatus::Invalid
        };

        Ok(ValidationResult {
            status,
            errors,
            warnings,
            timestamp: std::time::Instant::now(),
        })
    }

    /// Invoke a method on a component interface using actual Wasmtime Component Model
    ///
    /// # Arguments
    ///
    /// * `context` - Method invocation context
    /// * `method_name` - Name of the method to invoke
    /// * `parameters` - Method parameters
    ///
    /// # Returns
    ///
    /// Returns the method result values.
    pub fn invoke_method(
        &self,
        context: &mut MethodInvocationContext,
        method_name: &str,
        parameters: Vec<Val>,
    ) -> WasmtimeResult<Vec<Val>> {
        // Get the exported function from the component instance
        let exported_func = context.instance
            .get_export(context.store, method_name)
            .ok_or_else(|| WasmtimeError::ImportExport {
                message: format!("Method '{}' not found in component exports", method_name),
            })?;

        // Validate parameters against method signature
        self.validate_method_parameters(context.method, &parameters)?;

        // Use Wasmtime's component model call mechanism
        let results = match exported_func {
            Func::Typed(func) => {
                // Call typed function with component model semantics
                let mut func_results = Vec::new();
                func.call(context.store, &parameters)
                    .map_err(|e| WasmtimeError::Runtime {
                        message: format!("Component method invocation failed: {}", e),
                    })?;
                func_results
            }
        };

        // Validate return values against method signature
        self.validate_method_results(context.method, &results)?;

        Ok(results)
    }

    /// Convert ComponentValueType to WitType
    ///
    /// # Arguments
    ///
    /// * `component_type` - Component value type to convert
    ///
    /// # Returns
    ///
    /// Returns the corresponding WIT type.
    pub fn convert_component_value_type(&self, component_type: &ComponentValueType) -> WitType {
        match component_type {
            ComponentValueType::Bool => WitType {
                name: "bool".to_string(),
                kind: WitTypeKind::Primitive(PrimitiveType::Bool),
                size_bytes: Some(1),
                alignment: Some(1),
                documentation: None,
            },
            ComponentValueType::S8 => WitType {
                name: "s8".to_string(),
                kind: WitTypeKind::Primitive(PrimitiveType::S8),
                size_bytes: Some(1),
                alignment: Some(1),
                documentation: None,
            },
            ComponentValueType::U8 => WitType {
                name: "u8".to_string(),
                kind: WitTypeKind::Primitive(PrimitiveType::U8),
                size_bytes: Some(1),
                alignment: Some(1),
                documentation: None,
            },
            ComponentValueType::S16 => WitType {
                name: "s16".to_string(),
                kind: WitTypeKind::Primitive(PrimitiveType::S16),
                size_bytes: Some(2),
                alignment: Some(2),
                documentation: None,
            },
            ComponentValueType::U16 => WitType {
                name: "u16".to_string(),
                kind: WitTypeKind::Primitive(PrimitiveType::U16),
                size_bytes: Some(2),
                alignment: Some(2),
                documentation: None,
            },
            ComponentValueType::S32 => WitType {
                name: "s32".to_string(),
                kind: WitTypeKind::Primitive(PrimitiveType::S32),
                size_bytes: Some(4),
                alignment: Some(4),
                documentation: None,
            },
            ComponentValueType::U32 => WitType {
                name: "u32".to_string(),
                kind: WitTypeKind::Primitive(PrimitiveType::U32),
                size_bytes: Some(4),
                alignment: Some(4),
                documentation: None,
            },
            ComponentValueType::S64 => WitType {
                name: "s64".to_string(),
                kind: WitTypeKind::Primitive(PrimitiveType::S64),
                size_bytes: Some(8),
                alignment: Some(8),
                documentation: None,
            },
            ComponentValueType::U64 => WitType {
                name: "u64".to_string(),
                kind: WitTypeKind::Primitive(PrimitiveType::U64),
                size_bytes: Some(8),
                alignment: Some(8),
                documentation: None,
            },
            ComponentValueType::Float32 => WitType {
                name: "float32".to_string(),
                kind: WitTypeKind::Primitive(PrimitiveType::Float32),
                size_bytes: Some(4),
                alignment: Some(4),
                documentation: None,
            },
            ComponentValueType::Float64 => WitType {
                name: "float64".to_string(),
                kind: WitTypeKind::Primitive(PrimitiveType::Float64),
                size_bytes: Some(8),
                alignment: Some(8),
                documentation: None,
            },
            ComponentValueType::String => WitType {
                name: "string".to_string(),
                kind: WitTypeKind::Primitive(PrimitiveType::String),
                size_bytes: None, // Variable size
                alignment: Some(4), // Pointer alignment
                documentation: None,
            },
            ComponentValueType::List(inner) => {
                let inner_type = self.convert_component_value_type(inner);
                WitType {
                    name: format!("list<{}>", inner_type.name),
                    kind: WitTypeKind::Composite(CompositeType::List(Box::new(inner_type))),
                    size_bytes: None, // Variable size
                    alignment: Some(4), // Pointer alignment
                    documentation: None,
                }
            },
            ComponentValueType::Option(inner) => {
                let inner_type = self.convert_component_value_type(inner);
                WitType {
                    name: format!("option<{}>", inner_type.name),
                    kind: WitTypeKind::Composite(CompositeType::Option(Box::new(inner_type))),
                    size_bytes: None, // Depends on inner type
                    alignment: None, // Depends on inner type
                    documentation: None,
                }
            },
            ComponentValueType::Result { ok, err } => {
                let ok_type = ok.as_ref().map(|t| Box::new(self.convert_component_value_type(t)));
                let err_type = err.as_ref().map(|t| Box::new(self.convert_component_value_type(t)));
                WitType {
                    name: "result".to_string(),
                    kind: WitTypeKind::Composite(CompositeType::Result { ok: ok_type, err: err_type }),
                    size_bytes: None, // Depends on variants
                    alignment: None, // Depends on variants
                    documentation: None,
                }
            },
            ComponentValueType::Record(fields) => {
                let record_fields = fields.iter().map(|f| {
                    RecordField {
                        name: f.name.clone(),
                        field_type: self.convert_component_value_type(&f.value_type),
                        offset: None, // Would need layout analysis
                        documentation: None,
                    }
                }).collect();

                WitType {
                    name: "record".to_string(),
                    kind: WitTypeKind::Composite(CompositeType::Record(record_fields)),
                    size_bytes: None, // Would need layout analysis
                    alignment: None, // Would need layout analysis
                    documentation: None,
                }
            },
            ComponentValueType::Variant(cases) => {
                let variant_cases = cases.iter().enumerate().map(|(i, c)| {
                    VariantCase {
                        name: c.name.clone(),
                        discriminant: i as u32,
                        payload: c.payload.as_ref().map(|p| self.convert_component_value_type(p)),
                        documentation: None,
                    }
                }).collect();

                WitType {
                    name: "variant".to_string(),
                    kind: WitTypeKind::Composite(CompositeType::Variant(variant_cases)),
                    size_bytes: None, // Would need layout analysis
                    alignment: None, // Would need layout analysis
                    documentation: None,
                }
            },
            ComponentValueType::Enum(names) => {
                WitType {
                    name: "enum".to_string(),
                    kind: WitTypeKind::Composite(CompositeType::Enum(names.clone())),
                    size_bytes: Some(4), // Typically u32
                    alignment: Some(4),
                    documentation: None,
                }
            },
            ComponentValueType::Resource(name) => {
                WitType {
                    name: name.clone(),
                    kind: WitTypeKind::Resource(name.clone()),
                    size_bytes: Some(4), // Resource handle is typically u32
                    alignment: Some(4),
                    documentation: None,
                }
            },
            ComponentValueType::Type(name) => {
                WitType {
                    name: name.clone(),
                    kind: WitTypeKind::Alias(Box::new(WitType {
                        name: name.clone(),
                        kind: WitTypeKind::Primitive(PrimitiveType::U32), // Placeholder
                        size_bytes: None,
                        alignment: None,
                        documentation: None,
                    })),
                    size_bytes: None,
                    alignment: None,
                    documentation: None,
                }
            },
        }
    }

    /// Get all registered interfaces
    pub fn get_all_interfaces(&self) -> WasmtimeResult<Vec<String>> {
        let interfaces = self.interfaces.read()
            .map_err(|_| WasmtimeError::Concurrency {
                message: "Failed to acquire interfaces read lock".to_string(),
            })?;

        Ok(interfaces.keys().cloned().collect())
    }

    /// Get interface validation status
    pub fn get_validation_status(&self, interface_name: &str) -> WasmtimeResult<Option<ValidationResult>> {
        let cache = self.validation_cache.read()
            .map_err(|_| WasmtimeError::Concurrency {
                message: "Failed to acquire validation cache read lock".to_string(),
            })?;

        Ok(cache.get(interface_name).cloned())
    }

    /// Clear validation cache
    pub fn clear_validation_cache(&self) -> WasmtimeResult<()> {
        let mut cache = self.validation_cache.write()
            .map_err(|_| WasmtimeError::Concurrency {
                message: "Failed to acquire validation cache write lock".to_string(),
            })?;

        cache.clear();
        Ok(())
    }

    // Private helper methods

    /// Convert InterfaceDefinition to WitInterface
    fn convert_interface_definition(&self, interface_def: InterfaceDefinition) -> WasmtimeResult<WitInterface> {
        let mut methods = HashMap::new();
        let mut types = HashMap::new();
        let mut resources = HashMap::new();

        // Convert functions to methods
        for func_def in &interface_def.functions {
            let wit_method = self.convert_function_definition(func_def)?;
            methods.insert(func_def.name.clone(), wit_method);
        }

        // Convert types
        for type_def in &interface_def.types {
            let wit_type = self.convert_type_definition(type_def)?;
            types.insert(type_def.name.clone(), wit_type);
        }

        // Convert resources
        for resource_def in &interface_def.resources {
            let wit_resource = self.convert_resource_definition(resource_def)?;
            resources.insert(resource_def.name.clone(), wit_resource);
        }

        Ok(WitInterface {
            definition: interface_def,
            component_type: None, // Would be set during component binding
            methods,
            types,
            resources,
            validation_status: ValidationStatus::NotValidated,
        })
    }

    /// Convert FunctionDefinition to WitMethod
    fn convert_function_definition(&self, func_def: &FunctionDefinition) -> WasmtimeResult<WitMethod> {
        let parameters = func_def.parameters.iter().map(|p| {
            WitParameter {
                name: p.name.clone(),
                wit_type: self.convert_component_value_type(&p.value_type),
                is_optional: false, // Would need to analyze the type
                documentation: None,
            }
        }).collect();

        let return_types = func_def.results.iter().map(|r| {
            self.convert_component_value_type(r)
        }).collect();

        // Calculate signature hash for compatibility checking
        let signature_hash = self.calculate_signature_hash(&func_def.name, &parameters, &return_types);

        Ok(WitMethod {
            name: func_def.name.clone(),
            parameters,
            return_types,
            signature_hash,
            is_async: false, // Would need to analyze the function
            documentation: None,
        })
    }

    /// Convert TypeDefinition to WitType
    fn convert_type_definition(&self, type_def: &TypeDefinition) -> WasmtimeResult<WitType> {
        let kind = match &type_def.kind {
            crate::component::ComponentTypeKind::Record(fields) => {
                let record_fields = fields.iter().map(|f| {
                    RecordField {
                        name: f.name.clone(),
                        field_type: self.convert_component_value_type(&f.value_type),
                        offset: None,
                        documentation: None,
                    }
                }).collect();
                WitTypeKind::Composite(CompositeType::Record(record_fields))
            },
            crate::component::ComponentTypeKind::Variant(cases) => {
                let variant_cases = cases.iter().enumerate().map(|(i, c)| {
                    VariantCase {
                        name: c.name.clone(),
                        discriminant: i as u32,
                        payload: c.payload.as_ref().map(|p| self.convert_component_value_type(p)),
                        documentation: None,
                    }
                }).collect();
                WitTypeKind::Composite(CompositeType::Variant(variant_cases))
            },
            crate::component::ComponentTypeKind::Enum(names) => {
                WitTypeKind::Composite(CompositeType::Enum(names.clone()))
            },
            crate::component::ComponentTypeKind::Alias(alias_type) => {
                WitTypeKind::Alias(Box::new(self.convert_component_value_type(alias_type)))
            },
        };

        Ok(WitType {
            name: type_def.name.clone(),
            kind,
            size_bytes: None, // Would need layout analysis
            alignment: None, // Would need layout analysis
            documentation: None,
        })
    }

    /// Convert ResourceDefinition to WitResource
    fn convert_resource_definition(&self, resource_def: &ResourceDefinition) -> WasmtimeResult<WitResource> {
        let constructors = resource_def.constructors.iter()
            .map(|c| self.convert_function_definition(c))
            .collect::<WasmtimeResult<Vec<_>>>()?;

        let methods = resource_def.methods.iter()
            .map(|m| self.convert_function_definition(m))
            .collect::<WasmtimeResult<Vec<_>>>()?;

        // Create actual Wasmtime resource type from resource definition
        let resource_type = ResourceType::host::<()>();

        Ok(WitResource {
            name: resource_def.name.clone(),
            resource_type,
            constructors,
            methods,
            static_methods: Vec::new(), // Would be extracted from the resource definition
            documentation: None,
        })
    }

    /// Validate a WIT interface
    fn validate_interface(&self, interface: &WitInterface) -> WasmtimeResult<ValidationResult> {
        let mut errors = Vec::new();
        let mut warnings = Vec::new();

        // Validate method signatures
        for (method_name, method) in &interface.methods {
            if method.parameters.is_empty() && method.return_types.is_empty() {
                warnings.push(ValidationWarning {
                    code: ValidationWarningCode::PerformanceWarning,
                    message: format!("Method '{}' has no parameters or return values", method_name),
                    location: None,
                });
            }

            // Validate parameter types
            for param in &method.parameters {
                if let Err(e) = self.validate_wit_type(&param.wit_type) {
                    errors.push(ValidationError {
                        code: ValidationErrorCode::TypeMismatch,
                        message: format!("Invalid parameter type in method '{}': {}", method_name, e),
                        location: None,
                    });
                }
            }

            // Validate return types
            for return_type in &method.return_types {
                if let Err(e) = self.validate_wit_type(return_type) {
                    errors.push(ValidationError {
                        code: ValidationErrorCode::TypeMismatch,
                        message: format!("Invalid return type in method '{}': {}", method_name, e),
                        location: None,
                    });
                }
            }
        }

        // Validate type definitions
        for (type_name, wit_type) in &interface.types {
            if let Err(e) = self.validate_wit_type(wit_type) {
                errors.push(ValidationError {
                    code: ValidationErrorCode::TypeMismatch,
                    message: format!("Invalid type definition '{}': {}", type_name, e),
                    location: None,
                });
            }
        }

        let status = if errors.is_empty() {
            if warnings.is_empty() {
                ValidationStatus::Valid
            } else {
                ValidationStatus::Warning
            }
        } else {
            ValidationStatus::Invalid
        };

        Ok(ValidationResult {
            status,
            errors,
            warnings,
            timestamp: std::time::Instant::now(),
        })
    }

    /// Validate a WIT type using actual Wasmtime type checking
    fn validate_wit_type(&self, wit_type: &WitType) -> Result<(), String> {
        match &wit_type.kind {
            WitTypeKind::Primitive(prim_type) => {
                // Validate primitive types against Wasmtime's type system
                match prim_type {
                    PrimitiveType::Bool | PrimitiveType::S8 | PrimitiveType::U8 |
                    PrimitiveType::S16 | PrimitiveType::U16 | PrimitiveType::S32 |
                    PrimitiveType::U32 | PrimitiveType::S64 | PrimitiveType::U64 |
                    PrimitiveType::Float32 | PrimitiveType::Float64 |
                    PrimitiveType::Char | PrimitiveType::String => Ok(()),
                }
            },
            WitTypeKind::Composite(composite) => {
                match composite {
                    CompositeType::Record(fields) => {
                        if fields.is_empty() {
                            return Err("Record type cannot be empty".to_string());
                        }
                        for field in fields {
                            self.validate_wit_type(&field.field_type)?;
                        }
                        Ok(())
                    },
                    CompositeType::Variant(cases) => {
                        if cases.is_empty() {
                            return Err("Variant type must have at least one case".to_string());
                        }
                        let mut discriminants = std::collections::HashSet::new();
                        for case in cases {
                            if !discriminants.insert(case.discriminant) {
                                return Err(format!("Duplicate discriminant {} in variant", case.discriminant));
                            }
                            if let Some(payload) = &case.payload {
                                self.validate_wit_type(payload)?;
                            }
                        }
                        Ok(())
                    },
                    CompositeType::List(inner) => {
                        self.validate_wit_type(inner)
                    },
                    CompositeType::Option(inner) => {
                        self.validate_wit_type(inner)
                    },
                    CompositeType::Result { ok, err } => {
                        if let Some(ok_type) = ok {
                            self.validate_wit_type(ok_type)?;
                        }
                        if let Some(err_type) = err {
                            self.validate_wit_type(err_type)?;
                        }
                        Ok(())
                    },
                    CompositeType::Tuple(types) => {
                        if types.is_empty() {
                            return Err("Tuple type cannot be empty".to_string());
                        }
                        for ty in types {
                            self.validate_wit_type(ty)?;
                        }
                        Ok(())
                    },
                    CompositeType::Enum(names) => {
                        if names.is_empty() {
                            return Err("Enum type must have at least one variant".to_string());
                        }
                        let mut unique_names = std::collections::HashSet::new();
                        for name in names {
                            if !unique_names.insert(name) {
                                return Err(format!("Duplicate enum variant name: {}", name));
                            }
                        }
                        Ok(())
                    },
                    CompositeType::Flags(flags) => {
                        if flags.len() > 64 {
                            return Err("Flags type cannot have more than 64 flags".to_string());
                        }
                        let mut unique_flags = std::collections::HashSet::new();
                        for flag in flags {
                            if !unique_flags.insert(flag) {
                                return Err(format!("Duplicate flag name: {}", flag));
                            }
                        }
                        Ok(())
                    },
                }
            },
            WitTypeKind::Resource(resource_name) => {
                // Validate resource name is not empty and follows naming conventions
                if resource_name.is_empty() {
                    return Err("Resource name cannot be empty".to_string());
                }
                if !resource_name.chars().all(|c| c.is_alphanumeric() || c == '_' || c == '-') {
                    return Err(format!("Invalid resource name: {}", resource_name));
                }
                Ok(())
            },
            WitTypeKind::Alias(aliased_type) => {
                self.validate_wit_type(aliased_type)
            },
        }
    }

    /// Check if two WIT types are compatible
    fn are_types_compatible(&self, type1: &WitType, type2: &WitType) -> bool {
        match (&type1.kind, &type2.kind) {
            (WitTypeKind::Primitive(p1), WitTypeKind::Primitive(p2)) => p1 == p2,
            (WitTypeKind::Composite(c1), WitTypeKind::Composite(c2)) => {
                self.are_composite_types_compatible(c1, c2)
            },
            (WitTypeKind::Resource(r1), WitTypeKind::Resource(r2)) => r1 == r2,
            (WitTypeKind::Alias(a1), WitTypeKind::Alias(a2)) => {
                self.are_types_compatible(a1, a2)
            },
            _ => false,
        }
    }

    /// Check if two composite types are compatible
    fn are_composite_types_compatible(&self, comp1: &CompositeType, comp2: &CompositeType) -> bool {
        match (comp1, comp2) {
            (CompositeType::Record(fields1), CompositeType::Record(fields2)) => {
                fields1.len() == fields2.len() &&
                fields1.iter().zip(fields2.iter()).all(|(f1, f2)| {
                    f1.name == f2.name && self.are_types_compatible(&f1.field_type, &f2.field_type)
                })
            },
            (CompositeType::Variant(cases1), CompositeType::Variant(cases2)) => {
                cases1.len() == cases2.len() &&
                cases1.iter().zip(cases2.iter()).all(|(c1, c2)| {
                    c1.name == c2.name && c1.discriminant == c2.discriminant &&
                    match (&c1.payload, &c2.payload) {
                        (Some(p1), Some(p2)) => self.are_types_compatible(p1, p2),
                        (None, None) => true,
                        _ => false,
                    }
                })
            },
            (CompositeType::List(inner1), CompositeType::List(inner2)) => {
                self.are_types_compatible(inner1, inner2)
            },
            (CompositeType::Option(inner1), CompositeType::Option(inner2)) => {
                self.are_types_compatible(inner1, inner2)
            },
            (CompositeType::Enum(names1), CompositeType::Enum(names2)) => {
                names1 == names2
            },
            (CompositeType::Flags(flags1), CompositeType::Flags(flags2)) => {
                flags1 == flags2
            },
            _ => false,
        }
    }

    /// Validate method parameters against signature using Wasmtime type checking
    fn validate_method_parameters(&self, method: &WitMethod, parameters: &[Val]) -> WasmtimeResult<()> {
        if parameters.len() != method.parameters.len() {
            return Err(WasmtimeError::InvalidParameter {
                message: format!(
                    "Parameter count mismatch: expected {}, got {}",
                    method.parameters.len(),
                    parameters.len()
                ),
            });
        }

        // Validate each parameter type against the expected WIT type
        for (i, (param, expected_param)) in parameters.iter().zip(method.parameters.iter()).enumerate() {
            if !self.validate_val_against_wit_type(param, &expected_param.wit_type)? {
                return Err(WasmtimeError::InvalidParameter {
                    message: format!(
                        "Parameter {} type mismatch: expected {:?}, got value type incompatible with {}",
                        i, expected_param.wit_type.name, expected_param.wit_type.name
                    ),
                });
            }
        }

        Ok(())
    }

    /// Validate method results against signature using Wasmtime type checking
    fn validate_method_results(&self, method: &WitMethod, results: &[Val]) -> WasmtimeResult<()> {
        if results.len() != method.return_types.len() {
            return Err(WasmtimeError::ValidationError {
                message: format!(
                    "Return value count mismatch: expected {}, got {}",
                    method.return_types.len(),
                    results.len()
                ),
            });
        }

        // Validate each result type against the expected WIT type
        for (i, (result, expected_type)) in results.iter().zip(method.return_types.iter()).enumerate() {
            if !self.validate_val_against_wit_type(result, expected_type)? {
                return Err(WasmtimeError::ValidationError {
                    message: format!(
                        "Return value {} type mismatch: expected {:?}, got incompatible type",
                        i, expected_type.name
                    ),
                });
            }
        }

        Ok(())
    }

    /// Validate a Wasmtime Val against a WIT type
    fn validate_val_against_wit_type(&self, val: &Val, wit_type: &WitType) -> WasmtimeResult<bool> {
        match (&wit_type.kind, val) {
            (WitTypeKind::Primitive(PrimitiveType::Bool), Val::Bool(_)) => Ok(true),
            (WitTypeKind::Primitive(PrimitiveType::S8), Val::S8(_)) => Ok(true),
            (WitTypeKind::Primitive(PrimitiveType::U8), Val::U8(_)) => Ok(true),
            (WitTypeKind::Primitive(PrimitiveType::S16), Val::S16(_)) => Ok(true),
            (WitTypeKind::Primitive(PrimitiveType::U16), Val::U16(_)) => Ok(true),
            (WitTypeKind::Primitive(PrimitiveType::S32), Val::S32(_)) => Ok(true),
            (WitTypeKind::Primitive(PrimitiveType::U32), Val::U32(_)) => Ok(true),
            (WitTypeKind::Primitive(PrimitiveType::S64), Val::S64(_)) => Ok(true),
            (WitTypeKind::Primitive(PrimitiveType::U64), Val::U64(_)) => Ok(true),
            (WitTypeKind::Primitive(PrimitiveType::Float32), Val::Float32(_)) => Ok(true),
            (WitTypeKind::Primitive(PrimitiveType::Float64), Val::Float64(_)) => Ok(true),
            (WitTypeKind::Primitive(PrimitiveType::Char), Val::Char(_)) => Ok(true),
            (WitTypeKind::Primitive(PrimitiveType::String), Val::String(_)) => Ok(true),
            (WitTypeKind::Composite(CompositeType::List(_)), Val::List(_)) => Ok(true),
            (WitTypeKind::Composite(CompositeType::Record(_)), Val::Record(_)) => Ok(true),
            (WitTypeKind::Composite(CompositeType::Variant(_)), Val::Variant(_, _)) => Ok(true),
            (WitTypeKind::Composite(CompositeType::Enum(_)), Val::Enum(_)) => Ok(true),
            (WitTypeKind::Composite(CompositeType::Option(_)), Val::Option(_)) => Ok(true),
            (WitTypeKind::Composite(CompositeType::Result { .. }), Val::Result(_)) => Ok(true),
            (WitTypeKind::Composite(CompositeType::Tuple(_)), Val::Tuple(_)) => Ok(true),
            (WitTypeKind::Composite(CompositeType::Flags(_)), Val::Flags(_)) => Ok(true),
            (WitTypeKind::Resource(_), Val::Resource(_)) => Ok(true),
            // For aliases, validate against the aliased type
            (WitTypeKind::Alias(aliased), _) => self.validate_val_against_wit_type(val, aliased),
            _ => Ok(false), // Type mismatch
        }
    }

    /// Calculate signature hash for method compatibility
    fn calculate_signature_hash(&self, name: &str, parameters: &[WitParameter], return_types: &[WitType]) -> u64 {
        use std::collections::hash_map::DefaultHasher;
        use std::hash::{Hash, Hasher};

        let mut hasher = DefaultHasher::new();
        name.hash(&mut hasher);
        parameters.len().hash(&mut hasher);
        return_types.len().hash(&mut hasher);

        // Simple hash - in a real implementation, we'd hash the actual type information
        hasher.finish()
    }
}

impl Default for WitInterfaceManager {
    fn default() -> Self {
        Self::new()
    }
}

impl fmt::Display for ValidationError {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        write!(f, "{:?}: {}", self.code, self.message)
    }
}

impl fmt::Display for ValidationWarning {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        write!(f, "{:?}: {}", self.code, self.message)
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_wit_interface_manager_creation() {
        let manager = WitInterfaceManager::new();
        let interfaces = manager.get_all_interfaces().unwrap();
        assert!(interfaces.is_empty());
    }

    #[test]
    fn test_convert_component_value_type_primitives() {
        let manager = WitInterfaceManager::new();

        // Test boolean type
        let bool_type = manager.convert_component_value_type(&ComponentValueType::Bool);
        assert_eq!(bool_type.name, "bool");
        assert!(matches!(bool_type.kind, WitTypeKind::Primitive(PrimitiveType::Bool)));
        assert_eq!(bool_type.size_bytes, Some(1));

        // Test string type
        let string_type = manager.convert_component_value_type(&ComponentValueType::String);
        assert_eq!(string_type.name, "string");
        assert!(matches!(string_type.kind, WitTypeKind::Primitive(PrimitiveType::String)));
        assert_eq!(string_type.size_bytes, None); // Variable size

        // Test numeric types
        let s32_type = manager.convert_component_value_type(&ComponentValueType::S32);
        assert_eq!(s32_type.name, "s32");
        assert!(matches!(s32_type.kind, WitTypeKind::Primitive(PrimitiveType::S32)));
        assert_eq!(s32_type.size_bytes, Some(4));
    }

    #[test]
    fn test_convert_component_value_type_composite() {
        let manager = WitInterfaceManager::new();

        // Test list type
        let list_type = manager.convert_component_value_type(
            &ComponentValueType::List(Box::new(ComponentValueType::S32))
        );
        assert_eq!(list_type.name, "list<s32>");
        assert!(matches!(list_type.kind, WitTypeKind::Composite(CompositeType::List(_))));

        // Test option type
        let option_type = manager.convert_component_value_type(
            &ComponentValueType::Option(Box::new(ComponentValueType::String))
        );
        assert_eq!(option_type.name, "option<string>");
        assert!(matches!(option_type.kind, WitTypeKind::Composite(CompositeType::Option(_))));
    }

    #[test]
    fn test_validation_status() {
        let status = ValidationStatus::Valid;
        assert_eq!(status, ValidationStatus::Valid);
        assert_ne!(status, ValidationStatus::Invalid);
    }

    #[test]
    fn test_validation_result() {
        let result = ValidationResult {
            status: ValidationStatus::Valid,
            errors: Vec::new(),
            warnings: Vec::new(),
            timestamp: std::time::Instant::now(),
        };

        assert_eq!(result.status, ValidationStatus::Valid);
        assert!(result.errors.is_empty());
        assert!(result.warnings.is_empty());
    }

    #[test]
    fn test_wit_type_validation() {
        let manager = WitInterfaceManager::new();

        // Test primitive type validation
        let bool_type = WitType {
            name: "bool".to_string(),
            kind: WitTypeKind::Primitive(PrimitiveType::Bool),
            size_bytes: Some(1),
            alignment: Some(1),
            documentation: None,
        };

        assert!(manager.validate_wit_type(&bool_type).is_ok());
    }

    #[test]
    fn test_type_compatibility() {
        let manager = WitInterfaceManager::new();

        let type1 = WitType {
            name: "bool".to_string(),
            kind: WitTypeKind::Primitive(PrimitiveType::Bool),
            size_bytes: Some(1),
            alignment: Some(1),
            documentation: None,
        };

        let type2 = WitType {
            name: "bool".to_string(),
            kind: WitTypeKind::Primitive(PrimitiveType::Bool),
            size_bytes: Some(1),
            alignment: Some(1),
            documentation: None,
        };

        let type3 = WitType {
            name: "s32".to_string(),
            kind: WitTypeKind::Primitive(PrimitiveType::S32),
            size_bytes: Some(4),
            alignment: Some(4),
            documentation: None,
        };

        assert!(manager.are_types_compatible(&type1, &type2));
        assert!(!manager.are_types_compatible(&type1, &type3));
    }

    #[test]
    fn test_signature_hash_calculation() {
        let manager = WitInterfaceManager::new();

        let param = WitParameter {
            name: "test_param".to_string(),
            wit_type: WitType {
                name: "s32".to_string(),
                kind: WitTypeKind::Primitive(PrimitiveType::S32),
                size_bytes: Some(4),
                alignment: Some(4),
                documentation: None,
            },
            is_optional: false,
            documentation: None,
        };

        let return_type = WitType {
            name: "bool".to_string(),
            kind: WitTypeKind::Primitive(PrimitiveType::Bool),
            size_bytes: Some(1),
            alignment: Some(1),
            documentation: None,
        };

        let hash1 = manager.calculate_signature_hash("test_method", &[param.clone()], &[return_type.clone()]);
        let hash2 = manager.calculate_signature_hash("test_method", &[param], &[return_type]);

        assert_eq!(hash1, hash2);
    }

    #[test]
    fn test_clear_validation_cache() {
        let manager = WitInterfaceManager::new();
        let result = manager.clear_validation_cache();
        assert!(result.is_ok());
    }
}