# Week 4 工作计划

**时间**: 2026年第4周  
**主题**: 端到端集成与编辑器实用化

## 目标概述

Week 4的核心目标是**让系统真正可用**：完成端到端测试验证整个流程，集成编辑器实现实时参数编辑，补全架构文档形成完整知识体系。

**三大支柱:**
1. **验证完整性**: 端到端测试覆盖从定义到渲染的全流程
2. **提升实用性**: 编辑器集成让用户能实际使用
3. **完善文档**: 补全Architecture Bible，形成完整技术资产

---

## P0 任务 - 必须完成

### Task 1: 端到端NBT持久化测试

**目标**: 在实际Minecraft中验证Opening的完整生命周期

**步骤:**

1. **创建测试世界**
   ```java
   @Test
   void e2e_placeOpeningAndReload() {
       // 1. 创建测试世界
       ServerLevel level = createTestLevel();
       
       // 2. 放置Opening (Door)
       BlockPos pos = new BlockPos(0, 64, 0);
       OpeningInstance door = createDoor(900, 2100);
       placeOpening(level, pos, door);
       
       // 3. 保存世界
       saveLevel(level);
       
       // 4. 重新加载世界
       ServerLevel reloaded = loadLevel();
       
       // 5. 验证Opening仍然存在且数据完整
       OpeningBlockEntity be = getBlockEntity(reloaded, pos);
       assertNotNull(be.getInstance());
       assertEquals(door.typeId(), be.getInstance().typeId());
       assertEquals(door.parameters(), be.getInstance().parameters());
   }
   ```

2. **验证点:**
   - NBT正确写入chunk数据
   - 世界重启后BlockEntity正确恢复
   - 所有参数值完整保留
   - Transform矩阵正确
   - OpeningState正确

3. **边界情况:**
   - 多个Opening在同一chunk
   - 跨chunk边界
   - 世界保存时Opening正在打开/关闭动画中
   - 参数包含特殊值（极大、极小、负数）

**交付物:**
- `EndToEndPersistenceTest.java`
- 测试报告文档

**预估时间**: 1天

---

### Task 2: 编辑器参数实时修改集成

**目标**: 参数编辑器修改参数后，实时重新生成preview

**当前状态:**
- ✅ ParameterEditorScreen UI存在
- ✅ Pipeline可以生成几何体
- ❌ 参数修改后未触发重新生成

**实现:**

```java
public class ParameterEditorScreen extends Screen {
    private final PipelineResultCache previewCache = new PipelineResultCache(10);
    private PipelineResult currentPreview;
    
    @Override
    public void onParameterChanged(String key, ParameterValue newValue) {
        // 1. 更新参数集
        ParameterSet updated = currentParams.with(key, newValue);
        
        // 2. 失效旧缓存
        previewCache.invalidate(openingType.id(), currentParams);
        
        // 3. 异步生成新preview
        CompletableFuture.supplyAsync(() -> {
            return pipelineManager.generate(openingType, updated);
        }).thenAccept(result -> {
            // 4. 更新preview渲染
            this.currentPreview = result;
            this.needsRender = true;
        });
    }
    
    @Override
    public void render(PoseStack matrices, int mouseX, int mouseY, float delta) {
        super.render(matrices, mouseX, mouseY, delta);
        
        if (currentPreview != null) {
            renderPreview(matrices, currentPreview);
        }
    }
}
```

**验证点:**
- 滑动宽度slider，preview实时更新
- 修改材质，preview立即切换
- 无闪烁、卡顿
- 使用缓存避免重复生成

**交付物:**
- 更新 `ParameterEditorScreen.java`
- Preview渲染器实现
- 用户操作演示视频

**预估时间**: 2天

---

### Task 3: 补全Architecture文档

**目标**: 填补Architecture Bible的空白，形成完整知识体系

**待补充文档:**

1. **`docs/architecture/kernel/01-geometry-kernel.md`** ✅ (已存在)
   - 验证完整性
   - 补充缺失部分

2. **`docs/architecture/kernel/02-parameter-engine.md`** ✅ (已存在)
   - 检查是否与实现一致

3. **`docs/architecture/kernel/03-component-system.md`** ✅ (已存在)
   - 补充Component Graph示例

4. **`docs/architecture/editor/02-preview-renderer.md`** ❌ (缺失)
   - Preview渲染架构
   - 相机控制
   - 光照和材质

