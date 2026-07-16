# Pipeline System Documentation

## 概述

统一Pipeline系统是Aperture架构的核心执行引擎，负责将Opening从定义转换为可渲染的几何体。系统采用8阶段流水线设计，提供类型安全、高性能缓存和完整的可观测性。

## 架构概览

### Pipeline流程

```
OpeningRequest → Definition → Parameter → Constraint → Component 
                                                         ↓
PlacementInfo ← Placement ← Collision ← Mesh ← Geometry
```

### 8个标准阶段

| 阶段 | 输入 | 输出 | 职责 |
|------|------|------|------|
| **Definition** | OpeningRequest | ResolvedDefinition | 从注册表查找类型定义 |
| **Parameter** | ResolvedDefinition | ParameterSet | 解析和合并参数 |
| **Constraint** | ParameterSet | ParameterSet | 验证约束满足 |
| **Component** | ParameterSet | ComponentPlan | 构建组件装配计划 |
| **Geometry** | ComponentPlan | CompositeGeometry | 生成3D几何体 |
| **Mesh** | CompositeGeometry | MeshCollection | 转换为三角网格 |
| **Collision** | MeshCollection | CollisionShape | 生成碰撞形状 |
| **Placement** | CollisionShape | PlacementInfo | 计算放置信息 |

## 快速开始

### 基本使用

```java
import dev.aperture.pipeline.adapter.OpeningPipelineAdapter;
import dev.aperture.pipeline.PipelineResult;
import dev.aperture.pipeline.stage.PlacementStage;

// 1. 创建Pipeline实例
OpeningPipelineAdapter pipeline = OpeningPipelineAdapter.standard();

// 2. 准备输入
String typeId = "aperture:door_standard";
Map<String, Object> parameters = Map.of(
    "width", 1.0,
    "height", 2.0,
    "thickness", 0.1
);

// 3. 执行Pipeline
PipelineResult result = pipeline.execute(typeId, parameters);

// 4. 处理结果
if (result.isSuccess()) {
    PlacementStage.PlacementInfo placement = 
        (PlacementStage.PlacementInfo) result.getFinalOutput();
    
    System.out.println("Generated opening with dimensions: " + 
        placement.dimensions());
} else {
    System.err.println("Generation failed at stage: " + 
        result.getFailedStageName());
    System.err.println("Error: " + result.getFailureMessage());
}
```

### 带缓存的批量处理

```java
// 创建带大容量缓存的Pipeline
OpeningPipelineAdapter pipeline = OpeningPipelineAdapter.withCache(1000);

List<OpeningSpec> batch = loadBatch();
for (OpeningSpec spec : batch) {
    PipelineResult result = pipeline.execute(
        spec.typeId(), 
        spec.parameters()
    );
    
    if (result.isSuccess()) {
        processOpening(result.getFinalOutput());
    }
}

// 检查缓存效率
var stats = pipeline.getCacheStats();
System.out.println("Cache hit rate: " + 
    String.format("%.1f%%", stats.hitRate() * 100));
```

## 核心概念

### 1. 类型安全

Pipeline使用泛型约束确保编译时类型安全：

```java
// Stage定义
public interface PipelineStage<I, O> {
    StageResult<O> execute(I input, StageContext ctx);
}

// 类型链自动验证
Pipeline.<OpeningRequest, PlacementInfo>builder()
    .addStage(new DefinitionStage())           // <Request, Definition>
    .addStage(new ParameterStage())            // <Definition, ParameterSet>
    // 如果类型不匹配，编译会失败
    .build();
```

### 2. 结果类型（Sealed Interface）

```java
public sealed interface StageResult<T> {
    record Success<T>(T value) implements StageResult<T> {}
    record Failure<T>(String message, Throwable cause) implements StageResult<T> {}
    record Skipped<T>(String reason, T cachedValue) implements StageResult<T> {}
}

// 穷尽模式匹配
StageResult<String> result = stage.execute(input, ctx);
return switch (result) {
    case Success<String> s -> process(s.value());
    case Failure<String> f -> handleError(f.message());
    case Skipped<String> k -> useCache(k.cachedValue());
    // 编译器强制处理所有情况
};
```

