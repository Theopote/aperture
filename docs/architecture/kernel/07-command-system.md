# 07 — Command System

**Layer**: Kernel  
**Status**: 🎯 CRITICAL — Undo/Redo Foundation  
**Dependencies**: None (pure kernel)

---

## Overview

**Every modification is a Command.**

The Command System is Aperture's foundation for:
- **Undo/Redo** — Reversible operations
- **History** — Audit trail of all changes
- **Transactions** — Group multiple commands atomically
- **Replay** — Re-execute command sequence
- **Networking** — Serialize and sync commands
- **AI Integration** — Commands as structured actions

**Design Principle**: Commands are **immutable data** describing a change, not methods that mutate state. A command says "set width to 1800", not "mutate this object".

**This is NOT scattered mutation code. This IS a unified action system.**

---

## Why Command Pattern?

### Without Commands

```java
// Direct mutation
opening.setWidth(1800);
opening.setMullions(3);
opening.setMaterial("oak");

// Problems:
// - Can't undo
// - No history
// - Can't replay
// - Can't sync to server
// - Can't validate before executing
```

### With Commands

```java
// Create commands
var cmd1 = new SetParameterCommand("width", 1800);
var cmd2 = new SetParameterCommand("mullions", 3);
var cmd3 = new SetParameterCommand("frame_material", "aperture:oak");

// Execute
commandExecutor.execute(cmd1);
commandExecutor.execute(cmd2);
commandExecutor.execute(cmd3);

// Undo
commandExecutor.undo();  // Reverts material
commandExecutor.undo();  // Reverts mullions
commandExecutor.undo();  // Reverts width

// Redo
commandExecutor.redo();  // Reapplies width
```

**Benefits**:
- ✅ Every change is reversible
- ✅ Complete history
- ✅ Validate before execute
- ✅ Serialize for network/storage
- ✅ Group into transactions
- ✅ AI can generate commands

---

## Command Interface

### Core Contract

```java
public interface Command {
    /**
     * Unique command ID for serialization.
     */
    String commandId();
    
    /**
     * Human-readable description.
     */
    String description();
    
    /**
     * Execute the command, producing a result.
     * 
     * @param target The object to modify
     * @return Execution result (success + inverse command)
     */
    CommandResult execute(CommandTarget target);
    
    /**
     * Can this command be merged with the next?
     * Used to collapse repeated similar commands (e.g., dragging width slider).
     */
    default boolean canMerge(Command next) {
        return false;
    }
    
    /**
     * Merge this command with the next.
     */
    default Command merge(Command next) {
        throw new UnsupportedOperationException();
    }
}
```

### Command Result

```java
public record CommandResult(
    boolean success,
    String message,          // Error message if failed
    Command inverse          // Undo command (null if failed)
) {
    public static CommandResult success(Command inverse) {
        return new CommandResult(true, null, inverse);
    }
    
    public static CommandResult failure(String message) {
        return new CommandResult(false, message, null);
    }
}
```

### Command Target

**Abstraction over what can be commanded**:

```java
public sealed interface CommandTarget permits 
    OpeningInstanceTarget, DesignSessionTarget, WorldTarget {}

public record OpeningInstanceTarget(OpeningInstance instance) 
    implements CommandTarget {}

public record DesignSessionTarget(DesignSession session) 
    implements CommandTarget {}
```

---

## Command Types

### 1. SetParameterCommand

**Changes a single parameter value**.

```java
public record SetParameterCommand(
    String parameterKey,
    Object newValue
) implements Command {
    
    @Override
    public String commandId() {
        return "aperture:set_parameter";
    }
    
    @Override
    public String description() {
        return String.format("Set %s to %s", parameterKey, newValue);
    }
    
    @Override
    public CommandResult execute(CommandTarget target) {
        if (!(target instanceof OpeningInstanceTarget instanceTarget)) {
            return CommandResult.failure("Invalid target");
        }
        
        var instance = instanceTarget.instance();
        var oldValue = instance.getParameters().get(parameterKey);
        
        // Create new instance with updated parameter
        var newParams = instance.getParameters().with(parameterKey, newValue);
        var newInstance = instance.withParameters(newParams);
        
        // Update target (side effect)
        instanceTarget.update(newInstance);
        
        // Return inverse command (for undo)
        var inverse = new SetParameterCommand(parameterKey, oldValue);
        return CommandResult.success(inverse);
    }
    
    @Override
    public boolean canMerge(Command next) {
        // Merge repeated changes to same parameter
        return next instanceof SetParameterCommand other 
            && other.parameterKey.equals(this.parameterKey);
    }
    
    @Override
    public Command merge(Command next) {
        // Keep first command's old value, last command's new value
        return new SetParameterCommand(
            this.parameterKey,
            ((SetParameterCommand) next).newValue
        );
    }
}
```

