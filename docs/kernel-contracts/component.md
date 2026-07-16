# Component Module Contract

**Version**: 1.0  
**Last Updated**: 2026-07-16  
**Status**: Active  
**Owner**: Aperture Core Team

---

## Overview

Component模块提供**参数化组件系统和组件图计算**。组件是可重用的几何生成单元，通过Port连接形成有向无环图（DAG），支持拓扑排序和增量求值。

**核心原则**: Component是数据流节点，封装几何生成逻辑，通过Port通信，不包含Opening特定逻辑。

---

## Responsibilities

### ✅ 允许做的事

1. **组件定义**
   - Component接口定义
   - Input/Output Port定义
   - 组件注册和查找

2. **组件图构建**
   - 添加组件节点
   - 连接Port（数据流边）
   - 图验证（无环、类型匹配）

3. **拓扑求值**
   - 拓扑排序
   - 依赖追踪
   - 增量计算（脏标记传播）

4. **内置组件**
   - ProfileExtrudeComponent（轮廓拉伸）
   - RevolveComponent（旋转生成）
   - BooleanComponent（布尔运算）
   - TransformComponent（变换）
   - MaterialAssignComponent（材质分配）

5. **组件生命周期**
   - 组件实例化
   - 参数绑定
   - 求值执行
   - 缓存管理

---

## Forbidden

### ❌ 禁止做的事

1. **❌ 不能包含Opening特定逻辑**
   ```java
   // 错误
   public class DoorComponent extends Component {
       @Override
       public Shape compute(Context ctx) {
           // "门"的特定逻辑
       }
   }
   
   // 正确: 通用组件
   public class ExtrudeComponent extends Component {
       @Override
       public Shape compute(Context ctx) {
           Profile profile = getInput("profile");
           double height = getInput("height");
           return Geometry.extrude(profile, height);
       }
   }
   ```

2. **❌ 不能直接渲染**
   ```java
   // 错误
   public interface Component {
       void render(RenderContext ctx);  // ❌ 不应该渲染
   }
   
   // 正确
   public interface Component {
       Shape compute(ComponentContext ctx);  // 只计算几何
   }
   ```

3. **❌ 不能依赖Minecraft**
   ```java
   // 错误
   import net.minecraft.world.World;
   
   public class WorldAwareComponent extends Component {
       public Shape compute(World world) { ... }
   }
   
   // 正确: 平台无关
   public class GenericComponent extends Component {
       public Shape compute(ComponentContext ctx) { ... }
   }
   ```

4. **❌ 不能包含UI逻辑**
   ```java
   // 错误
   public interface Component {
       Widget createEditor();  // ❌ UI不属于Component
   }
   
   // 正确
   public interface Component {
       List<ParameterDefinition> parameters();  // 只提供元数据
   }
   ```

5. **❌ 不能绕过Port直接访问其他组件**
   ```java
   // 错误
   public class BadComponent extends Component {
       private OtherComponent dependency;
       
       public Shape compute(ComponentContext ctx) {
           return dependency.compute(ctx);  // ❌ 绕过图结构
       }
   }
   
   // 正确: 通过Port获取输入
   public class GoodComponent extends Component {
       public Shape compute(ComponentContext ctx) {
           Shape input = ctx.getInput("inputShape");  // ✅ 通过Port
           return transform(input);
       }
   }
   ```

---

## Allowed Dependencies

### ✅ 可以依赖的模块

1. **aperture-geometry** (几何计算)
   - 组件内部使用几何操作

2. **aperture-parameter** (参数系统)
   - 组件参数定义

3. **aperture-math** (数学工具)
   - 向量/矩阵运算

4. **Java标准库**
   - java.util.* (集合、Stream)

**依赖原则**: Component消费Parameter和Geometry，提供数据流抽象

---

## Forbidden Dependencies

### ❌ 禁止依赖的模块

1. **❌ aperture-opening** (Opening业务)
   - 理由: Component是通用组件系统，不绑定Opening

2. **❌ aperture-client** (渲染)
   - 理由: Component只产生几何，不关心显示

3. **❌ net.minecraft.*** (Minecraft)
   - 理由: 平台独立性

4. **❌ aperture-mesh** (网格生成)
   - 理由: Component输出Shape，Mesh是下游转换

---

## Input Types

### 📥 接受的输入

1. **组件定义**
   ```java
   public abstract class Component {
       private final String id;
       private final List<InputPort> inputs;
       private final List<OutputPort> outputs;
       
       protected Component(String id) {
           this.id = id;
           this.inputs = defineInputs();
           this.outputs = defineOutputs();
       }
       
       protected abstract List<InputPort> defineInputs();
       protected abstract List<OutputPort> defineOutputs();
       protected abstract void compute(ComponentContext ctx);
   }
   ```

