# Aperture Kernel API Guide

## 概述

Aperture Kernel是Aperture系统的统一API，提供简洁、类型安全的Opening生成接口。它隐藏了底层Pipeline的复杂性，让Opening生成变得简单直观。

## 快速开始

### 基本使用

```java
import dev.aperture.kernel.ApertureKernel;
import dev.aperture.kernel.OpeningResult;

// 1. 创建Kernel
try (ApertureKernel kernel = ApertureKernel.builder().build()) {
    
    // 2. 生成Opening
    OpeningResult result = kernel.generate(
        "aperture:door_standard",
        Map.of("width", 1.0, "height", 2.0)
    );
    
    // 3. 处理结果
    if (result.isSuccess()) {
        var success = result.asSuccess();
        System.out.println("Generated: " + success.placement().dimensions());
    } else {
        var failure = result.asFailure();
        System.err.println("Failed: " + failure.errorMessage());
    }
}
```

## 核心概念

### 1. Kernel实例

Kernel是Opening生成的入口点。通过Builder创建，支持自定义配置。

```java
ApertureKernel kernel = ApertureKernel.builder()
    .withCacheCapacity(1000)        // 缓存容量
    .withAsyncThreadPoolSize(8)     // 异步线程数
    .enableDebugLogging()           // 调试日志
    .build();
```

**重要：** Kernel实现了`AutoCloseable`，应使用try-with-resources或手动调用`close()`。

### 2. OpeningRequest

请求封装了Opening类型、参数和选项。

```java
// 简单构造
var request = new OpeningRequest(
    "aperture:door_standard",
    Map.of("width", 1.0, "height", 2.0)
);

// 带选项构造
var options = OpeningOptions.DEFAULT
    .withMeshQuality(MeshQuality.HIGH)
    .withCollisionStrategy(CollisionStrategy.SIMPLIFIED_MESH);

var request = new OpeningRequest(
    "aperture:window_standard",
    Map.of("width", 1.5),
    options
);
```

### 3. OpeningResult

结果是Sealed接口，只有Success和Failure两种变体。

```java
OpeningResult result = kernel.generate(request);

// 模式匹配（Java 17+）
switch (result) {
    case OpeningResult.Success success -> {
        PlacementInfo placement = success.placement();
        GenerationMetrics metrics = success.metrics();
        // 处理成功
    }
    case OpeningResult.Failure failure -> {
        String stage = failure.failedStage();
        String error = failure.errorMessage();
        // 处理失败
    }
}

// 或使用断言方法
if (result.isSuccess()) {
    var placement = result.asSuccess().placement();
}
```

## 功能详解

### 同步生成

标准的阻塞式生成，适合大多数场景。

```java
// 方式1：直接传参
OpeningResult result = kernel.generate(
    "aperture:door_standard",
    Map.of("width", 1.0, "height", 2.0)
);

// 方式2：使用Request对象
var request = new OpeningRequest("aperture:door_standard", params);
OpeningResult result = kernel.generate(request);
```

### 异步生成

非阻塞式生成，返回`CompletableFuture`。

```java
CompletableFuture<OpeningResult> future = kernel.generateAsync(request);

// 等待完成
OpeningResult result = future.get();

// 或使用回调
future.thenAccept(result -> {
    if (result.isSuccess()) {
        System.out.println("Async generation completed");
    }
}).exceptionally(ex -> {
    System.err.println("Failed: " + ex.getMessage());
    return null;
});
```

### 批量生成

顺序执行多个请求，最大化缓存利用率。

```java
List<OpeningRequest> requests = List.of(
    new OpeningRequest("aperture:door_standard", Map.of("width", 1.0)),
    new OpeningRequest("aperture:door_standard", Map.of("width", 1.5)),
    new OpeningRequest("aperture:window_standard", Map.of("width", 2.0))
);

List<OpeningResult> results = kernel.generateBatch(requests);

// 统计成功率
long successCount = results.stream()
    .filter(OpeningResult::isSuccess)
    .count();

System.out.println("Success: " + successCount + "/" + results.size());
```

