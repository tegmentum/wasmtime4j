# Eclipse IDE Integration Guide

This guide provides comprehensive setup instructions for developing with wasmtime4j in Eclipse IDE.

## Quick Setup

### 1. Import Existing Project

#### Using Git Integration

1. **File → Import**
2. **Git → Projects from Git → Clone URI**
3. **URI**: `https://github.com/tegmentum/wasmtime4j.git`
4. **Import as general project**, then convert to Maven

#### Using Maven Import

1. **File → Import**
2. **Existing Maven Projects**
3. **Browse** to the cloned `wasmtime4j` directory
4. **Select all modules** and click **Finish**

### 2. Workspace Setup

#### Java Build Path Configuration

1. **Right-click project → Properties**
2. **Java Build Path → Libraries**
3. **Modulepath/Classpath → Add Library → JRE System Library**
4. **Configure JRE**:
   - **Minimum**: Java 8
   - **Recommended**: Java 17+ for better development experience
   - **Required for Panama**: Java 23+

#### Project Facets

1. **Right-click project → Properties**
2. **Project Facets**
3. **Enable**:
   - Java (version 8 or higher)
   - Maven Integration

### 3. Maven Integration

Eclipse includes M2E (Maven to Eclipse) integration:

1. **Window → Preferences → Maven**
2. **Download repository index updates on startup**: Check
3. **Download Artifact Sources**: Check
4. **Download Artifact JavaDoc**: Uncheck (for performance)

#### Auto-Import Configuration

1. **Window → Preferences → Maven**
2. **Automatically import Maven projects**: Check
3. **Refresh workspace automatically**: Check

## Development Configuration

### Build Configuration

#### Compiler Settings

1. **Window → Preferences → Java → Compiler**
2. Configure:
   - **Compiler compliance level**: 1.8
   - **Use default compliance settings**: Check
   - **Enable preview features** (for Java 23+ Panama development)

#### Maven Compiler Settings

1. **Right-click project → Properties**
2. **Maven → Java EE Integration**
3. **Enable Maven nature**: Check

### Run Configurations

#### Unit Tests Configuration

1. **Run → Run Configurations**
2. **JUnit → New Configuration**
3. Configure:
   - **Name**: `Wasmtime4j Unit Tests`
   - **Project**: `wasmtime4j`
   - **Test runner**: JUnit 5
   - **Run all tests in the selected project**: Check
   - **VM arguments**:
     ```
     -Xmx1g
     -Djava.library.path=${workspace_loc}/wasmtime4j/target/natives
     -Dwasmtime4j.runtime=auto
     ```

#### Integration Tests Configuration

1. **Run → Run Configurations**
2. **JUnit → New Configuration**
3. Configure:
   - **Name**: `Wasmtime4j Integration Tests`
   - **Project**: `wasmtime4j-tests`
   - **Test runner**: JUnit 5
   - **Include and exclude tags**: Add `integration-tests` tag
   - **VM arguments**:
     ```
     -Xmx2g
     -Djava.library.path=${workspace_loc}/wasmtime4j/target/natives
     -Dprofile=integration-tests
     ```

#### Benchmark Configuration

1. **Run → Run Configurations**
2. **Java Application → New Configuration**
3. Configure:
   - **Name**: `JMH Benchmarks`
   - **Project**: `wasmtime4j-benchmarks`
   - **Main class**: `org.openjdk.jmh.Main`
   - **Program arguments**: `.*`
   - **VM arguments**:
     ```
     -Xmx4g
     -XX:+UseG1GC
     -Djava.library.path=${workspace_loc}/wasmtime4j/target/natives
     ```

### External Tools Integration

#### Maven Commands

Create external tools for common Maven commands:

1. **Run → External Tools → External Tools Configurations**
2. **Program → New Configuration**

**Clean and Compile**:
- **Name**: `Maven Clean Compile`
- **Location**: `/usr/bin/mvn` (or `${workspace_loc}/mvnw`)
- **Working Directory**: `${workspace_loc}/wasmtime4j`
- **Arguments**: `clean compile`

