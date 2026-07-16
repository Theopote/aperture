# Week 8: Kernel API Design

**时间**: 2026-07-16  
**状态**: 🚧 进行中  
**目标**: 设计并实现ApertureKernel统一API，整合Pipeline、Registry和ProfileCatalog

---

## 🎯 核心目标

创建统一的Kernel门面（Facade）接口，隐藏底层复杂性，为Opening生成提供简洁、类型安全的API。

### 关键职责

1. **统一入口** - 提供单一、清晰的Opening生成API
2. **管理依赖** - 整合Registry、Pipeline、ProfileCatalog
3. **生命周期管理** - 初始化、配置、关闭
4. **资源管理** - 缓存管理、线程池管理
5. **错误处理** - 统一的错误处理和日志

---

## 📐 设计原则

### 1. Facade模式
隐藏子系统复杂性，提供高层接口：
```
Client → ApertureKernel → [Pipeline, Registry, ProfileCatalog, Cache]
```

### 2. Builder模式
灵活配置，支持默认值：
```java
ApertureKernel kernel = ApertureKernel.builder()
    .withCacheCapacity(1000)
    .withProfiles(profileCatalog)
    .enableDebugLogging()
    .build();
```

### 3. 不可变性
Kernel实例创建后不可修改，线程安全。

### 4. 资源管理
实现AutoCloseable，支持try-with-resources。

---

## 🏗️ 架构设计

### 核心接口

```java
/**
 * Aperture Kernel - 统一的Opening生成API
 */
public interface ApertureKernel extends AutoCloseable {
    
    /**
     * 生成Opening（完整8阶段Pipeline）
     */
    OpeningResult generate(OpeningRequest request);
    
    /**
     * 生成Opening（简化API）
     */
    OpeningResult generate(String typeId, Map<String, Object> parameters);
    
    /**
     * 批量生成（利用缓存）
     */
    List<OpeningResult> generateBatch(List<OpeningRequest> requests);
    
    /**
     * 异步生成
     */
    CompletableFuture<OpeningResult> generateAsync(OpeningRequest request);
    
    /**
     * 只生成到指定阶段
     */
    <T> StageResult<T> generateUntil(OpeningRequest request, String stageName);
    
    /**
     * 获取Opening类型定义
     */
    Optional<OpeningTypeDefinition> getDefinition(String typeId);
    
    /**
     * 列出所有已注册的Opening类型
     */
    Set<String> listTypes();
    
    /**
     * 注册新的Opening类型
     */
    void registerType(OpeningTypeDefinition definition);
    
    /**
     * 清空缓存
     */
    void clearCache();
    
    /**
     * 获取统计信息
     */
    KernelStats getStats();
    
    @Override
    void close();
}
```

### OpeningRequest

```java
/**
 * Opening生成请求
 */
public record OpeningRequest(
    String typeId,
    Map<String, Object> parameters,
    OpeningOptions options
) {
    public OpeningRequest(String typeId, Map<String, Object> parameters) {
        this(typeId, parameters, OpeningOptions.DEFAULT);
    }
    
    // 验证
    public void validate() {
        Objects.requireNonNull(typeId, "typeId cannot be null");
        Objects.requireNonNull(parameters, "parameters cannot be null");
    }
}
```

### OpeningResult

```java
/**
 * Opening生成结果
 */
public sealed interface OpeningResult {
    
    record Success(
        String typeId,
        PlacementInfo placement,
        GenerationMetrics metrics
    ) implements OpeningResult {
        // 访问中间结果
        public <T> Optional<T> getStageOutput(String stageName);
    }
    
    record Failure(
        String typeId,
        String failedStage,
        String errorMessage,
        Throwable cause
    ) implements OpeningResult {}
    
    boolean isSuccess();
    String typeId();
}
```

### OpeningOptions