### 部分生成（实验性）

生成到指定阶段，用于调试或获取中间结果。

```java
// 注意：当前仅支持"placement"阶段
PartialResult<PlacementInfo> result = kernel.generateUntil(
    request,
    "placement"
);

if (result.isSuccess()) {
    PlacementInfo placement = result.asSuccess().getValue();
}
```

## 配置选项

### OpeningOptions

控制生成行为的选项。

```java
// 预定义选项
OpeningOptions.DEFAULT      // 标准配置
OpeningOptions.TEST         // 测试配置（无缓存，低质量）
OpeningOptions.PRODUCTION   // 生产配置（大缓存，高质量）

// 自定义选项
var options = OpeningOptions.DEFAULT
    .withCache(true)                              // 启用缓存
    .withDebugLogging(false)                      // 禁用调试
    .withMeshQuality(MeshQuality.HIGH)            // 高质量网格
    .withCollisionStrategy(CollisionStrategy.CONVEX_HULL);  // 凸包碰撞
```

#### MeshQuality

| 质量 | 三角形数 | 生成时间 | 用途 |
|------|---------|---------|------|
| LOW | ~50-200 | ~50-100ms | 快速预览 |
| NORMAL | ~200-800 | ~100-300ms | 标准使用 |
| HIGH | ~800-3000 | ~300-1000ms | 高质量渲染 |

#### CollisionStrategy

| 策略 | 性能 | 精度 | 用途 |
|------|------|------|------|
| BOUNDING_BOX | 最快 | 最低 | 粗略检测 |
| CONVEX_HULL | 快 | 低 | 简单形状 |
| SIMPLIFIED_MESH | 中 | 中 | 平衡 |
| EXACT_MESH | 慢 | 最高 | 精确检测 |

### KernelBuilder配置

```java
ApertureKernel kernel = ApertureKernel.builder()
    // 缓存配置
    .withCacheCapacity(1000)              // 缓存容量（0=禁用）
    
    // 异步配置
    .withAsyncThreadPoolSize(8)           // 异步线程数
    // 或使用自定义线程池
    .withExecutorService(myExecutor)
    
    // 依赖配置
    .withRegistry(customRegistry)         // 自定义Registry
    .withProfiles(customProfiles)         // 自定义ProfileCatalog
    
    // 日志配置
    .enableDebugLogging()                 // 启用调试日志
    
    .build();
```

#### 便捷工厂方法

```java
// 测试环境：无缓存，单线程，调试开启
ApertureKernel testKernel = KernelBuilder.buildForTesting();

// 生产环境：大缓存，8线程，调试关闭
ApertureKernel prodKernel = KernelBuilder.buildForProduction();
```

## 统计和监控

### 获取统计信息

```java
KernelStats stats = kernel.getStats();

System.out.println("Total requests: " + stats.totalRequests());
System.out.println("Success rate: " + 
    String.format("%.1f%%", stats.successRate() * 100));
System.out.println("Avg execution time: " + 
    String.format("%.1fms", stats.averageExecutionTimeMs()));

// 缓存统计
var cacheStats = stats.cacheStats();
System.out.println("Cache hit rate: " + 
    String.format("%.1f%%", cacheStats.hitRate() * 100));

// 失败统计
if (!stats.failuresByStage().isEmpty()) {
    System.out.println("Most failed stage: " + stats.getMostFailedStage());
}
```

### 健康检查

```java
if (!kernel.isHealthy()) {
    KernelStats stats = kernel.getStats();
    
    if (stats.successRate() < 0.95) {
        System.err.println("Low success rate: " + stats.successRate());
    }
    
    if (stats.cacheStats().hitRate() < 0.70) {
        System.err.println("Low cache hit rate");
        // 考虑增加缓存容量
    }
}
```

### GenerationMetrics

每次成功的生成都包含详细的性能指标。

