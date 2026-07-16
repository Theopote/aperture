# Renderer Module Contract

**Version**: 1.0  
**Last Updated**: 2026-07-16  
**Status**: Active  
**Owner**: Aperture Core Team

---

## Overview

Renderer模块负责**将网格数据转换为特定平台的渲染格式**并提交到渲染系统。它是Kernel和平台渲染API之间的桥接层。

**核心原则**: Renderer是适配层，将平台无关的Mesh转换为平台特定的渲染数据，不包含几何计算或网格生成逻辑。

---

## Responsibilities

### ✅ 允许做的事

1. **数据格式转换**
   - Mesh → 平台顶点缓冲
   - Vertex → 平台顶点格式
   - 索引数组 → 索引缓冲

2. **渲染提交**
   - 提交顶点到渲染系统
   - 设置渲染状态
   - 应用材质和纹理
   - 管理渲染批次

3. **渲染状态管理**
   - 深度测试
   - 面剔除
   - 混合模式
   - 线框/实体切换

4. **视口变换**
   - 模型矩阵
   - 视图矩阵
   - 投影矩阵
   - 坐标系转换

5. **调试可视化**
   - 线框渲染
   - 法向量可视化
   - 包围盒显示
   - 顶点着色

---

## Forbidden

### ❌ 禁止做的事

1. **❌ 不能包含网格生成**
   ```java
   // 错误
   public class Renderer {
       public void render(Shape shape) {
           Mesh mesh = MeshGenerator.generate(shape);  // ❌ Renderer不生成网格
           renderMesh(mesh);
       }
   }
   
   // 正确
   public class Renderer {
       public void render(Mesh mesh) {
           // 只负责渲染已有的网格
           renderMesh(mesh);
       }
   }
   ```

2. **❌ 不能包含几何计算**
   ```java
   // 错误
   public class Renderer {
       public void renderTransformed(Mesh mesh, Matrix4 transform) {
           Mesh transformed = Geometry.transform(mesh, transform);  // ❌ 几何变换
           renderMesh(transformed);
       }
   }
   
   // 正确
   public class Renderer {
       public void render(Mesh mesh, Matrix4 modelMatrix) {
           setModelMatrix(modelMatrix);  // 变换在GPU端完成
           renderMesh(mesh);
       }
   }
   ```

3. **❌ 不能包含Opening业务逻辑**
   ```java
   // 错误
   public class Renderer {
       public void renderDoor(Door door) {  // ❌ 不应该知道Door
           // ...
       }
   }
   
   // 正确
   public class Renderer {
       public void render(Mesh mesh, RenderOptions options) {  // 通用渲染
           // ...
       }
   }
   ```

4. **❌ 不能修改输入的网格**
   ```java
   // 错误
   public void render(Mesh mesh) {
       mesh.vertices()[0] = new Vertex(...);  // ❌ 不应该修改输入
       renderMesh(mesh);
   }
   
   // 正确
   public void render(Mesh mesh) {
       // mesh是只读的
       renderMesh(mesh);
   }
   ```

5. **❌ 不能绕过Pipeline直接调用**
   ```java
   // 错误: Opening直接调用Renderer
   public class Opening {
       public void display() {
           Mesh mesh = generateMesh();
           renderer.render(mesh);  // ❌ 应该通过Pipeline
       }
   }
   
   // 正确: Pipeline提供mesh，平台层调用Renderer
   // aperture-client/...ClientRenderer.java
   public void render(PipelineResult result) {
       if (result instanceof PipelineResult.Success success) {
           Mesh mesh = success.getMesh();
           renderer.render(mesh);  // ✅ 平台层调用
       }
   }
   ```

---

## Allowed Dependencies

### ✅ 可以依赖的模块

1. **aperture-mesh** (网格数据)
   - 读取Mesh、Vertex数据结构

2. **aperture-math** (数学工具)
   - 矩阵/向量运算（视口变换）

3. **平台渲染API** (仅在platform模块)
   - OpenGL (直接使用)
   - Minecraft rendering (aperture-client)

**依赖原则**: Renderer消费Mesh，适配平台API

---

## Forbidden Dependencies

### ❌ 禁止依赖的模块

1. **❌ aperture-geometry** (几何计算)
   - 理由: Renderer不做几何计算

2. **❌ aperture-component** (组件图)
   - 理由: Renderer不关心数据如何生成

3. **❌ aperture-parameter** (参数系统)
   - 理由: Renderer不直接处理参数

4. **❌ aperture-opening** (Opening业务)
   - 理由: Renderer是通用渲染，不绑定Opening

---

## Input Types

