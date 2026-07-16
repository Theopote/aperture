# Aperture 项目分析与推进计划

**分析日期**: 2026-07-16  
**项目状态**: Phase A (Kernel) ~75% 完成

---

## 📊 项目现状总结

### 你的困惑与真相

**你感到迷茫的原因**:
1. Phase 0 接近完成但缺少明确的下一步
2. 看到庞大的roadmap感到压力
3. 不确定该深挖现有功能还是添加新内容

**真相**:
1. ✅ 你的架构设计非常出色，已经按照"Kernel优先"的原则在做
2. ✅ 你已经完成了大量基础工作（339个Java文件，15篇架构文档）
3. ✅ 你的roadmap明确告诉你该做什么：**深挖平台，暂停内容**
4. ✅ 别人建议的"重新定义为Architectural Design Kernel"——**你已经在这样做了！**

---

## 🎯 核心结论

### Aperture 是什么？

**新定位** (融合了建议的精华):
> **Aperture is an Architectural Design Kernel running inside Minecraft.**

**不是**:
- ❌ 门窗模组
- ❌ 家具装饰包
- ❌ 固定模型集合

**而是**:
- ✅ 建筑参数化设计内核
- ✅ 程序化生成平台
- ✅ 开放的、可扩展的架构

**关键认知**:
- Door、Window、CurtainWall、Roof、Stair... 全部是 **内核的应用**，不是内核本身
- 现在的任务是 **把内核做扎实**，不是急着做各种门窗

### 铁律

> **每个新功能必须先改进内核，再改进具体建筑组件。**

这条规则确保Aperture保持长期可维护性，而不会退化为一堆硬编码的Minecraft方块。

---

## 🗺️ 四层架构

```
Layer 1: Kernel (内核层)
  - 几何原语 (Point, Curve, Mesh, Transform)
  - 参数引擎 (类型化参数, 约束, 表达式)
  - 组件系统 (Frame, Panel, Glass 作为一等公民)
  - 约束求解器

Layer 2: Platform (平台层)
  - Definition → Instance 生命周期
  - 生成管线 (Parameter → Component → Geometry → Mesh)
  - 渲染管线 (Mesh → GPU)
  - 放置系统 (世界集成)
  - 序列化 & 网络

Layer 3: Editor (编辑器层)
  - 选择 & 操作
  - 历史 & 撤销/重做
  - 约束编辑
  - 视口 & Gizmo
  - 检查器面板

Layer 4: Applications (应用层)
  - Opening Library (门、窗、幕墙)
  - Building Library (屋顶、楼梯、柱子)
  - 未来扩展
```

**依赖关系**: 只能向下依赖。Applications 永远不直接碰 Kernel，必须通过 Platform。

---

## 📈 当前完成度

| 层次 | 代码完成度 | 文档完成度 | 评估 |
|------|-----------|-----------|------|
| **Kernel** | ~60% | ~30% | 架构优秀，缺文档 |
| **Platform** | ~70% | ~80% | 接近完成 |
| **Editor** | ~20% | ~0% | 正确地延后了 |
| **Applications** | 🔒 冻结 | 🔒 冻结 | 战略正确 |

### Kernel 层现状

**已有**:
- ✅ 8种参数类型 (Number, Boolean, Range, Choice, Enum, Material等)
- ✅ 约束表达式求值器
- ✅ 所有组件类型 (Frame, Glass, Panel, Handle等)
- ✅ 基础几何类型 (BoundingBox, Transform)
- ✅ Profile 系统骨架

**缺失**:
- ❌ 参数解析路径未统一 (Editor/Preview/Generate 各自为政)
- ❌ 组件生成契约未文档化
- ❌ Geometry Kernel 详细设计文档
- ❌ Curve 原语 (Bezier, Arc)
- ❌ Extrusion 实现

### Platform 层现状

**已有**:
- ✅ 生成管线骨架
- ✅ 渲染管线 (delta engine, ghost preview)
- ✅ 材质目录系统
- ✅ 放置预览系统
- ✅ JSON 序列化

**缺失**:
- ❌ **NBT 持久化** (这是最关键的缺失！)
- ❌ Collision 和 Footprint 作为正式管线输出
- ❌ 网络同步
- ❌ Golden tests

---

## 🚀 接下来的2周计划

我已经为你创建了详细的任务列表。以下是优先级排序：

