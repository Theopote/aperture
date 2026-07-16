# 统一 Pipeline 使用指南

Aperture 只保留一套生产调度框架：`aperture-pipeline` 中的八阶段 Pipeline，并由 `ApertureKernel` 对外提供生成入口。

旧的 `OpeningGenerationPipeline`、`OpeningGenerator` 和 `RectangularWindowGenerator` 已移除，不提供兼容入口。

## 生产调用

Runtime 和客户端必须通过 Kernel 调用：

```java
OpeningResult result = kernel.generate(new OpeningRequest(
    "aperture:door",
    Map.of("width", 1200.0)
));

PipelineResult output = result.asSuccess().output();
```

世界实例通过 `OpeningInstanceRequestMapper` 转换成 `OpeningRequest`。Runtime 不得自行解析参数、建立组件计划或生成几何。

## 阶段边界

```text
DefinitionStage  -> OpeningTypeDefinition
ParameterStage   -> ResolvedParameters
ConstraintStage  -> ValidatedParameters
ComponentStage   -> PlannedOpening(ComponentPlan)
GeometryStage    -> CompiledGeometry(recipe + solids)
MeshStage        -> MeshAssembly
CollisionStage   -> BoundingBox
PlacementStage   -> PlacementInfo
```

每个阶段只消费上一阶段的结果：

- `ComponentStage` 只建立一次 `ComponentPlan`。
- `GeometryStage` 必须消费已有 definition、parameters 和 plan。
- `MeshStage` 是唯一允许生成 `MeshAssembly` 的阶段。
- `ResultMapper` 从 Geometry、Mesh、Collision 和 Placement 阶段组装 `OpeningResult.Success`。

## 装配规则

`KernelBuilder` 是生产依赖的唯一装配入口。它创建并共享：

- `OpeningTypeRegistry`
- `ProfileCatalogRegistry`
- `ComponentPlanBuilder`
- `OpeningGeometryCompiler`
- `OpeningMeshCompiler`
- `PipelineCache`
- `ExecutorService`

Stage 不得加载 classpath 目录、创建默认注册表或构造另一条 Pipeline。

## 注册与缓存

`DefinitionStage` 和 `ApertureKernelImpl` 使用同一个 `OpeningTypeRegistry`，因此 `kernel.registerType()` 注册后会立即对生成管线可见。

缓存统计由 `Pipeline.cacheStats()` 返回真实的 `PipelineCache.CacheStats`，`OpeningPipelineAdapter` 只负责透传，不使用反射或占位数据。