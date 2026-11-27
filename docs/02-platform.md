# 平台与构建记录（P0-T3）

本记录落实 `decisions.md` A5、A6、A17、A19、A20、A22。2026-11-20 起同步目标更新为 26.1.2 并新增 26.2，同时将 Forge 1.19.2 纳入首发交付；2026-11-27 按 Option A 方案将 **Forge 1.19.2 正式并入 `main` 单分支与单一 Gradle 9.2.1 工程**，使用 ModDevGradle `2.0.148` 同时驱动 NeoForge（`net.neoforged.moddev`）与 Forge 1.19.2（`net.neoforged.moddev.legacyforge`）。原 P0-T4 本地与 Linux CI 双版本证据见 [P0-T4 PR](https://github.com/spectrum527/ic2/pull/10)，2026-11-12 的 G0 结论见 [G0 记录](gates/G0.md)，Forge 1.19.2 的 P2 与首台机器历史验收分别见 [1.19.2 门禁记录](gates/1.19.2-P2.md) 与 [Forge 1.19.2 首台机器记录](forge-1.19.2-machine.md)。

## 平台与版本

| 用途 | 目标 | 工具链与验收 |
|---|---|---|
| 首发目标 | Minecraft 1.19.2 / Forge | JDK 17（`net.neoforged.moddev.legacyforge`，`disableRecompilation = false`）；同仓编译、SRG 重混淆打包并独立运行验收 |
| 主基线 | Minecraft 1.21.1 / NeoForge | JDK 21（`net.neoforged.moddev`）；MVP 与 G5 在此验收 |
| 同步移植 | Minecraft 26.1.2 / NeoForge | JDK 25（`net.neoforged.moddev`）；同步编译，运行验收从 P6 纳入 |
| 同步移植 | Minecraft 26.2 / NeoForge | JDK 25（`net.neoforged.moddev`）；同步编译，运行验收从 P6 纳入 |
| 行为参考 | Minecraft 1.19.2 / Forge（独立实例） | JDK 17；修复发布版 `1.2.0-port.20261119` / `1.2.4-port.20261120` 提供首发玩法基准，2.9.40 提供未覆盖行为的原始对照 |
| 补充参考 | Minecraft 1.12.2 / Forge | A25 要求 JDK 8；用于已批准缺失功能与交叉核对 |

- 构建插件：**ModDevGradle `2.0.148`**（A19），在单一 **Gradle Wrapper `9.2.1`** 工程内分别启用 `net.neoforged.moddev`（NeoForge 1.21.1 / 26.1.2 / 26.2）与 `net.neoforged.moddev.legacyforge`（Forge 1.19.2）。
- 当前固定组合：1.19.2 使用 Forge **43.5.0**；1.21.1 使用 NeoForge **21.1.265**；26.1.2 使用 **26.1.2.128**；26.2 使用 **26.2.0.95**；ModDevGradle **2.0.148**，Gradle Wrapper **9.2.1**。四个目标均在仓库统一的严格依赖校验（`gradle/verification-metadata.xml`）下构建。
- 官方固定版本工件：[NeoForge 26.1.2.128 POM](https://maven.neoforged.net/releases/net/neoforged/neoforge/26.1.2.128/neoforge-26.1.2.128.pom)、[NeoForge 26.2.0.95 POM](https://maven.neoforged.net/releases/net/neoforged/neoforge/26.2.0.95/neoforge-26.2.0.95.pom)。这些是选定值，不宣称始终为最新版。
- 工具链分工：`:common` 与 `:forge:1.19.2` 使用 **Java 17**（`src/minecraft/shared/` 同步保持 Java 17 兼容），`:neoforge:1.21.1` 使用 **Java 21**，`:neoforge:26.1.2` 与 `:neoforge:26.2` 使用 **Java 25**。各目标的构建和运行证据须分别保存，后续升级须重新验证。
- `:forge:1.19.2` 显式配置 **`disableRecompilation = false`**（覆盖 ModDevGradle 在 `CI=true` 时的默认 `true`），避免 NFRT 在重映射后的 `forge-1.19.2-43.5.0.jar` 中保留原始 `META-INF/FORGE.{SF,RSA}` 签名导致 JUnit 抛出 `SecurityException`。生产包由 `reobfJar` 输出至 `src/forge/1.19.2/build/libs/`，独立 QA 夹具由 `reobfQaFixtureJar` 输出至 `src/forge/1.19.2/build/qa-machine/fixtures/`，并直接由 ModDevGradle `RunGameTask` 导出 `prepareQaWorld` / `prepareQaPlay` 启动清单（无需 ForgeGradle 反射 hack 或自定义 canonical JAR 注入）。
- Fabric 在 v1.0 后按需求评估；当前四个首发目标为 Forge 1.19.2 与 NeoForge 1.21.1 / 26.1.2 / 26.2（A6）。

`main` 为四目标统一主线分支，所有改动在同一提交下同时面向 Forge 1.19.2 与三个 NeoForge 目标取证，不再维护分叉的 1.19.2 开发分支。

## 选择理由

主基线固定在 1.21.1，使 MVP 的构建、客户端、专用服务器和 G5 验收有统一目标；26.1.2 与 26.2 同步编译并运行验收；Forge 1.19.2 通过 `net.neoforged.moddev.legacyforge` 并入同一 Gradle 9.2.1 工程，消除了双分支回移漂移、双 Wrapper、双 `verification-metadata.xml` 与 ForgeGradle 反射脚本维护成本。修复版 1.19.2 仍独立作为行为参照，避免把旧环境的内部实现带入净室架构。

ModDevGradle 是已批准的构建方案；P0-T4 当时对 1.21.1 / 26.1 组合执行本地双版本构建，并记录 Wrapper 与依赖校验和；2026-11-27 的 Option A PoC 进一步在单一 Gradle 9.2.1 工程中完成了四目标共存构建、严格依赖校验、SRG `reobfJar` / `reobfQaFixtureJar` 校验、真实 Forge 43.5.0 专服 `runOptionalCompat` 以及 `verify.py` `smoke` / `world` / `play` / `mismatch` 全量实测。选型记录、编译证据、运行证据和 CI 结果分别保留，不能把历史结果算作新组合的验证。

## 四目标子模块结构

以下为统一 `main` 分支的模块职责边界：

```text
src/
  common/             Java 17 核心行为、内容定义与平台能力接口（严禁导入 Minecraft/加载器类）
  minecraft/
    shared/           四目标共用且保持 Java 17 兼容的 Minecraft 玩法层
  forge/
    1.19.2/           Forge 43.5.0 / JDK 17 适配（legacyforge）、SRG 重混淆与独立 QA 夹具/脚本
  neoforge/
    shared/           三个 NeoForge 目标共用的平台 Java
    1.21.1/           主基线注册、事件、网络、能力与客户端适配（JDK 21）
    26.1.2/           同步目标的构建与专属适配（JDK 25）
    26.2/             同步目标的构建与专属适配（JDK 25，含独立屏幕访问）
    26.x/src/main/java/ 两个 26.x 目标共用的 Java 适配
    pre-26.2/src/main/java/ 1.21.1 与 26.1.2 共用的屏幕访问
  datagen/            数据生成入口，按目标版本连接适配层（forge-1.19.2 / neoforge-*）
  test/               面向全部四目标的 JUnit 契约测试及 1.21.1 运行测试入口
```

依赖由版本适配层指向 `:common`，`:common` 不依赖任一适配模块，也不得引用 `net.minecraft.*`、`net.neoforged.*`、`net.minecraftforge.*` 或 `net.fabricmc.*`（由 `checkCommonImports` 强制检查）。平台能力通过小接口注入；版本差异收敛在对应适配层（例如 1.19.2 与 1.21+ 在注册表/NBT/附魔/装备资产路径上的差异由各自平台适配与 `processResources` 处理，26.2 的屏幕操作移入 `Gui` 而使用独立屏幕访问实现）。内容 PR 须通过全部四个目标的编译、契约与 CI 门禁，单个目标的运行结果不能替代其余目标的编译或运行结果。

原版素材仍在独立私有素材仓库，通过可选属性 `ic2.legacyAssetsDir`（保留旧属性别名） 引入；属性缺省时跳过该资源目录。新作资源放在 `assets/` 并在同路径时优先。外部 PNG 及其动画元数据只供参考；构建过滤这些图像，包括机器 GUI，详情见 A22。素材文件不得进入主仓库历史；P0-T4 必须验证有、无外部素材目录两种构建情形（A22）。

## 风险与验证责任

| 风险 | 处理及验收责任 |
|---|---|
| 平台组合与升级兼容性 | 保持固定版本和跨平台（macOS/Linux/Windows）`gradle/verification-metadata.xml` 校验值；升级后重跑全部四个目标的编译及对应运行门禁 |
| 版本专属类型渗入核心层 | 编译四个目标并执行 `checkCommonImports`；发现耦合时收敛到能力接口或 `src/minecraft/shared/` 适配器 |
| Java 21 API 渗入共享层 | `:common` 锁定 JDK 17 工具链，`:forge:1.19.2` 以 JDK 17 编译 `src/minecraft/shared/`，CI 同时覆盖 JDK 17/21/25 |
| 各目标的客户端、网络或存档行为分歧 | 分开记录四个目标的运行证据；NeoForge 26.x 与 Forge 1.19.2 均在 CI 执行独立实机/专服回归 |
| 仅编译通过，实际无法加载 | 保留生产 JAR 契约、客户端与专用服务器（含安装态 Forge 43.5.0）加载证据 |
| 外部素材目录缺失或资源覆盖顺序错误 | 分别构建并检查生成包；确认新作同路径优先、1.19.2 排除 1.21+ `equipment/` 资产且历史中无原版素材 |

## 回退方案

单次构建或适配回归可回到最后通过验证的依赖组合或实现提交；保留失败日志，再由对应角色修复。不能通过静默删除任一目标（含 Forge 1.19.2）的编译或运行检查来恢复绿色状态。

若未来 `net.neoforged.moddev.legacyforge` 在极少数上游变更下出现阻塞，可优先锁定已知验证版本（`2.0.148`）或按历史验证的双 Wrapper 方案临时隔离，但必须经决策 PR 修订 A19，不得破坏四目标共用的 `src/common/` 与 `src/minecraft/shared/` 边界。

## 统一四目标常用命令

在仓库根执行（Gradle 9.2.1 自动调度 JDK 17 / 21 / 25 工具链）：

```bash
# 全量四目标构建 + 单元测试 + 治理门禁（导入隔离、资产隔离、Spotless）
./gradlew clean build check

# 仅跑通用治理门禁与 :common
./gradlew checkCommonImports checkAssetIsolation spotlessCheck :common:build --console=plain

# 按目标构建并运行对应 JAR 契约测试（target 可选 1.19.2 / 1.21.1 / 26.1.2 / 26.2）
./gradlew :forge:1.19.2:build :forge:1.19.2:test :test:test -Pic2.testTarget=1.19.2 --console=plain
./gradlew :neoforge:1.21.1:build :test:test -Pic2.testTarget=1.21.1 --console=plain
./gradlew :neoforge:26.1.2:build :test:test -Pic2.testTarget=26.1.2 --console=plain
./gradlew :neoforge:26.2:build :test:test -Pic2.testTarget=26.2 --console=plain

# Forge 1.19.2 专服 GameTest、跨进程世界重载、客户端与安装态可选模组验证
./gradlew :forge:1.19.2:prepareQaWorld :forge:1.19.2:reobfQaFixtureJar --console=plain
python3 src/forge/1.19.2/qa/verify.py smoke --prepared --server-launch src/forge/1.19.2/build/qa-machine/server-launch.json --output build/evidence-smoke
python3 src/forge/1.19.2/qa/verify.py world --prepared --server-launch src/forge/1.19.2/build/qa-machine/server-launch.json --output build/evidence-world
python3 src/forge/1.19.2/qa/verify.py play --output build/evidence-play
python3 src/forge/1.19.2/qa/verify.py mismatch --output build/evidence-mismatch --prepared
./gradlew :forge:1.19.2:runPackagedWater -Pic2.compat.forgeServer=/path/to/forge-43.5.0 --console=plain
./gradlew :forge:1.19.2:runOptionalCompat -Pic2.compat.mode=both -Pic2.compat.forgeServer=/path/to/forge-43.5.0 --console=plain

# NeoForge 1.21.1 / 26.x 专服回归
./gradlew :test:runGameTest :test:runPlatformHostTest :test:runPlatformWorldTest :neoforge:26.1.2:runModernServerSmoke :neoforge:26.2:runModernServerSmoke --console=plain
```

生产包分别位于 `src/forge/1.19.2/build/libs/`（SRG 重混淆 `reobfJar`）与 `src/neoforge/<target>/build/libs/`。

## 迁移验证（2026-11-20 三 NeoForge 目标）

2026-11-20 在集成分支从干净目录执行了 73 个 Gradle 任务。共同及构建 JUnit 40/40，主基线宿主契约 5/5。每个 JAR 的 83 个生产条目均与实测的开发类和资源逐字节一致。

| Minecraft | JAR SHA-256 前 12 位 | 专服与存档 | 图形客户端 |
| --- | --- | --- | --- |
| 1.21.1 | `586863b25825` | GameTest 1/1，两个独立进程同档写入和读回通过 37、16 项检查 | 34 项通过。协议不匹配的客户端被拒绝，登录和回显均为 0 |
| 26.1.2 | `9598e02125fb` | 燃烧、原生存档往返、32 EU 出包和保存关服共 6 项通过 | 状态页、输入焦点和专服回显 15 项通过 |
| 26.2 | `023277800f72` | 同一组 6 项通过 | 同一组 15 项通过，覆盖新 `Gui` 屏幕接口 |

两个 26.x 专服分别将一块煤转换成 4,000 EU。工作中通过本版原生方块实体格式保存并读回 363 个余燃料刻和 370 EU，单进程完成往返。两版客户端均连接本机独立专服，通过合成键盘输入打开状态页，并通过按钮回调触发回显。P6 继续按版本验收完整玩家路线。

在仓库根运行 `./gradlew clean build :test:runGameTest :test:runPlatformHostTest :test:runPlatformWorldTest :neoforge:26.1.2:runModernServerSmoke :neoforge:26.2:runModernServerSmoke` 可复跑集成分支的构建和专服回归。生产包位于各目标模块的 `build/libs/`。

## 历史验证与交接（P0-T4）

以下为当时使用 1.21.1 / 26.1（NeoForge 21.1.265 / 26.1.0.19-beta）的记录，不是 26.1.2 / 26.2 的验证结果。现行三目标的构建、CI 与运行须分别另行取证。

`/usr/libexec/java_home -V` 已列出 JDK 17、21、25。本轮 P0-T4 在 JDK 21 / 25 下分别完成主基线与同步目标的真实 `clean build`，JAR 契约测试核对入口、元数据、字节码版本及完整许可文件；有/无外部素材及新作覆盖的自造资源回归也已执行。

主线专用服务器曾实际记录入口加载及 `Done`，随后人工停止并保存世界；该停止命令的非零退出码不冒充成功构建。未执行 26.1 运行验收、客户端功能或 GameTest；相似度缺私有输入时仍明确跳过。

负责人已批准 P0-T4 / P0-T5 首次依赖下载。P0-T5 已完成进入世界、放置机器、打开 GUI、退出重启后重入，以及 M01 / M08 的单边参考采样，见 [参考实例 PR](https://github.com/spectrum527/ic2/pull/13)；额外扣电的修复目标已由负责人批准，见 [B#41 决策与规格 PR](https://github.com/spectrum527/ic2/pull/14)；候选实现、回归与双边比较仍未完成。负责人现已确认 G0 通过，P2 可按任务依赖开工；原作书面授权状态以[当前说明](01-legal.md)为准，P2 的注册、生命周期、网络、配置与客户端功能仍须分别验收。