5. **`docs/architecture/editor/03-command-history.md`** ❌ (缺失)
   - Undo/Redo实现
   - Command模式
   - History stack管理

6. **`docs/architecture/platform/02-block-entity-integration.md`** ❌ (缺失)
   - OpeningBlockEntity详细设计
   - Tick逻辑
   - 与Pipeline集成

7. **`docs/architecture/00-index.md`** ❌ (缺失)
   - 所有文档索引
   - 阅读路径指南
   - Quick reference

**检查清单:**
- [ ] 每个模块都有对应文档
- [ ] 文档之间引用一致
- [ ] 代码示例可运行
- [ ] 图表清晰易懂
- [ ] 与实际代码同步

**交付物:**
- 4篇新文档
- 更新现有文档
- 文档索引

**预估时间**: 2天

---

## P1 任务 - 应该完成

### Task 4: 命令系统基础实现

**目标**: 实现Undo/Redo命令框架

**核心接口:**

```java
public interface Command {
    void execute();
    void undo();
    String description();
}

public class CommandHistory {
    private final Deque<Command> undoStack = new ArrayDeque<>();
    private final Deque<Command> redoStack = new ArrayDeque<>();
    
    public void execute(Command command) {
        command.execute();
        undoStack.push(command);
        redoStack.clear();
    }
    
    public void undo() {
        if (!undoStack.isEmpty()) {
            Command cmd = undoStack.pop();
            cmd.undo();
            redoStack.push(cmd);
        }
    }
    
    public void redo() {
        if (!redoStack.isEmpty()) {
            Command cmd = redoStack.pop();
            cmd.execute();
            undoStack.push(cmd);
        }
    }
}
```

**实现命令:**

1. **SetParameterCommand**
   ```java
   public class SetParameterCommand implements Command {
       private final OpeningInstance instance;
       private final String key;
       private final ParameterValue oldValue;
       private final ParameterValue newValue;
       
       @Override
       public void execute() {
           instance.setParameter(key, newValue);
       }
       
       @Override
       public void undo() {
           instance.setParameter(key, oldValue);
       }
   }
   ```

2. **PlaceOpeningCommand**
3. **DeleteOpeningCommand**
4. **TransformOpeningCommand**

**交付物:**
- `Command.java` 接口
- `CommandHistory.java` 实现
- 4个基础命令
- 单元测试

**预估时间**: 1.5天

---

### Task 5: Profile资产热重载

**目标**: 修改profile文件后，无需重启即可看到效果

**实现:**

```java
public class ProfileCatalogWatcher {
    private final WatchService watchService;
    private final Path profileDir;
    private final ProfileCatalogRegistry registry;
    
    public void startWatching() {
        profileDir.register(watchService, ENTRY_MODIFY, ENTRY_CREATE, ENTRY_DELETE);
        
        while (true) {
            WatchKey key = watchService.take();
            
            for (WatchEvent<?> event : key.pollEvents()) {
                Path changed = (Path) event.context();
                
                if (changed.toString().endsWith(".json")) {
                    reloadProfile(changed);
                }
            }
            
            key.reset();
        }
    }
    
    private void reloadProfile(Path profileFile) {
        // 1. 重新加载profile
        ProfileDefinition profile = loadProfile(profileFile);
        
        // 2. 更新注册表
        registry.register(profile.id(), profile);
        
        // 3. 失效所有使用该profile的缓存
        pipelineCache.invalidateProfileUsers(profile.id());
        
        // 4. 通知编辑器刷新
        eventBus.post(new ProfileReloadedEvent(profile.id()));
    }
}
```

**验证:**
- 修改 `profiles/frame_l50.json`
- 编辑器自动刷新preview
- 已放置的Opening在重新加载后使用新profile

**交付物:**
- `ProfileCatalogWatcher.java`
- 热重载测试
- 用户指南

**预估时间**: 1天

---

### Task 6: 性能优化总结文档

**目标**: 整理Week 2-3的性能工作，形成优化指南

**内容:**

1. **现状基准**
   - Cold generation: 80-150ms
   - Cached generation: <1ms
   - Memory per instance: ~85KB

2. **优化手段**
   - 结果缓存 (100倍提升)
   - Profile预加载
   - Mesh批处理

