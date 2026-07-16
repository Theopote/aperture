# Week 6 重构 - 当前状态和手动修复指南

**日期**: 2026-07-16  
**状态**: ⚠️ 需要手动修复

---

## 🚨 当前问题

### 创建了循环依赖

```
aperture-parameter (包含Opening业务逻辑)
    ↓ 依赖
aperture-core (OpeningInstance, ValidationResult等)
    ↓ 依赖  
aperture-parameter (ParameterSet等)
```

**根本原因**: 错误地将22个Opening业务逻辑文件移到了aperture-parameter模块

---

## 📋 需要手动执行的修复步骤

### Step 1: 删除aperture-parameter中的业务逻辑文件

```bash
cd aperture-26.1/aperture-parameter/src/main/java/dev/aperture

# 删除整个parametric/包（22个文件，都是Opening业务逻辑）
rm -rf parametric/

# 删除constraint/中依赖Opening的文件
rm constraint/ExpressionConstraintValidator.java
```

### Step 2: 恢复aperture-core中的import语句

aperture-core中的文件仍然需要引用`dev.aperture.core.parametric`，因为parametric包应该留在core中：

```bash
cd aperture-26.1/aperture-core/src/main/java

# 恢复parametric的import（因为它们仍在core中）
find . -name "*.java" -exec sed -i 's/import dev\.aperture\.parametric/import dev.aperture.core.parametric/g' {} \;
```

### Step 3: 更新aperture-parameter中保留文件的import

aperture-parameter中保留的约束引擎文件需要更新：

```bash
cd aperture-26.1/aperture-parameter/src/main/java/dev/aperture

# 这些文件保留在aperture-parameter中，但不应该引用Opening类型
# 检查并手动修复constraint/中的依赖
```

### Step 4: 验证最终结构

**aperture-parameter应该只包含**（9个文件）：
```
aperture-parameter/
└── src/main/java/dev/aperture/
    ├── parameter/
    │   ├── ParameterSet.java
    │   ├── ParameterType.java
    │   └── ParameterValue.java
    └── constraint/
        ├── ConstraintContext.java
        ├── ConstraintEvaluator.java
        ├── ConstraintExpression.java
        ├── ConstraintExpressionException.java
        ├── ExpressionLexer.java
        └── ExpressionParser.java
```

**aperture-core保留**：
- ✅ parametric/ (22个文件保持不变)
- ✅ constraint/ExpressionConstraintValidator.java

### Step 5: 验证构建

```bash
cd aperture-26.1

# 清理并重新构建
./gradlew clean
./gradlew :aperture-parameter:build
./gradlew :aperture-core:build
./gradlew build
```

---

## ✅ 正确的最终结构

### aperture-parameter (纯参数系统)

**职责**: 参数值容器 + 约束表达式引擎（无Opening概念）

**包含**:
- parameter/ (3个文件)
- constraint/ (6个文件：约束引擎核心)

**依赖**: 只依赖aperture-math

**不包含**: 任何Opening、Instance、Validation相关类

### aperture-core (Opening业务逻辑)

**职责**: Opening类型定义、实例管理、业务规则

**包含**:
- catalog/ (Opening类型注册)
- component/ (Opening组件)
- definition/ (Opening定义)
- instance/ (Opening实例)
- opening/ (Opening业务)
- parametric/ (Opening参数系统) ← 保留在这里
- constraint/ExpressionConstraintValidator.java ← 保留在这里
- validation/ (Opening验证)
- placement/ (Opening放置)
- serialization/ (序列化)

**依赖**: aperture-parameter, aperture-math

---

## 🎯 修复后的依赖关系

```
aperture-math
    ↓
aperture-parameter (纯参数：ParameterSet + 约束引擎)
    ↓
aperture-core (Opening业务 + parametric包)
    ↓
aperture-opening-geometry
    ↓
aperture-fabric
```

**无循环依赖** ✅

---

## 📊 文件移动统计（修正后）

| 包 | 文件数 | 移动到aperture-parameter | 保留在aperture-core |
|---|--------|------------------------|---------------------|
| parameter/ | 3 | ✅ 3 | |
| parametric/ | 22 | ❌ 0 | ✅ 22 |
| constraint/ | 15 | ✅ 6 (引擎) | ✅ 9 (业务) |
| **总计** | 40 | **9** | **31** |

---

## 💡 经验教训

### 错误原因

1. **按目录结构移动** - 没有分析文件的实际职责
2. **没有检查依赖** - 移动前没有检查import语句
3. **违反Contract** - parametric包明显依赖Opening，不应该在parameter模块

### 正确做法

1. **先看依赖** - 检查文件import了哪些类
2. **看职责** - parametric是"Opening参数系统"不是"通用参数系统"
3. **遵循Contract** - aperture-parameter不能知道Opening的存在

### 为什么parametric应该留在aperture-core？

看文件名就知道：
- `InstanceParameters` - 操作**Opening**Instance
- `ParametricEditor` - 编辑**Opening**参数
- `OpeningStateParameters` - **Opening**状态参数

这些都是Opening业务逻辑，不是通用参数系统！

---

## 🔄 替代方案：完全回滚

如果修复太复杂，可以完全回滚：

```bash
cd aperture-26.1

# 删除aperture-parameter模块
rm -rf aperture-parameter

# 恢复settings.gradle
# 移除: include 'aperture-parameter'

# 恢复aperture-core/build.gradle
# 移除: api project(':aperture-parameter')

# 重新构建
./gradlew clean build
```

然后重新规划更简单的重构策略。

---

## 🚀 建议的下一步

### 选项1: 修复当前重构（推荐）

按照上面的步骤手动修复，只保留9个纯参数文件在aperture-parameter

**优点**: 建立了parameter模块基础  
**缺点**: 需要手动操作

### 选项2: 完全回滚

撤销所有更改，保持原有结构

**优点**: 安全、简单  
**缺点**: 浪费了已完成的工作

### 选项3: 暂停重构，先完善Contract

重新审视哪些代码真正需要独立成模块

**优点**: 避免再次出错  
**缺点**: 延迟Week 6目标

---

## 📚 相关文档

- [Week 6修复方案](week-6-fix-plan.md)
- [Parameter Contract](../kernel-contracts/parameter.md)
- [Architecture Review](../architecture-review-response.md)

---

**总结**: Week 6重构遇到了循环依赖问题，需要手动移除22个Opening业务逻辑文件。修复后aperture-parameter将只包含9个纯参数系统文件，符合Contract设计。
