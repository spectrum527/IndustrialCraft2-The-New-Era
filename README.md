# IndustrialCraft 2（IC2）

这个工作区按 IC2 Experimental 的行为规格重建工业模组。游戏使用 `ic2` 命名空间，已有内容沿用 2.9.40 的注册与资源路径。主平台是 NeoForge 1.21.1（Java 21），同步构建 NeoForge 26.1（Java 25）。原有 Java 包和 Maven group 仍为 `com.industrialage.ia2`。

## 当前进度

G0 已通过。主线已交付注册、事件、网络、能力四个中立接口、JUnit 和 1.21.1 专服 GameTest。接下来补客户端与配置接口、平台组装和实际适配，再接入发电、线缆、储电和加工。G1、G2 和可玩闭环 G5 待验收。

机器、物品和线缆的纹理全部重绘。首批围绕发电机、BatBox、打粉机、铜缆与电池制作。新图放在 `assets/`，逐项登记作者、来源与许可。制作规则和当前任务见[纹理规格](spec/assets/textures.md)与[接续队列](AGENT-GUIDE.md#6-当前工作2026-11-19)。

## 工作入口

| 目录或文件 | 用途 |
|---|---|
| [决策表](decisions.md)与[章程](docs/00-charter.md) | 当前名称、范围、版本与 39 项 MVP |
| [协作规则](AGENTS.md)与[工作指南](AGENT-GUIDE.md) | 角色、交接与任务入口 |
| [行为规格](spec/README.md) | 实现与测试使用的规格 |
| [配对验证](parity/README.md) | 参考观测、候选场景和预期值 |
| [美术工作区](assets/README.md) | 新作目录及生产素材登记 |

在本目录运行：

```sh
./gradlew build
./gradlew :test:runGameTest
python3 src/test/resource_overlay.py
```

工程基线与阶段记录见 [G0](docs/gates/G0.md)。完整工作区的 `../Progress.md` 和私有计划仓库的 `task_plan.md` 记录本轮交接。代码使用 LGPL-3.0，新作素材使用 CC BY-SA 4.0。
