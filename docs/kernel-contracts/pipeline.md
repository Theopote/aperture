# Pipeline Module Contract

**Version**: 1.0  
**Last Updated**: 2026-07-16  
**Status**: Active  
**Owner**: Aperture Core Team

---

## Overview

Pipeline模块是Aperture的**核心数据流引擎**，实现8阶段生成管线（Definition → Parameter → Constraint → Component → Geometry → Mesh → Collision → Placement → Render）。Pipeline协调所有模块，提供统一的执行、缓存和错误处理机制。

**核心原则**: Pipeline是Kernel的中央调度器，所有几何生成必须经过Pipeline，禁止绕过Pipeline直接调用底层模块。

---

## Responsibilities

### ✅ 允许做的事

1. **Pipeline定义**
   - 8阶段接口定义
   - Stage注册和查找
   - Pipeline构建和配置

2. **Pipeline执行**
   - 顺序执行各阶段
   - 阶段间数据传递
   - 短路执行（提前失败）

3. **结果缓存**
   - 阶段级缓存
   - 完整Pipeline结果缓存
   - 缓存失效策略

4. **错误处理**
   - 阶段错误捕获
   - 错误上下文保存
   - 回退和重试

5. **可观测性**
   - 执行时间追踪
   - 阶段性能分析
   - 缓存命中率统计

---

## Forbidden

### ❌ 禁止做的事

1. **❌ 不能包含具体几何计算**
   ```java
   // 错误
   public class Pipeline {
       public PipelineResult execute(Definition def) {
           Shape shape = Geometry.extrude(...);  // ❌ Pipeline不做几何计算
           return new PipelineResult(shape);
       }
   }
   
   // 正确
   public class Pipeline {
       public PipelineResult execute(Definition def) {
           // 调用GeometryStage
           StageResult<Shape> result = geometryStage.execute(input);
           return result;
       }
   }
   ```

2. **❌ 不能绕过Stage直接调用底层模块**
   ```java
   // 错误: Opening直接调用Geometry
   public class Opening {
       public Shape generate() {
           return Geometry.extrude(profile, height);  // ❌ 绕过Pipeline
       }
   }
   
   // 正确: 通过Pipeline
   public class Opening {
       public PipelineResult generate() {
           return pipeline.execute(definition);  // ✅ 通过Pipeline
       }
   }
   ```

3. **❌ 不能依赖Minecraft**
   ```java
   // 错误
   import net.minecraft.world.World;
   
   public interface PipelineStage {
       StageResult execute(World world);  // ❌ 不应该依赖Minecraft
   }
   
   // 正确
   public interface PipelineStage<I, O> {
       StageResult<O> execute(I input, StageContext ctx);  // 平台无关
   }
   ```

4. **❌ 不能包含UI逻辑**
   ```java
   // 错误
   public class Pipeline {
       public void showProgress(ProgressBar bar) { ... }  // ❌ UI不属于Pipeline
   }
   
   // 正确
   public class Pipeline {
       public Observable<StageProgress> observeProgress() { ... }  // 提供事件流
   }
   ```

5. **❌ 不能硬编码Opening类型**
   ```java
   // 错误
   public class Pipeline {
       public PipelineResult executeDoor(DoorDefinition door) { ... }  // ❌ 太具体
       public PipelineResult executeWindow(WindowDefinition window) { ... }
   }
   
   // 正确
   public class Pipeline {
       public PipelineResult execute(OpeningDefinition def) { ... }  // 通用
   }
   ```

---

## Allowed Dependencies

### ✅ 可以依赖的模块

1. **aperture-parameter** (参数解析)
   - ParameterStage使用

2. **aperture-component** (组件图)
   - ComponentStage使用

3. **aperture-geometry** (几何计算)
   - GeometryStage使用

4. **aperture-mesh** (网格生成)
   - MeshStage使用

5. **aperture-math** (数学工具)
   - 工具函数

**依赖原则**: Pipeline协调所有Kernel模块，但只通过Stage接口调用

---

## Forbidden Dependencies

### ❌ 禁止依赖的模块

1. **❌ aperture-opening** (Opening业务)
   - 理由: Pipeline是通用执行引擎，不绑定Opening

2. **❌ aperture-client** (渲染)
   - 理由: Pipeline产生数据，不关心显示