2. **Port定义**
   ```java
   public record InputPort(
       String name,
       PortType type,
       boolean required
   ) {}
   
   public record OutputPort(
       String name,
       PortType type
   ) {}
   
   public enum PortType {
       SHAPE,          // 几何体
       PROFILE,        // 轮廓
       NUMBER,         // 数值
       VECTOR,         // 向量
       MATERIAL,       // 材质
       TRANSFORM       // 变换矩阵
   }
   ```

3. **组件图**
   ```java
   public class ComponentGraph {
       private final Map<String, ComponentNode> nodes = new HashMap<>();
       private final List<Edge> edges = new ArrayList<>();
       
       public void addComponent(String nodeId, Component component) {
           nodes.put(nodeId, new ComponentNode(nodeId, component));
       }
       
       public void connect(String fromNode, String fromPort, 
                          String toNode, String toPort) {
           validateConnection(fromNode, fromPort, toNode, toPort);
           edges.add(new Edge(fromNode, fromPort, toNode, toPort));
       }
   }
   ```

4. **求值上下文**
   ```java
   public class ComponentContext {
       private final Map<String, Object> inputs;
       private final Map<String, Object> outputs;
       private final ParameterSet parameters;
       
       public <T> T getInput(String portName) {
           return (T) inputs.get(portName);
       }
       
       public void setOutput(String portName, Object value) {
           outputs.put(portName, value);
       }
       
       public ParameterValue getParameter(String key) {
           return parameters.get(key);
       }
   }
   ```

### 输入验证

```java
private void validateConnection(String fromNode, String fromPort,
                                String toNode, String toPort) {
    // 1. 节点存在
    ComponentNode from = nodes.get(fromNode);
    ComponentNode to = nodes.get(toNode);
    if (from == null || to == null) {
        throw new IllegalArgumentException("Node not found");
    }
    
    // 2. Port存在
    OutputPort outPort = from.component().getOutputPort(fromPort);
    InputPort inPort = to.component().getInputPort(toPort);
    if (outPort == null || inPort == null) {
        throw new IllegalArgumentException("Port not found");
    }
    
    // 3. 类型匹配
    if (outPort.type() != inPort.type()) {
        throw new IllegalArgumentException(
            "Port type mismatch: " + outPort.type() + " vs " + inPort.type()
        );
    }
    
    // 4. 不产生环
    if (createsCycle(fromNode, toNode)) {
        throw new IllegalArgumentException("Connection would create cycle");
    }
}
```

---

## Output Types

### 📤 产生的输出

1. **求值结果**
   ```java
   public record EvaluationResult(
       Map<String, ComponentOutput> outputs,
       EvaluationMetrics metrics
   ) {}
   
   public record ComponentOutput(
       String componentId,
       String portName,
       Object value,
       long computeTimeMs
   ) {}
   
   public record EvaluationMetrics(
       int componentsEvaluated,
       int cacheHits,
       long totalTimeMs
   ) {}
   ```

2. **拓扑排序**
   ```java
   public List<String> topologicalSort() {
       List<String> sorted = new ArrayList<>();
       Set<String> visited = new HashSet<>();
       Set<String> visiting = new HashSet<>();
       
       for (String nodeId : nodes.keySet()) {
           if (!visited.contains(nodeId)) {
               visit(nodeId, visited, visiting, sorted);
           }
       }
       
       return sorted;
   }
   ```

3. **依赖图**
   ```java
   public record DependencyGraph(
       Map<String, Set<String>> dependencies,    // node -> upstream nodes
       Map<String, Set<String>> dependents       // node -> downstream nodes
   ) {
       public Set<String> getUpstream(String nodeId) {
           return dependencies.getOrDefault(nodeId, Set.of());
       }
       
       public Set<String> getDownstream(String nodeId) {
           return dependents.getOrDefault(nodeId, Set.of());
       }
   }
   ```

4. **执行计划**
   ```java
   public record ExecutionPlan(
       List<String> executionOrder,
       Map<String, Set<String>> parallelBatches
   ) {
       public boolean canExecuteInParallel(String node1, String node2) {
           // 检查是否有依赖关系
       }
   }
   ```

### 输出不变式

**保证**:
- 拓扑排序顺序合法（依赖节点先求值）
- 无环（DAG保证）
- 所有必需输入有值
- 输出类型与Port定义一致
- 增量计算正确性（只重算脏节点及其下游）

---

## Lifecycle

### 组件实例化

