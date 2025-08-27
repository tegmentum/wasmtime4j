---
created: 2025-08-27T00:32:32Z
last_updated: 2025-08-27T00:32:32Z
version: 1.0
author: Claude Code PM System
---

# Project Style Guide

## Code Style Standards

### Base Standard: Google Java Style Guide
**Authority**: https://google.github.io/styleguide/javaguide.html
**Enforcement**: Checkstyle with Google Java Style configuration
**Modification**: None - strict adherence to original standard

### Key Style Requirements

#### General Guidelines
- **Character Encoding**: UTF-8 for all source files
- **Line Length**: Maximum 120 characters (ignoring package/import statements and URLs)
- **File Length**: Maximum 2000 lines per source file
- **Indentation**: Spaces only, no tabs (2 spaces for Java, 4 for other languages)

#### Naming Conventions
- **Classes**: `UpperCamelCase` (e.g., `WasmRuntime`, `ModuleInstance`)
- **Methods**: `lowerCamelCase` (e.g., `loadModule`, `executeFunction`)
- **Variables**: `lowerCamelCase` (e.g., `moduleBytes`, `functionName`)
- **Constants**: `UPPER_CASE_WITH_UNDERSCORES` (e.g., `DEFAULT_MEMORY_SIZE`, `MAX_STACK_SIZE`)
- **Packages**: `lowercase` (e.g., `ai.tegmentum.wasmtime4j`, `ai.tegmentum.wasmtime4j.jni`)

#### Code Organization
- **Method Length**: Maximum 150 lines per method
- **Parameter Count**: Maximum 7 parameters per method
- **Import Style**: No wildcard imports (avoid `import java.util.*`)
- **Modifier Order**: Standard Java conventions (public, protected, private, abstract, static, final, transient, volatile, synchronized, native, strictfp)

## Package Structure Patterns

### Base Package Hierarchy
```
ai.tegmentum.wasmtime4j              # Public API interfaces
├── .jni                             # JNI implementation (private)
├── .panama                          # Panama implementation (private)  
├── .native                          # Native library interfaces
├── .common                          # Shared utilities
├── .exception                       # Exception classes
├── .factory                         # Factory classes
└── .util                           # Utility classes
```

### File Naming Patterns
- **Interfaces**: Descriptive nouns (e.g., `WasmRuntime`, `ModuleLoader`)
- **Implementations**: Interface name + "Impl" (e.g., `WasmRuntimeImpl`, `JniModuleLoader`)
- **Exceptions**: Problem + "Exception" (e.g., `CompilationException`, `ValidationException`)
- **Tests**: Class name + "Test" (e.g., `WasmRuntimeTest`, `ModuleLoaderTest`)
- **Native Files**: Rust files use snake_case (e.g., `wasmtime_bindings.rs`, `jni_exports.rs`)

## Documentation Standards

### Javadoc Requirements
**Scope**: Required for all public classes, methods, and fields
**Format**: Standard Javadoc with specific tags

```java
/**
 * Brief description of the class or method purpose.
 * 
 * <p>Longer description with implementation details, usage examples,
 * and important notes about behavior or constraints.
 *
 * @param paramName Description of parameter and expected values
 * @return Description of return value and possible conditions
 * @throws ExceptionType When this exception is thrown and why
 * @since 1.0
 * @see RelatedClass#relatedMethod
 */
```

### Comment Style
- **TODO Comments**: Use `// TODO: Description` format for tracking future work
- **Implementation Notes**: Use `/* */` for multi-line implementation explanations
- **Line Comments**: Use `//` for single-line explanations
- **Native Code Comments**: Use `///` for Rust documentation comments

### Documentation Patterns
- **Class Headers**: Purpose, usage patterns, thread safety notes
- **Method Documentation**: Parameters, return values, exceptions, side effects
- **Field Documentation**: Purpose, valid ranges, invariants
- **Package Documentation**: In `package-info.java` files

## Code Quality Standards

### Static Analysis Configuration

#### Checkstyle Rules
```xml
<!-- Google Java Style with wasmtime4j customizations -->
<module name="LineLength">
    <property name="max" value="120"/>
</module>
<module name="FileLength">
    <property name="max" value="2000"/>
</module>
<module name="MethodLength">
    <property name="max" value="150"/>
</module>
```