**Run Tests**:
- **Name**: `Maven Test`
- **Location**: `/usr/bin/mvn`
- **Working Directory**: `${workspace_loc}/wasmtime4j`
- **Arguments**: `test -q`

**Integration Tests**:
- **Name**: `Maven Integration Tests`
- **Location**: `/usr/bin/mvn`
- **Working Directory**: `${workspace_loc}/wasmtime4j`
- **Arguments**: `test -P integration-tests -q`

### Debugging Configuration

#### Debug Unit Tests

1. **Run → Debug Configurations**
2. **JUnit → New Configuration**
3. Configure:
   - **Name**: `Debug Wasmtime4j Tests`
   - **Project**: `wasmtime4j`
   - **VM arguments**:
     ```
     -Xmx1g
     -Djava.library.path=${workspace_loc}/wasmtime4j/target/natives
     -Dwasmtime4j.runtime=jni
     -Dwasmtime4j.debug=true
     -Djava.util.logging.level=FINE
     ```

#### Native Code Debugging

For native code debugging, install CDT (C/C++ Development Tools):

1. **Help → Eclipse Marketplace**
2. **Search**: "CDT"
3. **Install**: Eclipse CDT
4. **Create C++ Project** for native debugging

### Code Style and Quality

#### Code Formatting

1. **Window → Preferences → Java → Code Style → Formatter**
2. **Import** Google Java Style:
   - Download: [eclipse-java-google-style.xml](https://github.com/google/styleguide/blob/gh-pages/eclipse-java-google-style.xml)
   - **Import** the XML file

#### Code Templates

1. **Window → Preferences → Java → Code Style → Code Templates**
2. **Configure patterns for**:
   - **Comments → Types**:
   ```java
   /**
    * ${type_name}
    *
    * @author ${user}
    * @since ${date}
    */
   ```

   - **Code → New Java files**:
   ```java
   ${filecomment}
   ${package_declaration}

   import static org.junit.jupiter.api.Assertions.*;
   import org.junit.jupiter.api.Test;
   import org.junit.jupiter.api.BeforeEach;
   import org.junit.jupiter.api.AfterEach;

   import ai.tegmentum.wasmtime4j.*;

   ${typecomment}
   ${type_declaration}
   ```

#### Static Analysis Integration

##### Checkstyle Integration

1. **Help → Eclipse Marketplace**
2. **Search**: "Checkstyle"
3. **Install**: Checkstyle Plug-in
4. **Configure**:
   - **Window → Preferences → Checkstyle**
   - **New → External Configuration File**
   - **Location**: `${workspace_loc}/wasmtime4j/checkstyle.xml`

##### SpotBugs Integration

1. **Help → Eclipse Marketplace**
2. **Search**: "SpotBugs"
3. **Install**: SpotBugs Eclipse Plugin
4. **Configure**:
   - **Window → Preferences → SpotBugs**
   - **Exclude filter**: `${workspace_loc}/wasmtime4j/spotbugs-exclude.xml`

##### PMD Integration

1. **Help → Eclipse Marketplace**
2. **Search**: "PMD"
3. **Install**: PMD Eclipse Plugin
4. **Configure**:
   - **Window → Preferences → PMD**
   - **Rules**: Import `${workspace_loc}/wasmtime4j/pmd-ruleset.xml`

### Content Assist and Templates

#### Code Templates

Create useful code templates for WebAssembly development:

1. **Window → Preferences → Java → Editor → Templates**

**wasm-test** - Basic WebAssembly test:
```java
@Test
void test${name}() {
    byte[] wasmBytes = loadWasmFile("${file}");
    Module module = Module.fromBinary(engine, wasmBytes);
    Instance instance = Instance.newBuilder(store, module).build();

    ${cursor}

    instance.close();
    module.close();
}
```

**wasm-setup** - Test setup methods:
```java
@BeforeEach
void setUp() {
    engine = Engine.newBuilder().build();
    store = Store.newBuilder(engine).build();
}

@AfterEach
void tearDown() {
    if (store != null) {
        store.close();
    }
    if (engine != null) {
        engine.close();
    }
}
```

**wasm-func** - Function call pattern:
```java
Function func = instance.getFunction("${name}");
Object[] results = func.call(${params});
assertEquals(${expected}, results[0]);
```

#### Content Assist Configuration

1. **Window → Preferences → Java → Editor → Content Assist**
2. **Configure**:
   - **Auto activation delay**: 50ms
   - **Auto activation triggers for Java**: `.abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ`
   - **Show parameter names**: Check

## Project Setup from Scratch

### Creating a New Project

#### Using Maven Archetype

1. **File → New → Other**
2. **Maven → Maven Project**
3. **Configure archetype**:
   ```
   GroupId: ai.tegmentum
   ArtifactId: wasmtime4j-archetype
   Version: 1.0.0-SNAPSHOT
   ```

#### Manual Setup

1. **File → New → Maven Project**
2. **Create a simple project**: Check
3. **Configure**:
   - **Group Id**: `com.example`
   - **Artifact Id**: `my-wasmtime-app`
   - **Version**: `1.0.0-SNAPSHOT`
   - **Packaging**: `jar`

4. **Configure `pom.xml`**:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.example</groupId>
    <artifactId>my-wasmtime-app</artifactId>
    <version>1.0.0-SNAPSHOT</version>

    <properties>
        <maven.compiler.source>8</maven.compiler.source>
        <maven.compiler.target>8</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    </properties>

    <dependencies>
        <dependency>
            <groupId>ai.tegmentum</groupId>
            <artifactId>wasmtime4j</artifactId>
            <version>1.0.0-SNAPSHOT</version>
        </dependency>

        <!-- Testing -->
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter</artifactId>
            <version>5.10.4</version>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>3.11.0</version>
                <configuration>
                    <source>8</source>
                    <target>8</target>
                </configuration>
            </plugin>

            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-surefire-plugin</artifactId>
                <version>3.1.2</version>
            </plugin>
        </plugins>
    </build>
</project>
```

5. **Refresh Maven project**: Right-click → Maven → Reload Projects

### Project Structure

After setup, your Eclipse workspace should show:

```
my-wasmtime-app/
├── src/main/java/           # Application source code
├── src/main/resources/      # Application resources
├── src/test/java/           # Test source code
├── src/test/resources/      # Test resources (WASM files)
├── target/                  # Build output
├── pom.xml                  # Maven configuration
└── .project                 # Eclipse project file
```

## Troubleshooting

### Common Issues

#### Project Import Problems

**Problem**: Maven projects not recognized
**Solution**:
1. **File → Import → Existing Maven Projects**
2. **Right-click → Configure → Convert to Maven Project**
3. **Project → Clean → Clean all projects**

#### Native Library Loading

**Problem**: `UnsatisfiedLinkError` when running tests
**Solution**:
1. **Run Configuration → VM arguments**: Add `-Djava.library.path=${workspace_loc}/wasmtime4j/target/natives`
2. **Verify native library extraction**: Check `target/natives/` directory
3. **Clean and rebuild**: **Project → Clean**

#### Memory Issues

**Problem**: Out of memory during builds
**Solution**:
1. **Increase Eclipse memory**: Edit `eclipse.ini`:
   ```
   -Xmx4g
   -XX:+UseG1GC
   ```
2. **Increase Maven memory**: **Run Configuration → VM arguments**: `-Xmx2g`

#### M2E Integration Issues

**Problem**: Maven projects not building correctly
**Solution**:
1. **Right-click project → Maven → Reload Projects**
2. **Window → Preferences → Maven → Refresh workspace automatically**
3. **Project → Clean → Clean all projects**

### Performance Optimization

#### Eclipse Performance

1. **Window → Preferences → General → Startup and Shutdown**
   - **Disable unnecessary plugins**
   - **Plug-in activation**: Set to "Lazy"

2. **Window → Preferences → Java → Editor**
   - **Folding**: Disable unnecessary folding
   - **Mark occurrences**: Only when needed

3. **Window → Preferences → General → Workspace**
   - **Build automatically**: Disable for large projects
   - **Save automatically before build**: Uncheck

#### Build Performance

1. **Parallel builds**:
   - **Window → Preferences → General → Workspace**
   - **Build order → Use parallel jobs**: Check
   - **Max parallel builds**: Set to CPU cores

2. **Maven performance**:
   - **Window → Preferences → Maven**
   - **Do not automatically update dependencies**: Check
   - **Download sources**: Uncheck

### Workspace Organization

#### Working Sets

Organize large projects using working sets:

1. **Package Explorer → View Menu → Select Working Set**
2. **New → Java Working Set**
3. **Create sets**:
   - `wasmtime4j-core` (main API)
   - `wasmtime4j-impl` (JNI/Panama implementations)
   - `wasmtime4j-tests` (test modules)

#### Perspectives

Configure perspectives for different development tasks:

1. **Java Development**: Default for coding
2. **Debug**: For debugging sessions
3. **Git**: For version control operations

## Integration with Build Tools

### Maven Integration

Eclipse provides excellent Maven integration through M2E:

1. **Dependency management**: Automatic download and resolution
2. **Build lifecycle**: Execute Maven goals from Eclipse
3. **Multi-module support**: Handle complex project hierarchies
4. **Integration testing**: Run tests with proper classpath

### Git Integration (EGit)

Eclipse includes built-in Git support:

1. **Git perspective**: Switch to Git perspective for version control
2. **Git staging**: Stage and commit changes
3. **Branch management**: Create, switch, and merge branches
4. **Conflict resolution**: Built-in merge conflict resolution

### Continuous Integration

#### Jenkins Integration

Install Jenkins Eclipse plugin for CI integration:

1. **Help → Eclipse Marketplace**
2. **Search**: "Jenkins"
3. **Install**: Jenkins Editor Plugin

#### GitHub Integration

For GitHub repositories:

1. **Help → Eclipse Marketplace**
2. **Search**: "GitHub"
3. **Install**: GitHub Mylyn Connector

## Advanced Features

### Code Coverage

#### EclEmma (JaCoCo) Integration

1. **Help → Eclipse Marketplace**
2. **Search**: "EclEmma"
3. **Install**: EclEmma Java Code Coverage
4. **Usage**: Right-click → Coverage As → JUnit Test

### Profiling Integration

#### Eclipse MAT (Memory Analyzer)

1. **Help → Eclipse Marketplace**
2. **Search**: "Memory Analyzer"
3. **Install**: Eclipse Memory Analyzer
4. **Usage**: Generate heap dumps during testing

### Native Development

#### CDT Integration

For developers working on native code:

1. **Install CDT**: Help → Install New Software
2. **Create C++ project**: For Rust native library development
3. **Cross-language debugging**: Debug Java and native code together

## Additional Resources

- [Eclipse IDE Documentation](https://help.eclipse.org/)
- [M2E Maven Integration](https://www.eclipse.org/m2e/)
- [EGit Git Integration](https://www.eclipse.org/egit/)
- [Eclipse Marketplace](https://marketplace.eclipse.org/)

## Version Compatibility

| Eclipse Version | Wasmtime4j Version | Java Version | M2E Version | Notes |
|----------------|-------------------|--------------|-------------|-------|
| 2023-12+       | 1.0.0+            | 8-23         | 2.4.0+      | Full support |
| 2023-09        | 1.0.0+            | 8-21         | 2.3.0+      | Limited Panama support |
| 2023-06        | 1.0.0+            | 8-19         | 2.2.0+      | JNI only |
| 2022-12+       | 1.0.0+            | 8-17         | 2.1.0+      | Basic support |

For the best development experience, use Eclipse 2023-12 or later with Java 17+ for improved performance and features.