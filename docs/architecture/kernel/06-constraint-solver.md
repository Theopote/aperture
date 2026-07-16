# 06 — Constraint Solver

**Layer**: Kernel  
**Status**: 🎯 CRITICAL — Parameter Validation & Dependencies  
**Dependencies**: Parameter Engine

---

## Overview

**Constraints define what is valid.**

The Constraint Solver is Aperture's rule engine that:
- Validates parameter values against rules
- Enforces relationships between parameters
- Propagates parameter changes
- (Future) Solves equations to find valid parameter sets

**Design Principle**: Constraints are **declarative rules**, not imperative checks. A constraint says "width must exceed 30% of height", not "if width < height * 0.3 then throw error".

**This is NOT validation code scattered everywhere. This IS a unified constraint engine.**

---

## Why Constraint Solver?

### Without Constraints

```java
// Validation scattered in generator
if (width < 300) throw new Error("Width too small");
if (width > 6000) throw new Error("Width too large");
if (width < height * 0.3) throw new Error("Width/height ratio invalid");
if (mullions > 0 && mullionWidth >= width / (mullions + 1)) 
    throw new Error("Mullions too wide");
```

**Problems**:
- Rules duplicated across generator, editor, placement
- Hard to debug (which rule failed?)
- Can't analyze constraints (e.g., "show me valid ranges for width")
- Can't auto-fix (solver can't find valid values)

### With Constraint Solver

```json
{
  "parameters": {
    "width": { "type": "length", "min": 300, "max": 6000 },
    "height": { "type": "length", "min": 300, "max": 4000 },
    "mullions": { "type": "count", "min": 0, "max": 10 },
    "mullion_width": { "type": "length", "min": 30, "max": 100 }
  },
  "constraints": [
    {
      "expr": "width > height * 0.3",
      "message": "Width must exceed 30% of height"
    },
    {
      "expr": "mullions == 0 || mullion_width < width / (mullions + 1)",
      "message": "Mullions too wide for opening width"
    }
  ]
}
```

**Benefits**:
- ✅ Rules in one place (single source of truth)
- ✅ Validated everywhere (generator, editor, placement)
- ✅ Debuggable (know which constraint failed)
- ✅ Analyzable (compute valid parameter ranges)
- ✅ (Future) Solvable (find valid parameter combinations)

---

## Constraint Types

### 1. Range Constraints

**Built into parameter metadata**:

```java
RangeParameter.length(1200, 300, 6000)  // min, max implicit
```

**Validation**: `value >= min && value <= max`

**Examples**:
- Width: 300mm–6000mm
- Height: 300mm–4000mm
- Mullion count: 0–10

---

### 2. Expression Constraints

**Custom rules as mathematical expressions**:

```json
{
  "expr": "width > height * 0.3",
  "message": "Width must exceed 30% of height"
}
```

**Validation**: Evaluate expression, must return `true`

**Supported Operators**:
- Arithmetic: `+`, `-`, `*`, `/`, `%`, `^` (power)
- Comparison: `<`, `<=`, `>`, `>=`, `==`, `!=`
- Logical: `&&`, `||`, `!`
- Parentheses: `()`

**Examples**:
```json
[
  { "expr": "width / height <= 3.0", "message": "Aspect ratio max 3:1" },
  { "expr": "frame_width >= 20 && frame_width <= 150", "message": "Frame width 20-150mm" },
  { "expr": "mullions == 0 || width / (mullions + 1) >= 400", "message": "Each bay min 400mm" }
]
```

---

### 3. Dependency Constraints (Future)

**Parameters that are computed from others**:

```json
{
  "parameters": {
    "width": { "type": "length", "default": 1800 },
    "panel_count": { "type": "count", "default": 2 },
    "panel_width": { 
      "type": "length", 
      "expr": "width / panel_count"  // Computed parameter
    }
  }
}
```

**Validation**: Automatically satisfied (computed)

**Status**: Planned for NodeCraft phase

---

### 4. Conditional Constraints (Future)

**Rules that only apply in certain contexts**:

```json
{
  "expr": "track_type == 'pocket' && host_depth >= 150",
  "message": "Pocket doors require 150mm wall depth",
  "when": "track_type == 'pocket'"
}
```

**Validation**: Only evaluate if `when` is true

**Status**: Planned

---

## Constraint Evaluation

### Expression Parser

**Grammar** (simplified):