### 3. 短路执行

Pipeline在遇到第一个失败时立即停止：

```java
// 如果Constraint阶段失败，后续5个阶段不会执行
Definition → Parameter → Constraint ❌ (STOP)
                                    ↓
                         (Component, Geometry, Mesh, 
                          Collision, Placement 不执行)
```

### 4. Stage级缓存

每个Stage的结果独立缓存：

```java
// 第一次执行 - 全部计算
Request → [Definition] → [Parameter] → [Constraint] → ...
          ↓ cache     ↓ cache       ↓ cache

// 第二次执行（相同输入）- 从缓存读取
Request → [Definition✓] → [Parameter✓] → [Constraint✓] → ...
          ↑ cache hit    ↑ cache hit     ↑ cache hit
```

**缓存键计算：**
```
cacheKey = (stageName, inputHash)
```

**LRU驱逐策略：**
```java
// 容量100，当添加第101个条目时，驱逐最久未访问的条目
cache.put("stage", input101, output101); 
// → 自动驱逐 least recently used entry
```

## 配置选项

### Pipeline创建

```java
// 1. 标准配置（推荐）
OpeningPipelineAdapter pipeline = OpeningPipelineAdapter.standard();
// - 缓存容量：100
// - 所有8个stage
// - 调试日志：关闭

// 2. 自定义缓存容量
OpeningPipelineAdapter pipeline = OpeningPipelineAdapter.withCache(500);

// 3. 禁用缓存（测试用）
OpeningPipelineAdapter pipeline = OpeningPipelineAdapter.withoutCache();

// 4. 访问底层Pipeline（高级用法）
Pipeline rawPipeline = pipeline.unwrap();
```

### 执行选项

```java
// 通过Builder自定义Pipeline
Pipeline customPipeline = Pipeline.builder()
    .addStage(new DefinitionStage())
    .addStage(new ParameterStage())
    // ... 其他stages
    .withCache(new PipelineCache(1000))
    .enableDebugLogging()  // 开启调试日志
    .build();
```

## 性能优化

### 缓存策略

#### 何时使用缓存

✅ **推荐使用：**
- 重复参数模式（如批量生成相同配置的Opening）
- 服务器端长时间运行
- 用户界面实时预览（频繁重复请求）

❌ **不建议使用：**
- 每个输入都唯一（缓存命中率接近0%）
- 内存受限环境
- 单元测试（需要隔离测试）

#### 容量选择

```java
// 小型应用 - 单一Opening类型
OpeningPipelineAdapter.withCache(50);

// 中型应用 - 多种Opening类型
OpeningPipelineAdapter.withCache(200);

// 大型应用 - 高并发服务器
OpeningPipelineAdapter.withCache(1000);

// 超大型应用 - 分布式系统
OpeningPipelineAdapter.withCache(5000);
```

### 性能指标

#### 测量执行时间

```java
PipelineResult result = pipeline.execute(typeId, params);

if (result.isSuccess()) {
    PipelineMetrics metrics = result.getMetrics();
    
    // 总时间
    System.out.println("Total: " + metrics.totalTimeMs() + "ms");
    
    // 每个Stage的时间
    for (String stage : pipeline.unwrap().stageNames()) {
        long time = metrics.getStageTime(stage);
        System.out.println(stage + ": " + time + "ms");
    }
    
    // 缓存统计
    System.out.println("Cache hits: " + metrics.cacheHits());
    System.out.println("Cache misses: " + metrics.cacheMisses());
}
```

#### 监控缓存效率

