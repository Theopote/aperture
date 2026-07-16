# Geometry Module Contract

**Version**: 1.0  
**Last Updated**: 2026-07-16  
**Status**: Active  
**Owner**: Aperture Core Team

---

## Overview

Geometry模块提供**纯几何计算和操作**，是Aperture的数学基础。它不知道Opening、Door、Window等业务概念，只处理抽象的几何实体。

**核心原则**: Geometry是工具箱，不是应用。

---

## Responsibilities

### ✅ 允许做的事

1. **提供几何基础类型**
   - Point3D, Vector3D, Transform
   - Line, Curve, Arc, Spline
   - Plane, Surface, BSplineSurface
   - Solid, CSGSolid, BRepSolid

2. **实现几何操作**
   - Extrude (拉伸)
   - Revolve (旋转)
   - Sweep (扫掠)
   - Loft (放样)
   - Boolean (布尔运算: union, subtract, intersect)
   - Offset (偏移)
   - Fillet/Chamfer (圆角/倒角)

3. **生成三角网格**
   - Shape → Mesh 转换
   - 细分控制
   - 法线计算
   - UV映射

4. **几何查询**
   - 点是否在体内
   - 最近点计算
   - 相交检测
   - 边界框计算

5. **几何验证**
   - 拓扑一致性检查
   - 自交检测
   - 退化几何检测

---

## Forbidden

### ❌ 禁止做的事

1. **❌ 不能包含业务逻辑**
   ```java
   // 错误示例
   public class DoorGeometry { ... }  // Door是业务概念
   
   // 正确示例
   public class ExtrusionOperation { ... }  // Extrusion是几何操作
   ```

2. **❌ 不能依赖Minecraft**
   ```java
   // 错误
   import net.minecraft.world.level.block.Block;
   
   // 正确
   import dev.aperture.math.Vec3d;
   ```

3. **❌ 不能知道Opening概念**
   ```java
   // 错误
   public Mesh generateWindow(WindowDefinition def) { ... }
   
   // 正确
   public Mesh extrude(ProfileCurve profile, Vec3d direction) { ... }
   ```

4. **❌ 不能包含渲染代码**
   ```java
   // 错误
   public void render(PoseStack matrices) { ... }
   
   // 正确
   public Mesh generateMesh() { ... }  // 只生成数据
   ```

5. **❌ 不能包含世界交互**
   ```java
   // 错误
   public void placeInWorld(Level level, BlockPos pos) { ... }
   
   // 正确
   public Shape createShape(ShapeSpec spec) { ... }
   ```

6. **❌ 不能包含用户交互**
   ```java
   // 错误
   public void onMouseClick(int x, int y) { ... }
   
   // 正确
   public Vec3d rayIntersect(Ray ray) { ... }
   ```

---

## Allowed Dependencies

### ✅ 可以依赖的模块

1. **aperture-math** (必需)
   - Vec3d, Vec2d
   - Transform, Matrix4f
   - BoundingBox
   - 数学工具函数

2. **Java标准库**
   - java.util.*
   - java.lang.*
   - 不依赖java.awt (避免平台依赖)

3. **第三方几何库** (可选)
   - JOML (线性代数)
   - Apache Commons Math (数值计算)

**依赖原则**: 只依赖比自己更基础的模块

---

## Forbidden Dependencies

### ❌ 禁止依赖的模块

1. **❌ net.minecraft.*** (任何Minecraft代码)
   - 理由: 保持平台独立

2. **❌ aperture-opening** (Opening业务逻辑)
   - 理由: Geometry是Opening的依赖，不能反向

3. **❌ aperture-fabric** (Minecraft集成层)
   - 理由: 平台独立性

4. **❌ aperture-client** (渲染和UI)
   - 理由: Geometry只生成数据，不负责显示

5. **❌ aperture-core** (如果core包含业务逻辑)
   - 理由: 避免循环依赖

**例外**: 可以依赖 `aperture-definition` 的**类型定义**（如果纯数据结构）

---

## Input Types

### 📥 接受的输入

1. **几何规格 (Specifications)**
   ```java
   public record ExtrusionSpec(
       ProfileCurve profile,
       Vec3d direction,
       double distance
   ) {}
   ```

2. **基本几何类型**
   ```java
   - Vec3d point
   - Curve curve
   - Surface surface
   - Shape shape
   ```

3. **操作参数**
   ```java
   - double tolerance
   - int subdivisions
   - BooleanOp operation
   ```

4. **变换**
   ```java
   - Transform transform
   - Matrix4f matrix
   ```

### 输入验证规则

```java
public Shape extrude(ProfileCurve profile, Vec3d direction, double distance) {
    // 验证输入
    Objects.requireNonNull(profile, "profile cannot be null");
    Objects.requireNonNull(direction, "direction cannot be null");
    
    if (distance <= 0) {
        throw new IllegalArgumentException("distance must be positive");
    }
    
    if (direction.lengthSquared() < 1e-6) {
        throw new IllegalArgumentException("direction cannot be zero vector");
    }
    
    // 执行操作
    return performExtrusion(profile, direction, distance);
}
```

