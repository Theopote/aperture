# 01 — Geometry Kernel

**Layer**: Kernel  
**Status**: 🎯 CRITICAL — Pure Geometry Foundation  
**Dependencies**: aperture-math

---

## Overview

The Geometry Kernel is Aperture's **pure mathematical foundation** for 3D geometry. It provides:

- **Primitives**: Point, Vector, Plane, BoundingBox
- **Curves**: Line, Arc, Bezier, Polyline
- **Profiles**: 2D closed curves for extrusion
- **Surfaces**: Planar, Extrusion, Revolution
- **Solids**: CSG (Constructive Solid Geometry) operations
- **Transformations**: Translation, Rotation, Scale, Matrix
- **Topology**: Vertex, Edge, Face, Solid relationships

**Design Principle**: **Minecraft-free, coordinate-system agnostic, purely mathematical.**

The kernel operates in **millimeters** internally. Platform layer handles conversion to Minecraft blocks.

---

## Why Geometry Kernel?

### The Problem

Generating architectural geometry requires:
- **Precise positioning** (frame corners must align perfectly)
- **Boolean operations** (glass fits inside frame by subtraction)
- **Extrusion** (frame profiles swept along paths)
- **Transformations** (doors rotated, windows mirrored)
- **Collision detection** (footprint calculation)

Minecraft's block grid is **insufficient** for architectural precision. A 50mm frame cannot be represented as 0.05 blocks cleanly.

### The Solution

**Separate geometry calculation from Minecraft representation.**

```
Geometry Kernel (millimeters, pure math)
    ↓
Voxelization (convert to Minecraft blocks)
    ↓
Rendering (BlockState placement)
```

This allows:
- ✅ Frame profiles with sub-block precision
- ✅ Glass perfectly fitted to frame opening
- ✅ Clean corners and miters
- ✅ Accurate collision bounds
- ✅ Reusable geometry logic (non-Minecraft contexts)

---

## Coordinate System

### Units

**Internal**: millimeters (mm)
- `width: 1200` = 1200mm = 1.2m
- `frame_thickness: 50` = 50mm = 0.05m

**Minecraft**: blocks (1 block = 1000mm = 1m)
- Conversion: `blocks = mm / 1000.0`

**Rationale**: Millimeters avoid floating-point precision issues in architectural dimensions.

### Coordinate Space

**World Space** (global):
- Origin: Minecraft world origin (0, 0, 0)
- Axes: +X (east), +Y (up), +Z (south)

**Local Space** (opening-relative):
- Origin: Opening anchor point
- Axes: +X (right), +Y (up), +Z (forward/outward)

**Transformations** convert between spaces:
```java
Point worldPos = localToWorld.transform(localPos);
Point localPos = worldToLocal.transform(worldPos);
```

---

## Core Types

### Point3D

**Purpose**: Position in 3D space.

```java
public record Point3D(double x, double y, double z) {
    public static final Point3D ORIGIN = new Point3D(0, 0, 0);
    
    public Point3D add(Vector3D v) {
        return new Point3D(x + v.x(), y + v.y(), z + v.z());
    }
    
    public Vector3D subtract(Point3D other) {
        return new Vector3D(x - other.x(), y - other.y(), z - other.z());
    }
    
    public double distanceTo(Point3D other) {
        return subtract(other).length();
    }
}
```

**Current Status**: ✅ Exists (aperture-math)

---

### Vector3D

**Purpose**: Direction and magnitude in 3D space.

```java
public record Vector3D(double x, double y, double z) {
    public static final Vector3D ZERO = new Vector3D(0, 0, 0);
    public static final Vector3D X_AXIS = new Vector3D(1, 0, 0);
    public static final Vector3D Y_AXIS = new Vector3D(0, 1, 0);
    public static final Vector3D Z_AXIS = new Vector3D(0, 0, 1);
    
    public double length() {
        return Math.sqrt(x * x + y * y + z * z);
    }
    
    public Vector3D normalize() {
        double len = length();
        return new Vector3D(x / len, y / len, z / len);
    }
    
    public double dot(Vector3D other) {
        return x * other.x() + y * other.y() + z * other.z();
    }
    
    public Vector3D cross(Vector3D other) {
        return new Vector3D(
            y * other.z() - z * other.y(),
            z * other.x() - x * other.z(),
            x * other.y() - y * other.x()
        );
    }
    
    public Vector3D scale(double s) {
        return new Vector3D(x * s, y * s, z * s);
    }
}
```