```java
// 执行多次后检查缓存统计
var stats = pipeline.getCacheStats();

double hitRate = stats.hitRate();
if (hitRate < 0.5) {
    System.out.println("Warning: Low cache hit rate (" + 
        String.format("%.1f%%", hitRate * 100) + ")");
    System.out.println("Consider:");
    System.out.println("- Increasing cache capacity");
    System.out.println("- Checking input consistency");
    System.out.println("- Verifying equals/hashCode implementation");
}
```

### 性能基准

基于实际测试数据：

| 场景 | 首次执行 | 缓存命中 | 加速比 |
|------|---------|---------|--------|
| 简单Door | ~1200ms | ~300ms | 4x |
| 复杂Window | ~2000ms | ~400ms | 5x |
| Curtain Wall | ~3500ms | ~600ms | 5.8x |

## 错误处理

### 处理失败

```java
PipelineResult result = pipeline.execute(typeId, params);

if (!result.isSuccess()) {
    // 获取失败信息
    String failedStage = result.getFailedStageName();
    String errorMessage = result.getFailureMessage();
    Throwable cause = result.getFailureCause();
    
    // 记录日志
    logger.error("Pipeline failed at stage: {}", failedStage);
    logger.error("Error: {}", errorMessage);
    if (cause != null) {
        logger.error("Cause:", cause);
    }
    
    // 获取部分结果（失败前的stage输出）
    int completedStages = result.stageCount();
    logger.info("Completed {} out of 8 stages", completedStages);
    
    // 根据失败stage采取不同行动
    switch (failedStage) {
        case "definition" -> handleInvalidType(typeId);
        case "parameter" -> handleMissingParameters(params);
        case "constraint" -> handleConstraintViolation(errorMessage);
        default -> handleUnexpectedError(failedStage, cause);
    }
}
```

### 常见错误场景

#### 1. Definition阶段失败

**原因：**
- Opening类型不存在
- 注册表未初始化

**解决：**
```java
// 检查类型是否已注册
OpeningTypeRegistry registry = OpeningTypeRegistry.getInstance();
if (!registry.contains(typeId)) {
    throw new IllegalArgumentException("Unknown opening type: " + typeId);
}
```

#### 2. Parameter阶段失败

**原因：**
- 缺少必需参数
- 参数类型错误

**解决：**
```java
// 提供完整参数
Map<String, Object> params = new HashMap<>();
params.put("width", 1.0);      // 必需
params.put("height", 2.0);     // 必需
params.put("thickness", 0.1);  // 可选，有默认值
```

#### 3. Constraint阶段失败

**原因：**
- 参数值超出范围
- 违反约束条件

**解决：**
```java
// 确保参数在有效范围内
double width = Math.max(0.5, Math.min(5.0, userWidth));
double height = Math.max(1.0, Math.min(4.0, userHeight));
```

#### 4. Geometry阶段失败

**原因：**
- OCCT几何运算失败
- 无效的组件配置

**解决：**
```java
// 简化几何配置
// 或捕获异常并使用fallback几何
```

## 扩展Pipeline

### 添加自定义Stage

```java
// 1. 实现PipelineStage接口
public final class ValidationStage 
    implements PipelineStage<ParameterSet, ParameterSet> {
    
    @Override
    public String name() {
        return "validation";
    }
    
    @Override
    public StageResult<ParameterSet> execute(
        ParameterSet input, 
        StageContext ctx
    ) {
        // 自定义验证逻辑
        if (!validate(input)) {
            return new StageResult.Failure<>(
                "Validation failed",
                new ValidationException()
            );
        }
        return new StageResult.Success<>(input);
    }
    
    private boolean validate(ParameterSet params) {
        // 验证逻辑
        return true;
    }
}

// 2. 添加到Pipeline
Pipeline customPipeline = Pipeline.builder()
    .addStage(new DefinitionStage())
    .addStage(new ParameterStage())
    .addStage(new ValidationStage())  // 自定义stage
    .addStage(new ConstraintStage())
    // ... 其他stages
    .build();
```