**Usage**:
```java
var cmd = new SetParameterCommand("width", 1800.0);
executor.execute(cmd);
```

---

### 2. SetMultipleParametersCommand

**Changes multiple parameters atomically**.

```java
public record SetMultipleParametersCommand(
    Map<String, Object> newValues
) implements Command {
    
    @Override
    public String commandId() {
        return "aperture:set_multiple_parameters";
    }
    
    @Override
    public String description() {
        return String.format("Set %d parameters", newValues.size());
    }
    
    @Override
    public CommandResult execute(CommandTarget target) {
        if (!(target instanceof OpeningInstanceTarget instanceTarget)) {
            return CommandResult.failure("Invalid target");
        }
        
        var instance = instanceTarget.instance();
        var oldValues = new HashMap<String, Object>();
        
        // Capture old values
        for (var key : newValues.keySet()) {
            oldValues.put(key, instance.getParameters().get(key));
        }
        
        // Apply new values
        var newParams = instance.getParameters();
        for (var entry : newValues.entrySet()) {
            newParams = newParams.with(entry.getKey(), entry.getValue());
        }
        
        var newInstance = instance.withParameters(newParams);
        instanceTarget.update(newInstance);
        
        // Inverse
        var inverse = new SetMultipleParametersCommand(oldValues);
        return CommandResult.success(inverse);
    }
}
```

**Usage**:
```java
var cmd = new SetMultipleParametersCommand(Map.of(
    "width", 1800.0,
    "height", 2100.0,
    "mullions", 3
));
executor.execute(cmd);
```

---

### 3. TransformCommand

**Moves, rotates, or scales an opening**.

```java
public record TransformCommand(
    Transform3d delta
) implements Command {
    
    @Override
    public CommandResult execute(CommandTarget target) {
        if (!(target instanceof OpeningInstanceTarget instanceTarget)) {
            return CommandResult.failure("Invalid target");
        }
        
        var instance = instanceTarget.instance();
        var oldTransform = instance.getTransform();
        var newTransform = oldTransform.compose(delta);
        
        var newInstance = instance.withTransform(newTransform);
        instanceTarget.update(newInstance);
        
        // Inverse: apply opposite transform
        var inverse = new TransformCommand(delta.inverse());
        return CommandResult.success(inverse);
    }
}
```

---

### 4. DeleteOpeningCommand

**Removes an opening from the world**.

```java
public record DeleteOpeningCommand(
    OpeningInstanceId instanceId
) implements Command {
    
    @Override
    public CommandResult execute(CommandTarget target) {
        if (!(target instanceof WorldTarget worldTarget)) {
            return CommandResult.failure("Invalid target");
        }
        
        var world = worldTarget.world();
        var instance = world.getOpening(instanceId);
        
        if (instance == null) {
            return CommandResult.failure("Opening not found");
        }
        
        // Remove from world
        world.removeOpening(instanceId);
        
        // Inverse: recreate opening
        var inverse = new CreateOpeningCommand(instance);
        return CommandResult.success(inverse);
    }
}
```

---

### 5. CreateOpeningCommand

**Adds a new opening to the world**.

```java
public record CreateOpeningCommand(
    OpeningInstance instance
) implements Command {
    
    @Override
    public CommandResult execute(CommandTarget target) {
        if (!(target instanceof WorldTarget worldTarget)) {
            return CommandResult.failure("Invalid target");
        }
        
        var world = worldTarget.world();
        world.addOpening(instance);
        
        // Inverse: delete
        var inverse = new DeleteOpeningCommand(instance.getInstanceId());
        return CommandResult.success(inverse);
    }
}
```

---

### 6. CompositeCommand (Transaction)

**Groups multiple commands as one atomic operation**.

```java
public record CompositeCommand(
    List<Command> commands,
    String description
) implements Command {
    
    @Override
    public String commandId() {
        return "aperture:composite";
    }
    
    @Override
    public CommandResult execute(CommandTarget target) {
        var inverseCommands = new ArrayList<Command>();
        
        // Execute all commands
        for (var cmd : commands) {
            var result = cmd.execute(target);
            if (!result.success()) {
                // Rollback: execute inverse commands in reverse
                for (int i = inverseCommands.size() - 1; i >= 0; i--) {
                    inverseCommands.get(i).execute(target);
                }
                return CommandResult.failure("Transaction failed: " + result.message());
            }
            inverseCommands.add(result.inverse());
        }
        
        // Success: create composite inverse
        Collections.reverse(inverseCommands);
        var inverse = new CompositeCommand(inverseCommands, "Undo: " + description);
        return CommandResult.success(inverse);
    }
}
```

