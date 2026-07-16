# 端到端NBT持久化测试文档

## 概述

端到端测试验证Opening在实际Minecraft游戏中的完整生命周期，从放置到保存再到加载，确保所有数据正确持久化。

## 测试策略

### 使用Minecraft GameTest框架

Minecraft提供了GameTest框架用于在模拟游戏环境中进行测试：

```java
@GameTest(template = "aperture:empty_3x3x3")
public static void doorPersistence_basicRoundTrip(GameTestHelper helper) {
    // 测试逻辑
    helper.succeed();
}
```

**优势:**
- 真实的游戏环境
- BlockEntity实际运行
- NBT序列化/反序列化真实执行
- 可以测试多个方块交互

## 测试用例

### 1. 基础往返测试

**目标**: 验证Door的基本保存/加载

```java
@GameTest
public static void doorPersistence_basicRoundTrip(GameTestHelper helper) {
    // 1. 创建door instance
    OpeningInstance door = createDoor(900, 2100);
    
    // 2. 放置到世界
    helper.setBlock(pos, OpeningBlock.INSTANCE);
    obe.setInstance(door);
    
    // 3. 保存NBT
    CompoundTag tag = obe.getUpdateTag();
    
    // 4. 清除并重新加载
    helper.setBlock(pos, Blocks.AIR);
    helper.setBlock(pos, OpeningBlock.INSTANCE);
    obe2.load(tag);
    
    // 5. 验证数据完整
    assertEquals(door.typeId(), loaded.typeId());
    assertEquals(door.parameters(), loaded.parameters());
}
```

**验证点:**
- Instance ID preserved
- Type ID preserved
- All parameter values intact
- Transform matrix correct
- Opening state correct

### 2. 固定窗测试

**目标**: 验证Fixed Window持久化

```java
@GameTest
public static void fixedWindowPersistence_roundTrip(GameTestHelper helper) {
    OpeningInstance window = createFixedWindow(1200, 1500);
    // ... 保存/加载逻辑
    assertEquals(1200.0, loaded.parameters().get("width").asLength());
}
```

### 3. 多个Opening同chunk测试

**目标**: 验证多个Opening互不干扰

```java
@GameTest(template = "aperture:empty_5x5x5")
public static void multiplePersistence_sameChunk(GameTestHelper helper) {
    // 放置3个不同的Opening
    placeAndSetInstance(helper, pos1, door);
    placeAndSetInstance(helper, pos2, window1);
    placeAndSetInstance(helper, pos3, window2);
    
    // 保存所有
    // 重新加载
    // 验证所有都正确
}
```

**验证点:**
- 每个Opening独立保存
- 没有数据混淆
- 所有参数正确

### 4. 所有参数类型测试

**目标**: 验证8种参数类型都正确持久化

```java
@GameTest
public static void allParameterTypes_persist(GameTestHelper helper) {
    ParameterSet params = ParameterSet.builder()
        .put("length_param", ParameterValue.length(1500.0))
        .put("angle_param", ParameterValue.angle(45.0))
        .put("count_param", ParameterValue.count(3))
        .put("ratio_param", ParameterValue.number(0.75))
        .put("bool_param", ParameterValue.bool(true))
        .put("material_param", ParameterValue.materialRef("aperture:oak"))
        .put("profile_param", ParameterValue.profileRef("aperture:frame_l50"))
        .put("enum_param", ParameterValue.enumValue("left"))
        .build();
    
    // 保存/加载
    // 逐个验证
}
```

**参数类型覆盖:**
- ✅ Length (mm)
- ✅ Angle (degrees)
- ✅ Count (integer)
- ✅ Number (ratio/percentage)
- ✅ Bool (true/false)
- ✅ MaterialRef (resource ID)
- ✅ ProfileRef (resource ID)
- ✅ EnumValue (string)

## 实现变更

### OpeningBlockEntity重构

**之前**: 只存储UUID，通过runtime查找实例
```java
private @Nullable UUID instanceId;

public Optional<OpeningInstance> resolveInstance() {
    return runtime.instances().findById(instanceId);
}
```

**之后**: 直接存储完整实例
```java
private @Nullable OpeningInstance instance;

@Override
protected void saveAdditional(ValueOutput output) {
    if (instance != null) {
        OpeningInstanceNbtCodec.write(output, instance);
    }
}
```

**优势:**
- 不依赖external registry
- 数据完全自包含
- 更可靠的持久化
- 简化代码逻辑

## 运行测试

### 使用Fabric GameTest

```bash
# 运行所有端到端测试
./gradlew :aperture-fabric:runGametest

# 运行特定测试
./gradlew :aperture-fabric:runGametest \
  --tests "OpeningPersistenceE2ETest.doorPersistence_basicRoundTrip"
```

### 手动测试流程

如果GameTest环境配置困难，可以手动测试：

