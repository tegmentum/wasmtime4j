//! WIT (WebAssembly Interface Types) type definitions for the Component Model

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
            if i > 0 {
                json.push_str(",\n");
            }
            json.push_str(&format!("    {{\"name\": \"{}\"}}", function.name));
        }
        json.push_str("\n  ],\n");

        json.push_str("  \"types\": [\n");
        for (i, type_def) in self.types.iter().enumerate() {
            if i > 0 {
                json.push_str(",\n");
            }
            json.push_str(&format!("    {{\"name\": \"{}\"}}", type_def.name));
        }
        json.push_str("\n  ],\n");

        json.push_str("  \"resources\": [\n");
        for (i, resource) in self.resources.iter().enumerate() {
            if i > 0 {
                json.push_str(",\n");
            }
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
        err: Option<Box<ComponentValueType>>,
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