3. **❌ net.minecraft.*** (Minecraft)
   - 理由: 平台独立性

---

## Input Types

### 📥 接受的输入

1. **Pipeline定义**
   ```java
   public class PipelineDefinition {
       private final List<StageRegistration> stages;
       
       public static PipelineDefinition standard() {
           return new PipelineDefinition(List.of(
               new StageRegistration("definition", DefinitionStage.class),
               new StageRegistration("parameter", ParameterStage.class),
               new StageRegistration("constraint", ConstraintStage.class),
               new StageRegistration("component", ComponentStage.class),
               new StageRegistration("geometry", GeometryStage.class),
               new StageRegistration("mesh", MeshStage.class),
               new StageRegistration("collision", CollisionStage.class),
               new StageRegistration("placement", PlacementStage.class)
           ));
       }
   }
   ```

2. **Stage接口**
   ```java
   public interface PipelineStage<I, O> {
       /**
        * 执行当前阶段
        * 
        * @param input 上一阶段的输出
        * @param ctx 执行上下文
        * @return 当前阶段的输出
        */
       StageResult<O> execute(I input, StageContext ctx);
       
       /**
        * Stage名称（用于日志和缓存）
        */
       String name();
       
       /**
        * 是否可以跳过（如果输入未变化）
        */
       default boolean canSkip(I input, StageContext ctx) {
           return false;
       }
   }
   ```

3. **执行上下文**
   ```java
   public class StageContext {
       private final Map<String, Object> metadata;
       private final ExecutionOptions options;
       private final Logger logger;
       
       public <T> Optional<T> getMetadata(String key) {
           return Optional.ofNullable((T) metadata.get(key));
       }
       
       public void putMetadata(String key, Object value) {
           metadata.put(key, value);
       }
       
       public ExecutionOptions options() {
           return options;
       }
       
       public void log(String message) {
           logger.info(message);
       }
   }
   
   public record ExecutionOptions(
       boolean enableCache,
       boolean enableParallelExecution,
       int timeoutMs,
       LogLevel logLevel
   ) {
       public static ExecutionOptions DEFAULT = new ExecutionOptions(
           true,    // cache enabled
           false,   // no parallel execution
           30000,   // 30s timeout
           LogLevel.INFO
       );
   }
   ```

4. **Opening定义**
   ```java
   public record OpeningDefinition(
       String typeId,                          // "door", "window", etc.
       ParameterSet parameters,
       Optional<ComponentGraph> componentGraph,
       Map<String, Object> metadata
   ) {}
   ```

### 输入验证

```java
public PipelineResult execute(OpeningDefinition definition) {
    Objects.requireNonNull(definition, "definition cannot be null");
    
    if (definition.typeId() == null || definition.typeId().isBlank()) {
        throw new IllegalArgumentException("typeId cannot be null or blank");
    }
    
    if (definition.parameters() == null) {
        throw new IllegalArgumentException("parameters cannot be null");
    }
    
    return executeInternal(definition);
}
```

---

## Output Types

### 📤 产生的输出

1. **Pipeline结果**
   ```java
   public sealed interface PipelineResult {
       record Success(
           Map<String, StageOutput> stageOutputs,
           PipelineMetrics metrics
       ) implements PipelineResult {
           public <T> T getStageOutput(String stageName) {
               return (T) stageOutputs.get(stageName).value();
           }
           
           public Mesh getMesh() {
               return getStageOutput("mesh");
           }
           
           public CollisionShape getCollision() {
               return getStageOutput("collision");
           }
       }
       
       record Failure(
           String failedStage,
           String errorMessage,
           Throwable cause,
           Map<String, StageOutput> partialOutputs
       ) implements PipelineResult {}
       
       boolean isSuccess();
   }
   ```

2. **Stage输出**
   ```java
   public record StageOutput(
       String stageName,
       Object value,
       long executionTimeMs,
       boolean fromCache
   ) {}
   ```

3. **Stage结果**
   ```java
   public sealed interface StageResult<T> {
       record Success<T>(T value) implements StageResult<T> {}
       
       record Failure<T>(
           String message,
           Throwable cause
       ) implements StageResult<T> {}
       
       record Skipped<T>(
           String reason,
           T cachedValue
       ) implements StageResult<T> {}
       
       boolean isSuccess();
       T getValue();
   }
   ```