### 📥 接受的输入

1. **网格数据**
   ```java
   // Renderer消费Mesh（来自Pipeline）
   public interface Renderer {
       void render(Mesh mesh, RenderContext context);
   }
   ```

2. **渲染上下文**
   ```java
   public class RenderContext {
       private final Matrix4 modelMatrix;
       private final Matrix4 viewMatrix;
       private final Matrix4 projectionMatrix;
       private final RenderOptions options;
       
       public Matrix4 mvpMatrix() {
           return projectionMatrix
               .multiply(viewMatrix)
               .multiply(modelMatrix);
       }
   }
   ```

3. **渲染选项**
   ```java
   public record RenderOptions(
       RenderMode mode,           // SOLID, WIREFRAME, POINTS
       boolean enableLighting,
       boolean enableTextures,
       Optional<Material> material,
       boolean showNormals,       // 调试：显示法向量
       boolean showBoundingBox    // 调试：显示包围盒
   ) {
       public static RenderOptions DEFAULT = new RenderOptions(
           RenderMode.SOLID,
           true,    // lighting
           true,    // textures
           Optional.empty(),
           false,   // no normals
           false    // no bbox
       );
   }
   
   public enum RenderMode {
       SOLID,       // 实体渲染
       WIREFRAME,   // 线框渲染
       POINTS       // 点渲染（调试）
   }
   ```

4. **材质数据**
   ```java
   public record Material(
       Color diffuse,
       Color specular,
       float shininess,
       Optional<String> textureId
   ) {}
   ```

### 输入验证

```java
public void render(Mesh mesh, RenderContext context) {
    Objects.requireNonNull(mesh, "mesh cannot be null");
    Objects.requireNonNull(context, "context cannot be null");
    
    if (mesh.vertexCount() == 0) {
        // 空网格，跳过渲染
        return;
    }
    
    renderInternal(mesh, context);
}
```

---

## Output Types

### 📤 产生的输出

Renderer不产生数据结构输出，而是**副作用**（渲染到屏幕/帧缓冲）

1. **渲染统计**
   ```java
   public record RenderStats(
       int verticesRendered,
       int trianglesRendered,
       long renderTimeMs,
       int drawCalls
   ) {}
   
   public RenderStats getLastRenderStats() {
       return lastStats;
   }
   ```

2. **GPU资源句柄**（内部管理）
   ```java
   public class VertexBufferHandle {
       private final int vboId;    // OpenGL VBO ID
       private final int vaoId;    // OpenGL VAO ID
       private final int eboId;    // OpenGL EBO ID
       
       // 不暴露给外部，仅内部使用
   }
   ```

---

## Lifecycle

### 资源管理

**GPU资源创建**:
```java
public class MeshUploader {
    /**
     * 将Mesh上传到GPU
     * 
     * @param mesh 要上传的网格
     * @return GPU资源句柄
     */
    public VertexBufferHandle upload(Mesh mesh) {
        // 创建VBO
        int vboId = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vboId);
        
        FloatBuffer vertexData = prepareVertexData(mesh);
        glBufferData(GL_ARRAY_BUFFER, vertexData, GL_STATIC_DRAW);
        
        // 创建EBO（索引缓冲）
        int eboId = glGenBuffers();
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, eboId);
        
        IntBuffer indexData = prepareIndexData(mesh);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, indexData, GL_STATIC_DRAW);
        
        // 创建VAO（顶点数组对象）
        int vaoId = glGenVertexArrays();
        glBindVertexArray(vaoId);
        
        setupVertexAttributes();
        
        return new VertexBufferHandle(vboId, vaoId, eboId, mesh.triangleCount());
    }
    
    private FloatBuffer prepareVertexData(Mesh mesh) {
        FloatBuffer buffer = BufferUtils.createFloatBuffer(
            mesh.vertexCount() * VERTEX_SIZE_FLOATS
        );
        
        for (Vertex v : mesh.vertices()) {
            // Position
            buffer.put((float) v.position().x());
            buffer.put((float) v.position().y());
            buffer.put((float) v.position().z());
            
            // Normal
            buffer.put((float) v.normal().x());
            buffer.put((float) v.normal().y());
            buffer.put((float) v.normal().z());
            
            // UV (if present)
            v.uv().ifPresentOrElse(
                uv -> {
                    buffer.put((float) uv.u());
                    buffer.put((float) uv.v());
                },
                () -> {
                    buffer.put(0.0f);
                    buffer.put(0.0f);
                }
            );
        }
        
        buffer.flip();
        return buffer;
    }
    
    private static final int VERTEX_SIZE_FLOATS = 3 + 3 + 2;  // pos + normal + uv
}
```

