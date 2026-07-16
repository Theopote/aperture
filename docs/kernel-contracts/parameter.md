# Parameter Module Contract

**Version**: 1.0  
**Last Updated**: 2026-07-16  
**Status**: Active  
**Owner**: Aperture Core Team

---

## Overview

Parameter模块提供**参数定义、解析、验证和约束求解**系统。它处理用户输入的参数值，解析约束表达式，验证参数合法性。

**核心原则**: Parameter是数据层，不包含几何计算或业务逻辑。

---

## Responsibilities

### ✅ 允许做的事

1. **参数类型定义**
   - Length (长度，单位mm)
   - Angle (角度，单位度)
   - Count (整数计数)
   - Number (浮点数比例)
   - Bool (布尔值)
   - MaterialRef (材质引用)
   - ProfileRef (轮廓引用)
   - EnumValue (枚举值)

2. **参数集合管理**
   - ParameterSet创建
   - 参数读取/写入
   - 参数合并
   - 默认值处理

3. **约束表达式**
   - 解析约束表达式
   - 验证参数值
   - 依赖关系追踪
   - 错误消息生成

4. **参数解析**
   - 从Definition解析参数
   - 应用默认值
   - 应用用户覆盖
   - 计算派生参数

5. **类型安全**
   - 类型检查
   - 单位转换
   - 范围验证

---

## Forbidden

### ❌ 禁止做的事

1. **❌ 不能包含几何计算**
   ```java
   // 错误
   public double calculateFrameWidth(ParameterSet params) {
       return params.get("width").asLength() - 2 * frameThickness;  // 这是几何计算
   }
   
   // 正确
   public ParameterValue getWidth(ParameterSet params) {
       return params.get("width");  // 只返回参数值
   }
   ```

2. **❌ 不能依赖Minecraft**
   ```java
   // 错误
   import net.minecraft.nbt.CompoundTag;
   
   // 正确
   // Parameter模块使用自己的序列化
   ```

3. **❌ 不能包含Opening业务逻辑**
   ```java
   // 错误
   public boolean isDoorValid(ParameterSet params) {
       // 检查door特定规则
   }
   
   // 正确
   public ValidationResult validate(List<Constraint> constraints, ParameterSet params) {
       // 通用约束验证
   }
   ```

4. **❌ 不能包含UI逻辑**
   ```java
   // 错误
   public Widget createEditor(ParameterDef def) { ... }
   
   // 正确
   public ParameterMetadata describe(ParameterDef def) { ... }  // 只提供元数据
   ```

5. **❌ 不能直接生成几何体**
   ```java
   // 错误
   public Shape generateFromParameters(ParameterSet params) { ... }
   
   // 正确
   // 参数只是数据，由其他模块消费
   ```

---

## Allowed Dependencies

### ✅ 可以依赖的模块

1. **aperture-math** (数学工具)
   - 用于表达式计算
   - 单位转换

2. **Java标准库**
   - java.util.*
   - java.lang.*
   - java.math.* (BigDecimal等)

3. **表达式解析库** (可选)
   - ANTLR (语法解析)
   - JEP (表达式求值)

**依赖原则**: 只依赖数学和解析工具，不依赖业务模块

---

## Forbidden Dependencies

### ❌ 禁止依赖的模块

1. **❌ aperture-geometry** (几何计算)
   - 理由: Parameter是Geometry的输入，不能反向依赖

2. **❌ aperture-opening** (Opening业务)
   - 理由: Parameter是通用系统，不绑定Opening

3. **❌ net.minecraft.*** (Minecraft)
   - 理由: 平台独立性

4. **❌ aperture-client** (UI)
   - 理由: Parameter是数据层，不关心显示

---

## Input Types

### 📥 接受的输入

1. **参数定义**
   ```java
   public record ParameterDefinition(
       String key,
       ParameterType type,
       ParameterValue defaultValue,
       Optional<String> description,
       Optional<Range> range
   ) {}
   ```

2. **约束定义**
   ```java
   public record ConstraintDefinition(
       String expression,      // "width > 100"
       String errorMessage     // "Width must exceed 100mm"
   ) {}
   ```