**Current Status**: ✅ Exists (aperture-math)

---

### BoundingBox

**Purpose**: Axis-aligned bounding box for collision and culling.

```java
public record BoundingBox(Point3D min, Point3D max) {
    public static BoundingBox fromPoints(List<Point3D> points) {
        double minX = points.stream().mapToDouble(Point3D::x).min().orElse(0);
        double minY = points.stream().mapToDouble(Point3D::y).min().orElse(0);
        double minZ = points.stream().mapToDouble(Point3D::z).min().orElse(0);
        double maxX = points.stream().mapToDouble(Point3D::x).max().orElse(0);
        double maxY = points.stream().mapToDouble(Point3D::y).max().orElse(0);
        double maxZ = points.stream().mapToDouble(Point3D::z).max().orElse(0);
        return new BoundingBox(
            new Point3D(minX, minY, minZ),
            new Point3D(maxX, maxY, maxZ)
        );
    }
    
    public boolean contains(Point3D point) {
        return point.x() >= min.x() && point.x() <= max.x() &&
               point.y() >= min.y() && point.y() <= max.y() &&
               point.z() >= min.z() && point.z() <= max.z();
    }
    
    public boolean intersects(BoundingBox other) {
        return !(max.x() < other.min.x() || min.x() > other.max.x() ||
                 max.y() < other.min.y() || min.y() > other.max.y() ||
                 max.z() < other.min.z() || min.z() > other.max.z());
    }
    
    public BoundingBox expand(double amount) {
        return new BoundingBox(
            new Point3D(min.x() - amount, min.y() - amount, min.z() - amount),
            new Point3D(max.x() + amount, max.y() + amount, max.z() + amount)
        );
    }
    
    public Point3D center() {
        return new Point3D(
            (min.x() + max.x()) / 2,
            (min.y() + max.y()) / 2,
            (min.z() + max.z()) / 2
        );
    }
    
    public Vector3D size() {
        return new Vector3D(
            max.x() - min.x(),
            max.y() - min.y(),
            max.z() - min.z()
        );
    }
}
```

**Current Status**: ✅ Exists (aperture-core)

---

### Transform

**Purpose**: 4×4 transformation matrix for position, rotation, scale.

```java
public class Transform {
    private final double[][] matrix; // 4×4
    
    public static Transform identity() {
        return new Transform(new double[][] {
            {1, 0, 0, 0},
            {0, 1, 0, 0},
            {0, 0, 1, 0},
            {0, 0, 0, 1}
        });
    }
    
    public static Transform translation(Vector3D v) {
        return new Transform(new double[][] {
            {1, 0, 0, v.x()},
            {0, 1, 0, v.y()},
            {0, 0, 1, v.z()},
            {0, 0, 0, 1}
        });
    }
    
    public static Transform rotationY(double radians) {
        double cos = Math.cos(radians);
        double sin = Math.sin(radians);
        return new Transform(new double[][] {
            {cos, 0, sin, 0},
            {0, 1, 0, 0},
            {-sin, 0, cos, 0},
            {0, 0, 0, 1}
        });
    }
    
    public static Transform scale(double sx, double sy, double sz) {
        return new Transform(new double[][] {
            {sx, 0, 0, 0},
            {0, sy, 0, 0},
            {0, 0, sz, 0},
            {0, 0, 0, 1}
        });
    }
    
    public Point3D transform(Point3D p) {
        double x = matrix[0][0] * p.x() + matrix[0][1] * p.y() + matrix[0][2] * p.z() + matrix[0][3];
        double y = matrix[1][0] * p.x() + matrix[1][1] * p.y() + matrix[1][2] * p.z() + matrix[1][3];
        double z = matrix[2][0] * p.x() + matrix[2][1] * p.y() + matrix[2][2] * p.z() + matrix[2][3];
        return new Point3D(x, y, z);
    }
    
    public Vector3D transformVector(Vector3D v) {
        double x = matrix[0][0] * v.x() + matrix[0][1] * v.y() + matrix[0][2] * v.z();
        double y = matrix[1][0] * v.x() + matrix[1][1] * v.y() + matrix[1][2] * v.z();
        double z = matrix[2][0] * v.x() + matrix[2][1] * v.y() + matrix[2][2] * v.z();
        return new Vector3D(x, y, z);
    }
    
    public Transform compose(Transform other) {
        // Matrix multiplication
        double[][] result = new double[4][4];
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                result[i][j] = 0;
                for (int k = 0; k < 4; k++) {
                    result[i][j] += matrix[i][k] * other.matrix[k][j];
                }
            }
        }
        return new Transform(result);
    }
    
    public Transform inverse() {
        // Matrix inversion (Gauss-Jordan)
        // Implementation omitted for brevity
    }
}
```

