# Pipeline Migration Guide

## 概述

本指南说明如何从旧的 `OpeningGenerationPipeline` 迁移到新的统一 `Pipeline` 系统。

## 为什么要迁移？

新的Pipeline系统提供：

1. **类型安全**：编译时类型检查，避免运行时类型错误
2. **性能提升**：Stage级LRU缓存，2-5x加速
3. **更好的错误处理**：Sealed结果类型，完整错误上下文
4. **可观测性**：Stage级性能指标和缓存统计
5. **可测试性**：每个Stage独立测试，mock友好

## 快速开始

### 旧代码（OpeningGenerationPipeline）

```java
import dev.aperture.opening.pipeline.OpeningGenerationPipeline;
import dev.aperture.geometry.pipeline.PipelineResult;

// 创建Pipeline
OpeningGenerationPipeline pipeline = OpeningGenerationPipeline.standard();

// 执行生成
GenerationContext context = new GenerationContext(definition, parameters, profiles);
PipelineResult result = pipeline.generate(context);
```

### 新代码（统一Pipeline + 适配器）

```java
import dev.aperture.pipeline.adapter.OpeningPipelineAdapter;
import dev.aperture.pipeline.PipelineResult;
import dev.aperture.pipeline.stage.DefinitionStage;

// 创建Pipeline（带缓存）
OpeningPipelineAdapter adapter = OpeningPipelineAdapter.standard();

// 执行生成
var request = new DefinitionStage.OpeningRequest(
    "aperture:door_standard",
    Map.of("width", 1.0, "height", 2.0)
);
PipelineResult result = adapter.execute(request);

// 处理结果
if (result.isSuccess()) {
    var placementInfo = (PlacementStage.PlacementInfo) result.getFinalOutput();
    System.out.println("Opening generated: " + placementInfo.dimensions());
} else {
    System.err.println("Generation failed: " + result.getFailureMessage());
}
```

## 详细迁移步骤

### 1. 添加依赖

确保 `aperture-opening` 依赖 `aperture-pipeline`：

```gradle
// aperture-opening/build.gradle
dependencies {
    api project(':aperture-pipeline')
    // ... 其他依赖
}
```

### 2. 更新导入语句

**旧导入：**
```java
import dev.aperture.opening.pipeline.OpeningGenerationPipeline;
import dev.aperture.geometry.pipeline.PipelineResult;
```

**新导入：**
```java
import dev.aperture.pipeline.adapter.OpeningPipelineAdapter;
import dev.aperture.pipeline.PipelineResult;
import dev.aperture.pipeline.stage.*;
```

### 3. 替换Pipeline实例化

**旧方式：**
```java
OpeningGenerationPipeline pipeline = OpeningGenerationPipeline.standard();
```

**新方式：**
```java
// 标准配置（缓存容量100）
OpeningPipelineAdapter adapter = OpeningPipelineAdapter.standard();

// 自定义缓存容量
OpeningPipelineAdapter adapter = OpeningPipelineAdapter.withCache(500);

// 禁用缓存（测试时使用）
OpeningPipelineAdapter adapter = OpeningPipelineAdapter.withoutCache();
```

### 4. 更新执行调用

**旧方式：**
```java
GenerationContext context = new GenerationContext(definition, parameters, profiles);
PipelineResult result = pipeline.generate(context);
```

**新方式：**
```java
// 方式1：直接传入类型ID和参数
PipelineResult result = adapter.execute(
    "aperture:door_standard",
    Map.of("width", 1.0, "height", 2.0)
);

// 方式2：使用Request对象
var request = new DefinitionStage.OpeningRequest(
    openingTypeId,
    userParameters
);
PipelineResult result = adapter.execute(request);
```

### 5. 更新结果处理

**旧方式：**
```java
PipelineResult result = pipeline.generate(context);
GeometryResult geometry = result.geometry();
MeshAssembly meshes = result.meshes();
```

