# Contributing to Aperture

Thank you for considering contributing to Aperture! This document outlines the guidelines and processes for contributing to the project.

---

## Core Principles

### The Iron Law

> **"aperture-core and aperture-geometry SHALL NOT import net.minecraft.*"**

The Kernel layer must remain Minecraft-free. This is enforced by CI checks and is non-negotiable.

### Architecture Layers

Aperture is organized into four layers with strict dependency rules:

1. **Kernel** (Pure abstractions) — No Minecraft dependencies
2. **Platform** (Runtime system) — Bridges Kernel to Minecraft
3. **Editor** (CAD interaction) — Provides editing tools
4. **Applications** (Content) — Specific opening types

Dependencies flow downward only. See [`docs/architecture/00-dependency-rules.md`](docs/architecture/00-dependency-rules.md) for details.

---

## Development Process

We follow an 8-step development process documented in [`docs/DEVELOPMENT.md`](docs/DEVELOPMENT.md):

1. **Architecture** — Document design decisions
2. **Specification** — Write detailed specs
3. **Interface** — Define contracts
4. **Data Structures** — Implement models
5. **Tests** — Write tests first
6. **Implementation** — Implement the feature
7. **Examples** — Add usage examples
8. **Documentation** — Update docs

**Always start with documentation and tests.**

---

## Before You Start

### Read the Documentation

- [`docs/APERTURE-REDEFINED.md`](docs/APERTURE-REDEFINED.md) — Strategic framework
- [`docs/architecture/00-INDEX.md`](docs/architecture/00-INDEX.md) — Architecture index
- [`docs/architecture/01-vision.md`](docs/architecture/01-vision.md) — Project vision

### Understand the Priorities

**Platform before Content.**

We don't need more door types. We need a platform so robust that adding a new door type requires zero pipeline code changes — just JSON + assets.

---

## Setting Up Development Environment

### Prerequisites

- **Java 21+** (JDK 21 or later)
- **Gradle 8.5+** (wrapper included)
- **Git**
- **IDE**: IntelliJ IDEA or VS Code with Java extensions

### Build the Project

```bash
git clone https://github.com/yourusername/aperture.git
cd aperture
./gradlew build
```

### Run Tests

```bash
./gradlew test
```

### Check Architecture Boundaries

```bash
./gradlew checkKernelPurity
```

---

## Making Changes

### 1. Create a Branch

```bash
git checkout -b feature/your-feature-name
```

Branch naming:
- `feature/` — New features
- `fix/` — Bug fixes
- `docs/` — Documentation updates
- `refactor/` — Code refactoring

### 2. Make Your Changes

Follow the 8-step development process:

1. **Document first** — Update relevant architecture docs
2. **Write tests** — Before implementation
3. **Implement** — Keep changes focused
4. **Verify** — Run tests and architecture checks

### 3. Follow Code Style

- **Java**: Follow standard Java conventions
- **Formatting**: Use IDE auto-formatting (IntelliJ defaults)
- **Naming**: Clear, descriptive names (no abbreviations)
- **Comments**: Explain why, not what

### 4. Commit Messages

Use conventional commit format:

```
type(scope): short description

Longer description if needed.

Fixes #123
```

Types:
- `feat:` — New feature
- `fix:` — Bug fix
- `docs:` — Documentation
- `refactor:` — Code restructuring
- `test:` — Test additions/changes
- `chore:` — Build/tooling changes

Example:
```
feat(kernel): add Bezier curve support

Implements cubic Bezier curves for profile generation.
Includes De Casteljau's algorithm for point sampling.

Closes #45
```

---

## Pull Request Process

### 1. Ensure Quality

Before submitting:

- [ ] All tests pass (`./gradlew test`)
- [ ] Architecture checks pass (`./gradlew checkKernelPurity`)
- [ ] Code follows style guidelines
- [ ] Documentation updated
- [ ] No Minecraft imports in Kernel modules

### 2. Create Pull Request

- **Title**: Clear, descriptive summary
- **Description**: Explain what and why
- **Link issues**: Reference related issues
- **Screenshots**: For UI changes

### 3. PR Template

```markdown
## Description
Brief description of changes.

## Type of Change
- [ ] Bug fix
- [ ] New feature
- [ ] Breaking change
- [ ] Documentation update

## Checklist
- [ ] Tests pass
- [ ] Architecture boundaries respected
- [ ] Documentation updated
- [ ] No Kernel purity violations

## Related Issues
Closes #123
```

### 4. Review Process

- Maintainers will review your PR
- Address feedback promptly
- Keep discussions respectful and constructive
- CI must pass before merge

---

## Architecture Guidelines

