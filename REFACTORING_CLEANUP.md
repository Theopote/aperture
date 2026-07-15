# Aperture 项目重构 - 清理说明

## 已完成的重构

### 1. 几何基础类迁移
已将几何基础类从 `aperture-core` 移动到 `aperture-geometry`：
- `dev.aperture.core.geometry.*` → `dev.aperture.geometry.primitives.*`
  - Vec3d
  - BoundingBox  
  - Facing
  - Transform3d

### 2. Generator 包合并
已合并 generator 相关包：
- `dev.aperture.geometry.generator.OpeningGenerator` (接口)
- `dev.aperture.geometry.generator.RectangularWindowGenerator` (实现)

### 3. Fabric 适配器包重组
已将 Fabric 集成代码重新组织：
- `dev.aperture.placement.fabric.*` → `dev.aperture.fabric.placement.*`

### 4. 所有 import 语句已更新
- aperture-core 模块
- aperture-geometry 模块
- aperture-api 模块
- 主模块 (src/main 和 src/client)
- 所有测试文件

## 需要手动清理的旧文件

由于文件权限限制，以下旧文件需要手动删除：

### 删除旧的几何类（aperture-core）
```
aperture-core/src/main/java/dev/aperture/core/geometry/
├── BoundingBox.java
├── Facing.java
├── Transform3d.java
└── Vec3d.java
```

### 删除旧的 generator 包（aperture-geometry）
```
aperture-geometry/src/main/java/dev/aperture/geometry/generators/
└── RectangularWindowGenerator.java
```

### 删除旧的 Fabric 包（主模块）
```
src/main/java/dev/aperture/placement/
└── fabric/
    ├── FabricPlacementAdapter.java
    ├── FabricPlacementRaycast.java
    ├── FabricPlacementTarget.java
    ├── HostAnchor.java
    ├── HostClassifier.java
    ├── HostPlaneScanner.java
    ├── McBoundsConverter.java
    ├── McCoordinates.java
    ├── McFacingConverter.java
    └── McUnits.java
```

### 删除错误位置的 client 目录
```
src/main/java/dev/aperture/client/
└── placement/
```
**注意**：客户端代码应该在 `src/client/java/dev/aperture/client/`（Fabric Loom 的 split source sets）

### 删除空的测试目录
```
aperture-geometry/src/test/java/dev/aperture/geometry/generators/
```

## 手动删除命令

在项目根目录执行：

```bash
# 删除旧的几何类
rm -rf aperture-core/src/main/java/dev/aperture/core/geometry/

# 删除旧的 generator 包
rm -rf aperture-geometry/src/main/java/dev/aperture/geometry/generators/

# 删除旧的 Fabric 包（旧的 placement 目录）
rm -rf src/main/java/dev/aperture/placement/

# 删除错误位置的 client 目录
rm -rf src/main/java/dev/aperture/client/

# 删除空的测试目录
rmdir aperture-geometry/src/test/java/dev/aperture/geometry/generators/ 2>/dev/null || true
```

## 验证重构

构建项目以验证所有更改：

```bash
./gradlew clean build
```

## 重构后的项目结构

```
aperture-26.1/
├── aperture-api/              # 纯接口定义
│   └── src/main/java/dev/aperture/api/
│       ├── ApertureApi.java
│       ├── registry/
│       │   └── GeneratorRegistry.java
│       └── service/
│           └── OpeningGenerationService.java
│
├── aperture-core/             # 核心业务逻辑
│   └── src/main/java/dev/aperture/core/
│       ├── catalog/
│       ├── definition/
│       ├── instance/
│       ├── opening/
│       ├── parameter/
│       ├── placement/
│       ├── serialization/
│       └── validation/
│
├── aperture-geometry/         # 几何基础设施 + 生成器
│   └── src/main/java/dev/aperture/geometry/
│       ├── primitives/        # 新位置：基础几何类
│       │   ├── Vec3d.java
│       │   ├── BoundingBox.java
│       │   ├── Facing.java
│       │   └── Transform3d.java
│       ├── generator/         # 合并后的 generator 包
│       │   ├── OpeningGenerator.java
│       │   └── RectangularWindowGenerator.java
│       └── model/
│           ├── GeometryLayer.java
│           ├── GeometryResult.java
│           └── GeometrySolid.java
│
└── src/                       # Fabric/Minecraft 集成层
    ├── main/java/dev/aperture/
    │   ├── Aperture.java
    │   ├── bootstrap/
    │   │   └── ApertureBootstrap.java
    │   └── fabric/            # 新位置：Fabric 适配器
    │       └── placement/
    │           ├── FabricPlacementAdapter.java
    │           ├── FabricPlacementTarget.java
    │           ├── FabricPlacementRaycast.java
    │           ├── HostClassifier.java
    │           ├── HostPlaneScanner.java
    │           ├── HostAnchor.java
    │           ├── McCoordinates.java
    │           ├── McFacingConverter.java
    │           ├── McBoundsConverter.java
    │           └── McUnits.java
    │
    └── client/java/dev/aperture/client/
        ├── ApertureClient.java
        ├── placement/
        │   └── ClientPlacementPreview.java
        └── render/
            └── placement/
                └── PlacementPreviewRenderer.java
```

## 模块职责说明

### aperture-api
- 纯接口定义层
- 提供给插件开发者的公共 API
- 依赖：aperture-core, aperture-geometry

### aperture-core  
- 核心业务逻辑
- 类型定义、参数系统、验证、放置逻辑
- 依赖：aperture-geometry (仅用于几何基础类型)

### aperture-geometry
- 几何基础设施（primitives 包）
- 几何生成器接口和实现
- 依赖：aperture-core

### 主模块 (aperture)
- Fabric/Minecraft 集成
- 客户端和服务端入口
- 依赖：aperture-api

## 重构收益

1. **更清晰的模块职责**：每个模块的职责更加明确
2. **更好的依赖关系**：消除了循环依赖，依赖方向更合理
3. **更好的包组织**：相关的类放在同一个包中
4. **更易维护**：新的结构更容易理解和扩展