**GPU资源释放**:
```java
public class MeshUploader {
    public void delete(VertexBufferHandle handle) {
        glDeleteBuffers(handle.vboId());
        glDeleteBuffers(handle.eboId());
        glDeleteVertexArrays(handle.vaoId());
    }
}

// 使用RAII模式
public class AutoReleasedMesh implements AutoCloseable {
    private final VertexBufferHandle handle;
    
    public AutoReleasedMesh(Mesh mesh, MeshUploader uploader) {
        this.handle = uploader.upload(mesh);
    }
    
    @Override
    public void close() {
        uploader.delete(handle);
    }
}

// 使用
try (AutoReleasedMesh gpuMesh = new AutoReleasedMesh(mesh, uploader)) {
    renderer.render(gpuMesh.handle());
}
```

### 渲染循环

```java
public class OpeningRenderer {
    private final Map<Mesh, VertexBufferHandle> uploadedMeshes = new HashMap<>();
    
    public void render(Mesh mesh, RenderContext context) {
        // 获取或上传网格
        VertexBufferHandle handle = uploadedMeshes.computeIfAbsent(
            mesh, 
            m -> uploader.upload(m)
        );
        
        // 设置渲染状态
        setupRenderState(context.options());
        
        // 设置矩阵
        setMVPMatrix(context.mvpMatrix());
        
        // 绘制
        glBindVertexArray(handle.vaoId());
        glDrawElements(GL_TRIANGLES, handle.triangleCount() * 3, GL_UNSIGNED_INT, 0);
        glBindVertexArray(0);
    }
    
    public void cleanup() {
        // 释放所有GPU资源
        uploadedMeshes.values().forEach(uploader::delete);
        uploadedMeshes.clear();
    }
}
```

---

## Error Handling

### 异常类型

1. **RenderException** - 渲染系统通用异常
   ```java
   public class RenderException extends RuntimeException {
       public RenderException(String message) { super(message); }
       public RenderException(String message, Throwable cause) { super(message, cause); }
   }
   ```

2. **GPUResourceException** - GPU资源创建/释放失败
   ```java
   public class GPUResourceException extends RenderException {
       private final String resourceType;
       
       public GPUResourceException(String resourceType, String reason) {
           super("Failed to create GPU resource '" + resourceType + "': " + reason);
           this.resourceType = resourceType;
       }
   }
   ```

### 错误处理策略

**GPU资源检查**:
```java
public VertexBufferHandle upload(Mesh mesh) {
    int vboId = glGenBuffers();
    if (vboId == 0) {
        throw new GPUResourceException("VBO", "glGenBuffers returned 0");
    }
    
    glBindBuffer(GL_ARRAY_BUFFER, vboId);
    
    int error = glGetError();
    if (error != GL_NO_ERROR) {
        glDeleteBuffers(vboId);
        throw new GPUResourceException("VBO", "OpenGL error: " + error);
    }
    
    // ...
}
```

**渲染回退**:
```java
public void render(Mesh mesh, RenderContext context) {
    try {
        renderWithShaders(mesh, context);
    } catch (RenderException e) {
        // 回退到固定管线
        logger.warn("Shader rendering failed, falling back to fixed pipeline", e);
        renderWithFixedPipeline(mesh, context);
    }
}
```

---

## Performance Requirements

### 性能目标

- **Mesh上传到GPU**: < 10ms (< 10000三角形)
- **单次渲染调用**: < 1ms
- **完整帧渲染**: < 16ms (60 FPS)
- **顶点缓冲重用**: 避免每帧重新上传

### 优化策略

