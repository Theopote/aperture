# Week 7: Unified Pipeline Plan

**时间**: 2026-07-16  
**状态**: 🚧 进行中  
**目标**: 实现统一的Pipeline系统，建立8阶段标准化数据流

---

## 🎯 核心目标

### 实现统一的PipelineStage接口

根据[Pipeline Contract](../kernel-contracts/pipeline.md)，创建标准化的Pipeline系统：

1. **定义PipelineStage<I,O>接口** - 统一的阶段抽象
2. **实现8个标准Stage** - Definition → Parameter → Constraint → Component → Geometry → Mesh → Collision → Placement
3. **实现Pipeline执行引擎** - 顺序执行、短路、缓存
4. **实现StageContext** - 阶段间数据传递
5. **实现StageResult** - 统一的结果类型

---

## 📋 8个标准Stage

### 1. DefinitionStage
**输入**: OpeningDefinition  
**输出**: ResolvedDefinition  
**职责**: 查找Opening类型定义

### 2. ParameterStage
**输入**: ResolvedDefinition  
**输出**: ParameterSet  
**职责**: 解析参数，应用默认值和用户覆盖

### 3. ConstraintStage
**输入**: ParameterSet  
**输出**: ValidatedParameters  
**职责**: 验证约束，确保参数合法

### 4. ComponentStage
**输入**: ValidatedParameters  
**输出**: ComponentGraph  
**职责**: 构建组件图，绑定参数

### 5. GeometryStage
**输入**: ComponentGraph  
**输出**: Shape  
**职责**: 求值组件图，生成几何体

### 6. MeshStage
**输入**: Shape  
**输出**: Mesh  
**职责**: 将几何体转换为三角网格

### 7. CollisionStage
**输入**: Mesh  
**输出**: CollisionShape  
**职责**: 生成碰撞形状

### 8. PlacementStage
**输入**: CollisionShape  
**输出**: PlacementInfo  
**职责**: 计算放置信息（footprint、anchor等）

---

## 🏗️ 架构设计

### 核心接口

```java
// PipelineStage.java
public interface PipelineStage<I, O> {
    /**
     * 执行当前阶段
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

// StageResult.java
public sealed interface StageResult<T> {
    record Success<T>(T value) implements StageResult<T> {}
    record Failure<T>(String message, Throwable cause) implements StageResult<T> {}
    record Skipped<T>(String reason, T cachedValue) implements StageResult<T> {}
    
    boolean isSuccess();
    T getValue();
}

// StageContext.java
public class StageContext {
    private final Map<String, Object> metadata;
    private final ExecutionOptions options;
    
    public <T> Optional<T> getMetadata(String key);
    public void putMetadata(String key, Object value);
    public ExecutionOptions options();
}

// PipelineResult.java
public sealed interface PipelineResult {
    record Success(
        Map<String, StageOutput> stageOutputs,
        PipelineMetrics metrics
    ) implements PipelineResult {}
    
    record Failure(
        String failedStage,
        String errorMessage,
        Throwable cause,
        Map<String, StageOutput> partialOutputs
    ) implements PipelineResult {}
    
    boolean isSuccess();
}
```

### Pipeline执行引擎

```java
public class Pipeline {
    private final List<StageRegistration> stages;
    private final ExecutionOptions options;
    private final PipelineCache cache;
    
    public PipelineResult execute(OpeningDefinition definition) {
        StageContext ctx = new StageContext(options);
        Map<String, StageOutput> outputs = new HashMap<>();
        
        Object currentInput = definition;
        
        for (StageRegistration registration : stages) {
            String stageName = registration.name();
            PipelineStage stage = registration.stage();
            
            // 检查缓存
            Optional<Object> cached = cache.get(stageName, currentInput);
            if (cached.isPresent() && options.enableCache()) {
                outputs.put(stageName, new StageOutput(stageName, cached.get(), 0, true));
                currentInput = cached.get();
                continue;
            }
            
            // 执行Stage
            long startTime = System.currentTimeMillis();
            StageResult<?> result = stage.execute(currentInput, ctx);
            long duration = System.currentTimeMillis() - startTime;
            
            if (!result.isSuccess()) {
                // 短路返回
                return new PipelineResult.Failure(
                    stageName,
                    "Stage failed: " + stageName,
                    result instanceof StageResult.Failure f ? f.cause() : null,
                    outputs
                );
            }
            
            Object stageOutput = result.getValue();
            outputs.put(stageName, new StageOutput(stageName, stageOutput, duration, false));
            
            // 更新缓存
            if (options.enableCache()) {
                cache.put(stageName, currentInput, stageOutput);
            }
            
            currentInput = stageOutput;
        }
        
        return new PipelineResult.Success(outputs, buildMetrics(outputs));
    }
}
```

