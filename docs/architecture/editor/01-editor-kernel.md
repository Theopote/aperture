# 01 — Editor Kernel

**Layer**: Editor  
**Status**: 🎯 CRITICAL — CAD-Quality Interaction Foundation  
**Dependencies**: Kernel (geometry, parameters, commands)

---

## Overview

The Editor Kernel is Aperture's **GUI-independent interaction abstraction layer**. It provides:

- **Selection**: What is currently being edited
- **Manipulator**: Visual handles and drag math
- **Command**: Undoable edit operations
- **History**: Undo/Redo stack
- **Snap**: Constraint-based positioning
- **Inspector**: Parameter editing interface

**Design Principle**: **Separation of interaction logic from rendering.**

The Editor Kernel operates on abstract concepts (selection, gizmo transformation math, command execution). The GUI layer (Minecraft client) implements the visual representation.

```
User Input (mouse, keyboard)
    ↓
Editor Kernel (Selection, Manipulator, Command)
    ↓
Command Execution (modify parameters)
    ↓
Pipeline Invalidation (regenerate geometry)
    ↓
GUI Update (render new state)
```

---

## Why Editor Kernel?

### The Problem

CAD-quality editing requires:
- **Visual feedback** (gizmos, dimension overlays)
- **Precision input** (snap to grid, constraint projection)
- **Undo/Redo** (reversible operations)
- **Multi-mode editing** (move, resize, rotate)
- **Inspector sync** (gizmo drag ↔ text field)

Minecraft has **none of these**. We must build from scratch.

### The Solution

**Separate "what to edit" (kernel) from "how to render it" (GUI).**

```java
// Kernel: Abstract manipulation
public interface Manipulator {
    Transform computeTransform(DragEvent event);
}

// GUI: Concrete rendering
public class GizmoRenderer {
    void render(Manipulator manipulator, MatrixStack matrices);
}
```

This allows:
- ✅ Headless testing (test drag math without rendering)
- ✅ Multiple GUI implementations (in-game, external editor)
- ✅ Reusable interaction logic
- ✅ Clean command pattern

---

## Core Concepts

### Selection

**Purpose**: Track what the user is currently editing.

```java
public class EditorSelection {
    private final Set<OpeningInstance> selectedOpenings;
    private final Set<ComponentNode> selectedComponents;
    
    public boolean isEmpty() {
        return selectedOpenings.isEmpty() && selectedComponents.isEmpty();
    }
    
    public void select(OpeningInstance opening) {
        selectedOpenings.clear();
        selectedOpenings.add(opening);
        notifyListeners();
    }
    
    public void addToSelection(OpeningInstance opening) {
        selectedOpenings.add(opening);
        notifyListeners();
    }
    
    public void clear() {
        selectedOpenings.clear();
        selectedComponents.clear();
        notifyListeners();
    }
    
    public Optional<OpeningInstance> single() {
        return selectedOpenings.size() == 1 
            ? Optional.of(selectedOpenings.iterator().next())
            : Optional.empty();
    }
}
```

**Selection Modes**:
- **Single**: One opening (most common)
- **Multiple**: Multiple openings (bulk operations)
- **Component**: Individual component within opening (advanced)

**Current Status**: ✅ Basic implementation exists

---

### Manipulator

**Purpose**: Compute transformations from user input (drag, snap, constraints).

```java
public interface Manipulator {
    /**
     * Start manipulation (user clicked gizmo handle).
     */
    void begin(ManipulatorContext context);
    
    /**
     * Update manipulation (user dragging).
     */
    ManipulationResult update(DragEvent event);
    
    /**
     * Finish manipulation (user released mouse).
     */
    Command finish();
    
    /**
     * Cancel manipulation (user pressed Escape).
     */
    void cancel();
    
    /**
     * Get current gizmo state for rendering.
     */
    GizmoState getGizmoState();
}
```

### Manipulator Types

