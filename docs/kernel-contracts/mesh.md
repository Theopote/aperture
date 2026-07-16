# Mesh Module Contract

**Version**: 1.0  
**Last Updated**: 2026-07-16  
**Status**: Active  
**Owner**: Aperture Core Team

---

## Overview

Mesh模块负责**三角网格生成、优化和渲染数据准备**。它将几何体（Shape/Solid）转换为可渲染的三角网格，并提供网格分析和优化功能。

**核心原则**: Mesh是Geometry的下游，接受Shape输出三角网格，不包含渲染代码。

---

## Responsibilities

### ✅ 允许做的事

1. **网格生成**
   - 从几何体生成三角网格
   - 表面细分（tessellation）
   - 法向量计算
   - UV坐标生成

2. **网格数据结构**
   - Vertex（位置、法向、UV、颜色）
   - Triangle索引
   - Mesh元数据（顶点数、三角形数、包围盒）

3. **网格优化**
   - 顶点去重
   - 法向量平滑
   - 简化（LOD）
   - 拓扑清理

4. **网格分析**
   - 包围盒计算
   - 表面积计算
   - 拓扑验证（流形检查）
   - 网格质量检查

5. **数据格式转换**
   - 导出到OBJ
   - 导出到STL
   - 序列化为JSON（Golden Test）
   - 转换为渲染格式

---

## Forbidden

### ❌ 禁止做的事

1. **❌ 不能包含渲染代码**
   ```java
   // 错误
   public void render(Mesh mesh, RenderContext ctx) {
       GL11.glBegin(GL11.GL_TRIANGLES);
       // ...
   }
   
   // 正确
   public VertexBuffer toVertexBuffer(Mesh mesh) {
       return new VertexBuffer(mesh.vertices(), mesh.indices());
   }
   ```

2. **❌ 不能依赖Minecraft渲染系统**
   ```java
   // 错误
   import net.minecraft.client.render.VertexConsumer;
   
   public void emitToVertexConsumer(Mesh mesh, VertexConsumer consumer) { ... }
   
   // 正确
   // Mesh模块输出通用数据结构
   // 平台层（aperture-client）负责转换到Minecraft格式
   ```

3. **❌ 不能包含材质/纹理处理**
   ```java
   // 错误
   public Mesh applyTexture(Mesh mesh, Texture texture) { ... }
   
   // 正确
   public Mesh withUVCoordinates(Mesh mesh, UV[] uvs) { ... }  // 只提供UV坐标
   ```

4. **❌ 不能包含Opening业务逻辑**
   ```java
   // 错误
   public Mesh generateDoorMesh(DoorParameters params) { ... }
   
   // 正确
   public Mesh fromShape(Shape shape, MeshingOptions options) { ... }
   ```

5. **❌ 不能直接读取参数**
   ```java
   // 错误
   public Mesh generate(ParameterSet params) {
       double width = params.get("width").asLength();
       // ...
   }
   
   // 正确
   public Mesh fromShape(Shape shape) {
       // Mesh消费Shape，不关心参数来源
   }
   ```

---

## Allowed Dependencies

### ✅ 可以依赖的模块

1. **aperture-geometry** (几何计算)
   - 输入: Shape, Solid, Surface
   - 用于网格生成

2. **aperture-math** (数学工具)
   - 向量/矩阵运算
   - 包围盒计算

3. **Java标准库**
   - java.util.*
   - java.nio.* (ByteBuffer等)

**依赖原则**: 只依赖上游几何模块和数学工具

---

## Forbidden Dependencies

### ❌ 禁止依赖的模块

1. **❌ aperture-client** (渲染)
   - 理由: Mesh是数据准备，不关心如何渲染

2. **❌ net.minecraft.client.*** (Minecraft渲染)
   - 理由: 平台独立性

3. **❌ aperture-opening** (Opening业务)
   - 理由: Mesh是通用网格生成，不绑定Opening

4. **❌ aperture-parameter** (参数系统)
   - 理由: Mesh消费Shape，不直接处理参数

---

## Input Types

### 📥 接受的输入