4. **性能指标**
   ```java
   public record PipelineMetrics(
       long totalExecutionTimeMs,
       Map<String, Long> stageExecutionTimes,
       int cacheHits,
       int cacheMisses,
       int stagesExecuted,
       int stagesSkipped
   ) {
       public double cacheHitRate() {
           int total = cacheHits + cacheMisses;
           return total == 0 ? 0.0 : (double) cacheHits / total;
       }
       
       public String formatReport() {
           StringBuilder sb = new StringBuilder();
           sb.append("Pipeline Execution Report:\n");
           sb.append(String.format("  Total Time: %dms\n", totalExecutionTimeMs));
           sb.append(String.format("  Cache Hit Rate: %.1f%%\n", cacheHitRate() * 100));
           sb.append("  Stage Times:\n");
           
           stageExecutionTimes.forEach((stage, time) ->
               sb.append(String.format("    %s: %dms\n", stage, time))
           );
           
           return sb.toString();
       }
   }
   ```

### 输出不变式

**保证**:
- Success包含所有阶段的输出
- Failure包含失败阶段名称和错误信息
- 阶段按顺序执行（前一阶段失败则后续不执行）
- 缓存命中的阶段executionTime为0
- Metrics数据完整且准确

---

## Lifecycle

### Pipeline创建

```java
public class PipelineBuilder {
    private final List<StageRegistration> stages = new ArrayList<>();
    private ExecutionOptions options = ExecutionOptions.DEFAULT;
    
    public PipelineBuilder addStage(String name, PipelineStage<?, ?> stage) {
        stages.add(new StageRegistration(name, stage));
        return this;
    }
    
    public PipelineBuilder withOptions(ExecutionOptions options) {
        this.options = options;
        return this;
    }
    
    public Pipeline build() {
        validateStageChain(stages);
        return new Pipeline(stages, options);
    }
    
    private void validateStageChain(List<StageRegistration> stages) {
        for (int i = 0; i < stages.size() - 1; i++) {
            Class<?> outputType = stages.get(i).outputType();
            Class<?> inputType = stages.get(i + 1).inputType();
            
            if (!inputType.isAssignableFrom(outputType)) {
                throw new IllegalStateException(
                    "Stage type mismatch: " + stages.get(i).name() + 
                    " outputs " + outputType + ", but " + stages.get(i + 1).name() +
                    " expects " + inputType
                );
            }
        }
    }
}

// 使用
Pipeline pipeline = new PipelineBuilder()
    .addStage("parameter", new ParameterStage())
    .addStage("component", new ComponentStage())
    .addStage("geometry", new GeometryStage())
    .addStage("mesh", new MeshStage())
    .withOptions(ExecutionOptions.DEFAULT)
    .build();
```

### Pipeline执行

```java
public class Pipeline {
    private final List<StageRegistration> stages;
    private final ExecutionOptions options;
    private final PipelineCache cache;
    
    public PipelineResult execute(OpeningDefinition definition) {
        StageContext ctx = new StageContext(options);
        Map<String, StageOutput> outputs = new HashMap<>();
        PipelineMetrics.Builder metrics = new PipelineMetrics.Builder();
        
        Object currentInput = definition;
        
        for (StageRegistration registration : stages) {
            String stageName = registration.name();
            PipelineStage stage = registration.stage();
            
            // 检查缓存
            Optional<Object> cached = cache.get(stageName, currentInput);
            if (cached.isPresent() && options.enableCache()) {
                outputs.put(stageName, new StageOutput(
                    stageName, cached.get(), 0, true
                ));
                metrics.cacheHit();
                currentInput = cached.get();
                continue;
            }
            
            // 执行Stage
            long startTime = System.currentTimeMillis();
            StageResult<?> result = stage.execute(currentInput, ctx);
            long duration = System.currentTimeMillis() - startTime;
            
            if (!result.isSuccess()) {
                // 阶段失败，短路返回
                return new PipelineResult.Failure(
                    stageName,
                    "Stage failed: " + stageName,
                    result instanceof StageResult.Failure f ? f.cause() : null,
                    outputs
                );
            }
            
            Object stageOutput = result.getValue();
            outputs.put(stageName, new StageOutput(
                stageName, stageOutput, duration, false
            ));
            
            // 更新缓存
            if (options.enableCache()) {
                cache.put(stageName, currentInput, stageOutput);
            }
            
            metrics.stageTime(stageName, duration);
            metrics.cacheMiss();
            
            currentInput = stageOutput;
        }
        
        return new PipelineResult.Success(outputs, metrics.build());
    }
}
```