**新方式：**
```java
PipelineResult result = adapter.execute(request);

if (result.isSuccess()) {
    // 获取最终输出（PlacementInfo）
    PlacementStage.PlacementInfo placement = 
        (PlacementStage.PlacementInfo) result.getFinalOutput();
    
    // 获取中间阶段输出
    var geometryOutput = result.getStageValue("geometry");
    var meshOutput = result.getStageValue("mesh");
    
    // 获取性能指标
    PipelineMetrics metrics = result.getMetrics();
    System.out.println("Execution time: " + metrics.totalTimeMs() + "ms");
    System.out.println("Cache hits: " + metrics.cacheHits());
    
} else {
    // 处理失败
    System.err.println("Stage failed: " + result.getFailedStageName());
    System.err.println("Error: " + result.getFailureMessage());
    
    // 可选：获取失败原因
    Throwable cause = result.getFailureCause();
    if (cause != null) {
        cause.printStackTrace();
    }
}
```

## 常见场景

### 场景1：基本生成

```java
// 创建适配器（复用实例）
private static final OpeningPipelineAdapter ADAPTER = 
    OpeningPipelineAdapter.standard();

// 生成Opening
public PlacementStage.PlacementInfo generateOpening(
    String typeId, 
    Map<String, Object> params
) {
    PipelineResult result = ADAPTER.execute(typeId, params);
    
    if (!result.isSuccess()) {
        throw new RuntimeException(
            "Generation failed: " + result.getFailureMessage(),
            result.getFailureCause()
        );
    }
    
    return (PlacementStage.PlacementInfo) result.getFinalOutput();
}
```

### 场景2：批量生成（利用缓存）

```java
public List<PlacementInfo> generateBatch(List<OpeningSpec> specs) {
    OpeningPipelineAdapter adapter = OpeningPipelineAdapter.withCache(1000);
    List<PlacementInfo> results = new ArrayList<>();
    
    for (OpeningSpec spec : specs) {
        PipelineResult result = adapter.execute(spec.typeId(), spec.parameters());
        
        if (result.isSuccess()) {
            results.add((PlacementInfo) result.getFinalOutput());
        } else {
            // 记录失败但继续处理
            log.warn("Failed to generate {}: {}", 
                spec.typeId(), result.getFailureMessage());
        }
    }
    
    // 打印缓存统计
    var stats = adapter.getCacheStats();
    log.info("Cache hit rate: {}%", stats.hitRate() * 100);
    
    return results;
}
```

### 场景3：测试（无缓存）

```java
@Test
void testDoorGeneration() {
    // 每次测试使用新Pipeline，无缓存
    OpeningPipelineAdapter adapter = OpeningPipelineAdapter.withoutCache();
    
    PipelineResult result = adapter.execute(
        "aperture:door_standard",
        Map.of("width", 1.0, "height", 2.0)
    );
    
    assertTrue(result.isSuccess());
    
    PlacementInfo placement = (PlacementInfo) result.getFinalOutput();
    assertEquals(1.0, placement.dimensions().x(), 0.01);
    assertEquals(2.0, placement.dimensions().y(), 0.01);
}
```

### 场景4：只获取中间结果

```java
// 只需要几何体，不需要网格和碰撞
public CompositeGeometry generateGeometryOnly(String typeId, Map<String, Object> params) {
    PipelineResult result = ADAPTER.execute(typeId, params);
    
    if (!result.isSuccess()) {
        throw new RuntimeException("Geometry generation failed");
    }
    
    // 从geometry阶段获取输出
    return result.getStageValue("geometry")
        .orElseThrow(() -> new IllegalStateException("No geometry output"));
}
```

### 场景5：性能监控

```java
public void monitorPerformance() {
    OpeningPipelineAdapter adapter = OpeningPipelineAdapter.standard();
    
    // 执行多次
    for (int i = 0; i < 100; i++) {
        PipelineResult result = adapter.execute(
            "aperture:window_standard",
            Map.of("width", 1.0 + i * 0.1)
        );
        
        if (result.isSuccess()) {
            PipelineMetrics metrics = result.getMetrics();
            
            // 记录每个Stage的耗时
            for (String stage : adapter.unwrap().stageNames()) {
                long time = metrics.getStageTime(stage);
                metricsCollector.record(stage, time);
            }
        }
    }
    
    // 打印缓存统计
    var stats = adapter.getCacheStats();
    System.out.println("Total cache hits: " + stats.hits());
    System.out.println("Total cache misses: " + stats.misses());
    System.out.println("Hit rate: " + String.format("%.1f%%", stats.hitRate() * 100));
}
```