**TranslateManipulator** (Move):
```java
public class TranslateManipulator implements Manipulator {
    private Point3D startPosition;
    private Vector3D axis;  // Constrained axis (X, Y, Z, or null for free)
    
    @Override
    public ManipulationResult update(DragEvent event) {
        var delta = computeDelta(event, axis);
        var newPosition = startPosition.add(delta);
        
        // Apply snapping
        if (snapEnabled()) {
            newPosition = snapToGrid(newPosition);
        }
        
        return new ManipulationResult(newPosition, delta);
    }
    
    @Override
    public Command finish() {
        var finalDelta = currentPosition.subtract(startPosition);
        return new MoveOpeningCommand(selectedOpening, finalDelta);
    }
}
```

**ResizeManipulator** (Scale):
```java
public class ResizeManipulator implements Manipulator {
    private String parameterName;  // "width", "height"
    private double startValue;
    private Vector3D dragAxis;
    
    @Override
    public ManipulationResult update(DragEvent event) {
        var dragDistance = projectOntoAxis(event.position(), dragAxis);
        var newValue = startValue + dragDistance;
        
        // Apply constraints (min/max from parameter schema)
        newValue = clamp(newValue, parameterSchema.min(), parameterSchema.max());
        
        // Apply snapping (increment snapping: 100mm, 10mm, 1mm)
        if (snapEnabled()) {
            newValue = snapToIncrement(newValue, snapIncrement());
        }
        
        return new ManipulationResult(
            Map.of(parameterName, newValue),
            newValue - startValue
        );
    }
    
    @Override
    public Command finish() {
        return new SetParameterCommand(
            selectedOpening,
            parameterName,
            currentValue
        );
    }
}
```

**RotateManipulator** (Rotation):
```java
public class RotateManipulator implements Manipulator {
    private double startAngle;
    private Vector3D rotationAxis;
    
    @Override
    public ManipulationResult update(DragEvent event) {
        var angle = computeAngle(event.position(), rotationAxis);
        
        // Apply snapping (angle snapping: 45°, 15°, 5°, 1°)
        if (snapEnabled()) {
            angle = snapToAngle(angle, angleIncrement());
        }
        
        return new ManipulationResult(
            Transform.rotation(rotationAxis, angle),
            angle - startAngle
        );
    }
    
    @Override
    public Command finish() {
        return new RotateOpeningCommand(
            selectedOpening,
            rotationAxis,
            currentAngle - startAngle
        );
    }
}
```

**Current Status**: ⏳ Partial (interfaces exist, math incomplete)

---

## Gizmo System

### Gizmo

**Purpose**: Visual representation of manipulator (arrows, handles, dimension text).

```java
public interface Gizmo {
    /**
     * Get gizmo handles (clickable parts).
     */
    List<GizmoHandle> handles();
    
    /**
     * Check if point intersects any handle.
     */
    Optional<GizmoHandle> hitTest(Point3D point);
    
    /**
     * Get render data for current state.
     */
    GizmoRenderData getRenderData();
}
```

### GizmoHandle

```java
public record GizmoHandle(
    String id,
    GizmoHandleType type,
    Point3D position,
    Vector3D axis,
    BoundingBox bounds
) {
    public enum GizmoHandleType {
        TRANSLATE_X, TRANSLATE_Y, TRANSLATE_Z,
        RESIZE_WIDTH, RESIZE_HEIGHT,
        ROTATE_Y
    }
}
```

### Standard Gizmos

**Translation Gizmo** (3 arrows):
```
      Y (green)
      ↑
      |
      +----→ X (red)
     /
    ↙
   Z (blue)
```

**Resize Gizmo** (corner handles):
```
  +--------+
  |        | ← height handle
  |        |
  +--------+
      ↑
   width handle
```

**Rotation Gizmo** (circular arc):
```
    ⟲
   ╱ ╲
  |   |  ← rotation handle
   ╲ ╱
```

**Current Status**: ✅ Basic rendering exists, ⏳ Hit-testing incomplete