#### SpotBugs Configuration
- **Security**: FindSecBugs plugin enabled for security vulnerability detection
- **Performance**: Performance-related bug detection
- **Correctness**: Logic error and null pointer detection
- **Threading**: Concurrency and synchronization issues

#### PMD Rules
- **Design**: Complexity and design pattern violations
- **Best Practices**: Java best practice violations
- **Performance**: Performance anti-patterns
- **Code Style**: Additional style checking beyond Checkstyle

### Code Formatting

#### Automatic Formatting: Spotless
**Configuration**: Google Java Format with custom settings
**Usage**: 
- `./mvnw spotless:check` - Check formatting
- `./mvnw spotless:apply` - Auto-format code

#### Manual Formatting Guidelines
- **Braces**: Always use braces for control structures (if, while, for, etc.)
- **Whitespace**: Proper spacing around operators and after commas
- **Array Declarations**: Java-style (`String[] args` not `String args[]`)
- **Line Breaks**: Break long lines at logical boundaries
- **Alignment**: Align related elements for readability

## Best Practices

### Java-Specific Practices
- **Final Parameters**: Method parameters should be declared final where possible
- **Boolean Logic**: Avoid unnecessary boolean complexity (prefer `if (condition)` over `if (condition == true)`)
- **Switch Statements**: Always include a default case
- **Equals and HashCode**: Override both methods or neither
- **Visibility**: Keep fields private/protected, provide getters/setters as needed

### Native Code Integration
- **Error Handling**: Always check native call results and handle errors gracefully
- **Resource Management**: Use try-with-resources or explicit cleanup for native resources
- **Parameter Validation**: Validate all parameters before native calls
- **Thread Safety**: Document thread safety guarantees for native method wrappers
- **Memory Management**: Clear documentation of memory ownership and lifecycle

### Testing Patterns
- **Test Classes**: One test class per production class
- **Test Methods**: Descriptive names indicating what is being tested
- **Test Structure**: Arrange-Act-Assert pattern
- **Test Data**: Use meaningful test data that reflects real usage
- **Error Testing**: Test error conditions and edge cases

## File Organization

### Source File Structure
```java
// 1. License header (if required)
// 2. Package declaration
package ai.tegmentum.wasmtime4j;

// 3. Import statements (sorted, no wildcards)
import java.util.List;
import java.util.Optional;

// 4. Exactly one top-level class
/**
 * Class documentation
 */
public final class ExampleClass {
    // 5. Class contents in order:
    // - Static fields
    // - Instance fields  
    // - Constructors
    // - Methods (public before private)
    // - Nested classes
}
```

### Maven Project Structure
```
wasmtime4j/
├── pom.xml                          # Parent POM
├── wasmtime4j/                      # Public API module
│   ├── pom.xml
│   └── src/main/java/               # Java source
├── wasmtime4j-jni/                  # JNI implementation
│   ├── pom.xml
│   ├── src/main/java/               # Java JNI code
│   └── src/main/rust/               # Rust native code
├── wasmtime4j-panama/               # Panama implementation
│   ├── pom.xml
│   └── src/main/java/               # Java Panama code
└── wasmtime4j-tests/                # Integration tests
    ├── pom.xml
    └── src/test/java/               # Test source
```

## Error Handling Conventions

### Exception Hierarchy
```java
ai.tegmentum.wasmtime4j.exception
├── WasmException                    # Base exception
├── CompilationException             # Module compilation errors
├── ValidationException              # Module validation errors
├── RuntimeException                 # Execution-time errors
└── ConfigurationException           # Setup and configuration errors
```

### Error Message Format
- **Consistent Format**: "Operation failed: detailed reason"
- **Context Information**: Include relevant context (module name, function name, etc.)
- **Actionable Guidance**: When possible, suggest resolution steps
- **No Sensitive Data**: Never log or expose sensitive information in error messages

## Performance Guidelines

### Code Performance
- **Object Creation**: Minimize unnecessary object allocation in hot paths
- **String Operations**: Use StringBuilder for multiple string concatenations
- **Collections**: Use appropriate collection types for the use case
- **Native Calls**: Batch native operations when possible to reduce JNI/Panama overhead

### Documentation Performance
- **Lazy Evaluation**: Document when expensive operations are performed lazily
- **Caching Behavior**: Document what is cached and cache invalidation strategies
- **Thread Safety Cost**: Document performance implications of thread safety choices
- **Benchmark Integration**: Include performance benchmarks as documentation examples