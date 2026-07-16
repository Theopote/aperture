# 架构审查反馈分析与改进计划

**日期**: 2026-07-16  
**审查者**: 外部架构师  
**当前状态**: Week 4完成，Alpha可用

---

## 执行摘要

外部架构审查指出了三个核心问题和一个战略方向转变。这些反馈**高度合理且关键**，需要在继续功能开发前解决。建议暂停功能扩展，用Week 5-6进行**架构重构**，建立坚实的Kernel基础。

**核心判断**: 
- ✅ 反馈准确指出了当前架构的根本问题
- ✅ 建议的改进方向与Aperture长期愿景一致
- ⚠️ 需要立即行动，否则技术债会快速累积

---

## 问题一分析: Geometry与Opening耦合过紧

### 问题描述

**现状**:
```
aperture-opening-geometry
    ↓
既包含Opening逻辑，又包含Geometry生成
```

**问题**:
- `aperture-opening-geometry` 模块名暴露了耦合
- Opening是Geometry的**消费者**，不应该是Geometry的一部分
- 未来Roof、Stair、Furniture都需要`*-geometry`模块？

### 审查者建议

```
aperture-geometry (纯几何内核)
    ↓
aperture-opening (Opening抽象层)
    ↓
aperture-door (Door具体实现)
    ↓
aperture-window (Window具体实现)
```

**原则**: Geometry永远不知道Opening的存在

### 评估: ✅ 完全正确

**当前问题确认**:

1. **模块命名暴露耦合**
   ```
   aperture-opening-geometry/
       ├── generator/
       │   └── RectangularWindowGenerator.java  ← Opening逻辑
       └── operations/
           └── ProfileExtrusion.java             ← Geometry逻辑
   ```
   这两个职责不应该在同一个模块。

2. **未来扩展困难**
   - 添加Roof: 需要 `aperture-roof-geometry`?
   - 添加Stair: 需要 `aperture-stair-geometry`?
   - 不可持续

3. **违反关注点分离**
   - Geometry应该是通用工具箱
   - Opening只是使用这些工具

### 改进方案

#### 阶段1: 模块重构 (Week 5)

**重命名和拆分**:
```
aperture-geometry/
    ├── kernel/          ← 几何内核 (纯算法)
    ├── operations/      ← 几何操作 (extrude, boolean, etc)
    └── mesh/            ← Mesh生成

aperture-opening/
    ├── definition/      ← Opening定义系统
    ├── pipeline/        ← Pipeline框架
    └── generators/      ← 抽象Generator接口

aperture-types/
    ├── door/            ← Door实现
    ├── window/          ← Window实现
    └── common/          ← 共用组件
```

**依赖关系**:
```
aperture-geometry (不依赖任何业务逻辑)
    ↓
aperture-opening (依赖geometry，提供Opening框架)
    ↓
aperture-types (依赖opening，实现具体类型)
```

#### 阶段2: 代码迁移

**移动内容**:

1. `RectangularWindowGenerator` → `aperture-types/window/`
2. `DoorGenerator` → `aperture-types/door/`
3. `ProfileExtrusion` 保持在 `aperture-geometry/operations/`

**新的Generator接口**:
```java
// aperture-opening/src/main/java/dev/aperture/opening/generator/Generator.java
public interface Generator {
    PipelineResult generate(GenerationContext context);
}

// aperture-types/window/src/main/java/.../RectangularWindowGenerator.java
public class RectangularWindowGenerator implements Generator {
    // 只使用aperture-geometry的API
    // 不包含Geometry实现
}
```

---

## 问题二分析: Pipeline还未成为真正中心

### 问题描述

**审查者观察**:
当前数据流仍然存在跳跃:
```
Definition → Geometry → Render
```

跳过了中间的关键阶段。

**理想数据流**:
```
Definition
    ↓
Parameters
    ↓
Resolved Parameters
    ↓
Component Graph
    ↓
Geometry Graph
    ↓
Mesh
    ↓
Renderable
    ↓
Minecraft
```