**Current Status**: ✅ Exists (aperture-math)

---

## Curves

### Line Segment

**Purpose**: Straight line between two points.

```java
public record LineSegment(Point3D start, Point3D end) implements Curve {
    @Override
    public Point3D pointAt(double t) {
        // t ∈ [0, 1]
        return start.add(end.subtract(start).scale(t));
    }
    
    @Override
    public Vector3D tangentAt(double t) {
        return end.subtract(start).normalize();
    }
    
    @Override
    public double length() {
        return start.distanceTo(end);
    }
}
```

**Current Status**: ⏳ Partial (basic implementation exists)

---

### Arc

**Purpose**: Circular arc (for rounded corners, curved mullions).

```java
public record Arc(
    Point3D center,
    double radius,
    Vector3D normal,
    double startAngle,
    double endAngle
) implements Curve {
    @Override
    public Point3D pointAt(double t) {
        double angle = startAngle + t * (endAngle - startAngle);
        // Project onto plane defined by normal
        // Implementation requires plane math
    }
    
    @Override
    public Vector3D tangentAt(double t) {
        // Perpendicular to radius at t
    }
    
    @Override
    public double length() {
        return radius * Math.abs(endAngle - startAngle);
    }
}
```

**Current Status**: ❌ Planned (Phase 3)

---

### Bezier Curve

**Purpose**: Smooth curves for organic shapes.

```java
public record BezierCurve(List<Point3D> controlPoints) implements Curve {
    @Override
    public Point3D pointAt(double t) {
        // De Casteljau's algorithm
        return deCasteljau(controlPoints, t);
    }
    
    private Point3D deCasteljau(List<Point3D> points, double t) {
        if (points.size() == 1) return points.get(0);
        var next = new ArrayList<Point3D>();
        for (int i = 0; i < points.size() - 1; i++) {
            next.add(lerp(points.get(i), points.get(i + 1), t));
        }
        return deCasteljau(next, t);
    }
    
    private Point3D lerp(Point3D a, Point3D b, double t) {
        return a.add(b.subtract(a).scale(t));
    }
}
```

**Current Status**: ❌ Planned (Phase 4+)

---

## Profiles

### Profile

**Purpose**: 2D closed curve for extrusion (frame cross-sections).

```java
public interface Profile {
    /**
     * Vertices defining the profile shape in 2D (XY plane).
     */
    List<Point2D> vertices();
    
    /**
     * Whether the profile is closed (last vertex connects to first).
     */
    boolean isClosed();
    
    /**
     * Extrude profile along a path to create 3D geometry.
     */
    GeometrySolid extrude(Curve path);
    
    /**
     * Get bounding rectangle in 2D.
     */
    BoundingBox2D bounds();
}
```

### L-Profile (Frame)

**Example**: Standard frame profile (L-shaped).

```java
public record LProfile(double width, double depth, double thickness) implements Profile {
    @Override
    public List<Point2D> vertices() {
        return List.of(
            new Point2D(0, 0),
            new Point2D(width, 0),
            new Point2D(width, thickness),
            new Point2D(thickness, thickness),
            new Point2D(thickness, depth),
            new Point2D(0, depth)
        );
    }
    
    @Override
    public boolean isClosed() {
        return true;
    }
}
```

**Current Status**: ✅ Schema exists, ⏳ Extrusion partial

---

## Surfaces

### Planar Surface

**Purpose**: Flat surface (glass pane, panel).

```java
public record PlanarSurface(Point3D origin, Vector3D u, Vector3D v) implements Surface {
    public Point3D pointAt(double u, double v) {
        return origin.add(this.u.scale(u)).add(this.v.scale(v));
    }
    
    public Vector3D normal() {
        return u.cross(v).normalize();
    }
}
```

**Current Status**: ⏳ Partial

---

### Extruded Surface

**Purpose**: Profile swept along a curve (frame geometry).