```java
// 1. 批量渲染（减少draw call）
public class BatchRenderer {
    private final List<RenderBatch> batches = new ArrayList<>();
    
    public void addMesh(Mesh mesh, Matrix4 transform, Material material) {
        // 按材质分组
        RenderBatch batch = findOrCreateBatch(material);
        batch.addInstance(mesh, transform);
    }
    
    public void flush() {
        for (RenderBatch batch : batches) {
            batch.render();  // 一次draw call渲染多个实例
        }
        batches.clear();
    }
}

// 2. 实例化渲染（多个相同mesh）
public void renderInstanced(Mesh mesh, List<Matrix4> transforms) {
    VertexBufferHandle handle = uploadedMeshes.get(mesh);
    
    // 上传实例数据（变换矩阵）
    int instanceVBO = uploadInstanceData(transforms);
    
    // 实例化绘制
    glBindVertexArray(handle.vaoId());
    glDrawElementsInstanced(
        GL_TRIANGLES, 
        handle.triangleCount() * 3, 
        GL_UNSIGNED_INT, 
        0,
        transforms.size()
    );
}

// 3. 视锥剔除（不渲染屏幕外的对象）
public void render(List<Mesh> meshes, RenderContext context) {
    Frustum frustum = Frustum.fromMatrix(context.mvpMatrix());
    
    for (Mesh mesh : meshes) {
        if (frustum.intersects(mesh.boundingBox())) {
            renderMesh(mesh, context);
        }
        // 否则跳过
    }
}

// 4. LOD（距离自适应）
public void renderWithLOD(Mesh mesh, float distance, RenderContext context) {
    Mesh lodMesh = selectLOD(mesh, distance);
    renderMesh(lodMesh, context);
}

private Mesh selectLOD(Mesh mesh, float distance) {
    if (distance < 10.0f) {
        return mesh;  // 高精度
    } else if (distance < 50.0f) {
        return lodCache.get(mesh, LODLevel.MEDIUM);
    } else {
        return lodCache.get(mesh, LODLevel.LOW);
    }
}
```

---

## Platform Integration

### Minecraft Integration (aperture-client)

```java
/**
 * Minecraft平台的Renderer实现
 */
public class MinecraftMeshRenderer implements Renderer {
    @Override
    public void render(Mesh mesh, RenderContext context) {
        MatrixStack matrices = context.getMinecraftMatrices();
        VertexConsumerProvider.Immediate buffers = context.getBufferProvider();
        
        VertexConsumer consumer = buffers.getBuffer(RenderLayer.getSolid());
        
        int[] indices = mesh.indices();
        Vertex[] vertices = mesh.vertices();
        
        for (int i = 0; i < indices.length; i += 3) {
            Vertex v0 = vertices[indices[i]];
            Vertex v1 = vertices[indices[i+1]];
            Vertex v2 = vertices[indices[i+2]];
            
            emitVertex(consumer, matrices, v0);
            emitVertex(consumer, matrices, v1);
            emitVertex(consumer, matrices, v2);
        }
    }
    
    private void emitVertex(VertexConsumer consumer, MatrixStack matrices, Vertex v) {
        consumer.vertex(matrices.peek().getPositionMatrix(), 
                       (float) v.position().x(),
                       (float) v.position().y(),
                       (float) v.position().z())
               .color(255, 255, 255, 255)
               .normal(matrices.peek().getNormalMatrix(),
                      (float) v.normal().x(),
                      (float) v.normal().y(),
                      (float) v.normal().z())
               .next();
    }
}
```

### OpenGL Direct Rendering

```java
/**
 * 直接OpenGL渲染（用于编辑器预览）
 */
public class OpenGLRenderer implements Renderer {
    private ShaderProgram shader;
    
    @Override
    public void render(Mesh mesh, RenderContext context) {
        shader.use();
        
        // 设置uniform
        shader.setUniform("u_MVP", context.mvpMatrix());
        shader.setUniform("u_Model", context.modelMatrix());
        
        // 渲染
        VertexBufferHandle handle = getOrUpload(mesh);
        
        glBindVertexArray(handle.vaoId());
        
        if (context.options().mode() == RenderMode.WIREFRAME) {
            glPolygonMode(GL_FRONT_AND_BACK, GL_LINE);
        }
        
        glDrawElements(GL_TRIANGLES, handle.triangleCount() * 3, GL_UNSIGNED_INT, 0);
        
        if (context.options().mode() == RenderMode.WIREFRAME) {
            glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);
        }
        
        glBindVertexArray(0);
    }
}
```

---

## Examples

### ✅ 正确用法

```java
// 示例1: 基本渲染
Mesh mesh = /* from Pipeline */;
RenderContext context = new RenderContext(
    Matrix4.identity(),      // model
    camera.viewMatrix(),     // view
    camera.projectionMatrix(), // projection
    RenderOptions.DEFAULT
);

renderer.render(mesh, context);

// 示例2: 线框渲染（调试）
RenderOptions wireframe = new RenderOptions(
    RenderMode.WIREFRAME,
    false,  // no lighting
    false,  // no textures
    Optional.empty(),
    false,  // no normals
    true    // show bbox
);

RenderContext debugContext = context.withOptions(wireframe);
renderer.render(mesh, debugContext);

// 示例3: 多个网格渲染
List<Mesh> meshes = /* ... */;
for (Mesh mesh : meshes) {
    renderer.render(mesh, context);
}

// 示例4: 实例化渲染（多个相同mesh）
List<Matrix4> transforms = List.of(
    Matrix4.translation(0, 0, 0),
    Matrix4.translation(2, 0, 0),
    Matrix4.translation(4, 0, 0)
);

renderer.renderInstanced(mesh, transforms, context);

// 示例5: 资源管理
try (AutoReleasedMesh gpuMesh = new AutoReleasedMesh(mesh, uploader)) {
    renderer.render(gpuMesh.handle(), context);
}  // 自动释放GPU资源
```