### Kernel Layer Rules

**DO**:
- ✅ Use pure Java types (List, Map, Optional)
- ✅ Use Kernel types (Point3D, Vector3D, Transform)
- ✅ Keep logic Minecraft-agnostic
- ✅ Write extensive tests

**DON'T**:
- ❌ Import `net.minecraft.*`
- ❌ Import `net.fabricmc.*`
- ❌ Depend on Platform/Editor modules
- ❌ Use mutable state without justification

### Platform Layer Rules

**DO**:
- ✅ Bridge Kernel to Minecraft
- ✅ Handle voxelization and rendering
- ✅ Manage world persistence
- ✅ Keep editor-agnostic

**DON'T**:
- ❌ Put business logic here (belongs in Kernel)
- ❌ Depend on Editor modules
- ❌ Hardcode opening types

### Editor Layer Rules

**DO**:
- ✅ Provide CAD-quality interactions
- ✅ Implement undo/redo
- ✅ Keep content-agnostic

**DON'T**:
- ❌ Hardcode specific opening types
- ❌ Mix interaction logic with rendering

---

## Testing Guidelines

### Write Tests First

Follow TDD (Test-Driven Development):

1. Write a failing test
2. Implement minimal code to pass
3. Refactor

### Test Types

**Unit Tests** (required for all new code):
```java
@Test
void transform_rotateY90_rotatesCorrectly() {
    var t = Transform.rotationY(Math.PI / 2);
    var p = new Point3D(1, 0, 0);
    var result = t.transform(p);
    assertEquals(0, result.x(), 0.001);
}
```

**Integration Tests** (for cross-module features):
```java
@Test
void pipeline_fixedWindow_generatesCorrectMesh() {
    var result = pipeline.execute(fixedWindowType, params);
    assertNotNull(result.mesh());
    assertTrue(result.mesh().vertices().size() > 0);
}
```

**Golden Tests** (for pipeline stability):
```java
@Test
void pipeline_fixedWindow_matchesGolden() {
    var result = pipeline.execute(fixedWindowType, params);
    var golden = loadGoldenMesh("fixed_window_1200x1500.json");
    assertMeshEquals(golden, result.mesh());
}
```

---

## Documentation Guidelines

### Update Architecture Docs

When adding features, update relevant docs in `docs/architecture/`:

- **New systems**: Create new document
- **Changes to existing systems**: Update existing doc
- **Cross-cutting concerns**: Update multiple docs + index

### Code Documentation

**Javadoc** for public APIs:
```java
/**
 * Transforms a point by this transformation matrix.
 * 
 * @param point The point to transform
 * @return The transformed point
 */
public Point3D transform(Point3D point) {
    // ...
}
```

**Comments** for complex logic:
```java
// Use De Casteljau's algorithm for stable evaluation
// rather than direct power basis computation
return deCasteljau(controlPoints, t);
```

---

## Issue Guidelines

### Reporting Bugs

Include:
- **Description**: What happened vs. what you expected
- **Steps to reproduce**: Minimal reproduction case
- **Environment**: Java version, OS, Minecraft version
- **Logs**: Relevant error messages or stack traces

### Requesting Features

Include:
- **Use case**: Why you need this feature
- **Proposed solution**: How you envision it working
- **Alternatives**: Other approaches you considered
- **Layer**: Which layer this belongs to (Kernel/Platform/Editor/Applications)

---

## Code Review Checklist

As a reviewer, check:

- [ ] Architecture boundaries respected
- [ ] No Kernel purity violations
- [ ] Tests added/updated
- [ ] Documentation updated
- [ ] Code style consistent
- [ ] No unnecessary complexity
- [ ] Performance considerations addressed
- [ ] Error handling appropriate

---

## Community Guidelines

### Be Respectful

- Treat others with kindness and respect
- Welcome newcomers
- Assume good intent
- Give constructive feedback

### Be Collaborative

- Share knowledge
- Help others learn
- Ask questions when unclear
- Document your decisions

### Be Professional

- Keep discussions on-topic
- Avoid off-topic debates
- Respect maintainer decisions
- Follow the Code of Conduct

---

## Getting Help

- **Questions**: Open a discussion on GitHub
- **Issues**: Check existing issues first
- **Documentation**: Start with `docs/architecture/00-INDEX.md`
- **Real-time chat**: [If applicable, add Discord/Slack link]

---

## Recognition

Contributors are recognized in:
- GitHub contributors list
- Release notes
- Project documentation

Significant contributions may result in commit access.

---

## License

By contributing to Aperture, you agree that your contributions will be licensed under the CC0-1.0 license.

---

Thank you for contributing to Aperture! 🚪✨