3. **用户输入**
   ```java
   Map<String, ParameterValue> userOverrides = Map.of(
       "width", ParameterValue.length(1200.0),
       "height", ParameterValue.length(1500.0)
   );
   ```

4. **表达式上下文**
   ```java
   public record ExpressionContext(
       Map<String, Double> variables,
       Map<String, Function<Double[], Double>> functions
   ) {}
   ```

### 输入验证

```java
public ParameterValue parse(String key, String value, ParameterType type) {
    Objects.requireNonNull(key, "key cannot be null");
    Objects.requireNonNull(value, "value cannot be null");
    Objects.requireNonNull(type, "type cannot be null");
    
    return switch (type) {
        case LENGTH -> parseLength(value);
        case ANGLE -> parseAngle(value);
        // ...
    };
}

private ParameterValue parseLength(String value) {
    try {
        double num = Double.parseDouble(value);
        if (num < 0) {
            throw new IllegalArgumentException("Length cannot be negative");
        }
        return ParameterValue.length(num);
    } catch (NumberFormatException e) {
        throw new IllegalArgumentException("Invalid length: " + value, e);
    }
}
```

---

## Output Types

### 📤 产生的输出

1. **参数值**
   ```java
   public sealed interface ParameterValue {
       record Length(double mm) implements ParameterValue {}
       record Angle(double degrees) implements ParameterValue {}
       record Count(int value) implements ParameterValue {}
       record Number(double value) implements ParameterValue {}
       record Bool(boolean value) implements ParameterValue {}
       record MaterialRef(String id) implements ParameterValue {}
       record ProfileRef(String id) implements ParameterValue {}
       record EnumValue(String value) implements ParameterValue {}
       
       ParameterType type();
   }
   ```

2. **参数集合**
   ```java
   public final class ParameterSet {
       private final Map<String, ParameterValue> values;
       
       public Optional<ParameterValue> get(String key);
       public ParameterSet with(String key, ParameterValue value);
       public Set<String> keys();
       public int size();
   }
   ```

3. **验证结果**
   ```java
   public sealed interface ValidationResult {
       record Valid() implements ValidationResult {}
       record Invalid(List<ValidationIssue> issues) implements ValidationResult {}
       
       boolean isValid();
   }
   
   public record ValidationIssue(
       String parameterKey,
       String message,
       Severity severity
   ) {}
   ```

4. **约束求解结果**
   ```java
   public record ConstraintResult(
       boolean satisfied,
       String constraint,
       Optional<String> failureReason
   ) {}
   ```

### 输出不变式

**保证**:
- ParameterSet不可变（所有修改返回新实例）
- ParameterValue类型安全（不会返回错误类型）
- ValidationResult完整（包含所有错误）
- 约束求值确定性（相同输入→相同输出）

---

## Lifecycle

### 对象创建

**不可变对象**: Parameter相关对象都是不可变的
```java
// ParameterSet是不可变的
ParameterSet params1 = ParameterSet.empty();
ParameterSet params2 = params1.with("width", ParameterValue.length(1000));
// params1保持不变

// ParameterValue也是不可变的
ParameterValue.Length length = new ParameterValue.Length(1000);
// 没有setter方法
```

**Builder模式**: 构建复杂ParameterSet
```java
ParameterSet params = ParameterSet.builder()
    .put("width", ParameterValue.length(1200))
    .put("height", ParameterValue.length(1500))
    .put("count", ParameterValue.count(2))
    .build();
```

### 状态管理

**无状态**: 参数解析和验证都是无状态操作
```java
// 纯函数式API
public class ParameterResolver {
    public static ParameterSet resolve(
        OpeningTypeDefinition definition,
        ParameterSet overrides
    ) {
        // 不修改输入
        // 返回新的ParameterSet
        return mergeWithDefaults(definition.parameters(), overrides);
    }
}
```

**缓存**: 约束求值可以缓存
```java
public class CachedConstraintEvaluator {
    private final Map<String, CompiledExpression> cache = new HashMap<>();
    
    public boolean evaluate(String expression, ExpressionContext context) {
        CompiledExpression compiled = cache.computeIfAbsent(
            expression, 
            expr -> compiler.compile(expr)
        );
        return compiled.evaluate(context);
    }
}
```