```java
/**
 * Opening生成选项
 */
public record OpeningOptions(
    boolean enableCache,
    boolean enableDebugLogging,
    CollisionStrategy collisionStrategy,
    MeshQuality meshQuality
) {
    public static final OpeningOptions DEFAULT = new OpeningOptions(
        true,                               // enableCache
        false,                              // enableDebugLogging
        CollisionStrategy.CONVEX_HULL,      // collisionStrategy
        MeshQuality.NORMAL                  // meshQuality
    );
    
    public enum CollisionStrategy {
        CONVEX_HULL,
        BOUNDING_BOX,
        SIMPLIFIED_MESH,
        EXACT_MESH
    }
    
    public enum MeshQuality {
        LOW,      // 快速，低质量
        NORMAL,   // 平衡
        HIGH      // 慢速，高质量
    }
}
```

### KernelStats

```java
/**
 * Kernel统计信息
 */
public record KernelStats(
    long totalRequests,
    long successfulRequests,
    long failedRequests,
    double averageExecutionTimeMs,
    CacheStats cacheStats,
    Map<String, Long> failuresByStage
) {
    public double successRate() {
        return totalRequests == 0 ? 0.0 : 
            (double) successfulRequests / totalRequests;
    }
}
```

---

## 📦 实现结构

### 模块位置

新建 **aperture-kernel** 模块：

```
aperture-kernel/
├── build.gradle
└── src/
    ├── main/java/dev/aperture/kernel/
    │   ├── ApertureKernel.java           (接口)
    │   ├── ApertureKernelImpl.java       (实现)
    │   ├── KernelBuilder.java            (Builder)
    │   ├── OpeningRequest.java
    │   ├── OpeningResult.java
    │   ├── OpeningOptions.java
    │   ├── KernelStats.java
    │   ├── KernelException.java
    │   └── internal/
    │       ├── RequestValidator.java
    │       ├── ResultMapper.java
    │       └── StatsCollector.java
    └── test/java/dev/aperture/kernel/
        ├── ApertureKernelTest.java
        ├── KernelBuilderTest.java
        └── KernelIntegrationTest.java
```

### 依赖关系

```gradle
// aperture-kernel/build.gradle
dependencies {
    api project(':aperture-core')
    api project(':aperture-pipeline')
    api project(':aperture-parameter')
    api project(':aperture-geometry')
    
    testImplementation 'org.junit.jupiter:junit-jupiter:5.10.0'
    testImplementation 'org.mockito:mockito-core:5.5.0'
}
```

---

## 🔧 执行步骤

### Phase 1: 核心接口设计 (30分钟)

- [ ] 创建aperture-kernel模块
- [ ] 定义ApertureKernel接口
- [ ] 定义OpeningRequest
- [ ] 定义OpeningResult
- [ ] 定义OpeningOptions
- [ ] 定义KernelStats

### Phase 2: Kernel实现 (1.5小时)

- [ ] 实现ApertureKernelImpl
- [ ] 实现KernelBuilder
- [ ] 实现生成方法（generate, generateBatch）
- [ ] 实现Registry集成
- [ ] 实现Pipeline集成
- [ ] 实现统计收集

### Phase 3: 高级功能 (1小时)

- [ ] 实现异步生成（generateAsync）
- [ ] 实现部分生成（generateUntil）
- [ ] 实现批量优化
- [ ] 实现资源管理（close）

### Phase 4: 测试 (1小时)

- [ ] 单元测试（KernelBuilder, 各个方法）
- [ ] 集成测试（完整流程）
- [ ] 性能测试（批量、缓存）
- [ ] 并发测试（线程安全）

### Phase 5: 文档与示例 (1小时)

- [ ] API文档
- [ ] 使用示例
- [ ] 最佳实践指南
- [ ] 迁移指南（从OpeningPipelineAdapter）

---

## 📝 核心实现

### ApertureKernelImpl骨架

