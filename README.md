# IndustrialCraft 2（IC2） · Forge 1.19.2

0.1.0 是 IC2 Experimental 的独立重建。此分支为 Minecraft 1.19.2、Forge 43.5.0 和 Java 17 提供重混淆 JAR。新存档中可从橡胶树和基础加工起步，发展热能、动能、作物、UU 复制与游戏内核工业。[安装与第一条生产线](docs/first-playable.md) [0.1.0 四版下载](https://github.com/spectrum527/ic2/releases/tag/v0.1.0) · [实测记录](parity/evidence/first-playable-0.1.0/README.md)

## 构建与验证

使用 64 位 JDK 17。在仓库根目录运行以下命令，Windows 使用 `gradlew.bat`。首次构建会下载 Forge 和 Minecraft 开发依赖。

```sh
./gradlew build
./gradlew :forge:1.19.2:runClient
./gradlew :forge:1.19.2:runServer
```

生产 JAR 位于 `src/forge/1.19.2/build/libs/`。将它放进 Minecraft 1.19.2 / Forge 43.5.0 实例的 `mods/` 目录，两端使用同一构建。专服首次启动遵循 Minecraft 的 EULA 流程。修复版和本分支都使用 `ic2`，请用新实例和新存档测试。

```sh
./gradlew :test:runPlatformHostTest :test:runPlatformWorldTest
python3 src/forge/1.19.2/qa/verify.py play
python3 src/forge/1.19.2/qa/verify.py mismatch
```

`runPlatformHostTest` 运行 JUnit 和 Forge GameTest。`runPlatformWorldTest` 在两个独立专服进程中保存并重开同一存档。`play` 与 `mismatch` 启动实际游戏客户端，需要图形会话。开发夹具由单独的 `ic2_qa` 模组加载。

### 安装包复验

使用独立安装的 Forge 43.5.0 专服依赖。将下列参数改为当前 JAR、专服的 `libraries/` 和已确认的 `eula.txt`，输出目录使用新路径。脚本复制一份发行 JAR，所有专服都核对同一个 SHA-256，每组验收保留日志和结果。

```sh
./gradlew :forge:1.19.2:reobfJar
python3 -B src/forge/1.19.2/qa/verify_production.py \
  --jar src/forge/1.19.2/build/libs/ic2-forge-1.19.2-0.1.0.jar \
  --libraries /absolute/path/to/forge-server/libraries \
  --eula /absolute/path/to/forge-server/eula.txt \
  --directory .task-context/forge-release-proof
```

机器测例通过真实电缆和管道传输。首次安装、冷重启、整条生产线和客户端菜单各有独立记录。`verify.py smoke` 另跑实际加载器上的 GameTest，覆盖玩家操作触发的扫描、图样入库、单次和连续复制。测试模组位于单独源集。发布前由隔离的规格端执行相似度检验，交接只收录脱敏统计。

## 发行差异

参考配方中两种空单元的图案相同，中心都是玻璃板。此版保留旧单元的玻璃板配方，通用流体单元改用玻璃方块，两个配方仍各使用四个锡外壳，均可在同一生存世界合成。通用流体单元保留具体液体、数据和毫桶量，最大 1,000 mB。

废料盒可手持右键开启，也能以原生红石发射器自动开启。两种方式都从同一个权重表抽取物品。[黑箱对照和权重来源](spec/registry/scrap-box-observation.md)

## 工程依据

- 目标版本、工具校验值与构建输入见[平台记录](docs/02-platform.md)。
- 宿主行为、网络帧、配置边界与验收入口见[Forge 适配契约](spec/platform/forge-1.19.2.md)。
- 修复版的发行包提供黑盒行为参照。实现与 QA 分别从行为规格编写代码和断言，原版及修复版材料在独立环境保管。[洁净室决策](decisions.md) · [素材登记](assets/SOURCES.md)

代码遵循 LGPL-3.0，新作素材遵循 CC BY-SA 4.0。
