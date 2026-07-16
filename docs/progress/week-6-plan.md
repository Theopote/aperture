# Week 6: Module Restructure Plan

**时间**: 2026-07-16  
**状态**: 🚧 进行中  
**目标**: 按照Kernel Contract重组模块结构，建立清晰的依赖关系

---

## 📋 任务概览

### P0 任务

1. **重命名模块**: `aperture-opening-geometry` → `aperture-types`
2. **拆分aperture-core**:
   - 创建 `aperture-parameter`
   - 创建 `aperture-component`
   - 创建 `aperture-pipeline`
   - 保留 `aperture-core` (数学工具)
3. **修复依赖链**: 确保依赖方向正确
4. **更新构建配置**: Gradle配置更新

---

## 🎯 目标架构

### 当前结构 (Week 5)

```
aperture/
├── aperture-opening-geometry/    ❌ 命名不当（不只是Opening几何）
├── aperture-core/                ❌ 职责混乱（包含parameter, component, pipeline）
├── aperture-fabric/
└── src/client/
```

### 目标结构 (Week 6)

```
aperture/
├── aperture-math/                ✅ 数学工具（Vec3, Matrix4, AABB）
├── aperture-types/               ✅ 基础类型（重命名自opening-geometry）
├── aperture-parameter/           ✅ 参数系统（从core拆分）
├── aperture-component/           ✅ 组件图（从core拆分）
├── aperture-geometry/            ✅ 几何计算（OCCT集成）
├── aperture-mesh/                ✅ 网格生成
├── aperture-pipeline/            ✅ 管线引擎（从core拆分）
├── aperture-opening/             ✅ Opening业务逻辑
├── aperture-fabric/              ✅ Minecraft平台集成
└── aperture-client/              ✅ 客户端渲染
```

### 依赖关系

```
aperture-math (数学基础)
    ↓
aperture-types (基础类型)
    ↓
aperture-parameter (参数系统)
    ↓
aperture-component (组件图) ──┐
    ↓                        │
aperture-geometry (几何) ────┤
    ↓                        │
aperture-mesh (网格) ────────┤
    ↓                        │
aperture-pipeline (管线) ◄───┘ (协调所有模块)
    ↓
aperture-opening (业务层)
    ↓
aperture-fabric (平台层)
    ↓
aperture-client (渲染层)
```

---

## 📦 模块职责定义

### aperture-math
**职责**: 纯数学工具
- Vec3, Vec2
- Matrix4, Matrix3
- AABB (轴对齐包围盒)
- Transform
- 数学函数（插值、距离等）

**依赖**: 无（纯Java）

### aperture-types
**职责**: Kernel基础类型（重命名自aperture-opening-geometry）
- Shape, Solid, Surface接口
- Profile（轮廓）
- Material（材质）
- 基础几何类型

**依赖**: aperture-math

### aperture-parameter
**职责**: 参数系统
- ParameterValue, ParameterSet
- ParameterDefinition
- Constraint, ConstraintValidator
- 表达式解析器

**依赖**: aperture-types

### aperture-component
**职责**: 组件图系统
- Component接口
- ComponentGraph
- Port系统
- 拓扑排序
- 增量求值

**依赖**: aperture-parameter, aperture-types

### aperture-geometry
**职责**: 几何计算（OCCT集成）
- 拉伸、旋转
- 布尔运算
- 几何查询
- OCCT JNI绑定

**依赖**: aperture-types, aperture-math

### aperture-mesh
**职责**: 网格生成
- Mesh, Vertex数据结构
- 网格生成算法
- 法向计算
- 网格优化
- 导出（OBJ, STL）

**依赖**: aperture-geometry, aperture-types

### aperture-pipeline
**职责**: 管线执行引擎
- PipelineStage接口
- Pipeline执行器
- 缓存管理
- 性能追踪

**依赖**: aperture-parameter, aperture-component, aperture-geometry, aperture-mesh

### aperture-opening
**职责**: Opening业务逻辑
- OpeningDefinition
- OpeningInstance
- Opening类型注册
- 业务规则

**依赖**: aperture-pipeline

### aperture-fabric
**职责**: Minecraft平台集成
- Block, BlockEntity
- NBT序列化
- 世界交互

**依赖**: aperture-opening, net.minecraft

### aperture-client
**职责**: 客户端渲染
- 渲染器
- 参数编辑器UI
- 预览管理

**依赖**: aperture-fabric, net.minecraft.client

---

## 🔧 执行步骤

### 阶段1: 准备工作 (30分钟)

- [x] 创建Week 6计划文档
- [ ] 检查当前模块结构
- [ ] 识别需要移动的文件
- [ ] 备份当前代码（Git commit）

### 阶段2: 创建新模块 (1小时)

- [ ] 创建 `aperture-math/` 目录和build.gradle
- [ ] 创建 `aperture-parameter/` 目录和build.gradle
- [ ] 创建 `aperture-component/` 目录和build.gradle
- [ ] 创建 `aperture-pipeline/` 目录和build.gradle

### 阶段3: 重命名模块 (30分钟)

- [ ] 重命名 `aperture-opening-geometry` → `aperture-types`
- [ ] 更新所有import语句
- [ ] 更新settings.gradle

### 阶段4: 移动代码 (2小时)

- [ ] 从aperture-core移动数学类到aperture-math
- [ ] 从aperture-core移动参数类到aperture-parameter
- [ ] 从aperture-core移动组件类到aperture-component
- [ ] 从aperture-core移动管线类到aperture-pipeline

### 阶段5: 修复依赖 (1小时)

- [ ] 更新各模块的build.gradle依赖
- [ ] 修复import语句
- [ ] 解决循环依赖

### 阶段6: 验证构建 (30分钟)

- [ ] 运行 `./gradlew build`
- [ ] 运行测试
- [ ] 修复编译错误

### 阶段7: 更新文档 (30分钟)

- [ ] 更新README.md
- [ ] 更新架构文档
- [ ] 创建迁移指南

---

## ⚠️ 风险和注意事项

### 高风险操作

1. **重命名模块**: 会破坏所有import
2. **拆分aperture-core**: 可能引入循环依赖
3. **移动文件**: Git历史可能丢失

### 缓解策略

1. **Git操作**: 使用 `git mv` 保留历史
2. **逐步提交**: 每个阶段单独commit
3. **测试验证**: 每步之后运行测试
4. **IDE重构**: 使用IDE的重构功能而非手动

### 回滚计划

如果出现无法解决的问题：
1. `git reset --hard <commit>` 回滚到重构前
2. 重新评估策略
3. 分更小步骤执行

---

## 📊 进度追踪

| 阶段 | 预计时间 | 实际时间 | 状态 |
|------|---------|---------|------|
| 准备工作 | 30min | - | 🚧 |
| 创建新模块 | 1h | - | ⏳ |
| 重命名模块 | 30min | - | ⏳ |
| 移动代码 | 2h | - | ⏳ |
| 修复依赖 | 1h | - | ⏳ |
| 验证构建 | 30min | - | ⏳ |
| 更新文档 | 30min | - | ⏳ |

**总计**: 约6小时

---

## ✅ 验收标准

- [ ] 所有模块按Contract定义独立
- [ ] 依赖关系符合依赖图
- [ ] 无循环依赖
- [ ] `./gradlew build` 成功
- [ ] 所有测试通过
- [ ] 文档更新完成

---

## 📝 变更日志

| 日期 | 变更 | 负责人 |
|------|------|--------|
| 2026-07-16 | 创建Week 6计划 | Claude |

---

**下一步**: 检查当前模块结构，开始阶段1