---

## Output Types

### 📤 产生的输出

1. **几何对象**
   ```java
   - Shape (抽象几何)
   - Solid (实体)
   - Surface (曲面)
   - Curve (曲线)
   ```

2. **网格数据**
   ```java
   - Mesh (三角网格)
   - VertexBuffer (顶点缓冲)
   ```

3. **几何属性**
   ```java
   - BoundingBox (边界框)
   - Volume (体积)
   - Area (面积)
   ```

4. **查询结果**
   ```java
   - Vec3d (最近点)
   - boolean (相交判断)
   - List<Vec3d> (交点集合)
   ```

### 输出不变式

**保证**:
- 输出几何拓扑一致（无破损）
- 输出网格无退化三角形（除非输入已退化）
- 边界框始终包含实际几何
- 变换后几何与原始几何等价

**不保证**:
- 性能（由调用者控制细分参数）
- 内存占用（大几何体可能消耗大内存）

---

## Lifecycle

### 对象创建

**推荐**: 使用不可变对象
```java
// 好: 不可变
public record Vec3d(double x, double y, double z) {
    public Vec3d add(Vec3d other) {
        return new Vec3d(x + other.x, y + other.y, z + other.z);
    }
}

// 避免: 可变
public class MutableVec3d {
    public double x, y, z;
    public void add(Vec3d other) {
        this.x += other.x;  // 修改状态
    }
}
```

**Builder模式**: 复杂对象使用Builder
```java
Shape shape = new ShapeBuilder()
    .addCurve(curve1)
    .addCurve(curve2)
    .close()
    .build();
```

### 状态管理

**无状态优先**: 大部分操作应该是纯函数
```java
// 好: 无状态
public static Mesh extrude(ProfileCurve profile, Vec3d dir) {
    return new Mesh(...);  // 不修改输入
}

// 避免: 有状态
public class Extruder {
    private ProfileCurve profile;
    public void setProfile(ProfileCurve p) { this.profile = p; }
    public Mesh extrude() { ... }  // 依赖内部状态
}
```

**例外**: 大型几何体可以使用状态缓存
```java
public class CachedMesh {
    private Mesh mesh;
    private BoundingBox cachedBounds;
    
    public BoundingBox bounds() {
        if (cachedBounds == null) {
            cachedBounds = computeBounds();
        }
        return cachedBounds;
    }
}
```

### 资源清理

**不需要显式清理**: Java GC自动管理

**例外**: 如果使用native库
```java
public class NativeGeometry implements AutoCloseable {
    private long nativeHandle;
    
    @Override
    public void close() {
        if (nativeHandle != 0) {
            nativeFree(nativeHandle);
            nativeHandle = 0;
        }
    }
}
```

---

## Error Handling

### 异常类型

1. **IllegalArgumentException** - 参数不合法
   ```java
   if (distance <= 0) {
       throw new IllegalArgumentException("distance must be positive");
   }
   ```

2. **GeometryException** - 几何操作失败
   ```java
   public class GeometryException extends RuntimeException {
       public GeometryException(String message) { ... }
       public GeometryException(String message, Throwable cause) { ... }
   }
   
   // 使用
   if (!isManifold(shape)) {
       throw new GeometryException("Shape is not manifold");
   }
   ```

3. **NullPointerException** - 不应该发生
   ```java
   // 使用Objects.requireNonNull提前检查
   Objects.requireNonNull(input, "input cannot be null");
   ```

### 错误传播策略

**快速失败 (Fail-fast)**:
```java
public Mesh boolean(Shape a, Shape b, BooleanOp op) {
    Objects.requireNonNull(a);
    Objects.requireNonNull(b);
    Objects.requireNonNull(op);
    
    if (!a.isValid()) {
        throw new GeometryException("Shape a is invalid");
    }
    
    // 继续操作
}
```

**不吞异常**:
```java
// 错误: 吞掉异常
try {
    return computeMesh();
} catch (Exception e) {
    return null;  // 丢失错误信息
}

// 正确: 传播异常
try {
    return computeMesh();
} catch (MeshException e) {
    throw new GeometryException("Failed to compute mesh", e);
}
```

### 恢复机制

**降级策略**: 对于非关键失败
```java
public Mesh meshWithFallback(Shape shape, int quality) {
    try {
        return highQualityMesh(shape, quality);
    } catch (GeometryException e) {
        LOGGER.warn("High quality meshing failed, using fallback", e);
        return simpleMesh(shape);  // 降级到简单mesh
    }
}
```

---

## Performance Requirements

### 时间复杂度