```java
// 组件注册
public class ComponentRegistry {
    private static final Map<String, Class<? extends Component>> registry = new HashMap<>();
    
    static {
        register("extrude", ProfileExtrudeComponent.class);
        register("revolve", RevolveComponent.class);
        register("boolean", BooleanComponent.class);
    }
    
    public static Component create(String type, String id) {
        Class<? extends Component> clazz = registry.get(type);
        if (clazz == null) {
            throw new IllegalArgumentException("Unknown component type: " + type);
        }
        
        try {
            Constructor<? extends Component> ctor = clazz.getConstructor(String.class);
            return ctor.newInstance(id);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create component", e);
        }
    }
}
```

### 组件求值

**拓扑排序求值**:
```java
public class ComponentGraphEvaluator {
    public EvaluationResult evaluate(ComponentGraph graph) {
        // 1. 拓扑排序
        List<String> order = graph.topologicalSort();
        
        // 2. 按序求值
        Map<String, ComponentOutput> outputs = new HashMap<>();
        for (String nodeId : order) {
            ComponentNode node = graph.getNode(nodeId);
            
            // 准备输入
            ComponentContext ctx = prepareContext(node, outputs);
            
            // 执行组件
            long start = System.currentTimeMillis();
            node.component().compute(ctx);
            long duration = System.currentTimeMillis() - start;
            
            // 记录输出
            for (OutputPort port : node.component().outputs()) {
                Object value = ctx.getOutput(port.name());
                outputs.put(nodeId + "." + port.name(), 
                    new ComponentOutput(nodeId, port.name(), value, duration));
            }
        }
        
        return new EvaluationResult(outputs, computeMetrics(outputs));
    }
}
```

**增量求值**:
```java
public class IncrementalEvaluator {
    private final Map<String, Object> cache = new HashMap<>();
    private final Set<String> dirtyNodes = new HashSet<>();
    
    public void markDirty(String nodeId) {
        dirtyNodes.add(nodeId);
        
        // 传播脏标记到下游
        for (String downstream : graph.getDownstream(nodeId)) {
            markDirty(downstream);
        }
    }
    
    public EvaluationResult evaluateIncremental(ComponentGraph graph) {
        List<String> order = graph.topologicalSort();
        
        for (String nodeId : order) {
            if (dirtyNodes.contains(nodeId)) {
                // 重新计算
                ComponentOutput output = evaluateNode(graph.getNode(nodeId));
                cache.put(nodeId, output.value());
            }
            // 否则使用缓存
        }
        
        dirtyNodes.clear();
        return buildResult(cache);
    }
}
```

---

## Error Handling

### 异常类型

1. **ComponentException** - 组件系统通用异常
   ```java
   public class ComponentException extends RuntimeException {
       public ComponentException(String message) { super(message); }
       public ComponentException(String message, Throwable cause) { super(message, cause); }
   }
   ```

2. **CyclicGraphException** - 图中存在环
   ```java
   public class CyclicGraphException extends ComponentException {
       private final List<String> cycle;
       
       public CyclicGraphException(List<String> cycle) {
           super("Cyclic dependency detected: " + String.join(" -> ", cycle));
           this.cycle = cycle;
       }
   }
   ```

3. **MissingInputException** - 必需输入缺失
   ```java
   public class MissingInputException extends ComponentException {
       private final String componentId;
       private final String portName;
       
       public MissingInputException(String componentId, String portName) {
           super("Missing required input: " + componentId + "." + portName);
           this.componentId = componentId;
           this.portName = portName;
       }
   }
   ```

4. **PortTypeMismatchException** - Port类型不匹配
   ```java
   public class PortTypeMismatchException extends ComponentException {
       public PortTypeMismatchException(PortType expected, PortType actual) {
           super("Port type mismatch: expected " + expected + ", got " + actual);
       }
   }
   ```

### 错误处理策略

**构建时验证**:
```java
public void connect(String fromNode, String fromPort, String toNode, String toPort) {
    // 立即验证连接
    validateConnection(fromNode, fromPort, toNode, toPort);
    
    // 检测环
    if (createsCycle(fromNode, toNode)) {
        throw new CyclicGraphException(findCycle(fromNode, toNode));
    }
    
    edges.add(new Edge(fromNode, fromPort, toNode, toPort));
}
```

**求值时验证**:
```java
protected <T> T getInput(String portName, Class<T> expectedType) {
    InputPort port = getInputPort(portName);
    
    if (port.required() && !inputs.containsKey(portName)) {
        throw new MissingInputException(this.id, portName);
    }
    
    Object value = inputs.get(portName);
    if (value != null && !expectedType.isInstance(value)) {
        throw new PortTypeMismatchException(
            port.type(), 
            PortType.of(value.getClass())
        );
    }
    
    return expectedType.cast(value);
}
```