### 评估: ✅ 部分正确，需要澄清

**现状检查**:

我们**已经实现了8阶段Pipeline**:
```
Definition → Parameter → Constraint → Component
    ↓           ↓           ↓           ↓
Geometry → Mesh → Collision → Placement → Render
```

参见: `docs/architecture/kernel/04-generation-pipeline.md`

**但确实存在问题**:

1. **Stage接口不统一**
   - 每个Stage有不同的输入/输出类型
   - 没有统一的`Stage<I, O>`接口

2. **跳过Pipeline的捷径存在**
   ```java
   // 不好: 直接生成mesh
   Mesh mesh = ShapeMesher.mesh(shape);
   
   // 应该: 通过Pipeline
   PipelineResult result = pipeline.execute(definition, params);
   ```

3. **Stage之间缺乏契约**
   - 输入验证不完整
   - 错误处理不一致
   - 缓存策略分散

### 改进方案

#### 统一Stage接口

```java
/**
 * A single stage in the generation pipeline.
 * 
 * @param <I> Input type
 * @param <O> Output type
 */
public interface PipelineStage<I, O> {
    /**
     * Stage identifier (unique within pipeline).
     */
    String name();
    
    /**
     * Execute this stage.
     */
    StageResult<O> execute(StageContext<I> context);
    
    /**
     * Validate input before execution.
     */
    ValidationResult validate(I input);
    
    /**
     * Can this stage result be cached?
     */
    default boolean isCacheable() {
        return true;
    }
}

/**
 * Context passed to each stage.
 */
public record StageContext<I>(
    I input,
    Map<String, Object> sharedState,
    ProfileCatalogRegistry profiles,
    MaterialCatalogRegistry materials
) {}

/**
 * Result from stage execution.
 */
public sealed interface StageResult<O> {
    record Success<O>(O output) implements StageResult<O> {}
    record Failure<O>(String error, Throwable cause) implements StageResult<O> {}
}
```

#### 完整Pipeline实现

```java
public class GenerationPipeline {
    private final List<PipelineStage<?, ?>> stages;
    
    public PipelineResult execute(OpeningTypeDefinition definition, ParameterSet params) {
        var context = new ExecutionContext();
        
        // 1. Parameter Stage
        var paramResult = parameterStage.execute(
            new StageContext<>(definition, context.state, profiles, materials)
        );
        
        // 2. Constraint Stage
        var constraintResult = constraintStage.execute(
            new StageContext<>(paramResult.output(), context.state, profiles, materials)
        );
        
        // ... 依次执行所有stage
        
        return finalResult;
    }
}
```

#### 禁止绕过Pipeline

```java
// aperture-geometry: 只提供工具
public class ShapeMesher {
    // OK: 工具方法
    public static Mesh mesh(Shape shape) { ... }
}

// aperture-opening: 强制使用Pipeline
public class OpeningService {
    private final GenerationPipeline pipeline;
    
    public PipelineResult generate(OpeningInstance instance) {
        // 必须通过pipeline
        return pipeline.execute(instance.definition(), instance.parameters());
    }
    
    // 禁止: 直接访问ShapeMesher
}
```

---

## 问题三分析: Kernel职责不够纯

### 问题描述

**审查者观察**:
Parameter、Geometry、Runtime、Editor之间开始出现互相引用。

**理想依赖链**:
```
Math
  ↓
Geometry
  ↓
Kernel
  ↓
Runtime
  ↓
Editor
  ↓
Fabric
```

永远不能反向依赖。

### 评估: ✅ 高度正确

**当前问题确认**:

检查依赖图:
```bash
# aperture-core 的依赖
aperture-core/build.gradle:
    dependencies {
        api project(':aperture-math')
        api project(':aperture-geometry')  ← 问题！
    }
```

**问题**: `aperture-core` 依赖 `aperture-geometry`

**应该是**:
```
aperture-geometry 依赖 aperture-core
而不是反过来
```

**为什么这是问题**:
1. Core应该是最基础的定义（Definition, Parameter, Instance）
2. Geometry是使用这些定义的工具
3. 当前反向依赖导致循环

