# Forge 1.19.2 构建环境

| 项目 | 本分支使用的版本 |
|---|---|
| Minecraft | 1.19.2，Mojang 官方映射 |
| Forge | 43.5.0 |
| Java | JDK 17，生产类文件版本 61 |
| Gradle / ForgeGradle | 8.8 / 6.0.54 |
| 游戏标识 / Java 包 | `ic2` / `com.industrialage.ia2` |

包装器来自 Forge 43.5.0 官方 MDK。MDK ZIP 的 SHA-1 为 `6ab341eccdbf89ee3ef911afb43a890ed31d1f65`。Gradle 发行包的 SHA-256 为 `a4b4158601f8636cdeeab09bd76afb640030bb5b144aafe261a5e8af027dc612`。包装器启动时核对发行包，构建依赖的校验记录在 `gradle/verification-metadata.xml`。[Forge 下载页](https://files.minecraftforge.net/net/minecraftforge/forge/index_1.19.2.html) · [Gradle 8.8 校验值](https://downloads.gradle.org/distributions/gradle-8.8-bin.zip.sha256) · [Forge 开发环境](https://docs.minecraftforge.net/en/1.19.x/gettingstarted/)

`src/common` 的中立服务与 `src/forge/1.19.2` 的宿主实现一起编入生产 JAR。ForgeGradle 的 `reobfJar` 将开发环境名称映射到发行环境。资源包格式为 9。配置声明来自本仓库的 58 项行为规格。工具、依赖和原版游戏由 Gradle 获取，核心逻辑按本项目的洁净室分工维护。

ForgeGradle 在本机生成映射 JAR 时会写入时间戳。构建将条目按固定顺序、固定时间编码，再对整个归档执行 Gradle 严格 SHA-256 校验。Linux、macOS 和 Windows 的原生依赖按各自发行文件锁定。复验时可运行 `./gradlew --dependency-verification strict clean build`。

分支起点为 `10a57bacabd33c2fb282fd3a6b8bdc5991521cea`。主线当时的 NeoForge 运行记录可按提交复查。本分支的验证使用自己的 Forge 实例、存档和记录。[主线 P2 快照](https://github.com/spectrum527/ic2/tree/10a57bacabd33c2fb282fd3a6b8bdc5991521cea) · [1.19.2 验收](gates/1.19.2-P2.md)
