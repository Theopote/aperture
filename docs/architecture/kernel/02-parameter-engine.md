# 02 — Parameter Engine

**Layer**: Kernel  
**Module**: `aperture-core/parametric`, `aperture-core/parameter`  
**Status**: ✅ Implementation ~80% complete, Documentation new  
**Dependencies**: None (pure kernel)

---

## Overview

The Parameter Engine is Aperture's type-safe, constraint-aware parameter system. Every opening in Aperture is fully driven by parameters — dimensions, materials, counts, ratios, choices. The parameter engine handles:

- Type-safe parameter definitions with metadata
- Default value resolution
- Constraint validation
- Sparse override merging
- Schema versioning and migration
- Future: expression evaluation and dependency graphs

**Design Principle**: Parameters are **immutable data**, not configuration objects. A `ParameterSet` is a snapshot of values at a point in time.

---

## Core Concepts

### Parameter vs ParameterSet

```
Parameter (Definition)          ParameterSet (Values)
    ↓                                  ↓
"width: length, min 300"          { "width": 1200 }
"height: length, min 300"         { "height": 1500 }
"mullions: count, max 10"         { "mullions": 2 }
```

- **Parameter**: Metadata defining a parameter (type, constraints, default, units, UI hints)
- **ParameterSet**: A map of parameter names → actual values

### ParametricSchema

A `ParametricSchema` is the collection of all `Parameter` definitions for an opening type. It knows:
- What parameters exist
- Their types and constraints
- Default values
- How to merge instance overrides with defaults

**Example**:
```java
var schema = ParametricSchema.builder()
    .put("width", RangeParameter.length(1200, 300, 6000))
    .put("height", RangeParameter.length(1500, 300, 4000))
    .put("mullions", NumberParameter.count(0, 0, 10))
    .put("frame_material", MaterialParameter.slot("frame"))
    .put("operable", BooleanParameter.of(false))
    .build();
```

---

## Parameter Types

Aperture supports 8 parameter types, each with specific semantics and validation.

### 1. NumberParameter

**Purpose**: Numeric values with optional constraints.

**Variants**:
- `length(default, min, max)` — Dimensions in millimeters
- `count(default, min, max)` — Integer counts (panels, mullions, etc.)
- `angle(default, min, max)` — Angles in degrees
- `ratio(default, min, max)` — Normalized ratios (0.0–1.0)

**Example**:
```java
RangeParameter.length(1200, 300, 6000)  // width in mm
NumberParameter.count(2, 1, 5)          // panel count
NumberParameter.angle(0, 0, 90)         // open angle
NumberParameter.ratio(0.5, 0, 1)        // open ratio
```

**Storage**: `double` (internally millimeters, degrees, or dimensionless)

**Validation**: `value >= min && value <= max`

---

### 2. BooleanParameter

**Purpose**: True/false flags.

**Example**:
```java
BooleanParameter.of(false)  // operable
BooleanParameter.of(true)   // double_glazed
```

**Storage**: `boolean`

**Validation**: None (always valid)

---

### 3. EnumParameter

**Purpose**: Exclusive choice from a fixed set of symbolic values.

**Example**:
```java
EnumParameter.of("left", Set.of("left", "right"))        // hinge_side
EnumParameter.of("surface", Set.of("surface", "pocket")) // track_type
```

**Storage**: `String` (enum value)

**Validation**: `value ∈ allowedValues`

**Use When**: The options are mutually exclusive identifiers that code needs to switch on.

---

### 4. ChoiceParameter

**Purpose**: Exclusive choice from a set of options with human-readable labels.

**Example**:
```java
ChoiceParameter.of(
    new ChoiceOption("modern", "Modern Minimalist"),
    new ChoiceOption("traditional", "Traditional Raised Panel"),
    new ChoiceOption("glass", "Full Glass")
)
```

**Storage**: `String` (choice ID)

