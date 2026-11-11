# 重建决策表（全部已拍板，2026-11-10）

本表是 `docs/IC2-2.9.40-实现拆解与续更指南.md` §11（40 项烂尾清单）与工作区根基决策的最终结论。任务与任务卡必须与本表一致；如需推翻某项，先改本表再改任务。

## A. 根基决策

| # | 决策 | 结论 |
|---|---|---|
| A1 | 授权范围 | **源码 + 素材已获授权**（可修改、发布、复用贴图/音效/模型）；**IC2 名称/商标不在授权内** |
| A2 | 重建策略 | **坚持净室三角色隔离**——作为工程纪律（架构质量、可维护性），不再是法律必需；相似度检查保留为质量门禁 |
| A3 | 项目命名 | **Industrial Age 2**，mod id `ia2`，包根与 Maven group **`com.industrialage.ia2`（2026-11-10 定稿，不再更改）**，禁止 `ic2` |
| A4 | 工作区 | `IC2/rebuild/` 子目录（本目录），与 analysis/、docs/ 并列互引用 |
| A5 | 目标平台 | **NeoForge 1.21.1 为主基线**（MVP/G5 门禁在此验收）+ **NeoForge 26.1 同步移植目标**；common 层自第一天起抽象版本差异 |
| A6 | Fabric | v1.0 后按社区需求评估；MVP～v1.0 只发 NeoForge |
| A7 | 素材 | **私有阶段直接复用原版素材**（授权 A1），`rebuild/assets/` 登记来源；**转 Public 前必须全部替换为新作**：贴图、模型、音效、GUI 一项不留（2026-11-10 修订，原"高曝光素材逐步重绘"作废） |
| A8 | reference 携带 | 索引引用（不拷贝 9 万行源码），见 `reference/INDEX.md` |
| A9 | spec 管线 | `docs/mechanisms/` 9 份 → 按 `spec/_schema/` 模板 **schema 化重构**入库（Agent 批量转），非直接转正 |
| A10 | 配置面 | **精简重建**：只保留 58 个活键（33 死键全删），`ModConfigSpec`，不做旧键兼容层；P9-T5 只产出一份"旧键→新键"迁移文档（2026-11-10 确认） |
| A11 | 代码协议 | **LGPL-3.0**（转 Public 时生效；私有阶段仓库内即放置 `LICENSE`） |
| A12 | 仓库可见性 | **GitHub 私有仓库（spectrum527 账号）**，直到 A7 素材全部替换完成后才转 Public；公开范围仅 `rebuild/`（`reference/` 内容除外）。反编译产物、拆解文档、data-workspace、原 jar、IDA 库永久私有 |
| A13 | 发布节奏 | 转 Public 前**只允许邀请制封闭测试**（内部玩家群分发 jar），**不上** Modrinth / CurseForge |
| A14 | 授权存档 | 书面授权（版权方签署）脱敏存档于 `rebuild/docs/01-legal.md`；**转 Public 前复核**该文件是否保留或进一步脱敏（与 A15 对外口径一致） |
| A15 | 对外表述 | README / 项目页统一写"**受 IndustrialCraft 2 启发的独立项目**"，**不提**授权与原开发团队；"IndustrialCraft 2"只在这句描述性语境出现，不作为名称、标识或宣传语 |
| A16 | Orchestrator | **由 Agent 担任**；每次合并 PR、每次修改本文件均需负责人审批 |
| A17 | 文档约定 | 新文档放 `rebuild/docs/`；任务卡中 `common/` `neoforge/` `datagen/` `test/` 等路径一律相对 `rebuild/src/`；文件所有权以 `AGENTS.md` 为准；计划文档里的 `ic2-reforged/` 即 `rebuild/` |
| A18 | 托管与流程 | GitHub 账号 `spectrum527`：主仓库 https://github.com/spectrum527/ia2 （对应 `rebuild/`，remote `origin`）、素材仓库 https://github.com/spectrum527/ia2-legacy-assets （本地 `../ia2-legacy-assets/`）、独立计划仓库 https://github.com/spectrum527/ia2-plan （本地 `../plan/`，负责人于 2026-11-11 选定），均为 Private。计划仓库保留 Spec 任务卡的私有资料索引，不随主仓库转 Public。GitHub Projects 看板（状态值沿用 `plan/README.md`）+ GitHub Actions CI |
| A19 | 构建插件 | **ModDevGradle**（含多版本子模块，满足 A5 双版本编译） |
| A20 | MVP 与参考实例 | MVP 清单（≤40 项）由 Orchestrator Agent 起草；1.19.2 参考实例由 QA Agent 搭建：**ForgeGradle 1.19.2 开发环境 + 原 jar 作为 mod 加载**，离线开发用户名，首次联网下载依赖需负责人批准；实例放在私有的上层目录 `../reference-env/`，`rebuild/parity/` 只放场景脚本与期望值。负责人在 **G0 评审**时一并确认 |
| A21 | 新作素材协议 | 重绘后的贴图/模型/音效/GUI 采用 **CC BY-SA 4.0**（`LICENSE-ASSETS`）；外包与社区投稿须同意以该协议授权。私有阶段复用的原版素材不适用此协议 |
| A22 | 原版素材隔离 | 原版素材**永不进入主仓库 git 历史**：存放于独立私有仓库 **`ia2-legacy-assets`**（本地检出于 `../ia2-legacy-assets/`），由 Gradle 作为额外资源目录引入（属性 `ia2.legacyAssetsDir`，缺省时构建跳过）；新作素材进 `rebuild/assets/`，同路径时新作优先。转 Public 时删除该引用即可 |