---

## 📦 模块规划

### 创建aperture-pipeline模块

**目录结构**:
```
aperture-pipeline/
├── build.gradle
└── src/main/java/dev/aperture/pipeline/
    ├── PipelineStage.java
    ├── StageResult.java
    ├── StageContext.java
    ├── Pipeline.java
    ├── PipelineBuilder.java
    ├── PipelineResult.java
    ├── PipelineCache.java
    ├── stage/
    │   ├── DefinitionStage.java
    │   ├── ParameterStage.java
    │   ├── ConstraintStage.java
    │   ├── ComponentStage.java
    │   ├── GeometryStage.java
    │   ├── MeshStage.java
    │   ├── CollisionStage.java
    │   └── PlacementStage.java
    └── metrics/
        ├── PipelineMetrics.java
        └── StageOutput.java
```

**依赖关系**:
```gradle
dependencies {
    api project(':aperture-parameter')
    api project(':aperture-component')  // 待创建
    api project(':aperture-geometry')
    api project(':aperture-mesh')      // 待确认
    api project(':aperture-core')      // 用于Opening相关类型
}
```

---

## 🔧 执行步骤

### Phase 1: 创建核心接口 (30分钟)

- [ ] 创建aperture-pipeline模块
- [ ] 编写build.gradle
- [ ] 创建PipelineStage接口
- [ ] 创建StageResult接口
- [ ] 创建StageContext类
- [ ] 创建PipelineResult接口

### Phase 2: 实现Pipeline引擎 (1小时)

- [ ] 创建Pipeline类
- [ ] 实现execute()方法
- [ ] 实现PipelineBuilder
- [ ] 实现PipelineCache
- [ ] 实现PipelineMetrics

### Phase 3: 实现8个Stage (2小时)

- [ ] DefinitionStage
- [ ] ParameterStage
- [ ] ConstraintStage
- [ ] ComponentStage
- [ ] GeometryStage
- [ ] MeshStage
- [ ] CollisionStage
- [ ] PlacementStage

### Phase 4: 集成测试 (1小时)

- [ ] 创建测试用例
- [ ] 验证完整Pipeline流程
- [ ] 验证缓存功能
- [ ] 性能测试

### Phase 5: 更新现有代码 (1小时)

- [ ] 重构aperture-opening使用新Pipeline
- [ ] 更新aperture-fabric集成
- [ ] 更新文档

---

## ⏱️ 时间估算

| 阶段 | 预计时间 | 累计 |
|------|---------|------|
| Phase 1: 核心接口 | 30min | 30min |
| Phase 2: Pipeline引擎 | 1h | 1h30min |
| Phase 3: 8个Stage | 2h | 3h30min |
| Phase 4: 集成测试 | 1h | 4h30min |
| Phase 5: 更新代码 | 1h | 5h30min |
| **总计** | | **~6小时** |

---

## ✅ 验收标准

- [ ] aperture-pipeline模块创建成功
- [ ] PipelineStage接口定义完整
- [ ] Pipeline执行引擎工作正常
- [ ] 8个Stage全部实现
- [ ] 缓存功能正常
- [ ] 短路执行正常
- [ ] 性能指标收集正常
- [ ] 测试通过
- [ ] 符合Pipeline Contract

---

## 📊 成功指标

### 功能指标
- ✅ 完整Pipeline流程可运行
- ✅ 每个Stage独立可测试
- ✅ 缓存命中率 > 90%
- ✅ 错误处理完善

### 性能指标
- 完整Pipeline（冷启动）< 100ms
- 完整Pipeline（缓存命中）< 1ms
- 单Stage执行 < 20ms（平均）

### 代码质量
- 符合Pipeline Contract
- 接口清晰
- 易于扩展
- 文档完善

---

## 🚀 开始执行

**下一步**: Phase 1 - 创建aperture-pipeline模块和核心接口

准备开始！
