# Command History 设计文档

## 概述

Command History系统实现Undo/Redo功能，让用户可以撤销和重做对Opening的编辑操作。基于经典Command模式，每个操作封装为可执行、可撤销的命令对象。

## 设计目标

1. **完整的Undo/Redo**: 支持所有编辑操作
2. **直观的交互**: Ctrl+Z / Ctrl+Y 快捷键
3. **可靠的状态管理**: 保证操作可逆性
4. **网络同步**: 支持多人协作 (future)
5. **持久化**: 可选的操作历史保存

## Command模式

### 核心接口

```java
/**
 * Represents a reversible operation.
 */
public interface Command {
    /**
     * Executes the command, applying changes.
     */
    void execute();
    
    /**
     * Undoes the command, reverting changes.
     */
    void undo();
    
    /**
     * Gets a human-readable description.
     */
    String description();
    
    /**
     * Optional: Can this command be merged with the next one?
     * Useful for continuous operations like dragging.
     */
    default boolean canMerge(Command next) {
        return false;
    }
    
    /**
     * Optional: Merge with another command.
     */
    default Command merge(Command next) {
        throw new UnsupportedOperationException();
    }
}
```

### 命令特性

**幂等性**: 
- `execute()` 和 `undo()` 可以多次调用
- 状态始终一致

**原子性**:
- 命令要么完全成功，要么完全失败
- 不留中间状态

**可描述性**:
- 提供清晰的操作描述
- 用于UI显示历史记录

---

## CommandHistory实现

### 核心结构

```java
/**
 * Manages command execution history with undo/redo support.
 */
public class CommandHistory {
    private final Deque<Command> undoStack = new ArrayDeque<>();
    private final Deque<Command> redoStack = new ArrayDeque<>();
    private int maxHistorySize = 100;
    
    /**
     * Executes a command and adds it to history.
     */
    public void execute(Command command) {
        command.execute();
        undoStack.push(command);
        redoStack.clear();  // Clear redo stack on new command
        
        // Limit history size
        while (undoStack.size() > maxHistorySize) {
            undoStack.removeLast();
        }
    }
    
    /**
     * Undoes the last command.
     */
    public boolean undo() {
        if (undoStack.isEmpty()) {
            return false;
        }
        
        Command command = undoStack.pop();
        command.undo();
        redoStack.push(command);
        return true;
    }
    
    /**
     * Redoes the last undone command.
     */
    public boolean redo() {
        if (redoStack.isEmpty()) {
            return false;
        }
        
        Command command = redoStack.pop();
        command.execute();
        undoStack.push(command);
        return true;
    }
    
    /**
     * Checks if undo is available.
     */
    public boolean canUndo() {
        return !undoStack.isEmpty();
    }
    
    /**
     * Checks if redo is available.
     */
    public boolean canRedo() {
        return !redoStack.isEmpty();
    }
    
    /**
     * Gets description of next undo operation.
     */
    public Optional<String> undoDescription() {
        return undoStack.isEmpty() ? 
            Optional.empty() : 
            Optional.of(undoStack.peek().description());
    }
    
    /**
     * Gets description of next redo operation.
     */
    public Optional<String> redoDescription() {
        return redoStack.isEmpty() ? 
            Optional.empty() : 
            Optional.of(redoStack.peek().description());
    }
    
    /**
     * Clears all history.
     */
    public void clear() {
        undoStack.clear();
        redoStack.clear();
    }
}
```

### 栈管理

**Undo栈**: 
- 最近执行的命令在栈顶
- Undo时从栈顶弹出并执行undo()
- 弹出的命令压入redo栈

**Redo栈**:
- 最近撤销的命令在栈顶
- Redo时从栈顶弹出并执行execute()
- 弹出的命令压入undo栈
- 新命令执行时清空redo栈

**示例流程**:
```
初始: undo=[], redo=[]

execute(A): undo=[A], redo=[]
execute(B): undo=[B,A], redo=[]
execute(C): undo=[C,B,A], redo=[]

undo(): undo=[B,A], redo=[C]
undo(): undo=[A], redo=[C,B]

redo(): undo=[B,A], redo=[C]

execute(D): undo=[D,B,A], redo=[]  // C丢失
```

---

## 基础命令实现

### 1. SetParameterCommand

**用途**: 修改Opening的单个参数

