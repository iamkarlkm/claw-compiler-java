# Claw Compiler LSP Support

Language Server Protocol (LSP) implementation for the Claw compiler.

## Overview

This module provides IDE support for Claw through the LSP protocol, enabling features like code completion, syntax checking, jump to definition, and find references.

## Architecture

```
lsp/
├── server/                 # LSP Server Core
│   ├── Main.java           # Server Entry Point
│   └── ClawLanguageServer.java  # Core Server Class
├── protocol/               # LSP Protocol Implementation
│   └── CompletionItem.java       # Completion Items
├── utils/
│   ├── JSONUtils.java        # JSON Utilities
│   └── DiagnosticGenerator.java    # Diagnostic Generation
└── client/                  # LSP Client
    └── ClawLanguageClient.java   # Test Client
```

## Features Implemented

### Phase 1: Basic Framework (Week 1-2) ✅

- [x] LSP server entry point (`Main.java`)
- [x] Core server class (`ClawLanguageServer.java`)
- [x] LSP dependencies added to `pom.xml`
- [x] Project structure created
- [x] Basic protocol classes (`CompletionItem.java`)
- [x] Utility classes (`JSONUtils.java`, `DiagnosticGenerator.java`)
- [x] Test client (`ClawLanguageClient.java`)

### Phase 2: Core Features (Week 2-4) 🔄

- [ ] **Code Completion**
  - [ ] Type completion (Int, Float, String, Bool)
  - [ ] Function completion
  - [ ] Variable completion
  - [ ] Annotation completion

- [ ] **Syntax Checking**
  - [ ] Syntax validation
  - [ ] Type checking
  - [ ] Annotation validation
  - [ ] Error diagnostics

- [ ] **Jump to Definition**
  - [ ] Jump to function definition
  - [ ] Jump to type definition
  - [ ] Jump to annotation definition

- [ ] **Find References**
  - [ ] Find variable references
  - [ ] Find function references
  - [ ] Find type references

### Phase 3: Feature Enhancement (Week 4-6) 📋

- [ ] Hover support
- [ ] Rename support
- [ ] Document symbols

### Phase 4: Testing and Optimization (Week 6-8) 📋

- [ ] Unit tests
- [ ] Integration tests
- [ ] Performance optimization

## Building

```bash
# Compile the LSP server
mvn clean compile

# Package into JAR
mvn package

# Run the LSP server
java -cp target/classes com.claw.lsp.server.Main
```

## Usage

### Running the LSP Server

```bash
# Standard input/output mode
java -cp target/classes com.claw.lsp.server.Main

# Listen on a specific port
java -cp target/classes com.claw.lsp.server.Main --port 5007
```

### Testing with VS Code

1. Install the "Claw Compiler" extension from the VS Code Marketplace
2. Open a `.claw` file
3. Use LSP features:
   - Code completion (Ctrl+Space)
   - Hover (Ctrl+Hover)
   - Jump to definition (F12)
   - Find references (Shift+F12)

## Configuration

### Server Capabilities

The LSP server provides the following capabilities:

- **Completion**: Triggered by `(`, `.`, ` `, `@`
- **Diagnostics**: Real-time syntax and type checking
- **Definition**: Jump to symbols
- **References**: Find all references
- **Hover**: Information on hover
- **Rename**: Rename symbols with workspace edit

### Dependencies

- `org.eclipse.lsp4j` (0.21.1) - LSP protocol implementation
- `org.eclipse.lsp4j.jsonrpc` (0.21.1) - JSON-RPC over sockets
- `com.google.code.gson` (2.10.1) - JSON processing

## Implementation Status

| Phase | Status | Progress |
|-------|--------|----------|
| Phase 1: Basic Framework | ✅ Complete | 100% |
| Phase 2: Core Features | 🔄 In Progress | 0% |
| Phase 3: Feature Enhancement | 📋 Planned | 0% |
| Phase 4: Testing | 📋 Planned | 0% |
| **Total** | 📋 In Progress | **~10%** |

## Development

### Adding New Features

1. Create the provider class (e.g., `CompletionProvider.java`)
2. Implement the LSP interface method
3. Add tests in `src/test/java/com/claw/lsp/`
4. Update documentation

### File Structure

```
src/main/java/com/claw/lsp/
├── server/
│   ├── Main.java
│   └── ClawLanguageServer.java
├── protocol/
│   └── CompletionItem.java
├── utils/
│   ├── JSONUtils.java
│   └── DiagnosticGenerator.java
├── client/
│   └── ClawLanguageClient.java
└── README.md
```

## Testing

### Unit Tests

```bash
# Run all LSP tests
mvn test -Dtest=*LSP*

# Run specific test class
mvn test -Dtest=CompletionProviderTest
```

### Integration Tests

```bash
# Start server
java -cp target/classes com.claw.lsp.server.Main &

# Run integration tests
mvn verify
```

## Performance

Target performance metrics:

| Operation | Target | Optimization |
|-----------|--------|--------------|
| Code completion | ~100ms | Caching |
| Syntax checking | ~500ms | Async |
| Jump to definition | ~50ms | Semantic cache |
| Find references | ~200ms | Reference graph |

## Future Enhancements

- Multi-project support
- Workspace-level diagnostics
- Improved caching strategy
- Symbol hierarchy
- Code navigation (outline view)
- Quick fixes
- Code actions

## References

- [LSP Specification](https://microsoft.github.io/language-server-protocol/)
- [Eclipse LSP4J](https://projects.eclipse.org/projects/eclipse.lsp4j)
- [VS Code Extension API](https://code.visualstudio.com/api)
- [Claw Compiler Documentation](../../docs/)

## Contributing

When contributing to LSP features:

1. Follow the existing code structure
2. Add comprehensive tests
3. Update documentation
4. Ensure backward compatibility
5. Performance test new features

## License

Same as Claw Compiler project.

---

**Last Updated:** 2026-04-16
**Implementation Status:** ~10% Complete
**Estimated Completion:** 2026-06-12
