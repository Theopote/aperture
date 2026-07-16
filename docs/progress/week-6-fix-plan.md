# Week 6 重构问题诊断和修复方案

**日期**: 2026-07-16  
**问题**: 创建了循环依赖

---

## 🚨 问题诊断

### 发现的循环依赖

```
aperture-parameter
    ↓ (依赖)
aperture-core (通过InstanceParameters等类)
    ↓ (依赖)
aperture-parameter
```

**原因**: 错误地将业务逻辑类移到了aperture-parameter

### 错误分类的文件

#### parametric/ 包（应该留在aperture-core）
这些类都操作`OpeningInstance`、`OpeningTypeDefinition`等Opening业务对象：
- ❌ InstanceParameters.java - 操作OpeningInstance
- ❌ ParametricEditor.java - 编辑Opening参数
- ❌ ParametricValidator.java - 验证Opening
- ❌ OpeningStateParameters.java - Opening状态
- ❌ BooleanParameter.java - Opening参数定义
- ❌ ChoiceParameter.java
- ❌ EnumParameter.java
- ❌ MaterialParameter.java
- ❌ NumberParameter.java
- ❌ RangeParameter.java
- ❌ ParametricSchema.java - Opening schema
- ❌ 其他11个文件...

#### constraint/ 包中的部分文件
- ❌ ExpressionConstraintValidator.java - 验证Opening

---

## ✅ 正确的模块划分

### aperture-parameter（纯参数系统，无Opening依赖）

**应该包含**:
1. **parameter/**
   - ParameterSet.java ✅
   - ParameterType.java ✅
   - ParameterValue.java ✅

2. **constraint/** (约束引擎核心)
   - ConstraintContext.java ✅
   - ConstraintEvaluator.java ✅
   - ConstraintExpression.java ✅
   - ExpressionLexer.java ✅
   - ExpressionParser.java ✅
   - ConstraintExpressionException.java ✅

**依赖**: 只依赖aperture-math

### aperture-core（Opening业务逻辑）

**应该包含**:
1. **parameter/** (已在上面)
2. **parametric/** (全部保留)
3. **constraint/** 
   - ExpressionConstraintValidator.java (依赖Opening)
4. **definition/**, **instance/**, **catalog/**, **validation/** 等

**依赖**: aperture-parameter, aperture-math

---

## 🔧 修复方案

### 方案A: 最小修复（推荐）

**只移动纯参数类**，其他保留在aperture-core

```bash
# 回滚parametric/和部分constraint/
rm -rf aperture-parameter/src/main/java/dev/aperture/parametric
rm aperture-parameter/src/main/java/dev/aperture/constraint/ExpressionConstraintValidator.java

# aperture-parameter最终只包含:
# - parameter/ (3个文件)
# - constraint/ (5个核心引擎文件)
```

### 方案B: 完全回滚

**撤销所有更改**，重新思考架构

```bash
rm -rf aperture-parameter
git checkout -- .
```

---

## 📊 正确的依赖关系

```
aperture-math (纯数学)
    ↓
aperture-parameter (纯参数：ParameterSet, 约束引擎)
    ↓
aperture-core (Opening业务：InstanceParameters, ParametricEditor)
    ↓
aperture-opening (Opening几何生成)
```

**The Iron Law**: aperture-parameter不能依赖任何Opening概念

---

## 🎯 立即行动

### Step 1: 移除parametric/包

```bash
cd aperture-parameter/src/main/java/dev/aperture
rm -rf parametric
```

### Step 2: 移除业务逻辑constraint文件

```bash
cd aperture-parameter/src/main/java/dev/aperture/constraint
rm ExpressionConstraintValidator.java
```

### Step 3: 确认aperture-parameter内容

**只保留**:
- parameter/ParameterSet.java
- parameter/ParameterType.java  
- parameter/ParameterValue.java
- constraint/ConstraintContext.java
- constraint/ConstraintEvaluator.java
- constraint/ConstraintExpression.java
- constraint/ConstraintExpressionException.java
- constraint/ExpressionLexer.java
- constraint/ExpressionParser.java

**总计**: 9个文件

### Step 4: 恢复aperture-core的import

```bash
cd aperture-core/src/main/java
# parametric和ExpressionConstraintValidator仍在core中，无需修改import
```

### Step 5: 验证构建

```bash
./gradlew :aperture-parameter:build
./gradlew :aperture-core:build
```

---

## 💡 经验教训

1. **先理解依赖** - 移动代码前先分析依赖关系
2. **检查import** - 如果新模块import旧模块，说明有问题
3. **Contract优先** - 按Contract定义的职责划分，不按目录结构
4. **小步验证** - 每移动一个包就验证编译

---

**推荐**: 执行方案A（最小修复），只保留纯参数类在aperture-parameter