---

## Performance Requirements

### 时间复杂度

| 操作 | 复杂度 | 说明 |
|------|--------|------|
| 添加组件 | O(1) | HashMap插入 |
| 添加连接 | O(E) | 环检测 |
| 拓扑排序 | O(V+E) | DFS |
| 完整求值 | O(V·T) | T = 平均组件耗时 |
| 增量求值 | O(D·T) | D = 脏节点数 |

### 性能目标

- **小型图求值**: < 10ms (< 10个组件)
- **中型图求值**: < 50ms (< 50个组件)
- **增量求值**: < 5ms (单个组件变化)
- **环检测**: < 1ms

### 优化策略

```java
// 1. 缓存拓扑排序
public class CachedComponentGraph extends ComponentGraph {
    private List<String> cachedOrder;
    private boolean orderDirty = true;
    
    @Override
    public void connect(String from, String fromPort, String to, String toPort) {
        super.connect(from, fromPort, to, toPort);
        orderDirty = true;  // 标记顺序失效
    }
    
    @Override
    public List<String> topologicalSort() {
        if (orderDirty) {
            cachedOrder = super.topologicalSort();
            orderDirty = false;
        }
        return cachedOrder;
    }
}

// 2. 并行求值（无依赖的组件）
public class ParallelEvaluator {
    public EvaluationResult evaluate(ComponentGraph graph) {
        ExecutionPlan plan = graph.createExecutionPlan();
        
        for (Set<String> batch : plan.parallelBatches()) {
            // 同一批次的组件可以并行执行
            batch.parallelStream().forEach(nodeId -> {
                evaluateNode(graph.getNode(nodeId));
            });
        }
        
        return buildResult();
    }
}

// 3. 懒加载输入
public abstract class Component {
    protected <T> Supplier<T> lazyInput(String portName) {
        return () -> getInput(portName);
    }
    
    // 只在实际使用时才获取输入值
    public void compute(ComponentContext ctx) {
        Supplier<Shape> input = lazyInput("inputShape");
        
        if (someCondition) {
            Shape shape = input.get();  // 仅在需要时求值
        }
    }
}
```

---

## Built-in Components

### ProfileExtrudeComponent

```java
public class ProfileExtrudeComponent extends Component {
    public ProfileExtrudeComponent(String id) {
        super(id);
    }
    
    @Override
    protected List<InputPort> defineInputs() {
        return List.of(
            new InputPort("profile", PortType.PROFILE, true),
            new InputPort("height", PortType.NUMBER, true)
        );
    }
    
    @Override
    protected List<OutputPort> defineOutputs() {
        return List.of(
            new OutputPort("solid", PortType.SHAPE)
        );
    }
    
    @Override
    protected void compute(ComponentContext ctx) {
        Profile profile = ctx.getInput("profile");
        double height = ctx.getInput("height");
        
        Solid extruded = Geometry.extrude(profile, height);
        ctx.setOutput("solid", extruded);
    }
}
```

### BooleanComponent

```java
public class BooleanComponent extends Component {
    @Override
    protected List<InputPort> defineInputs() {
        return List.of(
            new InputPort("shapeA", PortType.SHAPE, true),
            new InputPort("shapeB", PortType.SHAPE, true),
            new InputPort("operation", PortType.NUMBER, true)  // 0=union, 1=subtract, 2=intersect
        );
    }
    
    @Override
    protected List<OutputPort> defineOutputs() {
        return List.of(
            new OutputPort("result", PortType.SHAPE)
        );
    }
    
    @Override
    protected void compute(ComponentContext ctx) {
        Shape a = ctx.getInput("shapeA");
        Shape b = ctx.getInput("shapeB");
        int op = ctx.getInput("operation");
        
        Shape result = switch (op) {
            case 0 -> Geometry.union(a, b);
            case 1 -> Geometry.subtract(a, b);
            case 2 -> Geometry.intersect(a, b);
            default -> throw new IllegalArgumentException("Invalid operation: " + op);
        };
        
        ctx.setOutput("result", result);
    }
}
```

---

## Examples

### ✅ 正确用法