---

## Command System

**Purpose**: All edits are reversible commands (see kernel/07-command-system.md).

### Editor Commands

**SetParameterCommand**:
```java
public class SetParameterCommand implements Command {
    private final OpeningInstance opening;
    private final String parameterName;
    private final Object newValue;
    private Object oldValue;
    
    @Override
    public void execute() {
        oldValue = opening.parameters().get(parameterName);
        opening.setParameter(parameterName, newValue);
        invalidatePipeline(opening);
    }
    
    @Override
    public void undo() {
        opening.setParameter(parameterName, oldValue);
        invalidatePipeline(opening);
    }
}
```

**MoveOpeningCommand**:
```java
public class MoveOpeningCommand implements Command {
    private final OpeningInstance opening;
    private final Vector3D delta;
    
    @Override
    public void execute() {
        opening.setPosition(opening.position().add(delta));
    }
    
    @Override
    public void undo() {
        opening.setPosition(opening.position().subtract(delta));
    }
}
```

**RotateOpeningCommand**:
```java
public class RotateOpeningCommand implements Command {
    private final OpeningInstance opening;
    private final Vector3D axis;
    private final double angle;
    
    @Override
    public void execute() {
        var rotation = Transform.rotation(axis, angle);
        opening.setTransform(rotation.compose(opening.transform()));
    }
    
    @Override
    public void undo() {
        var rotation = Transform.rotation(axis, -angle);
        opening.setTransform(rotation.compose(opening.transform()));
    }
}
```

**Current Status**: ✅ Command pattern exists, ⏳ Editor commands partial

---

## History System

**Purpose**: Undo/Redo stack management.

```java
public class EditorHistory {
    private final Deque<Command> undoStack = new ArrayDeque<>();
    private final Deque<Command> redoStack = new ArrayDeque<>();
    private int maxHistorySize = 100;
    
    public void execute(Command command) {
        command.execute();
        undoStack.push(command);
        redoStack.clear();  // Executing new command clears redo stack
        
        // Limit stack size
        if (undoStack.size() > maxHistorySize) {
            undoStack.removeLast();
        }
    }
    
    public void undo() {
        if (undoStack.isEmpty()) return;
        
        var command = undoStack.pop();
        command.undo();
        redoStack.push(command);
    }
    
    public void redo() {
        if (redoStack.isEmpty()) return;
        
        var command = redoStack.pop();
        command.execute();
        undoStack.push(command);
    }
    
    public boolean canUndo() {
        return !undoStack.isEmpty();
    }
    
    public boolean canRedo() {
        return !redoStack.isEmpty();
    }
    
    public void clear() {
        undoStack.clear();
        redoStack.clear();
    }
}
```

**Keyboard Shortcuts**:
- `Ctrl+Z` → Undo
- `Ctrl+Shift+Z` (or `Ctrl+Y`) → Redo

**Current Status**: ✅ Implementation exists

---

## Snap System

**Purpose**: Constraint-based positioning for precision.

### Snap Types

**Grid Snap**:
```java
public class GridSnap implements SnapProvider {
    private double gridSize = 1000.0;  // 1 block = 1000mm
    
    @Override
    public Point3D snap(Point3D point) {
        return new Point3D(
            Math.round(point.x() / gridSize) * gridSize,
            Math.round(point.y() / gridSize) * gridSize,
            Math.round(point.z() / gridSize) * gridSize
        );
    }
}
```

**Increment Snap** (for parameters):
```java
public class IncrementSnap implements SnapProvider {
    private double increment;
    
    public double snap(double value) {
        return Math.round(value / increment) * increment;
    }
}
```

**Angle Snap** (for rotation):
```java
public class AngleSnap implements SnapProvider {
    private double angleIncrement = Math.toRadians(15);  // 15°
    
    public double snap(double angle) {
        return Math.round(angle / angleIncrement) * angleIncrement;
    }
}
```