```java
var success = result.asSuccess();
GenerationMetrics metrics = success.metrics();

// 总时间
System.out.println("Total: " + metrics.totalTimeMs() + "ms");

// 缓存统计
System.out.println("Cache hits: " + metrics.cacheHits());
System.out.println("Cache misses: " + metrics.cacheMisses());
System.out.println("Hit rate: " + 
    String.format("%.1f%%", metrics.cacheHitRate() * 100));

// 阶段耗时
String slowest = metrics.getSlowestStage();
long slowestTime = metrics.getSlowestStageTime();
System.out.println("Slowest stage: " + slowest + " (" + slowestTime + "ms)");

// 完整报告
System.out.println(metrics.format());
```

## Registry管理

### 查询类型

```java
// 列出所有类型
Set<String> types = kernel.listTypes();
System.out.println("Available types: " + types);

// 获取类型定义
Optional<OpeningTypeDefinition> def = kernel.getDefinition("aperture:door_standard");
if (def.isPresent()) {
    System.out.println("Found type: " + def.get().id());
}
```

### 注册新类型

```java
OpeningTypeDefinition newType = // ... 创建定义
kernel.registerType(newType);

// 注意：注册会清空缓存！
```

## 缓存管理

### 清空缓存

```java
// 在定义变更后清空
kernel.registerType(newDefinition);  // 自动清空
// 或手动清空
kernel.clearCache();
```

### 缓存策略

**何时使用缓存：**
- ✅ 重复的参数组合（如批量生成）
- ✅ 用户界面实时预览
- ✅ 服务器端长时间运行

**何时禁用缓存：**
- ❌ 每个请求都唯一
- ❌ 测试环境
- ❌ 内存受限

```java
// 禁用缓存
ApertureKernel noCacheKernel = ApertureKernel.builder()
    .withCacheCapacity(0)
    .build();
```

## 错误处理

### 处理失败结果

```java
OpeningResult result = kernel.generate(request);

if (!result.isSuccess()) {
    var failure = result.asFailure();
    
    String stage = failure.failedStage();
    String message = failure.errorMessage();
    
    // 根据失败阶段采取不同行动
    switch (stage) {
        case "definition" -> 
            System.err.println("Type not found: " + result.typeId());
        case "parameter" -> 
            System.err.println("Missing or invalid parameters");
        case "constraint" -> 
            System.err.println("Constraint violation: " + message);
        case "geometry" -> 
            System.err.println("Geometry generation failed: " + message);
        default -> 
            System.err.println("Unknown error at " + stage);
    }
    
    // 获取原因（如果有）
    failure.getCause().ifPresent(Throwable::printStackTrace);
}
```

### 常见错误

| 错误 | 原因 | 解决方案 |
|------|------|----------|
| Type not found | 类型未注册 | 检查typeId，确保已注册 |
| Missing parameters | 缺少必需参数 | 检查参数完整性 |
| Constraint violation | 参数值超出范围 | 调整参数到有效范围 |
| Geometry failed | OCCT运算失败 | 简化几何或检查参数 |

### 异常处理

```java
try {
    OpeningResult result = kernel.generate(request);
    // 处理结果
} catch (IllegalStateException e) {
    // Kernel已关闭
    System.err.println("Kernel is closed");
} catch (IllegalArgumentException e) {
    // 参数验证失败
    System.err.println("Invalid argument: " + e.getMessage());
}
```

## 资源管理

### 生命周期

```java
// 推荐：try-with-resources
try (ApertureKernel kernel = ApertureKernel.builder().build()) {
    // 使用kernel
    OpeningResult result = kernel.generate(...);
} // 自动关闭

// 或手动管理
ApertureKernel kernel = ApertureKernel.builder().build();
try {
    // 使用kernel
} finally {
    kernel.close();
}
```

### 关闭行为

```java
kernel.close();

// 关闭后：
// - 线程池停止
// - 缓存清空
// - 所有操作抛出IllegalStateException

assertTrue(kernel.isClosed());
```

## 性能优化

### 1. 合理设置缓存容量

```java
// 小型应用
ApertureKernel.builder().withCacheCapacity(50).build();

// 中型应用
ApertureKernel.builder().withCacheCapacity(200).build();

// 大型应用
ApertureKernel.builder().withCacheCapacity(1000).build();
```