1. **几何体**
   ```java
   // 从Shape生成网格
   public interface Shape {
       // Mesh模块消费Shape接口
   }
   
   // 从Solid生成网格
   public interface Solid extends Shape {
       // ...
   }
   ```

2. **网格生成选项**
   ```java
   public record MeshingOptions(
       double tolerance,           // 细分容差（mm）
       boolean smoothNormals,      // 是否平滑法向
       boolean generateUVs,        // 是否生成UV
       OptimizationLevel optimization  // 优化级别
   ) {
       public static MeshingOptions DEFAULT = new MeshingOptions(
           0.1,    // 0.1mm tolerance
           true,   // smooth normals
           false,  // no UVs
           OptimizationLevel.MEDIUM
       );
   }
   
   public enum OptimizationLevel {
       NONE,       // 不优化
       LOW,        // 只去重
       MEDIUM,     // 去重+拓扑清理
       HIGH        // 完整优化+简化
   }
   ```

3. **顶点数据**
   ```java
   public record Vertex(
       Vec3 position,              // 位置 (x, y, z)
       Vec3 normal,                // 法向量
       Optional<UV> uv,            // UV坐标
       Optional<Color> color       // 顶点颜色
   ) {
       public Vertex(Vec3 position, Vec3 normal) {
           this(position, normal, Optional.empty(), Optional.empty());
       }
   }
   
   public record UV(double u, double v) {
       public UV {
           if (u < 0 || u > 1 || v < 0 || v > 1) {
               throw new IllegalArgumentException("UV must be in [0, 1]");
           }
       }
   }
   ```

### 输入验证

```java
public Mesh fromShape(Shape shape, MeshingOptions options) {
    Objects.requireNonNull(shape, "shape cannot be null");
    Objects.requireNonNull(options, "options cannot be null");
    
    if (options.tolerance() <= 0) {
        throw new IllegalArgumentException("tolerance must be positive");
    }
    
    // 生成网格
    return generate(shape, options);
}
```

---

## Output Types

### 📤 产生的输出

1. **网格数据结构**
   ```java
   public final class Mesh {
       private final Vertex[] vertices;
       private final int[] indices;       // 三角形索引（每3个一组）
       private final AABB boundingBox;
       private final MeshMetadata metadata;
       
       public Mesh(Vertex[] vertices, int[] indices) {
           this.vertices = validateVertices(vertices);
           this.indices = validateIndices(indices, vertices.length);
           this.boundingBox = computeBoundingBox(vertices);
           this.metadata = new MeshMetadata(
               vertices.length,
               indices.length / 3,
               computeSurfaceArea()
           );
       }
       
       public Vertex[] vertices() { return vertices.clone(); }
       public int[] indices() { return indices.clone(); }
       public int vertexCount() { return vertices.length; }
       public int triangleCount() { return indices.length / 3; }
       public AABB boundingBox() { return boundingBox; }
       public MeshMetadata metadata() { return metadata; }
   }
   ```

2. **网格元数据**
   ```java
   public record MeshMetadata(
       int vertexCount,
       int triangleCount,
       double surfaceArea,
       Optional<String> materialHint
   ) {}
   ```

3. **渲染数据缓冲区**
   ```java
   public final class VertexBuffer {
       private final FloatBuffer positions;   // x,y,z,x,y,z,...
       private final FloatBuffer normals;     // nx,ny,nz,nx,ny,nz,...
       private final FloatBuffer uvs;         // u,v,u,v,... (optional)
       private final IntBuffer indices;       // i1,i2,i3,i1,i2,i3,...
       
       public static VertexBuffer fromMesh(Mesh mesh) {
           // 转换为GPU友好的紧凑格式
       }
   }
   ```

