---
created: 2025-08-27T00:32:32Z
last_updated: 2025-08-27T00:32:32Z
version: 1.0
author: Claude Code PM System
---

# Project Structure

## Root Directory Layout

```
wasmtime4j/
├── .claude/                    # Claude Code PM system
│   ├── README.md              # PM system documentation
│   ├── agents/                # Task-oriented agents
│   ├── commands/              # Command definitions
│   │   ├── context/          # Context management commands
│   │   ├── pm/               # Project management commands
│   │   └── testing/          # Testing commands
│   ├── context/              # Project-wide context files (this directory)
│   ├── epics/                # PM's local workspace (should be in .gitignore)
│   ├── prds/                 # Product Requirements Documents
│   ├── rules/                # Rule files
│   └── scripts/              # Utility scripts
├── .git/                      # Git repository
├── AGENTS.md                  # Agent documentation
├── CLAUDE.md                  # Project guidance for Claude Code
├── COMMANDS.md                # Command reference
├── LICENSE                    # MIT license
└── README.md                  # Project documentation
```

## File Organization Patterns

### Configuration Files
- `CLAUDE.md` - Primary project guidance with wasmtime4j specifications and PM rules
- `LICENSE` - MIT license
- `.gitignore` - Not present, needs creation

### Documentation Structure
- Root-level markdown files for primary documentation
- `.claude/context/` - Structured context files for AI assistance
- `.claude/prds/` - Product requirements for planning

### Code Organization (Planned)
Based on CLAUDE.md specifications, the planned structure for wasmtime4j Java bindings:

```
wasmtime4j/                    # Root project
├── wasmtime4j/               # Public API interfaces and factory
├── wasmtime4j-benchmarks     # Performance benchmarks
├── wasmtime4j-native/        # Shared native Rust library
├── wasmtime4j-jni/           # JNI implementation (private/internal)
├── wasmtime4j-panama/        # Panama FFI implementation (private/internal)
└── wasmtime4j-tests/         # Integration tests and WebAssembly test suites
```

## Module Naming Convention

### Java Packages
- Base package: `ai.tegmentum.wasmtime4j`
- JNI package: `ai.tegmentum.wasmtime4j.jni`
- Panama package: `ai.tegmentum.wasmtime4j.panama`

### File Naming Patterns
- Maven projects: `pom.xml` in each module
- Maven wrapper: `mvnw` and `mvnw.cmd`
- Task files: `001.md`, `002.md` (local) → `{issue-id}.md` (after GitHub sync)

## Missing Structure Elements

1. **Build System**: No Maven files present yet
2. **Source Code**: No Java source directories
3. **Native Code**: No Rust source for wasmtime bindings
4. **Tests**: No test directories or frameworks configured
5. **CI/CD**: No GitHub Actions or build automation
6. **Documentation**: No API documentation or examples

## Project Type Indicators

- **Missing**: `pom.xml` (expected for Maven Java project)
- **Present**: Claude Code PM system (`.claude/` directory)
- **Purpose Conflict**: README describes PM system, but repo name suggests Java bindings

## Next Structure Steps

1. Create proper `.gitignore` for Java/Maven project
2. Set up Maven multi-module structure
3. Initialize Java source directories with proper package structure
4. Create native Rust library project structure
5. Set up test directories and frameworks