1. **启动游戏**
   ```bash
   ./gradlew :aperture-fabric:runClient
   ```

2. **创建测试世界**
   - 新建creative世界
   - 找到平坦区域

3. **放置Opening**
   - 使用Opening放置工具
   - 创建Door (900x2100)
   - 记录坐标 (如 100, 64, 100)

4. **保存世界**
   - 保存并退出到主菜单

5. **重新加载**
   - 重新进入世界
   - 导航到同一坐标

6. **验证**
   - Opening仍然存在
   - F3+H查看NBT数据
   - 参数编辑器显示正确值

## 边界情况

### 1. 跨Chunk边界

Opening跨越两个chunk边界时：
- 主BlockEntity在一个chunk
- 部分几何体在另一个chunk
- 两个chunk分别保存/加载

**测试**: 放置在chunk边界 (x=16n)

### 2. 动画状态中保存

Door正在打开/关闭动画中保存世界：
- OpeningState包含当前openRatio
- 重新加载后动画状态恢复

**测试**: 触发door打开，在动画中间保存

### 3. 极端参数值

参数包含极值：
- 最小值 (width=100mm)
- 最大值 (height=10000mm)
- 零值 (glass_ratio=0)
- 负值 (如果允许)

**测试**: 使用极端参数值

### 4. 缺失资产引用

参数引用不存在的材质/profile：
- material_ref = "aperture:nonexistent"
- 加载时找不到资产

**测试**: 使用无效引用，验证fallback

## NBT数据结构

### BlockEntity NBT格式

```nbt
{
    id: "aperture:opening",
    x: 100,
    y: 64,
    z: 100,
    hasInstance: 1b,
    schemaVersion: 1,
    instanceId: [I; 123, 456, 789, 101],
    typeId: "aperture:door",
    parameters: {
        width: {type: "length", value: 900.0d},
        height: {type: "length", value: 2100.0d},
        panel_count: {type: "count", value: 1}
    },
    transform: {...},
    host: {...},
    state: {...},
    revision: 0L
}
```

### 验证工具

```bash
# 导出world NBT
/data get block 100 64 100

# 查看特定tag
/data get block 100 64 100 parameters.width
```

## 性能考虑

### NBT序列化开销

每个OpeningBlockEntity的NBT大小：
- 基础数据: ~200 bytes
- 每个参数: ~50 bytes
- 8个参数: ~600 bytes
- **总计**: ~1KB per opening

### Chunk保存时间

假设一个chunk有10个Opening：
- 10 × 1KB = 10KB额外数据
- 序列化时间: <1ms
- 可以接受

### 内存占用

加载的Opening保存在BlockEntity中：
- 每个OpeningInstance: ~1KB (in memory)
- 1000个opening: ~1MB
- 可以接受

## 故障排查

### 问题1: 加载后instance为null

**原因**: NBT反序列化失败

**检查:**
```java
@Override
protected void loadAdditional(ValueInput input) {
    try {
        instance = OpeningInstanceNbtCodec.read(input);
    } catch (Exception e) {
        LOGGER.error("Failed to load opening instance", e);
        instance = null;
    }
}
```

### 问题2: 参数值不正确

**原因**: ParameterValue类型不匹配

**检查:**
- NBT中的type字段
- ParameterValue.type()匹配
- 正确的as*()方法调用

### 问题3: Transform/State丢失

**原因**: 可选字段未正确处理

**检查:**
```java
// 保存时
if (instance.transform() != null) {
    writeTransform(output, instance.transform());
}

// 加载时
Transform transform = readTransform(input).orElse(null);
```

## 成功标准

端到端测试通过的标准：

### 功能完整性
- ✅ 所有Opening类型可以保存/加载
- ✅ 所有8种参数类型正确持久化
- ✅ Transform和State正确保存
- ✅ 多个Opening互不干扰

### 数据完整性
- ✅ 无数据丢失
- ✅ 无精度损失
- ✅ UUID保持唯一
- ✅ 引用有效性检查

### 性能
- ✅ 序列化时间 <1ms per opening
- ✅ NBT大小 <2KB per opening
- ✅ 内存占用 <2KB per opening

### 健壮性
- ✅ 处理缺失字段
- ✅ 处理无效引用
- ✅ 版本兼容性
- ✅ 错误恢复

## 后续工作

完成端到端测试后：

1. **CI集成**
   - 自动运行GameTest
   - 每次PR都验证

2. **更多场景**
   - 大量Opening (性能测试)
   - 复杂Opening (curtain wall)
   - 边界情况覆盖

3. **迁移测试**
   - Schema版本升级
   - 向后兼容性
   - 数据迁移

4. **监控**
   - 持久化失败率
   - 序列化时间
   - NBT大小分布

---

**Created**: 2026-07-16  
**Status**: Implementation Complete, Testing Pending