### 改进方案

#### 重新定义依赖链

```
aperture-math          (数学基础)
    ↓
aperture-core          (核心定义: Definition, Parameter, Instance)
    ↓
aperture-geometry      (几何工具: Shape, Mesh, Operations)
    ↓
aperture-opening       (Opening框架: Pipeline, Generator)
    ↓
aperture-types         (具体类型: Door, Window)
    ↓
aperture-fabric        (Minecraft集成)
    ↓
aperture-client        (客户端渲染和编辑器)
```

#### 拆分aperture-core

**问题**: 当前`aperture-core`太大，包含了不应该在一起的东西

**解决**: 拆分为更细的模块
```
aperture-definition/   (Opening定义系统)
    - OpeningTypeDefinition
    - ComponentDefinition
    - ConstraintDefinition

aperture-parameter/    (参数系统)
    - ParameterSet
    - ParameterValue
    - ParameterResolver

aperture-instance/     (实例管理)
    - OpeningInstance
    - OpeningState
    - Transform

aperture-core/         (核心契约和接口)
    - 依赖上述三个模块
    - 提供统一入口
```

---

## 核心建议分析: Kernel Contract

### 问题描述

**审查者核心观点**:
> 真正缺少的不是代码，而是Kernel Contract

**什么是Kernel Contract**:
每个模块明确定义:
- 允许做什么
- 禁止做什么
- 输入/输出
- 生命周期
- 禁止依赖

### 评估: ✅ 极其关键

这是**最重要的反馈**。

**当前问题**:
1. 没有明确的模块边界契约
2. 开发者不清楚"可以"和"不可以"
3. 代码风格开始"漂移"

**示例**: Geometry模块应该允许什么？

**当前状态**: 隐式的，没有文档
**应该有**: 明确的契约文档

### 改进方案

#### 创建Kernel Contracts目录

```
docs/kernel-contracts/
├── README.md                  (Contract总览)
├── geometry.md               (Geometry契约)
├── parameter.md              (Parameter契约)
├── mesh.md                   (Mesh契约)
├── component.md              (Component契约)
├── pipeline.md               (Pipeline契约)
└── renderer.md               (Renderer契约)
```

#### Geometry Contract示例

```markdown
# Geometry Module Contract

## Responsibilities
- Provide geometric primitives (Point, Vector, Curve, Surface)
- Implement geometric operations (extrude, revolve, boolean)
- Generate meshes from geometric shapes

## Allowed Dependencies
- aperture-math (Vec3d, Transform, etc)
- aperture-definition (only for type references)

## FORBIDDEN Dependencies
- ❌ net.minecraft.*
- ❌ aperture-fabric
- ❌ aperture-opening
- ❌ Any rendering code

## Input Types
- GeometrySpec (abstract geometry definition)
- Parameters (size, count, etc)
- ProfileCurve (2D curves)

## Output Types
- Shape (geometric shapes)
- Mesh (triangulated meshes)
- GeometryResult (complete geometry)

## Lifecycle
1. Stateless operations (pure functions preferred)
2. Thread-safe (no mutable shared state)
3. Deterministic (same input → same output)

## Error Handling
- Throw IllegalArgumentException for invalid input
- Throw GeometryException for operation failures
- Never return null (use Optional)

## Performance Requirements
- Single operation: < 10ms
- Batch operations: use caching
- Memory: avoid large temporary allocations
```

---

## 核心建议分析: Kernel API (Service/Factory模式)

### 问题描述

**审查者建议**:
不要直接 `new Mesh`，而是通过:
```java
GeometryService
GeometryFactory
GeometryBuilder
```

**目的**:
- 统一接口
- 方便替换实现
- 支持多平台 (NodeCraft, FormaCraft, Web Editor)

### 评估: ✅ 正确但需要平衡

**优点**:
1. 统一API入口
2. 依赖注入友好
3. 易于测试和mock
4. 支持多实现

**担忧**:
1. 过度抽象会增加复杂度
2. 简单的几何操作不需要Factory

