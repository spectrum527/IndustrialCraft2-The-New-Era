# IndustrialCraft 2（IC2）

按照 IC2 Experimental 的游戏表现独立重建的工业模组。0.1.0 包含发电、储能与变压、矿物加工、热能与动能、两类管道、作物与采矿、便携装备、UU 复制和核反应堆。机器与物品使用项目作者提供的重绘纹理。 [0.1.0 四版下载](https://github.com/spectrum527/ic2/releases/tag/v0.1.0) · [实测记录](parity/evidence/first-playable-0.1.0/README.md)

## 安装与游玩

在下表选择游戏与加载器，将对应 JAR 放入独立实例的 `mods/`，多人游戏的两端使用同一版本。首次开荒可从橡胶树、火力发电机和打粉机开始。[安装与第一条生产线](docs/first-playable.md)

| 游戏 | 加载器 | Java |
|---|---|---|
| 1.19.2 | Forge 43.5.0 | 17 |
| 1.21.1 | NeoForge 21.1.265 | 21 |
| 26.1.2 | NeoForge 26.1.2.128 | 25 |
| 26.2 | NeoForge 26.2.0.95 | 25 |

## 构建

三个 NeoForge 分支共享玩法层，本工程可以分别构建三个目标。Forge 1.19.2 分支使用 ForgeGradle 的重混淆发行任务。

```sh
./gradlew :neoforge:1.21.1:jar :neoforge:26.1.2:jar :neoforge:26.2:jar
./gradlew :neoforge:1.21.1:nativeGameplayTest
```

各 JAR 位于相应模块的 `build/libs/`。验证任务将生产 JAR 装进独立服务端，包含从保存的世界冷启动复测。行为资料与测试记录见 [对照目录](parity/README.md)。代码和新作素材的许可分别见 [LICENSE](LICENSE) 与 [LICENSE-ASSETS](LICENSE-ASSETS)。