---

## Error Handling

### 异常类型

1. **PipelineException** - Pipeline系统通用异常
   ```java
   public class PipelineException extends RuntimeException {
       public PipelineException(String message) { super(message); }
       public PipelineException(String message, Throwable cause) { super(message, cause); }
   }
   ```

2. **StageExecutionException** - Stage执行失败
   ```java
   public class StageExecutionException extends PipelineException {
       private final String stageName;
       private final Object input;
       
       public StageExecutionException(String stageName, Object input, Throwable cause) {
           super("Stage '" + stageName + "' failed to execute", cause);
           this.stageName = stageName;
           this.input = input;
       }
   }
   ```

3. **PipelineTimeoutException** - 执行超时
   ```java
   public class PipelineTimeoutException extends PipelineException {
       private final long timeoutMs;
       
       public PipelineTimeoutException(long timeoutMs) {
           super("Pipeline execution exceeded timeout: " + timeoutMs + "ms");
           this.timeoutMs = timeoutMs;
       }
   }
   ```

### 错误处理策略

**阶段级错误捕获**:
```java
private StageResult<?> executeStageSafely(PipelineStage stage, Object input, StageContext ctx) {
    try {
        return stage.execute(input, ctx);
    } catch (Exception e) {
        ctx.log("Stage " + stage.name() + " threw exception: " + e.getMessage());
        return new StageResult.Failure<>(
            "Uncaught exception in stage " + stage.name(),
            e
        );
    }
}
```

**超时处理**:
```java
public PipelineResult executeWithTimeout(OpeningDefinition definition) {
    ExecutorService executor = Executors.newSingleThreadExecutor();
    Future<PipelineResult> future = executor.submit(() -> execute(definition));
    
    try {
        return future.get(options.timeoutMs(), TimeUnit.MILLISECONDS);
    } catch (TimeoutException e) {
        future.cancel(true);
        throw new PipelineTimeoutException(options.timeoutMs());
    } catch (Exception e) {
        throw new PipelineException("Pipeline execution failed", e);
    } finally {
        executor.shutdown();
    }
}
```

**部分结果保存**:
```java
// Failure包含已完成阶段的输出
public record Failure(
    String failedStage,
    String errorMessage,
    Throwable cause,
    Map<String, StageOutput> partialOutputs  // 已完成的阶段
) implements PipelineResult {
    public boolean hasPartialResults() {
        return !partialOutputs.isEmpty();
    }
    
    public Optional<Mesh> getPartialMesh() {
        return Optional.ofNullable(partialOutputs.get("mesh"))
            .map(StageOutput::value)
            .map(v -> (Mesh) v);
    }
}
```

---

## Performance Requirements

### 时间复杂度

| 操作 | 复杂度 | 说明 |
|------|--------|------|
| Pipeline执行 | O(Σt_i) | t_i = 第i阶段耗时 |
| 缓存查找 | O(1) | HashMap查找 |
| 缓存写入 | O(1) | HashMap插入 |
| Stage注册 | O(1) | 添加到列表 |

### 性能目标

- **完整Pipeline（冷启动）**: < 100ms
- **完整Pipeline（缓存命中）**: < 1ms
- **单阶段执行**: < 20ms（平均）
- **缓存查找**: < 0.1ms

### 缓存策略

```java
public class PipelineCache {
    private final Map<CacheKey, Object> cache = new ConcurrentHashMap<>();
    private final int maxSize;
    
    public Optional<Object> get(String stageName, Object input) {
        CacheKey key = CacheKey.of(stageName, input);
        return Optional.ofNullable(cache.get(key));
    }
    
    public void put(String stageName, Object input, Object output) {
        if (cache.size() >= maxSize) {
            evict();  // LRU驱逐
        }
        
        CacheKey key = CacheKey.of(stageName, input);
        cache.put(key, output);
    }
    
    public void invalidate(String stageName) {
        cache.keySet().removeIf(key -> key.stageName().equals(stageName));
    }
    
    public void clear() {
        cache.clear();
    }
}

// 缓存键（基于内容哈希）
public record CacheKey(String stageName, int inputHash) {
    public static CacheKey of(String stageName, Object input) {
        return new CacheKey(stageName, computeHash(input));
    }
    
    private static int computeHash(Object input) {
        // 深度哈希（考虑对象内容）
        return Objects.hash(input);  // 简化版本
    }
}
```