---

## Error Handling

### 异常类型

1. **ParameterException** - 参数系统通用异常
   ```java
   public class ParameterException extends RuntimeException {
       public ParameterException(String message) { super(message); }
       public ParameterException(String message, Throwable cause) { super(message, cause); }
   }
   ```

2. **ParameterParseException** - 解析失败
   ```java
   public class ParameterParseException extends ParameterException {
       private final String input;
       private final ParameterType expectedType;
       
       public ParameterParseException(String input, ParameterType type, Throwable cause) {
           super("Failed to parse '" + input + "' as " + type, cause);
           this.input = input;
           this.expectedType = type;
       }
   }
   ```

3. **ConstraintViolationException** - 约束违反
   ```java
   public class ConstraintViolationException extends ParameterException {
       private final List<ConstraintResult> violations;
       
       public ConstraintViolationException(List<ConstraintResult> violations) {
           super(formatViolations(violations));
           this.violations = violations;
       }
   }
   ```

### 错误处理策略

**验证不抛异常**: 返回ValidationResult
```java
// 好: 返回结果对象
public ValidationResult validate(ParameterSet params, List<Constraint> constraints) {
    List<ValidationIssue> issues = new ArrayList<>();
    
    for (Constraint constraint : constraints) {
        if (!constraint.isSatisfied(params)) {
            issues.add(new ValidationIssue(
                constraint.expression(),
                constraint.errorMessage(),
                Severity.ERROR
            ));
        }
    }
    
    return issues.isEmpty() 
        ? new ValidationResult.Valid()
        : new ValidationResult.Invalid(issues);
}

// 调用者决定如何处理
ValidationResult result = validate(params, constraints);
if (!result.isValid()) {
    // 显示错误给用户，或抛出异常
}
```

**解析抛异常**: 无效输入立即失败
```java
// 解析时快速失败
public ParameterValue parseLength(String input) {
    try {
        double value = Double.parseDouble(input);
        if (value < 0) {
            throw new ParameterParseException(
                input, 
                ParameterType.LENGTH,
                new IllegalArgumentException("Length cannot be negative")
            );
        }
        return ParameterValue.length(value);
    } catch (NumberFormatException e) {
        throw new ParameterParseException(input, ParameterType.LENGTH, e);
    }
}
```

---

## Performance Requirements

### 时间复杂度

| 操作 | 复杂度 | 说明 |
|------|--------|------|
| get参数 | O(1) | HashMap查找 |
| set参数 | O(n) | 创建新ParameterSet |
| 解析约束 | O(m) | m = 表达式长度 |
| 求值约束 | O(k) | k = 变量数 |
| 验证全部约束 | O(n·k) | n = 约束数 |

### 性能目标

- **单个约束求值**: < 1ms
- **完整参数验证**: < 10ms
- **表达式编译**: < 5ms (首次)
- **缓存命中**: < 0.1ms

### 优化策略

```java
// 1. 约束预编译
public class ConstraintCompiler {
    public CompiledConstraint compile(String expression) {
        // 编译一次，多次执行
        AST ast = parser.parse(expression);
        return ast.compile();
    }
}

// 2. 表达式缓存
private final Map<String, CompiledConstraint> constraintCache = new HashMap<>();

public boolean evaluate(String expr, ExpressionContext ctx) {
    CompiledConstraint compiled = constraintCache.computeIfAbsent(
        expr, 
        this::compile
    );
    return compiled.evaluate(ctx);
}

// 3. 延迟验证
public class LazyValidation {
    private ValidationResult cachedResult;
    
    public ValidationResult validate() {
        if (cachedResult == null) {
            cachedResult = performValidation();
        }
        return cachedResult;
    }
}
```

---

## Constraint Expression Language

### 语法

