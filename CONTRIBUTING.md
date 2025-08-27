# Contributing to Wasmtime4j

We welcome contributions to Wasmtime4j! This document provides guidelines for contributing to the project.

## Code of Conduct

This project adheres to a code of conduct. By participating, you are expected to uphold this code. Please report unacceptable behavior to [wasmtime4j@tegmentum.ai](mailto:wasmtime4j@tegmentum.ai).

## How to Contribute

### Reporting Issues

Before creating an issue, please:

1. Check if the issue already exists in the [issue tracker](https://github.com/wasmtime4j/wasmtime4j/issues)
2. Search through [closed issues](https://github.com/wasmtime4j/wasmtime4j/issues?q=is%3Aissue+is%3Aclosed) to see if it's already been addressed
3. Check the [FAQ](https://github.com/wasmtime4j/wasmtime4j/wiki/FAQ) and [troubleshooting guide](README.md#troubleshooting)

When creating an issue, please include:

- **Environment Information**: Java version, OS, architecture
- **Runtime Information**: JNI or Panama, Wasmtime version
- **Steps to Reproduce**: Minimal code example that demonstrates the problem
- **Expected vs Actual Behavior**: What you expected to happen vs what actually happened
- **Stack Traces**: Full stack traces for exceptions or crashes

### Feature Requests

Feature requests are welcome! Please:

1. Check if the feature has already been requested
2. Explain the use case and why this feature would be valuable
3. Consider if this feature aligns with the project's goals
4. Be prepared to contribute to the implementation

### Pull Requests

#### Before You Start

1. **Discuss Large Changes**: For significant changes, open an issue first to discuss the approach
2. **Check Existing Work**: Look for existing pull requests that might address the same issue
3. **Understand the Architecture**: Read the [project documentation](README.md#project-structure) to understand the codebase

#### Development Setup

1. **Fork and Clone**:
   ```bash
   git clone https://github.com/your-username/wasmtime4j.git
   cd wasmtime4j
   ```

2. **Install Prerequisites**:
   - Java 8+ (JNI development)
   - Java 23+ (Panama development)
   - Rust toolchain (latest stable)
   - Maven 3.6+

3. **Build and Test**:
   ```bash
   ./mvnw clean compile
   ./mvnw test
   ```

4. **Verify Quality Checks**:
   ```bash
   ./mvnw checkstyle:check spotless:check
   ```

#### Making Changes

1. **Create a Branch**:
   ```bash
   git checkout -b feature/your-feature-name
   # or
   git checkout -b fix/issue-description
   ```

2. **Follow Code Style**:
   - The project follows the [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html)
   - Use `./mvnw spotless:apply` to auto-format code
   - Run `./mvnw checkstyle:check` to verify compliance

3. **Write Tests**:
   - Add unit tests for new functionality
   - Ensure tests pass on both JNI and Panama implementations
   - Use descriptive test names and good assertions
   - Follow existing test patterns in the codebase

4. **Update Documentation**:
   - Update README.md for user-facing changes
   - Add or update Javadoc comments
   - Update examples if APIs change

#### Code Guidelines

##### General Principles

1. **Defensive Programming**: Always validate inputs and handle errors gracefully
2. **Resource Management**: Use try-with-resources and proper cleanup
3. **Performance**: Consider performance implications, especially for JNI calls
4. **Compatibility**: Maintain compatibility across Java versions and platforms

##### Code Style Requirements

- **Naming**: Use clear, descriptive names for variables, methods, and classes
- **Documentation**: All public APIs must have Javadoc
- **Error Handling**: Use appropriate exception types from `ai.tegmentum.wasmtime4j.exception`
- **Logging**: Use `java.util.logging.Logger` for logging
- **Final Parameters**: Method parameters should be declared `final`

##### Architecture Guidelines

1. **Module Separation**:
   - `wasmtime4j`: Public API only, no implementation details
   - `wasmtime4j-jni`: JNI-specific implementation
   - `wasmtime4j-panama`: Panama-specific implementation
   - `wasmtime4j-native`: Shared Rust native code

2. **Interface Design**:
   - Keep interfaces simple and focused
   - Use factory patterns for object creation
   - Implement `AutoCloseable` for resource management

3. **Native Code**:
   - All native methods must be defensive
   - Handle all native errors gracefully
   - Never allow native crashes to propagate to JVM

##### Testing Guidelines

1. **Test Coverage**:
   - Aim for high test coverage on public APIs
   - Test error conditions and edge cases
   - Test both JNI and Panama implementations

2. **Test Organization**:
   - Unit tests in `src/test/java`
   - Integration tests in `wasmtime4j-tests`
   - Benchmarks in `wasmtime4j-benchmarks`

3. **Test Naming**:
   ```java
   // Good
   void testEngineCreationWithValidConfig()
   void testModuleCompilationWithInvalidBytecode()

   // Bad
   void test1()
   void testEngine()
   ```

#### Submitting Your Pull Request

1. **Commit Guidelines**:
   - Use [Conventional Commits](https://conventionalcommits.org/) format
   - Write clear, descriptive commit messages
   - Keep commits focused and atomic

   Examples:
   ```
   feat(api): add fuel consumption support to Store interface
   fix(jni): prevent memory leak in module compilation
   docs(readme): update installation instructions for Maven
   test(benchmarks): add memory operation benchmarks
   ```

2. **Before Submitting**:
   ```bash
   # Ensure everything builds
   ./mvnw clean compile

   # Run all tests
   ./mvnw test

   # Check code quality
   ./mvnw checkstyle:check spotless:check

   # Verify no regressions in benchmarks (if applicable)
   ./mvnw -pl wasmtime4j-benchmarks test
   ```

3. **Pull Request Description**:
   - Reference related issues using `Fixes #123` or `Closes #123`
   - Describe what changes were made and why
   - Mention any breaking changes
   - Include testing notes for reviewers

4. **Pull Request Template**:
   ```markdown
   ## Summary
   Brief description of changes

   ## Changes Made
   - List of specific changes
   - Include breaking changes

   ## Testing
   - How you tested these changes
   - Any new test cases added

   ## Related Issues
   - Fixes #123
   - Related to #456
   ```

### Review Process

1. **Automated Checks**: All PRs must pass CI checks (build, tests, quality)
2. **Code Review**: At least one maintainer will review your changes
3. **Testing**: Changes will be tested on multiple platforms and Java versions
4. **Feedback**: Address reviewer feedback promptly and courteously

### Release Process

Releases follow semantic versioning (SemVer):

- **MAJOR**: Breaking API changes
- **MINOR**: New features, backward compatible
- **PATCH**: Bug fixes, backward compatible

## Development Workflow

### Setting Up Your Environment

1. **IDE Setup**:
   - IntelliJ IDEA or Eclipse recommended
   - Import as Maven project
   - Install Checkstyle plugin
   - Configure auto-formatting with Google Java Style

2. **Git Configuration**:
   ```bash
   git config --local core.autocrlf input
   git config --local pull.rebase true
   ```

### Common Tasks

#### Adding a New API

1. Add interface to `wasmtime4j` module
2. Implement in both `wasmtime4j-jni` and `wasmtime4j-panama`
3. Add corresponding native code if needed
4. Write comprehensive tests
5. Update documentation and examples

#### Fixing a Bug

1. Write a failing test that reproduces the bug
2. Implement the fix
3. Ensure the test passes
4. Check for similar issues in other parts of the codebase

#### Performance Improvements

1. Add benchmarks to measure current performance
2. Implement improvements
3. Verify benchmarks show improvement
4. Check that accuracy is maintained

### Working with Native Code

#### JNI Development

- Native methods are declared in Java classes
- Implementation goes in `wasmtime4j-native/src/jni/`
- Use defensive programming in native code
- Test on multiple platforms

#### Panama Development

- Use Panama FFI for Java 23+
- Implementation goes in `wasmtime4j-panama`
- Leverage shared native library from `wasmtime4j-native`
- Handle preview feature requirements

#### Building Native Code

```bash
# Build native library
cd wasmtime4j-native
cargo build --release

# Cross-compile for different platforms
cargo build --release --target x86_64-pc-windows-gnu
cargo build --release --target aarch64-apple-darwin
```

## Community

### Communication Channels

- **GitHub Issues**: Bug reports and feature requests
- **GitHub Discussions**: General questions and community discussion
- **Mailing List**: Development announcements and coordination

### Getting Help

If you need help while contributing:

1. Check the [wiki](https://github.com/wasmtime4j/wasmtime4j/wiki)
2. Ask questions in [GitHub Discussions](https://github.com/wasmtime4j/wasmtime4j/discussions)
3. Reach out to maintainers via email

### Recognition

Contributors will be:

- Listed in the CONTRIBUTORS.md file
- Mentioned in release notes for significant contributions
- Given appropriate credit in documentation and examples

## License

By contributing to Wasmtime4j, you agree that your contributions will be licensed under the same license as the project (MIT License).

---

Thank you for contributing to Wasmtime4j! Your contributions help make WebAssembly more accessible to the Java community.