**Geometric Snap** (future):
- Snap to edges
- Snap to vertices
- Snap to midpoints
- Snap to perpendicular/parallel

**Current Status**: ⏳ Basic grid snap exists, advanced snapping planned

---

## Inspector System

**Purpose**: Parameter editing via text fields, sliders, dropdowns.

### Inspector Interface

```java
public interface Inspector {
    /**
     * Build UI for editing opening parameters.
     */
    void buildUI(OpeningInstance opening, UIBuilder builder);
    
    /**
     * Called when parameter changed via UI.
     */
    void onParameterChanged(String parameterName, Object newValue);
}
```

### Example: Length Parameter

```java
public class LengthParameterInspector {
    public void buildUI(ParameterSchema schema, UIBuilder builder) {
        var slider = builder.slider()
            .min(schema.min())
            .max(schema.max())
            .value(schema.defaultValue())
            .onChange(this::onSliderChanged);
        
        var textField = builder.textField()
            .value(String.valueOf(schema.defaultValue()))
            .onChange(this::onTextFieldChanged);
        
        builder.row(slider, textField);
    }
    
    private void onSliderChanged(double value) {
        // Update text field
        textField.setValue(String.valueOf(value));
        
        // Execute command
        var command = new SetParameterCommand(
            selectedOpening,
            parameterName,
            value
        );
        history.execute(command);
    }
}
```

### Inspector ↔ Gizmo Sync

**Bidirectional sync**:
- User drags gizmo → Update inspector text field
- User edits text field → Update gizmo position

```java
public class InspectorGizmoSynchronizer {
    public void onGizmoDragged(String parameterName, Object newValue) {
        inspector.updateParameter(parameterName, newValue);
    }
    
    public void onInspectorEdited(String parameterName, Object newValue) {
        gizmo.updateParameter(parameterName, newValue);
        pipeline.invalidate(selectedOpening);
    }
}
```

**Current Status**: ⏳ Basic inspector exists, sync incomplete

---

## Edit Modes

**Purpose**: Different interaction paradigms.

### Mode Types

**Select Mode** (default):
- Click to select opening
- Drag to move
- Show bounding box gizmo

**Resize Mode**:
- Show resize handles
- Drag to change width/height
- Show dimension overlays

**Rotate Mode**:
- Show rotation gizmo
- Drag to rotate
- Show angle overlay

**Multi-Edit Mode**:
- Select multiple openings
- Bulk operations (delete, move, copy)

### Mode State Machine

```java
public class EditorModeController {
    private EditorMode currentMode = EditorMode.SELECT;
    
    public void setMode(EditorMode mode) {
        currentMode.onExit();
        currentMode = mode;
        currentMode.onEnter();
    }
    
    public void handleInput(InputEvent event) {
        currentMode.handleInput(event);
    }
}
```

**Keyboard Shortcuts**:
- `V` → Select mode
- `R` → Resize mode
- `E` → Rotate mode

**Current Status**: ⏸️ Planned (Phase 3)

---

## Live Preview

**Purpose**: Real-time feedback during manipulation.

### Preview Pipeline

```java
public class LivePreviewController {
    private PipelineResult cachedResult;
    
    public void onManipulationUpdate(ManipulationResult result) {
        // Update parameters
        var newParams = applyManipulation(currentParams, result);
        
        // Re-run pipeline (cached/incremental)
        cachedResult = pipeline.execute(openingType, newParams);
        
        // Trigger render
        notifyRenderer(cachedResult);
    }
    
    public void onManipulationFinish() {
        // Commit cached result to world
        commitToWorld(cachedResult);
        cachedResult = null;
    }
}
```

**Performance Target**: < 16ms update (60 FPS during drag)

**Optimization**:
- Incremental pipeline (only recompute affected stages)
- LOD rendering (simplified mesh during drag)
- Caching (reuse unchanged components)

**Current Status**: ⏳ Basic preview exists, performance needs work

---

