# IndustrialCraft 2 · Forge 1.19.2

这个分支为 Minecraft 1.19.2 独立构建 IC2。它从重建仓库的 P2 合并点 `10a57bac` 分出，复用本项目按行为规格开发的通用层，在 Forge 43.5.0 上接入注册、事件、方位能力、每档配置和网络。进入游戏后，可在控制设置里为“打开 IC2 状态”绑定按键。服务端管理员可用 `/ic2 config reload` 重载当前存档的配置。

P2 是平台开发基座。发电、输电、机器加工和生存配方由后续阶段接入。G0、G1、G2 的分支准入决定及每项技术验收的范围见[分支进度与门禁](docs/gates/1.19.2-P2.md)。

## 构建与验证

使用 64 位 JDK 17。在仓库根目录运行以下命令，Windows 使用 `gradlew.bat`。首次构建会下载 Forge 和 Minecraft 开发依赖。

```sh
./gradlew build
./gradlew :forge:1.19.2:runClient
./gradlew :forge:1.19.2:runServer
```

生产 JAR 位于 `src/forge/1.19.2/build/libs/`。将它放进 Minecraft 1.19.2 / Forge 43.5.0 实例的 `mods/` 目录，两端使用同一构建。专服首次启动遵循 Minecraft 的 EULA 流程。修复版和本分支都使用 `ic2`，请用新实例和新存档测试。

```sh
python3 src/forge/1.19.2/qa/verify.py smoke
python3 src/forge/1.19.2/qa/verify.py world
python3 src/forge/1.19.2/qa/verify.py play
python3 src/forge/1.19.2/qa/verify.py mismatch
```

`world` 在独立专服进程中保存并重开同一存档。`play` 与 `mismatch` 启动实际游戏客户端，需要图形会话。开发夹具由单独的 `ic2_qa` 模组加载。

## 工程依据

- 目标版本、工具校验值与构建输入见[平台记录](docs/02-platform.md)。
- 宿主行为、网络帧、配置边界与验收入口见[Forge 适配契约](spec/platform/forge-1.19.2.md)。
- 修复版的发行包提供黑盒行为参照。实现与 QA 分别从行为规格编写代码和断言，原版及修复版材料在独立环境保管。[洁净室决策](decisions.md) · [素材登记](assets/SOURCES.md)

代码遵循 LGPL-3.0，新作素材遵循 CC BY-SA 4.0。