```
expr       := logical
logical    := comparison (('&&' | '||') comparison)*
comparison := additive (('<' | '<=' | '>' | '>=' | '==' | '!=') additive)?
additive   := multiplicative (('+' | '-') multiplicative)*
multiplicative := power (('*' | '/' | '%') power)*
power      := unary ('^' unary)?
unary      := ('!' | '-')? primary
primary    := NUMBER | IDENTIFIER | '(' expr ')'
```

**Implementation**:

```java
public class ExpressionParser {
    private final List<Token> tokens;
    private int current = 0;
    
    public Expr parse(String source) {
        var lexer = new ExpressionLexer(source);
        this.tokens = lexer.tokenize();
        return parseExpression();
    }
    
    private Expr parseExpression() {
        return parseLogical();
    }
    
    private Expr parseLogical() {
        var left = parseComparison();
        
        while (match(TokenType.AND, TokenType.OR)) {
            var op = previous();
            var right = parseComparison();
            left = new BinaryExpr(left, op, right);
        }
        
        return left;
    }
    
    // ... (full recursive descent parser)
}
```

**AST**:

```java
public sealed interface Expr permits 
    BinaryExpr, UnaryExpr, LiteralExpr, VariableExpr, GroupExpr {}

public record BinaryExpr(Expr left, Token operator, Expr right) implements Expr {}
public record UnaryExpr(Token operator, Expr operand) implements Expr {}
public record LiteralExpr(Object value) implements Expr {}
public record VariableExpr(String name) implements Expr {}
public record GroupExpr(Expr inner) implements Expr {}
```

---

### Constraint Evaluator

**Evaluates AST against parameter context**:

```java
public class ConstraintEvaluator {
    private final List<ConstraintRule> constraints;
    
    public boolean evaluate(String exprString, ConstraintContext context) {
        var parser = new ExpressionParser();
        var expr = parser.parse(exprString);
        return evaluateExpr(expr, context);
    }
    
    private boolean evaluateExpr(Expr expr, ConstraintContext context) {
        return switch (expr) {
            case BinaryExpr binary -> evaluateBinary(binary, context);
            case UnaryExpr unary -> evaluateUnary(unary, context);
            case LiteralExpr literal -> (boolean) literal.value();
            case VariableExpr variable -> context.get(variable.name());
            case GroupExpr group -> evaluateExpr(group.inner(), context);
        };
    }
    
    private boolean evaluateBinary(BinaryExpr expr, ConstraintContext context) {
        var left = evaluateNumeric(expr.left(), context);
        var right = evaluateNumeric(expr.right(), context);
        
        return switch (expr.operator().type()) {
            case LESS -> left < right;
            case LESS_EQUAL -> left <= right;
            case GREATER -> left > right;
            case GREATER_EQUAL -> left >= right;
            case EQUAL_EQUAL -> Math.abs(left - right) < 1e-6;
            case BANG_EQUAL -> Math.abs(left - right) >= 1e-6;
            case PLUS -> left + right;
            case MINUS -> left - right;
            case STAR -> left * right;
            case SLASH -> left / right;
            // ...
        };
    }
}
```

---

### Constraint Context

**Provides parameter values to evaluator**:

```java
public class ConstraintContext {
    private final Map<String, Object> values;
    
    public static ConstraintContext from(ParameterSet parameters) {
        return new ConstraintContext(parameters.values());
    }
    
    public double getDouble(String name) {
        var value = values.get(name);
        if (value instanceof Number num) {
            return num.doubleValue();
        }
        throw new ConstraintEvaluationException("Not a number: " + name);
    }
    
    public boolean getBoolean(String name) {
        var value = values.get(name);
        if (value instanceof Boolean bool) {
            return bool;
        }
        throw new ConstraintEvaluationException("Not a boolean: " + name);
    }
    
    public String getString(String name) {
        var value = values.get(name);
        return value != null ? value.toString() : null;
    }
}
```

---

## Validation Workflow

### Parametric Validator

**Validates parameters against all constraints**:

```java
public class ParametricValidator {
    private final OpeningTypeDefinition definition;
    private final ConstraintEvaluator evaluator;
    
    public ParametricValidator(OpeningTypeDefinition definition) {
        this.definition = definition;
        this.evaluator = new ConstraintEvaluator(definition.constraints());
    }
    
    public ValidationResult validate(ParameterSet parameters) {
        var issues = new ArrayList<ValidationIssue>();
        var context = ConstraintContext.from(parameters);
        
        // 1. Validate range constraints (from parameter metadata)
        for (var entry : definition.getParametricSchema().parameters().entrySet()) {
            var name = entry.getKey();
            var param = entry.getValue();
            var value = parameters.get(name);
            
            var rangeIssues = validateRange(name, value, param.metadata());
            issues.addAll(rangeIssues);
        }
        
        // 2. Validate expression constraints
        for (var constraint : definition.constraints()) {
            try {
                var result = evaluator.evaluate(constraint.expr(), context);
                if (!result) {
                    issues.add(ValidationIssue.error(
                        null,  // No specific parameter
                        constraint.message(),
                        constraint.expr()
                    ));
                }
            } catch (ConstraintEvaluationException e) {
                issues.add(ValidationIssue.error(
                    null,
                    "Constraint evaluation failed: " + e.getMessage(),
                    constraint.expr()
                ));
            }
        }
        
        return new ValidationResult(issues.isEmpty(), issues);
    }
    
    private List<ValidationIssue> validateRange(
        String name, 
        Object value, 
        ParameterMetadata metadata
    ) {
        if (metadata.minValue() == null && metadata.maxValue() == null) {
            return List.of();
        }
        
        if (!(value instanceof Number num)) {
            return List.of();  // Type error handled elsewhere
        }
        
        var issues = new ArrayList<ValidationIssue>();
        var doubleValue = num.doubleValue();
        
        if (metadata.minValue() != null) {
            var min = ((Number) metadata.minValue()).doubleValue();
            if (doubleValue < min) {
                issues.add(ValidationIssue.error(
                    name,
                    String.format("%s must be >= %s", name, min),
                    null
                ));
            }
        }
        
        if (metadata.maxValue() != null) {
            var max = ((Number) metadata.maxValue()).doubleValue();
            if (doubleValue > max) {
                issues.add(ValidationIssue.error(
                    name,
                    String.format("%s must be <= %s", name, max),
                    null
                ));
            }
        }
        
        return issues;
    }
}
```

---

## Incremental Validation

**Problem**: Re-validating all constraints on every parameter change is expensive.

**Solution**: Track which constraints depend on which parameters, only re-validate affected constraints.

### Constraint Dependencies

**Analyze constraint expressions to extract parameter dependencies**:

```java
public class ConstraintDependencyAnalyzer {
    public Set<String> analyzeParameterDeps(String expr) {
        var parser = new ExpressionParser();
        var ast = parser.parse(expr);
        return extractVariables(ast);
    }
    
    private Set<String> extractVariables(Expr expr) {
        return switch (expr) {
            case VariableExpr variable -> Set.of(variable.name());
            case BinaryExpr binary -> {
                var left = extractVariables(binary.left());
                var right = extractVariables(binary.right());
                var combined = new HashSet<>(left);
                combined.addAll(right);
                yield combined;
            }
            case UnaryExpr unary -> extractVariables(unary.operand());
            case GroupExpr group -> extractVariables(group.inner());
            case LiteralExpr literal -> Set.of();
        };
    }
}
```

**Example**:
```
Constraint: "width > height * 0.3"
Dependencies: { "width", "height" }

Constraint: "mullions == 0 || mullion_width < width / (mullions + 1)"
Dependencies: { "mullions", "mullion_width", "width" }
```

---

### Incremental Validator

```java
public class IncrementalValidator {
    private final ParametricValidator validator;
    private final Map<ConstraintRule, Set<String>> constraintDeps;
    
    public IncrementalValidator(OpeningTypeDefinition definition) {
        this.validator = new ParametricValidator(definition);
        this.constraintDeps = analyzeConstraints(definition.constraints());
    }
    
    public ValidationResult validateIncremental(
        ParameterSet oldParams,
        ParameterSet newParams
    ) {
        var changedKeys = computeChangedKeys(oldParams, newParams);
        
        if (changedKeys.isEmpty()) {
            // No changes, validation state unchanged
            return validator.validate(newParams);
        }
        
        // Find affected constraints
        var affectedConstraints = new ArrayList<ConstraintRule>();
        for (var entry : constraintDeps.entrySet()) {
            var constraint = entry.getKey();
            var deps = entry.getValue();
            if (!Collections.disjoint(deps, changedKeys)) {
                affectedConstraints.add(constraint);
            }
        }
        
        // Re-validate only affected constraints
        var issues = new ArrayList<ValidationIssue>();
        var context = ConstraintContext.from(newParams);
        
        for (var constraint : affectedConstraints) {
            var result = evaluator.evaluate(constraint.expr(), context);
            if (!result) {
                issues.add(ValidationIssue.error(null, constraint.message(), constraint.expr()));
            }
        }
        
        // Also re-validate range constraints for changed parameters
        for (var key : changedKeys) {
            var param = definition.getParametricSchema().parameters().get(key);
            var value = newParams.get(key);
            var rangeIssues = validateRange(key, value, param.metadata());
            issues.addAll(rangeIssues);
        }
        
        return new ValidationResult(issues.isEmpty(), issues);
    }
}
```