```
expression := comparison | logical

comparison := term op term
    where op := ">" | "<" | ">=" | "<=" | "==" | "!="

logical := expression ("&&" | "||") expression
         | "!" expression
         | "(" expression ")"

term := number | parameter | function

function := "min" "(" term "," term ")"
          | "max" "(" term "," term ")"
          | "abs" "(" term ")"
          | "sqrt" "(" term ")"

parameter := identifier

identifier := [a-zA-Z_][a-zA-Z0-9_]*
```

### 示例

```java
// 简单比较
"width > 100"
"height < 3000"
"count >= 1"

// 逻辑组合
"width > 100 && width < 3000"
"panel_count == 1 || panel_count == 2"

// 关系约束
"width / height > 0.5"
"width >= height * 0.8"

// 函数调用
"min(width, height) > 100"
"glass_ratio >= 0.0 && glass_ratio <= 1.0"

// 复杂约束
"(panel_count == 2) => (width >= 1600)"  // 蕴含
```

### 变量命名规则

- 小写字母开头
- 下划线分隔单词
- 不能使用保留字

---

## Examples

### ✅ 正确用法

```java
// 示例1: 创建ParameterSet
ParameterSet params = ParameterSet.builder()
    .put("width", ParameterValue.length(1200.0))
    .put("height", ParameterValue.length(1500.0))
    .put("panel_count", ParameterValue.count(1))
    .put("glass_ratio", ParameterValue.number(0.3))
    .build();

// 示例2: 读取参数
double width = params.get("width").asLength();
int panelCount = params.get("panel_count").asCount();

// 示例3: 修改参数（返回新实例）
ParameterSet updated = params.with("width", ParameterValue.length(1400.0));

// 示例4: 验证约束
List<Constraint> constraints = List.of(
    new Constraint("width > 100", "Width must exceed 100mm"),
    new Constraint("width / height > 0.5", "Width/height ratio must exceed 0.5")
);

ValidationResult result = ConstraintValidator.validate(params, constraints);
if (!result.isValid()) {
    result.issues().forEach(issue -> 
        System.err.println(issue.message())
    );
}

// 示例5: 解析参数定义
ParameterSet resolved = ParameterResolver.resolve(
    definition,         // OpeningTypeDefinition
    userOverrides       // 用户提供的覆盖值
);
```

### ❌ 错误用法

```java
// 错误1: 尝试修改不可变对象
ParameterSet params = ParameterSet.empty();
params.values.put("width", ...);  // ❌ 编译错误，values是private final

// 错误2: 假设参数存在
double width = params.get("width").asLength();  // ❌ 如果width不存在会抛NPE

// 正确: 使用Optional
params.get("width")
    .map(ParameterValue::asLength)
    .orElse(DEFAULT_WIDTH);

// 错误3: 在Parameter模块中做几何计算
public double calculateArea(ParameterSet params) {  // ❌ 不属于Parameter职责
    double w = params.get("width").asLength();
    double h = params.get("height").asLength();
    return w * h;  // 这是几何计算
}

// 错误4: 类型不安全的访问
ParameterValue value = params.get("width");
int count = (int) value;  // ❌ 运行时类型错误

// 正确: 使用类型安全的访问器
if (value instanceof ParameterValue.Length length) {
    double mm = length.mm();
}
```

---

## Migration Guide

### 违规代码迁移

**场景1: Parameter模块中包含几何计算**

**现状**:
```java
// aperture-parameter/.../ParameterUtils.java
public class ParameterUtils {
    public static double calculateFrameWidth(ParameterSet params) {
        return params.get("width").asLength() - 2 * FRAME_THICKNESS;
    }
}
```

**迁移**:
```java
// 移到 aperture-geometry 或 aperture-opening
public class FrameCalculator {
    public static double calculateFrameWidth(double outerWidth, double thickness) {
        return outerWidth - 2 * thickness;
    }
}

// Parameter模块只提供参数访问
public class ParameterAccessor {
    public static double getWidth(ParameterSet params) {
        return params.get("width").asLength();
    }
}
```

---

## Version History

| Version | Date | Changes |
|---------|------|---------|
| 1.0 | 2026-07-16 | 初始版本 |

---

**Status**: ✅ Active  
**Enforcement**: Manual review + CI checks (planned)