**Validation**: `value ∈ options.map(_.id)`

**Use When**: Options have display names different from internal IDs, or when you want to provide descriptions.

**Difference from Enum**: `ChoiceParameter` is UI-focused (labels, descriptions), `EnumParameter` is code-focused (symbolic identifiers).

---

### 5. MaterialParameter

**Purpose**: Reference to a material definition in the material catalog.

**Example**:
```java
MaterialParameter.slot("frame")    // frame_material
MaterialParameter.slot("glazing")  // glass_material
```

**Storage**: `String` (material ID like `"aperture:oak_frame"`)

**Validation**: Deferred to material resolver (material must exist in catalog)

**Resolution**: `MaterialResolver` looks up the material definition and returns block IDs, textures, etc.

---

### 6. RangeParameter

**Purpose**: Numeric range with units and precision.

**Example**:
```java
RangeParameter.length(1200, 300, 6000)      // width (mm)
RangeParameter.custom(50, 20, 150, "mm")    // frame_width
```

**Storage**: `double`

**Validation**: `value >= min && value <= max`

**Difference from NumberParameter**: `RangeParameter` explicitly declares units and is used when the parameter represents a physical dimension with unit conversions.

---

### 7. ArrayParameter (Future)

**Purpose**: List of values (e.g., panel widths in a curtain wall grid).

**Status**: Planned for Phase 11 (Curtain Wall), not yet implemented.

**Example** (conceptual):
```java
ArrayParameter.of(
    NumberParameter.length(900, 300, 2000),  // element type
    1, 10                                     // min/max count
)
```

---

### 8. NestedParameter (Future)

**Purpose**: Parameter groups (e.g., `glass.thickness`, `glass.tint`, `glass.low_e`).

**Status**: Planned for advanced configurations, not yet implemented.

**Example** (conceptual):
```java
NestedParameter.group(
    "thickness", NumberParameter.length(6, 3, 12),
    "tint", ChoiceParameter.of(...),
    "low_e", BooleanParameter.of(false)
)
```

---

## Parameter Metadata

Every `Parameter` carries metadata for validation, UI rendering, and serialization:

```java
public record ParameterMetadata(
    ParameterKind kind,          // NUMBER, BOOLEAN, ENUM, etc.
    NumberUnit unit,             // LENGTH, ANGLE, COUNT, RATIO, NONE
    Object defaultValue,         // Type-specific default
    Object minValue,             // Optional constraint
    Object maxValue,             // Optional constraint
    Set<String> allowedValues,   // For ENUM/CHOICE
    String description,          // UI tooltip (optional)
    boolean userEditable         // Can user change this? (vs. derived)
)
```

**Example**:
```java
var widthParam = RangeParameter.length(1200, 300, 6000);
var metadata = widthParam.metadata();

metadata.kind()         // ParameterKind.NUMBER
metadata.unit()         // NumberUnit.LENGTH
metadata.defaultValue() // 1200.0
metadata.minValue()     // 300.0
metadata.maxValue()     // 6000.0
```

---

## Parameter Resolution Flow

The parameter engine follows a strict resolution flow:

```
Definition (ParametricSchema)
    ↓
Sparse Overrides (from Instance or Editor)
    ↓
Merge with Defaults
    ↓
Resolved ParameterSet (complete, valid)
    ↓
Constraint Validation
    ↓
Generator / Editor / Preview
```

### Step 1: Sparse Overrides

An `OpeningInstance` stores only **overrides** (values different from schema defaults):

```json
{
  "typeId": "aperture:fixed_window",
  "parameters": {
    "width": 1800,
    "mullions": 2
  }
}
```

Missing keys (`height`, `frame_material`, etc.) use schema defaults.

### Step 2: Merge with Defaults

`OpeningTypeDefinition.resolveParameters(overrides)` produces a **complete** `ParameterSet`:

```java
var definition = catalog.get(OpeningId.of("aperture:fixed_window"));
var overrides = ParameterSet.builder()
    .put("width", 1800.0)
    .put("mullions", 2.0)
    .build();

var resolved = definition.resolveParameters(overrides);

resolved.getDouble("width")    // 1800.0 (from override)
resolved.getDouble("height")   // 1500.0 (from schema default)
resolved.getInt("mullions")    // 2 (from override)
resolved.getString("frame_material") // "aperture:oak_frame" (default)
```

**Implementation** (`ParametricSchema.mergeDefaults`):
```java
public ParameterSet mergeDefaults(ParameterSet overrides) {
    var builder = ParameterSet.builder();
    for (var entry : parameters.entrySet()) {
        var name = entry.getKey();
        var param = entry.getValue();
        var value = overrides.contains(name) 
            ? overrides.get(name) 
            : param.metadata().defaultValue();
        builder.put(name, value);
    }
    return builder.build();
}
```

### Step 3: Constraint Validation

After resolution, constraints are evaluated (see `ConstraintEvaluator`):

```java
var validator = new ParametricValidator(definition);
var result = validator.validate(resolved);

if (!result.isValid()) {
    for (var issue : result.getIssues()) {
        System.err.println(issue.getMessage());
    }
}
```

**Constraint Types**:
1. **Range constraints**: Built into parameter metadata (`min`, `max`)
2. **Expression constraints**: Custom rules in the definition (see [kernel/04-constraint-solver.md](04-constraint-solver.md))

**Example Constraint**:
```json
{
  "constraints": [
    {
      "expr": "width > height * 0.3",
      "message": "Width must exceed 30% of height"
    }
  ]
}
```

---

## Unified Resolution Path

**Critical Design Decision**: All code paths must use the **same resolver**.

### Anti-Pattern (Current Issue)

```java
// ❌ Editor directly modifies parameters
editor.setWidth(1800);

// ❌ Preview uses different resolver
preview.updateParameter("width", 1800);

// ❌ Generator bypasses resolution
generator.generate(rawParameters);
```

**Result**: Inconsistent behavior. Editor preview shows one thing, committed opening shows another.

### Correct Pattern (Target Architecture)

```java
// ✅ Single entry point
var resolver = new OpeningParameterResolver(definition);

// Editor
var resolved = resolver.resolve(editor.getParameterOverrides());
editor.updatePreview(resolved);

// Preview
var resolved = resolver.resolve(placementSession.getParameterOverrides());
placementService.updatePreview(resolved);

// Generator
var resolved = resolver.resolve(instance.getParameters());
generator.generate(definition, resolved);
```

**Benefit**: Change `width` → all three see the same value, same constraint validation, same default merging.

**Implementation Status**: 
- ⚠️ Resolver exists (`OpeningTypeDefinition.resolveParameters`)
- ❌ Not used consistently across Editor/Preview/Generate
- 🎯 **Week 2 Priority**: Refactor all call sites to use unified path

---

## Parameter Serialization

### JSON Schema

Parameters are serialized as a simple key-value map:

```json
{
  "parameters": {
    "width": 1800,
    "height": 1500,
    "mullions": 2,
    "frame_material": "aperture:oak_frame",
    "operable": false
  }
}
```

**Type Encoding**:
- `NUMBER`, `RANGE`: JSON number (internally stored as `double`)
- `BOOLEAN`: JSON boolean
- `ENUM`, `CHOICE`, `MATERIAL`: JSON string
- `ARRAY` (future): JSON array
- `NESTED` (future): JSON object

### Sparse Storage

Instances store only **overrides**:

```java
var schema = definition.getParametricSchema();
var fullValues = ParameterSet.builder()
    .put("width", 1800.0)
    .put("height", 1500.0)  // same as default
    .put("mullions", 2.0)
    .build();

var sparse = schema.extractOverrides(fullValues);
// Result: { "width": 1800.0, "mullions": 2.0 }
// "height" omitted because it matches the schema default
```