**Example**:
```
Change: width: 1200 → 1800

Affected constraints:
- "width > height * 0.3" (depends on width)
- "mullions == 0 || mullion_width < width / (mullions + 1)" (depends on width)

Unaffected constraints:
- "height > 300" (doesn't depend on width)

Revalidate: 2 constraints (not all)
```

---

## Constraint Solving (Future)

**Goal**: Given some parameters, compute values for others that satisfy all constraints.

**Example**:
```
Given: height = 1500
Find: width such that "width > height * 0.3" and "width >= 300" and "width <= 6000"

Solution: width ∈ [450, 6000]
```

### Solver Interface

```java
public interface ConstraintSolver {
    /**
     * Solves for unknown parameters given some known values.
     * 
     * @param schema Parameter schema with constraints
     * @param known Known parameter values
     * @param unknown Parameters to solve for
     * @return Solution set (may be a range)
     */
    SolutionSet solve(
        ParametricSchema schema,
        ParameterSet known,
        Set<String> unknown
    );
}
```

### Solution Set

```java
public sealed interface SolutionSet permits 
    PointSolution, RangeSolution, MultipleSolutions, NoSolution {}

public record PointSolution(ParameterSet values) implements SolutionSet {}

public record RangeSolution(
    String parameter,
    double min,
    double max
) implements SolutionSet {}

public record MultipleSolutions(List<ParameterSet> solutions) implements SolutionSet {}

public record NoSolution(String reason) implements SolutionSet {}
```

---

### Solving Strategies

**1. Interval Arithmetic** (for linear constraints):

```
Constraint: width > height * 0.3
Given: height = 1500
Transform: width > 1500 * 0.3 = 450
Solution: width ∈ [450, ∞)

Intersect with range constraint:
width ∈ [300, 6000]
Result: width ∈ [450, 6000]
```

**2. Symbolic Manipulation** (for equations):

```
Constraint: panel_width = width / panel_count
Given: width = 1800, panel_count = 2
Solve: panel_width = 1800 / 2 = 900
```

**3. Numerical Optimization** (for complex constraints):

Use gradient descent or simulated annealing when constraints are non-linear.

**Status**: All solving strategies are future work (Phase 12: NodeCraft)

---

## Error Handling

### Constraint Evaluation Errors

| Error | Cause | Action |
|-------|-------|--------|
| `DivisionByZeroException` | `width / mullions` when `mullions = 0` | Treat as constraint failure (validation error) |
| `UnknownVariableException` | Reference to non-existent parameter | Abort validation, log error |
| `TypeMismatchException` | `width && height` (logical on numbers) | Abort validation, log error |
| `ParseException` | Invalid expression syntax | Abort at definition load time |

### User-Facing Errors

**Good error messages**:

```
❌ Bad: "Constraint failed"
✅ Good: "Width must exceed 30% of height (current: 400mm < 450mm)"

❌ Bad: "Invalid parameters"
✅ Good: "Each bay must be at least 400mm wide (current: 300mm with 4 mullions)"
```

**Implementation**:

```java
public record ValidationIssue(
    ValidationSeverity severity,
    String parameterKey,
    String message,          // Human-readable
    String constraint        // For debugging
) {
    public String format(ParameterSet parameters) {
        if (constraint != null && parameterKey != null) {
            var value = parameters.get(parameterKey);
            return String.format("%s (current value: %s, constraint: %s)", 
                message, value, constraint);
        }
        return message;
    }
}
```

---

## Integration with Editor

### Real-Time Validation

**As user types in Inspector**:

```java
public class ParameterInspector {
    private final IncrementalValidator validator;
    
    public void onParameterChanged(String key, Object newValue) {
        var newParams = currentParams.with(key, newValue);
        var validation = validator.validateIncremental(currentParams, newParams);
        
        // Update UI
        if (validation.valid()) {
            clearErrors();
            enableGenerateButton();
        } else {
            for (var issue : validation.getErrors()) {
                showError(issue.parameterKey(), issue.message());
            }
            disableGenerateButton();
        }
        
        currentParams = newParams;
    }
}
```