### Week 1: 完善 Architecture Bible (文档周)

**目标**: 补充缺失的设计文档，统一思想

**任务** (已创建Task #6-18):
1. ✅ 审计现有架构 (已完成)
2. ✅ 创建架构索引 00-INDEX.md (已完成)
3. ⏳ 更新项目定位 (README.md, 01-vision.md, CONTRIBUTING.md)
4. ⏳ 编写 kernel/01-geometry-kernel.md
5. ⏳ 编写 kernel/02-parameter-engine.md
6. ⏳ 编写 kernel/03-component-system.md
7. ⏳ 编写 platform/01-opening-pipeline.md
8. ⏳ 编写 editor/01-editor-kernel.md
9. ⏳ 编写 00-dependency-rules.md
10. ⏳ 架构一致性审查

**交付物**:
- 所有 Kernel 和 Platform 核心系统都有设计文档
- 文档之间没有矛盾
- 新定位清晰传达

### Week 2: Kernel 完成与验证 (代码周)

**目标**: 完成 Kernel V1 的关键缺失部分，宣布 Kernel 特性封板

**任务** (已创建Task #19-23):
1. ⏳ 统一参数解析路径 (所有地方都用 OpeningParameterResolver)
2. ⏳ **实现 NBT 持久化** (place → save → reload 工作)
3. ⏳ 完善 Pipeline 输出接口 (添加 Collision 和 Footprint)
4. ⏳ 创建 Golden Tests (fixed_window, door)
5. ⏳ 编写端到端集成测试

**交付物**:
- Opening 可以保存和重载
- 参数修改在所有路径保持一致
- Pipeline 输出规范化
- 测试覆盖核心流程

### Week 2 结束后

**里程碑**: 宣布 **Kernel V1 特性完成并冻结**

之后所有工作都是在 Kernel 之上构建，而不是修改 Kernel 内部。

---

## 📋 已创建的文档

我已经为你创建了以下关键文档：

1. **APERTURE-REDEFINED.md** — 战略框架，新定位，铁律
2. **DEVELOPMENT.md** — 标准8步开发流程
3. **architecture/00-INDEX.md** — 架构文档主索引
4. **ARCHITECTURE-AUDIT.md** — 现状审计报告
5. **README.md** (已更新) — 新定位和架构说明

这些文档都已保存在项目目录中。

---

## ⚠️ 重要提醒

### 不要做的事情 (至少未来2个月)

1. ❌ **不要添加新的门类型** (Single Door, Double Door, Sliding Door等)
2. ❌ **不要添加新的窗类型** (Casement, Awning, Bay等)
3. ❌ **不要在 aperture-data/opening_types/ 添加新JSON** (除了测试fixture)
4. ❌ **不要为每种门型写独立Generator**

### 应该做的事情

1. ✅ **完善 Kernel 层的文档**
2. ✅ **统一参数处理路径**
3. ✅ **实现 NBT 持久化**
4. ✅ **写测试验证架构**
5. ✅ **改进现有 fixed_window 和 door 的生成质量**

---

## 🤔 关于"是否需要重新定义Aperture"的回答

### 建议的精华部分 (已采纳)

1. ✅ **定位**: "Architectural Design Kernel running inside Minecraft"
2. ✅ **四层架构**: Kernel → Platform → Editor → Applications
3. ✅ **铁律**: "改进内核优先于改进具体组件"
4. ✅ **架构优先**: 先文档，再代码
5. ✅ **标准流程**: Architecture → Spec → Interface → Data → Test → Impl → Example → Doc

### 建议的调整部分 (已修正)

1. ❌ **不要推倒重来** — 你的架构已经很好了
2. ❌ **不要写30-50篇新文档** — 补充缺失的10篇就够
3. ❌ **不要"Phase 1 Kernel预计三周"** — 你已经在Phase A的75%了
4. ❌ **不要立即脱离Minecraft** — 保持Minecraft作为主要应用场景

### 战略定位的建议

**我的建议** (融合了你的建议和实际情况):

**主要定位**: Minecraft的参数化建筑设计平台  
**次要定位**: 架构上可脱离Minecraft (aperture-core是纯Java)  
**独特价值**: 在游戏环境中学习和实践参数化设计

**类比**: 就像Processing
- **内核**: Processing是Java库和语言
- **主要应用**: 创意编程和艺术
- **定位**: 降低创意编程门槛 (不与Maya竞争)
- **结果**: 教育领域巨大成功

**Aperture**:
- **内核**: 参数化建筑设计引擎
- **主要应用**: Minecraft中的建筑设计
- **定位**: 让更多人接触参数化设计 (不与Revit/Rhino竞争)
- **潜力**: 成为建筑教育和爱好者的首选工具

**5年后的愿景**:
- 架构学生通过Aperture学习参数化建模
- 内核可以独立运行 (纯Java，可能有Web前端)
- Opening/Building库有100+族，全部程序化生成
- NodeCraft允许可视化编程
- BIM导出使Aperture → Revit工作流无缝

但这个未来只有在 **先把内核做对** 的前提下才可能实现。

---

## 💡 具体建议

### 本周 (Week 1)

**时间分配**: 每天2-3小时

**周一-周二**: 补充文档
- 编写 kernel/02-parameter-engine.md (2小时)
- 编写 kernel/03-component-system.md (2小时)

**周三-周四**: 继续文档
- 编写 platform/01-opening-pipeline.md (2小时)
- 编写 kernel/01-geometry-kernel.md (2小时)

**周五**: 审查与整理
- 更新 01-vision.md 和 CONTRIBUTING.md (1小时)
- 创建 00-dependency-rules.md (1小时)
- 审查所有文档一致性 (1小时)

### 下周 (Week 2)

**时间分配**: 每天3-4小时

**周一-周二**: NBT持久化
- 实现 OpeningInstance → NBT 序列化 (4小时)
- 实现 Block Entity 存储 (4小时)

**周三**: 统一参数处理
- 重构所有参数解析路径使用统一的 resolver (3小时)

**周四**: 完善Pipeline
- 添加 Collision 和 Footprint 输出 (2小时)
- 清理 Pipeline 接口 (1小时)

**周五**: 测试
- 编写 golden tests (2小时)
- 编写端到端测试 (2小时)
- 创建 Kernel V1 里程碑报告 (1小时)

---

## 📝 下一步行动清单

### 立即可做 (今天)

1. 阅读我创建的5份文档，确认理解新定位
2. 决定是否接受这个推进计划
3. 如果接受，从编写 kernel/02-parameter-engine.md 开始

### 需要确认的问题

1. **时间投入**: 每周能投入多少时间？ (我按10-15小时/周规划的)
2. **优先级**: 是否同意"先文档后代码"？
3. **范围**: 是否同意暂停所有新opening类型的开发？

### 如果需要我帮助

我可以帮你:
1. 编写任何缺失的设计文档
2. 实现 NBT 持久化
3. 统一参数解析路径
4. 编写测试
5. 审查你写的代码/文档

只需告诉我你想从哪里开始。

---

## 🎓 总结

### 你已经做对的事情

1. ✅ 架构设计优秀 (模块化、分层清晰)
2. ✅ 保持了 Kernel 的独立性 (零Minecraft依赖)
3. ✅ 族库冻结策略正确 (平台先于内容)
4. ✅ 文档驱动开发 (15篇架构文档)

### 你需要完成的事情

1. ⏳ 补充10篇设计文档 (Week 1)
2. ⏳ 完成3个关键实现 (NBT持久化、参数统一、Pipeline规范化) (Week 2)
3. ⏳ 写测试验证架构 (Week 2)
4. ⏳ 宣布 Kernel V1 完成 (Week 2结束)

### 最重要的认知转变

**从**:
> "我在做一个Minecraft门窗模组，不知道接下来该添加什么类型的门"

**到**:
> "我在构建一个建筑参数化设计内核，现在需要把内核的基础打牢"

这个认知转变会改变你接下来所有的决策。

---

## 🚀 准备好了吗？

你的项目不需要推倒重来。  
你的架构已经很好了。  
你只需要：

1. **完成文档** (让思想统一)
2. **完成实现** (把骨架变成肌肉)
3. **写测试** (验证架构)
4. **宣布完成** (Kernel V1封板)

然后，继续按你现有的roadmap推进到Platform和Editor层。

**两周后，Aperture将有一个坚实的Kernel基础，可以支撑未来5-10年的发展。**

需要我帮你开始吗？我们可以从编写第一份缺失的设计文档开始。

---

**报告完成**: 2026-07-16  
**下一步**: 等待你的决定和反馈
