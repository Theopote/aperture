# Kernel Contracts

## 目的

Kernel Contracts定义了Aperture核心模块的**契约**和**边界**。每个模块明确规定：

- ✅ **允许做什么** (Responsibilities)
- ❌ **禁止做什么** (Forbidden)
- 📥 **输入类型** (Inputs)
- 📤 **输出类型** (Outputs)
- 🔄 **生命周期** (Lifecycle)
- 🚫 **禁止依赖** (Forbidden Dependencies)

## 为什么需要Contract？

**问题**: 没有明确边界，代码风格会"漂移"

**解决**: Contract作为**法律文件**，强制执行模块职责

**类比**: 就像API文档，但是针对模块而不是函数

## Contract列表

| 模块 | 文档 | 状态 | 核心职责 |
|------|------|------|----------|
| **Geometry** | [geometry.md](geometry.md) | ✅ | 几何计算和操作 |
| **Parameter** | [parameter.md](parameter.md) | ✅ | 参数定义和解析 |
| **Mesh** | [mesh.md](mesh.md) | ✅ | 网格生成和处理 |
| **Component** | [component.md](component.md) | ✅ | 组件图和依赖 |
| **Pipeline** | [pipeline.md](pipeline.md) | ✅ | 生成流程编排 |
| **Renderer** | [renderer.md](renderer.md) | ✅ | 渲染抽象 |

## 如何使用Contract

### 开发新功能时

1. **确定归属**: 这个功能属于哪个模块？
2. **检查Contract**: 该模块允许这个职责吗？
3. **检查依赖**: 需要的依赖在允许列表中吗？
4. **遵循规则**: 按照生命周期和错误处理规则实现

### Code Review时

1. **验证边界**: 代码是否越界到其他模块职责？
2. **检查依赖**: 是否引入了禁止的依赖？
3. **验证接口**: 输入输出类型是否符合Contract？

### 重构时

1. **识别违规**: 哪些代码违反了Contract？
2. **制定计划**: 如何迁移到正确位置？
3. **保持兼容**: 临时保留兼容层

## Contract模板

每个Contract文档应包含以下部分：

```markdown
# [Module Name] Contract

## Overview
一句话描述模块职责

## Responsibilities
- 明确列出允许做什么
- 每项一条

## Forbidden
- ❌ 明确列出禁止做什么
- 每项一条

## Allowed Dependencies
- 列出允许依赖的模块
- 说明为什么需要

## Forbidden Dependencies
- ❌ 列出禁止依赖的模块
- 说明为什么禁止

## Input Types
- 列出接受的输入类型
- 包含验证规则

## Output Types
- 列出产生的输出类型
- 包含不变式

## Lifecycle
- 对象创建规则
- 状态管理规则
- 清理规则

## Error Handling
- 异常类型
- 错误传播策略
- 恢复机制

## Performance Requirements
- 时间复杂度要求
- 空间复杂度要求
- 缓存策略

## Examples
- 正确用法示例
- 错误用法示例
```

## 执行机制

### 1. 文档Review

所有PR必须检查是否符合Contract

### 2. CI检查（未来）

```bash
# 检查依赖违规
./scripts/check-contracts.sh

# 输出违规报告
Module 'aperture-geometry' depends on 'net.minecraft.*'
FORBIDDEN by contract: geometry.md
```

### 3. 定期审计

每月review所有模块，更新Contract

## 版本控制

Contract文档也有版本：

```markdown
---
version: 1.0
last_updated: 2026-07-16
status: active
---
```

## Contract变更流程

**修改Contract需要**:

1. 提出RFC (Request for Comments)
2. 团队讨论和投票
3. 更新Contract文档
4. 通知所有开发者
5. 给出迁移时间（如breaking change）

## 常见问题

### Q: Contract太严格，限制创新？

A: Contract定义**边界**而非**实现**。在边界内有充分自由。

### Q: 如果需要跨边界怎么办？

A: 
1. 通过允许的依赖模块
2. 提出Contract修改RFC
3. 创建新的桥接模块

### Q: Contract和接口文档的区别？

A: 
- **接口文档**: 函数级别，如何调用
- **Contract**: 模块级别，职责边界

## 参考资料

- [Clean Architecture](https://blog.cleancoder.com/uncle-bob/2012/08/13/the-clean-architecture.html)
- [Hexagonal Architecture](https://alistair.cockburn.us/hexagonal-architecture/)
- [Domain-Driven Design](https://www.domainlanguage.com/ddd/)

---

**Created**: 2026-07-16  
**Purpose**: 建立清晰的模块边界  
**Status**: Week 5 - Kernel Contract Phase