### 改进方案: 混合策略

#### 对外API使用Service模式

```java
// 对外暴露的主要API
public interface GeometryService {
    Shape createShape(ShapeSpec spec);
    Mesh generateMesh(Shape shape, MeshOptions options);
    Shape performBoolean(Shape a, Shape b, BooleanOp op);
}

// 默认实现
public class DefaultGeometryService implements GeometryService {
    private final ShapeFactory shapeFactory;
    private final MeshGenerator meshGenerator;
    
    @Override
    public Shape createShape(ShapeSpec spec) {
        return shapeFactory.create(spec);
    }
    
    // ...
}
```

#### 内部实现保持简洁

```java
// 内部可以直接new (不暴露给外部)
class ShapeFactory {
    Shape create(ShapeSpec spec) {
        return new ConcreteShape(spec);  // OK内部使用
    }
}
```

#### API Gateway模式

```java
// 单一入口点
public class ApertureGeometry {
    private static GeometryService instance;
    
    public static GeometryService service() {
        return instance;
    }
    
    public static void initialize(GeometryService service) {
        instance = service;
    }
}

// 使用
Shape shape = ApertureGeometry.service().createShape(spec);
```

---

## 战略转变: 从"对象"到"过程"

### 审查者核心观点

> Aperture 更适合围绕**过程（Process）**组织，而不是**对象（Object）**

**理想模型**:
```
Resolve Parameters
      ↓
Validate Constraints
      ↓
Build Component Graph
      ↓
Generate Geometry
      ↓
Generate Mesh
      ↓
Create Runtime Object
      ↓
Render
```

每个阶段:
- 明确的输入/输出
- 不可变数据传递
- 类似编译器或渲染管线

### 评估: ✅ 与我们的Pipeline愿景完全一致

**这正是我们想要的**！

**当前状态**: 已经有Pipeline概念，但执行不够严格

**需要加强**:
1. 每个Stage独立
2. Stage之间只通过不可变数据传递
3. 禁止跳过Pipeline直接生成

**这与审查者的"Kernel Pipeline Milestone"建议完全一致**

---

## 建议的六周计划评估

### Week 1: Kernel Contracts ✅ 强烈同意

**任务**:
- 编写每个模块的Contract文档
- 定义职责、生命周期、禁止事项

**优先级**: 🔴 最高

### Week 2: Kernel API ✅ 同意

**任务**:
- 建立Service/Factory/Builder API
- 统一入口点

**优先级**: 🔴 高

### Week 3: Pipeline Engine ✅ 强烈同意

**任务**:
- 统一Stage接口
- StageContext和StageResult
- 严格的Pipeline执行

**优先级**: 🔴 最高

**注意**: 先不添加Door，专注Pipeline本身

### Week 4: Incremental Update ⚠️ 推迟

**原因**:
- 这是优化，不是架构基础
- 应该在Pipeline稳定后再做
- 建议推迟到Month 2

### Week 5: Command System ✅ 同意

**已有设计**: `docs/architecture/editor/03-command-history.md`

**需要**: 实现并集成到Pipeline

### Week 6: Procedural Door ✅ 强烈同意

**验收标准**:
1. Door必须通过Pipeline生成
2. Door不包含Mesh生成逻辑
3. 如果绕过Pipeline = 架构失败

---

## Kernel Pipeline Milestone

### 定义

**目标**: 建立坚实的Kernel基础

**验收标准**:
1. ✅ 任何构件必须通过统一Pipeline生成
2. ✅ 每个Stage有明确契约
3. ✅ Door只提供参数和定义，不包含生成逻辑

### 评估: ✅ 这是正确的里程碑

**当前问题**: 我们已经在做功能，但基础不够坚实

**正确做法**: 暂停功能扩展，先建立Kernel

**时机**: **现在**是重构的最佳时机
- 代码量还不大
- 架构问题已暴露
- 用户还没有依赖旧API

---

## 推荐的行动计划

### 立即暂停

