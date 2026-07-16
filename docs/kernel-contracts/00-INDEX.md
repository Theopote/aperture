# Kernel Contracts Index

**Last Updated**: 2026-07-16

本目录包含Aperture Kernel各模块的Contract文档，定义了清晰的模块边界和职责。

---

## 📋 Contract列表

| Contract | 状态 | 描述 |
|----------|------|------|
| [README.md](README.md) | ✅ 完成 | Contract系统总览和模板 |
| [geometry.md](geometry.md) | ✅ 完成 | Geometry模块：几何计算、拉伸、布尔运算 |
| [parameter.md](parameter.md) | ✅ 完成 | Parameter模块：参数定义、解析、约束验证 |
| [mesh.md](mesh.md) | ✅ 完成 | Mesh模块：三角网格生成、优化、导出 |
| [component.md](component.md) | ✅ 完成 | Component模块：组件图、拓扑求值、数据流 |
| [pipeline.md](pipeline.md) | ✅ 完成 | Pipeline模块：8阶段管线、缓存、执行引擎 |
| [renderer.md](renderer.md) | ✅ 完成 | Renderer模块：渲染适配、GPU资源管理 |

---

## 🎯 核心原则

所有Contract遵循以下原则：

1. **单一职责**: 每个模块专注一个核心职责
2. **依赖方向**: 依赖必须单向，不能形成环
3. **平台独立**: Kernel模块不依赖Minecraft
4. **不可变性**: 优先使用不可变数据结构
5. **错误透明**: 明确的错误处理策略

---

## 📊 依赖关系图

```
aperture-parameter ─────────┐
                           │
                           ▼
aperture-component ──> aperture-geometry ──> aperture-mesh ──> aperture-renderer
                           │                      │
                           │                      │
                           └──> aperture-pipeline ◄┘
                                      │
                                      ▼
                              aperture-opening (业务层)
                                      │
                                      ▼
                              aperture-fabric (平台层)
```

**依赖规则**:
- ✅ 向右依赖（数据流方向）
- ✅ 向下依赖（抽象→具体）
- ❌ 反向依赖（违反依赖倒置）
- ❌ Opening/Platform → Kernel（业务不能反向依赖）

---

## 🚫 The Iron Law

**Kernel模块禁止依赖**:

```java
// ❌ 禁止
import net.minecraft.*;
import aperture.fabric.*;
import aperture.client.*;
import aperture.opening.*;  // (除非明确允许)
```

**原因**: Kernel是平台无关的几何引擎，必须可以独立运行（测试、导出、其他平台移植）。

---

## 📖 如何阅读Contract

每个Contract文档包含以下部分：

1. **Overview**: 模块核心职责一句话总结
2. **Responsibilities**: 允许做的事（✅）
3. **Forbidden**: 禁止做的事（❌）
4. **Dependencies**: 允许/禁止的依赖
5. **Input/Output Types**: 接口定义
6. **Lifecycle**: 对象创建和状态管理
7. **Error Handling**: 异常类型和处理策略
8. **Performance**: 时间复杂度和优化目标
9. **Examples**: 正确/错误用法对比
10. **Migration Guide**: 违规代码如何重构

---

## 🔍 快速查找

### 按职责查找

- **数据定义**: parameter.md
- **几何计算**: geometry.md
- **网格生成**: mesh.md
- **数据流编排**: component.md
- **管线执行**: pipeline.md
- **渲染输出**: renderer.md

### 按依赖查找

- **不依赖任何Kernel模块**: parameter.md, geometry.md
- **依赖Geometry**: mesh.md, component.md
- **依赖所有模块**: pipeline.md
- **依赖Mesh**: renderer.md

### 按阶段查找

Pipeline 8阶段对应的模块：

1. **Definition** → parameter.md (解析定义)
2. **Parameter** → parameter.md (参数解析)
3. **Constraint** → parameter.md (约束验证)
4. **Component** → component.md (组件图)
5. **Geometry** → geometry.md (几何计算)
6. **Mesh** → mesh.md (网格生成)
7. **Collision** → geometry.md (碰撞形状)
8. **Placement** → geometry.md (放置信息)

Render → renderer.md (渲染适配)

---

## 📈 Week 5进度

**目标**: 完成6个核心Contract文档

**完成情况**:
- [x] README.md (Contract系统总览)
- [x] geometry.md (2026-07-16)
- [x] parameter.md (2026-07-16)
- [x] mesh.md (2026-07-16)
- [x] component.md (2026-07-16)
- [x] pipeline.md (2026-07-16)
- [x] renderer.md (2026-07-16)

**状态**: ✅ Week 5 P0任务完成

---

## 🔄 Contract生命周期

### 状态

- **Draft**: 草稿，正在编写
- **Active**: 已生效，正在执行
- **Deprecated**: 已废弃，计划移除
- **Superseded**: 已被新版本取代

### 更新流程

1. 提出修改（PR + 说明理由）
2. 团队Review（检查影响范围）
3. 更新Contract文档
4. 更新Version History
5. 更新相关代码

### Enforcement

- **当前**: 人工Review
- **Week 8+**: CI检查（ArchUnit规则）

---

## 📚 参考资料

- [Architecture Review Response](../architecture-review-response.md) - Contract系统的起源
- [Week 5-10 Plan](../progress/week-5-10-plan.md) - 架构重构路线图
- [Pipeline Design](../architecture/01-generation-pipeline.md) - Pipeline架构设计

---

**维护者**: Aperture Core Team  
**创建日期**: 2026-07-16  
**状态**: Active