## Dimension Overlays

**Purpose**: Show measurements during editing.

### Overlay Types

**Length Dimension**:
```
  +----------+
  |          |  ← "1200mm"
  |          |
  +----------+
```

**Angle Dimension**:
```
    ⟲ 45°
   ╱ ╲
  |   |
```

**Distance Dimension** (between objects):
```
  [Opening A]  ←--500mm--→  [Opening B]
```

### Dimension Renderer

```java
public class DimensionOverlayRenderer {
    public void renderLengthDimension(
        Point3D start,
        Point3D end,
        String label,
        MatrixStack matrices
    ) {
        // Draw line
        drawLine(start, end, COLOR_DIMENSION_LINE);
        
        // Draw arrows
        drawArrow(start, direction(start, end));
        drawArrow(end, direction(end, start));
        
        // Draw label at midpoint
        var midpoint = lerp(start, end, 0.5);
        drawText(label, midpoint, matrices);
    }
}
```

**Current Status**: ⏸️ Planned (Phase 3)

---

## Keyboard Shortcuts

| Shortcut | Action |
|----------|--------|
| `Ctrl+Z` | Undo |
| `Ctrl+Shift+Z` | Redo |
| `Delete` | Delete selected opening |
| `Escape` | Cancel manipulation / Clear selection |
| `V` | Select mode |
| `R` | Resize mode |
| `E` | Rotate mode |
| `G` | Toggle grid snap |
| `Ctrl+D` | Duplicate selected opening |
| `Tab` | Cycle through openings |

**Current Status**: ⏳ Partial (basic shortcuts exist)

---

## Mouse Interaction

### Click Detection

```java
public class EditorMouseHandler {
    public void onMouseClick(Point3D rayOrigin, Vector3D rayDirection) {
        // 1. Check gizmo handles first
        var handle = gizmo.hitTest(rayOrigin, rayDirection);
        if (handle.isPresent()) {
            beginManipulation(handle.get());
            return;
        }
        
        // 2. Raycast against openings in world
        var hit = rayCastOpenings(rayOrigin, rayDirection);
        if (hit.isPresent()) {
            selection.select(hit.get());
            return;
        }
        
        // 3. Click on empty space → clear selection
        selection.clear();
    }
}
```

### Drag Detection

```java
public class EditorDragHandler {
    private Point3D dragStart;
    private boolean isDragging = false;
    
    public void onMouseMove(Point3D newPosition) {
        if (!isDragging) return;
        
        var event = new DragEvent(dragStart, newPosition);
        var result = manipulator.update(event);
        
        // Update live preview
        livePreview.update(result);
    }
    
    public void onMouseRelease() {
        if (isDragging) {
            var command = manipulator.finish();
            history.execute(command);
            isDragging = false;
        }
    }
}
```

**Current Status**: ✅ Basic mouse handling exists

---

## Performance Considerations

### Incremental Updates

**Problem**: Full pipeline re-run on every drag frame is too slow.

**Solution**: Incremental pipeline (see kernel/04-generation-pipeline.md).

```java
public class IncrementalEditor {
    public void onParameterChanged(String param, Object value) {
        // Determine which pipeline stages are affected
        var affectedStages = analyzeImpact(param);
        
        if (affectedStages.contains(Stage.GEOMETRY)) {
            // Re-run from geometry onward
            pipeline.executeFrom(Stage.GEOMETRY, cachedComponents);
        } else if (affectedStages.contains(Stage.MESH)) {
            // Re-run only mesh generation
            pipeline.executeFrom(Stage.MESH, cachedGeometry);
        }
    }
}
```

### LOD During Drag

**Problem**: High-poly mesh rendering is slow during drag.

**Solution**: Switch to simplified mesh during manipulation.

```java
public class LODController {
    public Mesh getMesh(OpeningInstance opening, RenderContext ctx) {
        if (ctx.isManipulating()) {
            return cachedLowPolyMesh;  // Simplified
        } else {
            return cachedHighPolyMesh;  // Full quality
        }
    }
}
```