---

## The 8 Stages

### 1. Definition Stage

```java
public class DefinitionStage implements PipelineStage<OpeningDefinition, ResolvedDefinition> {
    @Override
    public StageResult<ResolvedDefinition> execute(OpeningDefinition input, StageContext ctx) {
        // 查找Opening类型定义
        OpeningTypeDefinition typeDef = registry.lookup(input.typeId());
        
        if (typeDef == null) {
            return new StageResult.Failure<>(
                "Unknown opening type: " + input.typeId(),
                null
            );
        }
        
        return new StageResult.Success<>(
            new ResolvedDefinition(typeDef, input.parameters())
        );
    }
}
```

### 2. Parameter Stage

```java
public class ParameterStage implements PipelineStage<ResolvedDefinition, ParameterSet> {
    @Override
    public StageResult<ParameterSet> execute(ResolvedDefinition input, StageContext ctx) {
        // 解析参数：应用默认值和用户覆盖
        ParameterSet resolved = ParameterResolver.resolve(
            input.typeDefinition().parameters(),
            input.userParameters()
        );
        
        return new StageResult.Success<>(resolved);
    }
}
```

### 3. Constraint Stage

```java
public class ConstraintStage implements PipelineStage<ParameterSet, ValidatedParameters> {
    @Override
    public StageResult<ValidatedParameters> execute(ParameterSet input, StageContext ctx) {
        // 验证约束
        List<Constraint> constraints = ctx.getMetadata("constraints");
        ValidationResult result = ConstraintValidator.validate(input, constraints);
        
        if (!result.isValid()) {
            return new StageResult.Failure<>(
                "Constraint violation: " + result.issues().get(0).message(),
                null
            );
        }
        
        return new StageResult.Success<>(
            new ValidatedParameters(input)
        );
    }
}
```

### 4. Component Stage

```java
public class ComponentStage implements PipelineStage<ValidatedParameters, ComponentGraph> {
    @Override
    public StageResult<ComponentGraph> execute(ValidatedParameters input, StageContext ctx) {
        // 构建组件图
        ComponentGraph graph = ctx.getMetadata("componentGraph");
        
        if (graph == null) {
            return new StageResult.Failure<>(
                "No component graph provided",
                null
            );
        }
        
        // 绑定参数到组件
        graph.bindParameters(input.parameters());
        
        return new StageResult.Success<>(graph);
    }
}
```

### 5. Geometry Stage

```java
public class GeometryStage implements PipelineStage<ComponentGraph, Shape> {
    @Override
    public StageResult<Shape> execute(ComponentGraph input, StageContext ctx) {
        // 求值组件图
        EvaluationResult result = ComponentGraphEvaluator.evaluate(input);
        
        // 获取最终Shape
        Shape finalShape = result.getOutput("output", "shape");
        
        return new StageResult.Success<>(finalShape);
    }
}
```

### 6. Mesh Stage

```java
public class MeshStage implements PipelineStage<Shape, Mesh> {
    @Override
    public StageResult<Mesh> execute(Shape input, StageContext ctx) {
        MeshingOptions options = ctx.options().meshingOptions();
        
        try {
            Mesh mesh = MeshGenerator.generate(input, options);
            return new StageResult.Success<>(mesh);
        } catch (MeshGenerationException e) {
            return new StageResult.Failure<>(
                "Failed to generate mesh: " + e.getMessage(),
                e
            );
        }
    }
}
```

### 7. Collision Stage

```java
public class CollisionStage implements PipelineStage<Mesh, CollisionShape> {
    @Override
    public StageResult<CollisionShape> execute(Mesh input, StageContext ctx) {
        // 生成碰撞形状（简化的包围盒或凸包）
        CollisionShape collision = CollisionShapeGenerator.fromMesh(input);
        return new StageResult.Success<>(collision);
    }
}
```

### 8. Placement Stage

```java
public class PlacementStage implements PipelineStage<CollisionShape, PlacementInfo> {
    @Override
    public StageResult<PlacementInfo> execute(CollisionShape input, StageContext ctx) {
        // 计算放置信息（footprint、anchor point等）
        PlacementInfo info = PlacementCalculator.calculate(input);
        return new StageResult.Success<>(info);
    }
}
```