❌ **暂停以下工作**:
- 新Opening类型 (Casement Window, Skylight)
- 编辑器UI改进
- 材质系统
- 多Opening类型测试

### Week 5: Kernel Contract (2026-07-23)

**P0 任务**:
1. 创建 `docs/kernel-contracts/` 目录
2. 编写6个Contract文档:
   - geometry.md
   - parameter.md
   - mesh.md
   - component.md
   - pipeline.md
   - renderer.md
3. 每个Contract包含:
   - Responsibilities
   - Allowed/Forbidden dependencies
   - Input/Output types
   - Lifecycle rules
   - Error handling policy

**交付物**: 完整的Contract文档集

### Week 6: Module Restructure (2026-07-30)

**P0 任务**:
1. 重命名模块:
   - `aperture-opening-geometry` → `aperture-types`
2. 拆分模块:
   - `aperture-core` → `aperture-definition`, `aperture-parameter`, `aperture-instance`
3. 修正依赖链:
   - 确保单向依赖
   - 移除所有反向依赖
4. 更新所有import

**交付物**: 清晰的模块结构

### Week 7: Unified Pipeline (2026-08-06)

**P0 任务**:
1. 实现 `PipelineStage<I, O>` 接口
2. 实现 `StageContext` 和 `StageResult`
3. 重构现有8个Stage为统一接口
4. 实现Pipeline执行引擎
5. 禁止绕过Pipeline的直接调用

**交付物**: 统一的Pipeline系统

### Week 8: Kernel API (2026-08-13)

**P0 任务**:
1. 设计 Service 接口:
   - `GeometryService`
   - `ParameterService`
   - `ComponentService`
2. 实现 Factory 和 Builder
3. 创建统一入口 `ApertureKernel`
4. 重构现有代码使用新API

**交付物**: 统一的Kernel API

### Week 9: Validate with Door (2026-08-20)

**P0 任务**:
1. 使用新Pipeline重写Door生成
2. 确保Door只提供:
   - `door.json` 定义
   - 参数配置
   - 组件描述
3. 所有Mesh生成通过Pipeline
4. 验证不能绕过Pipeline

**验收**: Door成为第一个完全符合新架构的Opening

### Week 10: Documentation & Testing (2026-08-27)

**P0 任务**:
1. 更新所有架构文档
2. 编写Kernel API使用指南
3. 创建示例项目
4. 完整测试覆盖

**交付物**: 完整的Kernel v1.0

---

## 风险评估

### 风险1: 重构时间超出预期

**可能性**: 中等  
**影响**: 高  
**缓解**: 
- 严格控制范围
- 优先P0任务
- 每周review进度

### 风险2: 破坏现有功能

**可能性**: 高  
**影响**: 中等  
**缓解**:
- 保持测试通过
- 渐进式重构
- 保留旧API兼容层（临时）

### 风险3: 过度抽象

**可能性**: 中等  
**影响**: 中等  
**缓解**:
- Contract先行，防止随意抽象
- 保持YAGNI原则
- 每周架构review

---

## 结论

### 核心判断

**审查反馈的价值**: ⭐⭐⭐⭐⭐ (5/5)

这是**极其宝贵的反馈**，准确指出了架构的根本问题。

### 推荐决策

✅ **接受审查建议**  
✅ **暂停功能开发**  
✅ **投入5-6周进行架构重构**  
✅ **以Kernel Pipeline Milestone为目标**

### 理由

1. **现在是最佳重构时机**
   - 代码量还不大 (~10k lines)
   - 架构问题已暴露
   - 没有外部用户依赖

2. **不重构的后果**
   - 技术债快速累积
   - 每个新功能都在错误的基础上
   - 未来重构成本10倍增长

3. **重构的收益**
   - 坚实的Kernel基础
   - 清晰的模块边界
   - 易于扩展 (NodeCraft, FormaCraft)
   - 长期可维护

### 下一步

立即执行Week 5计划：**Kernel Contract文档编写**

---

**Created**: 2026-07-16  
**Status**: 建议接受  
**Decision**: 等待用户确认
