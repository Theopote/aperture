# Week 6: Module Restructure - Revised Plan

**日期**: 2026-07-16  
**状态**: 🚧 执行中

---

## 📊 当前结构分析

### 已存在的模块

```
aperture-math/           ✅ 已存在 (Vec3d, BoundingBox, Transform3d)
aperture-core/           ❌ 职责混杂
  ├── catalog/          → Opening业务逻辑
  ├── component/        → Opening组件定义（非通用组件图）
  ├── constraint/       → 应拆分到aperture-parameter
  ├── definition/       → Opening定义
  ├── geometry/         → 空目录
  ├── instance/         → Opening实例
  ├── material/         → 应移到aperture-types
  ├── opening/          → Opening业务
  ├── parameter/        → 应拆分到aperture-parameter
  ├── parametric/       → 应拆分到aperture-parameter
  ├── placement/        → Opening放置逻辑
  ├── serialization/    → 序列化工具
  └── validation/       → 验证逻辑

aperture-opening-geometry/  ❌ 应重命名为aperture-opening
  ├── component/        → ComponentPlan（Opening组装）
  ├── geometry/         → Opening几何生成器
  └── pipeline/         → Opening管线（非通用Pipeline）

aperture-geometry/      ✅ 几何计算（OCCT绑定）
aperture-render/        ✅ 渲染
aperture-runtime/       ❓ 待确认
aperture-editor/        ✅ 编辑器
aperture-fabric/        ✅ Minecraft集成
```

---

## 🎯 务实的重构策略

### 阶段1: 最小改动（本周完成）

**目标**: 建立清晰边界，不做大规模代码移动

1. **重命名模块**
   - `aperture-opening-geometry` → `aperture-opening`
   
2. **创建aperture-parameter模块**
   - 移动: aperture-core/parameter/ → aperture-parameter/
   - 移动: aperture-core/parametric/ → aperture-parameter/parametric/
   - 移动: aperture-core/constraint/ → aperture-parameter/constraint/
   
3. **整理aperture-types**
   - 移动: aperture-core/material/ → aperture-types/material/
   - aperture-types作为基础类型库（Profile, Material等）

4. **aperture-core重新定位**
   - 保留为Opening核心业务逻辑
   - 包含: catalog, definition, instance, opening, placement, validation, serialization
   - 依赖: aperture-parameter, aperture-types

5. **更新依赖链**
   ```
   aperture-math
       ↓
   aperture-types
       ↓
   aperture-parameter
       ↓
   aperture-core (Opening业务)
       ↓
   aperture-opening (Opening几何生成)
       ↓
   aperture-fabric
   ```

---

## 📋 执行步骤

### Step 1: Git提交当前状态
```bash
cd /sessions/charming-lucid-pasteur/mnt/aperture-26.1
git add -A
git commit -m "Week 5: Kernel Contract文档完成"
```

### Step 2: 创建aperture-parameter模块
```bash
mkdir -p aperture-parameter/src/main/java/dev/aperture/parameter
mkdir -p aperture-parameter/src/test/java/dev/aperture/parameter
```

创建 aperture-parameter/build.gradle:
```gradle
dependencies {
    api project(':aperture-types')
    api project(':aperture-math')
}
```

### Step 3: 移动参数相关代码
```bash
# 移动parameter包
git mv aperture-core/src/main/java/dev/aperture/core/parameter aperture-parameter/src/main/java/dev/aperture/

# 移动parametric包
git mv aperture-core/src/main/java/dev/aperture/core/parametric aperture-parameter/src/main/java/dev/aperture/

# 移动constraint包
git mv aperture-core/src/main/java/dev/aperture/core/constraint aperture-parameter/src/main/java/dev/aperture/
```

### Step 4: 移动material到aperture-types
```bash
git mv aperture-core/src/main/java/dev/aperture/core/material aperture-opening-geometry/src/main/java/dev/aperture/opening/material
# 稍后重命名aperture-opening-geometry时一起处理
```

### Step 5: 重命名aperture-opening-geometry
```bash
git mv aperture-opening-geometry aperture-opening
```

更新settings.gradle:
```gradle
include 'aperture-opening'  // 替换 'aperture-opening-geometry'
```

### Step 6: 更新所有import语句
使用IDE的全局查找替换:
- `dev.aperture.core.parameter` → `dev.aperture.parameter`
- `dev.aperture.core.parametric` → `dev.aperture.parameter.parametric`
- `dev.aperture.core.constraint` → `dev.aperture.parameter.constraint`
- `dev.aperture.opening.geometry` → `dev.aperture.opening`

### Step 7: 更新依赖配置
更新各模块的build.gradle添加aperture-parameter依赖

### Step 8: 验证构建
```bash
./gradlew clean build
```

---

## ⏱️ 预计时间

| 步骤 | 时间 |
|------|------|
| Git提交 | 5min |
| 创建aperture-parameter | 10min |
| 移动代码 | 30min |
| 重命名模块 | 10min |
| 更新import | 20min |
| 更新依赖 | 15min |
| 验证构建 | 15min |
| **总计** | **~2小时** |

---

## ✅ 成功标准

- [ ] aperture-parameter模块创建成功
- [ ] aperture-opening-geometry重命名为aperture-opening
- [ ] 参数相关代码移至aperture-parameter
- [ ] 所有import语句更新
- [ ] ./gradlew build成功
- [ ] 依赖方向正确

---

**下一步**: 开始执行Step 1 - Git提交