### 2. 批量处理优化

```java
// 对请求排序以提高缓存命中率
List<OpeningRequest> sorted = requests.stream()
    .sorted(Comparator.comparing(OpeningRequest::typeId))
    .toList();

List<OpeningResult> results = kernel.generateBatch(sorted);
```

### 3. 选择合适的质量

```java
// 预览：使用LOW质量
var previewOptions = OpeningOptions.DEFAULT
    .withMeshQuality(MeshQuality.LOW);

// 最终渲染：使用HIGH质量
var renderOptions = OpeningOptions.DEFAULT
    .withMeshQuality(MeshQuality.HIGH);
```

### 4. 监控性能

```java
// 定期检查
KernelStats stats = kernel.getStats();

if (stats.averageExecutionTimeMs() > 2000) {
    System.out.println("Warning: Slow average execution time");
}

if (stats.cacheStats().hitRate() < 0.5) {
    System.out.println("Warning: Low cache hit rate");
    System.out.println("Consider increasing cache capacity");
}
```

## 最佳实践

### 1. 复用Kernel实例

```java
// ❌ 错误：每次创建新实例
public void generate() {
    ApertureKernel kernel = ApertureKernel.builder().build();
    kernel.generate(...);
    kernel.close();
}

// ✅ 正确：复用实例
private static final ApertureKernel KERNEL = 
    ApertureKernel.builder().build();

public void generate() {
    KERNEL.generate(...);
}
```

### 2. 使用try-with-resources

```java
// ✅ 推荐
try (ApertureKernel kernel = ApertureKernel.builder().build()) {
    return kernel.generate(request);
}
```

### 3. 检查结果类型

```java
// ✅ 使用模式匹配
if (result instanceof OpeningResult.Success success) {
    processSuccess(success);
}

// ✅ 或使用isSuccess()
if (result.isSuccess()) {
    var success = result.asSuccess();
}

// ❌ 避免直接转换
// var success = (OpeningResult.Success) result; // 可能抛异常
```

### 4. 监控和日志

```java
// 开发环境：启用日志
ApertureKernel devKernel = ApertureKernel.builder()
    .enableDebugLogging()
    .build();

// 生产环境：集成监控
OpeningResult result = kernel.generate(request);
if (result.isSuccess()) {
    metrics.recordSuccess(result.asSuccess().metrics().totalTimeMs());
} else {
    metrics.recordFailure(result.asFailure().failedStage());
}
```

## 故障排查

### 问题1：生成失败

```java
// 1. 检查结果
if (!result.isSuccess()) {
    var failure = result.asFailure();
    System.out.println("Failed at: " + failure.failedStage());
    System.out.println("Message: " + failure.errorMessage());
    failure.getCause().ifPresent(Throwable::printStackTrace);
}

// 2. 启用调试日志
ApertureKernel debugKernel = ApertureKernel.builder()
    .enableDebugLogging()
    .build();
```

### 问题2：性能慢

```java
// 1. 检查缓存命中率
KernelStats stats = kernel.getStats();
if (stats.cacheStats().hitRate() < 0.5) {
    // 增加缓存容量或检查输入一致性
}

// 2. 检查阶段耗时
if (result.isSuccess()) {
    GenerationMetrics metrics = result.asSuccess().metrics();
    System.out.println("Slowest: " + metrics.getSlowestStage() + 
        " (" + metrics.getSlowestStageTime() + "ms)");
}
```

### 问题3：内存占用高

```java
// 1. 减小缓存容量
ApertureKernel.builder().withCacheCapacity(50).build();

// 2. 定期清空缓存
kernel.clearCache();

// 3. 使用try-with-resources确保资源释放
```

## 参考资料

- [Pipeline系统文档](./pipeline-system.md)
- [Pipeline迁移指南](./pipeline-migration-guide.md)
- [Week 8计划](./progress/week-8-plan.md)
- [Week 8测试总结](./progress/week-8-testing-summary.md)