---

## Examples

### ✅ 正确用法

```java
// 示例1: 标准Pipeline执行
OpeningDefinition definition = new OpeningDefinition(
    "door",
    ParameterSet.builder()
        .put("width", ParameterValue.length(1200))
        .put("height", ParameterValue.length(2000))
        .build(),
    Optional.empty(),
    Map.of()
);

Pipeline pipeline = PipelineBuilder.standard().build();
PipelineResult result = pipeline.execute(definition);

if (result.isSuccess()) {
    Mesh mesh = ((PipelineResult.Success) result).getMesh();
    // 使用mesh
} else {
    PipelineResult.Failure failure = (PipelineResult.Failure) result;
    System.err.println("Failed at stage: " + failure.failedStage());
}

// 示例2: 自定义Pipeline
Pipeline customPipeline = new PipelineBuilder()
    .addStage("parameter", new ParameterStage())
    .addStage("geometry", new GeometryStage())
    .addStage("mesh", new MeshStage())
    .withOptions(new ExecutionOptions(
        true,    // enable cache
        false,   // no parallel
        60000,   // 60s timeout
        LogLevel.DEBUG
    ))
    .build();

// 示例3: 性能分析
PipelineResult result = pipeline.execute(definition);
if (result instanceof PipelineResult.Success success) {
    System.out.println(success.metrics().formatReport());
    // Output:
    // Pipeline Execution Report:
    //   Total Time: 85ms
    //   Cache Hit Rate: 0.0%
    //   Stage Times:
    //     parameter: 2ms
    //     component: 5ms
    //     geometry: 60ms
    //     mesh: 18ms
}

// 示例4: 部分结果处理
PipelineResult result = pipeline.execute(definition);
if (result instanceof PipelineResult.Failure failure) {
    if (failure.hasPartialResults()) {
        // 使用部分结果（例如geometry成功但mesh失败）
        Optional<Shape> shape = failure.partialOutputs()
            .get("geometry")
            .map(output -> (Shape) output.value());
    }
}
```

### ❌ 错误用法

```java
// 错误1: 绕过Pipeline直接调用底层模块
public class Opening {
    public Shape generate() {
        // ❌ 不应该直接调用Geometry
        return Geometry.extrude(profile, height);
    }
}

// 正确: 通过Pipeline
public class Opening {
    public PipelineResult generate() {
        return pipeline.execute(definition);
    }
}

// 错误2: 在Pipeline中硬编码Opening类型
public class Pipeline {
    public PipelineResult executeDoor(DoorDefinition door) { ... }  // ❌
    public PipelineResult executeWindow(WindowDefinition window) { ... }  // ❌
}

// 正确: 通用执行
public class Pipeline {
    public PipelineResult execute(OpeningDefinition def) { ... }  // ✅
}

// 错误3: 忽略错误处理
PipelineResult result = pipeline.execute(definition);
Mesh mesh = ((PipelineResult.Success) result).getMesh();  // ❌ 可能ClassCastException

// 正确: 检查结果类型
if (result instanceof PipelineResult.Success success) {
    Mesh mesh = success.getMesh();
} else {
    // 处理错误
}
```

---

## Migration Guide

### 违规代码迁移

**场景1: Opening直接调用Geometry**

**现状**:
```java
// aperture-opening/.../Door.java
public class Door {
    public Shape generate() {
        Profile outerProfile = /* ... */;
        Solid outer = Geometry.extrude(outerProfile, height);  // ❌ 绕过Pipeline
        
        Profile innerProfile = /* ... */;
        Solid inner = Geometry.extrude(innerProfile, height - frameThickness);
        
        return Geometry.subtract(outer, inner);
    }
}
```

**迁移**:
```java
// aperture-opening/.../Door.java
public class Door {
    private final Pipeline pipeline;
    
    public PipelineResult generate() {
        OpeningDefinition definition = new OpeningDefinition(
            "door",
            this.parameters,
            Optional.of(buildComponentGraph()),
            Map.of()
        );
        
        return pipeline.execute(definition);  // ✅ 通过Pipeline
    }
    
    private ComponentGraph buildComponentGraph() {
        // 构建组件图（定义数据流）
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