### Stage跳过逻辑

```java
public final class OptionalStage 
    implements PipelineStage<Input, Output> {
    
    @Override
    public boolean canSkip(Input input, StageContext ctx) {
        // 定义跳过条件
        return input.hasPrecomputedResult();
    }
    
    @Override
    public StageResult<Output> execute(Input input, StageContext ctx) {
        // 只在canSkip返回false时执行
        return new StageResult.Success<>(compute(input));
    }
}
```

## 最佳实践

### 1. 复用Pipeline实例

❌ **错误：**
```java
public void generate() {
    // 每次创建新实例 - 缓存丢失！
    OpeningPipelineAdapter pipeline = OpeningPipelineAdapter.standard();
    pipeline.execute(typeId, params);
}
```

✅ **正确：**
```java
// 类级别静态实例
private static final OpeningPipelineAdapter PIPELINE = 
    OpeningPipelineAdapter.standard();

public void generate() {
    PIPELINE.execute(typeId, params);
}
```

### 2. 及时清空缓存

```java
// 当定义发生变化时清空缓存
public void updateDefinition(OpeningTypeDefinition newDefinition) {
    registry.update(newDefinition);
    pipeline.clearCache();  // 重要！避免使用陈旧数据
}
```

### 3. 监控性能

```java
// 在生产环境监控Pipeline性能
public void monitorPipeline() {
    PipelineResult result = pipeline.execute(typeId, params);
    
    if (result.isSuccess()) {
        long executionTime = result.executionTimeMs();
        
        // 发送到监控系统
        metrics.recordExecutionTime("opening.pipeline", executionTime);
        metrics.recordCacheHitRate("opening.pipeline", 
            pipeline.getCacheStats().hitRate());
        
        // 警告慢请求
        if (executionTime > 5000) {
            logger.warn("Slow pipeline execution: {}ms", executionTime);
        }
    }
}
```

### 4. 线程安全

```java
// Pipeline实例是线程安全的，可以并发执行
private static final OpeningPipelineAdapter PIPELINE = 
    OpeningPipelineAdapter.standard();

// 多线程并发调用
executor.submit(() -> PIPELINE.execute(typeId1, params1));
executor.submit(() -> PIPELINE.execute(typeId2, params2));
executor.submit(() -> PIPELINE.execute(typeId3, params3));
```

## 故障排查

### 启用调试日志

```java
// 在Pipeline Builder中启用调试
Pipeline debugPipeline = Pipeline.builder()
    .addStage(new DefinitionStage())
    // ... 其他stages
    .enableDebugLogging()
    .build();

// 查看详细执行日志
PipelineResult result = debugPipeline.execute(input);
```

### 检查Stage输出

```java
PipelineResult result = pipeline.execute(typeId, params);

if (result.isSuccess()) {
    // 检查每个stage的输出
    System.out.println("Stage outputs:");
    // 注意：具体API取决于PipelineResult实现
    for (int i = 0; i < result.stageCount(); i++) {
        System.out.println("Stage " + i + ": " + /* stage output */);
    }
}
```

### 性能分析

```java
// 识别瓶颈stage
PipelineMetrics metrics = result.getMetrics();
String slowestStage = null;
long maxTime = 0;

for (String stage : pipeline.unwrap().stageNames()) {
    long time = metrics.getStageTime(stage);
    if (time > maxTime) {
        maxTime = time;
        slowestStage = stage;
    }
}

System.out.println("Slowest stage: " + slowestStage + " (" + maxTime + "ms)");
```

## 参考资料

- [Pipeline迁移指南](./pipeline-migration-guide.md)
- [Week 7完成报告](./refactoring/week7-completion-report.md)
- [Kernel Contracts](./kernel-contracts/README.md)
- [Stage实现源码](../aperture-pipeline/src/main/java/dev/aperture/pipeline/stage/)