```java
public final class ApertureKernelImpl implements ApertureKernel {
    
    private final OpeningTypeRegistry registry;
    private final OpeningPipelineAdapter pipeline;
    private final ProfileCatalogRegistry profiles;
    private final StatsCollector statsCollector;
    private final ExecutorService executorService;
    private final boolean enableDebugLogging;
    
    ApertureKernelImpl(KernelConfig config) {
        this.registry = config.registry();
        this.pipeline = config.pipeline();
        this.profiles = config.profiles();
        this.statsCollector = new StatsCollector();
        this.executorService = config.executorService();
        this.enableDebugLogging = config.enableDebugLogging();
    }
    
    @Override
    public OpeningResult generate(OpeningRequest request) {
        request.validate();
        
        long startTime = System.currentTimeMillis();
        
        try {
            // 执行Pipeline
            PipelineResult pipelineResult = pipeline.execute(
                request.typeId(),
                request.parameters()
            );
            
            // 映射结果
            OpeningResult result = ResultMapper.map(request, pipelineResult);
            
            // 收集统计
            long duration = System.currentTimeMillis() - startTime;
            statsCollector.record(result, duration);
            
            return result;
            
        } catch (Exception e) {
            statsCollector.recordFailure(request.typeId(), "exception");
            return new OpeningResult.Failure(
                request.typeId(),
                "unknown",
                "Unexpected error: " + e.getMessage(),
                e
            );
        }
    }
    
    @Override
    public OpeningResult generate(String typeId, Map<String, Object> parameters) {
        return generate(new OpeningRequest(typeId, parameters));
    }
    
    @Override
    public List<OpeningResult> generateBatch(List<OpeningRequest> requests) {
        return requests.stream()
            .map(this::generate)
            .toList();
    }
    
    @Override
    public CompletableFuture<OpeningResult> generateAsync(OpeningRequest request) {
        return CompletableFuture.supplyAsync(
            () -> generate(request),
            executorService
        );
    }
    
    @Override
    public Optional<OpeningTypeDefinition> getDefinition(String typeId) {
        return registry.get(typeId);
    }
    
    @Override
    public Set<String> listTypes() {
        return registry.getAllIds();
    }
    
    @Override
    public void registerType(OpeningTypeDefinition definition) {
        registry.register(definition);
        pipeline.clearCache(); // 清空缓存，避免陈旧数据
    }
    
    @Override
    public void clearCache() {
        pipeline.clearCache();
    }
    
    @Override
    public KernelStats getStats() {
        return statsCollector.getStats();
    }
    
    @Override
    public void close() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
```

### KernelBuilder

```java
public final class KernelBuilder {
    private OpeningTypeRegistry registry;
    private ProfileCatalogRegistry profiles;
    private int cacheCapacity = 100;
    private boolean enableDebugLogging = false;
    private int asyncThreadPoolSize = 4;
    
    public KernelBuilder withRegistry(OpeningTypeRegistry registry) {
        this.registry = registry;
        return this;
    }
    
    public KernelBuilder withProfiles(ProfileCatalogRegistry profiles) {
        this.profiles = profiles;
        return this;
    }
    
    public KernelBuilder withCacheCapacity(int capacity) {
        this.cacheCapacity = capacity;
        return this;
    }
    
    public KernelBuilder enableDebugLogging() {
        this.enableDebugLogging = true;
        return this;
    }
    
    public KernelBuilder withAsyncThreadPoolSize(int size) {
        this.asyncThreadPoolSize = size;
        return this;
    }
    
    public ApertureKernel build() {
        // 使用默认值填充缺失的依赖
        if (registry == null) {
            registry = OpeningTypeRegistry.getInstance();
        }
        if (profiles == null) {
            profiles = ProfileCatalogRegistry.getDefault();
        }
        
        // 创建Pipeline
        OpeningPipelineAdapter pipeline = 
            OpeningPipelineAdapter.withCache(cacheCapacity);
        
        // 创建线程池
        ExecutorService executorService = 
            Executors.newFixedThreadPool(asyncThreadPoolSize);
        
        // 创建配置
        KernelConfig config = new KernelConfig(
            registry,
            pipeline,
            profiles,
            executorService,
            enableDebugLogging
        );
        
        return new ApertureKernelImpl(config);
    }
}
```