**Benefit**: 
- Smaller save files
- Schema changes (e.g., raising default height from 1500 to 1800) automatically apply to old instances

---

## Schema Versioning

Every `ParametricSchema` is versioned:

```json
{
  "schemaVersion": 1,
  "parameters": { ... }
}
```

**Migration Path** (future):

```java
public interface ParameterMigration {
    int fromVersion();
    int toVersion();
    ParameterSet migrate(ParameterSet oldParams);
}
```

**Example**: Rename parameter
```java
class RenameMullionsToMullionCount implements ParameterMigration {
    public int fromVersion() { return 1; }
    public int toVersion() { return 2; }
    
    public ParameterSet migrate(ParameterSet old) {
        return ParameterSet.builder()
            .putAll(old)
            .remove("mullions")
            .put("mullion_count", old.getInt("mullions"))
            .build();
    }
}
```

**Status**: Migration framework exists (`MigrationContext`), but no migrations written yet.

---

## Constraint Expressions

Parameters can have **constraint rules** that cross parameter boundaries.

**Example**:
```json
{
  "parameters": {
    "width": { "type": "length", "default": 1800 },
    "height": { "type": "length", "default": 2100 },
    "panel_count": { "type": "count", "default": 2 }
  },
  "constraints": [
    {
      "expr": "width / panel_count >= 600",
      "message": "Each panel must be at least 600mm wide"
    },
    {
      "expr": "height / width <= 2.5",
      "message": "Aspect ratio must not exceed 2.5:1"
    }
  ]
}
```

**Expression Syntax** (subset of mathematical expressions):
- Operators: `+`, `-`, `*`, `/`, `%`, `^` (power)
- Comparisons: `<`, `<=`, `>`, `>=`, `==`, `!=`
- Logical: `&&`, `||`, `!`
- Parentheses: `()`
- Functions (future): `min()`, `max()`, `abs()`, `sqrt()`

**Evaluation**:
```java
var evaluator = new ConstraintEvaluator(definition.getConstraints());
var context = ConstraintContext.from(resolvedParameters);

for (var constraint : definition.getConstraints()) {
    var result = evaluator.evaluate(constraint.expr(), context);
    if (!result) {
        // Constraint violated
        System.err.println(constraint.message());
    }
}
```

**Implementation**: See `aperture-core/constraint/`
- `ExpressionLexer` — Tokenizes expressions
- `ExpressionParser` — Builds AST
- `ConstraintEvaluator` — Evaluates AST against parameter context

**Status**: ✅ Basic expressions work. Advanced features (functions, conditionals) are future.

---

## Parameter Dependencies (Future)

**Goal**: When parameter A changes, automatically update parameter B.

**Example**: 
```
user changes width → panel_width = width / panel_count (auto-computed)
```

**Planned Approach** (Phase 12: NodeCraft):
- Parameters can be **expressions** instead of fixed values
- `ParametricGraph` describes dependencies
- Topological sort ensures correct evaluation order
- Incremental re-evaluation (only recompute downstream parameters)

**Current Limitation**: All parameters are independent. Derived values must be computed in the generator.

---

## Parameter UI Hints (Future)

Parameters can carry UI rendering hints:

```java
public record ParameterMetadata(
    // ... existing fields
    String group,           // "Dimensions", "Materials", "Hardware"
    int displayOrder,       // Sort order in Inspector
    String icon,            // Icon ID for UI
    boolean advanced        // Hide in basic mode
)
```

**Status**: Planned for Editor Phase (Phase C). Metadata structure exists but hints not yet used.

---

## Example: Fixed Window Schema

Full example of a well-defined parameter schema:

