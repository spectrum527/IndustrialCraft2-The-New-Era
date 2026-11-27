# 四目标统一构建与测试框架（P2-T1 / P2-T8）

## §1 构建范围与产物（P2-T1）

**输入**为同一提交、仓库锁定的 Gradle Wrapper（`9.2.1`）、ModDevGradle（`2.0.148`，含 `net.neoforged.moddev` 与 `net.neoforged.moddev.legacyforge`）、Forge / NeoForge 版本，以及对应 JDK（A5、A19）。构建不要求外部素材目录（A22）。Platform 负责模块及版本构建，QA 负责测试用例、运行证据和 CI 测试门禁。

| Gradle 项目 | 职责及输出 |
| --- | --- |
| `:common` | Java 17 的核心、内容定义及中立接口；不依赖任一版本适配模块，也不得导入 Minecraft 或加载器类。 |
| `:forge:1.19.2` | JDK 17（`net.neoforged.moddev.legacyforge`，`disableRecompilation = false`）；编译并重混淆生成 1.19.2 生产 JAR（`reobfJar`）与独立 QA 夹具 JAR（`reobfQaFixtureJar`），承载 Forge 1.19.2 单元测试与专服/客户端运行验收。 |
| `:neoforge:1.21.1` | JDK 21（`net.neoforged.moddev`）；编译并生成 1.21.1 生产 JAR，承载 MVP 与主基线运行验收。 |
| `:neoforge:26.1.2` | JDK 25（`net.neoforged.moddev`）；编译并生成 26.1.2 生产 JAR；运行验收自 P6 起。 |
| `:neoforge:26.2` | JDK 25（`net.neoforged.moddev`）；编译并生成 26.2 生产 JAR；运行验收自 P6 起。 |
| `:datagen` | 数据生成入口，按目标连接适配层。 |
| `:test` | 面向全部四个目标（支持 `-Pic2.testTarget=<1.19.2|1.21.1|26.1.2|26.2>`）的 JUnit 契约测试，以及 1.21.1 开发专服 GameTest / 宿主测试入口。 |

**验收**：在统一 `main` 分支，`./gradlew build` 须构建全部四个目标（Forge 1.19.2、NeoForge 1.21.1 / 26.1.2 / 26.2）。CI 通过并行的治理作业、NeoForge 三目标矩阵作业与 Forge 1.19.2 作业分别取证。任一目标编译、打包失败或未执行均不能通过。各目标生产 JAR 均不得包含仅测试使用的依赖、JUnit 或 GameTest 用例、结构模板、其他夹具或测试 mod 元数据（1.19.2 亦不得打包 `ic2_qa` 或 1.21+ 专属 `equipment/` 资源）。

## §2 JUnit（P2-T8）

`./gradlew :test:test` 通过 JUnit Platform 运行核心与构建契约测试，无须启动游戏。输入为当前提交的测试类及被测产物；输出保留测试计数与失败详情。至少执行一个用例；断言失败、无法执行、零用例均不能通过。共同逻辑用 JUnit 验收，不用服务端启动代替断言。

## §3 1.21.1 开发专服 GameTest（P2-T8）

- **输入**：JDK 21、锁定的 1.21.1 运行环境、当前提交的开发 mod 和独立测试类及资源。运行在隔离目录，仅在本次测试运行中载入测试内容；测试资源含可定位的结构模板，启动不依赖客户端、登录会话、私有素材或已有世界。专服启动前置条件须在该目录满足。
- **运行入口**：ModDevGradle 提供 `gameTestServer` 运行类型，且锁定的 1.21.1 NeoForge 依赖声明了对应开发专服运行。为本次运行选定测试命名空间（`neoforge.enabledGameTestNamespaces`）。`./gradlew :test:runGameTest` 已在 P2-T8 交付，直接启动独立的 1.21.1 GameTest 专服。
- **最小场景**：注册一个必跑的 smoke。在测试结构中放置并读回一个方块，断言状态后结束用例。场景不依赖后续内容任务；保留能定位该 smoke 标识与结果的本次运行证据。
- **输出**：成功须同时满足专服正常退出、至少执行一个用例、指定 smoke 通过且必跑用例无失败。有限超时后结束进程并保存日志；非零退出码阻断验收。零退出、启动日志或发现测试均不能单独证明执行。

运行类型及源集配置见 [ModDevGradle 官方文档](https://github.com/neoforged/ModDevGradle#runs)；命名空间筛选和专服退出语义见 [NeoForge 1.21.1 Game Tests](https://docs.neoforged.net/docs/1.21.1/misc/gametest/#buildscript-configurations)。任务名由本项目提供。

## §4 CI 与证据（P2-T8）

统一 `main` 分支的每个 PR 与推送以当前提交并行执行三类作业：治理与通用检查（`governance`：`checkCommonImports`、`checkAssetIsolation`、`spotlessCheck`、`:common:build`、Python 套件及相似度门禁）、NeoForge 三目标矩阵（`1.21.1`、`26.1.2`、`26.2` 构建、`-Pic2.testTarget` 契约与对应运行检查）以及 Forge `1.19.2`（`:forge:1.19.2:build`、`:forge:1.19.2:test`、`:test:test -Pic2.testTarget=1.19.2`、`verify.py` `smoke`/`world`/`play`/`mismatch`、`runPackagedWater` 与四组合 `runOptionalCompat`）。测试输出须来自本次运行；缓存、空任务或上次日志不能充当执行证据。
通过和失败都保存全部四个目标的构建结果、JUnit 报告、专服/客户端日志及 GameTest 结果（smoke 标识、实际执行数、通过数、失败数和进程退出状态）。结果可由测试框架或运行入口生成。

| 用例状态 | 判据 |
| --- | --- |
| 通过 | 当次测试确实执行并通过断言。整项验收还须满足相应运行及构建条件。 |
| 失败 | 已执行的用例断言失败或用例超时，保存失败详情。 |
| 未执行 | 启动失败、未发现、跳过或无法取得本次用例执行证据，保存已有日志及原因。 |

失败和未执行都阻断相应门禁；基础设施失败另记，不冒充用例断言失败。QA 交接须覆盖全部四个目标的编译结果、JUnit 计数、指定 smoke / 运行验收实测结果及生产 JAR 隔离检查。