**Usage**:
```java
var transaction = new CompositeCommand(List.of(
    new SetParameterCommand("width", 1800),
    new SetParameterCommand("height", 2100),
    new SetParameterCommand("frame_material", "aperture:oak")
), "Configure door");

executor.execute(transaction);  // All or nothing
```

---

## Command Executor

### History Stack

```java
public class CommandExecutor {
    private final Deque<Command> undoStack = new ArrayDeque<>();
    private final Deque<Command> redoStack = new ArrayDeque<>();
    private final CommandTarget target;
    private final int maxHistorySize;
    
    public CommandExecutor(CommandTarget target, int maxHistorySize) {
        this.target = target;
        this.maxHistorySize = maxHistorySize;
    }
    
    /**
     * Execute a command and push to undo stack.
     */
    public CommandResult execute(Command command) {
        var result = command.execute(target);
        
        if (result.success()) {
            // Add to undo stack
            undoStack.push(result.inverse());
            
            // Clear redo stack (new action invalidates redo history)
            redoStack.clear();
            
            // Enforce max history size
            while (undoStack.size() > maxHistorySize) {
                undoStack.removeLast();
            }
        }
        
        return result;
    }
    
    /**
     * Undo the last command.
     */
    public boolean undo() {
        if (undoStack.isEmpty()) {
            return false;
        }
        
        var undoCommand = undoStack.pop();
        var result = undoCommand.execute(target);
        
        if (result.success()) {
            redoStack.push(result.inverse());
            return true;
        }
        
        return false;
    }
    
    /**
     * Redo the last undone command.
     */
    public boolean redo() {
        if (redoStack.isEmpty()) {
            return false;
        }
        
        var redoCommand = redoStack.pop();
        var result = redoCommand.execute(target);
        
        if (result.success()) {
            undoStack.push(result.inverse());
            return true;
        }
        
        return false;
    }
    
    /**
     * Can undo?
     */
    public boolean canUndo() {
        return !undoStack.isEmpty();
    }
    
    /**
     * Can redo?
     */
    public boolean canRedo() {
        return !redoStack.isEmpty();
    }
    
    /**
     * Get undo stack (for UI display).
     */
    public List<String> getUndoHistory() {
        return undoStack.stream()
            .map(Command::description)
            .toList();
    }
}
```

---

### Command Merging

**Problem**: Dragging a slider generates hundreds of SetParameterCommand. We don't want 200 undo steps.

**Solution**: Merge consecutive similar commands.

```java
public class MergingCommandExecutor extends CommandExecutor {
    private static final long MERGE_WINDOW_MS = 500;  // Merge within 500ms
    private Command lastCommand;
    private long lastCommandTime;
    
    @Override
    public CommandResult execute(Command command) {
        var now = System.currentTimeMillis();
        
        // Try to merge with last command
        if (lastCommand != null 
            && (now - lastCommandTime) < MERGE_WINDOW_MS
            && lastCommand.canMerge(command)) {
            
            // Merge: replace last command with merged version
            undoStack.pop();
            var merged = lastCommand.merge(command);
            var result = super.execute(merged);
            
            lastCommand = merged;
            lastCommandTime = now;
            return result;
        }
        
        // Cannot merge: execute normally
        var result = super.execute(command);
        lastCommand = command;
        lastCommandTime = now;
        return result;
    }
}
```

**Effect**:
```
Without merging:
Undo stack: [Set width 1200, Set width 1250, Set width 1300, ..., Set width 1800]
(50 undo steps)

With merging:
Undo stack: [Set width 1800]
(1 undo step)
```

---

## Command Serialization

### JSON Format

```json
{
  "commandId": "aperture:set_parameter",
  "data": {
    "parameterKey": "width",
    "newValue": 1800
  }
}
```

### Serializer

```java
public class CommandSerializer {
    private final Map<String, CommandCodec<?>> codecs = new HashMap<>();
    
    public CommandSerializer() {
        registerCodec("aperture:set_parameter", new SetParameterCommandCodec());
        registerCodec("aperture:set_multiple_parameters", new SetMultipleParametersCommandCodec());
        // ...
    }
    
    public String serialize(Command command) {
        var codec = codecs.get(command.commandId());
        if (codec == null) {
            throw new IllegalArgumentException("No codec for: " + command.commandId());
        }
        return codec.encode(command);
    }
    
    public Command deserialize(String json) {
        var node = parseJson(json);
        var commandId = node.get("commandId").asText();
        
        var codec = codecs.get(commandId);
        if (codec == null) {
            throw new IllegalArgumentException("No codec for: " + commandId);
        }
        
        return codec.decode(node.get("data"));
    }
}
```