---

## 📊 使用示例

### 基本使用

```java
// 1. 创建Kernel
ApertureKernel kernel = ApertureKernel.builder()
    .withCacheCapacity(500)
    .build();

// 2. 生成Opening
OpeningResult result = kernel.generate(
    "aperture:door_standard",
    Map.of("width", 1.0, "height", 2.0)
);

// 3. 处理结果
if (result.isSuccess()) {
    var success = (OpeningResult.Success) result;
    PlacementInfo placement = success.placement();
    System.out.println("Generated: " + placement.dimensions());
} else {
    var failure = (OpeningResult.Failure) result;
    System.err.println("Failed: " + failure.errorMessage());
}

// 4. 关闭Kernel
kernel.close();
```

### try-with-resources

```java
try (ApertureKernel kernel = ApertureKernel.builder().build()) {
    
    OpeningResult result = kernel.generate("aperture:window_standard", params);
    
    if (result.isSuccess()) {
        processOpening(result);
    }
    
} // 自动关闭资源
```

### 批量生成

```java
List<OpeningRequest> batch = List.of(
    new OpeningRequest("aperture:door_standard", Map.of("width", 1.0)),
    new OpeningRequest("aperture:door_standard", Map.of("width", 1.5)),
    new OpeningRequest("aperture:window_standard", Map.of("width", 2.0))
);

List<OpeningResult> results = kernel.generateBatch(batch);

long successCount = results.stream()
    .filter(OpeningResult::isSuccess)
    .count();

System.out.println("Success rate: " + successCount + "/" + results.size());
```

### 异步生成

```java
CompletableFuture<OpeningResult> future = kernel.generateAsync(request);

future.thenAccept(result -> {
    if (result.isSuccess()) {
        System.out.println("Async generation completed");
    }
}).exceptionally(ex -> {
    System.err.println("Async generation failed: " + ex.getMessage());
    return null;
});
```

### 监控统计

```java
// 生成多个Opening后
KernelStats stats = kernel.getStats();

System.out.println("Total requests: " + stats.totalRequests());
System.out.println("Success rate: " + 
    String.format("%.1f%%", stats.successRate() * 100));
System.out.println("Avg execution time: " + 
    String.format("%.1fms", stats.averageExecutionTimeMs()));
System.out.println("Cache hit rate: " + 
    String.format("%.1f%%", stats.cacheStats().hitRate() * 100));
```

---

## ⏱️ 时间估算

| 阶段 | 预计时间 | 累计 |
|------|---------|------|
| Phase 1: 核心接口 | 30min | 30min |
| Phase 2: Kernel实现 | 1.5h | 2h |
| Phase 3: 高级功能 | 1h | 3h |
| Phase 4: 测试 | 1h | 4h |
| Phase 5: 文档 | 1h | 5h |
| **总计** | | **~5小时** |

---

## ✅ 验收标准

- [ ] aperture-kernel模块创建成功
- [ ] ApertureKernel接口定义完整
- [ ] 基本生成功能正常
- [ ] 批量生成功能正常
- [ ] 异步生成功能正常
- [ ] 统计收集正常
- [ ] 资源管理正常（AutoCloseable）
- [ ] 测试通过（单元+集成）
- [ ] 文档完整

---

## 📊 成功指标

### 功能指标
- ✅ 提供简洁的API
- ✅ 隐藏Pipeline细节
- ✅ 支持同步和异步
- ✅ 支持批量优化

### 性能指标
- 单次生成 < 1500ms（含缓存）
- 批量生成吞吐量 > 50 req/s
- 异步开销 < 10ms

### 代码质量
- API设计简洁直观
- 线程安全
- 资源管理正确
- 文档完善

---

## 🚀 开始执行

**下一步**: Phase 1 - 创建aperture-kernel模块和核心接口

准备开始！
