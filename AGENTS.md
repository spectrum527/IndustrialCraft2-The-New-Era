# Agent 工作规则（Industrial Age 2 净室重建）

> 本文件是所有参与 rebuild/ 的 Agent 的硬约束。任务卡可以细化，但不得与本文件冲突。

## 1. 角色与净室纪律

净室三角色隔离**保留**（工程纪律：保证新架构自由、避免反编译习气渗入实现；相似度检查作为质量门禁继续跑）。

| 角色 | 可读 | 禁止 |
|---|---|---|
| **Spec 角色**（Spec Lead / Spec Agent） | `reference/INDEX.md` 指向的全部反编译源码与资源（2.9.40 与 2.8.222 两版）、`../docs/`、`../data-workspace/`、`../missing-features/` | 把反编译代码片段（>5 行）、SRG 名、行号写进 `spec/`（规格只描述行为） |
| **实现角色**（Core/Platform/Content/Client/QA） | `spec/`、`decisions.md`、公开 API 文档、测试与 parity 期望值 | 读 `reference/`、`../analysis/`、`../analysis-1122/`、`../missing-features/`、`../docs/mechanisms/`、`../data-workspace/`；在上下文中出现这些路径 |
| **资产角色**（Artist/Audio） | `assets/` 登记表、原版素材（经 `reference/INDEX.md` 定位，仅用于复用/重绘） | — |

补充：

- 任务卡只引用 `spec/` 章节号；PR 描述必须填"依据的 spec 章节"。
- 版本基线（decisions.md A23）：2.9.40 为行为基线，2.8.222 只用于缺失功能与交叉验证；两版不一致时 Spec 角色登记为 B 表"待裁决"项，不得自行取舍。
- 相似度检查同时对照**两版**反编译源码（`--reference` 传两次）；CI 对 `src/` vs `reference/` 跑 5-gram Jaccard 相似度：单文件 >25% 失败、>15% 警告、连续 8 行相同直接失败。
- 例外：**素材与数值**。原版贴图/音效/模型私有阶段按 decisions.md A7 复用，但存放于独立私有仓库 `ia2-legacy-assets`、不进本仓库历史（A22），登记于 `assets/SOURCES.md`；转 Public 前须全部替换为新作（CC BY-SA 4.0，A21）。数值与公式是事实，spec 里照抄。

## 2. 文件所有权

| 目录 | 所有者 |
|---|---|
| `spec/` | Spec 角色 |
| `src/common/energy|machine|recipe|fluid|crop|reactor/` | Core Agent |
| `src/neoforge/`（含 1.21.1 与 26.1 分叉） | Platform Agent |
| `src/common/content/` | Content Agent（按波次分） |
| `src/client/`、`src/compat/` | Client Agent |
| `src/datagen/` | Content Agent + Platform Agent |
| `src/test/` | QA Agent |
| `assets/` | Artist/Audio Agent |
| `parity/` | QA Agent |
| `docs/` | Docs Agent（`00-charter.md`、`01-legal.md`、`02-platform.md` 归 Orchestrator） |
| `decisions.md` | Orchestrator（唯一写入者；每次改动需负责人审批，decisions.md A16） |

路径约定（decisions.md A17）：任务卡中的 `common/`、`neoforge/`、`datagen/`、`test/` 等一律相对 `rebuild/src/`。所有 PR 合并需负责人审批（A16）。

跨目录修改需 Orchestrator 批准。`decisions.md` 是唯一决策事实源，发现任务与决策冲突时：停手 → 报 Orchestrator → 改决策或改任务。

## 3. 平台与版本（decisions.md A5）

- 主基线 **NeoForge 1.21.1**（Java 21）：MVP/G5 门禁在此验收。
- 同步目标 **NeoForge 26.1**：common 层不得 import 版本专属 API；版本差异收敛在 `src/neoforge/` 的版本子模块中。
- 1.19.2 只是 parity 参考环境（跑原版 jar 采期望值），**不是发布目标**。

## 4. 工程规约

- Java 21；4 空格缩进；`final`/`record`/`sealed` 按宜使用。
- 公开 API 必须有 javadoc；禁止 `System.out`（用 SLF4J）；核心层禁止 import 平台类（CI 检查）。
- 所有新增逻辑必须有单测或 parity 场景；PR ≤ 400 行（生成代码/数据除外）。
- 网络层禁止反射序列化与从网络数据加载类（decisions.md：不做 DataEncoder 复刻）。
- 配置键沿用原版活键命名（`balance/energy/...` 等 58 键，见 `../data-workspace/12-reference/config_keys_full.csv` 的 consumer 列非空项——该文件仅 Spec 角色可读，活跃键清单已入 spec）。
- 每个内容任务的验收必须包含 decisions.md B 表中挂在该任务下的"修复项"回归测试。

## 5. 交接协议

完成任务必须留下三件套，缺一不合并：PR（代码+测试+spec 引用）、交接说明（做了/没做/怎么验证/下游依赖）、任务板状态更新。