### Command Codec

```java
public interface CommandCodec<T extends Command> {
    String encode(T command);
    T decode(JsonNode data);
}

public class SetParameterCommandCodec implements CommandCodec<SetParameterCommand> {
    @Override
    public String encode(SetParameterCommand command) {
        return String.format("""
            {
              "commandId": "%s",
              "data": {
                "parameterKey": "%s",
                "newValue": %s
              }
            }
            """, 
            command.commandId(), 
            command.parameterKey(), 
            encodeValue(command.newValue())
        );
    }
    
    @Override
    public SetParameterCommand decode(JsonNode data) {
        var key = data.get("parameterKey").asText();
        var value = decodeValue(data.get("newValue"));
        return new SetParameterCommand(key, value);
    }
}
```

---

## Networking

### Client → Server

**Client sends command to server**:

```java
public class ClientCommandSender {
    private final NetworkChannel channel;
    private final CommandSerializer serializer;
    
    public void sendCommand(Command command) {
        var json = serializer.serialize(command);
        var packet = new CommandPacket(json);
        channel.send(packet);
    }
}
```

### Server → Clients

**Server executes command and broadcasts to all clients**:

```java
public class ServerCommandHandler {
    private final CommandExecutor executor;
    private final CommandSerializer serializer;
    
    public void handleCommand(CommandPacket packet, Player sender) {
        // Deserialize
        var command = serializer.deserialize(packet.getJson());
        
        // Validate (server authority)
        if (!canExecute(sender, command)) {
            sender.sendMessage("Command not permitted");
            return;
        }
        
        // Execute
        var result = executor.execute(command);
        
        if (result.success()) {
            // Broadcast to all clients (including sender)
            broadcastCommand(command);
        } else {
            sender.sendMessage("Command failed: " + result.message());
        }
    }
}
```

---

## AI Integration

**AI generates commands instead of directly mutating**:

```java
public class AICommandGenerator {
    /**
     * Interprets natural language and generates commands.
     * 
     * Example: "Make it wider" → SetParameterCommand("width", currentWidth * 1.2)
     */
    public List<Command> interpretNaturalLanguage(String input, OpeningInstance current) {
        // NLP magic here...
        
        if (input.contains("wider")) {
            var currentWidth = current.getParameters().getDouble("width");
            var newWidth = currentWidth * 1.2;
            return List.of(new SetParameterCommand("width", newWidth));
        }
        
        if (input.contains("add mullions")) {
            var currentCount = current.getParameters().getInt("mullions");
            var newCount = currentCount + 1;
            return List.of(new SetParameterCommand("mullions", newCount));
        }
        
        // ...
        
        return List.of();
    }
}
```

**User workflow**:
1. User: "Make the door oak with glass panel"
2. AI generates: `[SetParameterCommand("frame_material", "aperture:oak"), SetParameterCommand("panel_material", "aperture:glass")]`
3. User reviews commands (UI shows preview)
4. User confirms → Executor runs commands
5. User can undo if not satisfied

---

## Command Validation

**Validate before execution**:

```java
public interface CommandValidator {
    ValidationResult validate(Command command, CommandTarget target);
}

public class ParameterCommandValidator implements CommandValidator {
    @Override
    public ValidationResult validate(Command command, CommandTarget target) {
        if (!(command instanceof SetParameterCommand paramCmd)) {
            return ValidationResult.valid();
        }
        
        if (!(target instanceof OpeningInstanceTarget instanceTarget)) {
            return ValidationResult.invalid("Invalid target");
        }
        
        var instance = instanceTarget.instance();
        var definition = getDefinition(instance.getTypeId());
        
        // Simulate parameter change
        var newParams = instance.getParameters().with(
            paramCmd.parameterKey(), 
            paramCmd.newValue()
        );
        
        // Validate constraints
        var validator = new ParametricValidator(definition);
        return validator.validate(newParams);
    }
}
```

**Usage**:
```java
var cmd = new SetParameterCommand("width", 100);  // Too small
var validation = validator.validate(cmd, target);

if (!validation.valid()) {
    System.err.println("Cannot execute: " + validation.getErrors());
    // Don't execute
} else {
    executor.execute(cmd);
}
```

---

## Command Replay

**Save session as command log, replay later**:

```java
public class CommandLog {
    private final List<Command> commands = new ArrayList<>();
    
    public void record(Command command) {
        commands.add(command);
    }
    
    public void saveToFile(Path path) throws IOException {
        var serializer = new CommandSerializer();
        var json = commands.stream()
            .map(serializer::serialize)
            .collect(Collectors.joining(",\n", "[\n", "\n]"));
        Files.writeString(path, json);
    }
    
    public static CommandLog loadFromFile(Path path) throws IOException {
        var json = Files.readString(path);
        var serializer = new CommandSerializer();
        var log = new CommandLog();
        
        // Parse JSON array, deserialize each command
        var array = parseJsonArray(json);
        for (var node : array) {
            var command = serializer.deserialize(node.toString());
            log.record(command);
        }
        
        return log;
    }
    
    public void replay(CommandExecutor executor) {
        for (var command : commands) {
            executor.execute(command);
        }
    }
}
```

**Use cases**:
- **Tutorial recordings**: Replay steps to teach users
- **Bug reproduction**: User sends command log to developers
- **Design templates**: Save command sequence as reusable template
- **Automated testing**: Replay command log to verify behavior

---

## Current Status

| Component | Status | Notes |
|-----------|--------|-------|
| Command interface | ⏳ Partial | `EditCommand` exists in `aperture-editor`, needs generalization |
| SetParameterCommand | ⏳ Partial | Exists as `SetParameterCommand` in editor, needs polish |
| CommandExecutor | ⏳ Partial | `EditHistory` exists but limited |
| Undo/Redo | ⏳ Partial | Skeleton exists, not fully wired |
| Command merging | ❌ Missing | Needed for smooth editor UX |
| Serialization | ❌ Missing | Needed for networking |
| Validation | ❌ Missing | Currently executes blindly |
| Composite commands | ❌ Missing | Needed for transactions |
| Networking | ❌ Missing | Planned for multiplayer |

---

## Acceptance Criteria

### For Kernel V1 (Week 2)
- [ ] Command interface finalized
- [ ] SetParameterCommand, SetMultipleParametersCommand, TransformCommand
- [ ] CommandExecutor with undo/redo stacks
- [ ] Unit tests for commands and executor

### For Editor V1 (Phase C)
- [ ] Command merging (smooth slider UX)
- [ ] Command validation (prevent invalid commands)
- [ ] CompositeCommand for transactions
- [ ] UI shows undo/redo history

### For Platform V1 (Phase B)
- [ ] Command serialization (JSON)
- [ ] Network sync (client → server → clients)
- [ ] Command log recording
- [ ] Replay from log

### For AI (Phase F)
- [ ] AI generates commands from natural language
- [ ] User reviews AI-generated commands before execution

---

## Testing Strategy

### Command Tests

```java
@Test
void setParameterCommand_execute_updatesParameter() {
    var instance = createTestInstance();
    var target = new OpeningInstanceTarget(instance);
    var cmd = new SetParameterCommand("width", 1800.0);
    
    var result = cmd.execute(target);
    
    assertTrue(result.success());
    assertEquals(1800.0, target.instance().getParameters().getDouble("width"));
}

@Test
void setParameterCommand_undo_revertsChange() {
    var instance = createTestInstance();  // width = 1200
    var target = new OpeningInstanceTarget(instance);
    var cmd = new SetParameterCommand("width", 1800.0);
    
    var result = cmd.execute(target);
    var inverse = result.inverse();
    inverse.execute(target);
    
    assertEquals(1200.0, target.instance().getParameters().getDouble("width"));
}
```

### Executor Tests

```java
@Test
void commandExecutor_undo_revertsLastCommand() {
    var executor = new CommandExecutor(target, 100);
    executor.execute(new SetParameterCommand("width", 1800));
    
    executor.undo();
    
    assertEquals(1200.0, target.instance().getParameters().getDouble("width"));
}

@Test
void commandExecutor_redo_reappliesCommand() {
    var executor = new CommandExecutor(target, 100);
    executor.execute(new SetParameterCommand("width", 1800));
    executor.undo();
    
    executor.redo();
    
    assertEquals(1800.0, target.instance().getParameters().getDouble("width"));
}
```

---

## Related Documents

- [kernel/04-generation-pipeline.md](04-generation-pipeline.md) — Commands trigger pipeline
- [kernel/02-parameter-engine.md](02-parameter-engine.md) — Commands modify parameters
- Editor docs (future) — Editor uses commands

---

**Document Status**: ✅ Complete  
**Last Updated**: 2026-07-16  
**Implementation**: ~20% (skeleton exists, needs completion)  
**Next Review**: After Editor V1 (Phase C)