```java
public record ExtrudedSurface(Profile profile, Curve path) implements Surface {
    public GeometrySolid toSolid() {
        // Sample profile at intervals along path
        // Connect vertices to form quads
        // Return mesh
    }
}
```

**Current Status**: ❌ Planned (Week 3)

---

## Solids

### GeometrySolid

**Purpose**: 3D volumetric geometry (the output of component generators).

```java
public interface GeometrySolid {
    /**
     * Get mesh representation (vertices + faces).
     */
    Mesh toMesh();
    
    /**
     * Get axis-aligned bounding box.
     */
    BoundingBox bounds();
    
    /**
     * Check if point is inside solid.
     */
    boolean contains(Point3D point);
    
    /**
     * Transform solid by matrix.
     */
    GeometrySolid transform(Transform t);
}
```

### Box Solid

```java
public record BoxSolid(Point3D min, Point3D max) implements GeometrySolid {
    @Override
    public Mesh toMesh() {
        // Generate 8 vertices, 12 triangles (2 per face)
    }
    
    @Override
    public BoundingBox bounds() {
        return new BoundingBox(min, max);
    }
}
```

**Current Status**: ✅ Basic implementation exists

---

## CSG (Constructive Solid Geometry)

**Purpose**: Boolean operations for complex shapes.

### Operations

**Union** (A ∪ B):
```java
GeometrySolid union(GeometrySolid a, GeometrySolid b);
```

**Subtraction** (A − B):
```java
GeometrySolid subtract(GeometrySolid a, GeometrySolid b);
```
*Example*: Frame solid − Glass cutout = Frame with opening

**Intersection** (A ∩ B):
```java
GeometrySolid intersect(GeometrySolid a, GeometrySolid b);
```

### Implementation Strategy

**Phase 1** (Kernel V1): No CSG — use manual mesh construction  
**Phase 2** (Platform V1): Basic box-box CSG (sufficient for rectangular openings)  
**Phase 3+**: Full mesh CSG using BSP trees or manifold library

**Current Status**: ❌ Planned (Phase 2)

---

## Mesh

### Mesh Representation

**Purpose**: Triangulated geometry for rendering and voxelization.

```java
public class Mesh {
    private final List<Point3D> vertices;
    private final List<Triangle> triangles;
    
    public record Triangle(int v0, int v1, int v2) {
        public Vector3D normal(List<Point3D> vertices) {
            var p0 = vertices.get(v0);
            var p1 = vertices.get(v1);
            var p2 = vertices.get(v2);
            var e1 = p1.subtract(p0);
            var e2 = p2.subtract(p0);
            return e1.cross(e2).normalize();
        }
    }
    
    public Mesh transform(Transform t) {
        var newVertices = vertices.stream()
            .map(t::transform)
            .toList();
        return new Mesh(newVertices, triangles);
    }
    
    public BoundingBox bounds() {
        return BoundingBox.fromPoints(vertices);
    }
}
```

**Current Status**: ✅ Basic implementation exists

---

## Topology

**Purpose**: Understand connectivity for advanced operations (future).

```java
// Half-edge data structure (Phase 4+)
public class HalfEdgeMesh {
    public record Vertex(Point3D position, HalfEdge outgoing) {}
    public record HalfEdge(Vertex origin, HalfEdge twin, HalfEdge next, Face face) {}
    public record Face(HalfEdge edge, Vector3D normal) {}
    
    // Enables:
    // - Mesh validation (manifold check)
    // - Edge operations (collapse, split)
    // - Subdivision surfaces
}
```

**Current Status**: ❌ Planned (Phase 4+)

---

## Voxelization

**Purpose**: Convert continuous geometry to Minecraft block grid.

### Strategy

**Sampling**: Check block centers, classify as inside/outside/boundary.

```java
public class Voxelizer {
    public BlockGrid voxelize(GeometrySolid solid, double blockSize) {
        var bounds = solid.bounds();
        var grid = new BlockGrid();
        
        // Iterate block positions in bounds
        for (double x = bounds.min().x(); x <= bounds.max().x(); x += blockSize) {
            for (double y = bounds.min().y(); y <= bounds.max().y(); y += blockSize) {
                for (double z = bounds.min().z(); z <= bounds.max().z(); z += blockSize) {
                    var center = new Point3D(x + blockSize/2, y + blockSize/2, z + blockSize/2);
                    if (solid.contains(center)) {
                        grid.set(toBlockPos(center, blockSize), BlockType.SOLID);
                    }
                }
            }
        }
        
        return grid;
    }
}
```

