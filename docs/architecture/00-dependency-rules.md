# 00 — Dependency Rules

**Purpose**: Enforce architectural layer boundaries through automated CI checks.

---

## The Iron Law

> **"Aperture-core and aperture-geometry SHALL NOT import net.minecraft.*"**

The Kernel layer (pure abstractions) must remain Minecraft-free to ensure:
- Reusability across contexts (other voxel games, external tools)
- Testability without Minecraft runtime
- Clear separation of concerns
- Future portability

---

## Layer Dependency Rules

### Layer 1: Kernel (Pure Abstractions)

**Modules**: `aperture-math`, `aperture-parameter`, `aperture-core`, `aperture-geometry`, `aperture-opening`, `aperture-pipeline`, `aperture-kernel`

**Allowed Dependencies**:
- ✅ Java Standard Library
- ✅ Other Kernel modules
- ✅ Pure Java libraries (Gson, Commons Math, etc.)

**Forbidden Dependencies**:
- ❌ `net.minecraft.*`
- ❌ `net.fabricmc.*`
- ❌ Any Platform/Editor/Application modules

**Rationale**: Kernel abstractions must be environment-agnostic.

---

### Layer 2: Platform (Runtime System)

**Modules**: `aperture-runtime`, `aperture-render`, `aperture-fabric`

**Allowed Dependencies**:
- ✅ All Kernel modules
- ✅ `net.minecraft.*` (Minecraft APIs)
- ✅ `net.fabricmc.*` (Fabric APIs)
- ✅ Other Platform modules

**Forbidden Dependencies**:
- ❌ Editor modules (Platform should not depend on Editor)
- ❌ Application modules (Platform should not know about specific door/window types)

**Rationale**: Platform bridges Kernel to Minecraft but remains editor-agnostic.

---

### Layer 3: Editor (CAD Interaction)

**Modules**: `aperture-editor`, client-side GUI code

**Allowed Dependencies**:
- ✅ All Kernel modules
- ✅ All Platform modules
- ✅ Minecraft client APIs
- ✅ Rendering libraries

**Forbidden Dependencies**:
- ❌ Application modules (Editor should not depend on specific opening types)

**Rationale**: Editor provides interaction abstractions, not content.

---

### Layer 4: Applications (Content)

**Modules**: Opening type definitions (JSON data packs), component generators

**Allowed Dependencies**:
- ✅ All Kernel modules
- ✅ All Platform modules
- ✅ Asset files (profiles, materials)

**Forbidden Dependencies**:
- ❌ Other Application modules (opening types should be independent)

**Rationale**: Applications are content, not infrastructure.

---

## Module Dependency Graph

```
┌─────────────────────────────────────────────────────┐
│  Applications (data packs, generators)              │
└─────────────────────────────────────────────────────┘
                        ↓
┌─────────────────────────────────────────────────────┐
│  Editor (aperture-editor, GUI)                      │
└─────────────────────────────────────────────────────┘
                        ↓
┌─────────────────────────────────────────────────────┐
│  Platform (runtime, fabric, render, opening-geom)   │
└─────────────────────────────────────────────────────┘
                        ↓
┌─────────────────────────────────────────────────────┐
│  Kernel (core, geometry, math)                      │
│  NO MINECRAFT IMPORTS                               │
└─────────────────────────────────────────────────────┘
```

**Rule**: Modules may only depend on modules in the same layer or layers below.

---

## Gradle Dependency Enforcement

### Build Script Check

Add to `build.gradle.kts`:

```kotlin
// Enforce Kernel purity
tasks.register("checkKernelPurity") {
    doLast {
        val kernelModules = listOf("aperture-core", "aperture-geometry", "aperture-math")
        val forbiddenPatterns = listOf("net.minecraft", "net.fabricmc")
        
        kernelModules.forEach { module ->
            val sourceDir = file("$module/src/main/java")
            if (!sourceDir.exists()) return@forEach
            
            sourceDir.walk().filter { it.extension == "java" }.forEach { file ->
                val content = file.readText()
                forbiddenPatterns.forEach { pattern ->
                    if (content.contains("import $pattern")) {
                        throw GradleException(
                            "IRON LAW VIOLATION: $file imports $pattern\n" +
                            "Kernel modules (aperture-core, aperture-geometry, aperture-math) " +
                            "must not depend on Minecraft."
                        )
                    }
                }
            }
        }
        println("✅ Kernel purity check passed")
    }
}

// Run on every build
tasks.named("build") {
    dependsOn("checkKernelPurity")
}
```

### Dependency Declaration Validation

```kotlin
// In each Kernel module's build.gradle.kts
configurations.all {
    resolutionStrategy.eachDependency {
        if (requested.group.startsWith("net.minecraft") || 
            requested.group.startsWith("net.fabricmc")) {
            throw GradleException(
                "Kernel module cannot depend on Minecraft/Fabric: ${requested.module}"
            )
        }
    }
}
```

---

## CI Pipeline Check

### GitHub Actions Workflow

Add to `.github/workflows/architecture-check.yml`:

```yaml
name: Architecture Boundary Check

on: [push, pull_request]

jobs:
  check-kernel-purity:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      
      - name: Set up JDK 21
        uses: actions/setup-java@v3
        with:
          java-version: '21'
          
      - name: Check Kernel Purity
        run: ./gradlew checkKernelPurity
        
      - name: Fail if violations found
        if: failure()
        run: |
          echo "❌ ARCHITECTURE VIOLATION DETECTED"
          echo "Kernel modules must not import net.minecraft.* or net.fabricmc.*"
          exit 1
```

---

## Manual Inspection Script

`scripts/check-dependencies.sh`:

```bash
#!/bin/bash

echo "Checking Kernel module purity..."

KERNEL_MODULES=("aperture-core" "aperture-geometry" "aperture-math")
VIOLATIONS=0

for module in "${KERNEL_MODULES[@]}"; do
    echo "Checking $module..."
    
    if grep -r "import net.minecraft" "$module/src/main/java" 2>/dev/null; then
        echo "❌ VIOLATION: $module imports net.minecraft.*"
        VIOLATIONS=$((VIOLATIONS + 1))
    fi
    
    if grep -r "import net.fabricmc" "$module/src/main/java" 2>/dev/null; then
        echo "❌ VIOLATION: $module imports net.fabricmc.*"
        VIOLATIONS=$((VIOLATIONS + 1))
    fi
done

if [ $VIOLATIONS -eq 0 ]; then
    echo "✅ All Kernel modules are pure"
    exit 0
else
    echo "❌ Found $VIOLATIONS violation(s)"
    exit 1
fi
```

---

## Common Violations and Fixes

### Violation 1: Minecraft Types in Kernel

**Bad**:
```java
// aperture-core/src/.../OpeningInstance.java
import net.minecraft.util.math.BlockPos;

public class OpeningInstance {
    private BlockPos position;  // ❌ Minecraft type in Kernel
}
```

**Good**:
```java
// aperture-core/src/.../OpeningInstance.java
import aperture.math.Point3D;

public class OpeningInstance {
    private Point3D position;  // ✅ Pure Kernel type
}
```

### Violation 2: Platform Logic in Kernel

**Bad**:
```java
// aperture-core/src/.../ComponentGenerator.java
import net.minecraft.block.BlockState;

public interface ComponentGenerator {
    BlockState generate();  // ❌ Returns Minecraft type
}
```

**Good**:
```java
// aperture-core/src/.../ComponentGenerator.java
public interface ComponentGenerator {
    GeometrySolid generate();  // ✅ Returns Kernel type
}

// aperture-runtime/src/.../ComponentVoxelizer.java
import net.minecraft.block.BlockState;

public class ComponentVoxelizer {
    public BlockState toBlockState(GeometrySolid solid) {
        // Platform converts Kernel geometry to Minecraft blocks
    }
}
```

### Violation 3: Editor Depending on Specific Opening Types

**Bad**:
```java
// aperture-editor/src/.../GizmoFactory.java
import aperture.applications.door.DoorGizmo;

public class GizmoFactory {
    public Gizmo create(OpeningInstance opening) {
        if (opening.type().id().equals("aperture:door")) {
            return new DoorGizmo();  // ❌ Editor knows about specific door
        }
    }
}
```

**Good**:
```java
// aperture-editor/src/.../GizmoFactory.java
public class GizmoFactory {
    public Gizmo create(OpeningInstance opening) {
        // Generic gizmo based on parameter schema
        return new ParametricGizmo(opening.type().parameters());  // ✅ Generic
    }
}
```

---

## Exception Cases

### Allowed Cross-Layer References

**Test Dependencies**: Test code may depend on layers above for integration testing.

```kotlin
// aperture-core/build.gradle.kts
dependencies {
    testImplementation(project(":aperture-runtime"))  // ✅ OK for tests
}
```

**Type References**: Types may reference higher layers through interfaces, but not implementations.

```java
// aperture-core (Kernel)
public interface GeometryRenderer {
    void render(GeometrySolid solid);  // ✅ Interface in Kernel
}

// aperture-render (Platform)
public class MinecraftGeometryRenderer implements GeometryRenderer {
    // ✅ Implementation in Platform
}
```

---

## Enforcement Strategy

### Development Phase

1. **Run `checkKernelPurity` locally** before committing
2. **Pre-commit hook** runs architecture checks
3. **PR template** includes "Architecture boundaries respected" checkbox

### CI Phase

1. **GitHub Actions** runs on every push/PR
2. **Fail CI** if violations detected
3. **Block merge** until violations fixed

### Code Review Phase

1. **Reviewer checks** for architectural violations
2. **Document exceptions** if absolutely necessary
3. **Refactor** violations before merge

---

## Documentation Requirements

### Module README

Each module must document its layer and dependencies:

```markdown
# aperture-core

**Layer**: Kernel (Pure Abstractions)

**Dependencies**:
- aperture-math (Kernel)
- Gson (JSON parsing)

**Dependents**:
- aperture-runtime (Platform)
- aperture-editor (Editor)

**Constraints**:
- MUST NOT import net.minecraft.*
- MUST NOT import net.fabricmc.*
```

### Architecture Decision Records

Exceptions to dependency rules must be documented as ADRs:

```markdown
# ADR-0003: Allow aperture-core to depend on SLF4J

**Status**: Accepted

**Context**: Kernel needs logging for debugging.

**Decision**: Allow dependency on SLF4J (logging facade, not implementation).

**Rationale**: SLF4J is a pure Java logging interface with no Minecraft coupling.
```

---

## Current Status

| Check | Status | Priority |
|-------|--------|----------|
| Manual script | ✅ | - |
| Gradle task | ❌ | 🔥 HIGH |
| CI workflow | ❌ | 🔥 HIGH |
| Pre-commit hook | ❌ | MEDIUM |
| Module READMEs | ❌ | MEDIUM |

---

## Action Items

- [ ] Implement `checkKernelPurity` Gradle task
- [ ] Add CI workflow for architecture checks
- [ ] Audit existing code for violations
- [ ] Fix any violations found
- [ ] Document exceptions as ADRs
- [ ] Add module READMEs with dependency declarations
- [ ] Create pre-commit hook script
- [ ] Update CONTRIBUTING.md with architecture rules

---

**Document Status**: ✅ Complete  
**Last Updated**: 2026-07-16  
**Implementation**: ⏸️ Planned (Week 1)  
**Next Review**: After CI implementation