```java
// 示例1: 构建组件图
ComponentGraph graph = new ComponentGraph();

// 添加组件
graph.addComponent("frame", ComponentRegistry.create("extrude", "frame"));
graph.addComponent("glass", ComponentRegistry.create("extrude", "glass"));
graph.addComponent("combined", ComponentRegistry.create("boolean", "combined"));

// 连接组件
graph.connect("frame", "solid", "combined", "shapeA");
graph.connect("glass", "solid", "combined", "shapeB");

// 求值
EvaluationResult result = ComponentGraphEvaluator.evaluate(graph);
Shape finalShape = result.getOutput("combined", "result");

// 示例2: 增量更新
IncrementalEvaluator evaluator = new IncrementalEvaluator(graph);

// 首次求值
EvaluationResult result1 = evaluator.evaluate();

// 修改参数
graph.getNode("frame").setParameter("height", 2000.0);
evaluator.markDirty("frame");

// 增量求值（只重算frame和downstream）
EvaluationResult result2 = evaluator.evaluateIncremental();

// 示例3: 自定义组件
public class MyComponent extends Component {
    public MyComponent(String id) {
        super(id);
    }
    
    @Override
    protected List<InputPort> defineInputs() {
        return List.of(
            new InputPort("input", PortType.SHAPE, true)
        );
    }
    
    @Override
    protected List<OutputPort> defineOutputs() {
        return List.of(
            new OutputPort("output", PortType.SHAPE)
        );
    }
    
    @Override
    protected void compute(ComponentContext ctx) {
        Shape input = ctx.getInput("input");
        Shape transformed = /* custom logic */;
        ctx.setOutput("output", transformed);
    }
}
```

### ❌ 错误用法

```java
// 错误1: 创建环
graph.addComponent("A", ...);
graph.addComponent("B", ...);
graph.connect("A", "out", "B", "in");
graph.connect("B", "out", "A", "in");  // ❌ CyclicGraphException

// 错误2: 类型不匹配
graph.connect("shapeOutput", "solid", "numberInput", "value");  // ❌ PortTypeMismatchException

// 错误3: 绕过Port直接访问
public class BadComponent extends Component {
    private Shape cachedShape;  // ❌ 不应该存储其他组件的输出
    
    public void compute(ComponentContext ctx) {
        // 应该通过 ctx.getInput() 获取
    }
}

// 错误4: 在组件中包含Opening特定逻辑
public class DoorFrameComponent extends Component {  // ❌ 太具体
    public void compute(ComponentContext ctx) {
        // 门框特定计算
    }
}

// 正确: 通用组件组合
// 使用 ExtrudeComponent + TransformComponent + BooleanComponent 组合
```

---

## Component Graph Serialization

```java
/**
 * 将组件图序列化为JSON
 */
public class ComponentGraphSerializer {
    public static JsonObject toJson(ComponentGraph graph) {
        return JsonObject.builder()
            .put("version", "1.0")
            .put("components", serializeComponents(graph.getNodes()))
            .put("connections", serializeConnections(graph.getEdges()))
            .build();
    }
    
    private static JsonArray serializeComponents(Map<String, ComponentNode> nodes) {
        JsonArray.Builder builder = JsonArray.builder();
        
        for (ComponentNode node : nodes.values()) {
            builder.add(JsonObject.builder()
                .put("id", node.id())
                .put("type", node.component().getClass().getSimpleName())
                .put("parameters", serializeParameters(node.parameters()))
                .build());
        }
        
        return builder.build();
    }
    
    private static JsonArray serializeConnections(List<Edge> edges) {
        JsonArray.Builder builder = JsonArray.builder();
        
        for (Edge edge : edges) {
            builder.add(JsonObject.builder()
                .put("from", edge.fromNode() + "." + edge.fromPort())
                .put("to", edge.toNode() + "." + edge.toPort())
                .build());
        }
        
        return builder.build();
    }
    
    public static ComponentGraph fromJson(JsonObject json) {
        // 反序列化
    }
}
```

---

## Migration Guide

### 违规代码迁移

**场景1: Opening特定组件**

**现状**:
```java
// aperture-component/.../DoorFrameComponent.java
public class DoorFrameComponent extends Component {
    public void compute(ComponentContext ctx) {
        // 门框特定逻辑
    }
}
```

**迁移**:
```java
// aperture-component: 提供通用组件
// (ExtrudeComponent, BooleanComponent已存在)

// aperture-opening: 组合通用组件
public class DoorDefinition {
    public ComponentGraph buildComponentGraph() {
        ComponentGraph graph = new ComponentGraph();
        
        // 使用通用组件组合出门框
        graph.addComponent("outerFrame", extrudeComponent);
        graph.addComponent("innerCutout", extrudeComponent);
        graph.addComponent("frame", booleanSubtract);
        
        graph.connect("outerFrame", "solid", "frame", "shapeA");
        graph.connect("innerCutout", "solid", "frame", "shapeB");
        
        return graph;
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
