# IndustrialCraft²: The New Era 1.0.0 自审查报告

本报告由 **ChatGPT（本次 Codex 会话）** 于 **2026-11-21** 审查并署名，涵盖 Forge 1.19.2 和 NeoForge 1.21.1、26.1.2、26.2 的正式发布源码与 JAR。

我核对了洁净室规格、隔离角色交付的源码比较结果、美术来源登记，以及四个可安装包中的实际文件。在本次覆盖的发布实现中，未发现抄录 IndustrialCraft² 原作或修复版源代码的证据。机器公式与配方的公开资料来源为 **MC 百科（mcmod.cn）**。感谢条目编辑者记录机器参数、加工条件、原料和产物。这些资料用于编写行为规格，版本差异通过运行游戏复核，目标平台的实现和测试由项目独立编写。 [MC 百科 · IC2](https://www.mcmod.cn/class/2.html) · [打粉机配方示例](https://www.mcmod.cn/item/241.html) · [电解与装罐的行为规格](../spec/machines/electrolyzer-and-canners.md)

## 代码如何核对

规格人员负责观察行为并整理输入、输出与边界。实现和测试人员依行为规格及公开平台 API 工作。隔离审查角色将实际 Gradle 编译输入逐一绑定到 Git 源文件和已审记录，参照为 Experimental 2.9.40、2.8.222 与修复版 1.2.4。 [项目洁净室规则](../AGENTS.md) · [参照版本](../reference/INDEX.md)

| 发行目标 | 生产 Java 编译输入 | 复用全量审查 | 复用近期增量审查 |
| --- | ---: | ---: | ---: |
| Forge 1.19.2 | 211 | 177 | 34 |
| NeoForge 1.21.1 | 216 | 184 | 32 |
| NeoForge 26.1.2 | 216 | 184 | 32 |
| NeoForge 26.2 | 216 | 184 | 32 |

四个平台合计 859 次源码输入。按路径和文件内容去重，当前为 353 组。其中 300 组沿用历史全量审查中逐字节相同的记录，53 组来自后续变更审查。全量审查曾对 338 组做 1,019,408 对比较，增量审查对 53 组做 159,848 对比较。增量的原始结果有 6 组超过 25% 阈值，各组均复核到实际执行体。历史 `CellItem.java` 的连续八行匹配出现在 import 区，原始失败记录保留。近期变动源码的连续八行匹配为零。 [全量审查](../parity/evidence/originality/README.md) · [增量审查及各文件原分数](../parity/evidence/gameplay-compat-originality/README.md)

## 游戏素材与品牌

作者提供了游戏美术重绘包和本项目的图标、封面。四份成品中的 77 张 GUI 均与回交原稿逐字节一致。Forge 的 1,632 张游戏 PNG 中有 1,612 张直接来自回交包，NeoForge 每版 1,644 张中有 1,624 张直接来自回交包。各版其余 20 张包括 19 张由重绘原稿染色或裁切的变体，以及一张项目补绘的橡胶树叶。与三份原始版本的媒体比对，未发现生产纹理复用其 PNG 原文件或相同可见像素。 [素材来源](../assets/SOURCES.md) · [纹理文件指纹](../assets/PRODUCTION-TEXTURES.csv)

游戏的 15 份动画元数据沿用帧序、帧时等播放参数。模型中有 32 项简单的父模型与纹理绑定在解析后相同。随包的 59 个 OGG 音效与项目合成公式相符。验收截图含运行中的游戏或模组界面，工作台对照图还含修复版实机画面，均按文档用途登记。README 的两张 JPEG 直接取自作者附件，限用于标识本项目。 [美术许可](../LICENSE-ASSETS) · [品牌条款](branding.md)

## 成品与授权边界

我核对了四个 1.0.0 发行 JAR 与上一轮已完成实机回归的候选包。所有已编译游戏类均逐字节一致。可解释的变化为发布元数据、许可及随包声明，以及中英文创造模式物品栏的正式名称。四份正式包随后分别装进真实客户端和独立专服，均读到正式名称与 1.0.0。四版都成功打开打粉机和 UU 扫描机并正常退出。此前候选的真实客户端验收覆盖 471 个中文界面场景，另验 96 组储物箱拆装和 16 组可选模组安装。首发 JAR 的指纹和源码束摘要见机器可读记录。 [首发核验记录](../parity/evidence/release-1.0.0/summary.json) · [玩法实测](../parity/evidence/gameplay-fixes/README.md)

负责人确认，**本项目尚未获得 IndustrialCraft² 原作权利方的书面授权**。作者将游戏美术重绘按 CC BY-SA 4.0 发布，品牌图仅供项目标识使用。代码采用 LGPL-3.0-only。维护团队承诺开源、非营利运营，欢迎提交 PR 和申请共创。本署名确认的是上述内部核查过程及其结果。权利归属和原作授权状态由权利声明分别说明。 [双语权利声明](../NOTICE.md)

## English review statement

I reviewed the clean-room process, the isolated source comparisons, the artwork register and the four 1.0.0 JARs. I found no evidence of copied implementation code from IndustrialCraft² or its repair project in the reviewed release sources. MC百科 (mcmod.cn) is credited as the public source for machine formulas and recipes. The team used observable behavior to check version differences and wrote its own code and tests. [MC百科’s IC2 entry](https://www.mcmod.cn/class/2.html) · [Macerator recipes](https://www.mcmod.cn/item/241.html)

The four Gradle production targets account for 859 source inputs, or 353 distinct path-and-content pairs. Each maps to an existing full or incremental review. Raw similarity flags, including a historical import match, remain available. Every release has 77 GUI textures that match the author's supplied artwork byte for byte. The game textures include credited redraws and project-created variants. Animation timing and simple model bindings reflect game behavior. Game screenshots and the identification-only icon and cover have separate rights. All compiled game classes match the previously tested candidates. Each 1.0.0 JAR also ran on a matching client and dedicated server, where the macerator and UU scanner screens opened successfully. The project's own licenses cover its contributions. It has received no written authorization from the original IndustrialCraft² rights holders.

**Signed by ChatGPT**, acting as the AI reviewer in this Codex session, November 21, 2026.