3. **优化路径图**
   - 短期 (Week 4-5)
     - [ ] 增量更新（只重新生成变化的component）
     - [ ] GPU加速的mesh生成
   - 中期 (Month 2)
     - [ ] LOD系统
     - [ ] 异步生成
   - 长期 (Month 3+)
     - [ ] 分布式缓存
     - [ ] SIMD向量化

4. **监控指标**
   - P50, P95, P99 generation time
   - Cache hit rate
   - Memory footprint
   - Frame time影响

**交付物:**
- `docs/performance/optimization-guide.md`
- 性能监控dashboard设计

**预估时间**: 0.5天

---

## P2 任务 - 可选完成

### Task 7: 实现Casement Window (开启窗)

**目标**: 继Door之后的第二个完整opening类型

**特点:**
- 带铰链的可开启窗扇
- 开启角度参数 (0-90度)
- 支持内开/外开

**实现复用:**
- 使用现有RectangularWindowGenerator
- 复用Door的铰链逻辑
- Golden测试框架直接复用

**预估时间**: 1天

---

### Task 8: 高级约束表达式

**目标**: 支持更复杂的参数约束

**当前支持:**
```
width > 100
width < 3000
width / height > 0.5
```

**新增支持:**
```
width % 100 == 0  // 必须是100的倍数
panel_count in [1, 2, 4]  // 枚举值
frame_depth >= thickness + 20  // 跨参数约束
if (has_glass) then glass_thickness > 0  // 条件约束
```

**实现:**
- 扩展表达式解析器
- 添加模运算、枚举、条件
- 更新文档

**预估时间**: 1.5天

---

### Task 9: 编辑器UI改进

**目标**: 提升编辑器易用性

**改进项:**
1. 参数分组 (Dimensions, Materials, Hardware)
2. 单位显示和转换 (mm/cm/m)
3. 预设配置 (Standard sizes, Custom)
4. 实时验证反馈 (红色标记非法值)
5. 键盘快捷键 (Ctrl+Z undo, Ctrl+Y redo)

**预估时间**: 1天

---

## 时间分配

| 任务 | 优先级 | 预估时间 | 负责人 |
|------|--------|----------|--------|
| Task 1: 端到端NBT测试 | P0 | 1天 | Core |
| Task 2: 编辑器参数集成 | P0 | 2天 | Editor |
| Task 3: 补全架构文档 | P0 | 2天 | Doc |
| Task 4: 命令系统 | P1 | 1.5天 | Editor |
| Task 5: Profile热重载 | P1 | 1天 | Assets |
| Task 6: 性能优化文档 | P1 | 0.5天 | Performance |
| Task 7: Casement Window | P2 | 1天 | Opening |
| Task 8: 高级约束 | P2 | 1.5天 | Constraint |
| Task 9: UI改进 | P2 | 1天 | Editor |

**P0总计**: 5天（1周，顺序执行）  
**P1总计**: 3天（可并行，选择性完成）  
**P2总计**: 4天（缓冲，根据进度决定）

---

## 里程碑检查点

### Day 1-2: 验证完整性
- [ ] 端到端NBT测试通过
- [ ] 证明Opening可以在实际游戏中保存/加载
- [ ] 发现并修复任何NBT相关bug

### Day 3-4: 编辑器集成
- [ ] 参数修改实时生效
- [ ] Preview渲染流畅
- [ ] 缓存加速明显

### Day 5: 文档补全
- [ ] 所有核心模块都有文档
- [ ] 文档索引完成
- [ ] 新开发者可以通过文档快速上手

### Day 6-7 (Stretch): 命令与资产
- [ ] Undo/Redo可用
- [ ] Profile热重载工作
- [ ] 性能优化文档完成

---

## 风险与应对

### 风险1: 端到端测试环境复杂

**描述**: 需要启动完整Minecraft服务器，创建世界，保存/加载

**影响**: 可能耗时超出预期

**应对:**
- 使用Minecraft test framework (如有)
- 创建最小化测试世界
- Mock不必要的部分
- 如果环境搭建困难，改为手动测试 + 详细记录

### 风险2: 编辑器集成遇到渲染问题

**描述**: Preview渲染可能遇到OpenGL/渲染管线问题

**影响**: 延迟编辑器可用性

**应对:**
- 先实现逻辑部分（参数更新）
- 渲染部分可以先用简化版（线框）
- 寻求有经验的图形程序员帮助
- 如果渲染太复杂，改为重新打开GUI触发更新