| 操作 | 复杂度 | 说明 |
|------|--------|------|
| Vec3d加减 | O(1) | 常数时间 |
| 变换应用 | O(n) | n = 顶点数 |
| 边界框计算 | O(n) | n = 顶点数 |
| 布尔运算 | O(n²) | n = 面数 |
| Mesh生成 | O(n·s) | n = 曲面复杂度, s = 细分数 |

### 空间复杂度

| 数据结构 | 空间 | 说明 |
|----------|------|------|
| Vec3d | 24 bytes | 3 doubles |
| Mesh | O(n) | n = 顶点数 |
| BRepSolid | O(n) | n = 拓扑元素数 |

### 性能目标

- **单次操作**: < 10ms (简单几何)
- **批量操作**: 使用缓存
- **大型几何**: 支持LOD (Level of Detail)

### 缓存策略

```java
public class CachedShape {
    private final Shape shape;
    private Mesh cachedMesh;
    
    public Mesh getMesh(int quality) {
        if (cachedMesh == null || cachedMesh.quality() != quality) {
            cachedMesh = generateMesh(shape, quality);
        }
        return cachedMesh;
    }
}
```

---

## Threading Model

### 线程安全

**不可变对象**: 自动线程安全
```java
public record Vec3d(...) {}  // 不可变 → 线程安全
```

**可变对象**: 需要同步
```java
public class MeshBuilder {
    private final List<Vec3d> vertices = new ArrayList<>();
    
    // 不是线程安全的
    public synchronized void addVertex(Vec3d v) {
        vertices.add(v);
    }
}
```

**推荐**: 避免共享可变状态

---

## Examples

### ✅ 正确用法

```java
// 示例1: 拉伸Profile生成Frame
ProfileCurve lProfile = ProfileCurve.fromPoints(List.of(
    new Vec2d(0, 0),
    new Vec2d(50, 0),
    new Vec2d(50, 10),
    new Vec2d(10, 10),
    new Vec2d(10, 80),
    new Vec2d(0, 80)
));

Vec3d start = new Vec3d(0, 0, 0);
Vec3d end = new Vec3d(1200, 0, 0);

ExtrusionShape extrusion = ExtrudeOp.linear(lProfile, start, end);
Mesh mesh = ShapeMesher.mesh(extrusion);

// 示例2: 布尔运算
Shape box1 = BoxShape.create(new Vec3d(0,0,0), new Vec3d(100,100,100));
Shape box2 = BoxShape.create(new Vec3d(50,50,50), new Vec3d(150,150,150));
Shape union = BooleanOp.union(box1, box2);

// 示例3: 变换
Transform transform = Transform.builder()
    .translate(new Vec3d(100, 0, 0))
    .rotateY(Math.PI / 4)
    .build();
    
Shape transformed = shape.transform(transform);
```

### ❌ 错误用法

```java
// 错误1: 依赖Opening概念
public Mesh generateDoor(DoorDefinition door) {  // ❌ Door是业务概念
    // ...
}

// 错误2: 依赖Minecraft
public Shape fromBlockState(BlockState state) {  // ❌ BlockState是Minecraft类
    // ...
}

// 错误3: 包含渲染逻辑
public void renderShape(Shape shape, PoseStack matrices) {  // ❌ 渲染不是Geometry职责
    // ...
}

// 错误4: 可变操作
public void modifyMesh(Mesh mesh) {  // ❌ 应该返回新Mesh
    mesh.vertices.clear();  // 修改输入
}
```

---

## Migration Guide

如果现有代码违反Contract，如何迁移？

### 违规类型1: Opening逻辑混入Geometry

**现状**:
```java
// aperture-geometry/src/.../RectangularWindowGenerator.java
public class RectangularWindowGenerator {
    public PipelineResult generate(OpeningDefinition def) { ... }
}
```

**迁移**:
```java
// 1. 保留几何操作在 aperture-geometry
public class RectangularExtrusion {
    public static Shape extrude(ProfileCurve profile, double width, double height) { ... }
}

// 2. 移动Generator到 aperture-types
// aperture-types/window/src/.../RectangularWindowGenerator.java
public class RectangularWindowGenerator implements Generator {
    public PipelineResult generate(GenerationContext ctx) {
        // 使用aperture-geometry的工具
        Shape shape = RectangularExtrusion.extrude(...);
        return ...;
    }
}
```

### 违规类型2: 反向依赖

**现状**: aperture-core依赖aperture-geometry

**迁移**: 拆分模块，确保单向依赖

---

## Version History

| Version | Date | Changes |
|---------|------|---------|
| 1.0 | 2026-07-16 | 初始版本 |

---

## References

- [Computational Geometry](https://en.wikipedia.org/wiki/Computational_geometry)
- [Boundary Representation](https://en.wikipedia.org/wiki/Boundary_representation)
- [OpenCASCADE](https://www.opencascade.com/) - 参考实现

---

**Status**: ✅ Active  
**Enforcement**: Manual review + CI checks (planned)