## B. 烂尾内容处置（拆解指南 §11 #21–#40）

| 项 | 处置 | 落点 |
|---|---|---|
| #21 IJetpack 死接口（背包不能飞） | **修复**：自行实现飞行 tick（燃料/推力/降落伞） | P7-T5（高级装甲） |
| #22 beam 粒子加速器（未注册死代码） | **砍掉** | — |
| #23 Covers 盖板死包 | **重新设计**：做成真功能（机器盖板系统，装饰+功能） | P7-T11（新任务） |
| #24 OreValues 空注册表（矿机退化） | **修复**：注册表填充 + 矿机目标判定回归 | P7-T4（世界机器） |
| #25 dynamite 雷管（无代码） | ~~砍掉~~ **被 C-MF8 覆盖（2026-11-11）**：dynamite + dynamite_sticky 进 v1.0 内容池（P7-T13），实现时参考 1.12.2 版 | P7-T13 |
| #26 TradingMarket（无消费方） | **砍掉**（交易机保留本地交易，不做无线市场） | P7-T9 |
| #27 `ic2:tank` 注册雷（放置即崩） | **修复**：正常注册路径，回归测试 | P7-T9 |
| #28 potato 作物卡不可达 | **修复** | P7-T2 |
| #29 耕地外土壤湿度刷新疑似崩溃 | **修复**（重建状态机时消解） | P7-T2 |
| #30 反应堆 EU 模式热缓冲永不清零 | **修复** | P7-T1 |
| #31 GuiDisplay/Effect 死子包 | **砍掉**（新网络层本就不做反射序列化） | P2-T6 |
| #33 电动力机（EU→kU 纯损耗） | **挂起**：kU 接口保留、机器不移植，v1.0 后评估新 kU 消费者 | — |
| #34 泡沫染色+强化泡沫烂尾 | **修复并实现**：painter 染色泡沫墙 + 强化泡沫分支 | P6-T12 |
| #35 UU 链 5 个加载环节缺失 | **补全按原设计**：resolver 注册、UuIndex.refresh、uu 命令、assembly bench 配方、ini 加载器 | P7-T3 |
| #36 专用服机器循环音疑似静音 | **修复**（音频系统重建时一并处理并实机验证） | P8-T4 |
| #37 磁化器下行死代码 | 修复（重写时自然消解） | P7-T10 |
| #38 换热器 ×20 凭空产热 | **修复** + parity 回归测试 | P6-T9 |
| #39 流体热机罐谓词误用 | **修复** | P6-T9 |
| #40 死配置键（并入 A10） | 精简重建 | P2-T5 |
| 蒸汽回压器依赖外部 forge:steam | **修复**：注册 `ia2` 蒸汽到 `c:steam`/`forge:steam` 标签，与外部蒸汽互通 | P6-T10 |
| #6 能量网异步空壳 | **同步为主 + 预留 ICalculator 可插拔接口**（不做真异步） | P3-T1/T3 |
| #8 两套 GUI 并存 | **统一数据驱动 + 手写例外通道**（极特殊界面如高级采矿机扫描视图） | P4-T5 |
| 原版十余个实证 bug（HeatSwitch/maxHeat/变压器 tooltip/热交换 ×20 兜底/罐谓词/服务端音效等） | **全部修复**，逐项写 parity 回归测试 | 各内容任务验收项 |