4. **导出格式**
   ```java
   // OBJ格式
   public String toOBJ(Mesh mesh) {
       StringBuilder sb = new StringBuilder();
       sb.append("# Generated by Aperture\n");
       
       // 顶点
       for (Vertex v : mesh.vertices()) {
           sb.append(String.format("v %.6f %.6f %.6f\n", 
               v.position().x(), v.position().y(), v.position().z()));
       }
       
       // 法向
       for (Vertex v : mesh.vertices()) {
           sb.append(String.format("vn %.6f %.6f %.6f\n",
               v.normal().x(), v.normal().y(), v.normal().z()));
       }
       
       // 面
       int[] indices = mesh.indices();
       for (int i = 0; i < indices.length; i += 3) {
           sb.append(String.format("f %d//%d %d//%d %d//%d\n",
               indices[i]+1, indices[i]+1,
               indices[i+1]+1, indices[i+1]+1,
               indices[i+2]+1, indices[i+2]+1));
       }
       
       return sb.toString();
   }
   
   // JSON格式（Golden Test）
   public JsonObject toJson(Mesh mesh) {
       return JsonObject.builder()
           .put("vertexCount", mesh.vertexCount())
           .put("triangleCount", mesh.triangleCount())
           .put("vertices", serializeVertices(mesh.vertices()))
           .put("indices", mesh.indices())
           .build();
   }
   ```

### 输出不变式

**保证**:
- 所有三角形索引有效（< vertexCount）
- 顶点数组非空
- 索引数量是3的倍数
- 法向量归一化
- 包围盒紧密包含所有顶点
- 无退化三角形（面积 > ε）

**验证**:
```java
private static int[] validateIndices(int[] indices, int vertexCount) {
    if (indices.length % 3 != 0) {
        throw new IllegalArgumentException("Index count must be multiple of 3");
    }
    
    for (int i = 0; i < indices.length; i++) {
        if (indices[i] < 0 || indices[i] >= vertexCount) {
            throw new IllegalArgumentException(
                "Invalid index " + indices[i] + " at position " + i
            );
        }
    }
    
    return indices;
}
```

---

## Lifecycle

### 对象创建

**不可变对象**: Mesh是不可变的
```java
public final class Mesh {
    private final Vertex[] vertices;
    private final int[] indices;
    
    public Mesh(Vertex[] vertices, int[] indices) {
        this.vertices = vertices.clone();  // 防御性拷贝
        this.indices = indices.clone();
    }
    
    public Vertex[] vertices() {
        return vertices.clone();  // 返回副本
    }
}
```

**Builder模式**: 构建复杂网格
```java
public class MeshBuilder {
    private final List<Vertex> vertices = new ArrayList<>();
    private final List<Integer> indices = new ArrayList<>();
    
    public MeshBuilder addVertex(Vec3 pos, Vec3 normal) {
        vertices.add(new Vertex(pos, normal));
        return this;
    }
    
    public MeshBuilder addTriangle(int i1, int i2, int i3) {
        indices.add(i1);
        indices.add(i2);
        indices.add(i3);
        return this;
    }
    
    public Mesh build() {
        return new Mesh(
            vertices.toArray(Vertex[]::new),
            indices.stream().mapToInt(Integer::intValue).toArray()
        );
    }
}
```

### 状态管理

**无状态生成**: 网格生成是纯函数
```java
public class MeshGenerator {
    public static Mesh generate(Shape shape, MeshingOptions options) {
        // 纯函数：相同输入→相同输出
        // 不修改shape
        // 不依赖外部状态
        
        MeshBuilder builder = new MeshBuilder();
        tessellate(shape, options, builder);
        return builder.build();
    }
}
```

**缓存**: 可以缓存网格生成结果
```java
public class CachedMeshGenerator {
    private final Map<ShapeHash, Mesh> cache = new HashMap<>();
    
    public Mesh generate(Shape shape, MeshingOptions options) {
        ShapeHash hash = ShapeHash.of(shape, options);
        return cache.computeIfAbsent(hash, h -> 
            MeshGenerator.generate(shape, options)
        );
    }
}
```

---

## Error Handling

### 异常类型

1. **MeshException** - 网格系统通用异常
   ```java
   public class MeshException extends RuntimeException {
       public MeshException(String message) { super(message); }
       public MeshException(String message, Throwable cause) { super(message, cause); }
   }
   ```

2. **InvalidMeshException** - 网格数据无效
   ```java
   public class InvalidMeshException extends MeshException {
       private final String validationError;
       
       public InvalidMeshException(String error) {
           super("Invalid mesh: " + error);
           this.validationError = error;
       }
   }
   ```