### 风险3: 文档编写时间不足

**描述**: 补全4篇文档可能需要更多时间

**影响**: 文档质量下降或不完整

**应对:**
- 优先核心文档 (Platform, Editor)
- 使用模板加速编写
- 代码注释转文档
- 部分文档可以留待Week 5补充

---

## 成功标准

Week 4结束时，应该达到以下状态：

### 功能完整性
- ✅ Opening可以在实际游戏中完整工作（放置、保存、加载）
- ✅ 编辑器可以实时修改参数并看到效果
- ✅ Undo/Redo基本可用

### 代码质量
- ✅ 端到端测试覆盖关键路径
- ✅ 无已知critical bug
- ✅ 代码结构清晰，符合架构设计

### 文档完善
- ✅ Architecture Bible完整（所有模块有文档）
- ✅ 新开发者可以通过文档了解系统
- ✅ 每个功能都有使用示例

### 性能稳定
- ✅ 编辑器操作流畅（<100ms响应）
- ✅ 缓存hit rate > 60%
- ✅ 无内存泄漏

---

## Week 4之后展望

完成Week 4后，Aperture将达到**Alpha可用**状态：

**Alpha可用的定义:**
- 核心功能完整（定义、生成、编辑、持久化）
- 至少2种opening类型完整实现（Door, Window）
- 编辑器基本可用
- 文档完整
- 性能可接受

**后续方向:**

### Month 2: 扩展与优化
- 更多opening类型 (Sliding door, Bay window, Skylight)
- 高级功能 (材质系统, 光照系统)
- 性能优化 (LOD, 异步生成)
- 多人游戏支持

### Month 3: 生态与工具
- Asset创作工具
- 社区profile/material库
- API文档与SDK
- 插件系统

### Long-term: 商业化与影响力
- 发布到CurseForge/Modrinth
- 建立社区
- 与建筑师合作
- 拓展到其他建筑元素

---

## 每日计划 (建议)

### Day 1 (Monday)
- 上午: 搭建端到端测试环境
- 下午: 实现基础NBT测试用例
- 晚上: 运行测试，记录问题

### Day 2 (Tuesday)
- 上午: 修复Day 1发现的bug
- 下午: 完善端到端测试（边界情况）
- 晚上: 测试通过，编写总结

### Day 3 (Wednesday)
- 上午: 设计编辑器集成架构
- 下午: 实现参数变化监听
- 晚上: 集成Pipeline生成

### Day 4 (Thursday)
- 上午: 实现Preview渲染
- 下午: 缓存集成和优化
- 晚上: 完整流程测试

### Day 5 (Friday)
- 上午: 审查现有文档
- 下午: 编写2篇新文档
- 晚上: 编写文档索引

### Day 6 (Saturday)
- 上午: 继续文档编写
- 下午: 命令系统基础实现
- 晚上: 代码review和整理

### Day 7 (Sunday)
- 上午: Profile热重载实现
- 下午: 性能优化文档
- 晚上: Week 4总结，Week 5计划

---

## 关键交付物清单

### 代码
- [ ] `EndToEndPersistenceTest.java`
- [ ] 更新 `ParameterEditorScreen.java`
- [ ] `PreviewRenderer.java`
- [ ] `Command.java` + `CommandHistory.java`
- [ ] 4个基础Command实现
- [ ] `ProfileCatalogWatcher.java` (if P1 completed)

### 文档
- [ ] `docs/architecture/editor/02-preview-renderer.md`
- [ ] `docs/architecture/editor/03-command-history.md`
- [ ] `docs/architecture/platform/02-block-entity-integration.md`
- [ ] `docs/architecture/00-index.md`
- [ ] `docs/performance/optimization-guide.md` (if P1 completed)
- [ ] `docs/progress/week-4-summary.md`

### 测试
- [ ] 端到端NBT测试套件
- [ ] 命令系统单元测试
- [ ] 编辑器集成手动测试报告

### 演示
- [ ] 编辑器实时修改演示视频
- [ ] 端到端持久化演示
- [ ] Undo/Redo演示 (if completed)

---

**Let's make Aperture truly usable in Week 4! 🚀**

---

**Created**: 2026-07-16  
**Target Start**: Week 4  
**Owner**: Aperture Development Team
