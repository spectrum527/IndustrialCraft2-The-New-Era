# Agent 角色与交接

本页供所有角色查阅。工作规则以 [AGENTS.md](../AGENTS.md) 为准，项目决策以 [decisions.md](../decisions.md) 为准；任务卡细化工作范围，不得与二者冲突。遇到冲突、规格缺口或无法执行的验收时，停手并报告 Orchestrator，不猜测、不伪造结果。

## 角色协议

各角色先读取任务卡和与任务相关的决策；实现角色只以已评审的行为规格、公开 API 文档、测试与 parity 期望值为工作依据。Spec 可阅读私有行为资料，但向其他角色只交付行为规格，不传递私有资料位置、源码片段或实现细节。素材角色按素材登记与授权范围处理资源。

| 角色 | 输入 | 输出 | 禁止事项 |
|---|---|---|---|
| Orchestrator | 工作规则、决策、任务依赖与负责人意见 | 拆卡、排依赖、协调门禁；维护决策和章程、法律、平台文档 | 未经负责人批准修改决策或合并 PR；向实现角色传递私有资料位置或源码 |
| Spec Lead / Spec Agent | 任务卡、决策、获准的私有行为资料 | `spec/` 中的行为、数值、公式、边界和失败模式；报告规格缺口 | 在规格中写入超过 5 行的源码片段、混淆名或行号；把私有资料位置或实现细节交给实现角色 |
| Core | 已评审规格、公开 API、测试与期望值 | `src/common/` 下 energy、machine、recipe、fluid、crop、reactor 核心逻辑 | 读取私有行为资料；在核心层引入平台或版本专属 API；猜测缺失规格 |
| Platform | 已评审规格、公开 API、平台任务卡 | `src/neoforge/` 平台适配；与 Content 共同负责 `src/datagen/` | 读取私有行为资料；把版本差异放入 common；未经批准越权改动 |
| Content | 已评审规格、参数与内容任务卡 | `src/common/content/` 内容定义；与 Platform 共同负责 `src/datagen/` | 读取私有行为资料；猜测数值或遗漏该任务关联修复项的回归测试 |
| Client | 已评审规格、公开 API、界面与兼容性任务卡 | `src/client/`、`src/compat/` 的界面与兼容层 | 读取私有行为资料；猜测未规定的行为；未经批准越权改动 |
| QA | 已评审规格、测试任务卡与期望值 | `src/test/` 测试、`parity/` 场景与对比报告 | 读取私有源码；将环境准备或脚本存在当作实际运行通过；伪造验收输出 |
| Artist / Audio | 素材任务卡、素材登记表及获准使用的素材 | `assets/` 中的新作与来源登记；私有阶段素材按决策隔离 | 将原版素材提交到主仓库历史；转公开前保留未替换的原版素材 |
| Docs | 已评审规格、决策、任务卡及已验证的项目事实 | `docs/` 文档和交接材料，例外文件由 Orchestrator 负责 | 改写决策或规则；将文档完成写成阶段门禁通过；未经批准越权改动 |

P1-T8 的验收责任人为 Spec Lead；parity 的实际写入由 QA 的独立净室任务承接。P0-T8 的评审文档由 Docs 编写，负责人签认。

## 任务与交接流程

1. **任务卡**：Orchestrator 拆分并确认依赖。卡片写明 ID、阶段、负责人、依赖、输入、输出、验收与约束；规格只引用 `spec/` 章节，每卡最多引用两份规格文件、涉及最多五个源码文件。验收应为可执行命令或可量化指标。无适用规格时如实注明；规格不足时向 Spec Lead 报缺口。
2. **所有权检查**：领卡前确认角色、可写范围和验收。跨目录修改须先获 Orchestrator 批准；任务与决策冲突时停手上报。任务卡中的 `common/`、`neoforge/`、`datagen/` 和 `test/` 路径相对 `rebuild/src/`。
3. **PR 与交接三件套**：提交 PR，包含适用的代码、测试和依据的 spec 章节；文档任务如无适用章节须明确写明。留下交接说明，列出做了什么、没做什么、验证命令及结果、下游依赖和相关修复项回归情况；更新[任务板](https://github.com/spectrum527/ic2-plan/issues/1)状态。三件缺一不合并。
4. **评审与合并**：按所有权安排评审，按 A16 执行代码评审和质量门禁。`decisions.md` 仅由 Orchestrator 提出和写入，本轮已定事项可按 A16 直接更新。

## 写入所有权

| 范围 | 所有者 |
|---|---|
| `spec/` | Spec |
| `src/common/energy/`、`machine/`、`recipe/`、`fluid/`、`crop/`、`reactor/` | Core |
| `src/neoforge/`，包括版本子模块 | Platform |
| `src/common/content/` | Content |
| `src/client/`、`src/compat/` | Client |
| `src/datagen/` | Content 与 Platform |
| `src/test/`、`parity/` | QA |
| `assets/` | Artist / Audio |
| `docs/` | Docs；`00-charter.md`、`01-legal.md`、`02-platform.md` 由 Orchestrator 负责 |
| `decisions.md` | Orchestrator，按 A16 维护 |

## 阶段状态

文档写完、PR 提交或某项准备工作完成，都只能证明对应产物或动作已完成。阶段门禁须按任务卡和决策逐项验收，并由规定的负责人确认；未执行的实机运行、尚缺的授权材料和待签认事项应明确列为缺口，不得推断为已完成。阶段进度以[任务板](https://github.com/spectrum527/ic2-plan/issues/1)及其验收记录为准。
