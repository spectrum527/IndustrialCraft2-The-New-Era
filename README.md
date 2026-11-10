# Industrial Age 2 — 净室重建工作区

> 本目录是 IC2 2.9.40 净室重建项目的**执行工作区**，供 Agent 与协作者直接在此工作。
> 名称：**Industrial Age 2**（mod id `ia2`，包根 `com.industrialage.ia2`）。
> 法律基线：**源码与素材已获书面授权**（存档 `docs/01-legal.md`）；IC2 名称与商标不在授权内。代码 LGPL-3.0，新作素材 CC BY-SA 4.0。
> 可见性：**私有仓库**，素材全部重绘后才转 Public（decisions.md A7/A12）。转 Public 前须把本 README 改写为对外版：只称"受 IndustrialCraft 2 启发的独立项目"，不提授权与原团队（A15）。
> 决策记录：[decisions.md](./decisions.md)（先读，全部已拍板）。Agent 入门：[AGENT-GUIDE.md](./AGENT-GUIDE.md)。

## 目录

```
rebuild/
├── README.md        本文件
├── AGENTS.md        Agent 工作规则（角色、净室纪律、文件所有权）——Agent 必读
├── decisions.md     全部决策表（烂尾处置三选一的最终结论）
├── reference/       反编译源码与资源的【索引】（净室规则：仅 Spec 角色可读）
├── spec/            行为规格（schema 化；实现者唯一的"原版怎么做"信息源）
│   └── _schema/     规格模板与字段约定
├── src/             新实现（Gradle 多模块：common/ neoforge/，P2 落盘）
├── parity/          与 1.19.2 参考实例的期望值对照（P1-T8 采集）
└── assets/          素材复用登记（原版素材拷入规范与来源记录）
```

## 与上层工作区的关系

| 上层目录 | 角色 | 谁可以读 |
|---|---|---|
| `../analysis/` | 原始 jar + 反编译源码 + 解包资源（唯一事实源，`reference/INDEX.md` 指向它） | 仅 Spec 角色（净室纪律，见 AGENTS.md） |
| `../docs/` | 拆解报告（总报告 + 9 份机制文档）与分期计划 | Spec 角色读全文；实现者只读 `spec/` 引用的章节 |
| `../data-workspace/` | 结构化数据（注册表/配方/贴图表等 44 CSV） | 同上：经 spec 引用，实现者不直接读 |
| `../plan/tasks/` | 任务卡 | 所有角色（读自己的卡） |

## 当前进度

- [x] 决策闭合（decisions.md，2026-11-10；负责人问答补充 A11–A21）
- [x] 工作区骨架（本目录）
- [ ] P0：`.gitignore` + 首次提交、`LICENSE`、`docs/01-legal.md`（负责人提供授权原文）、章程 `docs/00-charter.md`、`docs/02-platform.md`、`src/` ModDevGradle 骨架（需 JDK 21）、`../reference-env/` 参考实例
- [ ] P1：`docs/mechanisms/` → `spec/` schema 化重构（spec/README.md 的工序）
- [ ] P1-T8：parity 期望值采集（需 1.19.2 参考实例）
- [ ] P2 起：按《净室重建分期计划》推进（注意其 §6 已并入本工作区决策）

## 快速上手（给 Orchestrator）

1. 读 `decisions.md` + `AGENTS.md`。
2. P0 收尾：把《净室重建分期计划》§11 第一周清单映射到本目录执行。
3. 派发 P1：spec 管线见 `spec/README.md`；机制文档→spec 的映射表在《净室重建分期计划》§10.4。