```java
public class SetParameterCommand implements Command {
    private final OpeningInstance instance;
    private final String parameterKey;
    private final ParameterValue oldValue;
    private final ParameterValue newValue;
    
    public SetParameterCommand(
        OpeningInstance instance,
        String key,
        ParameterValue oldValue,
        ParameterValue newValue
    ) {
        this.instance = instance;
        this.parameterKey = key;
        this.oldValue = oldValue;
        this.newValue = newValue;
    }
    
    @Override
    public void execute() {
        ParameterSet updated = instance.parameters()
            .with(parameterKey, newValue);
        instance.setParameters(updated);
    }
    
    @Override
    public void undo() {
        ParameterSet updated = instance.parameters()
            .with(parameterKey, oldValue);
        instance.setParameters(updated);
    }
    
    @Override
    public String description() {
        return String.format("Set %s to %s", parameterKey, newValue);
    }
    
    @Override
    public boolean canMerge(Command next) {
        // 可以合并连续的同参数修改
        if (!(next instanceof SetParameterCommand other)) {
            return false;
        }
        return this.instance == other.instance &&
               this.parameterKey.equals(other.parameterKey);
    }
    
    @Override
    public Command merge(Command next) {
        SetParameterCommand other = (SetParameterCommand) next;
        // 合并：保留初始oldValue，使用最新newValue
        return new SetParameterCommand(
            instance,
            parameterKey,
            this.oldValue,  // 保留原始值
            other.newValue  // 使用最新值
        );
    }
}
```

**使用示例**:
```java
// 用户拖动width slider: 900 → 1000 → 1100
Command cmd1 = new SetParameterCommand(instance, "width", 900, 1000);
Command cmd2 = new SetParameterCommand(instance, "width", 1000, 1100);

// 合并为单个命令
Command merged = cmd1.merge(cmd2);  // 900 → 1100

history.execute(merged);
// Undo一次即可回到900
```

---

### 2. PlaceOpeningCommand

**用途**: 放置新的Opening到世界

```java
public class PlaceOpeningCommand implements Command {
    private final Level level;
    private final BlockPos pos;
    private final OpeningInstance instance;
    private @Nullable BlockState previousState;
    
    public PlaceOpeningCommand(
        Level level,
        BlockPos pos,
        OpeningInstance instance
    ) {
        this.level = level;
        this.pos = pos;
        this.instance = instance;
    }
    
    @Override
    public void execute() {
        // 保存之前的方块状态
        previousState = level.getBlockState(pos);
        
        // 放置Opening
        level.setBlock(pos, OpeningBlock.INSTANCE.defaultBlockState(), 3);
        
        // 设置BlockEntity数据
        if (level.getBlockEntity(pos) instanceof OpeningBlockEntity be) {
            be.setInstance(instance);
        }
    }
    
    @Override
    public void undo() {
        // 恢复之前的方块
        level.setBlock(pos, previousState, 3);
    }
    
    @Override
    public String description() {
        return String.format("Place %s at %s", 
            instance.typeId(), 
            pos);
    }
}
```

---

### 3. DeleteOpeningCommand

**用途**: 删除已放置的Opening

```java
public class DeleteOpeningCommand implements Command {
    private final Level level;
    private final BlockPos pos;
    private @Nullable OpeningInstance savedInstance;
    private @Nullable BlockState savedState;
    
    public DeleteOpeningCommand(Level level, BlockPos pos) {
        this.level = level;
        this.pos = pos;
    }
    
    @Override
    public void execute() {
        // 保存当前状态用于undo
        savedState = level.getBlockState(pos);
        
        if (level.getBlockEntity(pos) instanceof OpeningBlockEntity be) {
            savedInstance = be.getInstance();
        }
        
        // 删除方块
        level.removeBlock(pos, false);
    }
    
    @Override
    public void undo() {
        // 恢复Opening
        level.setBlock(pos, savedState, 3);
        
        if (level.getBlockEntity(pos) instanceof OpeningBlockEntity be) {
            be.setInstance(savedInstance);
        }
    }
    
    @Override
    public String description() {
        return String.format("Delete opening at %s", pos);
    }
}
```

---

### 4. TransformOpeningCommand

**用途**: 移动、旋转、缩放Opening

```java
public class TransformOpeningCommand implements Command {
    private final OpeningInstance instance;
    private final Transform oldTransform;
    private final Transform newTransform;
    
    public TransformOpeningCommand(
        OpeningInstance instance,
        Transform oldTransform,
        Transform newTransform
    ) {
        this.instance = instance;
        this.oldTransform = oldTransform;
        this.newTransform = newTransform;
    }
    
    @Override
    public void execute() {
        instance.setTransform(newTransform);
    }
    
    @Override
    public void undo() {
        instance.setTransform(oldTransform);
    }
    
    @Override
    public String description() {
        return "Transform opening";
    }
    
    @Override
    public boolean canMerge(Command next) {
        // 连续的transform可以合并
        return next instanceof TransformOpeningCommand other &&
               this.instance == other.instance;
    }
    
    @Override
    public Command merge(Command next) {
        TransformOpeningCommand other = (TransformOpeningCommand) next;
        return new TransformOpeningCommand(
            instance,
            this.oldTransform,  // 保留初始transform
            other.newTransform   // 使用最新transform
        );
    }
}
```