```java
var schema = ParametricSchema.builder()
    // Dimensions
    .put("width", RangeParameter.length(1200, 300, 6000))
    .put("height", RangeParameter.length(1500, 300, 4000))
    .put("frame_width", RangeParameter.length(50, 20, 150))
    .put("frame_depth", RangeParameter.length(80, 30, 200))
    
    // Layout
    .put("mullions", NumberParameter.count(0, 0, 10))
    .put("mullion_width", RangeParameter.length(50, 30, 100))
    
    // State
    .put("open_angle", NumberParameter.angle(0, 0, 90))
    
    // Materials
    .put("frame_material", MaterialParameter.slot("frame"))
    .put("glass_material", MaterialParameter.slot("glazing"))
    
    // Options
    .put("sill", BooleanParameter.of(true))
    .put("trim", BooleanParameter.of(false))
    
    .build();
```

**Constraints**:
```json
[
  {
    "expr": "width > height * 0.3",
    "message": "Width must exceed 30% of height"
  },
  {
    "expr": "mullions == 0 || mullion_width < width / (mullions + 1)",
    "message": "Mullions too wide for opening width"
  }
]
```

**Instance Override**:
```json
{
  "typeId": "aperture:fixed_window",
  "parameters": {
    "width": 1800,
    "mullions": 2,
    "trim": true
  }
}
```

**Resolved Values**:
```
width: 1800.0          (override)
height: 1500.0         (default)
frame_width: 50.0      (default)
frame_depth: 80.0      (default)
mullions: 2            (override)
mullion_width: 50.0    (default)
open_angle: 0.0        (default)
frame_material: "aperture:oak_frame" (default)
glass_material: "aperture:glazing_clear" (default)
sill: true             (default)
trim: true             (override)
```

---

## API Reference

### Core Classes

**`ParametricSchema`** — Collection of parameter definitions
```java
public record ParametricSchema(Map<String, Parameter> parameters) {
    public static Builder builder();
    public ParameterSet mergeDefaults(ParameterSet overrides);
    public ParameterSet extractOverrides(ParameterSet fullValues);
}
```

**`Parameter`** — Base interface for all parameter types
```java
public interface Parameter {
    ParameterMetadata metadata();
}
```

**`ParameterSet`** — Immutable map of parameter values
```java
public record ParameterSet(Map<String, Object> values) {
    public static Builder builder();
    public double getDouble(String name);
    public int getInt(String name);
    public boolean getBoolean(String name);
    public String getString(String name);
    public boolean contains(String name);
}
```

**`ParameterMetadata`** — Type, constraints, units, defaults
```java
public record ParameterMetadata(
    ParameterKind kind,
    NumberUnit unit,
    Object defaultValue,
    Object minValue,
    Object maxValue,
    Set<String> allowedValues,
    String description,
    boolean userEditable
)
```

### Parameter Type Constructors

**`RangeParameter`**:
```java
RangeParameter.length(double defaultValue, double min, double max)
RangeParameter.custom(double defaultValue, double min, double max, String unit)
```

**`NumberParameter`**:
```java
NumberParameter.count(int defaultValue, int min, int max)
NumberParameter.angle(double defaultValue, double min, double max)
NumberParameter.ratio(double defaultValue, double min, double max)
```

**`BooleanParameter`**:
```java
BooleanParameter.of(boolean defaultValue)
```

**`EnumParameter`**:
```java
EnumParameter.of(String defaultValue, Set<String> allowedValues)
```

**`ChoiceParameter`**:
```java
ChoiceParameter.of(ChoiceOption... options)
ChoiceParameter.of(List<ChoiceOption> options)
```

**`MaterialParameter`**:
```java
MaterialParameter.slot(String slotName)
```

### Validation

**`ParametricValidator`**:
```java
public class ParametricValidator {
    public ParametricValidator(OpeningTypeDefinition definition);
    public ValidationResult validate(ParameterSet parameters);
}
```