3. **MeshGenerationException** - 网格生成失败
   ```java
   public class MeshGenerationException extends MeshException {
       private final Shape failedShape;
       
       public MeshGenerationException(Shape shape, String reason, Throwable cause) {
           super("Failed to generate mesh from " + shape.getClass().getSimpleName() 
                 + ": " + reason, cause);
           this.failedShape = shape;
       }
   }
   ```

### 错误处理策略

**验证时快速失败**:
```java
public Mesh(Vertex[] vertices, int[] indices) {
    if (vertices == null || vertices.length == 0) {
        throw new InvalidMeshException("Vertices cannot be null or empty");
    }
    
    if (indices == null || indices.length == 0) {
        throw new InvalidMeshException("Indices cannot be null or empty");
    }
    
    if (indices.length % 3 != 0) {
        throw new InvalidMeshException("Index count must be multiple of 3");
    }
    
    this.vertices = validateVertices(vertices);
    this.indices = validateIndices(indices, vertices.length);
}
```

**生成失败回退**:
```java
public Mesh generateWithFallback(Shape shape, MeshingOptions options) {
    try {
        return generate(shape, options);
    } catch (MeshGenerationException e) {
        // 尝试降低精度重新生成
        MeshingOptions relaxed = options.withTolerance(options.tolerance() * 2);
        return generate(shape, relaxed);
    }
}
```

---

## Performance Requirements

### 时间复杂度

| 操作 | 复杂度 | 说明 |
|------|--------|------|
| 生成网格 | O(n) | n = 表面采样点数 |
| 顶点去重 | O(n log n) | 使用空间索引 |
| 法向平滑 | O(n·k) | k = 平均邻居数 |
| 包围盒 | O(n) | 遍历所有顶点 |
| 导出OBJ | O(n) | 线性写入 |

### 性能目标

- **简单形状网格生成**: < 10ms (< 1000三角形)
- **复杂形状网格生成**: < 100ms (< 10000三角形)
- **网格优化**: < 50ms
- **OBJ导出**: < 20ms

### 内存使用

```java
// 估算网格内存占用
public class MeshMemoryEstimator {
    public static long estimate(Mesh mesh) {
        long vertexSize = mesh.vertexCount() * VERTEX_SIZE;
        long indexSize = mesh.triangleCount() * 3 * Integer.BYTES;
        return vertexSize + indexSize;
    }
    
    private static final int VERTEX_SIZE = 
        3 * Double.BYTES +  // position
        3 * Double.BYTES +  // normal
        2 * Double.BYTES +  // UV (optional)
        4 * Byte.BYTES;     // color (optional)
}
```

### 优化策略

```java
// 1. 顶点去重（减少内存和带宽）
public Mesh deduplicateVertices(Mesh mesh) {
    Map<Vertex, Integer> uniqueVertices = new HashMap<>();
    List<Vertex> newVertices = new ArrayList<>();
    int[] newIndices = new int[mesh.indices().length];
    
    for (int i = 0; i < mesh.indices().length; i++) {
        Vertex v = mesh.vertices()[mesh.indices()[i]];
        int newIndex = uniqueVertices.computeIfAbsent(v, vertex -> {
            newVertices.add(vertex);
            return newVertices.size() - 1;
        });
        newIndices[i] = newIndex;
    }
    
    return new Mesh(newVertices.toArray(Vertex[]::new), newIndices);
}

// 2. 空间索引（加速邻居查询）
public class SpatialIndex {
    private final Map<GridCell, List<Integer>> grid = new HashMap<>();
    
    public void insert(int vertexIndex, Vec3 position) {
        GridCell cell = GridCell.of(position, cellSize);
        grid.computeIfAbsent(cell, k -> new ArrayList<>()).add(vertexIndex);
    }
    
    public List<Integer> query(Vec3 position, double radius) {
        // 只查询附近grid cell
    }
}

// 3. 批量生成（减少对象创建）
public List<Mesh> generateBatch(List<Shape> shapes, MeshingOptions options) {
    return shapes.parallelStream()
        .map(shape -> generate(shape, options))
        .toList();
}
```

---

## Meshing Algorithms

### 表面细分