---

### 5. CompositeCommand

**用途**: 将多个命令组合为原子操作

```java
public class CompositeCommand implements Command {
    private final List<Command> commands;
    private final String description;
    
    public CompositeCommand(String description, Command... commands) {
        this.description = description;
        this.commands = List.of(commands);
    }
    
    @Override
    public void execute() {
        for (Command cmd : commands) {
            cmd.execute();
        }
    }
    
    @Override
    public void undo() {
        // 反向撤销
        for (int i = commands.size() - 1; i >= 0; i--) {
            commands.get(i).undo();
        }
    }
    
    @Override
    public String description() {
        return description;
    }
}
```

**使用示例**:
```java
// 复制Opening：删除旧的 + 放置新的
Command delete = new DeleteOpeningCommand(level, oldPos);
Command place = new PlaceOpeningCommand(level, newPos, instance.copy());
Command move = new CompositeCommand("Move opening", delete, place);

history.execute(move);
// Undo一次即可撤销整个移动
```

---

## 编辑器集成

### ParameterEditorScreen

```java
public class ParameterEditorScreen extends Screen {
    private final CommandHistory commandHistory = new CommandHistory();
    
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Ctrl+Z: Undo
        if (keyCode == GLFW.GLFW_KEY_Z && hasControlDown()) {
            if (commandHistory.undo()) {
                refreshPreview();
                return true;
            }
        }
        
        // Ctrl+Y: Redo
        if (keyCode == GLFW.GLFW_KEY_Y && hasControlDown()) {
            if (commandHistory.redo()) {
                refreshPreview();
                return true;
            }
        }
        
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
    
    private void onParameterChanged(String key, ParameterValue newValue) {
        ParameterValue oldValue = currentParams.get(key);
        
        Command cmd = new SetParameterCommand(
            instance,
            key,
            oldValue,
            newValue
        );
        
        commandHistory.execute(cmd);
        refreshPreview();
    }
}
```

### 历史记录UI

```java
public class HistoryPanel {
    private final CommandHistory history;
    
    public void render(GuiGraphics graphics, int x, int y) {
        // 显示undo/redo按钮
        Button undoButton = Button.builder(
            Component.literal("Undo: " + 
                history.undoDescription().orElse("Nothing")),
            btn -> history.undo()
        )
        .bounds(x, y, 150, 20)
        .active(history.canUndo())
        .build();
        
        Button redoButton = Button.builder(
            Component.literal("Redo: " + 
                history.redoDescription().orElse("Nothing")),
            btn -> history.redo()
        )
        .bounds(x + 160, y, 150, 20)
        .active(history.canRedo())
        .build();
    }
}
```

---

## 命令合并策略

### 为什么需要合并？

用户拖动slider时会产生大量中间值：
```
Width: 900 → 920 → 940 → 960 → 980 → 1000
```

不合并会导致：
- 历史记录膨胀
- Undo需要点击多次
- 用户体验差

### 合并策略

**1. 时间窗口合并**
```java
public class MergingCommandHistory extends CommandHistory {
    private static final long MERGE_WINDOW_MS = 500;
    private long lastCommandTime = 0;
    
    @Override
    public void execute(Command command) {
        long now = System.currentTimeMillis();
        
        if (!undoStack.isEmpty() && 
            now - lastCommandTime < MERGE_WINDOW_MS &&
            undoStack.peek().canMerge(command)) {
            
            // 合并命令
            Command merged = undoStack.pop().merge(command);
            undoStack.push(merged);
        } else {
            // 新命令
            super.execute(command);
        }
        
        lastCommandTime = now;
    }
}
```

**2. 同类型合并**
```java
// 只合并相同参数的连续修改
@Override
public boolean canMerge(Command next) {
    if (!(next instanceof SetParameterCommand other)) {
        return false;
    }
    return this.parameterKey.equals(other.parameterKey);
}
```

**3. 显式分组**
```java
// 用户松开鼠标时，标记分组结束
public void onMouseReleased() {
    commandHistory.markGroupEnd();
}
```

---

## 网络同步 (Future)

### 设计思路

**客户端**:
- 执行命令，立即应用
- 发送命令到服务器
- 等待服务器确认

**服务器**:
- 接收命令
- 验证合法性
- 广播给其他客户端
- 发送确认

**冲突解决**:
- 使用操作变换 (Operational Transformation)
- 或CRDT (Conflict-free Replicated Data Types)

### 命令序列化