**`ConstraintEvaluator`**:
```java
public class ConstraintEvaluator {
    public ConstraintEvaluator(List<ConstraintRule> constraints);
    public boolean evaluate(String expr, ConstraintContext context);
}
```

---

## Testing Strategy

### Unit Tests

**Test coverage**:
- ✅ Parameter type construction
- ✅ Metadata extraction
- ✅ Default value merging
- ✅ Override extraction
- ✅ Constraint evaluation (basic expressions)
- ⏳ Edge cases (nulls, type mismatches, missing parameters)

**Example Test**:
```java
@Test
void parameterSet_mergeDefaults_appliesOverrides() {
    var schema = ParametricSchema.builder()
        .put("width", RangeParameter.length(1200, 300, 6000))
        .put("height", RangeParameter.length(1500, 300, 4000))
        .build();
    
    var overrides = ParameterSet.builder()
        .put("width", 1800.0)
        .build();
    
    var resolved = schema.mergeDefaults(overrides);
    
    assertEquals(1800.0, resolved.getDouble("width"));  // override
    assertEquals(1500.0, resolved.getDouble("height")); // default
}
```

### Integration Tests

**Test scenarios**:
- Editor changes parameter → Preview updates → Generate uses same value
- Constraint violation → Editor shows error → Generate blocked
- Schema migration → Old instances load with updated defaults

---

## Current Status

| Component | Status | Notes |
|-----------|--------|-------|
| Parameter types (8) | ✅ Complete | All 8 types implemented |
| ParametricSchema | ✅ Complete | Merge/extract works |
| ParameterSet | ✅ Complete | Immutable, type-safe |
| Metadata | ✅ Complete | All fields defined |
| Constraint expressions | ✅ Basic | Simple math/logic works, functions future |
| **Unified resolution** | ❌ **Gap** | Not used consistently (Week 2 priority) |
| Schema versioning | ⏳ Framework | Migration interface exists, no migrations yet |
| Expression dependencies | ⏸️ Future | Planned for NodeCraft phase |

---

## Acceptance Criteria

**For Kernel V1**:
- [x] All 8 parameter types implemented
- [x] ParametricSchema merges defaults correctly
- [x] Constraint evaluator handles math/logic expressions
- [ ] **Unified resolution path** (Editor, Preview, Generate use same resolver)
- [ ] Unit tests cover all parameter types and edge cases
- [ ] Integration test: change parameter in Editor → same value in Generate

**For Platform V1** (Phase B):
- [ ] Schema migration framework has at least 1 example migration
- [ ] Parameter changes trigger incremental pipeline invalidation

**For NodeCraft** (Phase F):
- [ ] Parameters can be expressions (computed from other parameters)
- [ ] Dependency graph evaluation
- [ ] Incremental re-evaluation

---

## Related Documents

- [kernel/03-component-system.md](03-component-system.md) — Components reference parameters
- [kernel/04-constraint-solver.md](04-constraint-solver.md) — Constraint evaluation detail
- [platform/01-opening-pipeline.md](../platform/01-opening-pipeline.md) — Parameters flow into generation
- [02-domain-model.md](../02-domain-model.md) — OpeningTypeDefinition owns ParametricSchema

---

## Future Extensions

### Phase C (Editor)
- Parameter grouping for UI (`"Dimensions"`, `"Materials"`, `"Hardware"`)
- Display order hints
- Advanced/basic mode filtering

### Phase F (NodeCraft)
- Parameters as expressions (`panel_width = width / panel_count`)
- Dependency graph evaluation
- Conditional parameters (`if operable then show handle_type`)

### Phase 12+ (Advanced)
- External parameter sources (link to spreadsheet, database)
- Parameter animation curves (for motion simulation)
- Unit conversion (mm ↔ inches, degrees ↔ radians)

---

**Document Status**: ✅ Complete  
**Last Updated**: 2026-07-16  
**Implementation**: ~80% (unified resolution pending)  
**Next Review**: After Kernel V1 completion