**Current Status**: ⏸️ Planned (Phase 3)

---

## Testing Strategy

### Unit Tests

**Manipulator math**:
```java
@Test
void resizeManipulator_dragRight_increasesWidth() {
    var manipulator = new ResizeManipulator("width", 1000.0, Vector3D.X_AXIS);
    manipulator.begin(context);
    
    var event = new DragEvent(
        new Point3D(0, 0, 0),
        new Point3D(200, 0, 0)  // Drag 200mm right
    );
    var result = manipulator.update(event);
    
    assertEquals(1200.0, result.newValue("width"));
}
```

**Snap system**:
```java
@Test
void gridSnap_offGridPoint_snapsToGrid() {
    var snap = new GridSnap(1000.0);
    var point = new Point3D(1234, 5678, 9012);
    var snapped = snap.snap(point);
    
    assertEquals(new Point3D(1000, 6000, 9000), snapped);
}
```

### Integration Tests

**Command execution**:
```java
@Test
void setParameterCommand_executeAndUndo_restoresOriginalValue() {
    var opening = createTestOpening();
    var command = new SetParameterCommand(opening, "width", 1500.0);
    
    var originalWidth = opening.parameters().get("width");
    
    command.execute();
    assertEquals(1500.0, opening.parameters().get("width"));
    
    command.undo();
    assertEquals(originalWidth, opening.parameters().get("width"));
}
```

**Current Status**: ⏳ Basic tests exist, coverage incomplete

---

## Current Status

| Feature | Status | Priority |
|---------|--------|----------|
| Selection model | ✅ | - |
| Manipulator interfaces | ✅ | - |
| TranslateManipulator | ⏳ | HIGH |
| ResizeManipulator | ⏳ | 🔥 CRITICAL |
| RotateManipulator | ⏳ | MEDIUM |
| Gizmo rendering | ✅ | - |
| Gizmo hit-testing | ⏳ | HIGH |
| Command pattern | ✅ | - |
| Editor commands | ⏳ | HIGH |
| History (Undo/Redo) | ✅ | - |
| Grid snap | ✅ | - |
| Increment snap | ⏳ | HIGH |
| Inspector UI | ⏳ | HIGH |
| Inspector ↔ Gizmo sync | ❌ | 🔥 CRITICAL |
| Live preview | ⏳ | HIGH |
| Dimension overlays | ❌ | MEDIUM |
| Edit modes | ❌ | LOW |

---

## Acceptance Criteria

### For Platform V1 (Phase B)
- [x] Selection model working
- [x] Command pattern and history
- [ ] ResizeManipulator complete (drag handle → update width)
- [ ] Inspector ↔ Gizmo bidirectional sync
- [ ] Live preview during drag (< 100ms latency)
- [ ] Grid snap working

### For Editor V1 (Phase C)
- [ ] TranslateManipulator complete
- [ ] RotateManipulator complete
- [ ] Gizmo hit-testing accurate
- [ ] Increment and angle snap
- [ ] Dimension overlays
- [ ] Keyboard shortcuts

### For Production (Phase D+)
- [ ] Multi-edit mode
- [ ] Geometric snap (edge, vertex, midpoint)
- [ ] LOD during drag
- [ ] Incremental pipeline updates
- [ ] Performance: < 16ms per frame during drag

---

## Related Documents

- [kernel/07-command-system.md](../kernel/07-command-system.md) — Command pattern foundation
- [kernel/04-generation-pipeline.md](../kernel/04-generation-pipeline.md) — Pipeline invalidation
- [platform/01-opening-pipeline.md](../platform/01-opening-pipeline.md) — Live preview pipeline

---

**Document Status**: ✅ Complete  
**Last Updated**: 2026-07-16  
**Implementation**: ~30% (selection/history exist, manipulators partial)  
**Next Review**: After Platform V1 implementation