```java
public interface SerializableCommand extends Command {
    CompoundTag toNbt();
    static Command fromNbt(CompoundTag tag);
}

public class SetParameterCommand implements SerializableCommand {
    @Override
    public CompoundTag toNbt() {
        CompoundTag tag = new CompoundTag();
        tag.putString("type", "set_parameter");
        tag.putUUID("instanceId", instance.instanceId());
        tag.putString("key", parameterKey);
        // ... 序列化oldValue和newValue
        return tag;
    }
    
    public static Command fromNbt(CompoundTag tag) {
        UUID instanceId = tag.getUUID("instanceId");
        String key = tag.getString("key");
        // ... 反序列化
        return new SetParameterCommand(...);
    }
}
```

---

## 持久化 (Optional)

### 保存历史记录

```java
public class PersistedCommandHistory extends CommandHistory {
    private final Path historyFile;
    
    @Override
    public void execute(Command command) {
        super.execute(command);
        
        if (command instanceof SerializableCommand sc) {
            appendToFile(sc.toNbt());
        }
    }
    
    public void loadFromFile() {
        List<CompoundTag> tags = readFromFile(historyFile);
        for (CompoundTag tag : tags) {
            Command cmd = SerializableCommand.fromNbt(tag);
            undoStack.push(cmd);
        }
    }
}
```

**用途**:
- 崩溃恢复
- 操作审计
- 协作回放

---

## 测试

### 单元测试

```java
@Test
void commandHistory_undoRedo_worksCorrectly() {
    CommandHistory history = new CommandHistory();
    MockCommand cmd1 = new MockCommand("A");
    MockCommand cmd2 = new MockCommand("B");
    
    // Execute
    history.execute(cmd1);
    history.execute(cmd2);
    assertTrue(cmd1.executed);
    assertTrue(cmd2.executed);
    
    // Undo
    history.undo();
    assertTrue(cmd2.undone);
    assertFalse(cmd1.undone);
    
    // Redo
    history.redo();
    assertTrue(cmd2.executed);
    
    // Undo all
    history.undo();
    history.undo();
    assertTrue(cmd1.undone);
}

@Test
void setParameterCommand_merge_preservesOriginalValue() {
    SetParameterCommand cmd1 = new SetParameterCommand(
        instance, "width", 
        ParameterValue.length(900), 
        ParameterValue.length(1000)
    );
    
    SetParameterCommand cmd2 = new SetParameterCommand(
        instance, "width", 
        ParameterValue.length(1000), 
        ParameterValue.length(1100)
    );
    
    Command merged = cmd1.merge(cmd2);
    
    merged.execute();
    assertEquals(1100, instance.parameters().get("width").asLength());
    
    merged.undo();
    assertEquals(900, instance.parameters().get("width").asLength());
}
```

---

## 性能考虑

### 内存占用

每个命令占用内存：
- SetParameterCommand: ~100 bytes
- PlaceOpeningCommand: ~500 bytes
- 100条历史: ~10-50KB

**可接受**，不是瓶颈。

### 执行开销

- Command execute/undo: <1ms
- Pipeline重新生成: ~100ms (使用缓存)

**优化**: 延迟Pipeline生成，等待命令稳定。

---

## 最佳实践

### 1. 粒度适中

**太细**:
```java
// 不好：每个字符都是一个命令
onCharTyped(char c) {
    history.execute(new InsertCharCommand(c));
}
// 结果：输入"hello"需要5次undo
```

**太粗**:
```java
// 不好：整个编辑会话是一个命令
onApply() {
    history.execute(new EditSessionCommand(allChanges));
}
// 结果：无法撤销到中间状态
```

**合适**:
```java
// 好：参数修改是单位
onParameterChange(key, value) {
    history.execute(new SetParameterCommand(key, oldValue, value));
}
// 支持合并连续修改
```

### 2. 保证可逆性

```java
@Override
public void execute() {
    // 保存必要的旧状态
    this.oldState = saveCurrentState();
    
    // 执行修改
    applyChanges();
}

@Override
public void undo() {
    // 使用保存的状态恢复
    restoreState(oldState);
}
```

### 3. 清晰的描述

```java
@Override
public String description() {
    // 不好
    return "Change parameter";
    
    // 好
    return String.format("Set width to %.0fmm", newValue.asLength());
}
```

---

## 总结

Command History系统提供：
- ✅ 完整的Undo/Redo支持
- ✅ 命令合并减少历史记录
- ✅ 可扩展的命令类型
- ✅ 网络同步基础 (future)
- ✅ 持久化支持 (optional)

**下一步**: 实现基础命令并集成到编辑器。

---

**Created**: 2026-07-16  
**Status**: Design Complete  
**Implementation**: Week 4-5