### Constraint Feedback

**Show valid ranges in UI**:

```
Width: [______1800______]
       300            6000
       
Error: Width must exceed 450mm (30% of height 1500mm)
       ^^^ Show minimum valid value
```

---

## Current Status

| Component | Status | Notes |
|-----------|--------|-------|
| Range validation | ✅ Complete | Built into ParameterMetadata |
| Expression parser | ✅ Complete | Supports math, logic, comparison |
| Expression evaluator | ✅ Complete | AST-based evaluation |
| ParametricValidator | ✅ Complete | Validates all constraints |
| Dependency analysis | ❌ Missing | Need for incremental validation |
| Incremental validation | ❌ Missing | Currently re-validates all |
| Constraint solving | ❌ Missing | Future (NodeCraft phase) |
| Error messages | ⏳ Partial | Basic messages exist, need improvement |

---

## Acceptance Criteria

### For Kernel V1 (Week 2)
- [x] Expression parser supports math, logic, comparison
- [x] ConstraintEvaluator validates expressions
- [x] ParametricValidator checks all constraints
- [ ] Constraint dependency analysis
- [ ] Incremental validation (only re-check affected constraints)
- [ ] Unit tests for parser, evaluator, validator

### For Platform V1 (Phase B)
- [ ] Editor uses incremental validation
- [ ] Error messages show current value + constraint
- [ ] Valid range hints in UI (future)

### For NodeCraft (Phase F)
- [ ] Constraint solver (find valid parameter values)
- [ ] Solver supports interval arithmetic
- [ ] Solver supports symbolic manipulation

---

## Testing Strategy

### Parser Tests

```java
@Test
void parser_arithmetic_parsesCorrectly() {
    var parser = new ExpressionParser();
    var expr = parser.parse("width + height * 2");
    
    assertTrue(expr instanceof BinaryExpr);
    // Assert AST structure
}

@Test
void parser_comparison_parsesCorrectly() {
    var expr = parser.parse("width > height * 0.3");
    
    assertTrue(expr instanceof BinaryExpr);
    assertEquals(TokenType.GREATER, ((BinaryExpr) expr).operator().type());
}
```

### Evaluator Tests

```java
@Test
void evaluator_simpleComparison_evaluatesCorrectly() {
    var evaluator = new ConstraintEvaluator(List.of());
    var context = ConstraintContext.from(ParameterSet.builder()
        .put("width", 1200.0)
        .put("height", 1500.0)
        .build());
    
    assertTrue(evaluator.evaluate("width < height", context));
    assertFalse(evaluator.evaluate("width > height", context));
}

@Test
void evaluator_complexExpression_evaluatesCorrectly() {
    var context = ConstraintContext.from(ParameterSet.builder()
        .put("width", 1800.0)
        .put("height", 1500.0)
        .build());
    
    assertTrue(evaluator.evaluate("width > height * 0.3", context));  // 1800 > 450
    assertTrue(evaluator.evaluate("width / height <= 3.0", context)); // 1.2 <= 3.0
}
```

### Validator Tests

```java
@Test
void validator_validParameters_passesValidation() {
    var definition = loadDefinition("aperture:fixed_window");
    var validator = new ParametricValidator(definition);
    var params = ParameterSet.builder()
        .put("width", 1800.0)
        .put("height", 1500.0)
        .put("mullions", 2)
        .build();
    
    var result = validator.validate(params);
    
    assertTrue(result.valid());
}

@Test
void validator_invalidRatio_failsValidation() {
    var params = ParameterSet.builder()
        .put("width", 400.0)   // Too narrow for height
        .put("height", 1500.0)
        .build();
    
    var result = validator.validate(params);
    
    assertFalse(result.valid());
    assertThat(result.getErrors())
        .anyMatch(err -> err.getMessage().contains("30% of height"));
}
```

---

## Related Documents

- [kernel/02-parameter-engine.md](02-parameter-engine.md) — Parameters being validated
- [kernel/04-generation-pipeline.md](04-generation-pipeline.md) — Constraint Stage in pipeline
- [kernel/05-component-graph.md](05-component-graph.md) — Component constraints (future)

---

**Document Status**: ✅ Complete  
**Last Updated**: 2026-07-16  
**Implementation**: ~70% (parser/evaluator done, incremental validation missing)  
**Next Review**: After Kernel V1