**Challenges**:
- **Aliasing**: Thin geometry (50mm frame) may be missed by sampling
- **Material Assignment**: Which block type for each voxel?
- **Optimization**: Only voxelize surface, not interior

**Current Status**: ⏳ Partial (basic voxelization exists)

---

## Performance Considerations

### Immutability

All geometry types are **immutable records**. Transformations return new instances.

**Rationale**:
- Thread-safe by default
- Caching-friendly (hash once, reuse)
- No defensive copying

### Lazy Evaluation

Expensive operations (mesh generation, CSG) are **deferred until needed**.

```java
public class LazyMesh {
    private final Supplier<Mesh> generator;
    private Mesh cached;
    
    public Mesh get() {
        if (cached == null) {
            cached = generator.get();
        }
        return cached;
    }
}
```

### Spatial Indexing

For large scenes, use **spatial data structures**:
- **Octree**: Hierarchical space partitioning (collision detection)
- **BVH**: Bounding volume hierarchy (ray tracing)

**Current Status**: ❌ Planned (Phase 3+)

---

## Testing Strategy

### Unit Tests

**Geometry primitives**:
```java
@Test
void transform_rotateY90_rotatesCorrectly() {
    var t = Transform.rotationY(Math.PI / 2);
    var p = new Point3D(1, 0, 0);
    var result = t.transform(p);
    assertEquals(0, result.x(), 0.001);
    assertEquals(0, result.z(), 0.001);
}
```

**Curve sampling**:
```java
@Test
void lineSegment_pointAt_interpolatesCorrectly() {
    var line = new LineSegment(Point3D.ORIGIN, new Point3D(10, 0, 0));
    assertEquals(new Point3D(5, 0, 0), line.pointAt(0.5));
}
```

### Integration Tests

**Profile extrusion**:
```java
@Test
void lProfile_extrude_generatesCorrectMesh() {
    var profile = new LProfile(50, 80, 10);
    var path = new LineSegment(Point3D.ORIGIN, new Point3D(0, 0, 1000));
    var solid = profile.extrude(path);
    
    assertTrue(solid.bounds().size().z() >= 1000);
}
```

### Visual Tests

**Golden images**: Render geometry, compare against reference images.

**Current Status**: ⏸️ Planned (Week 2)

---

## Current Status

| Feature | Status | Priority |
|---------|--------|----------|
| Point3D, Vector3D | ✅ | - |
| BoundingBox | ✅ | - |
| Transform | ✅ | - |
| LineSegment | ⏳ | HIGH |
| Arc | ❌ | MEDIUM |
| Bezier | ❌ | LOW |
| Profile (schema) | ✅ | - |
| Profile extrusion | ⏳ | 🔥 CRITICAL |
| Planar surface | ⏳ | HIGH |
| Box solid | ✅ | - |
| Mesh | ✅ | - |
| CSG operations | ❌ | HIGH |
| Voxelization | ⏳ | HIGH |
| Topology (half-edge) | ❌ | LOW |

---

## Acceptance Criteria

### For Kernel V1 (Week 2)
- [x] Point3D, Vector3D, Transform complete
- [x] BoundingBox complete
- [ ] Profile extrusion working (L-profile → 3D frame)
- [ ] Basic mesh generation (box, extruded profile)
- [ ] Voxelization (mesh → block grid)

### For Platform V1 (Phase B)
- [ ] Arc curves (rounded corners)
- [ ] Basic CSG (box-box boolean)
- [ ] Optimized voxelization (surface-only)

### For Production (Phase C+)
- [ ] Bezier curves (organic shapes)
- [ ] Full mesh CSG (any geometry)
- [ ] Half-edge topology
- [ ] Spatial indexing (octree/BVH)

---

## Related Documents

- [kernel/02-parameter-engine.md](02-parameter-engine.md) — Parameters drive geometry generation
- [kernel/03-component-system.md](03-component-system.md) — Components produce GeometrySolid
- [kernel/04-generation-pipeline.md](04-generation-pipeline.md) — Pipeline uses geometry kernel

---

**Document Status**: ✅ Complete  
**Last Updated**: 2026-07-16  
**Implementation**: ~50% (primitives complete, curves/CSG partial)  
**Next Review**: After Kernel V1 implementation