```java
public interface Tessellator {
    /**
     * 将参数曲面细分为三角网格
     * 
     * @param surface 参数曲面
     * @param tolerance 细分容差（mm）
     * @return 三角网格
     */
    Mesh tessellate(Surface surface, double tolerance);
}

// 实现：Adaptive subdivision
public class AdaptiveTessellator implements Tessellator {
    @Override
    public Mesh tessellate(Surface surface, double tolerance) {
        MeshBuilder builder = new MeshBuilder();
        subdivide(surface, 0, 1, 0, 1, tolerance, builder);
        return builder.build();
    }
    
    private void subdivide(Surface surf, 
                          double u0, double u1, 
                          double v0, double v1,
                          double tol, MeshBuilder builder) {
        // 计算曲率
        double curvature = estimateCurvature(surf, u0, u1, v0, v1);
        
        if (curvature < tol) {
            // 曲率小，直接生成四边形（两个三角形）
            emitQuad(surf, u0, u1, v0, v1, builder);
        } else {
            // 递归细分
            double uMid = (u0 + u1) / 2;
            double vMid = (v0 + v1) / 2;
            subdivide(surf, u0, uMid, v0, vMid, tol, builder);
            subdivide(surf, uMid, u1, v0, vMid, tol, builder);
            subdivide(surf, u0, uMid, vMid, v1, tol, builder);
            subdivide(surf, uMid, u1, vMid, v1, tol, builder);
        }
    }
}
```

### 法向计算

```java
public class NormalCalculator {
    /**
     * 计算平面法向（每个三角形独立）
     */
    public static Mesh computeFlatNormals(Mesh mesh) {
        Vertex[] vertices = mesh.vertices();
        int[] indices = mesh.indices();
        
        Vertex[] newVertices = new Vertex[vertices.length];
        
        for (int i = 0; i < indices.length; i += 3) {
            Vec3 p0 = vertices[indices[i]].position();
            Vec3 p1 = vertices[indices[i+1]].position();
            Vec3 p2 = vertices[indices[i+2]].position();
            
            Vec3 normal = computeTriangleNormal(p0, p1, p2);
            
            newVertices[indices[i]] = vertices[indices[i]].withNormal(normal);
            newVertices[indices[i+1]] = vertices[indices[i+1]].withNormal(normal);
            newVertices[indices[i+2]] = vertices[indices[i+2]].withNormal(normal);
        }
        
        return new Mesh(newVertices, indices);
    }
    
    /**
     * 计算平滑法向（相邻三角形法向平均）
     */
    public static Mesh computeSmoothNormals(Mesh mesh) {
        Vertex[] vertices = mesh.vertices();
        int[] indices = mesh.indices();
        
        // 累积每个顶点的法向
        Vec3[] normals = new Vec3[vertices.length];
        Arrays.fill(normals, Vec3.ZERO);
        
        for (int i = 0; i < indices.length; i += 3) {
            Vec3 p0 = vertices[indices[i]].position();
            Vec3 p1 = vertices[indices[i+1]].position();
            Vec3 p2 = vertices[indices[i+2]].position();
            
            Vec3 faceNormal = computeTriangleNormal(p0, p1, p2);
            
            normals[indices[i]] = normals[indices[i]].add(faceNormal);
            normals[indices[i+1]] = normals[indices[i+1]].add(faceNormal);
            normals[indices[i+2]] = normals[indices[i+2]].add(faceNormal);
        }
        
        // 归一化
        Vertex[] newVertices = new Vertex[vertices.length];
        for (int i = 0; i < vertices.length; i++) {
            newVertices[i] = vertices[i].withNormal(normals[i].normalize());
        }
        
        return new Mesh(newVertices, indices);
    }
    
    private static Vec3 computeTriangleNormal(Vec3 p0, Vec3 p1, Vec3 p2) {
        Vec3 e1 = p1.subtract(p0);
        Vec3 e2 = p2.subtract(p0);
        return e1.cross(e2).normalize();
    }
}
```

---

## Examples

### ✅ 正确用法