### ❌ 错误用法

```java
// 错误1: 在Renderer中生成网格
public class Renderer {
    public void render(Shape shape) {
        Mesh mesh = MeshGenerator.generate(shape);  // ❌ 不应该生成网格
        renderMesh(mesh);
    }
}

// 正确: Renderer只接受Mesh
public class Renderer {
    public void render(Mesh mesh) {
        renderMesh(mesh);
    }
}

// 错误2: 修改输入网格
public void render(Mesh mesh) {
    mesh.vertices()[0] = new Vertex(...);  // ❌ 不应该修改
    renderMesh(mesh);
}

// 错误3: 不释放GPU资源
public void render(Mesh mesh) {
    VertexBufferHandle handle = uploader.upload(mesh);
    renderInternal(handle);
    // ❌ 忘记删除handle，导致GPU内存泄漏
}

// 正确: 使用缓存或RAII
private final Map<Mesh, VertexBufferHandle> cache = new HashMap<>();

public void render(Mesh mesh) {
    VertexBufferHandle handle = cache.computeIfAbsent(mesh, uploader::upload);
    renderInternal(handle);
}

public void cleanup() {
    cache.values().forEach(uploader::delete);
    cache.clear();
}

// 错误4: Opening直接调用Renderer
public class Opening {
    public void display() {
        renderer.render(generateMesh());  // ❌ 应该通过Pipeline
    }
}
```

---

## Debug Visualization

```java
/**
 * 调试可视化功能
 */
public class DebugRenderer {
    /**
     * 渲染法向量（调试）
     */
    public void renderNormals(Mesh mesh, RenderContext context) {
        for (Vertex v : mesh.vertices()) {
            Vec3 start = v.position();
            Vec3 end = start.add(v.normal().scale(0.1));  // 0.1单位长度
            
            renderLine(start, end, Color.BLUE, context);
        }
    }
    
    /**
     * 渲染包围盒（调试）
     */
    public void renderBoundingBox(AABB bbox, RenderContext context) {
        Vec3 min = bbox.min();
        Vec3 max = bbox.max();
        
        // 8个顶点
        Vec3[] corners = {
            new Vec3(min.x(), min.y(), min.z()),
            new Vec3(max.x(), min.y(), min.z()),
            new Vec3(max.x(), max.y(), min.z()),
            new Vec3(min.x(), max.y(), min.z()),
            new Vec3(min.x(), min.y(), max.z()),
            new Vec3(max.x(), min.y(), max.z()),
            new Vec3(max.x(), max.y(), max.z()),
            new Vec3(min.x(), max.y(), max.z())
        };
        
        // 12条边
        int[][] edges = {
            {0,1}, {1,2}, {2,3}, {3,0},  // 底面
            {4,5}, {5,6}, {6,7}, {7,4},  // 顶面
            {0,4}, {1,5}, {2,6}, {3,7}   // 竖边
        };
        
        for (int[] edge : edges) {
            renderLine(corners[edge[0]], corners[edge[1]], Color.GREEN, context);
        }
    }
    
    /**
     * 渲染顶点（调试）
     */
    public void renderVertices(Mesh mesh, RenderContext context) {
        for (Vertex v : mesh.vertices()) {
            renderPoint(v.position(), 5.0f, Color.RED, context);
        }
    }
}
```

---

## Migration Guide

### 违规代码迁移

**场景1: Renderer中包含网格生成**

**现状**:
```java
// aperture-renderer/.../MeshRenderer.java
public class MeshRenderer {
    public void render(Shape shape, RenderContext ctx) {
        Mesh mesh = MeshGenerator.generate(shape);  // ❌ 不应该生成
        renderMesh(mesh, ctx);
    }
}
```

**迁移**:
```java
// aperture-renderer: 只负责渲染
public class MeshRenderer {
    public void render(Mesh mesh, RenderContext ctx) {
        renderMesh(mesh, ctx);
    }
}

// aperture-client: 从Pipeline获取mesh
public void renderOpening(PipelineResult result, RenderContext ctx) {
    if (result instanceof PipelineResult.Success success) {
        Mesh mesh = success.getMesh();
        meshRenderer.render(mesh, ctx);
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