## C. 未决策（留待后续）

- 无。当前范围内所有决策已闭合；后续新增决策追加在本节并注明日期。
- （C1 新作素材协议已于 2026-11-10 闭合，见 A21。）

### 2026-11-11 新增：1.12.2 缺失功能处置（✅ 已全部拍板，同日闭合；普查结果见 `../missing-features/README.md`）

编号 C-MF*（避免与既有 C1 冲突）：

| # | 议题 | 结论 | 落点 |
|---|---|---|---|
| C-MF1 | 蒸汽焦炉子链 | ✅ **纳入 v0.5**（前电力时代开局 + 半流体发电机衔接） | P6-T13（新增） |
| C-MF2 | 流体管道 + 泵盖板 | ✅ **纳入 v0.5**（物流基建） | P6-T14（新增） |
| C-MF3 | 蒸汽动力链（燃烧箱/蒸汽机/实体动能机械） | v1.0 后设计评审（1.12.2 自身未注册，需自行补完设计） | Post-1.0 候选池 |
| C-MF4 | Profile 双内容切换 | ❌ **砍掉**（IA2 只做 Experimental 线） | — |
| C-MF5 | 泡沫玩法 | ✅ **改为喷枪喷涂玩法**（喷枪 8000mB 罐 + CF 背包 + BFS 洪泛 10/1 格 100mB/格 + 线缆绝缘/脚手架固化；直接放置不再做主玩法） | P6-T12（设计更新） |
| C-MF6 | 物品管道 | v1.0 后观察社区需求（1.12.2 死代码，需自研路由） | Post-1.0 候选池 |
| C-MF7 | 交易终端 | v1.0（GUI 重做；与个人方块同期） | v1.0 范围 |
| C-MF8 | 28 项缺失物品档位 | ✅ **按建议档位表**：背包供电 4 件 + solar_helmet + static_boots + cropnalyzer + 碘片食用修复 → MVP 后第一批（P6/P7）；酒系统、plasma_launcher、electric_hoe、weeding_trowel、remote、charging 电池×4、upgrade_kit、dynamite×2、foam_sprayer（随 C-MF5）→ v1.0 内容池 | P6-T12/P7-T2/P7-T5/P7-T12/T13 |
| C-MF9 | 音频引擎 | ✅ **新架构 + 旧规格**：代码按 1.19 SoundInstance 路线（弃 PaulsCode 反射绑定），行为规格（音源池/遮挡衰减/audio.* 4 键/设置滑条/111 触发点清单）照抄 1.12.2 | P8-T4（补充规格来源） |
| C-MF10 | 1.12.2 独有配置段 | ✅ **worldgen 走数据包**（24 键不进 ModConfigSpec，与 A10 一致）；agriculture 评估后只收活键；BC 开关不收 | P2-T5 |

## D. 变更记录

- 2026-11-10：负责人问答闭合全部争议点——确认 A2（净室重写）、A5（26.1 时机）、A10；修订 A3（包名定稿）、A7（转 Public 前全量重绘）；新增 A11–A22。
- 2026-11-10：A12/A18 由"GitHub 组织"改为 `spectrum527` 账号下的两个 Private 仓库；`rebuild/` 已接入 `origin`（跟踪 `origin/main`），`ia2-legacy-assets` 已克隆到 `../ia2-legacy-assets/`。
- 2026-11-11：负责人选择将现有 `plan/` 建为独立私有仓库 `spectrum527/ia2-plan`；A18 登记该选择，任务卡仍保留既有目录约定与逐 PR 审批流程。
- 2026-11-11：完成 1.12.2（2.8.222）对照普查——REA 归档盘点 + Vineflower 反编译（analysis-1122/）+ 类级 diff（513 缺失类）+ 注册表甄别（28 真缺失/15 改名）+ 4 份子系统报告，产出 `missing-features/`；新增待决策 C-MF1–C-MF10。
- 2026-11-11：负责人问答闭合 C-MF1–C-MF10：焦炉链与流体管道进 v0.5（新增 P6-T13/T14）、泡沫改喷枪玩法（P6-T12 更新）、背包供电等第一批物品（P7-T12 新增）、Profile 砍、蒸汽动力链/物品管道进 Post-1.0 候选池、交易终端 v1.0、音频新架构+旧规格、worldgen 走数据包。