```java
// 示例1: 从Shape生成网格
Shape shape = /* ... */;
MeshingOptions options = MeshingOptions.DEFAULT;
Mesh mesh = MeshGenerator.generate(shape, options);

// 示例2: 构建网格
MeshBuilder builder = new MeshBuilder();
builder.addVertex(new Vec3(0, 0, 0), Vec3.UNIT_Z);
builder.addVertex(new Vec3(1, 0, 0), Vec3.UNIT_Z);
builder.addVertex(new Vec3(0, 1, 0), Vec3.UNIT_Z);
builder.addTriangle(0, 1, 2);
Mesh mesh = builder.build();

// 示例3: 优化网格
Mesh optimized = MeshOptimizer.optimize(mesh, OptimizationLevel.HIGH);

// 示例4: 导出网格
String obj = MeshExporter.toOBJ(mesh);
Files.writeString(Path.of("output.obj"), obj);

// 示例5: 平滑法向
Mesh smooth = NormalCalculator.computeSmoothNormals(mesh);
```

### ❌ 错误用法

```java
// 错误1: 修改不可变网格
Mesh mesh = /* ... */;
mesh.vertices()[0] = new Vertex(...);  // ❌ 返回的是副本，修改无效

// 错误2: 直接依赖参数
public Mesh generateDoorMesh(ParameterSet params) {  // ❌ 不应该直接读参数
    double width = params.get("width").asLength();
    // ...
}

// 正确: 接受Shape
public Mesh generateMesh(Shape doorShape) {
    // Mesh不关心参数如何变成Shape
}

// 错误3: 在Mesh模块中渲染
public void render(Mesh mesh) {  // ❌ 渲染不属于Mesh职责
    GL11.glBegin(GL11.GL_TRIANGLES);
    // ...
}

// 正确: 提供渲染数据
public VertexBuffer prepareForRendering(Mesh mesh) {
    return VertexBuffer.fromMesh(mesh);
}

// 错误4: 无效索引
int[] indices = {0, 1, 2, 3};  // ❌ 索引数不是3的倍数
Mesh mesh = new Mesh(vertices, indices);  // 抛出异常
```

---

## Golden Test Integration

```java
/**
 * 将Mesh序列化为JSON用于Golden Test
 */
public class MeshSerializer {
    public static JsonObject toJson(Mesh mesh) {
        return JsonObject.builder()
            .put("version", "1.0")
            .put("vertexCount", mesh.vertexCount())
            .put("triangleCount", mesh.triangleCount())
            .put("vertices", serializeVertices(mesh.vertices()))
            .put("indices", mesh.indices())
            .put("boundingBox", serializeBoundingBox(mesh.boundingBox()))
            .build();
    }
    
    private static JsonArray serializeVertices(Vertex[] vertices) {
        JsonArray.Builder builder = JsonArray.builder();
        for (Vertex v : vertices) {
            builder.add(JsonObject.builder()
                .put("pos", serializeVec3(v.position()))
                .put("normal", serializeVec3(v.normal()))
                .build());
        }
        return builder.build();
    }
    
    public static Mesh fromJson(JsonObject json) {
        // 反序列化用于测试验证
    }
}
```

---

## Migration Guide

### 违规代码迁移

**场景1: Mesh模块中包含渲染代码**

**现状**:
```java
// aperture-mesh/.../MeshRenderer.java
public class MeshRenderer {
    public void render(Mesh mesh, MatrixStack matrices) {
        VertexConsumer consumer = /* ... */;
        for (int i = 0; i < mesh.triangleCount(); i++) {
            // 直接提交到Minecraft渲染系统
        }
    }
}
```

**迁移**:
```java
// aperture-mesh: 只提供数据准备
public class MeshData {
    public static VertexBuffer toVertexBuffer(Mesh mesh) {
        return new VertexBuffer(mesh.vertices(), mesh.indices());
    }
}

// aperture-client: 渲染实现
public class MinecraftMeshRenderer {
    public void render(VertexBuffer buffer, MatrixStack matrices) {
        // Minecraft specific rendering
    }
}
```

---

## Version History

| Version | Date | Changes |
|---------|------|---------|
| 1.0 | 2026-07-16 | 初始版本 |

---

**Status**: ✅ Active  
**Enforcement**: Manual review + CI checks (planned)