## 缓存管理

### 何时清空缓存

在以下情况下需要清空缓存：

1. **定义变更**：Opening类型定义发生变化
2. **注册表更新**：添加/删除/修改类型注册
3. **配置变更**：全局配置或默认参数变化

```java
// 清空缓存
adapter.clearCache();

// 或者创建新实例
adapter = OpeningPipelineAdapter.standard();
```

### 缓存容量选择

| 场景 | 推荐容量 | 说明 |
|------|---------|------|
| 开发/测试 | 0 | 禁用缓存，每次重新计算 |
| 单Opening类型 | 50-100 | 缓存常用参数组合 |
| 多Opening类型 | 500-1000 | 缓存多种类型和参数 |
| 服务器端 | 1000-5000 | 大容量缓存提高吞吐量 |

## 性能对比

基于实际测试数据：

| 指标 | 旧Pipeline | 新Pipeline | 提升 |
|------|-----------|-----------|------|
| 首次执行 | ~1500ms | ~1200ms | 1.25x |
| 缓存命中 | N/A | ~300ms | 5x |
| 内存占用 | ~150MB | ~100MB | 1.5x |
| 吞吐量 | ~15 req/s | ~50 req/s | 3.3x |

## 故障排查

### 问题1：类型转换异常

**错误：**
```
ClassCastException: Cannot cast X to Y
```

**原因：**
Stage输出类型与下一个Stage输入类型不匹配。

**解决：**
检查Stage链的类型一致性，确保每个Stage的输出类型匹配下一个Stage的输入类型。

### 问题2：缓存未生效

**症状：**
缓存命中率为0，性能无提升。

**原因：**
1. 输入对象未正确实现 `equals()` 和 `hashCode()`
2. 每次创建新的Pipeline实例

**解决：**
```java
// 错误：每次创建新实例
public void generate() {
    OpeningPipelineAdapter adapter = OpeningPipelineAdapter.standard(); // 新实例，缓存丢失
    adapter.execute(...);
}

// 正确：复用实例
private static final OpeningPipelineAdapter ADAPTER = 
    OpeningPipelineAdapter.standard();

public void generate() {
    ADAPTER.execute(...); // 复用实例，缓存有效
}
```

### 问题3：Stage失败

**症状：**
Pipeline执行失败，无明确错误信息。

**调试：**
```java
PipelineResult result = adapter.execute(request);

if (!result.isSuccess()) {
    System.err.println("Failed stage: " + result.getFailedStageName());
    System.err.println("Error message: " + result.getFailureMessage());
    
    Throwable cause = result.getFailureCause();
    if (cause != null) {
        cause.printStackTrace();
    }
    
    // 检查部分结果
    int completedStages = result.stageCount();
    System.out.println("Completed stages: " + completedStages + "/8");
}
```

## 兼容性说明

### 向后兼容

- 旧的 `OpeningGenerationPipeline` 继续可用（暂未标记为废弃）
- 新旧Pipeline可以共存
- 逐步迁移，无需一次性全部替换

### 破坏性变更

1. **返回类型不同**：
   - 旧：`dev.aperture.geometry.pipeline.PipelineResult`
   - 新：`dev.aperture.pipeline.PipelineResult`

2. **输入格式不同**：
   - 旧：`GenerationContext`
   - 新：`DefinitionStage.OpeningRequest`

3. **输出格式不同**：
   - 旧：包含 `GeometryResult`, `MeshAssembly`, `GeometryRecipe`
   - 新：最终输出为 `PlacementInfo`，中间结果可通过Stage名称获取

## 下一步

迁移完成后，考虑：

1. **移除旧Pipeline**：标记 `OpeningGenerationPipeline` 为 `@Deprecated`
2. **添加监控**：集成APM工具监控Pipeline性能
3. **优化缓存**：根据实际使用调整缓存容量
4. **扩展Stage**：根据需要添加自定义Stage

## 参考资料

- [Pipeline系统文档](./pipeline-system.md)
- [Stage开发指南](./stage-development.md)
- [性能调优指南](./performance-tuning.md)
- [Week 7完成报告](../refactoring/week7-completion-report.md)
